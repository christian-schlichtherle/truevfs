/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.spec.prompting;

import net.java.truecommons.shed.Option;
import net.java.truecommons.shed.UniqueObject;

import javax.annotation.concurrent.ThreadSafe;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static net.java.truecommons.shed.Option.apply;

/**
 * @param  <K> the type of the prompting keys.
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@SuppressWarnings("LoopStatementThatDoesntLoop")
@ThreadSafe
final class SharedKeyManager<K extends PromptingKey<K>>
extends UniqueObject {

    private final Map<URI, SharedKeyProvider<K>> providers = new HashMap<>();

    private Option<SharedKeyProvider<K>> get(URI uri) {
        return apply(providers.get(uri));
    }

    private Option<SharedKeyProvider<K>> put(URI uri, SharedKeyProvider<K> p) {
        return apply(providers.put(uri, p));
    }

    private Option<SharedKeyProvider<K>> remove(URI uri) {
        return apply(providers.remove(uri));
    }

    synchronized SharedKeyProvider<K> provider(final URI uri) {
        for (final SharedKeyProvider<K> p : get(uri))
            return p;
        final SharedKeyProvider<K> p = new SharedKeyProvider<K>();
        put(uri, p);
        p.link();
        return p;
    }

    synchronized void release(final URI uri) {
        for (final SharedKeyProvider<K> p : get(uri))
            p.release();
    }

    synchronized void link(final URI originUri, final URI targetUri) {
        for (final SharedKeyProvider<K> originProvider : get(originUri)) {
            for (final SharedKeyProvider<K> targetProvider : put(targetUri, originProvider)) {
                if (targetProvider == originProvider)
                    return;
                targetProvider.unlink();
            }
            originProvider.link();
        }
    }

    synchronized void unlink(final URI uri) {
        for (final SharedKeyProvider<K> p : remove(uri))
            p.unlink();
    }
}
