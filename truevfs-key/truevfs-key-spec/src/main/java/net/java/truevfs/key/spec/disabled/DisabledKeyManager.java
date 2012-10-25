/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.disabled;

import java.net.URI;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.key.spec.AbstractKeyManager;
import net.java.truevfs.key.spec.KeyManager;
import net.java.truevfs.key.spec.KeyProvider;

/**
 * This key manager fails to resolve any secret keys.
 *
 * @since  TrueVFS 0.9.4
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class DisabledKeyManager extends AbstractKeyManager<Object> {

    /** The singleton instance of this class. */
    public static final KeyManager<Object> SINGLETON = new DisabledKeyManager();

    private DisabledKeyManager() { }

    @Override
    public KeyProvider<Object> access(URI resource) {
        return DisabledKeyProvider.SINGLETON;
    }

    @Override
    public void move(URI oldResource, URI newResource) { }

    @Override
    public void delete(URI resource) { }

    @Override
    public void release(URI resource) { }
}
