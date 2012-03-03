/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.file.zip;

import de.schlichtherle.truezip.file.ConcurrentSyncTestSuite;
import de.schlichtherle.truezip.fs.archive.zip.JarDriver;

/**
 * @author Christian Schlichtherle
 */
public final class JarConcurrentSyncIT extends ConcurrentSyncTestSuite<JarDriver> {

    @Override
    protected String getSuffixList() {
        return "jar";
    }

    @Override
    protected JarDriver newArchiveDriver() {
        return new JarDriver(getTestConfig().getIOPoolProvider());
    }
}
