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

import de.schlichtherle.crypto.io.raes.KeyManagerRaesParameters;
import de.schlichtherle.crypto.io.raes.RaesKeyException;
import de.schlichtherle.crypto.io.raes.RaesOutputStream;
import de.schlichtherle.crypto.io.raes.RaesParameters;
import de.schlichtherle.crypto.io.raes.RaesReadOnlyFile;
import de.schlichtherle.io.archive.Archive;
import de.schlichtherle.io.archive.spi.ArchiveEntry;
import de.schlichtherle.io.archive.spi.InputArchive;
import de.schlichtherle.io.archive.spi.OutputArchive;
import de.schlichtherle.io.archive.spi.TransientIOException;
import de.schlichtherle.io.archive.zip.JarDriver;
import de.schlichtherle.io.archive.zip.JarEntry;
import de.schlichtherle.io.rof.ReadOnlyFile;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.OutputStream;
import javax.swing.Icon;

/**
 * An abstract archive driver which builds RAES encrypted ZIP files
 * and optionally authenticates the cipher data of the input archive files
 * presented to it.
 * <p>
 * The constructor takes an optional authentication trigger parameter which
 * can be used by subclasses to fine tune the authentication process.
 * When omitted, the RAES Message Authentication Code (MAC) is <em>always</em>
 * validated for the cipher text of input archive files.
 * <p>
 * Instances of this base class are immutable.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.0
 */
public abstract class AbstractZipRaesDriver extends JarDriver {
    private static final long serialVersionUID = 8191673749851616843L;

    private final long authenticationTrigger;

    /**
     * Constructs a new abstract ZIP.RAES driver which uses the given byte
     * size to trigger verification of the Message Authentication Code (MAC).
     * Note that the given parameter only affects the authentication of the
     * <em>cipher text</em> in input archives - the <em>cipher key</em> and
     * <em>file length</em> are always authenticated with RAES.
     *
     * @param authenticationTrigger The trigger 
     *        If set to a negative value, the MAC of input
     *        archive files gets <em>never</em> verified.
     *        If set to {@link Long#MAX_VALUE}, the MAC of input
     *        archive files gets <em>always</em> authenticated.
     *        Otherwise, the MAC of input archive files up to this size in
     *        bytes (including the overhead for the RAES wrapper data) only
     *        gets authenticated.
     *        
     */
    protected AbstractZipRaesDriver(
            Icon openIcon,
            Icon closedIcon,
            boolean preambled,
            boolean postambled,
            final int level,
            final long authenticationTrigger) {
        super(openIcon, closedIcon, preambled, postambled, level);
        this.authenticationTrigger = authenticationTrigger;
    }

    /**
     * Returns the authentication trigger provided to the constructor.
     * Note that this method is final for security reasons.
     */
    public final long getAuthenticationTrigger() {
        return authenticationTrigger;
    }

    /**
     * This implementation calls {@link #getRaesParameters}, with which it
     * initializes a new {@link RaesReadOnlyFile}.
     * Next, if the gross file length of the archive is smaller than or equal
     * to the authentication trigger, the MAC authentication on the cipher
     * text is performed.
     * Finally, the <code>RaesReadOnlyFile</code> is passed on to the super
     * class implementation.
     */
    public InputArchive createInputArchive(
            final Archive archive,
            final ReadOnlyFile rof)
    throws IOException {
        final RaesReadOnlyFile rrof;
        try {
            rrof = RaesReadOnlyFile.getInstance(rof, getRaesParameters(archive));
        } catch (RaesKeyException failure) {
            throw new TransientIOException(failure);
        }

        if (rof.length() <= getAuthenticationTrigger()) { // intentionally compares rof, not rrof!
            // Note: If authentication fails, this is reported through some
            // sort of IOException, not a FileNotFoundException!
            // This allows the client to treat the tampered archive like an
            // ordinary file which may be read, written or deleted.
            rrof.authenticate();
        }

        return super.createInputArchive(archive, rrof);
    }

    /**
     * Creates a new ZipEntry, enforcing that the data gets
     * <code>DEFLATED</code> when written, even if copying data from a
     * <code>STORED</code> source entry.
     */
    public ArchiveEntry createArchiveEntry(
            final Archive archive,
            final String entryName,
            final ArchiveEntry template)
    throws CharConversionException {
        final JarEntry entry = (JarEntry) super.createArchiveEntry(
                archive, entryName, template);
        if (entry.getMethod() != JarEntry.DEFLATED) {
            // Enforce deflation for enhanced authentication security.
            entry.setMethod(JarEntry.DEFLATED);
            entry.setCompressedSize(JarEntry.UNKNOWN);
        }
        return entry;
    }

    /**
     * This implementation calls {@link #getRaesParameters}, with which it
     * initializes a new {@link RaesOutputStream}, and finally passes the
     * resulting stream to the super class implementation.
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

        return super.createOutputArchive(archive, ros, source);
    }

    /**
     * Returns the {@link RaesParameters} for the given canonical path name.
     * 
     * @param archive The abstract archive representation which TrueZIP's
     *        internal <code>ArchiveController</code> is processing
     *        - never <code>null</code>.
     *
     * @return The {@link RaesParameters} to use for accessing the
     *         prospective RAES encrypted ZIP file.
     */
    public RaesParameters getRaesParameters(Archive archive) {
        return new KeyManagerRaesParameters(archive.getPath());
    }
}
