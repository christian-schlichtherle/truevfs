/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.it.jar;

import global.namespace.truevfs.commons.cio.IoBufferPool;
import global.namespace.truevfs.commons.zipdriver.JarDriver;
import global.namespace.truevfs.it.base.TPathITSuite;
import global.namespace.truevfs.kernel.api.FsTestConfig;

/**
 * @author Christian Schlichtherle
 */
public final class JarPathIT extends TPathITSuite<JarDriver> {

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
