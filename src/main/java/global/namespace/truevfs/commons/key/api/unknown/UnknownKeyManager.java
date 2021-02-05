/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.key.api.unknown;

import global.namespace.truevfs.commons.key.api.KeyManager;
import global.namespace.truevfs.commons.key.api.KeyProvider;

import java.net.URI;

/**
 * This key manager fails to resolve any keys.
 *
 * @author Christian Schlichtherle
 */
public final class UnknownKeyManager implements KeyManager<Object> {

    /**
     * The singleton instance of this class.
     */
    public static final KeyManager<Object> SINGLETON = new UnknownKeyManager();

    private UnknownKeyManager() {
    }

    @Override
    public KeyProvider<Object> provider(URI uri) {
        return UnknownKeyProvider.SINGLETON;
    }

    @Override
    public void release(URI uri) {
    }

    @Override
    public void link(URI originUri, URI targetUri) {
    }

    @Override
    public void unlink(URI uri) {
    }
}
