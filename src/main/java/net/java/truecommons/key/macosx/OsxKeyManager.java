/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.macosx;

import net.java.truecommons.key.macosx.keychain.DuplicateItemException;
import net.java.truecommons.key.macosx.keychain.Keychain;
import net.java.truecommons.key.macosx.keychain.Keychain.AttributeClass;
import net.java.truecommons.key.macosx.keychain.Keychain.Item;
import net.java.truecommons.key.macosx.keychain.Keychain.Visitor;
import net.java.truecommons.key.macosx.keychain.KeychainException;
import net.java.truecommons.key.spec.AbstractKeyManager;
import net.java.truecommons.key.spec.KeyManager;
import net.java.truecommons.key.spec.KeyProvider;
import net.java.truecommons.key.spec.prompting.AbstractPromptingPbeParameters;
import net.java.truecommons.logging.LocalizedLogger;

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
import java.util.Optional;

import static net.java.truecommons.key.macosx.keychain.Keychain.AttributeClass.GENERIC;
import static net.java.truecommons.key.macosx.keychain.Keychain.AttributeClass.SERVICE;
import static net.java.truecommons.key.macosx.keychain.Keychain.ItemClass.GENERIC_PASSWORD;
import static net.java.truecommons.shed.Buffers.*;

/**
 * Uses Apple's Keychain Services API to persist passwords.
 *
 * @author Christian Schlichtherle
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class OsxKeyManager<P extends AbstractPromptingPbeParameters<P, ?>>
        extends AbstractKeyManager<P> {

    private static final String KEYCHAIN = "TrueCommons KeyManager";
    private static final String ACCOUNT = KEYCHAIN;

    private final KeyManager<P> manager;
    private final Class<P> keyClass;
    private volatile boolean skip;

    public OsxKeyManager(final KeyManager<P> manager, final Class<P> keyClass) {
        this.manager = Objects.requireNonNull(manager);
        this.keyClass = Objects.requireNonNull(keyClass);
    }

    @Override
    public KeyProvider<P> provider(URI uri) {
        return new OsxKeyProvider<>(this, uri, manager.provider(uri));
    }

    @Override
    public void release(URI uri) {
        skip = false;
        manager.release(uri);
    }

    @Override
    public void link(final URI originUri, final URI targetUri) {
        final Optional<P> param = getKey(originUri);
        manager.link(originUri, targetUri);
        setKey(targetUri, param);
    }

    @Override
    public void unlink(final URI uri) {
        manager.unlink(uri);
        setKey(uri, Optional.empty());
    }

    Optional<P> getKey(final URI uri) {
        final GetKeyAction action = new GetKeyAction();
        runAction(uri, action);
        return action.optParam;
    }

    void setKey(URI uri, Optional<P> optionalParam) {
        runAction(uri, new SetKeyAction(optionalParam));
    }

    static ByteBuffer serialize(final Object object) {
        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream(512)) {
            try (XMLEncoder encoder = new XMLEncoder(bos)) {
                encoder.writeObject(object);
            }
            bos.flush(); // redundant
            return copy(ByteBuffer.wrap(bos.toByteArray()));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T deserialize(final ByteBuffer xml) {
        final byte[] array = new byte[xml.remaining()]; // cannot use bb.array()!
        xml.duplicate().get(array);
        try (XMLDecoder decoder = new XMLDecoder(new ByteArrayInputStream(array))) {
            return (T) decoder.readObject();
        }
    }

    private void runAction(final URI uri, final Action action) {
        if (!skip) {
            try (Keychain keychain = Keychain.open(KEYCHAIN, null)) {
                action.run(new Controller() {

                    final Map<AttributeClass, ByteBuffer> attributes = new EnumMap<>(AttributeClass.class);

                    {
                        attributes.put(AttributeClass.ACCOUNT, byteBuffer(ACCOUNT));
                        attributes.put(SERVICE, byteBuffer(uri.toString()));
                    }

                    @Override
                    public void setAttribute(final AttributeClass key, final Optional<ByteBuffer> optionalValue) {
                        if (optionalValue.isPresent()) {
                            final ByteBuffer value = optionalValue.get();
                            attributes.put(key, value);
                            return;
                        }
                        attributes.remove(key);
                    }

                    @Override
                    public void createItem(ByteBuffer secret) throws KeychainException {
                        keychain.createItem(GENERIC_PASSWORD, attributes, secret);
                    }

                    @Override
                    public void visitItems(Visitor visitor) throws KeychainException {
                        keychain.visitItems(GENERIC_PASSWORD, attributes, visitor);
                    }
                });
            } catch (final Exception e) {
                skip = true;
                new LocalizedLogger(OsxKeyManager.class).debug("access.exception", e);
            }
        }
    }

    private interface Action {

        void run(Controller controller) throws KeychainException;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private interface Controller {

        void setAttribute(AttributeClass key, Optional<ByteBuffer> optionalValue);

        void createItem(ByteBuffer secret) throws KeychainException;

        void visitItems(Visitor visitor) throws KeychainException;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final class GetKeyAction implements Action, Visitor {

        Optional<P> optParam = Optional.empty();

        @Override
        public void run(Controller controller) throws KeychainException {
            controller.visitItems(this);
        }

        @Override
        public void visit(final Item item) throws KeychainException {
            optParam = Optional.ofNullable(item.getAttribute(GENERIC)).map(OsxKeyManager::deserialize);
            if (!optParam.isPresent()) {
                optParam = Optional.of(newKey());
            }
            final P p = optParam.get();
            assert null == p.getSecret();
            final ByteBuffer secret = item.getSecret();
            try {
                p.setSecret(secret);
            } finally {
                fill(secret, (byte) 0);
            }
        }

        private P newKey() {
            try {
                return keyClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private final class SetKeyAction implements Action {

        private final Optional<P> optParam;

        SetKeyAction(final Optional<P> optParam) {
            this.optParam = optParam;
        }

        @Override
        public void run(final Controller controller) throws KeychainException {
            if (optParam.isPresent()) {
                final P param = optParam.get();
                final Optional<ByteBuffer> optSecret = Optional.ofNullable(param.getSecret());
                if (optSecret.isPresent()) {
                    final ByteBuffer secret = optSecret.get();
                    try {
                        final ByteBuffer newXml = serialize(param);
                        final P newParam = deserialize(newXml); // rip off transient fields

                        class UpdateVisitor implements Visitor {
                            @Override
                            public void visit(final Item item) throws KeychainException {
                                {
                                    final ByteBuffer oldSecret =
                                            item.getSecret();
                                    if (!secret.equals(oldSecret))
                                        item.setSecret(secret);
                                }
                                {
                                    final Optional<ByteBuffer> oldXml = Optional.ofNullable(item.getAttribute(GENERIC));
                                    final Optional<P> oldParam = oldXml.map(OsxKeyManager::deserialize);
                                    if (!Optional.of(newParam).equals(oldParam)) {
                                        item.setAttribute(GENERIC, newXml);
                                    }
                                }
                            }
                        }

                        try {
                            controller.setAttribute(GENERIC, Optional.of(newXml));
                            controller.createItem(secret);
                        } catch (final DuplicateItemException ex) {
                            controller.setAttribute(GENERIC, Optional.empty());
                            controller.visitItems(new UpdateVisitor());
                        }
                    } finally {
                        fill(secret, (byte) 0);
                    }

                    return;
                }
                throw new IllegalStateException();
            }

            class DeleteVisitor implements Visitor {
                @Override
                public void visit(Item item) throws KeychainException {
                    item.delete();
                }
            }

            controller.visitItems(new DeleteVisitor());
        }
    }
}
