/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.cio;

import java.io.IOException;

/**
 * An abstract I/O buffer pool.
 * 
 * @author Christian Schlichtherle
 */
public abstract class AbstractIoBufferPool implements IoBufferPool {

    @Override
    public final void release(IoBuffer buffer) throws IOException {
        buffer.release();
    }
}
