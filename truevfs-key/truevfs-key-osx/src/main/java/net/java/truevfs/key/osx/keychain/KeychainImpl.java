/*
 * Copyright (C) 2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.osx.keychain;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;
import static net.java.truevfs.key.osx.keychain.CoreFoundation.*;
import net.java.truevfs.key.osx.keychain.Keychain.AttributeClass;
import net.java.truevfs.key.osx.keychain.Keychain.Item;
import net.java.truevfs.key.osx.keychain.Keychain.ItemClass;
import net.java.truevfs.key.osx.keychain.Keychain.Visitor;
import static net.java.truevfs.key.osx.keychain.KeychainUtils.*;
import static net.java.truevfs.key.osx.keychain.Security.*;
import net.java.truevfs.key.osx.keychain.Security.SecKeychainAttributeInfo;
import net.java.truevfs.key.osx.keychain.Security.SecKeychainAttributeList;
import net.java.truevfs.key.osx.keychain.Security.SecKeychainItemRef;
import net.java.truevfs.key.osx.keychain.Security.SecKeychainRef;
import net.java.truevfs.key.osx.keychain.Security.SecKeychainSearchRef;
import static net.java.truevfs.key.spec.safe.BufferUtils.*;

/**
 * The default implementation of {@link Keychain}.
 * Unfortunately, Apple's Keychain Services API isn't thread-safe, so this
 * implementation provides a rather coarse-grained approach to achieve
 * thread-safety.
 * In particular, no two threads can concurrently visit items.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class KeychainImpl extends Keychain {

    private @CheckForNull SecKeychainRef ref;

    KeychainImpl() throws KeychainException {
        final PointerByReference pr = new PointerByReference();
        check(SecKeychainCopyDefault(pr));
        this.ref = new SecKeychainRef(pr.getValue());
    }

    KeychainImpl(final String path, final @CheckForNull char[] password)
    throws KeychainException {
        final boolean prompt = null == password || 0 >= password.length;
        final PointerByReference pr = new PointerByReference();
        final ByteBuffer buffer = prompt ? null : byteBuffer(password);
        final int length = prompt ? 0 : (int) buffer.remaining();
        synchronized (KeychainImpl.class) { // this isn't really isolated!
            int s = SecKeychainCreate(path, length, buffer, prompt, null, pr);
            if (s == errSecDuplicateKeychain) s = SecKeychainOpen(path, pr);
            check(s);
        }
        this.ref = new SecKeychainRef(pr.getValue());
    }

    @Override
    public synchronized void createItem(
            final ItemClass id,
            final Map<AttributeClass, ByteBuffer> attributes,
            final ByteBuffer secret)
    throws KeychainException {
        check(SecKeychainItemCreateFromContent(
                id.getTag(),
                list(attributes),
                secret.remaining(), secret,
                ref(),
                null,
                null));
    }

    @Override
    public synchronized void visitItems(
            @CheckForNull ItemClass id,
            final @CheckForNull Map<AttributeClass, ByteBuffer> attributes,
            final Visitor visitor)
    throws KeychainException {
        if (null == id) id = ItemClass.ANY_ITEM;

        final SecKeychainSearchRef sr;
        {
            final PointerByReference srr = new PointerByReference();
            check(SecKeychainSearchCreateFromAttributes(
                    ref(),
                    id.getTag(),
                    list(attributes),
                    srr));
            sr = new SecKeychainSearchRef(srr.getValue());
        }

        class Visit implements Callable<Void> {

            private final EnumMap<ItemClass, SecKeychainAttributeInfo>
                    infos = new EnumMap<>(ItemClass.class);

            SecKeychainAttributeInfo info(final ItemClass id) {
                SecKeychainAttributeInfo info = infos.get(id);
                if (null != info) return info;
                info = KeychainUtils.info(id.getAttributeClasses());
                infos.put(id, info);
                return info;
            }

            @Override
            public Void call() throws KeychainException {
                while (true) {
                    final SecKeychainItemRef ir;
                    {
                        final PointerByReference irr = new PointerByReference();
                        final int status  = SecKeychainSearchCopyNext(sr, irr);
                        switch (status) {
                            case errSecItemNotFound:
                                return null;
                            case 0:
                                ir = new SecKeychainItemRef(irr.getValue());
                                break;
                            default:
                                throw KeychainException.create(status);
                        }
                    }

                    class ItemImpl implements Item {

                        private ItemClass id;

                        @Override
                        public ItemClass getItemClass()
                        throws KeychainException {
                            ItemClass id = this.id;
                            if (null != id) return id;
                            final IntByReference cr = new IntByReference();
                            check(SecKeychainItemCopyAttributesAndData(
                                    ir, null, cr, null, null, null));
                            final int c = cr.getValue();
                            this.id = id = ItemClass.lookup(c);
                            if (null == id)
                                throw (KeychainException) KeychainException
                                        .create(errSecUnimplemented)
                                        .initCause(new UnsupportedOperationException("Unknown class id: " + c));
                            return id;
                        }

                        @Override
                        public Map<AttributeClass, ByteBuffer> getAttributes()
                        throws KeychainException {
                            final PointerByReference ar = new PointerByReference();
                            check(SecKeychainItemCopyAttributesAndData(
                                    ir, info(getItemClass()), null, ar, null, null));
                            final SecKeychainAttributeList
                                    l = new SecKeychainAttributeList(
                                        ar.getValue());
                            try {
                                l.read();
                                return map(l);
                            } finally {
                                SecKeychainItemFreeAttributesAndData(l, null);
                            }
                        }

                        @Override
                        public void putAttributes(final Map<AttributeClass, ByteBuffer> attributes)
                        throws KeychainException {
                            check(SecKeychainItemModifyAttributesAndData(
                                    ir,
                                    list(Objects.requireNonNull(attributes)),
                                    0, null));
                        }

                        @Override
                        public ByteBuffer getSecret() throws KeychainException {
                            final IntByReference lr = new IntByReference();
                            final PointerByReference dr = new PointerByReference();
                            check(SecKeychainItemCopyAttributesAndData(
                                    ir, null, null, null, lr, dr));
                            final Pointer p = dr.getValue();
                            try {
                                final long l = lr.getValue() & 0xFFFF_FFFFL;
                                return (ByteBuffer) ByteBuffer
                                        .allocateDirect((int) l)
                                        .put(p.getByteBuffer(0, l))
                                        .flip();
                            } finally {
                                SecKeychainItemFreeAttributesAndData(null, p);
                            }
                        }

                        @Override
                        public void setSecret(final ByteBuffer secret)
                        throws KeychainException {
                            final int length = secret.remaining();
                            check(SecKeychainItemModifyAttributesAndData(
                                    ir, null, length, secret));
                        }

                        @Override
                        public void delete() throws KeychainException {
                            check(SecKeychainItemDelete(ir));
                        }
                    } // ItemImpl

                    try {
                        visitor.visit(new ItemImpl());
                    } finally {
                        CFRelease(ir);
                    }
                }
            }
        } // Visit

        try {
            new Visit().call();
        } finally {
            CFRelease(sr);
        }
    }

    @Override
    public synchronized void delete() throws KeychainException {
        check(SecKeychainDelete(ref()));
        close();
    }

    private static void check(int status) throws KeychainException {
        if (errSecSuccess != status) throw KeychainException.create(status);
    }

    private SecKeychainRef ref() throws KeychainException {
        if (null == ref) check(errSecInvalidKeychain);
        return ref;
    }

    @Override
    public synchronized void close() {
        final SecKeychainRef ref = this.ref;
        if (null == ref) return;
        this.ref = null;
        CFRelease(ref);
    }

    @Override
    @SuppressWarnings("FinalizeDeclaration")
    protected void finalize() throws Throwable {
        try { super.finalize(); }
        finally { close(); }
    }
}
