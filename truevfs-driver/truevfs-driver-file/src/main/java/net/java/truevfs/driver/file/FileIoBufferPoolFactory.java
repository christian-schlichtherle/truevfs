/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.file;

import javax.annotation.concurrent.Immutable;
import net.java.truevfs.kernel.spec.cio.IoBufferPool;
import net.java.truevfs.kernel.spec.spi.IoBufferPoolFactory;

/**
 * Creates {@linkplain FileIoBufferPool temp file based I/O buffer pools}.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class FileIoBufferPoolFactory extends IoBufferPoolFactory {
    @Override
    public IoBufferPool get() {
        return new FileIoBufferPool();
    }
}
