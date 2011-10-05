/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * An entry which provides input sockets.
 *
 * @see     OutputEntry
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface InputEntry<E extends InputEntry<E>> extends Entry {

    /**
     * Returns an input socket for reading this entry.
     * The method {@link InputSocket#getLocalTarget()} of the returned socket
     * must return this entry.
     *
     * @return An input socket for reading this entry.
     */
    @NonNull InputSocket<E> getInputSocket();
}
