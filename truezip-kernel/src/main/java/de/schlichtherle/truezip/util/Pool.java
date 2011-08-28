/*
 * Copyright (C) 2011 Schlichtherle IT Services
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

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * An interface for pooling strategies.
 * <p>
 * Implementations must be thread-safe.
 * However, this does not necessarily apply to the implementation of its
 * managed resources.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public interface Pool<R, E extends Exception> {

    /**
     * Allocates a resource from this pool.
     * <p>
     * Mind that a pool implementation should not hold references to its
     * allocated resources because this could cause a memory leak.
     *
     * @return A resource.
     * @throws Exception if allocating the resource failed for any reason.
     */
    R allocate() throws E;

    /**
     * Releases a previously allocated resource to this pool.
     * Implementations may throw an {@link IllegalArgumentException} or an
     * {@link IllegalStateException} upon the conditions explained below.
     *
     * @param  resource a resource.
     * @throws RuntimeException if the given resource has not been allocated
     *         by this pool or has already been released to this pool and the
     *         implementation cannot tolerate this.
     * @throws Exception if releasing the resource failed for any other reason.
     */
    void release(R resource) throws E;

    /**
     * This interface is designed to be used with Pools which enable their
     * resources to release itself.
     * TODO for TrueZIP 8: This should be named "Resource".
     */
    interface Releasable<E extends Exception> {

        /**
         * Releases this resource to its pool.
         * Implementations may throw an {@link IllegalStateException} upon the
         * conditions explained below.
         *
         * @throws IllegalStateException if this resource has already been
         *         released to its pool and the implementation cannot tolerate
         *         this.
         */
        void release() throws E;
    }
}
