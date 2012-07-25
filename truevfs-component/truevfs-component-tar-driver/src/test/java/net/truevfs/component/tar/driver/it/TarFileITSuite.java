/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.component.tar.driver.it;

import net.truevfs.access.TFileITSuite;
import net.truevfs.kernel.spec.FsArchiveDriver;

/**
 * @param  <D> The type of the archive driver.
 * @author Christian Schlichtherle
 */
public abstract class TarFileITSuite<D extends FsArchiveDriver<?>>
extends TFileITSuite<D> {

    /**
     * Skipped because appending to TAR files is not supported.
     */
    @Override
    @SuppressWarnings("NoopMethodInAbstractClass")
    public final void testGrowing() {
    }
}