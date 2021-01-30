/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.macosx;

import net.java.truecommons3.key.macosx.keychain.DuplicateItemException;
import net.java.truecommons3.key.macosx.keychain.Keychain;
import net.java.truecommons3.key.macosx.keychain.Keychain.AttributeClass;
import net.java.truecommons3.key.macosx.keychain.Keychain.Item;
import net.java.truecommons3.key.macosx.keychain.Keychain.Visitor;
import net.java.truecommons3.key.macosx.keychain.KeychainException;
import net.java.truecommons3.key.spec.AbstractKeyManager;
import net.java.truecommons3.key.spec.KeyManager;
import net.java.truecommons3.key.spec.KeyProvider;
import net.java.truecommons3.key.spec.prompting.AbstractPromptingPbeParameters;
import net.java.truecommons3.logging.LocalizedLogger;
import net.java.truecommons3.shed.Option;

import javax.annotation.concurrent.ThreadSafe;
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

import static net.java.truecommons3.key.macosx.keychain.Keychain.AttributeClass.GENERIC;
import static net.java.truecommons3.key.macosx.keychain.Keychain.AttributeClass.SERVICE;
import static net.java.truecommons3.key.macosx.keychain.Keychain.ItemClass.GENERIC_PASSWORD;
import static net.java.truecommons3.shed.Buffers.*;

/**
 * Uses Apple's Keychain Services API to persist passwords.
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@SuppressWarnings("LoopStatementThatDoesntLoop")
@ThreadSafe
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
        final Option<P> param = getKey(originUri);
        manager.link(originUri, targetUri);
        setKey(targetUri, param);
    }

    @Override
    public void unlink(final URI uri) {
        manager.unlink(uri);
        setKey(uri, Option.<P>none());
    }

    Option<P> getKey(final URI uri) {
        final GetKeyAction action = new GetKeyAction();
        runAction(uri, action);
        return action.param;
    }

    void setKey(final URI uri, final Option<P> optionalParam) {
        runAction(uri, new SetKeyAction(optionalParam));
    }

    static Option<ByteBuffer> serialize(final Option<?> optionalObject) {
        for (final Object object : optionalObject) {
            try (final ByteArrayOutputStream bos = new ByteArrayOutputStream(512)) {
                try (XMLEncoder encoder = new XMLEncoder(bos)) {
                    encoder.writeObject(object);
                }
                bos.flush(); // redundant
                return Option.some(copy(ByteBuffer.wrap(bos.toByteArray())));
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return Option.none();
    }

    static Option<?> deserialize(final Option<ByteBuffer> optionalXml) {
        for (final ByteBuffer xml : optionalXml) {
            final byte[] array = new byte[xml.remaining()]; // cannot use bb.array()!
            xml.duplicate().get(array);
            try (XMLDecoder decoder = new XMLDecoder(new ByteArrayInputStream(array))) {
                return Option.apply(decoder.readObject());
            }
        }
        return Option.none();
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
                    public void setAttribute(final AttributeClass key, final Option<ByteBuffer> optionalValue) {
                        for (final ByteBuffer value : optionalValue) {
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

    private interface Controller {

        void setAttribute(AttributeClass key, Option<ByteBuffer> optionalValue);

        void createItem(ByteBuffer secret) throws KeychainException;

        void visitItems(Visitor visitor) throws KeychainException;
    }

    private final class GetKeyAction implements Action, Visitor {

        Option<P> param = Option.none();

        @Override
        public void run(Controller controller) throws KeychainException {
            controller.visitItems(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void visit(final Item item) throws KeychainException {
            param = (Option<P>) deserialize(Option.apply(item.getAttribute(GENERIC)));
            if (param.isEmpty())
                param = Option.some(newKey());
            for (final P p : param) {
                assert null == p.getSecret();
                final ByteBuffer secret = item.getSecret();
                try {
                    p.setSecret(secret);
                } finally {
                    fill(secret, (byte) 0);
                }
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

        private final Option<P> optionalParam;

        public SetKeyAction(final Option<P> optionalParam) {
            this.optionalParam = optionalParam;
        }

        @Override
        public void run(final Controller controller) throws KeychainException {
            for (final P param : optionalParam) {
                for (final ByteBuffer newSecret : Option.apply(param.getSecret())) {
                    try {
                        final Option<ByteBuffer> newXml = serialize(optionalParam);
                        @SuppressWarnings("unchecked")
                        final Option<P> newParam = (Option<P>) deserialize(newXml); // rip off transient fields

                        class UpdateVisitor implements Visitor {
                            @Override
                            public void visit(final Item item) throws KeychainException {
                                {
                                    final ByteBuffer oldSecret =
                                            item.getSecret();
                                    if (!newSecret.equals(oldSecret))
                                        item.setSecret(newSecret);
                                }
                                {
                                    final Option<ByteBuffer> oldXml = Option.apply(item.getAttribute(GENERIC));
                                    @SuppressWarnings("unchecked")
                                    final Option<P> oldParam = (Option<P>) deserialize(oldXml);
                                    if (!newParam.equals(oldParam))
                                        item.setAttribute(GENERIC, newXml.get());
                                }
                            }
                        }

                        try {
                            controller.setAttribute(GENERIC, newXml);
                            controller.createItem(newSecret);
                        } catch (final DuplicateItemException ex) {
                            controller.setAttribute(GENERIC, Option.<ByteBuffer>none());
                            controller.visitItems(new UpdateVisitor());
                        }
                    } finally {
                        fill(newSecret, (byte) 0);
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
