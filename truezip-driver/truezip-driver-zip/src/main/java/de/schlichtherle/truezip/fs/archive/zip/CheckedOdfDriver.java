/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.socket.IOPoolProvider;
import javax.annotation.concurrent.Immutable;

/**
 * An archive driver for ODF files which checks the CRC-32 value for all ZIP
 * entries in input archives.
 * The additional CRC-32 computation makes this class slower than its super
 * class.
 * <p>
 * If there is a mismatch of the CRC-32 values for a ZIP entry in an input
 * archive, the {@link java.io.InputStream#close} method of the corresponding
 * stream for the archive entry will throw a
 * {@link de.schlichtherle.truezip.zip.CRC32Exception}.
 * Other than this, the archive entry will be processed normally.
 * So if just the CRC-32 value for the entry in the archive file has been
 * modified, you can still read its entire contents.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public class CheckedOdfDriver extends OdfDriver {

    public CheckedOdfDriver(IOPoolProvider ioPoolProvider) {
        super(ioPoolProvider);
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@code true}
     */
    @Override
    protected boolean check(ZipInputShop input, ZipArchiveEntry entry) {
        return true;
    }
}
