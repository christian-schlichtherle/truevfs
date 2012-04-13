/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip;

import de.truezip.driver.zip.io.RawOutputStream;
import de.truezip.driver.zip.io.ZipCryptoParameters;
import static de.truezip.driver.zip.io.ZipEntry.STORED;
import static de.truezip.driver.zip.io.ZipEntry.UNKNOWN;
import de.truezip.kernel.FsModel;
import static de.truezip.kernel.cio.Entry.Access.WRITE;
import static de.truezip.kernel.cio.Entry.Size.DATA;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.io.DecoratingOutputStream;
import de.truezip.kernel.io.OutputBusyException;
import de.truezip.kernel.io.Sink;
import de.truezip.kernel.io.Streams;
import de.truezip.kernel.util.JointIterator;
import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import javax.annotation.CheckForNull;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An output service for writing ZIP files.
 * This output service can only write one entry concurrently.
 * 
 * @see    ZipInputService
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public final class ZipOutputService
extends RawOutputStream<ZipDriverEntry>
implements OutputService<ZipDriverEntry> {

    private final FsModel model;
    private final ZipDriver driver;
    private @CheckForNull IOBuffer<?> postamble;
    private @CheckForNull ZipDriverEntry bufferedEntry;
    private ZipCryptoParameters param;

    @CreatesObligation
    public ZipOutputService(
            final FsModel model,
            final Sink sink,
            final @CheckForNull @WillNotClose ZipInputService source,
            final ZipDriver driver)
    throws IOException {
        super(  sink,
                null != source && source.isAppendee() ? source : null,
                driver);
        this.driver = driver;
        try {
            if (null == (this.model = model))
                throw new NullPointerException();
            if (null != source) {
                if (!source.isAppendee()) {
                    // Retain comment and preamble of input ZIP archive.
                    super.setComment(source.getComment());
                    if (0 < source.getPreambleLength()) {
                        final InputStream in = source.getPreambleInputStream();
                        Throwable ex = null;
                        try {
                            Streams.cat(in, source.offsetsConsiderPreamble() ? this : out);
                        } catch (final Throwable ex2) {
                            ex = ex2;
                            throw ex2;
                        } finally {
                            try {
                                in.close();
                            } catch (final IOException ex2) {
                                if (null == ex)
                                    throw ex2;
                                ex.addSuppressed(ex2);
                            }
                        }
                    }
                }
                // Retain postamble of input ZIP file.
                if (0 < source.getPostambleLength()) {
                    this.postamble = getIOPool().allocate();
                    Streams.copy(   source.getPostambleInputStream(),
                                    this.postamble.outputSocket().stream());
                }
            }
        } catch (final Throwable ex) {
            try {
                super.close();
            } catch (final IOException ex2) {
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

    private IOPool<?> getIOPool() {
        return driver.getIOPool();
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
    public Iterator<ZipDriverEntry> iterator() {
        final ZipDriverEntry tempEntry = this.bufferedEntry;
        if (null == tempEntry)
            return super.iterator();
        return new JointIterator<>(
                super.iterator(),
                Collections.singletonList(tempEntry).iterator());
    }

    @Override
    public @CheckForNull ZipDriverEntry entry(final String name) {
        ZipDriverEntry entry = super.entry(name);
        if (null != entry)
            return entry;
        entry = this.bufferedEntry;
        return null != entry && name.equals(entry.getName()) ? entry : null;
    }

    @Override
    public OutputSocket<ZipDriverEntry> outputSocket(final ZipDriverEntry entry) { // local target
        if (null == entry)
            throw new NullPointerException();

        final class Output extends OutputSocket<ZipDriverEntry> {
            @Override
            public ZipDriverEntry localTarget() {
                return entry;
            }

            @Override
            public OutputStream stream()
            throws IOException {
                if (isBusy())
                    throw new OutputBusyException(entry.getName());
                final Entry peer;
                final long size;
                boolean process = true;
                if (entry.isDirectory()) {
                    entry.setMethod(STORED);
                    entry.setCrc(0);
                    entry.setCompressedSize(0);
                    entry.setSize(0);
                    return new EntryOutputStream(entry, true);
                } else if (null != (peer = peerTarget())
                        && UNKNOWN != (size = peer.getSize(DATA))) {
                    entry.setSize(size);
                    if (peer instanceof ZipDriverEntry) {
                        // Set up entry attributes for Raw Data Copying (RDC).
                        // A preset method in the entry takes priority.
                        // The ZIP.RAES drivers use this feature to enforce
                        // deflation for enhanced authentication security.
                        final ZipDriverEntry zpt = (ZipDriverEntry) peer;
                        process = driver.process(ZipOutputService.this, entry, zpt);
                        if (!process) {
                            entry.setPlatform(zpt.getPlatform());
                            entry.setEncrypted(zpt.isEncrypted());
                            entry.setMethod(zpt.getMethod());
                            entry.setCompressedSize(zpt.getCompressedSize());
                            entry.setCrc(zpt.getCrc());
                            entry.setExtra(zpt.getExtra());
                        }
                    }
                }
                if (0 == entry.getSize())
                    entry.setMethod(STORED);
                if (STORED == entry.getMethod()) {
                    if (0 == entry.getSize()) {
                        entry.setCompressedSize(0);
                        entry.setCrc(0);
                    } else if (UNKNOWN == entry.getSize()
                            || UNKNOWN == entry.getCompressedSize()
                            || UNKNOWN == entry.getCrc()) {
                        assert process : "The CRC-32, compressed size and size properties should be set in the peer target!";
                        return new BufferedEntryOutputStream(entry, process);
                    }
                }
                return new EntryOutputStream(entry, process);
            }
        } // Output

        return new Output();
    }

    /**
     * Returns whether this output archive is busy writing an archive entry
     * or not.
     * 
     * @return Whether this output archive is busy writing an archive entry
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
        finish();
        final IOBuffer<?> pa = this.postamble;
        if (null != pa) {
            this.postamble = null;
            final InputSocket<?> is = pa.inputSocket();
            Throwable ex = null;
            try {
                final InputStream in = is.stream();
                try {
                    // If the output ZIP file differs in length from the
                    // input ZIP file then pad the output to the next four
                    // byte boundary before appending the postamble.
                    // This might be required for self extracting files on
                    // some platforms, e.g. Windows x86.
                    final long ol = length();
                    final long ipl = is.localTarget().getSize(DATA);
                    if ((ol + ipl) % 4 != 0)
                        write(new byte[4 - (int) (ol % 4)]);
                    Streams.cat(in, this);
                } catch (final Throwable ex2) {
                    ex = ex2;
                    throw ex2;
                } finally {
                    try {
                        in.close();
                    } catch (final IOException ex2) {
                        if (null == ex)
                            throw ex2;
                        ex.addSuppressed(ex2);
                    }
                }
            } catch (final Throwable ex2) {
                ex = ex2;
                throw ex2;
            } finally {
                try {
                    pa.release();
                } catch (final IOException ex2) {
                    if (null == ex)
                        throw ex2;
                    ex.addSuppressed(ex2);
                }
            }
        }
        super.close();
    }

    /**
     * This entry output stream writes directly to this output service.
     * It can only be used if this output service is not currently busy with
     * writing another entry and the entry holds enough information to write
     * the entry header.
     * These preconditions are checked by
     * {@link #outputSocket(ZipDriverEntry)}.
     */
    private final class EntryOutputStream extends DecoratingOutputStream {
        EntryOutputStream(final ZipDriverEntry entry, final boolean process)
        throws IOException {
            super(ZipOutputService.this);
            putNextEntry(entry, process);
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            closeEntry();
        }
    } // EntryOutputStream

    /**
     * This entry output stream writes the ZIP archive entry to an
     * {@linkplain IOBuffer I/O buffer}.
     * When the stream gets closed, the I/O buffer is then copied to this
     * output service and finally deleted.
     */
    @CleanupObligation
    private final class BufferedEntryOutputStream
    extends DecoratingOutputStream {
        final IOBuffer<?> buffer;
        final ZipDriverEntry entry;
        final boolean process;
        boolean closed;

        @CreatesObligation
        BufferedEntryOutputStream(
                final ZipDriverEntry entry,
                final boolean process)
        throws IOException {
            assert STORED == entry.getMethod();
            this.entry = entry;
            this.process = process;
            final IOBuffer<?> buffer = this.buffer = getIOPool().allocate();
            try {
                this.out = new CheckedOutputStream(buffer.outputSocket().stream(), new CRC32());
            } catch (final Throwable ex) {
                try {
                    buffer.release();
                } catch (final IOException ex2) {
                    ex.addSuppressed(ex2);
                }
                throw ex;
            }
            bufferedEntry = entry;
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            if (closed)
                return;
            out.close();
            save();
            bufferedEntry = null;
            closed = true;
        }

        void save() throws IOException {
            Throwable ex = null;
            try {
                copyProperties();
            } catch (final Throwable ex2) {
                ex = ex2;
                throw ex2;
            } finally {
                try {
                    storeBuffer();
                } catch (final IOException ex2) {
                    if (null == ex)
                        throw ex2;
                    ex.addSuppressed(ex2);
                }
            }
        }

        void copyProperties() {
            final IOBuffer<?> src = this.buffer;
            final ZipDriverEntry dst = this.entry;
            dst.setCrc(((CheckedOutputStream) out).getChecksum().getValue());
            final long length = src.getSize(DATA);
            dst.setCompressedSize(length);
            dst.setSize(length);
            if (UNKNOWN == dst.getTime())
                dst.setTime(src.getTime(WRITE));
        }

        void storeBuffer() throws IOException {
            final IOBuffer<?> buffer = this.buffer;
            final InputStream in = buffer.inputSocket().stream();
            Throwable ex = null;
            try {
                putNextEntry(entry, process);
                try {
                    Streams.cat(in, ZipOutputService.this);
                } catch (final Throwable ex2) {
                    ex = ex2;
                    throw ex2;
                } finally {
                    try {
                        closeEntry();
                    } catch (final IOException ex2) {
                        if (null == ex)
                            throw ex2;
                        ex.addSuppressed(ex2);
                    }
                }
            } catch (final Throwable ex2) {
                ex = ex2;
                throw ex2;
            } finally {
                try {
                    in.close();
                } catch (final IOException ex2) {
                    if (null == ex)
                        throw ex2;
                    ex.addSuppressed(ex2);
                }
            }
            buffer.release();
        }
    } // BufferedEntryOutputStream
}
