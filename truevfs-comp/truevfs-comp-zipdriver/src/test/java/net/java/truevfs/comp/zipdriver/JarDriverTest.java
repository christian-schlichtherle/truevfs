/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zipdriver;

import net.java.truecommons.cio.IoBufferPool;
import net.java.truevfs.kernel.spec.FsArchiveDriverTestSuite;
import net.java.truevfs.kernel.spec.FsTestConfig;

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
                return FsTestConfig.get().getPool();
            }
        };
    }

    @Override
    protected String getUnencodableName() { return null; }
}
