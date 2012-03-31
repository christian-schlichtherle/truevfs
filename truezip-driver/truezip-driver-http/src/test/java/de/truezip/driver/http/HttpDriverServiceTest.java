/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.http;

import de.truezip.kernel.FsDriverProvider;
import de.truezip.kernel.spi.FsDriverServiceTestSuite;

/**
 * @author Christian Schlichtherle
 */
public final class HttpDriverServiceTest extends FsDriverServiceTestSuite {
    @Override
    protected String getSuffixes() {
        return "http|https";
    }

    @Override
    protected FsDriverProvider newDriverProvider() {
        return new HttpDriverService();
    }
}
