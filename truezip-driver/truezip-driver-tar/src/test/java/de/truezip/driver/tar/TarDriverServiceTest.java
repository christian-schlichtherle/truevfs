/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.tar;

import de.truezip.kernel.FsDriverProvider;
import de.truezip.kernel.spi.FsDriverServiceTestSuite;

/**
 * @author Christian Schlichtherle
 */
public class TarDriverServiceTest extends FsDriverServiceTestSuite {

    @Override
    protected String getSuffixes() {
        return "tar|tar.bz2|tar.gz|tbz|tb2|tgz";
    }

    @Override
    protected FsDriverProvider newDriverProvider() {
        return new TarDriverService();
    }
}
