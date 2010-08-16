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

package de.schlichtherle.io.archive.zip.raes;

import de.schlichtherle.crypto.io.raes.RaesKeyException;
import de.schlichtherle.crypto.io.raes.RaesOutputStream;
import de.schlichtherle.io.archive.Archive;
import de.schlichtherle.io.archive.spi.InputArchive;
import de.schlichtherle.io.archive.spi.OutputArchive;
import de.schlichtherle.io.archive.spi.TransientIOException;
import de.schlichtherle.io.archive.zip.JarInputArchive;
import java.io.IOException;
import java.io.OutputStream;
import javax.swing.Icon;

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
 * @since TrueZIP 6.0
 * @see SafeZipRaesDriver
 */
public class ParanoidZipRaesDriver extends AbstractZipRaesDriver {
    private static final long serialVersionUID = 6373537182537867796L;

    /**
     * Equivalent to {@link #ParanoidZipRaesDriver(Icon, Icon, boolean, boolean, int)
     * this(null, null, false, false, DEFAULT_LEVEL)}.
     */
    public ParanoidZipRaesDriver() {
        this(null, null, false, false, DEFAULT_LEVEL);
    }

    /**
     * Equivalent to {@link #ParanoidZipRaesDriver(Icon, Icon, boolean, boolean, int)
     * this(null, null, false, false, level)}.
     */
    public ParanoidZipRaesDriver(int level) {
        this(null, null, false, false, level);
    }

    /** Constructs a new paranoid ZIP.RAES driver. */
    public ParanoidZipRaesDriver(
            Icon openIcon,
            Icon closedIcon,
            boolean preambled,
            boolean postambled,
            final int level) {
        super(openIcon, closedIcon, preambled, postambled, level, Long.MAX_VALUE);
    }

    /**
     * This implementation calls {@link #getRaesParameters}, with which it
     * initializes a new {@link RaesOutputStream}, and finally passes the
     * resulting stream to
     * {@link #createZipOutputArchive(Archive, OutputStream, ZipInputArchive)}.
     * <p>
     * Note that this limits the number of concurrent output entry streams
     * to one in order to inhibit writing unencrypted temporary files for
     * buffering the written entries.
     */
    public OutputArchive createOutputArchive(
            final Archive archive,
            final OutputStream out,
            final InputArchive source)
    throws IOException {
        final RaesOutputStream ros;
        try {
            ros = RaesOutputStream.getInstance(out, getRaesParameters(archive));
        } catch (RaesKeyException failure) {
            throw new TransientIOException(failure);
        }

        return createZipOutputArchive(archive, ros, (JarInputArchive) source);
    }
}
