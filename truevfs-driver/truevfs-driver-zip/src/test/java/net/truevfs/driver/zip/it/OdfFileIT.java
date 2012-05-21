/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.it;

import net.truevfs.access.TFileITSuite;
import net.truevfs.driver.zip.OdfDriver;
import net.truevfs.kernel.cio.IoPool;

/**
 * @author  Christian Schlichtherle
 */
public final class OdfFileIT extends TFileITSuite<OdfDriver> {

    @Override
    protected String getExtensionList() {
        return "odf";
    }

    @Override
    protected OdfDriver newArchiveDriver() {
        return new OdfDriver() {
            @Override
            public IoPool<?> getIoPool() {
                return getTestConfig().getIoPoolProvider().getIoPool();
            }
        };
    }
}