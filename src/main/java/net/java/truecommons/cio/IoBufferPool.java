/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.cio;

import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.shed.Pool;
import net.java.truecommons.shed.UniqueObject;

/**
 * An abstract pool for allocating I/O buffers, which can get used as a
 * volatile storage for bulk I/O.
 * Typical implementations may use temporary files for big data or byte arrays
 * for small data.
 * <p>
 * Subclasses should be thread-safe.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class IoBufferPool
extends UniqueObject implements Pool<IoBuffer, IOException> {

    @Override public final void release(IoBuffer buffer) throws IOException {
        buffer.release();
    }
}
