/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.jar.it;

import net.java.truevfs.access.TPathITSuite;
import net.java.truevfs.component.zip.driver.CheckedJarDriver;
import net.java.truevfs.component.zip.driver.JarDriver;
import net.java.truevfs.kernel.spec.TestConfig;
import net.java.truevfs.kernel.spec.cio.IoBufferPool;

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
                return TestConfig.get().getPool();
            }
        };
    }
}
