/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.def;

import java.net.URI;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.key.spec.AbstractKeyManager;
import net.java.truevfs.key.spec.KeyProvider;

/**
 * This key manager fails to resolve any secret keys.
 *
 * @since  TrueVFS 0.9.4
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class DefaultKeyManager extends AbstractKeyManager<Object> {

    @Override
    public KeyProvider<Object> access(URI resource) {
        return new DefaultKeyProvider();
    }

    @Override
    public void move(URI oldResource, URI newResource) { }

    @Override
    public void delete(URI resource) { }

    @Override
    public void release(URI resource) { }
}
