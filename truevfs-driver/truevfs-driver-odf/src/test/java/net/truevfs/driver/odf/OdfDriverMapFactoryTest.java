/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.odf;

import net.truevfs.kernel.spec.FsDriverMapProvider;
import net.truevfs.kernel.spec.spi.FsDriverMapFactoryTestSuite;

/**
 * @author Christian Schlichtherle
 */
public final class OdfDriverMapFactoryTest extends FsDriverMapFactoryTestSuite {
    @Override
    protected String getExtensions() {
        return "odg|odp|ods|odt|otg|otp|ots|ott|odb|odf|odm|oth";
    }

    @Override
    protected FsDriverMapProvider newDriverProvider() {
        return new OdfDriverMapFactory();
    }
}
