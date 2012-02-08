/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.file.tar;

import de.schlichtherle.truezip.fs.archive.tar.TarGZipDriver;
import de.schlichtherle.truezip.socket.IOPoolProvider;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class TarGZipFileIT extends TarFileTestBase<TarGZipDriver> {

    @Override
    protected String getSuffixList() {
        return "tar.gz";
    }

    @Override
    protected TarGZipDriver newArchiveDriver(IOPoolProvider provider) {
        return new TarGZipDriver(provider);
    }
}
