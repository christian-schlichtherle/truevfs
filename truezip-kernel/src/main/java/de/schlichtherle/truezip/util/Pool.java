/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.util;

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
public interface Pool<R, X extends Exception> {

    /**
     * Allocates a resource from this pool.
     * <p>
     * Mind that a pool implementation should not hold references to its
     * allocated resources because this could cause a memory leak.
     *
     * @return A resource.
     * @throws Exception if allocating the resource failed for any reason.
     */
    R allocate() throws X;

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
    void release(R resource) throws X;

    /**
     * This interface is designed to be used with Pools which enable their
     * resources to release itself.
     * TODO for TrueZIP 8: This should be named "Resource".
     */
    interface Releasable<X extends Exception> {

        /**
         * Releases this resource to its pool.
         * Implementations may throw an {@link IllegalStateException} upon the
         * conditions explained below.
         *
         * @throws IllegalStateException if this resource has already been
         *         released to its pool and the implementation cannot tolerate
         *         this.
         */
        void release() throws X;
    }
}
