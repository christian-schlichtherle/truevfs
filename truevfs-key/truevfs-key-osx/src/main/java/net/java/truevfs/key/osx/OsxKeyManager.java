/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.osx;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
                    private @CheckForNull AesPbeParameters param;

                    @Override
                    public void visit(final Item item) throws KeychainException {
                        param = decode(item.getAttributes().get(GENERIC));
                        if (null == param) param = new AesPbeParameters();
                        final ByteBuffer secret = item.getSecret();
                        try {
                            param.setSecret(secret);
                        } finally {
                            fill(secret, (byte) 0);
                        }
                    }
                }

                final Read read = new Read();
                keychain.visitItems(GENERIC_PASSWORD, attributes, read);
                return read.param;
            }
        });
    }

    void setKey(
            final URI resource,
            final @CheckForNull AesPbeParameters param) {
        access(resource, new Action<Void>() {
            @Override
            public Void call(
                    final Keychain keychain,
                    final Map<AttributeClass, ByteBuffer> attributes)
            throws KeychainException {

                if (null != param) {
                    final ByteBuffer encSecret = param.getSecret();
                    try {
                        final ByteBuffer encParam = encode(param);

                        class Update implements Visitor {
                            @Override
                            public void visit(Item item) throws KeychainException {
                                final Map<AttributeClass, ByteBuffer>
                                        attributes = item.getAttributes();
                                attributes.put(GENERIC, encParam);
                                item.putAttributes(attributes);
                                item.setSecret(encSecret);
                            }
                        }

                        try {
                            attributes.put(GENERIC, encParam);
                            keychain.createItem(GENERIC_PASSWORD, attributes, encSecret);
                        } catch (final DuplicateItemException ex) {
                            attributes.remove(GENERIC);
                            keychain.visitItems(GENERIC_PASSWORD, attributes, new Update());
                        }
                    } finally {
                        fill(encSecret, (byte) 0);
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

    static @CheckForNull AesPbeParameters decode(final @CheckForNull ByteBuffer bb) {
        if (null == bb) return null;
        final byte[] array = new byte[bb.remaining()]; // cannot use bb.array()!
        bb.duplicate().get(array);
        try (final XMLDecoder _ = new XMLDecoder(new ByteArrayInputStream(array))) {
            final AesPbeParameters param = (AesPbeParameters) _.readObject();
            assert null == param.getSecret();
            return param;
        }
    }

    static @CheckForNull ByteBuffer encode(@CheckForNull AesPbeParameters param) {
        if (null == param) return null;
        param = param.clone();
        param.setSecret(null);
        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream(512)) {
            try (final XMLEncoder _ = new XMLEncoder(bos)) {
                _.writeObject(param);
            }
            bos.flush(); // redundant
            return copy(ByteBuffer.wrap(bos.toByteArray()));
        } catch (final IOException ex) {
            logger.warn("encode.exception", ex);
            return null;
        }
    }

    private @CheckForNull <T> T access(final URI resource, final Action<T> action) {
        if (skip) return null;
        try {
            return action.call(open(), attributes(resource));
        } catch (final KeychainException ex) {
            skip = true;
            logger.debug("access.exception", ex);
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
