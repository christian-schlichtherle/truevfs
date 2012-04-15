/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.tar;

import de.truezip.kernel.FsModel;
import static de.truezip.kernel.cio.Entry.Size.DATA;
import static de.truezip.kernel.cio.Entry.UNKNOWN;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.io.*;
import de.truezip.kernel.util.ExceptionBuilder;
import de.truezip.kernel.util.Maps;
import static de.truezip.kernel.util.Maps.initialCapacity;
import de.truezip.kernel.util.SuppressedExceptionBuilder;
import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

/**
 * An implementation of {@link OutputService} to write TAR archives.
 * <p>
 * Because the TAR file format needs to know each entry's length in advance,
 * entries from an unknown source are actually written to temp files and copied
 * to the underlying {@link TarArchiveOutputStream} upon a call to their
 * {@link OutputStream#close} method.
 * Note that this implies that the {@code close()} method may fail with
 * an {@link IOException}.
 * <p>
 * If the size of an entry is known in advance it's directly written to the
 * underlying {@code TarArchiveOutputStream} instead.
 * <p>
 * This output archive can only write one entry concurrently.
 *
 * @see    TarInputService
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public final class TarOutputService
implements OutputService<TarDriverEntry> {

    /**
     * The number of entries which can be initially accomodated by
     * the internal hash map without resizing it, which is {@value}.
     */
    public static final int OVERHEAD_SIZE = Maps.OVERHEAD_SIZE;

    /** Maps entry names to tar entries [String -> TarDriverEntry]. */
    private final Map<String, TarDriverEntry>
            entries = new LinkedHashMap<>(initialCapacity(OVERHEAD_SIZE));

    private final OutputStream out;
    private final TarArchiveOutputStream tos;
    private final TarDriver driver;
    private boolean busy;

    @CreatesObligation
    public TarOutputService(
            final FsModel model,
            final Sink sink,
            final TarDriver driver)
    throws IOException {
        if (null == model)
            throw new NullPointerException();
        if (null == (this.driver = driver))
            throw new NullPointerException();
        final OutputStream out = this.out = sink.stream();
        try {
            final TarArchiveOutputStream
                    tos = this.tos = new TarArchiveOutputStream(out);
            tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
        } catch (final Throwable ex) {
            try {
                out.close();
            } catch (final IOException ex2) {
                ex.addSuppressed(ex2);
            }
            throw ex;
        }
    }

    private IOPool<?> getIOPool() {
        return driver.getIOPool();
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public Iterator<TarDriverEntry> iterator() {
        return Collections.unmodifiableCollection(entries.values()).iterator();
    }

    @Override
    public @CheckForNull TarDriverEntry entry(String name) {
        return entries.get(name);
    }

    @Override
    public OutputSocket<TarDriverEntry> output(final TarDriverEntry local) {
        if (null == local)
            throw new NullPointerException();

        final class Output extends OutputSocket<TarDriverEntry> {
            @Override
            public TarDriverEntry localTarget() {
                return local;
            }

            @Override
            public OutputStream stream() throws IOException {
                if (isBusy())
                    throw new OutputBusyException(local.getName());
                if (local.isDirectory()) {
                    updateProperties(local, DirectoryTemplate.INSTANCE);
                    return new EntryOutputStream(local);
                }
                updateProperties(local, peerTarget());
                if (UNKNOWN == local.getSize())
                    return new BufferedEntryOutputStream(local);
                return new EntryOutputStream(local);
            }
        } // Output

        return new Output();
    }

    void updateProperties(
            final TarDriverEntry local,
            final @CheckForNull Entry peer) {
        if (UNKNOWN == local.getModTime().getTime())
            local.setModTime(System.currentTimeMillis());
        if (null != peer) {
            if (UNKNOWN == local.getSize())
                local.setSize(peer.getSize(DATA));
        }
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
    } // DirectoryTemplate

    /**
     * Returns whether this output archive is busy writing an archive entry
     * or not.
     */
    private boolean isBusy() {
        return busy;
    }

    @Override
    public void close() throws IOException {
        tos.close();
    }

    /**
     * This entry output stream writes directly to our subclass.
     * It can only be used if this output stream is not currently busy
     * writing another entry and the entry holds enough information to
     * write the entry header.
     * These preconditions are checked by {@link #output(TarDriverEntry)}.
     */
    @CleanupObligation
    private final class EntryOutputStream extends DecoratingOutputStream {
        boolean closed;

        @CreatesObligation
        EntryOutputStream(final TarDriverEntry local)
        throws IOException {
            super(tos);
            tos.putArchiveEntry(local);
            entries.put(local.getName(), local);
            busy = true;
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            if (closed)
                return;
            tos.closeArchiveEntry();
            closed = true;
            busy = false;
        }
    } // EntryOutputStream

    /**
     * This entry output stream writes the entry to a temporary file.
     * When the stream is closed, the temporary file is then copied to this
     * output stream and finally deleted.
     */
    @CleanupObligation
    private final class BufferedEntryOutputStream
    extends DecoratingOutputStream {
        final IOBuffer<?> buffer;
        final TarDriverEntry local;
        boolean closed;

        @CreatesObligation
        BufferedEntryOutputStream(final TarDriverEntry local)
        throws IOException {
            this.local = local;
            final IOBuffer<?> buffer = this.buffer = getIOPool().allocate();
            try {
                this.out = buffer.output().stream();
            } catch (final Throwable ex) {
                try {
                    buffer.release();
                } catch (final IOException ex2) {
                    ex.addSuppressed(ex2);
                }
                throw ex;
            }
            entries.put(local.getName(), local);
            busy = true;
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            if (closed)
                return;
            out.close();
            updateProperties(local, buffer);
            storeBuffer();
            closed = true;
            busy = false;
        }

        void storeBuffer() throws IOException {
            final IOBuffer<?> buffer = this.buffer;
            final InputStream in = buffer.input().stream();
            Throwable ex = null;
            try {
                final ExceptionBuilder<IOException, IOException>
                        builder = new SuppressedExceptionBuilder<>();
                final TarArchiveOutputStream tos = TarOutputService.this.tos;
                tos.putArchiveEntry(local);
                try {
                    Streams.cat(in, tos);
                } catch (final InputException ex2) { // NOT IOException!
                    builder.warn(ex2);
                }
                try {
                    tos.closeArchiveEntry();
                } catch (final IOException ex2) {
                    builder.warn(ex2);
                }
                builder.check();
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
