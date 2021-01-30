/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.shed;

/**
 * An interface for pooling strategies.
 * <p>
 * Implementations must be thread-safe.
 * However, this does not necessarily apply to the implementation of its
 * managed resources.
 *
 * @param  <R> the type of the resources managed by this pool.
 * @param  <X> the type of the exceptions thrown by this pool.
 * @author Christian Schlichtherle
 */
public interface Pool<R, X extends Exception> {

    /**
     * Allocates a resource from this pool.
     * <p>
     * Mind that a pool implementation should not hold references to its
     * allocated resources because this could cause a memory leak.
     *
     * @return A resource.
     * @throws X if allocating the resource fails for any reason.
     */
    R allocate() throws X;

    /**
     * Releases a previously allocated resource to this pool.
     *
     * @param  resource a resource.
     * @throws IllegalArgumentException if the given resource has not been
     *         allocated by this pool and the implementation cannot tolerate
     *         this.
     * @throws X if releasing the resource fails for any other reason.
     */
    void release(R resource) throws X;
}
