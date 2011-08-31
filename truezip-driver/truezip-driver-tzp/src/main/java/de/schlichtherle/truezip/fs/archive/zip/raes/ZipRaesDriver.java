/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.zip.raes;

import de.schlichtherle.truezip.crypto.raes.RaesOutputStream;
import de.schlichtherle.truezip.crypto.raes.RaesParameters;
import de.schlichtherle.truezip.crypto.raes.RaesReadOnlyFile;
import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.crypto.raes.param.KeyManagerRaesParameters;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import de.schlichtherle.truezip.fs.archive.zip.JarArchiveEntry;
import de.schlichtherle.truezip.fs.archive.zip.JarDriver;
import de.schlichtherle.truezip.fs.archive.zip.OptionOutputSocket;
import de.schlichtherle.truezip.fs.archive.zip.ZipArchiveEntry;
import de.schlichtherle.truezip.fs.archive.zip.ZipInputShop;
import de.schlichtherle.truezip.key.KeyManagerProvider;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import de.schlichtherle.truezip.socket.InputShop;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.LazyOutputSocket;
import de.schlichtherle.truezip.socket.OutputShop;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.OutputStream;
import net.jcip.annotations.Immutable;

/**
 * An abstract archive driver which builds RAES encrypted ZIP files
 * and optionally authenticates the cipher data of the input archive files
 * presented to it.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public abstract class ZipRaesDriver extends JarDriver {

    /**
     * The key manager provider for accessing protected resources (cryptography).
     */
    private final KeyManagerProvider keyManagerProvider;

    /**
     * Constructs a new RAES encrypted ZIP file driver.
     *
     * @param ioPoolProvider the I/O entry pool provider for allocating
     *        temporary I/O entries (buffers).
     * @param keyManagerProvider the key manager provider for accessing
     *        protected resources (cryptography).
     */
    public ZipRaesDriver(   IOPoolProvider ioPoolProvider,
                            final KeyManagerProvider keyManagerProvider) {
        super(ioPoolProvider);
        if (null == keyManagerProvider)
            throw new NullPointerException();
        this.keyManagerProvider = keyManagerProvider;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Since TrueZIP 7.3, the implementation in the class {@link ZipRaesDriver}
     * returns {@code true} for future use.
     * 
     * @return {@code true}
     */
    @Override
    public final boolean getPreambled() {
        return true;
    }

    /**
     * Returns the provider for key managers for accessing protected resources
     * (encryption).
     * <p>
     * The implementation in {@link ZipRaesDriver} simply returns the value of
     * the field {@link #keyManagerProvider}.
     * 
     * @return The provider for key managers for accessing protected resources
     *         (encryption).
     * @since  TrueZIP 7.3.
     */
    @Override
    protected final KeyManagerProvider getKeyManagerProvider() {
        return keyManagerProvider;
    }

    /**
     * Returns the RAES parameters for the given file system model
     * or {@code null} if not available.
     * <p>
     * The implementation in the class {@link ZipRaesDriver} returns
     * {@code new KeyManagerRaesParameters(getKeyManager(), mountPointUri(model))}.
     * 
     * @param  model the file system model.
     * @return The RAES parameters for the given file system model
     *         or {@code null} if not available.
     */
    protected @CheckForNull RaesParameters raesParameters(FsModel model) {
        return new KeyManagerRaesParameters(
                getKeyManagerProvider().get(AesCipherParameters.class),
                mountPointUri(model));
    }

    /**
     * Returns the value of the property {@code authenticationTrigger}.
     * <p>
     * If the cipher text length of an input RAES file is smaller than or equal
     * to this value, then the Hash-based Message Authentication Code (HMAC)
     * for the entire cipher text is computed and verified in order to
     * authenticate the input RAES file.
     * <p>
     * Otherwise, if the cipher text length of an input RAES file is greater
     * than this value, then initially only the cipher key and the cipher text
     * length get authenticated.
     * In addition, whenever an entry is subsequently accessed, then it's
     * CRC-32 value is checked.
     * <p>
     * Consequently, if the value of this property is set to a negative value,
     * then the entire cipher text gets <em>never</em> authenticated (CRC-32
     * checking only), and if set to {@link Long#MAX_VALUE}, then the entire
     * cipher text gets <em>always</em> authenticated (no CRC-32 checking).
     *
     * @return The value of the property {@code authenticationTrigger}.
     */
    protected abstract long getAuthenticationTrigger();

    @Override
    protected final boolean check(ZipInputShop input, ZipArchiveEntry entry) {
        // Optimization: If the cipher text alias the encrypted ZIP file is
        // smaller than the authentication trigger, then its entire cipher text
        // has already been authenticated by {@link ZipRaesDriver#newInputShop}.
        // Hence, checking the CRC-32 value of the entry is redundant.
        return input.length() > getAuthenticationTrigger();
    }

    @Override
    public FsController<?>
    newController(FsModel model, FsController<?> parent) {
        return new ZipRaesController(superNewController(model, parent), this);
    }

    /**
     * Returns a new {@link JarArchiveEntry}, enforcing that the data gets
     * {@code DEFLATED} when written, even if copying data from a
     * {@code STORED} source entry.
     * This feature strengthens the security level of the authentication
     * process and inhibits the use of an unencrypted temporary I/O entry
     * (usually a temporary file) in case the output is not copied from a file
     * system entry as its input.
     * <p>
     * Furthermore, the output option preference {@link FsOutputOption#ENCRYPT}
     * is cleared in order to prevent adding a redundant encryption layer for
     * the individual ZIP entry.
     * This would not have any effect on the security level, but increase the
     * size of the resulting archive file and heat the CPU.
     */
    @Override
    public ZipArchiveEntry newEntry(
            String path,
            Type type,
            Entry template,
            BitField<FsOutputOption> mknod)
    throws CharConversionException {
        return super.newEntry(path, type, template,
                mknod.set(COMPRESS).clear(ENCRYPT));
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in {@link ZipRaesDriver} calls
     * {@link #raesParameters}, with which it initializes a new
     * {@link RaesReadOnlyFile}.
     * Next, if the gross file length of the archive is smaller than or equal
     * to the authentication trigger, the MAC authentication on the cipher
     * text is performed.
     * Finally, the {@link RaesReadOnlyFile} is passed on to the super
     * class implementation.
     */
    @Override
    public final InputShop<ZipArchiveEntry>
    newInputShop(   final FsModel model,
                    final InputSocket<?> input)
    throws IOException {
        final ReadOnlyFile rof = input.newReadOnlyFile();
        try {
            final RaesReadOnlyFile rrof = RaesReadOnlyFile.getInstance(
                    rof, raesParameters(model));
            if (rrof.length() <= getAuthenticationTrigger()) // compare rrof, not rof!
                rrof.authenticate();
            return newInputShop(model, rrof);
        } catch (IOException ex) {
            rof.close();
            throw ex;
        }
    }

    /**
     * Sets {@link FsOutputOption#STORE} in {@code options} before
     * forwarding the call to {@code controller}.
     */
    @Override
    public final OptionOutputSocket getOutputSocket(
            final FsController<?> controller,
            final FsEntryName name,
            BitField<FsOutputOption> options,
            final @CheckForNull Entry template) {
        options = options.clear(GROW);
        // Leave FsOutputOption.COMPRESS untouched - the driver shall be given
        // opportunity to apply its own preferences to sort out such a conflict.
        BitField<FsOutputOption> options2 = options.set(STORE);
        return new OptionOutputSocket(
                controller.getOutputSocket(name, options2, template),
                options); // use modified options!
    }

    @Override
    protected OutputShop<ZipArchiveEntry> newOutputShop(
            final FsModel model,
            final OptionOutputSocket output,
            final @CheckForNull ZipInputShop source)
    throws IOException {
        final OutputStream out = new LazyOutputSocket<Entry>(output)
                .newOutputStream();
        try {
            final RaesOutputStream ros = RaesOutputStream.getInstance(
                    out, raesParameters(model));
            return newOutputShop(model, ros, source);
        } catch (IOException ex) {
            out.close();
            throw ex;
        }
    }
}
