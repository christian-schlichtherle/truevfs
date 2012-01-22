/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import net.jcip.annotations.Immutable;

/**
 * @since   TrueZIP 7.5
 * @see     FsResourceController
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public final class FsCurrentThreadIOBusyException extends FsResourceIOBusyException {
    private static final long serialVersionUID = 1L;

    FsCurrentThreadIOBusyException(final int local) {
        super("Number of open I/O streams for the current thread: %d", local);
    }
}
