package net.truevfs.driver.zip.it;

/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */


import net.truevfs.driver.zip.JarDriver;
import net.truevfs.kernel.cio.IOPool;
import net.truevfs.access.TPathITSuite;

/**
 * @author  Christian Schlichtherle
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
            public IOPool<?> getIOPool() {
                return getTestConfig().getIOPoolProvider().getIOPool();
            }
        };
    }
}