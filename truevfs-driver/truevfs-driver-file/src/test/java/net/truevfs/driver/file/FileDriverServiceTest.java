/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.file;

import net.truevfs.kernel.FsDriverProvider;
import net.truevfs.kernel.spi.FsDriverServiceTestSuite;

/**
 * @author Christian Schlichtherle
 */
public final class FileDriverServiceTest extends FsDriverServiceTestSuite {
    @Override
    protected String getExtensions() {
        return "file";
    }

    @Override
    protected FsDriverProvider newDriverProvider() {
        return new FileDriverService();
    }
}
