/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip;

import de.truezip.kernel.FsDriverProvider;
import de.truezip.kernel.spi.FsDriverServiceTestSuite;

/**
 * @author Christian Schlichtherle
 */
public final class ZipDriverServiceTest extends FsDriverServiceTestSuite {
    @Override
    protected String getExtensions() {
        return "zip|ear|jar|war|odg|odp|ods|odt|otg|otp|ots|ott|odb|odf|odm|oth|exe";
    }

    @Override
    protected FsDriverProvider newDriverProvider() {
        return new ZipDriverService();
    }
}
