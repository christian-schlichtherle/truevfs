/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.sl;

import net.truevfs.kernel.FsDriverProvider;
import net.truevfs.kernel.FsDriverProviderTestSuite;

/**
 * @author Christian Schlichtherle
 */
public class FsDriverLocatorTest extends FsDriverProviderTestSuite {
    @Override
    protected String getExtensions() {
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
