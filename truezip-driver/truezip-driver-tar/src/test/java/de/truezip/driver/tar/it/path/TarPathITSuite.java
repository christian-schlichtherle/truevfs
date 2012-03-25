/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.tar.it.path;

import de.schlichtherle.truezip.fs.FsArchiveDriver;
import de.schlichtherle.truezip.nio.file.TPathTestSuite;

/**
 * @param   <D> The type of the archive driver.
 * @author  Christian Schlichtherle
 */
public abstract class TarPathITSuite<D extends FsArchiveDriver<?>>
extends TPathTestSuite<D> {

    /**
     * Skipped because appending to TAR files is not supported.
     */
    @Override
    public final void testGrowing() {
    }
}