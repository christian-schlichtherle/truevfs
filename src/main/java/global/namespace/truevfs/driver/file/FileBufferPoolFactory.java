/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.file;

import global.namespace.service.wight.annotation.ServiceImplementation;
import global.namespace.truevfs.comp.cio.IoBufferPool;
import global.namespace.truevfs.kernel.spec.spi.IoBufferPoolFactory;

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
