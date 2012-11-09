/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.prompting;

import java.net.URI;
import java.util.Objects;
import java.util.Random;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.key.spec.UnknownKeyException;
import net.java.truevfs.key.spec.prompting.PromptingKeyProvider.Controller;
import net.java.truevfs.key.spec.prompting.PromptingKeyProvider.View;
import static net.java.truevfs.key.spec.prompting.TestView.Action.ENTER;

/**
 * A view implementation which uses its properties for providing a key whenever
 * the user is prompted.
 *
 * @param  <K> The type of the safe key.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class TestView<K extends PromptingKey<K>> implements View<K> {

    private volatile @CheckForNull URI resource;
    private volatile @CheckForNull K key;
    private volatile Action action = ENTER;

    public Action getAction() { return action; }

    public void setAction(final Action action) {
        this.action = Objects.requireNonNull(action);
    }

    public @CheckForNull URI getResource() { return resource; }

    public void setResource(final @CheckForNull URI resource) {
        this.resource = resource;
    }

    public @CheckForNull K getKey() { return key; }

    public void setKey(final @CheckForNull K key) { this.key = key; }

    @Override
    public synchronized void
    promptKeyForWriting(Controller<K> controller)
    throws UnknownKeyException {
        final URI resource = getResource();
        if (null != resource && !resource.equals(controller.getResource()))
            throw new IllegalArgumentException();
        controller.getKey();
        action.promptKeyForWriting(controller, key);
    }

    @Override
    public synchronized void
    promptKeyForReading(Controller<K> controller, boolean invalid)
    throws UnknownKeyException {
        final URI resource = getResource();
        if (null != resource && !resource.equals(controller.getResource()))
            throw new IllegalArgumentException();
        try {
            controller.getKey();
            throw new IllegalArgumentException();
        } catch (IllegalStateException expected) {
        }
        action.promptKeyForReading(controller, key);
    }

    @SuppressWarnings("PublicInnerClass")
    public enum Action {
        ENTER {
            @Override
            <K extends PromptingKey<K>> void
            promptKeyForWriting(
                    final Controller<? super K> controller,
                    final @CheckForNull K key)
            throws UnknownKeyException {
                controller.setKey(null);
                controller.setKey(null != key ? key.clone() : null);
            }

            @Override
            <K extends PromptingKey<K>> void
            promptKeyForReading(
                    final Controller<? super K> controller,
                    final @CheckForNull K key)
            throws UnknownKeyException {
                controller.setKey(null);
                controller.setKey(null == key ? null : key.clone());
            }
        },

        CANCEL {
            private final Random rnd = new Random();

            @Override
            <K extends PromptingKey<K>> void
            promptKeyForWriting(
                    final Controller<? super K> controller,
                    final @CheckForNull K key)
            throws UnknownKeyException {
                if (rnd.nextBoolean()) {
                    throw new KeyPromptingCancelledException();
                }  else {
                    controller.setKey(null);
                }
            }

            @Override
            <K extends PromptingKey<K>> void
            promptKeyForReading(
                    final Controller<? super K> controller,
                    final @CheckForNull K key)
            throws UnknownKeyException {
                if (rnd.nextBoolean()) {
                    throw new KeyPromptingCancelledException();
                } else {
                    controller.setKey(null);
                }
            }
        },

        IGNORE {
            @Override
            <K extends PromptingKey<K>> void
            promptKeyForWriting(
                    Controller<? super K> controller,
                    @CheckForNull K key)
            throws UnknownKeyException {
            }

            @Override
            <K extends PromptingKey<K>> void
            promptKeyForReading(
                    Controller<? super K> controller,
                    @CheckForNull K key)
            throws UnknownKeyException {
            }
        };

        abstract <K extends PromptingKey<K>> void
        promptKeyForWriting(
                Controller<? super K> controller,
                @CheckForNull K key)
        throws UnknownKeyException;

        abstract <K extends PromptingKey<K>> void
        promptKeyForReading(
                Controller<? super K> controller,
                @CheckForNull K key)
        throws UnknownKeyException;
    } // enum Action
}