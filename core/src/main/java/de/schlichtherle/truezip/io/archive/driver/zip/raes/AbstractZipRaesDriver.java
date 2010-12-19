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
package de.schlichtherle.truezip.io.archive.driver.zip.raes;

import de.schlichtherle.truezip.io.filesystem.FileSystemController;
import de.schlichtherle.truezip.io.filesystem.MountPoint;
import de.schlichtherle.truezip.io.archive.model.ArchiveModel;
import de.schlichtherle.truezip.io.socket.LazyOutputSocket;
import de.schlichtherle.truezip.io.socket.DecoratingOutputSocket;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.io.socket.DecoratingInputSocket;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.InputShop;
import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.entry.Entry.Type;
import de.schlichtherle.truezip.crypto.io.raes.KeyManagerRaesParameters;
import de.schlichtherle.truezip.crypto.io.raes.RaesKeyException;
import de.schlichtherle.truezip.crypto.io.raes.RaesOutputStream;
import de.schlichtherle.truezip.crypto.io.raes.RaesParameters;
import de.schlichtherle.truezip.crypto.io.raes.RaesReadOnlyFile;
import de.schlichtherle.truezip.io.socket.OutputShop;
import de.schlichtherle.truezip.io.TabuFileException;
import de.schlichtherle.truezip.io.archive.driver.zip.JarDriver;
import de.schlichtherle.truezip.io.archive.driver.zip.JarEntry;
import de.schlichtherle.truezip.io.archive.driver.zip.ZipEntry;
import de.schlichtherle.truezip.io.archive.driver.zip.ZipInputShop;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.OutputStream;

import static de.schlichtherle.truezip.io.zip.ZipEntry.*;

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

    @Override
    @NonNull
    public FileSystemController<? extends ArchiveModel> newController(
            @NonNull MountPoint mountPoint,
            @NonNull FileSystemController<?> parent) {
        return new KeyManagerArchiveController(
                super.newController(mountPoint, parent), this);
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
    public ZipInputShop newInputShop(
            final ArchiveModel model,
            final InputSocket<?> target)
    throws IOException {
        class Input extends DecoratingInputSocket<Entry> {
            Input() {
                super(target);
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                final ReadOnlyFile rof = super.newReadOnlyFile();
                try {
                    final RaesReadOnlyFile rrof;
                    try {
                        rrof = RaesReadOnlyFile.getInstance(
                                rof, getRaesParameters(model));
                    } catch (RaesKeyException ex) {
                        throw new TabuFileException(ex);
                    }
                    if (rof.length() <= getAuthenticationTrigger()) { // intentionally compares rof, not rrof!
                        // Note: If authentication fails, this is reported through some
                        // sort of IOException, not a FileNotFoundException!
                        // This allows the client to treat the tampered archive like an
                        // ordinary file which may be read, written or deleted.
                        rrof.authenticate();
                    }
                    return rrof;
                } catch (IOException ex) {
                    rof.close();
                    throw ex;
                }
            }
        } // class Input

        return super.newInputShop(model, new Input());
    }

    /**
     * Creates a new {@link JarEntry}, enforcing that the data gets
     * {@code DEFLATED} when written, even if copying data from a
     * {@code STORED} source entry.
     * This feature strengthens the security of the authentication process.
     */
    @Override
    public JarEntry newEntry(
            final String path,
            final Type type,
            final Entry template)
    throws CharConversionException {
        final JarEntry entry = super.newEntry(path, type, template);
        if (entry.getMethod() != DEFLATED) {
            // Enforce deflation for enhanced authentication security.
            entry.setMethod(DEFLATED);
            entry.setCompressedSize(UNKNOWN);
        }
        return entry;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link AbstractZipRaesDriver} calls
     * {@link #getRaesParameters} for authentication.
     */
    @Override
    public OutputShop<ZipEntry> newOutputShop(
            final ArchiveModel model,
            final OutputSocket<?> target,
            final InputShop<ZipEntry> source)
    throws IOException {
        class Output extends DecoratingOutputSocket<Entry> {
            Output() {
                super(target);
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                final OutputStream out
                        = new LazyOutputSocket<Entry>(getBoundSocket())
                            .newOutputStream();
                try {
                    try {
                        return RaesOutputStream.getInstance(
                                out, getRaesParameters(model));
                    } catch (RaesKeyException ex) {
                        throw new TabuFileException(ex);
                    }
                } catch (IOException cause) {
                    try {
                        out.close();
                    } catch (IOException ex) {
                        throw (IOException) ex.initCause(cause);
                    }
                    throw cause;
                }
            }
        } // class Output

        return super.newOutputShop(model, new Output(), source);
    }

    /**
     * Returns the {@link RaesParameters} for the given canonical path name.
     * 
     * @param model The abstract archive representation which TrueZIP's
     *        internal {@code ArchiveController} is processing
     *        - never {@code null}.
     * @return The {@link RaesParameters} to use for accessing the
     *         prospective RAES encrypted ZIP file.
     */
    protected RaesParameters getRaesParameters(ArchiveModel model) {
        return new KeyManagerRaesParameters(model.getMountPoint().getUri());
    }
}
