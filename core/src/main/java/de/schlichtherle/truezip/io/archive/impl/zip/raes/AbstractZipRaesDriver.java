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

package de.schlichtherle.truezip.io.archive.impl.zip.raes;

import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.Type;
import de.schlichtherle.truezip.crypto.io.raes.KeyManagerRaesParameters;
import de.schlichtherle.truezip.crypto.io.raes.RaesKeyException;
import de.schlichtherle.truezip.crypto.io.raes.RaesOutputStream;
import de.schlichtherle.truezip.crypto.io.raes.RaesParameters;
import de.schlichtherle.truezip.crypto.io.raes.RaesReadOnlyFile;
import de.schlichtherle.truezip.io.archive.ArchiveDescriptor;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.output.ArchiveOutput;
import de.schlichtherle.truezip.io.archive.driver.TransientIOException;
import de.schlichtherle.truezip.io.archive.impl.zip.JarDriver;
import de.schlichtherle.truezip.io.archive.impl.zip.JarEntry;
import de.schlichtherle.truezip.io.archive.impl.zip.ZipEntry;
import de.schlichtherle.truezip.io.archive.impl.zip.ZipInput;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.OutputStream;

import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.UNKNOWN;
import static de.schlichtherle.truezip.io.zip.ZipEntry.DEFLATED;

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
            boolean preambled,
            boolean postambled,
            final int level,
            final long authenticationTrigger) {
        super(preambled, postambled, level);
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
     * Finally, the {@code RaesReadOnlyFile} is passed on to the super
     * class implementation.
     */
    @Override
    public ZipInput newArchiveInput(
            final ArchiveDescriptor archive,
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
        return super.newArchiveInput(archive, rrof);
    }

    /**
     * Creates a new {@link JarEntry}, enforcing that the data gets
     * {@code DEFLATED} when written, even if copying data from a
     * {@code STORED} source entry.
     * This feature strengthens the security of the authentication process.
     */
    @Override
    public ZipEntry newArchiveEntry(
            final String path,
            final Type type,
            final ArchiveEntry template)
    throws CharConversionException {
        final JarEntry entry = (JarEntry) super.newArchiveEntry(path, type, template);
        if (entry.getMethod() != DEFLATED) {
            // Enforce deflation for enhanced authentication security.
            entry.setMethod(DEFLATED);
            entry.setCompressedSize(UNKNOWN);
        }
        return entry;
    }

    /**
     * The implementation in this class calls {@link #getRaesParameters}
     * and decorates the given {@link OutputStream} with a new
     * {@link RaesOutputStream} before passing the result to the super class
     * implementation.
     *
     * @param archive The archive to write.
     * @param out The {@link OutputStream} to decorate with a
     *        {@link RaesOutputStream}.
     * @param source The source from which archive entries will be copied to
     *        the destination.
     */
    @Override
    public ArchiveOutput newArchiveOutput(
            final ArchiveDescriptor archive,
            final OutputStream out,
            final ZipInput source)
    throws IOException {
        final RaesOutputStream ros;
        try {
            ros = RaesOutputStream.getInstance(out, getRaesParameters(archive));
        } catch (RaesKeyException failure) {
            throw new TransientIOException(failure);
        }
        return super.newArchiveOutput(archive, ros, source);
    }

    /**
     * Returns the {@link RaesParameters} for the given canonical path name.
     * 
     * @param archive The abstract archive representation which TrueZIP's
     *        internal {@code ArchiveController} is processing
     *        - never {@code null}.
     *
     * @return The {@link RaesParameters} to use for accessing the
     *         prospective RAES encrypted ZIP file.
     */
    public RaesParameters getRaesParameters(ArchiveDescriptor archive) {
        return new KeyManagerRaesParameters(archive.getMountPoint());
    }
}
