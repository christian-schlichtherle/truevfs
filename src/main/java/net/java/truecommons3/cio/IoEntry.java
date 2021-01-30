/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.cio;

/**
 * An entry which provides I/O services.
 *
 * @param  <This> the type of this I/O entry.
 * @author Christian Schlichtherle
 */
public interface IoEntry<This extends IoEntry<This>> extends Entry {

    /**
     * Returns an input socket for reading this entry.
     * The method {@link InputSocket#target()} of the returned socket
     * must return this entry.
     *
     * @return An input socket for reading this entry.
     */
    InputSocket<? extends This> input();

    /**
     * Returns an output socket for writing this entry.
     * The method {@link OutputSocket#target()} of the returned socket
     * must return this entry.
     *
     * @return An output socket for writing this entry.
     */
    OutputSocket<? extends This> output();
}
