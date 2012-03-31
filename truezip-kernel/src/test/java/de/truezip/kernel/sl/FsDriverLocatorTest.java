/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.sl;

import de.truezip.kernel.FsDriverProvider;
import de.truezip.kernel.FsDriverProviderTestSuite;

/**
 * @author Christian Schlichtherle
 */
public class FsDriverLocatorTest extends FsDriverProviderTestSuite {
    @Override
    protected String getSuffixes() {
        return "null";
    }

    @Override
    protected FsDriverProvider newDriverProvider() {
        return FsDriverLocator.SINGLETON;
    }

    @Override
    public void testGet() {
        // This test cannot work because there are no drivers provided by this
        // module.
    }
}
