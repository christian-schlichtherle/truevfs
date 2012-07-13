/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.odf;

import net.truevfs.driver.zip.core.AbstractZipDriverEntry;
import net.truevfs.kernel.spec.FsArchiveDriverTestSuite;
import net.truevfs.kernel.spec.cio.IoBufferPool;

/**
 * @author Christian Schlichtherle
 */
public final class OdfDriverTest
extends FsArchiveDriverTestSuite<AbstractZipDriverEntry, OdfDriver> {

    @Override
    protected OdfDriver newArchiveDriver() {
        return new OdfDriver() {
            @Override
            public IoBufferPool<?> getIoBufferPool() {
                return getTestConfig().getIoBufferPool();
            }
        };
    }

    @Override
    protected String getUnencodableName() {
        return null;
    }
}
