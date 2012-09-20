/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.tar.xz;

import net.java.truevfs.comp.tardriver.TarDriverEntry;
import net.java.truevfs.kernel.spec.FsArchiveDriverTestSuite;

/**
 * @author Christian Schlichtherle
 */
public final class TarXZDriverTest
extends FsArchiveDriverTestSuite<TarDriverEntry, TarXZDriver> {

    @Override
    protected TarXZDriver newArchiveDriver() {
        return new TestTarXZDriver();
    }

    @Override
    protected String getUnencodableName() {
        return null;
    }
}
