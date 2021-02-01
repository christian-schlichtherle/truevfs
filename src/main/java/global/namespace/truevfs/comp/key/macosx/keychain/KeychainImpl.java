/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.macosx.keychain;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import global.namespace.truevfs.comp.key.macosx.keychain.Security.*;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import static global.namespace.truevfs.comp.key.macosx.keychain.CoreFoundation.CFRelease;
import static global.namespace.truevfs.comp.key.macosx.keychain.KeychainUtils.list;
import static global.namespace.truevfs.comp.key.macosx.keychain.KeychainUtils.map;
import static global.namespace.truevfs.comp.key.macosx.keychain.Security.*;
import static global.namespace.truevfs.comp.shed.Buffers.byteBuffer;

/**
 * The default implementation of {@link Keychain}.
 * Unfortunately, Apple's Keychain Services API isn't thread-safe, so this
 * implementation provides a rather coarse-grained approach to achieve
 * thread-safety.
 * In particular, no two threads can concurrently visit items.
 *
 * @author Christian Schlichtherle
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
final class KeychainImpl extends Keychain {

    private Optional<SecKeychainRef> optRef;

    KeychainImpl() throws KeychainException {
        final PointerByReference pr = new PointerByReference();
        check(SecKeychainCopyDefault(pr));
        this.optRef = Optional.of(new SecKeychainRef(pr.getValue()));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    KeychainImpl(final String path, final Optional<char[]> password) throws KeychainException {
        final boolean prompt = !password.isPresent() || 0 >= password.get().length;
        final PointerByReference pr = new PointerByReference();
        final ByteBuffer buffer = prompt ? null : byteBuffer(password.get());
        final int length = prompt ? 0 : buffer.remaining();
        synchronized (KeychainImpl.class) { // this isn't really isolated!
            int s = SecKeychainCreate(path, length, buffer, prompt, null, pr);
            if (s == errSecDuplicateKeychain)
                s = SecKeychainOpen(path, pr);
            check(s);
        }
        this.optRef = Optional.of(new SecKeychainRef(pr.getValue()));
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
            @Nullable ItemClass id,
            final @Nullable Map<AttributeClass, ByteBuffer> attributes,
            final Visitor visitor)
            throws KeychainException {
        if (null == id)
            id = ItemClass.ANY_ITEM;

        final SecKeychainSearchRef sr;
        {
            final PointerByReference srr = new PointerByReference();
            check(SecKeychainSearchCreateFromAttributes(
                    ref(),
                    id.getTag(),
                    Optional.ofNullable(attributes).map(KeychainUtils::list).orElse(null),
                    srr));
            sr = new SecKeychainSearchRef(srr.getValue());
        }

        class Visit implements Callable<Void> {

            private final EnumMap<ItemClass, SecKeychainAttributeInfo>
                    infos = new EnumMap<>(ItemClass.class);

            SecKeychainAttributeInfo info(final ItemClass id) {
                SecKeychainAttributeInfo info = infos.get(id);
                if (null != info)
                    return info;
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
                        final int status = SecKeychainSearchCopyNext(sr, irr);
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

                    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
                    class ItemImpl implements Item {

                        Optional<ItemClass> oic = Optional.empty();

                        @Override
                        public ItemClass getItemClass()
                                throws KeychainException {
                            if (oic.isPresent()) {
                                return oic.get();
                            }
                            final IntByReference cr = new IntByReference();
                            check(SecKeychainItemCopyAttributesAndData(
                                    ir, null, cr, null, null, null));
                            final int c = cr.getValue();
                            oic = ItemClass.lookup(c);
                            if (!oic.isPresent())
                                throw (KeychainException) KeychainException
                                        .create(errSecUnimplemented)
                                        .initCause(new UnsupportedOperationException("Unknown class id: " + c));
                            return oic.get();
                        }

                        @Override
                        public @Nullable
                        ByteBuffer getAttribute(
                                AttributeClass id)
                                throws KeychainException {
                            return getAttributes(KeychainUtils.info(id)).get(id);
                        }

                        @Override
                        public void setAttribute(
                                final AttributeClass id,
                                final @Nullable ByteBuffer value)
                                throws KeychainException {
                            final Map<AttributeClass, ByteBuffer> attributes =
                                    new EnumMap<>(AttributeClass.class);
                            attributes.put(id, value);
                            putAttributeMap(attributes);
                        }

                        @Override
                        public Map<AttributeClass, ByteBuffer> getAttributeMap()
                                throws KeychainException {
                            return getAttributes(info(getItemClass()));
                        }

                        private Map<AttributeClass, ByteBuffer> getAttributes(
                                final SecKeychainAttributeInfo info)
                                throws KeychainException {
                            final PointerByReference ar = new PointerByReference();
                            check(SecKeychainItemCopyAttributesAndData(
                                    ir, info, null, ar, null, null));
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
                        public void putAttributeMap(
                                final Map<AttributeClass, ByteBuffer> attributes)
                                throws KeychainException {
                            check(SecKeychainItemModifyAttributesAndData(
                                    ir,
                                    list(attributes),
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
                            check(SecKeychainItemModifyAttributesAndData(
                                    ir, null, secret.remaining(), secret));
                        }

                        @Override
                        public void delete() throws KeychainException {
                            check(SecKeychainItemDelete(ir));
                        }
                    }

                    try {
                        visitor.visit(new ItemImpl());
                    } finally {
                        CFRelease(ir);
                    }
                }
            }
        }

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
        if (errSecSuccess != status)
            throw KeychainException.create(status);
    }

    private SecKeychainRef ref() throws KeychainException {
        return optRef.orElseThrow(() -> KeychainException.create(errSecInvalidKeychain));
    }

    @Override
    public synchronized void close() {
        if (optRef.isPresent()) {
            final SecKeychainRef r = optRef.get();
            optRef = Optional.empty();
            CFRelease(r);
        }
    }

    @Override
    @SuppressWarnings("FinalizeDeclaration")
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }
}
