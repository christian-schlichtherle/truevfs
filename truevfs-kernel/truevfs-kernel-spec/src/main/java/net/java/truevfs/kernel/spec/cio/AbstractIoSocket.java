/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.cio;

import java.io.IOException;
import javax.annotation.concurrent.Immutable;

/**
 * Abstract base class for I/O sockets.
 * 
 * @param  <E> the type of the {@linkplain #target() target entry} for I/O
 *         operations.
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class AbstractIoSocket<E extends Entry>
implements IoSocket<E> {

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        Object target;
        try {
            target = target();
        } catch (final IOException ex) {
            target = ex;
        }
        return String.format("%s[target=%s]", getClass().getName(), target);
    }
}
