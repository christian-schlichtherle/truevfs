/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.nio.file.tar;

import de.schlichtherle.truezip.fs.archive.FsArchiveDriver;
import de.schlichtherle.truezip.nio.file.TPathTestSuite;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class TarPathTestSuite<D extends FsArchiveDriver<?>>
extends TPathTestSuite<D> {

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
