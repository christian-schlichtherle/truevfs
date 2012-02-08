/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.file.tar;

import de.schlichtherle.truezip.fs.archive.tar.TarBZip2Driver;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class TarBZip2FileIT extends TarFileTestBase<TarBZip2Driver> {

    @Override
    protected String getSuffixList() {
        return "tar.bz2";
    }

    @Override
    protected TarBZip2Driver newArchiveDriver(IOPoolProvider provider) {
        return new TarBZip2Driver(provider) {
            @Override
            public int getLevel() {
                return BZip2CompressorOutputStream.MIN_BLOCKSIZE;
            }
        };
    }
}
