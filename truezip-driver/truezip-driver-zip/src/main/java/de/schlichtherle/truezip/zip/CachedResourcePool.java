/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.zip;

import de.schlichtherle.truezip.util.Pool;
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
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
abstract class CachedResourcePool<R> implements Pool<R, RuntimeException> {

    private final Set<R> allocated = new HashSet<R>();
    private final Map<R, Reference<R>> released = new HashMap<R, Reference<R>>();

    /**
     * Returns a new resource.
     * 
     * @return A new resource.
     */
    protected abstract R newResource();

    @Override
    public R allocate() {
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
        if (null == resource)
            resource = newResource();
        synchronized (this) {
            allocated.add(resource);
        }
        return resource;
    }

    @Override
    public synchronized void release(R resource) {
        released.put(resource, new SoftReference<R>(resource));
        allocated.remove(resource);
    }
}
