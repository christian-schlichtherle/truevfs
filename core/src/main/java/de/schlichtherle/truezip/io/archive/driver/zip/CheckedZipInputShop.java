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

import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.zip.CRC32Exception;
import de.schlichtherle.truezip.io.zip.ZipEntryFactory;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.ZipException;

/**
 * A {@link ZipInputShop} which checks the CRC-32 value for all ZIP entries.
 * The additional CRC-32 computation makes this class slower than its super
 * class.
 * <p>
 * If there is a mismatch of the CRC-32 values for a ZIP entry in an input
 * archive, the {@link InputStream#close()} method of the corresponding stream
 * for the archive entry will throw a {@link CRC32Exception}.
 * Other than this, the archive entry will be processed normally.
 * So if just the CRC-32 value for the entry in the archive file has been
 * modified, you can still read its entire contents.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 * @see CheckedZipDriver
 */
public class CheckedZipInputShop extends ZipInputShop {
    
    public CheckedZipInputShop(
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

    /** Overridden to read from a checked input stream. */
    @Override
    public InputSocket<ZipEntry> getInputSocket(final ZipEntry entry)
    throws FileNotFoundException {
        if (!entry.equals(getEntry(entry.getName())))
            throw new IllegalArgumentException("interface contract violation");

        class Input extends InputSocket<ZipEntry> {
            @Override
            public ZipEntry getLocalTarget() {
                return entry;
            }

            @Override
            public InputStream newInputStream() throws IOException {
                return CheckedZipInputShop.this.getInputStream(
                        entry.getName(),
                        true,
                        !(getRemoteTarget() instanceof ZipEntry));
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                throw new UnsupportedOperationException();
            }
        }

        return new Input();
    }
}
