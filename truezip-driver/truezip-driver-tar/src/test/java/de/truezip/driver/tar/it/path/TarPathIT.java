/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.tar.it.path;

import de.truezip.driver.tar.TarDriver;

/**
 * @author  Christian Schlichtherle
 */
public final class TarPathIT extends TarPathITSuite<TarDriver> {

    @Override
    protected String getSuffixList() {
        return "tar";
    }

    @Override
    protected TarDriver newArchiveDriver() {
        return new TarDriver(getTestConfig().getIOPoolProvider());
    }
}