/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.key;

import de.truezip.key.PromptingKeyProvider.View;
import java.util.Map;
import javax.annotation.concurrent.Immutable;

/**
 * Implements a provider for a single prompting key manager which will use the
 * prompting key provider view given to the
 * {@linkplain #PromptingKeyManagerProvider constructor}.
 * This class is convenient to use if you want to implement a custom view for
 * key prompting.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public final class PromptingKeyManagerProvider
extends AbstractKeyManagerProvider {

    private final Map<Class<?>, KeyManager<?>> managers;

    /**
     * Constructs a new prompting key manager service using the given view.
     * 
     * @param <K> the type of the safe keys.
     * @param clazz the class of the safe keys.
     * @param view the prompting key provider view for the safe keys.
     */
    public <K extends SafeKey<K>> PromptingKeyManagerProvider(
            final Class<K> clazz,
            final View<K> view) {
        this.managers = newMap(new Object[][] {{
            clazz,
            new PromptingKeyManager<>(view)
        }});
    }

    @Override
    public Map<Class<?>, KeyManager<?>> getKeyManagers() {
        return managers;
    }
}
