/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.tar.bzip2.it;

import net.truevfs.component.tar.driver.it.TarPathITSuite;
import net.truevfs.driver.tar.bzip2.TarBZip2Driver;
import net.truevfs.driver.tar.bzip2.TestTarBZip2Driver;

/**
 * @author Christian Schlichtherle
 */
public final class TarBZip2PathIT extends TarPathITSuite<TarBZip2Driver> {
    @Override
    protected String getExtensionList() {
        return "tar.bz2";
    }

    @Override
    protected TarBZip2Driver newArchiveDriver() {
        return new TestTarBZip2Driver();
    }
}
