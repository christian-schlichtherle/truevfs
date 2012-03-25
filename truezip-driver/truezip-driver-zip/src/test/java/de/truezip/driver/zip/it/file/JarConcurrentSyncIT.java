/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.it.file;

import de.schlichtherle.truezip.file.ConcurrentSyncTestSuite;
import de.truezip.driver.zip.JarDriver;

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
