/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.fs.archive.driver.zip.raes;

import de.schlichtherle.truezip.io.fs.concurrent.FsConcurrentModel;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.io.fs.archive.driver.zip.ZipEntry;
import de.schlichtherle.truezip.socket.InputShop;
import de.schlichtherle.truezip.crypto.io.raes.RaesKeyException;
import de.schlichtherle.truezip.crypto.io.raes.RaesOutputStream;
import de.schlichtherle.truezip.socket.OutputShop;
import de.schlichtherle.truezip.io.TabuFileException;
import de.schlichtherle.truezip.io.fs.archive.driver.zip.ZipDriver;
import de.schlichtherle.truezip.io.fs.archive.driver.zip.ZipInputShop;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.socket.LazyOutputSocket;
import java.io.IOException;
import java.io.OutputStream;

import static java.util.zip.Deflater.BEST_COMPRESSION;

/**
 * A paranoid archive driver which builds RAES encrypted ZIP files.
 * This driver <em>always</em> checks the cipher text of input archive files
 * using the RAES Message Authentication Code (MAC), which makes it slower than
 * the {@link SafeZipRaesDriver} for archive files larger than 512 KB and
 * may pause the client application on the first access to the archive file
 * for a while if the file is large.
 * Note that the CRC-32 value of the plain text ZIP file is never checked
 * because this is made redundant by the MAC verification.
 * <p>
 * In addition, this driver limits the number of concurrent entry output
 * streams to one, so that writing unencrypted temporary files is inhibited.
 * <p>
 * Instances of this class are immutable.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 * @see SafeZipRaesDriver
 */
public class ParanoidZipRaesDriver extends AbstractZipRaesDriver {

    private static final long serialVersionUID = 6373537182537867796L;

    /**
     * Equivalent to {@link #ParanoidZipRaesDriver(boolean, boolean, int)
     * this(false, false, Deflater.BEST_COMPRESSION)}.
     */
    public ParanoidZipRaesDriver() {
        this(false, false, BEST_COMPRESSION);
    }

    /**
     * Equivalent to {@link #ParanoidZipRaesDriver(boolean, boolean, int)
     * this(false, false, level)}.
     */
    public ParanoidZipRaesDriver(int level) {
        this(false, false, level);
    }

    /** Constructs a new paranoid ZIP.RAES driver. */
    public ParanoidZipRaesDriver(
            boolean preambled,
            boolean postambled,
            final int level) {
        super(preambled, postambled, level, Long.MAX_VALUE);
    }

    /**
     * This implementation calls {@link #getRaesParameters}, with which it
     * initializes a new {@link RaesOutputStream}, and finally passes the
     * resulting stream to {@link ZipDriver#newZipOutputShop}.
     * <p>
     * Note that this limits the number of concurrent output entry streams
     * to one in order to inhibit writing unencrypted temporary files for
     * buffering the written entries.
     */
    @Override
    public OutputShop<ZipEntry> newOutputShop(
            final FsConcurrentModel model,
            final OutputSocket<?> output,
            final InputShop<ZipEntry> source)
    throws IOException {
        final OutputStream out = new LazyOutputSocket<Entry>(output)
                .newOutputStream();
        try {
            final RaesOutputStream ros;
            try {
                ros = RaesOutputStream.getInstance(
                        out, getRaesParameters(model));
            } catch (RaesKeyException ex) {
                throw new TabuFileException(ex);
            }
            return newZipOutputShop(model, ros, (ZipInputShop) source);
        } catch (IOException cause) {
            try {
                out.close();
            } catch (IOException ex) {
                throw (IOException) ex.initCause(cause);
            }
            throw cause;
        }
    }
}
