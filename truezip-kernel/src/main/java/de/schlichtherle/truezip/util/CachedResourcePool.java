/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.util;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.ThreadSafe;

/**
 * A memory sensitive pool of cached resources.
 *
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public abstract class CachedResourcePool<R, E extends Exception>
implements Pool<R, E> {

    private final Set<R> allocated = new HashSet<R>();
    private final Map<R, Reference<R>> released = new HashMap<R, Reference<R>>();

    /**
     * Returns a new resource.
     * 
     * @return A new resource.
     */
    protected abstract R newResource() throws E;

    /**
     * Resets the given resource which is allocated from the pool before it
     * gets returned to the client.
     * <p>
     * The implementation in the class {@link CachedResourcePool} does nothing.
     */
    protected void reset(R resource) throws E {
    }

    @Override
    public R allocate() throws E {
        R resource = null;
        synchronized (this) {
            final Iterator<Reference<R>> i = released.values().iterator();
            while (i.hasNext()) {
                resource = i.next().get();
                i.remove();
                if (null != resource)
                    break;
            }
        }
        // Creating a new resource may be costly, so we do not want to lock
        // this object while waiting for it.
        // As a downside, this may result in redundant creation of a resource.
        if (null != resource)
            reset(resource);
        else
            resource = newResource();
        synchronized (this) {
            allocated.add(resource);
        }
        return resource;
    }

    @Override
    public synchronized void release(R resource) throws E {
        released.put(resource, new SoftReference<R>(resource));
        allocated.remove(resource);
    }
}
