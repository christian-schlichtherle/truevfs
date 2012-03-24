/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.entry;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

/**
 * A service for writing entries to this container.
 * <p>
 * All methods of this interface must reflect all entries, including those
 * which have just been partially written yet, i.e. which have not already
 * received a call to their {@code close()} method.
 *
 * @param   <E> the type of the entries.
 * @see     InputService
 * @author  Christian Schlichtherle
 */
// TODO: Consider renaming to OutputArchive
@CleanupObligation
public interface OutputService<E extends Entry>
extends Closeable, EntryContainer<E> {

    /**
     * {@inheritDoc}
     * <p>
     * The iterator returned by this method must be unmodifiable.
     */
    @Override
    Iterator<E> iterator();

    /**
     * Returns an output socket for writing to the given entry.
     *
     * @param  entry the entry, which will be the
     *         {@link OutputSocket#getLocalTarget local target} of the returned
     *         output socket.
     * @return An output socket for writing to the given entry.
     */
    // TODO: This should return OutputSocket<E>.
    OutputSocket<? extends E> getOutputSocket(E entry);

    @Override
    @DischargesObligation
    void close() throws IOException;
}
