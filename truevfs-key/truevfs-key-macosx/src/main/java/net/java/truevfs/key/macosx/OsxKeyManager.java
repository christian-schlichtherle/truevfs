/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.macosx;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.logging.LocalizedLogger;
import net.java.truevfs.key.macosx.keychain.DuplicateItemException;
import net.java.truevfs.key.macosx.keychain.Keychain;
import net.java.truevfs.key.macosx.keychain.Keychain.AttributeClass;
import static net.java.truevfs.key.macosx.keychain.Keychain.AttributeClass.*;
import net.java.truevfs.key.macosx.keychain.Keychain.Item;
import static net.java.truevfs.key.macosx.keychain.Keychain.ItemClass.*;
import net.java.truevfs.key.macosx.keychain.Keychain.Visitor;
import net.java.truevfs.key.macosx.keychain.KeychainException;
import net.java.truevfs.key.spec.AbstractKeyManager;
import net.java.truevfs.key.spec.KeyManager;
import net.java.truevfs.key.spec.KeyProvider;
import net.java.truevfs.key.spec.prompting.AbstractPromptingPbeParameters;
import static net.java.truevfs.key.spec.util.Buffers.*;
import org.slf4j.Logger;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class OsxKeyManager<P extends AbstractPromptingPbeParameters<P, ?>>
extends AbstractKeyManager<P> {

    private static final String KEYCHAIN = "TrueVFS";
    private static final Logger logger = new LocalizedLogger(OsxKeyManager.class);

    private final KeyManager<P> manager;
    private final Class<P> keyClass;
    private Keychain keychain;
    private volatile boolean skip;

    public OsxKeyManager(final KeyManager<P> manager, final Class<P> keyClass) {
        this.manager = Objects.requireNonNull(manager);
        this.keyClass = Objects.requireNonNull(keyClass);
    }

    @Override
    public KeyProvider<P> provider(URI resource) {
        return new OsxKeyProvider<>(this, resource, manager.provider(resource));
    }

    @Override
    public void link(final URI oldResource, final URI newResource) {
        final P param = getKey(oldResource);
        manager.link(oldResource, newResource);
        setKey(newResource, param);
    }

    @Override
    public void unlink(final URI resource) {
        manager.unlink(resource);
        setKey(resource, null);
    }

    @Override
    public void release(URI resource) {
        skip = false;
        manager.release(resource);
    }

    @CheckForNull P getKey(final URI resource) {
        return access(resource, new Action<P>() {
            @Override
            public P call(
                    final Keychain keychain,
                    final Map<AttributeClass, ByteBuffer> attributes)
            throws KeychainException {

                class Read implements Visitor {
                    private @CheckForNull P param;

                    @Override
                    public void visit(final Item item) throws KeychainException {
                        param = (P) deserialize(item.getAttribute(GENERIC));
                        if (null == param) try {
                            param = keyClass.newInstance();
                        } catch (final InstantiationException | IllegalAccessException ex) {
                            logger.debug("getKey.exception", ex);
                            return;
                        }
                        assert null == param.getSecret();
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
            final @CheckForNull P param) {
        access(resource, new Action<Void>() {
            @Override
            public Void call(
                    final Keychain keychain,
                    final Map<AttributeClass, ByteBuffer> attributes)
            throws KeychainException {

                if (null != param) {
                    final ByteBuffer newSecret = param.getSecret();
                    try {
                        final ByteBuffer newXml = serialize(param);
                        final P newParam = (P) deserialize(newXml); // rip off transient fields

                        class Update implements Visitor {
                            @Override
                            public void visit(final Item item)
                            throws KeychainException {
                                {
                                    final ByteBuffer oldSecret =
                                            item.getSecret();
                                    if (!newSecret.equals(oldSecret))
                                        item.setSecret(newSecret);
                                }
                                {
                                    final @CheckForNull ByteBuffer oldXml =
                                            item.getAttribute(GENERIC);
                                    final @CheckForNull P oldParam =
                                            (P) deserialize(oldXml);
                                    if (!newParam.equals(oldParam))
                                        item.setAttribute(GENERIC, newXml);
                                }
                            }
                        } // Update

                        try {
                            attributes.put(GENERIC, newXml);
                            keychain.createItem(GENERIC_PASSWORD, attributes, newSecret);
                        } catch (final DuplicateItemException ex) {
                            attributes.remove(GENERIC);
                            keychain.visitItems(GENERIC_PASSWORD, attributes, new Update());
                        }
                    } finally {
                        fill(newSecret, (byte) 0);
                    }
                } else {

                    class Delete implements Visitor {
                        @Override
                        public void visit(Item item) throws KeychainException {
                            item.delete();
                        }
                    } // Update

                    keychain.visitItems(GENERIC_PASSWORD, attributes, new Delete());
                }
                return null;
            }
        });
    }

    static @CheckForNull ByteBuffer serialize(
            final @CheckForNull Object object) {
        if (null == object) return null;
        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream(512)) {
            try (final XMLEncoder _ = new XMLEncoder(bos)) {
                _.writeObject(object);
            }
            bos.flush(); // redundant
            return copy(ByteBuffer.wrap(bos.toByteArray()));
        } catch (final IOException ex) {
            logger.warn("serialize.exception", ex);
            return null;
        }
    }

    static @CheckForNull Object deserialize(final @CheckForNull ByteBuffer xml) {
        if (null == xml) return null;
        final byte[] array = new byte[xml.remaining()]; // cannot use bb.array()!
        xml.duplicate().get(array);
        try (final XMLDecoder _ = new XMLDecoder(new ByteArrayInputStream(array))) {
            return (AbstractPromptingPbeParameters<?, ?>) _.readObject();
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
        return keychain = Keychain.open(KEYCHAIN, null);
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
