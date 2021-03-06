/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.odf.it;

import net.java.truevfs.access.it.TFileITSuite;
import net.java.truevfs.driver.odf.OdfDriver;
import net.java.truevfs.kernel.spec.FsTestConfig;
import net.java.truecommons.cio.IoBufferPool;

/**
 * @author Christian Schlichtherle
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
            public IoBufferPool getPool() {
                return FsTestConfig.get().getPool();
            }
        };
    }
}
