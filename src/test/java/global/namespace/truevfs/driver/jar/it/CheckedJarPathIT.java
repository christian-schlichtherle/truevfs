/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.jar.it;

import global.namespace.truevfs.comp.cio.IoBufferPool;
import global.namespace.truevfs.access.it.TPathITSuite;
import global.namespace.truevfs.comp.zipdriver.CheckedJarDriver;
import global.namespace.truevfs.comp.zipdriver.JarDriver;
import global.namespace.truevfs.kernel.spec.FsTestConfig;

/**
 * @author Christian Schlichtherle
 */
public final class CheckedJarPathIT extends TPathITSuite<JarDriver> {

    @Override
    protected String getExtensionList() {
        return "jar";
    }

    @Override
    protected JarDriver newArchiveDriver() {
        return new CheckedJarDriver() {
            @Override
            public IoBufferPool getPool() {
                return FsTestConfig.get().getPool();
            }
        };
    }
}
