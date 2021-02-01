/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.tar.gzip;

import java.util.zip.Deflater;
import global.namespace.truevfs.kernel.spec.FsTestConfig;
import global.namespace.truevfs.comp.cio.IoBufferPool;

/**
 * Extends its super class to configure it for minimizing heap usage.
 * 
 * @author Christian Schlichtherle
 */
public final class TestTarGZipDriver extends TarGZipDriver {
    @Override
    public IoBufferPool getPool() {
        return FsTestConfig.get().getPool();
    }

    @Override
    public int getLevel() {
        return Deflater.BEST_SPEED;
    }
}
