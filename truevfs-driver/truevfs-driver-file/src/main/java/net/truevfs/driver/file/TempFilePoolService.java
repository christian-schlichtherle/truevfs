/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.file;

import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.cio.IoPool;
import net.truevfs.kernel.spi.IoPoolService;

/**
 * Provides {@link TempFilePool#INSTANCE}.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class TempFilePoolService extends IoPoolService {

    @Override
    public IoPool<?> getIoPool() {
        return TempFilePool.INSTANCE;
    }

    /** @return -100 */
    @Override
    public int getPriority() {
        return -100;
    }
}
