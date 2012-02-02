/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.io;

import javax.annotation.CheckForNull;
import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Indicates that an input resource (input stream etc.) for an entry has been
 * forced to close.
 *
 * @see     OutputClosedException
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class InputClosedException extends IOException {
    private static final long serialVersionUID = 4563928734723923649L;

    public InputClosedException() {
        super("Input resource has been closed!");
    }

    public InputClosedException(@CheckForNull Throwable cause) {
        super("Input resource has been closed!", cause);
    }
}
