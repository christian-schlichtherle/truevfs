/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import net.jcip.annotations.ThreadSafe;

/**
 * @since   TrueZIP 7.5
 * @see     FsResourceController
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public final class FsThreadsIOBusyException extends FsResourceIOBusyException {
    private static final long serialVersionUID = 1L;

    FsThreadsIOBusyException(final int total, final int local) {
        super("Total (thread local) number of open I/O streams: %d (%d)", total, local);
    }
}
