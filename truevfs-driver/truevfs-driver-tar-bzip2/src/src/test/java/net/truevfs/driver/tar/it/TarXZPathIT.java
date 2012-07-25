/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.tar.it;

import net.truevfs.driver.tar.TarXZDriver;
import net.truevfs.driver.tar.TestTarXZDriver;

/**
 * @author Christian Schlichtherle
 */
public final class TarXZPathIT extends TarPathITSuite<TarXZDriver> {

    @Override
    protected String getExtensionList() {
        return "tar.xz";
    }

    @Override
    protected TarXZDriver newArchiveDriver() {
        return new TestTarXZDriver();
    }
}
