/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.tar.xz.it;

import net.truevfs.component.tar.driver.it.TarFileITSuite;
import net.truevfs.driver.tar.xz.TarXZDriver;
import net.truevfs.driver.tar.xz.TestTarXZDriver;

/**
 * @author Christian Schlichtherle
 */
public final class TarXZFileIT extends TarFileITSuite<TarXZDriver> {
    @Override
    protected String getExtensionList() {
        return "tar.xz";
    }

    @Override
    protected TarXZDriver newArchiveDriver() {
        return new TestTarXZDriver();
    }
}
