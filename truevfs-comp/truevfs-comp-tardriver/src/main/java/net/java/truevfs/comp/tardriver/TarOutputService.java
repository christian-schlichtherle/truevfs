/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.tardriver;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truecommons.io.*;
import static net.java.truecommons.shed.HashMaps.OVERHEAD_SIZE;
import static net.java.truecommons.shed.HashMaps.initialCapacity;
import net.java.truecommons.shed.SuppressedExceptionBuilder;
import static net.java.truevfs.comp.tardriver.TarDriver.*;
import net.java.truevfs.kernel.spec.FsModel;
import net.java.truevfs.kernel.spec.cio.*;
import static net.java.truevfs.kernel.spec.cio.Entry.Size.DATA;
import static net.java.truevfs.kernel.spec.cio.Entry.UNKNOWN;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

/**
 * An output service for writing TAR files.
 * This output service can only write one entry concurrently.
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
 *
 * @see    TarInputService
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public final class TarOutputService
implements OutputService<TarDriverEntry> {

    /** Maps entry names to tar entries [String -> TarDriverEntry]. */
    private final Map<String, TarDriverEntry>
            entries = new LinkedHashMap<>(initialCapacity(OVERHEAD_SIZE));

    private final TarArchiveOutputStream taos;
    private final TarDriver driver;
    private boolean busy;

    @CreatesObligation
    public TarOutputService(
            final FsModel model,
            final Sink sink,
            final TarDriver driver)
    throws IOException {
        Objects.requireNonNull(model);
        this.driver = Objects.requireNonNull(driver);
        final OutputStream out = sink.stream();
        try {
            final TarArchiveOutputStream
                    taos = this.taos = new TarArchiveOutputStream(out,
                        DEFAULT_BLKSIZE, DEFAULT_RCDSIZE, driver.getEncoding());
            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
        } catch (final Throwable ex) {
            try {
                out.close();
            } catch (final Throwable ex2) {
                ex.addSuppressed(ex2);
            }
            throw ex;
        }
    }

    private IoBufferPool getPool() {
        return driver.getPool();
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
        Objects.requireNonNull(local);
        final class Output extends AbstractOutputSocket<TarDriverEntry> {
            @Override
            public TarDriverEntry target() {
                return local;
            }

            @Override
            public OutputStream stream(final InputSocket<? extends Entry> peer)
            throws IOException {
                if (isBusy()) throw new OutputBusyException(local.getName());
                if (local.isDirectory()) {
                    updateProperties(local, DirectoryTemplate.INSTANCE);
                    return new EntryOutputStream(local);
                }
                updateProperties(local, target(peer));
                return UNKNOWN == local.getSize()
                        ? new BufferedEntryOutputStream(local)
                        : new EntryOutputStream(local);
            }
        } // Output
        return new Output();
    }

    void updateProperties(
            final TarDriverEntry local,
            final @CheckForNull Entry peer) {
        if (UNKNOWN == local.getModTime().getTime())
            local.setModTime(System.currentTimeMillis());
        if (null != peer)
            if (UNKNOWN == local.getSize())
                local.setSize(peer.getSize(DATA));
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
     * Returns whether this TAR output service is busy writing an archive entry
     * or not.
     * 
     * @return Whether this TAR output service is busy writing an archive entry
     *         or not.
     */
    private boolean isBusy() {
        return busy;
    }

    @Override
    public void close() throws IOException {
        taos.close();
    }

    /**
     * This entry output stream writes directly to the subclass.
     * It can only be used if this output stream is not currently busy
     * writing another entry and the entry holds enough information to
     * write the entry header.
     * These preconditions are checked by {@link #output(TarDriverEntry)}.
     */
    @CleanupObligation
    private final class EntryOutputStream extends DisconnectingOutputStream {
        boolean closed;

        @CreatesObligation
        EntryOutputStream(final TarDriverEntry local)
        throws IOException {
            super(taos);
            taos.putArchiveEntry(local);
            entries.put(local.getName(), local);
            busy = true;
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
            busy = false;
            taos.closeArchiveEntry();
        }
    } // EntryOutputStream

    /**
     * This entry output stream writes the entry to an I/O buffer.
     * When the stream is closed, the temporary file is then copied to this
     * output stream and finally deleted.
     */
    @CleanupObligation
    private final class BufferedEntryOutputStream
    extends DecoratingOutputStream {
        final IoBuffer buffer;
        final TarDriverEntry local;
        boolean closed;

        @CreatesObligation
        BufferedEntryOutputStream(final TarDriverEntry local)
        throws IOException {
            this.local = local;
            final IoBuffer buffer = this.buffer = getPool().allocate();
            try {
                this.out = buffer.output().stream(null);
            } catch (final Throwable ex) {
                try {
                    buffer.release();
                } catch (final Throwable ex2) {
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
            if (closed) return;
            closed = true;
            busy = false;
            out.close();
            updateProperties(local, buffer);
            storeBuffer();
        }

        void storeBuffer() throws IOException {
            final IoBuffer buffer = this.buffer;
            final SuppressedExceptionBuilder<IOException>
                    builder = new SuppressedExceptionBuilder<>();
            try (final InputStream in = buffer.input().stream(null)) {
                final TarArchiveOutputStream taos = TarOutputService.this.taos;
                taos.putArchiveEntry(local);
                try {
                    Streams.cat(in, taos);
                } catch (final InputException ex) { // NOT IOException!
                    builder.warn(ex);
                }
                try {
                    taos.closeArchiveEntry();
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
