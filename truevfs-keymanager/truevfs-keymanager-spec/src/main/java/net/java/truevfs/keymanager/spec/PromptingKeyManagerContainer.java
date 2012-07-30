/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.keymanager.spec;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.keymanager.spec.PromptingKeyProvider.View;

/**
 * Implements a provider for a single prompting key manager which will use the
 * prompting key provider view given to the
 * {@linkplain #PromptingKeyManagerContainer constructor}.
 * This class is convenient to use if you want to implement a custom view for
 * key prompting.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public final class PromptingKeyManagerContainer
extends AbstractKeyManagerContainer {

    private final Map<Class<?>, KeyManager<?>> managers;

    /**
     * Constructs a new prompting key manager service using the given view.
     * 
     * @param <K> the type of the safe keys.
     * @param clazz the class of the safe keys.
     * @param view the prompting key provider view for the safe keys.
     */
    public <K extends SafeKey<K>> PromptingKeyManagerContainer(
            final Class<K> clazz,
            final View<K> view) {
        final Map<Class<?>, KeyManager<?>> map = new HashMap<>(2);
        map.put(clazz, new PromptingKeyManager<>(view));
        managers = Collections.unmodifiableMap(map);
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<Class<?>, KeyManager<?>> get() {
        return managers;
    }
}
