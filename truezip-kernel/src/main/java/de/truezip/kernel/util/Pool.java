/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.util;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;

/**
 * An interface for pooling strategies.
 * <p>
 * Implementations must be thread-safe.
 * However, this does not necessarily apply to the implementation of its
 * managed resources.
 *
 * @param  <R> The type of the resources managed by this pool.
 * @param  <X> The type of the exceptions thrown by this pool.
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
     * @throws X if allocating the resource failed for any reason.
     */
    @CreatesObligation
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
     * @throws X if releasing the resource failed for any other reason.
     */
    @DischargesObligation
    void release(R resource) throws X;
}
