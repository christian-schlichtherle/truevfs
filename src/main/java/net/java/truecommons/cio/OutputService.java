/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.cio;

import java.io.Closeable;

/**
 * A service for writing entries to this container.
 * <p>
 * All methods of this interface must reflect all entries, including those
 * which have just been partially written yet, i.e. which have not already
 * received a call to their {@code close()} method.
 *
 * @param   <E> the type of the entries in this container.
 * @see     InputService
 * @author  Christian Schlichtherle
 */
public interface OutputService<E extends Entry>
extends Closeable, Container<E> {

    /**
     * Returns an output socket for writing to the given entry.
     *
     * @param  entry the entry, which must be the
     *         {@linkplain OutputSocket#target local target} of the
     *         returned output socket.
     * @return An output socket for writing to the given entry.
     */
    OutputSocket<E> output(E entry);
}
