/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.spi;

import de.schlichtherle.truezip.fs.FsManagerProvider;
import de.schlichtherle.truezip.fs.sl.FsManagerLocator;

/**
 * An abstract locatable service for a file system manager.
 * Implementations of this abstract class are subject to service location
 * by the class {@link FsManagerLocator}.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class FsManagerService implements FsManagerProvider {

    /**
     * Returns a priority to help the file system manager service locator.
     * The greater number wins!
     * 
     * @return {@code 0}, as by the implementation in the class
     *         {@link FsManagerService}.
     */
    public int getPriority() {
        return 0;
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[priority=%d]",
                getClass().getName(),
                getPriority());
    }
}
