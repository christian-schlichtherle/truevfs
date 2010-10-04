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

import de.schlichtherle.truezip.io.socket.input.CommonInputSocket;
import de.schlichtherle.truezip.io.socket.input.CommonInputShop;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.zip.RawZipFile;
import de.schlichtherle.truezip.io.zip.ZipEntryFactory;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.ZipException;

/**
 * An implementation of {@link CommonInputShop} to read ZIP archives.
 *
 * @see ZipDriver
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ZipInputShop
extends RawZipFile<ZipEntry>
implements CommonInputShop<ZipEntry> {

    public ZipInputShop(
            ReadOnlyFile rof,
            String charset,
            boolean preambled,
            boolean postambled,
            ZipEntryFactory<ZipEntry> factory)
    throws  NullPointerException,
            UnsupportedEncodingException,
            FileNotFoundException,
            ZipException,
            IOException {
        super(rof, charset, preambled, postambled, factory);
    }

    @Override
    public CommonInputSocket<ZipEntry> newInputSocket(final ZipEntry entry)
    throws FileNotFoundException {
        if (getEntry(entry.getName()) != entry)
            throw new IllegalArgumentException("interface contract violation");
        class InputSocket extends CommonInputSocket<ZipEntry> {
            @Override
            public ZipEntry getTarget() {
                return entry;
            }

            @Override
            public InputStream newInputStream() throws IOException {
                return ZipInputShop.this.getInputStream(
                        entry.getName(),
                        false,
                        !(getPeerTarget() instanceof ZipEntry));
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                throw new UnsupportedOperationException();
            }
        }
        return new InputSocket();
    }
}
