/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.io;

import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Indicates that an I/O operation cannot succeed because some file system
 * resource is busy.
 * Sub classes should be more specific about the operation and the busy
 * resource.
 * <p>
 * This exception should be recoverable, meaning it should be possible to
 * successfully retry the operation as soon as the resource is not busy anymore
 * and no other exceptional conditions apply.
 * 
 * @since  TrueZIP 7.5
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class BusyIOException extends IOException {
    private static final long serialVersionUID = 1L;

    final Object[] args;

    protected BusyIOException(final String message, final Object... args) {
        super(message);
        this.args = args;
    }

    @Override
    public String getMessage() {
        return String.format(super.getMessage(), args);
    }
}
