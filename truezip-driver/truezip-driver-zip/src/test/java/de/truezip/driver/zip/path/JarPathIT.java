/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.path;

import de.truezip.driver.zip.JarDriver;
import de.truezip.kernel.cio.IOPool;
import de.truezip.path.TPathITSuite;

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
                return getTestConfig().getIOPoolProvider().get();
            }
        };
    }
}