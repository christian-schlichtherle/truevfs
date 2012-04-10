/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.raes;

import de.truezip.driver.zip.*;
import de.truezip.driver.zip.raes.crypto.RaesOutputStream;
import de.truezip.driver.zip.raes.crypto.RaesParameters;
import de.truezip.driver.zip.raes.crypto.RaesReadOnlyChannel;
import de.truezip.kernel.FsAccessOption;
import static de.truezip.kernel.FsAccessOption.*;
import de.truezip.kernel.FsController;
import de.truezip.kernel.FsEntryName;
import de.truezip.kernel.FsModel;
import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.cio.Entry.Type;
import de.truezip.kernel.cio.MultiplexingOutputService;
import de.truezip.kernel.cio.OutputService;
import de.truezip.kernel.io.AbstractSink;
import de.truezip.kernel.io.AbstractSource;
import de.truezip.kernel.io.Source;
import de.truezip.kernel.util.BitField;
import de.truezip.key.param.AesPbeParameters;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.CheckForNull;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.Immutable;

/**
 * An abstract archive driver for RAES encrypted ZIP files which optionally
 * authenticates the cipher data of the input archive files presented to it.
 * <p>
 * Sub-classes must be thread-safe and should be immutable!
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class ZipRaesDriver extends JarDriver {

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
     * Returns the RAES parameters for the given file system model.
     * <p>
     * The implementation in the class {@link ZipRaesDriver} returns
     * {@code new KeyManagerRaesParameters(getKeyManagerProvider().get(AesPbeParameters.class), mountPointUri(model))}.
     * 
     * @param  model the file system model.
     * @return The RAES parameters for the given file system model.
     */
    protected RaesParameters raesParameters(FsModel model) {
        return new KeyManagerRaesParameters(
                getKeyManagerProvider().get(AesPbeParameters.class),
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
    protected final boolean check(ZipInputService input, ZipDriverEntry entry) {
        // Optimization: If the cipher text alias the encrypted ZIP file is
        // smaller than the authentication trigger, then its entire cipher text
        // has already been authenticated by {@link ZipRaesDriver#newZipInputService}.
        // Hence, checking the CRC-32 value of the entry is redundant.
        return input.length() > getAuthenticationTrigger();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link ZipRaesDriver} decorates the
     * given controller with a package private controller which keeps track of
     * the encryption keys.
     * This should get overridden in order to return just {@code controller} if
     * and only if you are overriding
     * {@link #raesParameters(FsModel)}, too, and do not want to use the
     * locatable key manager to resolve passwords for RAES encryption.
     */
    @Override
    public <M extends FsModel> FsController<M>
    decorate(FsController<M> controller) {
        return new ZipRaesKeyController<>(controller, this);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in {@link ZipRaesDriver} calls
     * {@link #raesParameters}, with which it initializes a new
     * {@link RaesReadOnlyChannel}.
     * Next, if the gross file length of the archive is smaller than or equal
     * to the authentication trigger, the MAC authentication on the cipher
     * text is performed.
     * Finally, the {@link RaesReadOnlyChannel} is passed on to the super class
     * implementation.
     */
    @Override
    protected ZipInputService newZipInputService(
            final FsModel model,
            final Source source)
    throws IOException {
        final class Source extends AbstractSource {
            @Override
            public SeekableByteChannel channel() throws IOException {
                final RaesReadOnlyChannel channel = RaesReadOnlyChannel
                        .create(raesParameters(model), source);
                try {
                    if (channel.size() <= getAuthenticationTrigger())
                        channel.authenticate();
                } catch (final Throwable ex) {
                    try {
                        channel.close();
                    } catch (final Throwable ex2) {
                        ex.addSuppressed(ex2);
                    }
                    throw ex;
                }
                return channel;
            }
        } // Source

        return new ZipInputService(model, new Source(), this);
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    protected OutputService<ZipDriverEntry> newOutputService(
            final FsModel model, final OptionOutputSocket output, @CheckForNull
                                                                  @WillNotClose
    final ZipInputService source)
    throws IOException {
        final class Sink extends AbstractSink {
            @Override
            public OutputStream stream() throws IOException {
                return RaesOutputStream.create(raesParameters(model), output);
            }
        } // Sink

        return new MultiplexingOutputService<>(
                new ZipOutputService(model, new Sink(), source, this),
                getIOPool());
    }

    /**
     * Sets {@link FsAccessOption#STORE} in {@code options} before
     * forwarding the call to {@code controller}.
     */
    @Override
    protected final OptionOutputSocket
    getOutputSocket(
            final FsController<?> controller,
            final FsEntryName name,
            BitField<FsAccessOption> options) {
        options = options.clear(GROW);
        // Leave FsAccessOption.COMPRESS untouched - the controller shall have the
        // opportunity to apply its own preferences to sort out such a conflict.
        return new OptionOutputSocket(
                controller.getOutputSocket(name, options.set(STORE), null),
                options); // use modified options!
    }

    /**
     * Returns a new {@link ZipDriverEntry}, enforcing that the data gets
     * {@code DEFLATED} when written, even if copying data from a
     * {@code STORED} source entry.
     * This feature strengthens the security level of the authentication
     * process and inhibits the use of an unencrypted temporary I/O entry
     * (usually a temporary file) in case the output is not copied from a file
     * system entry as its input.
     * <p>
     * Furthermore, the method {@link ZipDriverEntry#clearEncryption()} is
     * called in order to prevent adding a redundant encryption layer for the
     * individual ZIP entry because this would confuse users, increase the size
     * of the resulting archive file and unecessarily heat the CPU.
     */
    @Override
    public ZipDriverEntry newEntry(
            final String path,
            final Type type,
            final Entry template,
            final BitField<FsAccessOption> mknod) {
        final ZipDriverEntry entry
                = super.newEntry(path, type, template, mknod.set(COMPRESS));
        // Fix for http://java.net/jira/browse/TRUEZIP-176 :
        // Entry level encryption is enabled if mknod.get(ENCRYPTED) is true
        // OR template is an instance of ZipEntry
        // AND ((ZipEntry) template).isEncrypted() is true.
        // Now switch off entry level encryption because encryption is already
        // provided by the RAES wrapper file format.
        entry.clearEncryption();
        return entry;
    }
}
