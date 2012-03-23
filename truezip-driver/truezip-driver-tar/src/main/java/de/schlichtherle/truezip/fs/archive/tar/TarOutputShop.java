/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.tar;

import de.schlichtherle.truezip.entry.Entry;
import static de.schlichtherle.truezip.entry.Entry.Size.DATA;
import static de.schlichtherle.truezip.entry.Entry.UNKNOWN;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.io.OutputBusyException;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.OutputShop;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.Maps;
import static de.schlichtherle.truezip.util.Maps.initialCapacity;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
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
 * An implementation of {@link OutputShop} to write TAR archives.
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
     */
    public static final int OVERHEAD_SIZE = Maps.OVERHEAD_SIZE;

    /** Maps entry names to tar entries [String -> TarDriverEntry]. */
    private final Map<String, TarDriverEntry> entries
            = new LinkedHashMap<String, TarDriverEntry>(
                    initialCapacity(OVERHEAD_SIZE));

    private final OutputStream delegate;
    private final IOPool<?> pool;
    private boolean busy;

    @CreatesObligation
    public TarOutputShop(   final TarDriver driver,
                            final @WillCloseWhenClosed OutputStream out) {
        super(out);
        this.delegate = out;
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
    public OutputSocket<TarDriverEntry> getOutputSocket(final TarDriverEntry entry) {
        if (null == entry)
            throw new NullPointerException();

        class Output extends OutputSocket<TarDriverEntry> {
            @Override
            public TarDriverEntry getLocalTarget() {
                return entry;
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                if (isBusy())
                    throw new OutputBusyException(entry.getName());
                if (entry.isDirectory()) {
                    entry.setSize(0);
                    return new EntryOutputStream(entry);
                }
                final Entry peer = getPeerTarget();
                long size;
                if (null != peer && UNKNOWN != (size = peer.getSize(DATA))) {
                    entry.setSize(size);
                    return new EntryOutputStream(entry);
                }
                // The source entry does not exist or cannot support Raw Data
                // Copying (RDC) to the destination entry.
                // So we need to write the output to a temporary buffer and
                // copy it upon close().
                return new BufferedEntryOutputStream(
                        pool.allocate(),
                        entry);
            }
        } // Output

        return new Output();
    }

    /**
     * Returns whether this output archive is busy writing an archive entry
     * or not.
     */
    private boolean isBusy() {
        return busy;
    }

    @Override
    public void close() throws IOException {
        super.close();
        // Workaround for super class implementation which may not have
        // been left in a consistent state if the decorated stream has
        // thrown an IOException upon the first call to its close() method.
        // See http://java.net/jira/browse/TRUEZIP-234
        delegate.close();
    }

    /**
     * This entry output stream writes directly to our subclass.
     * It can only be used if this output stream is not currently busy
     * writing another entry and the entry holds enough information to
     * write the entry header.
     * These preconditions are checked by {@link #getOutputSocket(TarDriverEntry)}.
     */
    private final class EntryOutputStream extends DecoratingOutputStream {
        boolean closed;

        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        EntryOutputStream(final TarDriverEntry entry)
        throws IOException {
            super(TarOutputShop.this);
            putArchiveEntry(entry);
            entries.put(entry.getName(), entry);
            busy = true;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            delegate.write(b, off, len);
        }

        @Override
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
    private final class BufferedEntryOutputStream
    extends DecoratingOutputStream {
        final IOPool.Entry<?> buffer;
        final TarDriverEntry entry;
        boolean closed;

        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        BufferedEntryOutputStream(
                final IOPool.Entry<?> buffer,
                final TarDriverEntry entry)
        throws IOException {
            super(buffer.getOutputSocket().newOutputStream());
            this.buffer = buffer;
            this.entry = entry;
            entries.put(entry.getName(), entry);
            busy = true;
        }

        @Override
        public void close() throws IOException {
            if (closed)
                return;
            delegate.close();
            closed = true;
            store();
        }

        void store() throws IOException {
            final IOPool.Entry<?> buffer = this.buffer;
            assert null != buffer;

            final TarDriverEntry entry = this.entry;
            assert null != entry;

            TarOutputShop.this.busy = false;
            try {
                final InputStream in = buffer.getInputSocket().newInputStream();
                try {
                    entry.setSize(buffer.getSize(DATA));
                    if (UNKNOWN == entry.getModTime().getTime())
                        entry.setModTime(System.currentTimeMillis());
                    putArchiveEntry(entry);
                    try {
                        Streams.cat(in, TarOutputShop.this);
                    } finally {
                        closeArchiveEntry();
                    }
                } finally {
                    in.close();
                }
            } finally {
                buffer.release();
            }
        }
    } // BufferedEntryOutputStream
}
