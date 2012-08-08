/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.zip.raes;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.CheckForNull;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.Immutable;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.comp.zip.driver.JarDriver;
import net.java.truevfs.comp.zip.driver.JarDriverEntry;
import net.java.truevfs.comp.zip.driver.ZipInputService;
import net.java.truevfs.comp.zip.driver.ZipOutputService;
import net.java.truevfs.driver.zip.raes.crypto.RaesOutputStream;
import net.java.truevfs.driver.zip.raes.crypto.RaesParameters;
import net.java.truevfs.driver.zip.raes.crypto.RaesReadOnlyChannel;
import net.java.truevfs.kernel.spec.FsAccessOption;
import static net.java.truevfs.kernel.spec.FsAccessOption.*;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsInputSocketSource;
import net.java.truevfs.kernel.spec.FsModel;
import net.java.truevfs.kernel.spec.FsNodeName;
import net.java.truevfs.kernel.spec.FsOutputSocketSink;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.Entry.Type;
import net.java.truevfs.kernel.spec.cio.InputService;
import net.java.truevfs.kernel.spec.cio.MultiplexingOutputService;
import net.java.truevfs.kernel.spec.cio.OutputService;

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
     * Since TrueVFS 7.3, the implementation in the class {@link ZipRaesDriver}
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
     * {@code new KeyManagerRaesParameters(getKeyManagerContainer().getKeyManager(AesPbeParameters.class), mountPointUri(model))}.
     * 
     * @param  model the file system model.
     * @return The RAES parameters for the given file system model.
     */
    protected RaesParameters raesParameters(FsModel model) {
        return new KeyManagerRaesParameters(getKeyManagerContainer(),
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
     * length getKeyManager authenticated.
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
    public final boolean check(JarDriverEntry local, ZipInputService<JarDriverEntry> input) {
        // Optimization: If the cipher text alias the encrypted ZIP file is
        // smaller than the authentication trigger, then its entire cipher text
        // has already been authenticated by {@link ZipRaesDriver#zipInput}.
        // Hence, checking the CRC-32 value of the entry is redundant.
        return input.length() > getAuthenticationTrigger();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link ZipRaesDriver} decorates the
     * given controller with a package private controller which keeps track of
     * the encryption keys.
     * This should getKeyManager overridden in order to return just {@code controller} if
     * and only if you are overriding
     * {@link #raesParameters(FsModel)}, too, and do not want to use the
     * locatable key manager to resolve passwords for RAES encryption.
     */
    @Override
    public FsController decorate(FsController controller) {
        return new ZipRaesKeyController(controller, this);
    }

    @Override
    protected ZipInputService<JarDriverEntry> newZipInput(
            final FsModel model,
            final FsInputSocketSource source)
    throws IOException {
        final class Source extends FsInputSocketSource {
            Source() { super(source); }

            @Override
            public SeekableByteChannel channel() throws IOException {
                final RaesReadOnlyChannel channel = RaesReadOnlyChannel
                        .create(raesParameters(model), source);
                try {
                    if (channel.size() <= getAuthenticationTrigger())
                        channel.authenticate();
                    return channel;
                } catch (final Throwable ex) {
                    try {
                        channel.close();
                    } catch (final IOException ex2) {
                        ex.addSuppressed(ex2);
                    }
                    throw ex;
                }
            }
        }
        return new ZipInputService<>(model, new Source(), this);
    }

    @Override
    protected OutputService<JarDriverEntry> newOutput(
            final FsModel model,
            final FsOutputSocketSink sink,
            final @CheckForNull @WillNotClose InputService<JarDriverEntry> input)
    throws IOException {
        final ZipInputService<JarDriverEntry> zis = (ZipInputService<JarDriverEntry>) input;
        return new MultiplexingOutputService<>(getPool(),
                new ZipOutputService<>(model, new RaesSocketSink(model, sink), zis, this));
    }

    @SuppressWarnings("PackageVisibleInnerClass")
    final class RaesSocketSink extends FsOutputSocketSink {
        private final FsModel model;
        private final FsOutputSocketSink sink;

        RaesSocketSink(final FsModel model, final FsOutputSocketSink sink) {
            super(sink);
            this.model = model;
            this.sink = sink;
        }

        @Override
        public OutputStream stream() throws IOException {
            return RaesOutputStream.create(raesParameters(model), sink);
        }

        @Override
        public SeekableByteChannel channel() throws IOException {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Sets {@link FsAccessOption#STORE} in {@code options} before
     * forwarding the call to {@code controller}.
     */
    @Override
    protected final FsOutputSocketSink sink(
            BitField<FsAccessOption> options,
            final FsController controller,
            final FsNodeName name) {
        // Leave FsAccessOption.COMPRESS untouched - the driver shall be given
        // opportunity to apply its own preferences to sort out such a conflict.
        options = options.set(STORE);
        // The RAES file format cannot support GROWing.
        options = options.clear(GROW);
        return new FsOutputSocketSink(options,
                controller.output(options, name, null));
    }

    /**
     * Returns a new {@link ZipDriverEntry}, enforcing that the data gets
     * {@code DEFLATED} when written, even if copying data from a
     * {@code STORED} source entry.
     * This feature strengthens the security level of the authentication
     * process and inhibits the use of an unencrypted temporary I/O entry
     * (usually a temporary file) in case the sink is not copied from a file
     * system entry as its input.
     * <p>
     * Furthermore, the method {@link ZipDriverEntry#clearEncryption()} is
     * called in order to prevent adding a redundant encryption layer for the
     * individual ZIP entry because this would confuse users, increase the size
     * of the resulting archive file and unecessarily heat the CPU.
     */
    @Override
    public JarDriverEntry newEntry(
            final BitField<FsAccessOption> options,
            final String name,
            final Type type,
            final @CheckForNull Entry template) {
        final JarDriverEntry entry
                = super.newEntry(options.set(COMPRESS), name, type, template);
        // Fix for http://java.net/jira/browse/TRUEZIP-176 :
        // Entry level encryption is enabled if mknod.getKeyManager(ENCRYPTED) is true
        // OR template is an instance of ZipEntry
        // AND ((ZipEntry) template).isEncrypted() is true.
        // Now switch off entry level encryption because encryption is already
        // provided by the RAES wrapper file format.
        entry.clearEncryption();
        return entry;
    }
}
