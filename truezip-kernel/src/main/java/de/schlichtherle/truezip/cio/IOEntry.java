/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.cio;

/**
 * An entry which provides I/O services.
 *
 * @param  <E> the type of this I/O entry.
 * @author Christian Schlichtherle
 */
public interface IOEntry<E extends IOEntry<E>> extends Entry {

    /**
     * Returns an input socket for reading this entry.
     * The method {@link InputSocket#getLocalTarget()} of the returned socket
     * must return this entry.
     *
     * @return An input socket for reading this entry.
     */
    InputSocket<E> getInputSocket();

    /**
     * Returns an output socket for writing this entry.
     * The method {@link InputSocket#getLocalTarget()} of the returned socket
     * must return this entry.
     *
     * @return An output socket for writing this entry.
     */
    OutputSocket<E> getOutputSocket();
}