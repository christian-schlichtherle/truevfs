/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.it;

import net.truevfs.access.TFileITSuite;
import net.truevfs.driver.zip.JarDriver;
import net.truevfs.kernel.spec.cio.IoPool;

/**
 * @author  Christian Schlichtherle
 */
public final class JarFileIT extends TFileITSuite<JarDriver> {

    @Override
    protected String getExtensionList() {
        return "jar";
    }

    @Override
    protected JarDriver newArchiveDriver() {
        return new JarDriver() {
            @Override
            public IoPool<?> getIoPool() {
                return getTestConfig().getIoPoolProvider().getIoPool();
            }
        };
    }
}