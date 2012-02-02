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

/**
 * An entry which provides output sockets.
 *
 * @see     InputEntry
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface OutputEntry<E extends OutputEntry<E>> extends Entry {

    /**
     * Returns an output socket for writing this entry.
     * The method {@link InputSocket#getLocalTarget()} of the returned socket
     * must return this entry.
     *
     * @return An output socket for writing this entry.
     */
    // TODO: Declare to return OutputSocket<? extends E>
    OutputSocket<E> getOutputSocket();
}
