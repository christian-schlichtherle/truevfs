/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zipdriver;

import net.java.truevfs.comp.zipdriver.JarDriver;
import net.java.truevfs.comp.zipdriver.JarDriverEntry;
import net.java.truevfs.kernel.spec.FsArchiveDriverTestSuite;
import net.java.truevfs.kernel.spec.TestConfig;
import net.java.truecommons.cio.IoBufferPool;

/**
 * @author Christian Schlichtherle
 */
public final class JarDriverTest
extends FsArchiveDriverTestSuite<JarDriverEntry, JarDriver> {

    @Override
    protected JarDriver newArchiveDriver() {
        return new JarDriver() {
            @Override
            public IoBufferPool getPool() {
                return TestConfig.get().getPool();
            }
        };
    }

    @Override
    protected String getUnencodableName() {
        return null;
    }
}
