/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.macos;

import global.namespace.truevfs.comp.key.api.KeyManager;
import global.namespace.truevfs.comp.key.api.KeyProvider;
import global.namespace.truevfs.comp.key.api.prompting.AbstractPromptingPbeParameters;
import global.namespace.truevfs.comp.key.macos.keychain.DuplicateItemException;
import global.namespace.truevfs.comp.key.macos.keychain.Keychain;
import global.namespace.truevfs.comp.key.macos.keychain.Keychain.AttributeClass;
import global.namespace.truevfs.comp.key.macos.keychain.Keychain.Item;
import global.namespace.truevfs.comp.key.macos.keychain.Keychain.Visitor;
import global.namespace.truevfs.comp.key.macos.keychain.KeychainException;
import global.namespace.truevfs.comp.logging.LocalizedLogger;
import lombok.val;

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

import static global.namespace.truevfs.comp.key.macos.keychain.Keychain.AttributeClass.GENERIC;
import static global.namespace.truevfs.comp.key.macos.keychain.Keychain.AttributeClass.SERVICE;
import static global.namespace.truevfs.comp.key.macos.keychain.Keychain.ItemClass.GENERIC_PASSWORD;
import static global.namespace.truevfs.comp.util.Buffers.*;

/**
 * Uses Apple's Keychain Services API to persist passwords.
 *
 * @author Christian Schlichtherle
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class MacosKeyManager<P extends AbstractPromptingPbeParameters<P, ?>> implements KeyManager<P> {

    private static final String KEYCHAIN = "TrueVFS.keychain-db";
    private static final String ACCOUNT = "TrueVFS";

    private final KeyManager<P> manager;
    private final Class<P> keyClass;
    private volatile boolean skip;

    public static <P extends AbstractPromptingPbeParameters<P, ?>>
    KeyManager<P> create(KeyManager<P> manager, Class<P> keyClass) {
        return new MacosKeyManager<>(manager, keyClass);
    }

    private MacosKeyManager(final KeyManager<P> manager, final Class<P> keyClass) {
        this.manager = Objects.requireNonNull(manager);
        this.keyClass = Objects.requireNonNull(keyClass);
    }

    @Override
    public KeyProvider<P> provider(URI uri) {
        return new MacosKeyProvider<>(this, uri, manager.provider(uri));
    }

    @Override
    public void release(URI uri) {
        skip = false;
        manager.release(uri);
    }

    @Override
    public void link(final URI originUri, final URI targetUri) {
        val param = getKey(originUri);
        manager.link(originUri, targetUri);
        setKey(targetUri, param);
    }

    @Override
    public void unlink(final URI uri) {
        manager.unlink(uri);
        setKey(uri, Optional.empty());
    }

    Optional<P> getKey(final URI uri) {
        val action = new GetKeyAction();
        runAction(uri, action);
        return action.optParam;
    }

    void setKey(URI uri, Optional<P> optionalParam) {
        runAction(uri, new SetKeyAction(optionalParam));
    }

    static ByteBuffer serialize(final Object object) {
        try (val bos = new ByteArrayOutputStream(512)) {
            try (val encoder = new XMLEncoder(bos)) {
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
        val array = new byte[xml.remaining()]; // cannot use bb.array()!
        xml.duplicate().get(array);
        try (val decoder = new XMLDecoder(new ByteArrayInputStream(array))) {
            return (T) decoder.readObject();
        }
    }

    private void runAction(final URI uri, final Action action) {
        if (!skip) {
            try (val keychain = Keychain.open(KEYCHAIN, () -> new char[0])) {
                action.run(new Controller() {

                    final Map<AttributeClass, ByteBuffer> attributes = new EnumMap<>(AttributeClass.class);

                    {
                        attributes.put(AttributeClass.ACCOUNT, byteBuffer(ACCOUNT));
                        attributes.put(SERVICE, byteBuffer(uri.toString()));
                    }

                    @Override
                    public void setAttribute(final AttributeClass key, final Optional<ByteBuffer> optionalValue) {
                        if (optionalValue.isPresent()) {
                            val value = optionalValue.get();
                            attributes.put(key, value);
                        } else {
                            attributes.remove(key);
                        }
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
                new LocalizedLogger(MacosKeyManager.class).debug("access.exception", e);
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
            optParam = Optional.ofNullable(item.getAttribute(GENERIC)).map(MacosKeyManager::deserialize);
            if (!optParam.isPresent()) {
                optParam = Optional.of(newKey());
            }
            val p = optParam.get();
            assert null == p.getSecret();
            val secret = item.getSecret();
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
                val param = optParam.get();
                val optSecret = Optional.ofNullable(param.getSecret());
                if (optSecret.isPresent()) {
                    val secret = optSecret.get();
                    try {
                        val newXml = serialize(param);
                        val newParam = deserialize(newXml); // rip off transient fields
                        try {
                            controller.setAttribute(GENERIC, Optional.of(newXml));
                            controller.createItem(secret);
                        } catch (final DuplicateItemException ex) {
                            controller.setAttribute(GENERIC, Optional.empty());
                            controller.visitItems(item -> {
                                {
                                    val oldSecret = item.getSecret();
                                    if (!secret.equals(oldSecret)) {
                                        item.setSecret(secret);
                                    }
                                }
                                {
                                    val oldXml = Optional.ofNullable(item.getAttribute(GENERIC));
                                    val oldParam = oldXml.map(MacosKeyManager::deserialize);
                                    if (!Optional.of(newParam).equals(oldParam)) {
                                        item.setAttribute(GENERIC, newXml);
                                    }
                                }
                            });
                        }
                    } finally {
                        fill(secret, (byte) 0);
                    }

                    return;
                }
                throw new IllegalStateException();
            }

            controller.visitItems(Item::delete);
        }
    }
}
