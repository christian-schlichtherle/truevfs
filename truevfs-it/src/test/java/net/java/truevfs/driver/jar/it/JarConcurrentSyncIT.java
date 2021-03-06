/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.jar.it;

import net.java.truecommons.cio.IoBufferPool;
import net.java.truevfs.access.it.ConcurrentSyncITSuite;
import net.java.truevfs.comp.zipdriver.JarDriver;
import net.java.truevfs.kernel.spec.FsTestConfig;

/**
 * @author Christian Schlichtherle
 */
public final class JarConcurrentSyncIT extends ConcurrentSyncITSuite<JarDriver> {

    @Override
    protected String getExtensionList() {
        return "jar";
    }

    @Override
    protected JarDriver newArchiveDriver() {
        return new JarDriver() {
            @Override
            public IoBufferPool getPool() {
                return FsTestConfig.get().getPool();
            }
        };
    }
}
