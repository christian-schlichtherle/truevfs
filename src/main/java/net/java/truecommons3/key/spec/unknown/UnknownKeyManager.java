/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.spec.unknown;

import java.net.URI;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons3.key.spec.AbstractKeyManager;
import net.java.truecommons3.key.spec.KeyManager;
import net.java.truecommons3.key.spec.KeyProvider;

/**
 * This key manager fails to resolve any keys.
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class UnknownKeyManager extends AbstractKeyManager<Object> {

    /** The singleton instance of this class. */
    public static final KeyManager<Object> SINGLETON = new UnknownKeyManager();

    private UnknownKeyManager() { }

    @Override
    public KeyProvider<Object> provider(URI uri) {
        return UnknownKeyProvider.SINGLETON;
    }

    @Override
    public void release(URI uri) { }

    @Override
    public void link(URI originUri, URI targetUri) { }

    @Override
    public void unlink(URI uri) { }
}
