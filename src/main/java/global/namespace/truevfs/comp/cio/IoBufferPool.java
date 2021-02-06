/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.cio;

import global.namespace.truevfs.comp.util.Pool;

import java.io.IOException;

/**
 * An abstract pool for allocating I/O buffers, which can get used as a volatile storage for bulk I/O.
 * Typical implementations may use temporary files for big data or byte arrays for small data.
 * <p>
 * Subclasses should be thread-safe.
 *
 * @author Christian Schlichtherle
 */
public interface IoBufferPool extends Pool<IoBuffer, IOException> {

    @Override
    default void release(IoBuffer buffer) throws IOException {
        buffer.release();
    }
}
