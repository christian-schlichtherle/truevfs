/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip;

import net.truevfs.driver.zip.core.AbstractZipDriverEntry;
import net.truevfs.kernel.spec.FsArchiveDriverTestSuite;
import net.truevfs.kernel.spec.TestConfig;
import net.truevfs.kernel.spec.cio.IoBuffer;
import net.truevfs.kernel.spec.cio.IoBufferPool;

/**
 * @author Christian Schlichtherle
 */
public final class ZipDriverTest
extends FsArchiveDriverTestSuite<AbstractZipDriverEntry, ZipDriver> {

    @Override
    protected ZipDriver newArchiveDriver() {
        return new ZipDriver() {
            @Override
            public IoBufferPool<? extends IoBuffer<?>> getPool() {
                return TestConfig.get().getPool();
            }
        };
    }

    @Override
    protected String getUnencodableName() {
        return "\u2297";
    }
}
