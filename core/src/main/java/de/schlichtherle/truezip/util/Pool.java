/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.util;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * An interface for pooling strategies.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface Pool<R, E extends Exception> {

    /**
     * Allocates a resource from this pool.
     *
     * @return A resource.
     * @throws Exception if allocating the resource failed for any reason.
     */
    @NonNull
    public R allocate() throws E;

    /**
     * Releases a previously allocated resource to this pool.
     *
     * @param  resource a resource.
     * @throws IllegalArgumentException if the given resource has not been
     *         allocated by this pool.
     * @throws Exception if releasing the resource failed for any other reason.
     */
    public void release(@NonNull R resource) throws E;
}
