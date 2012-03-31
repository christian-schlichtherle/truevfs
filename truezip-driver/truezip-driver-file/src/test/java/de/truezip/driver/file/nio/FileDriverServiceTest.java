/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.file.nio;

import de.truezip.kernel.FsDriverProvider;
import de.truezip.kernel.spi.FsDriverServiceTestSuite;

/**
 * @author Christian Schlichtherle
 */
public final class FileDriverServiceTest extends FsDriverServiceTestSuite {
    @Override
    protected String getSuffixes() {
        return "file";
    }

    @Override
    protected FsDriverProvider newDriverProvider() {
        return new FileDriverService();
    }
}
