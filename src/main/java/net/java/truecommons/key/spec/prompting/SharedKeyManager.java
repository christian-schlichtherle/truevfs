/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.spec.prompting;

import net.java.truecommons.shed.UniqueObject;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @param <K> the type of the prompting keys.
 * @author Christian Schlichtherle
 */
final class SharedKeyManager<K extends PromptingKey<K>> extends UniqueObject {

    private final Map<URI, SharedKeyProvider<K>> providers = new HashMap<>();

    private Optional<SharedKeyProvider<K>> get(URI uri) {
        return Optional.ofNullable(providers.get(uri));
    }

    private Optional<SharedKeyProvider<K>> put(URI uri, SharedKeyProvider<K> p) {
        return Optional.ofNullable(providers.put(uri, p));
    }

    private Optional<SharedKeyProvider<K>> remove(URI uri) {
        return Optional.ofNullable(providers.remove(uri));
    }

    synchronized SharedKeyProvider<K> provider(URI uri) {
        return get(uri).orElseGet(() -> {
            final SharedKeyProvider<K> p = new SharedKeyProvider<K>();
            put(uri, p);
            p.link();
            return p;
        });
    }

    synchronized void release(URI uri) {
        get(uri).ifPresent(SharedKeyProvider::release);
    }

    synchronized void link(final URI originUri, final URI targetUri) {
        final Optional<SharedKeyProvider<K>> optOriginProvider = get(originUri);
        if (optOriginProvider.isPresent()) {
            final SharedKeyProvider<K> originProvider = optOriginProvider.get();
            final Optional<SharedKeyProvider<K>> optTargetProvider = put(targetUri, originProvider);
            if (optTargetProvider.isPresent()) {
                final SharedKeyProvider<K> targetProvider = optTargetProvider.get();
                if (targetProvider == originProvider) {
                    return;
                }
                targetProvider.unlink();
            }
            originProvider.link();
        }
    }

    synchronized void unlink(URI uri) {
        remove(uri).ifPresent(SharedKeyProvider::unlink);
    }
}
