/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.file;

import net.truevfs.driver.zip.JarDriver;
import net.truevfs.file.ConcurrentSyncITSuite;
import net.truevfs.kernel.cio.IOPool;

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
            public IOPool<?> getIOPool() {
                return getTestConfig().getIOPoolProvider().getIOPool();
            }
        };
    }
}