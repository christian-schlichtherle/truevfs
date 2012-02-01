/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import java.util.Iterator;
import net.jcip.annotations.ThreadSafe;

/**
 * An abstract decorator for a file system manager.
 * 
 * @param   <M> The type of the decorated file system manager.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public abstract class FsDecoratingManager<M extends FsManager>
extends FsManager {

    protected final M delegate;

    /**
     * Constructs a new decorating file system manager.
     *
     * @param manager the decorated file system manager.
     */
    protected FsDecoratingManager(final M manager) {
        if (null == manager)
            throw new NullPointerException();
        this.delegate = manager;
    }

    @Override
    public FsController<?>
    getController(FsMountPoint mountPoint, FsCompositeDriver driver) {
        return delegate.getController(mountPoint, driver);
    }

    @Override
    public int getSize() {
        return delegate.getSize();
    }

    @Override
    public Iterator<FsController<?>> iterator() {
        return delegate.iterator();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return new StringBuilder()
                .append(getClass().getName())
                .append("[delegate=")
                .append(delegate)
                .append(']')
                .toString();
    }
}
