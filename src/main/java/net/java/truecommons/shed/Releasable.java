/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.shed;

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
     * @throws X if releasing the resource fails for any reason.
     */
    void release() throws X;
}
