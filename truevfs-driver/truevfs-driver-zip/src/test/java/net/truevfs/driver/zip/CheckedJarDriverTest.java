/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip;

import net.truevfs.kernel.spec.FsArchiveDriverTestSuite;
import net.truevfs.kernel.spec.cio.IoBuffer;
import net.truevfs.kernel.spec.cio.IoPool;

/**
 * @author Christian Schlichtherle
 */
public final class CheckedJarDriverTest
extends FsArchiveDriverTestSuite<ZipDriverEntry, CheckedJarDriver> {

    @Override
    protected CheckedJarDriver newArchiveDriver() {
        return new CheckedJarDriver() {
            @Override
            public IoPool<? extends IoBuffer<?>> getIoPool() {
                return getTestConfig().getIoPoolProvider().getIoPool();
            }
        };
    }

    @Override
    protected String getUnencodableName() {
        return null;
    }
}
