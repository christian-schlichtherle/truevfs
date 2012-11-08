/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.prompting;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.key.spec.AbstractKeyManagerMap;
import net.java.truevfs.key.spec.KeyManager;
import net.java.truevfs.key.spec.prompting.PromptingKeyProvider.View;

/**
 * Implements a map for a single prompting key manager which will use the
 * prompting key provider view given to the
 * {@linkplain #PromptingKeyManagerMap constructor}.
 * This class is provided for convenience and may be used to ease the
 * implementation of a custom view for key prompting.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class PromptingKeyManagerMap
extends AbstractKeyManagerMap {

    private final Map<Class<?>, KeyManager<?>> managers;

    /**
     * Constructs a new prompting key manager service using the given view.
     *
     * @param <K> the type of the prompting keys.
     * @param clazz the class of the prompting keys.
     * @param view the view for the prompting key provider.
     */
    public <K extends PromptingKey<K, ?>> PromptingKeyManagerMap(
            final Class<K> clazz,
            final View<K> view) {
        final Map<Class<?>, KeyManager<?>> map = new HashMap<>(2);
        map.put(clazz, new PromptingKeyManager<>(view));
        managers = Collections.unmodifiableMap(map);
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<Class<?>, KeyManager<?>> get() { return managers; }
}
