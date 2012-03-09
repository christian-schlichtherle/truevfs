/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.file.tar;

import de.schlichtherle.truezip.file.TFileTestSuite;
import de.schlichtherle.truezip.fs.archive.FsArchiveDriver;

/**
 * @param  <D> The type of the archive driver.
 * @author Christian Schlichtherle
 */
public abstract class TarFileTestSuite<D extends FsArchiveDriver<?>>
extends TFileTestSuite<D> {

    /**
     * Skipped because appending to TAR files is currently not supported.
     * 
     * @deprecated 
     */
    @Deprecated
    @Override
    public final void testGrowing() {
    }
}
