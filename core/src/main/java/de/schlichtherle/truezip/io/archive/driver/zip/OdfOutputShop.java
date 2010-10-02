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

package de.schlichtherle.truezip.io.archive.driver.zip;

import de.schlichtherle.truezip.io.socket.output.FilterOutputSocket;
import de.schlichtherle.truezip.io.socket.output.CommonOutputSocket;
import de.schlichtherle.truezip.io.archive.driver.MultiplexedArchiveOutputShop;
import java.io.IOException;
import java.io.OutputStream;

import static de.schlichtherle.truezip.io.archive.driver.ArchiveEntry.UNKNOWN;
import static de.schlichtherle.truezip.io.zip.ZipEntry.STORED;

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
    public CommonOutputSocket<ZipEntry> newOutputSocket(final ZipEntry entry)
    throws IOException {
        class OutputSocket extends FilterOutputSocket<ZipEntry> {
            OutputSocket() throws IOException {
                super(OdfOutputShop.super.newOutputSocket(entry));
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                if (MIMETYPE.equals(entry.getName())) {
                    mimetype = true;
                    if (entry.getMethod() == UNKNOWN)
                        entry.setMethod(STORED);
                }
                return super.newOutputStream();
            }
        }
        return new OutputSocket();
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
