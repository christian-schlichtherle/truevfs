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

package de.schlichtherle.truezip.io.archive.zip.raes;

import de.schlichtherle.truezip.io.archive.Archive;
import de.schlichtherle.truezip.io.archive.zip.CheckedJarInputArchive;
import de.schlichtherle.truezip.io.archive.zip.ZipInputArchive;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import java.io.IOException;
import javax.swing.Icon;

/**
 * A safe archive driver which builds RAES encrypted ZIP files.
 * For input archive files up to 512 KB, the cipher text gets authenticated
 * using the RAES provided Message Authentication Code (MAC) <em>before</em>
 * the archive can be accessed by a client application.
 * For larger input archive files, the MAC is not used, but instead the
 * CRC-32 value of the decrypted and deflated archive entries is checked
 * when the archive entry stream is <em>closed</em> by the client application,
 * resulting in some {@link IOException}.
 * <p>
 * This operation mode is considered to be safe:
 * Although a formal prove is missing, it should be computationally
 * infeasible to modify an archive file so that <em>after</em> decryption
 * of the archive and <em>after</em> inflation (decompression) of an
 * entry's data its CRC-32 value still matches!
 * This should hold true even though CRC-32 is not at all a good cryptographic
 * hash function because of its frequent collisions, its linear output and
 * small output size.
 * It's the ZIP inflation algorithm which actually comes to our rescue!
 * <p>
 * Instances of this class are immutable.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.0
 * @see ParanoidZipRaesDriver
 */
public class SafeZipRaesDriver extends AbstractZipRaesDriver {
    private static final long serialVersionUID = 7701862504670759996L;

    /**
     * The default trigger for authentication in bytes ({@value}).
     * Input archive files smaller than or equal to this size get verified
     * using the RAES Message Authentication Code (MAC) before they are
     * accessed.
     */
    public static final long DEFAULT_AUTHENTICATION_TRIGGER = 512 * 1024;

    /**
     * Equivalent to {@link #SafeZipRaesDriver(Icon, Icon, boolean, boolean, int, long)
     * this(null, null, false, false, DEFAULT_LEVEL, DEFAULT_AUTHENTICATION_TRIGGER)}.
     */
    public SafeZipRaesDriver() {
        this(null, null, false, false, DEFAULT_LEVEL, DEFAULT_AUTHENTICATION_TRIGGER);
    }

    /**
     * Equivalent to {@link #SafeZipRaesDriver(Icon, Icon, boolean, boolean, int, long)
     * this(null, null, false, false, level, DEFAULT_AUTHENTICATION_TRIGGER)}.
     */
    public SafeZipRaesDriver(int level) {
        this(null, null, false, false, level, DEFAULT_AUTHENTICATION_TRIGGER);
    }

    /** Constructs a new safe ZIP.RAES driver. */
    public SafeZipRaesDriver(
            Icon openIcon,
            Icon closedIcon,
            boolean preambled,
            boolean postambled,
            final int level,
            final long authenticationTrigger) {
        super(openIcon, closedIcon, preambled, postambled, level, authenticationTrigger);
    }

    /**
     * {@inheritDoc}
     * <p>
     * If the net file length of the archive is larger than the authentication
     * trigger, then a {@link CheckedJarInputArchive} for CRC-32
     * authentication is returned, otherwise a plain {@link ZipInputArchive}
     * which doesn't do any authentication.
     * <p>
     * This complements the behaviour of the
     * {@link AbstractZipRaesDriver#createInputArchive} method in the super
     * class, which authenticates the cipher text using the MAC iff the gross
     * file length is smaller than or equal to the authentication trigger.
     * <p>
     * Note that this leaves a small window for gross file lengths of about
     * {@link #getAuthenticationTrigger} bytes where the archive is both MAC
     * and CRC-32 authenticated.
     */
    @Override
    protected ZipInputArchive createZipInputArchive(
            Archive archive,
            ReadOnlyFile rof)
    throws IOException {
        // Optimization: If the read-only file is smaller than the
        // authentication trigger, then its entire cipher text has already
        // been authenticated by
        // {@link AbstractZipRaesDriver#createInputArchive}.
        // Hence, checking the CRC-32 value of the plain text ZIP file is
        // redundant.
        return rof.length() > getAuthenticationTrigger()
                ? new CheckedJarInputArchive(
                    rof, getCharset(), getPreambled(), getPostambled())
                : super.createZipInputArchive(archive, rof);
    }
}
