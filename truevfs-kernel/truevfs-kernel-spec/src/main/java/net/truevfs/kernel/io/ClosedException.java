/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.io;

import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Indicates that a resource (stream, channel, etc.) has been closed.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class ClosedException extends IOException {
    private static final long serialVersionUID = 7502497562473974639L;
}