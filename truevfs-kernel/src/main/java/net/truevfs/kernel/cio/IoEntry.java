/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio;

/**
 * An entry which provides I/O services.
 *
 * @param  <E> the type of this I/O entry.
 * @author Christian Schlichtherle
 */
public interface IoEntry<E extends IoEntry<E>> extends Entry {

    /**
     * Returns an input socket for reading this entry.
     * The method {@link InputSocket#localTarget()} of the returned socket
     * must return this entry.
     *
     * @return An input socket for reading this entry.
     */
    InputSocket<E> input();

    /**
     * Returns an output socket for writing this entry.
     * The method {@link InputSocket#localTarget()} of the returned socket
     * must return this entry.
     *
     * @return An output socket for writing this entry.
     */
    OutputSocket<E> output();
}