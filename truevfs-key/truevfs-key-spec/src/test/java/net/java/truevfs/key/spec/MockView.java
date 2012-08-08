/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec;

import net.java.truevfs.key.spec.SafeKey;
import net.java.truevfs.key.spec.UnknownKeyException;
import net.java.truevfs.key.spec.KeyPromptingCancelledException;
import java.net.URI;
import java.util.Objects;
import java.util.Random;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;
import static net.java.truevfs.key.spec.MockView.Action.ENTER;
import net.java.truevfs.key.spec.PromptingKeyProvider.Controller;
import net.java.truevfs.key.spec.PromptingKeyProvider.View;

/**
 * A view implementation which uses its properties for providing a key whenever
 * the user is prompted.
 *
 * @param  <K> The type of the safe key.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class MockView<K extends SafeKey<K>> implements View<K> {
    private volatile @CheckForNull URI resource;
    private volatile @CheckForNull K key;
    private volatile Action action = ENTER;
    private volatile boolean changeRequested;

    public Action getAction() {
        return action;
    }

    public void setAction(final Action action) {
        this.action = Objects.requireNonNull(action);
    }

    public @CheckForNull URI getResource() {
        return resource;
    }

    public void setResource(final @CheckForNull URI resource) {
        this.resource = resource;
    }

    public @CheckForNull K getKey() {
        return key;
    }

    public void setKey(@CheckForNull K key) {
        this.key = key;
    }

    public boolean isChangeRequested() {
        return changeRequested;
    }

    public void setChangeRequested(final boolean changeRequested) {
        this.changeRequested = changeRequested;
    }

    @Override
    public synchronized void
    promptWriteKey(Controller<K> controller)
    throws UnknownKeyException {
        final URI resource = getResource();
        if (null != resource && !resource.equals(controller.getResource()))
            throw new IllegalArgumentException();
        controller.getKey();
        try {
            controller.setChangeRequested(true);
            throw new IllegalArgumentException();
        } catch (IllegalStateException expected) {
        }
        try {
            controller.setChangeRequested(false);
            throw new IllegalArgumentException();
        } catch (IllegalStateException expected) {
        }
        action.promptWriteKey(controller, key);
    }

    @Override
    public synchronized void
    promptReadKey(Controller<K> controller, boolean invalid)
    throws UnknownKeyException {
        final URI resource = getResource();
        if (null != resource && !resource.equals(controller.getResource()))
            throw new IllegalArgumentException();
        try {
            controller.getKey();
            throw new IllegalArgumentException();
        } catch (IllegalStateException expected) {
        }
        action.promptReadKey(controller, key, changeRequested);
    }

    @SuppressWarnings("PublicInnerClass")
    public enum Action {
        ENTER {
            @Override
            <K extends SafeKey<K>> void
            promptWriteKey(Controller<? super K> controller, K key)
            throws UnknownKeyException {
                controller.setKey(null);
                controller.setKey(null != key ? key.clone() : null);
            }

            @Override
            <K extends SafeKey<K>> void
            promptReadKey(Controller<? super K> controller, K key, boolean changeRequested)
            throws UnknownKeyException {
                controller.setKey(null);
                controller.setChangeRequested(false);
                controller.setChangeRequested(true);
                controller.setKey(null != key ? key.clone() : null);
                controller.setChangeRequested(changeRequested);
            }
        },

        CANCEL {
            private final Random rnd = new Random();

            @Override
            <K extends SafeKey<K>> void
            promptWriteKey(Controller<? super K> controller, K key)
            throws UnknownKeyException {
                if (rnd.nextBoolean()) {
                    throw new KeyPromptingCancelledException();
                }  else {
                    controller.setKey(null);
                }
            }

            @Override
            <K extends SafeKey<K>> void
            promptReadKey(Controller<? super K> controller, K key, boolean changeRequested)
            throws UnknownKeyException {
                if (rnd.nextBoolean()) {
                    throw new KeyPromptingCancelledException();
                } else {
                    controller.setChangeRequested(false);
                    controller.setKey(null);
                    controller.setChangeRequested(true);
                }
            }
        },

        IGNORE {
            @Override
            <K extends SafeKey<K>> void
            promptWriteKey(Controller<? super K> controller, K key)
            throws UnknownKeyException {
            }

            @Override
            <K extends SafeKey<K>> void
            promptReadKey(Controller<? super K> controller, K key, boolean changeRequested)
            throws UnknownKeyException {
            }
        };

        abstract <K extends SafeKey<K>> void
        promptWriteKey(Controller<? super K> controller, @CheckForNull K key)
        throws UnknownKeyException;

        abstract <K extends SafeKey<K>> void
        promptReadKey(Controller<? super K> controller, @CheckForNull K key, boolean changeRequested)
        throws UnknownKeyException;
    } // enum Action
}