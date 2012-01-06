/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.EntryContainer;
import de.schlichtherle.truezip.entry.Entry;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A service for input sockets.
 *
 * @param   <E> The type of the entries.
 * @see     OutputService
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public interface InputService<E extends Entry> extends EntryContainer<E> {

    /**
     * Returns an input socket for reading from the entry with the given name.
     *
     * @param  name an {@link Entry#getName() entry name}.
     * @return An input socket for reading from the entry with the given name.
     */
    InputSocket<? extends E> getInputSocket(String name);
}
