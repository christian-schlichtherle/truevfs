/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.file;

import global.namespace.service.wight.annotation.ServiceImplementation;
import net.java.truecommons.cio.IoBufferPool;
import net.java.truevfs.kernel.spec.spi.IoBufferPoolFactory;

/**
 * Creates {@linkplain FileBufferPool temp file based I/O buffer pools}.
 *
 * @author Christian Schlichtherle
 */
@ServiceImplementation(priority = -100)
public final class FileBufferPoolFactory implements IoBufferPoolFactory {

    @Override
    public IoBufferPool get() {
        return new FileBufferPool();
    }
}
