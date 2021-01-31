/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.tardriver;

import net.java.truecommons.cio.*;
import net.java.truecommons.io.DecoratingOutputStream;
import net.java.truecommons.io.DisconnectingOutputStream;
import net.java.truecommons.io.Sink;
import net.java.truecommons.io.Streams;
import net.java.truevfs.kernel.spec.FsModel;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

import static net.java.truecommons.cio.Entry.Size.DATA;
import static net.java.truecommons.cio.Entry.UNKNOWN;
import static net.java.truecommons.shed.HashMaps.OVERHEAD_SIZE;
import static net.java.truecommons.shed.HashMaps.initialCapacity;
import static org.apache.commons.compress.archivers.tar.TarConstants.DEFAULT_BLKSIZE;

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
 * @author Christian Schlichtherle
 * @see TarInputService
 */
public final class TarOutputService implements OutputService<TarDriverEntry> {

    /**
     * Maps entry names to tar entries [String -> TarDriverEntry].
     */
    private final Map<String, TarDriverEntry> entries = new LinkedHashMap<>(initialCapacity(OVERHEAD_SIZE));

    private final OutputStream out;
    private final TarArchiveOutputStream taos;
    private final TarDriver driver;
    private boolean busy;

    public TarOutputService(final FsModel model, final Sink sink, final TarDriver driver) throws IOException {
        Objects.requireNonNull(model);
        this.driver = Objects.requireNonNull(driver);
        out = sink.stream();
        try {
            final TarArchiveOutputStream taos = new TarArchiveOutputStream(out, DEFAULT_BLKSIZE, driver.getEncoding());
            taos.setAddPaxHeadersForNonAsciiNames(driver.getAddPaxHeaderForNonAsciiNames());
            taos.setLongFileMode(driver.getLongFileMode());
            taos.setBigNumberMode(driver.getBigNumberMode());
            this.taos = taos;
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
    public @CheckForNull
    TarDriverEntry entry(String name) {
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
            public OutputStream stream(final InputSocket<? extends Entry> peer) throws IOException {
                if (isBusy()) {
                    throw new OutputBusyException(local.getName());
                }
                if (local.isDirectory()) {
                    updateProperties(local, DirectoryTemplate.INSTANCE);
                    return new EntryOutputStream(local);
                }
                updateProperties(local, target(peer));
                return UNKNOWN == local.getSize()
                        ? new BufferedEntryOutputStream(local)
                        : new EntryOutputStream(local);
            }
        }
        return new Output();
    }

    private void updateProperties(final TarDriverEntry local, final @CheckForNull Entry peer) {
        if (UNKNOWN == local.getModTime().getTime()) {
            local.setModTime(System.currentTimeMillis());
        }
        if (null != peer && UNKNOWN == local.getSize()) {
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

        @Override
        public Boolean isPermitted(Access type, Entity entity) {
            return null;
        }
    }

    /**
     * Returns whether this TAR output service is busy writing an archive entry
     * or not.
     *
     * @return Whether this TAR output service is busy writing an archive entry
     * or not.
     */
    private boolean isBusy() {
        return busy;
    }

    @Override
    public void close() throws IOException {
        taos.close();
        out.close(); // idempotence
    }

    /**
     * This entry output stream writes directly to the subclass.
     * It can only be used if this output stream is not currently busy
     * writing another entry and the entry holds enough information to
     * write the entry header.
     * These preconditions are checked by {@link #output(TarDriverEntry)}.
     */
    private final class EntryOutputStream extends DisconnectingOutputStream {

        boolean closed;

        EntryOutputStream(final TarDriverEntry local) throws IOException {
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
        public void close() throws IOException {
            if (closed) {
                return;
            }
            closed = true;
            busy = false;
            taos.closeArchiveEntry();
        }
    }

    /**
     * This entry output stream writes the entry to an I/O buffer.
     * When the stream is closed, the temporary file is then copied to this
     * output stream and finally deleted.
     */
    private final class BufferedEntryOutputStream extends DecoratingOutputStream {

        final IoBuffer buffer;
        final TarDriverEntry local;
        boolean closed;

        BufferedEntryOutputStream(final TarDriverEntry local) throws IOException {
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
        public void close() throws IOException {
            assert null != out;
            if (closed) {
                return;
            }
            closed = true;
            busy = false;
            out.close();
            updateProperties(local, buffer);
            storeBuffer();
        }

        @SuppressWarnings("ThrowFromFinallyBlock")
        void storeBuffer() throws IOException {
            final IoBuffer buffer = this.buffer;
            Throwable t1 = null;
            try (final InputStream in = buffer.input().stream(null)) {
                final TarArchiveOutputStream taos = TarOutputService.this.taos;
                taos.putArchiveEntry(local);
                Streams.cat(in, taos);
                taos.closeArchiveEntry();
            } catch (final Throwable t2) {
                t1 = t2;
                throw t2;
            } finally {
                try {
                    buffer.release();
                } catch (final Throwable t2) {
                    if (null == t1) {
                        throw t2;
                    }
                    t1.addSuppressed(t2);
                }
            }
        }
    }
}
