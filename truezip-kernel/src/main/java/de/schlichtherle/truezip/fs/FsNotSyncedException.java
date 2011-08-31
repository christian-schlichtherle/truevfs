/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.ThreadSafe;

/**
 * Indicates that a file system should get
 * {@link FsController#sync(BitField, ExceptionHandler) synced} before a
 * file system operation can commence.
 * This exception type is reserved for use within the TrueZIP Kernel in order
 * to catch it and sync the file system.
 * Unless there is a bug, an exception of this type <em>never</em> pops up to
 * a TrueZIP application.
 *
 * @since   TrueZIP 7.3
 * @see     FsSyncController
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public final class FsNotSyncedException extends FsException {
    private static final long serialVersionUID = 2345952581284762637L;

    public FsNotSyncedException() {
    }
}
