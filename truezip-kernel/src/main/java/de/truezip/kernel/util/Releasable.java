/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.util;

/**
 * A resource which can release itself to its associated {@link Pool}.
 *
 * @param  <X> The type of the exceptions thrown by this releasable.
 * @author Christian Schlichtherle
 */
public interface Releasable<X extends Exception> {

    /**
     * Releases this resource to its pool.
     *
     * @throws IllegalStateException if this resource has already been
     *         released to its pool and the implementation cannot tolerate
     *         this.
     * @throws X if releasing the resource failed for any other reason.
     */
    void release() throws X;
}