/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.http;

import net.truevfs.kernel.FsDriverProvider;
import net.truevfs.kernel.spi.FsDriverServiceTestSuite;

/**
 * @author Christian Schlichtherle
 */
public final class HttpDriverServiceTest extends FsDriverServiceTestSuite {
    @Override
    protected String getExtensions() {
        return "http|https";
    }

    @Override
    protected FsDriverProvider newDriverProvider() {
        return new HttpDriverService();
    }
}
