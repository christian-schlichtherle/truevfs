/*
 * Copyright (C) 2007-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.archive.impl.zip;

import de.schlichtherle.truezip.io.archive.output.ArchiveOutputStreamSocket;
import de.schlichtherle.truezip.io.socket.IOReference;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.spi.MultiplexedOutputArchive;
import de.schlichtherle.truezip.io.archive.output.ArchiveOutput;
import java.io.IOException;
import java.io.OutputStream;

import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.UNKNOWN;
import static de.schlichtherle.truezip.io.zip.ZipEntry.STORED;

/**
 * Created by {@link OdfDriver} to meet the special requirements of
 * OpenDocument Format (ODF) files.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class OdfOutput
extends MultiplexedOutputArchive<ZipEntry> {

    /** The name of the entry to receive tender, loving care. */
    private static final String MIMETYPE = "mimetype";

    /** Whether we have started to write the <i>mimetype</i> entry or not. */
    private boolean mimetype;

    /** Creates a new {@code OdfOutput}. */
    public OdfOutput(ArchiveOutput target) {
        super(target);
    }

    @Override
    protected OutputStream newOutputStream(
            final ArchiveOutputStreamSocket<? extends ZipEntry> dst,
            final IOReference<? extends ArchiveEntry> src)
    throws IOException {
        final ZipEntry dstEntry = dst.get();
        if (MIMETYPE.equals(dstEntry.getName())) {
            mimetype = true;
            if (dstEntry.getMethod() == UNKNOWN)
                dstEntry.setMethod(STORED);
        }
        return super.newOutputStream(dst, src);
    }

    @Override
    public boolean isTargetBusy() {
        return !mimetype || super.isTargetBusy();
    }

    @Override
    public void close() throws IOException {
        mimetype = true; // trigger writing temps
        super.close();
    }
}
