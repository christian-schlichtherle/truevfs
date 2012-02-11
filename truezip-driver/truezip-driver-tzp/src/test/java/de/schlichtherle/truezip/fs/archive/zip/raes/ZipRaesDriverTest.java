/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.zip.raes;

import de.schlichtherle.truezip.fs.archive.FsCharsetArchiveDriverTestSuite;
import de.schlichtherle.truezip.fs.archive.zip.ZipDriverEntry;
import de.schlichtherle.truezip.socket.IOPoolProvider;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class ZipRaesDriverTest
extends FsCharsetArchiveDriverTestSuite<ZipDriverEntry, ZipRaesDriver> {

    @Override
    protected TestZipRaesDriver newArchiveDriver(final IOPoolProvider provider) {
        return new TestZipRaesDriver(provider);
    }

    @Override
    protected String getUnencodableName() {
        return null;
    }
}
