/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.tar;

import net.truevfs.kernel.spec.FsDriverProvider;
import net.truevfs.kernel.spec.spi.FsDriverServiceTestSuite;

/**
 * @author Christian Schlichtherle
 */
public class TarDriverServiceTest extends FsDriverServiceTestSuite {

    @Override
    protected String getExtensions() {
        return "tar|tar.bz2|tbz|tb2|tar.gz|tgz|tar.xz|txz";
    }

    @Override
    protected FsDriverProvider newDriverProvider() {
        return new TarDriverService();
    }
}
