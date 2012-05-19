/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

/**
 * A service for reading entries from this container.
 *
 * @param  <E> the type of the entries in this container.
 * @see    OutputService
 * @author Christian Schlichtherle
 */
@CleanupObligation
public interface InputService<E extends Entry>
extends Closeable, Container<E> {

    /**
     * {@inheritDoc}
     * <p>
     * The iterator returned by this method must be unmodifiable.
     */
    @Override
    Iterator<E> iterator();

    /**
     * Returns an input socket for reading from the entry with the given name.
     *
     * @param  name an {@link Entry#getName() entry name}.
     * @return An input socket for reading from the entry with the given name.
     */
    InputSocket<E> input(String name);

    @Override
    @DischargesObligation
    void close() throws IOException;
}
