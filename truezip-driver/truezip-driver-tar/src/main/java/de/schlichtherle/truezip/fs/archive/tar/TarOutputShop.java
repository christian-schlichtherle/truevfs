/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.tar;

import de.schlichtherle.truezip.entry.Entry;
import static de.schlichtherle.truezip.entry.Entry.Size.DATA;
import static de.schlichtherle.truezip.entry.Entry.UNKNOWN;
import static de.schlichtherle.truezip.fs.archive.tar.TarDriver.DEFAULT_BLKSIZE;
import static de.schlichtherle.truezip.fs.archive.tar.TarDriver.DEFAULT_RCDSIZE;
import de.schlichtherle.truezip.io.*;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.OutputShop;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.JSE7;
import de.schlichtherle.truezip.util.HashMaps;
import static de.schlichtherle.truezip.util.HashMaps.initialCapacity;
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
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
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
 * @see    TarInputShop
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class TarOutputShop
extends TarArchiveOutputStream
implements OutputShop<TarDriverEntry> {

    /**
     * The number of entries which can be initially accomodated by
     * the internal hash map without resizing it, which is {@value}.
     * 
     * @since  TrueZIP 7.3
     * @deprecated since TrueZIP 7.5.5
     */
    public static final int OVERHEAD_SIZE = HashMaps.OVERHEAD_SIZE;

    /** HashMaps entry names to tar entries [String -> TarDriverEntry]. */
    private final Map<String, TarDriverEntry> entries
            = new LinkedHashMap<String, TarDriverEntry>(
                    initialCapacity(HashMaps.OVERHEAD_SIZE));

    private final IOPool<?> pool;
    private boolean busy;

    @CreatesObligation
    public TarOutputShop(   final TarDriver driver,
                            final @WillCloseWhenClosed OutputStream out) {
        super(out, DEFAULT_BLKSIZE, DEFAULT_RCDSIZE, driver.getEncoding());
        super.setLongFileMode(LONGFILE_GNU);
        this.pool = driver.getPool();
    }

    @Override
    public int getSize() {
        return entries.size();
    }

    @Override
    public Iterator<TarDriverEntry> iterator() {
        return Collections.unmodifiableCollection(entries.values()).iterator();
    }

    @Override
    public @CheckForNull TarDriverEntry getEntry(String name) {
        return entries.get(name);
    }

    @Override
    public OutputSocket<TarDriverEntry> getOutputSocket(final TarDriverEntry local) {
        if (null == local)
            throw new NullPointerException();

        final class Output extends OutputSocket<TarDriverEntry> {
            @Override
            public TarDriverEntry getLocalTarget() {
                return local;
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                if (isBusy())
                    throw new OutputBusyException(local.getName());
                if (local.isDirectory()) {
                    updateProperties(local, DirectoryTemplate.INSTANCE);
                    return new EntryOutputStream(local);
                }
                updateProperties(local, getPeerTarget());
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

    /**
     * This entry output stream writes directly to our subclass.
     * It can only be used if this output stream is not currently busy
     * writing another entry and the entry holds enough information to
     * write the entry header.
     * These preconditions are checked by {@link #getOutputSocket(TarDriverEntry)}.
     */
    @CleanupObligation
    private final class EntryOutputStream extends DecoratingOutputStream {
        boolean closed;

        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        EntryOutputStream(final TarDriverEntry local)
        throws IOException {
            super(TarOutputShop.this);
            putArchiveEntry(local);
            entries.put(local.getName(), local);
            busy = true;
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            if (closed)
                return;
            closeArchiveEntry();
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
        final IOPool.Entry<?> buffer;
        final TarDriverEntry local;
        boolean closed;

        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        BufferedEntryOutputStream(final TarDriverEntry local)
        throws IOException {
            super(null);
            this.local = local;
            final IOPool.Entry<?> buffer = this.buffer = pool.allocate();
            try {
                this.delegate = buffer.getOutputSocket().newOutputStream();
            } catch (final IOException ex) {
                try {
                    buffer.release();
                } catch (final IOException ex2) {
                    if (JSE7.AVAILABLE) ex.addSuppressed(ex2);
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
            delegate.close();
            updateProperties(local, buffer);
            storeBuffer();
            closed = true;
            busy = false;
        }

        void storeBuffer() throws IOException {
            final IOPool.Entry<?> buffer = this.buffer;
            final InputStream in = buffer.getInputSocket().newInputStream();
            final SequentialIOExceptionBuilder<IOException, SequentialIOException> builder
                    = SequentialIOExceptionBuilder.create(IOException.class, SequentialIOException.class);
            try {
                final TarArchiveOutputStream taos = TarOutputShop.this;
                taos.putArchiveEntry(local);
                try {
                    Streams.cat(in, taos);
                } catch (final InputException ex2) { // NOT IOException!
                    builder.warn(ex2);
                }
                try {
                    taos.closeArchiveEntry();
                } catch (final IOException ex2) {
                    builder.warn(ex2);
                }
                builder.check();
            } catch (final IOException ex) {
                builder.warn(ex);
            } finally {
                try {
                    in.close();
                } catch (final IOException ex) {
                    builder.warn(ex);
                }
            }
            builder.check();
            buffer.release();
        }
    } // BufferedEntryOutputStream
}
