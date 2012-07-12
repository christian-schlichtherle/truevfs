/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.tar;

import net.truevfs.kernel.spec.FsDriverMapProvider;
import net.truevfs.kernel.spec.spi.FsDriverMapFactoryTestSuite;

/**
 * @author Christian Schlichtherle
 */
public class TarDriverMapFactoryTest extends FsDriverMapFactoryTestSuite {

    @Override
    protected String getExtensions() {
        return "tar|tar.bz2|tbz|tb2|tar.gz|tgz|tar.xz|txz";
    }

    @Override
    protected FsDriverMapProvider newDriverProvider() {
        return new TarDriverMapFactory();
    }
}
