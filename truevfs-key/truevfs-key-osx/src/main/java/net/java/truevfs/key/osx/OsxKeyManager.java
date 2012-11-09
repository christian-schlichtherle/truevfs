/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.osx;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.logging.LocalizedLogger;
import net.java.truevfs.key.osx.keychain.DuplicateItemException;
import net.java.truevfs.key.osx.keychain.Keychain;
import net.java.truevfs.key.osx.keychain.Keychain.AttributeClass;
import static net.java.truevfs.key.osx.keychain.Keychain.AttributeClass.*;
import net.java.truevfs.key.osx.keychain.Keychain.Item;
import static net.java.truevfs.key.osx.keychain.Keychain.ItemClass.*;
import net.java.truevfs.key.osx.keychain.Keychain.Visitor;
import net.java.truevfs.key.osx.keychain.KeychainException;
import net.java.truevfs.key.spec.AbstractKeyManager;
import net.java.truevfs.key.spec.KeyManager;
import net.java.truevfs.key.spec.KeyProvider;
import net.java.truevfs.key.spec.param.AesPbeParameters;
import static net.java.truevfs.key.spec.util.BufferUtils.*;
import org.slf4j.Logger;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class OsxKeyManager extends AbstractKeyManager<AesPbeParameters> {

    private static final String KEYCHAIN = "TrueVFS";
    private static final Logger logger = new LocalizedLogger(OsxKeyManager.class);

    private final KeyManager<AesPbeParameters> manager;
    private Keychain keychain;
    private volatile boolean skip;

    public OsxKeyManager(final KeyManager<AesPbeParameters> manager) {
        this.manager = Objects.requireNonNull(manager);
    }

    @Override
    public KeyProvider<AesPbeParameters> provider(URI resource) {
        return new OsxKeyProvider(this, resource, manager.provider(resource));
    }

    @Override
    public void move(final URI oldResource, final URI newResource) {
        final AesPbeParameters key = getKey(oldResource);
        manager.move(oldResource, newResource);
        setKey(newResource, key);
        setKey(oldResource, null);
    }

    @Override
    public void delete(final URI resource) {
        manager.delete(resource);
        setKey(resource, null);
    }

    @Override
    public void release(URI resource) {
        skip = false;
        manager.release(resource);
    }

    @CheckForNull AesPbeParameters getKey(final URI resource) {
        return access(resource, new Action<AesPbeParameters>() {
            @Override
            public AesPbeParameters call(
                    final Keychain keychain,
                    final Map<AttributeClass, ByteBuffer> attributes)
            throws KeychainException {

                class Read implements Visitor {
                    private @CheckForNull AesPbeParameters key;

                    @Override
                    public void visit(final Item item) throws KeychainException {
                        final char[] pw = charArray(item.getSecret());
                        try {
                            (key = new AesPbeParameters()).setPassword(pw);
                        } finally {
                            Arrays.fill(pw, (char) 0);
                        }
                    }
                }

                final Read read = new Read();
                keychain.visitItems(GENERIC_PASSWORD, attributes, read);
                return read.key;
            }
        });
    }

    void setKey(
            final URI resource,
            final @CheckForNull AesPbeParameters key) {
        access(resource, new Action<Void>() {
            @Override
            public Void call(
                    final Keychain keychain,
                    final Map<AttributeClass, ByteBuffer> attributes)
            throws KeychainException {

                if (null != key) {
                    final char[] pw = key.getPassword();
                    try {
                        final ByteBuffer buffer = byteBuffer(pw);

                        class Update implements Visitor {
                            @Override
                            public void visit(Item item) throws KeychainException {
                                item.setSecret(buffer);
                            }
                        }

                        try {
                            keychain.createItem(GENERIC_PASSWORD, attributes, buffer);
                        } catch (final DuplicateItemException ex) {
                            keychain.visitItems(GENERIC_PASSWORD, attributes, new Update());
                        }
                    } finally {
                        Arrays.fill(pw, (char) 0);
                    }
                } else {

                    class Delete implements Visitor {
                        @Override
                        public void visit(Item item) throws KeychainException {
                            item.delete();
                        }
                    }

                    keychain.visitItems(GENERIC_PASSWORD, attributes, new Delete());
                }
                return null;
            }
        });
    }

    private @CheckForNull <T> T access(final URI resource, final Action<T> action) {
        if (skip) return null;
        try {
            return action.call(open(), attributes(resource));
        } catch (final KeychainException ex) {
            skip = true;
            logger.debug("keychainException", ex);
            return null;
        }
    }

    private interface Action<T> {
        @CheckForNull T call(
                Keychain keychain,
                Map<AttributeClass, ByteBuffer> attributes)
        throws KeychainException;
    } // Action

    private static Map<AttributeClass, ByteBuffer> attributes(final URI resource) {
        final Map<AttributeClass, ByteBuffer>
                m = new EnumMap<>(AttributeClass.class);
        m.put(ACCOUNT, byteBuffer("TrueVFS"));
        m.put(SERVICE, byteBuffer(resource.toString()));
        return m;
    }

    private synchronized Keychain open() throws KeychainException {
        if (null != keychain) return keychain;
        final char[] password = KEYCHAIN.toCharArray();
        try {
            return keychain = Keychain.open(KEYCHAIN, password);
        } finally {
            Arrays.fill(password, (char) 0);
        }
    }

    private synchronized void close() {
        try (Keychain _ = keychain) { keychain = null; }
    }

    @Override
    @SuppressWarnings("FinalizeDeclaration")
    protected void finalize() throws Throwable {
        try { super.finalize(); }
        finally { close(); }
    }
}
