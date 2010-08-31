/*
 * Copyright (C) 2009-2010 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.schlichtherle.truezip.io.archive.driver.zip;

import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.ZipException;

/**
 * A {@link ZipInputArchive} which checks the CRC-32 value for all ZIP entries.
 * The additional CRC-32 computation makes this class slower than its super
 * class.
 * <p>
 * If there is a mismatch of the CRC-32 values for a ZIP entry in an input
 * archive, the {@link java.io.InputStream#close} method of the corresponding
 * stream for the archive entry will throw a
 * {@link de.schlichtherle.truezip.io.zip.CRC32Exception}.
 * Other than this, the archive entry will be processed normally.
 * So if just the CRC-32 value for the entry in the archive file has been
 * modified, you can still read its entire contents.
 * 
 * @see ZipInputArchive
 * @see CheckedZipDriver
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class CheckedZipInputArchive extends ZipInputArchive {
    
    public CheckedZipInputArchive(
            ReadOnlyFile rof,
            String charset,
            de.schlichtherle.truezip.io.zip.ZipEntryFactory factory,
            boolean preambled,
            boolean postambled)
    throws  NullPointerException,
            UnsupportedEncodingException,
            FileNotFoundException,
            ZipException,
            IOException {
        super(rof, charset, factory, preambled, postambled);
    }

    /**
     * Overridden to read from a checked input stream.
     */
    @Override
    public InputStream newInputStream(
            ArchiveEntry entry,
            ArchiveEntry dstEntry)
    throws  IOException {
        return super.getInputStream(
                entry.getName(), true, !(dstEntry instanceof ZipEntry));
    }
}