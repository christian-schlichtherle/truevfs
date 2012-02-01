/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.EntryContainer;

/**
 * A service for output sockets.
 * <p>
 * All methods of this interface must reflect all entries, including those
 * which have only been partially written yet, i.e. which have not already
 * received a call to their {@code close()} method.
 *
 * @param   <E> The type of the entries.
 * @see     InputService
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface OutputService<E extends Entry> extends EntryContainer<E> {

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
}
