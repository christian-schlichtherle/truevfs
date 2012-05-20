/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.tar.access;

import net.truevfs.kernel.FsArchiveDriver;
import net.truevfs.access.TPathITSuite;

/**
 * @param   <D> The type of the archive driver.
 * @author  Christian Schlichtherle
 */
public abstract class TarPathITSuite<D extends FsArchiveDriver<?>>
extends TPathITSuite<D> {

    /**
     * Skipped because appending to TAR files is not supported.
     */
    @Override
    public final void testGrowing() {
    }
}