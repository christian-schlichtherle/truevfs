/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.tar;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOption.STORE;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;

/**
 * An archive driver for XZ compressed TAR files (TAR.XZ).
 * <p>
 * Subclasses must be thread-safe and should be immutable!
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public class TarXZDriver extends TarDriver {

    public TarXZDriver(IOPoolProvider provider) {
        super(provider);
    }

    /**
     * The buffer size used for reading and writing.
     * Optimized for performance.
     */
    public static final int BUFFER_SIZE = Streams.BUFFER_SIZE;

    /**
     * Returns the size of the I/O buffer.
     * <p>
     * The implementation in the class {@link TarXZDriver} returns
     * {@link #BUFFER_SIZE}.
     *
     * @return The size of the I/O buffer.
     */
    public int getBufferSize() {
        return BUFFER_SIZE;
    }

    /**
     * Returns the compression level to use when writing an XZ output stream.
     * <p>
     * The implementation in the class {@link TarXZDriver} returns
     * {@link LZMA2Options#PRESET_DEFAULT}.
     * 
     * @return The compression level to use when writing a XZ output stream.
     */
    public int getPreset() {
        return LZMA2Options.PRESET_DEFAULT;
    }

    /**
     * Sets {@link FsOutputOption#STORE} in {@code options} before
     * forwarding the call to {@code controller}.
     */
    @Override
    public OutputSocket<?> getOutputSocket( FsController<?> controller,
                                            FsEntryName name,
                                            BitField<FsOutputOption> options,
                                            @CheckForNull Entry template) {
        return controller.getOutputSocket(name, options.set(STORE), template);
    }

    @Override
    protected TarInputShop newTarInputShop(FsModel model, InputStream in)
    throws IOException {
        return super.newTarInputShop(model,
                new XZInputStream(
                    new BufferedInputStream(in, getBufferSize())));
    }

    @Override
    protected TarOutputShop newTarOutputShop(
            final FsModel model,
            final OutputStream out,
            final TarInputShop source)
    throws IOException {
        return super.newTarOutputShop(model,
                new FixedXZOutputStream(
                    new FixedBufferedOutputStream(out, getBufferSize()),
                    new LZMA2Options(getPreset())),
                source);
    }

    private static final class FixedXZOutputStream extends XZOutputStream {
        final FixedBufferedOutputStream out;

        private FixedXZOutputStream(
                final FixedBufferedOutputStream out,
                final LZMA2Options options)
        throws IOException {
            super(out, options);
            this.out = out;
        }

        @Override
        public void close() throws IOException {
            // Workaround for super class implementation which remembers and
            // rethrows any IOException thrown by the decorated output stream.
            // Unfortunately, this doesn't work with TrueZIP's
            // FsControllerException, which is an IOException.
            // TODO: Remove all this in TrueVFS. TrueVFS uses a
            // ControlFlowException instead, which is a RuntimeException and
            // should not interfere with the super class implementation in this
            // way.
            out.ignoreClose = true;
            super.close();
            out.ignoreClose = false;
            out.close();
        }
    } // FixedXZOutputStream
}
