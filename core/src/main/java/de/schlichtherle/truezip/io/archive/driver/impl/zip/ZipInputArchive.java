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

package de.schlichtherle.truezip.io.archive.driver.impl.zip;

import de.schlichtherle.truezip.io.archive.controller.InputArchiveMetaData;
import de.schlichtherle.truezip.io.archive.driver.ArchiveInputStreamSocket;
import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.driver.InputArchive;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.socket.IOReference;
import de.schlichtherle.truezip.io.socket.IOReferences;
import de.schlichtherle.truezip.io.zip.BasicZipFile;
import de.schlichtherle.truezip.io.zip.ZipEntryFactory;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.ZipException;

/**
 * An implementation of {@link InputArchive} to read ZIP archives.
 *
 * @see ZipDriver
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ZipInputArchive
extends BasicZipFile<ZipEntry>
implements InputArchive<ZipEntry> {

    private InputArchiveMetaData metaData;

    public ZipInputArchive(
            ReadOnlyFile rof,
            String charset,
            boolean preambled,
            boolean postambled,
            ZipEntryFactory factory)
    throws  NullPointerException,
            UnsupportedEncodingException,
            FileNotFoundException,
            ZipException,
            IOException {
        super(rof, charset, preambled, postambled, factory);
    }

    @Override
    public ArchiveInputStreamSocket<ZipEntry> getInputStreamSocket(
            final ZipEntry entry)
    throws FileNotFoundException {
        assert getEntry(entry.getName()) == entry : "violation of contract for InputArchive";
        class InputStreamSocket implements ArchiveInputStreamSocket<ZipEntry> {
            @Override
            public ZipEntry get() {
                return entry;
            }

            @Override
            public InputStream newInputStream(
                    final IOReference<? extends ArchiveEntry> dst)
            throws IOException {
                final ArchiveEntry dstEntry = IOReferences.deref(dst);
                return ZipInputArchive.this.newInputStream(entry, dstEntry);
            }
        }
        return new InputStreamSocket();
    }

    protected InputStream newInputStream(ZipEntry entry, ArchiveEntry dstEntry)
    throws IOException {
        return super.getInputStream(    entry.getName(), false,
                                        !(dstEntry instanceof ZipEntry));
    }

    public InputArchiveMetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(InputArchiveMetaData metaData) {
        this.metaData = metaData;
    }
}
