/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.disabled;

import java.net.URI;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.key.spec.AbstractKeyManager;
import net.java.truevfs.key.spec.KeyProvider;

/**
 * @since  TrueVFS 0.9.4
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class DisabledKeyManager extends AbstractKeyManager<Object> {

    @Override
    public KeyProvider<Object> make(URI resource) {
        return new DisabledKeyProvider();
    }

    @Override
    public KeyProvider<Object> get(URI resource) { return null; }

    @Override
    public KeyProvider<Object> move(URI oldResource, URI newResource) {
        return null;
    }

    @Override
    public KeyProvider<Object> delete(URI resource) { return null; }

    @Override
    public void unlock(URI resource) { }
}
