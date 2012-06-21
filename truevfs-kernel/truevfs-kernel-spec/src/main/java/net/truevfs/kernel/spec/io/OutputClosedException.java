/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.io;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Indicates that an output resource (stream, channel etc.) has been closed.
 *
 * @see    InputClosedException
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class OutputClosedException extends ClosedException {
    private static final long serialVersionUID = 4563928734723923649L;
}
