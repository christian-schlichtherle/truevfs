/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zipdriver;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import javax.annotation.CheckForNull;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truecommons.cio.*;
import net.java.truecommons.cio.Entry.Access;
import net.java.truecommons.cio.Entry.Size;
import static net.java.truecommons.cio.Entry.Size.DATA;
import static net.java.truecommons.cio.Entry.UNKNOWN;
import net.java.truecommons.io.DecoratingOutputStream;
import net.java.truecommons.io.DisconnectingOutputStream;
import net.java.truecommons.io.InputException;
import net.java.truecommons.io.Streams;
import net.java.truecommons.shed.CompoundIterator;
import net.java.truecommons.shed.SuppressedExceptionBuilder;
import net.java.truevfs.comp.zip.AbstractZipOutputStream;
import net.java.truevfs.comp.zip.ZipCryptoParameters;
import static net.java.truevfs.comp.zip.ZipEntry.STORED;
import static net.java.truevfs.kernel.spec.FsAccessOption.GROW;
import net.java.truevfs.kernel.spec.FsModel;
import net.java.truevfs.kernel.spec.FsOutputSocketSink;

/**
 * An output service for writing ZIP files.
 * This output service can only write one entry concurrently.
 *
 * @param  <E> the type of the ZIP driver entries.
 * @see    ZipInputService
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public final class ZipOutputService<E extends AbstractZipDriverEntry>
extends AbstractZipOutputStream<E> implements OutputService<E> {

    private final FsModel model;
    private final AbstractZipDriver<E> driver;
    private @CheckForNull IoBuffer postamble;
    private @CheckForNull E bufferedEntry;
    private ZipCryptoParameters param;

    @CreatesObligation
    public ZipOutputService(
            final FsModel model,
            final FsOutputSocketSink sink,
            final @CheckForNull @WillNotClose ZipInputService<E> source,
            final AbstractZipDriver<E> driver)
    throws IOException {
        super(  sink,
                null != source && sink.getOptions().get(GROW) ? source : null,
                driver);
        this.driver = driver;
        try {
            this.model = Objects.requireNonNull(model);
            if (null != source) {
                if (!sink.getOptions().get(GROW)) {
                    // Retain comment and preamble of input ZIP archive.
                    super.setComment(source.getComment());
                    if (0 < source.getPreambleLength()) {
                        try (final InputStream in = source.getPreambleInputStream()) {
                            Streams.cat(in, source.offsetsConsiderPreamble() ? this : out);
                        }
                    }
                }
                // Retain postamble of input ZIP file.
                if (0 < source.getPostambleLength()) {
                    this.postamble = getPool().allocate();
                    Streams.copy(   source.getPostambleInputStream(),
                                    this.postamble.output().stream(null));
                }
            }
        } catch (final Throwable ex) {
            try {
                super.close();
            } catch (final Throwable ex2) {
                ex.addSuppressed(ex2);
            }
            throw ex;
        }
    }

    /**
     * Returns the file system model provided to the constructor.
     *
     * @return The file system model provided to the constructor.
     */
    public FsModel getModel() {
        return model;
    }

    private IoBufferPool getPool() {
        return driver.getPool();
    }

    @Override
    protected ZipCryptoParameters getCryptoParameters() {
        ZipCryptoParameters param = this.param;
        if (null == param)
            this.param = param = driver.zipCryptoParameters(this);
        return param;
    }

    @Override
    public int size() {
        return super.size() + (null != this.bufferedEntry ? 1 : 0);
    }

    @Override
    public Iterator<E> iterator() {
        final E bufferedEntry = this.bufferedEntry;
        if (null == bufferedEntry) return super.iterator();
        return new CompoundIterator<>(
                super.iterator(),
                Collections.singletonList(bufferedEntry).iterator());
    }

    @Override
    public @CheckForNull E entry(final String name) {
        E entry = super.entry(name);
        if (null != entry) return entry;
        entry = this.bufferedEntry;
        return null != entry && name.equals(entry.getName()) ? entry : null;
    }

    @Override
    public OutputSocket<E> output(final E local) { // local target
        Objects.requireNonNull(local);
        final class Output extends AbstractOutputSocket<E> {
            @Override
            public E target() {
                return local;
            }

            @Override
            public OutputStream stream(InputSocket<? extends Entry> peer)
            throws IOException {
                if (isBusy()) throw new OutputBusyException(local.getName());
                if (local.isDirectory()) {
                    updateProperties(local, DirectoryTemplate.INSTANCE);
                    return new EntryOutputStream(local, false);
                }
                final boolean rdc = updateProperties(local, target(peer));
                if (STORED == local.getMethod()) {
                    if (UNKNOWN == local.getCrc()
                            || UNKNOWN == local.getSize()
                            || UNKNOWN == local.getCompressedSize()) {
                        assert !rdc : "The CRC-32, size and compressed size properties must be set when using RDC!";
                        return new BufferedEntryOutputStream(local);
                    }
                }
                return new EntryOutputStream(local, rdc);
            }
        }
        return new Output();
    }

    boolean updateProperties(
            final E local,
            final @CheckForNull Entry peer) {
        boolean rdc = false;
        if (UNKNOWN == local.getTime())
            local.setTime(System.currentTimeMillis());
        if (null != peer) {
            if (UNKNOWN == local.getSize())
                local.setSize(peer.getSize(DATA));
            if (peer instanceof AbstractZipDriverEntry) {
                // Set up entry attributes for Raw Data Copying (RDC).
                final AbstractZipDriverEntry zpeer = (AbstractZipDriverEntry) peer;
                rdc = driver.rdc(this, local, zpeer);
                if (rdc) {
                    local.setPlatform(zpeer.getPlatform());
                    local.setEncrypted(zpeer.isEncrypted());
                    local.setMethod(zpeer.getMethod());
                    local.setCrc(zpeer.getCrc());
                    local.setSize(zpeer.getSize());
                    local.setCompressedSize(zpeer.getCompressedSize());
                    local.setExtra(zpeer.getExtra());
                }
            }
        }
        if (0 == local.getSize()) {
            rdc = false;
            local.clearEncryption();
            local.setMethod(STORED);
            local.setCrc(0);
            local.setCompressedSize(0);
        }
        return rdc;
    }

    private static final class DirectoryTemplate implements Entry {
        static final DirectoryTemplate INSTANCE = new DirectoryTemplate();

        @Override
        public String getName() {
            return "/";
        }

        @Override
        public long getSize(Size type) {
            return 0;
        }

        @Override
        public long getTime(Access type) {
            return UNKNOWN;
        }

        @Override
        public Boolean isPermitted(Access type, Entity entity) {
            return null;
        }
    } // DirectoryTemplate

    /**
     * Returns whether this ZIP output service is busy writing an archive entry
     * or not.
     *
     * @return Whether this ZIP output service is busy writing an archive entry
     *         or not.
     */
    @Override
    public boolean isBusy() {
        return super.isBusy() || null != this.bufferedEntry;
    }

    /**
     * Retains the postamble of the source source ZIP file, if any.
     */
    @Override
    public void close() throws IOException {
        super.finish();
        final IoBuffer postamble = this.postamble;
        if (null != postamble) {
            this.postamble = null;
            final InputSocket<?> input = postamble.input();
            Throwable ex = null;
            try {
                try (final InputStream in = input.stream(null)) {
                    // If the output ZIP file differs in length from the
                    // input ZIP file then pad the output to the next four
                    // byte boundary before appending the postamble.
                    // This might be required for self extracting files on
                    // some platforms, e.g. Windows x86.
                    final long ol = length();
                    final long ipl = input.target().getSize(DATA);
                    if ((ol + ipl) % 4 != 0)
                        write(new byte[4 - (int) (ol % 4)]);
                    Streams.cat(in, this);
                }
            } catch (final Throwable ex2) {
                ex = ex2;
                throw ex2;
            } finally {
                try {
                    postamble.release();
                } catch (final Throwable ex2) {
                    if (null == ex)
                        throw ex2;
                    ex.addSuppressed(ex2);
                }
            }
        }
        super.close();
    }

    /**
     * This entry output stream writes directly to this ZIP output service.
     * It can only be used if this output service is not currently busy with
     * writing another entry and the entry holds enough information to write
     * the entry header.
     * These preconditions are checked by
     * {@link #output(AbstractZipDriverEntry)}.
     */
    @CleanupObligation
    private final class EntryOutputStream extends DisconnectingOutputStream {
        boolean closed;

        @CreatesObligation
        EntryOutputStream(final E local, final boolean rdc)
        throws IOException {
            super(ZipOutputService.this);
            putNextEntry(local, !rdc);
        }

        @Override
        public boolean isOpen() {
            return !closed;
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            if (closed) return;
            closed = true;
            closeEntry();
        }
    } // EntryOutputStream

    /**
     * This entry output stream writes the ZIP archive entry to an
     * {@linkplain IoBuffer I/O buffer}.
     * When the stream gets closed, the I/O buffer is then copied to this
     * output service and finally deleted.
     */
    @CleanupObligation
    private final class BufferedEntryOutputStream
    extends DecoratingOutputStream {
        final IoBuffer buffer;
        final E local;
        boolean closed;

        @CreatesObligation
        BufferedEntryOutputStream(final E local)
        throws IOException {
            assert STORED == local.getMethod();
            this.local = local;
            final IoBuffer buffer = this.buffer = getPool().allocate();
            try {
                this.out = new CheckedOutputStream(
                        buffer.output().stream(null), new CRC32());
            } catch (final Throwable ex) {
                try {
                    buffer.release();
                } catch (final Throwable ex2) {
                    ex.addSuppressed(ex2);
                }
                throw ex;
            }
            bufferedEntry = local;
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            if (closed) return;
            closed = true;
            bufferedEntry = null;
            out.close();
            updateProperties();
            storeBuffer();
        }

        void updateProperties() {
            final E local = this.local;
            final IoBuffer buffer = this.buffer;
            local.setCrc(((CheckedOutputStream) out).getChecksum().getValue());
            final long length = buffer.getSize(DATA);
            local.setSize(length);
            local.setCompressedSize(length);
            ZipOutputService.this.updateProperties(local, buffer);
        }

        void storeBuffer() throws IOException {
            final IoBuffer buffer = this.buffer;
            final SuppressedExceptionBuilder<IOException>
                    builder = new SuppressedExceptionBuilder<>();
            try (final InputStream in = buffer.input().stream(null)) {
                final ZipOutputService<E> zos = ZipOutputService.this;
                zos.putNextEntry(local, true);
                try {
                    Streams.cat(in, zos);
                } catch (final InputException ex) { // NOT IOException!
                    builder.warn(ex);
                }
                try {
                    zos.closeEntry();
                } catch (final IOException ex) {
                    builder.warn(ex);
                }
            } catch (final IOException ex) {
                builder.warn(ex);
            } finally {
                try {
                    buffer.release();
                } catch (final IOException ex) {
                    builder.warn(ex);
                }
            }
            builder.check();
        }
    } // BufferedEntryOutputStream
}
