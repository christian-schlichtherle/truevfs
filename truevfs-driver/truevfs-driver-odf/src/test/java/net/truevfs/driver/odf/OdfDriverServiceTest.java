/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.odf;

import net.truevfs.kernel.spec.FsDriverProvider;
import net.truevfs.kernel.spec.spi.FsDriverServiceTestSuite;

/**
 * @author Christian Schlichtherle
 */
public final class OdfDriverServiceTest extends FsDriverServiceTestSuite {
    @Override
    protected String getExtensions() {
        return "odg|odp|ods|odt|otg|otp|ots|ott|odb|odf|odm|oth";
    }

    @Override
    protected FsDriverProvider newDriverProvider() {
        return new OdfDriverService();
    }
}
