/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.cio;

import java.io.IOException;
import java.util.Objects;

/**
 * An abstract decorator for an I/O buffer pool.
 *
 * @author Christian Schlichtherle
 */
public abstract class DecoratingIoBufferPool implements IoBufferPool {

    protected IoBufferPool pool;

    protected DecoratingIoBufferPool() {
    }

    protected DecoratingIoBufferPool(final IoBufferPool pool) {
        this.pool = Objects.requireNonNull(pool);
    }

    @Override
    public IoBuffer allocate() throws IOException {
        return pool.allocate();
    }
}
