/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.tar.file;

import net.truevfs.file.TFileITSuite;
import net.truevfs.kernel.FsArchiveDriver;

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
    public final void testGrowing() {
    }
}
