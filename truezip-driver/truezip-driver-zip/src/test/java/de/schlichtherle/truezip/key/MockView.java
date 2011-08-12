/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.key;

import static de.schlichtherle.truezip.key.MockView.Action.*;
import de.schlichtherle.truezip.key.PromptingKeyProvider.Controller;
import de.schlichtherle.truezip.key.PromptingKeyProvider.View;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.net.URI;
import java.util.Random;
import net.jcip.annotations.ThreadSafe;

/**
 * A view implementation which uses its properties for providing a key whenever
 * the user is prompted.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public final class MockView<K extends SafeKey<K>> implements View<K> {
    private volatile @CheckForNull URI resource;
    private volatile @CheckForNull K key;
    private volatile Action action = ENTER;
    private volatile boolean changeRequested;

    public Action getAction() {
        return action;
    }

    public void setAction(final Action action) {
        if (null == action)
            throw new NullPointerException();
        this.action = action;
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
