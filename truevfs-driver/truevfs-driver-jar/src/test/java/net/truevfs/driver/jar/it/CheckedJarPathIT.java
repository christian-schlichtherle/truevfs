/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.jar.it;

import net.truevfs.access.TPathITSuite;
import net.truevfs.component.zip.driver.CheckedJarDriver;
import net.truevfs.component.zip.driver.JarDriver;
import net.truevfs.kernel.spec.TestConfig;
import net.truevfs.kernel.spec.cio.IoBufferPool;

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
            public IoBufferPool<?> getPool() {
                return TestConfig.get().getPool();
            }
        };
    }
}
