/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.io;

import java.io.IOException;
import net.jcip.annotations.ThreadSafe;

/**
 * Indicates that an output resource (output stream etc.) for an entry has been
 * forced to close.
 *
 * @see     InputClosedException
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class OutputClosedException extends IOException {
    private static final long serialVersionUID = 4563928734723923649L;

    public OutputClosedException() {
        super("Output resource has been closed!");
    }

    public OutputClosedException(Throwable cause) {
        super("Output resource has been closed!", cause);
    }
}
