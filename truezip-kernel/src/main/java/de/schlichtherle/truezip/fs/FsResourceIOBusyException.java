/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.io.FileBusyException;
import net.jcip.annotations.ThreadSafe;

/**
 * @since   TrueZIP 7.5
 * @see     FsResourceController
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public abstract class FsResourceIOBusyException extends FileBusyException {
    private static final long serialVersionUID = 1L;

    final Object[] args;

    FsResourceIOBusyException(final String message, final Object... args) {
        super(message);
        this.args = args;
    }

    @Override
    public String getMessage() {
        return String.format(super.getMessage(), args);
    }
}
