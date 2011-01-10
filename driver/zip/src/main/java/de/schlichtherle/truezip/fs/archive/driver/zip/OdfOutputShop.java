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

package de.schlichtherle.truezip.fs.archive.driver.zip;

import de.schlichtherle.truezip.socket.DecoratingOutputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.fs.archive.MultiplexedArchiveOutputShop;
import java.io.IOException;
import java.io.OutputStream;

import static de.schlichtherle.truezip.fs.archive.ArchiveEntry.UNKNOWN;
import static de.schlichtherle.truezip.zip.ZipEntry.STORED;

/**
 * Created by {@link OdfDriver} to meet the special requirements of
 * OpenDocument Format (ODF) files.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class OdfOutputShop extends MultiplexedArchiveOutputShop<ZipEntry> {

    /** The name of the entry to receive tender, loving care. */
    private static final String MIMETYPE = "mimetype";

    /** Whether we have started to write the <i>mimetype</i> entry or not. */
    private boolean mimetype;

    /** Creates a new {@code OdfOutputShop}. */
    public OdfOutputShop(ZipOutputShop target) {
        super(target);
    }

    @Override
    public OutputSocket<ZipEntry> getOutputSocket(final ZipEntry entry) {
        if (null == entry)
            throw new NullPointerException();

        class Output extends DecoratingOutputSocket<ZipEntry> {
            Output() {
                super(OdfOutputShop.super.getOutputSocket(entry));
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                if (MIMETYPE.equals(entry.getName())) {
                    mimetype = true;
                    if (UNKNOWN == entry.getMethod())
                        entry.setMethod(STORED);
                }
                return super.newOutputStream();
            }
        } // class Output

        return new Output();
    }

    @Override
    public boolean isBusy() {
        return !mimetype || super.isBusy();
    }

    @Override
    public void close() throws IOException {
        mimetype = true; // trigger writing temps
        super.close();
    }
}
