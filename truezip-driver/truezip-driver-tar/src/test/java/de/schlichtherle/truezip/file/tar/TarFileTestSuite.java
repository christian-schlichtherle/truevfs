/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.file.tar;

import de.schlichtherle.truezip.file.TFileTestSuite;
import de.schlichtherle.truezip.fs.archive.FsArchiveDriver;

/**
 * @param   <D> The type of the archive driver.
 * @author  Christian Schlichtherle
 * @version $Id$
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
