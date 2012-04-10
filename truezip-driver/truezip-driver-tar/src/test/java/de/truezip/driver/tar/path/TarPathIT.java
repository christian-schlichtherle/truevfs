/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.tar.path;

import de.truezip.driver.tar.TarDriver;
import de.truezip.kernel.cio.IOPool;

/**
 * @author Christian Schlichtherle
 */
public final class TarPathIT extends TarPathITSuite<TarDriver> {

    @Override
    protected String getExtensionList() {
        return "tar";
    }

    @Override
    protected TarDriver newArchiveDriver() {
        return new TarDriver() {
            @Override
            public IOPool<?> getIOPool() {
                return getTestConfig().getIOPoolProvider().get();
            }
        };
    }
}