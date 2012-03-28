/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.file;

import de.truezip.driver.zip.JarDriver;
import de.truezip.file.TFileITSuite;

/**
 * @author  Christian Schlichtherle
 */
public final class JarFileIT extends TFileITSuite<JarDriver> {

    @Override
    protected String getSuffixList() {
        return "jar";
    }

    @Override
    protected JarDriver newArchiveDriver() {
        return new JarDriver(getTestConfig().getIOPoolProvider());
    }
}