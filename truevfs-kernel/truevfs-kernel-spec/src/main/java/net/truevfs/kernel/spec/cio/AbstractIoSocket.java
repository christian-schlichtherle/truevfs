/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.cio;

import java.io.IOException;
import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.spec.util.UniqueObject;

/**
 * Abstract base class for I/O sockets.
 * 
 * @param  <T> the type of the {@linkplain #target() target} entry for I/O
 *         operations.
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class AbstractIoSocket<T extends Entry>
extends UniqueObject implements IoSocket<T> {

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
