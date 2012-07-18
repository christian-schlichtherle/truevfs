/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.file;

import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.spec.cio.IoBuffer;
import net.truevfs.kernel.spec.cio.IoBufferPool;
import net.truevfs.kernel.spec.spi.IoBufferPoolFactory;

/**
 * Creates {@linkplain TempFilePool temp file based I/O buffer pools}.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class TempFilePoolFactory extends IoBufferPoolFactory {
    @Override
    public IoBufferPool<? extends IoBuffer<?>> get() {
        return new TempFilePool();
    }
}
