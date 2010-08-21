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

package de.schlichtherle.truezip.io.archive.zip;

import de.schlichtherle.truezip.io.InputArchiveMetaData;
import de.schlichtherle.truezip.io.archive.spi.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.spi.InputArchive;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.util.zip.BasicZipFile;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.zip.ZipException;

/**
 * An implementation of {@link InputArchive} to read ZIP archives.
 *
 * @see ZipDriver
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ZipInputArchive
        extends BasicZipFile
        implements InputArchive {

    private InputArchiveMetaData metaData;

    public ZipInputArchive(
            ReadOnlyFile rof,
            String charset,
            de.schlichtherle.truezip.util.zip.ZipEntryFactory factory,
            boolean preambled,
            boolean postambled)
    throws  NullPointerException,
            UnsupportedEncodingException,
            FileNotFoundException,
            ZipException,
            IOException {
        super(rof, charset, factory, preambled, postambled);
    }

    public int getNumArchiveEntries() {
        return super.size();
    }

    public Enumeration getArchiveEntries() {
        return super.entries();
    }

    public ArchiveEntry getArchiveEntry(final String entryName) {
        return (ZipEntry) super.getEntry(entryName);
    }

    public InputStream getInputStream(
            final ArchiveEntry entry,
            final ArchiveEntry dstEntry)
    throws IOException {
        return super.getInputStream(
                entry.getName(), false, !(dstEntry instanceof ZipEntry));
    }

    //
    // Metadata implementation.
    //

    public InputArchiveMetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(InputArchiveMetaData metaData) {
        this.metaData = metaData;
    }
}
