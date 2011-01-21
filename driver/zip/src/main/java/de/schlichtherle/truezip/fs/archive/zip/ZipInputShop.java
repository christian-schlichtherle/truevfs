/*
 * Copyright (C) 2009-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.InputShop;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.zip.RawZipFile;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static de.schlichtherle.truezip.zip.ZipEntry.*;

/**
 * An implementation of {@link InputShop} to read ZIP archives.
 *
 * @see     ZipOutputShop
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public class ZipInputShop
extends RawZipFile<ZipArchiveEntry>
implements InputShop<ZipArchiveEntry> {

    public ZipInputShop(ZipDriver driver, ReadOnlyFile rof)
    throws IOException {
        super(rof, driver.getCharset(), driver.getPreambled(), driver.getPostambled(), driver);
    }

    @Override
    public int getSize() {
        return super.size();
    }

    @Override
    public InputSocket<ZipArchiveEntry> getInputSocket(final String name) {
        if (null == name)
            throw new NullPointerException();

        class Input extends InputSocket<ZipArchiveEntry> {
            @Override
            public ZipArchiveEntry getLocalTarget() throws IOException {
                final ZipArchiveEntry entry = getEntry(name);
                if (null == entry)
                    throw new FileNotFoundException(name + " (entry not found)");
                return entry;
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                throw new FileNotFoundException(name + " (unsupported operation)"); // TODO: Support this feature for STORED entries.
            }

            @Override
            public InputStream newInputStream() throws IOException {
                final Entry entry = getPeerTarget();
                return ZipInputShop.this.getInputStream(
                        getLocalTarget().getName(),
                        false,
                        !(entry instanceof ZipArchiveEntry)
                            || ((ZipArchiveEntry) entry).getMethod() != DEFLATED);
            }
        } // class Input

        return new Input();
    }
}
