/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.tar;

import static de.truezip.kernel.cio.Entry.Size.DATA;
import static de.truezip.kernel.cio.Entry.UNKNOWN;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.io.DecoratingOutputStream;
import de.truezip.kernel.io.OutputBusyException;
import de.truezip.kernel.io.Streams;
import de.truezip.kernel.util.Maps;
import static de.truezip.kernel.util.Maps.initialCapacity;
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
extends TarArchiveOutputStream
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
    private final IOPool<?> pool;
    private boolean busy;

    @CreatesObligation
    public TarOutputService(final TarDriver driver,
                            final @WillCloseWhenClosed OutputStream out) {
        super(out);
        this.out = out;
        super.setLongFileMode(LONGFILE_GNU);
        this.pool = driver.getIOPool();
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
    public @CheckForNull TarDriverEntry getEntry(String name) {
        return entries.get(name);
    }

    @Override
    public OutputSocket<TarDriverEntry> getOutputSocket(final TarDriverEntry entry) {
        if (null == entry)
            throw new NullPointerException();

        final class Output extends OutputSocket<TarDriverEntry> {
            @Override
            public TarDriverEntry getLocalTarget() {
                return entry;
            }

            @Override
            public OutputStream newStream() throws IOException {
                if (isBusy())
                    throw new OutputBusyException(entry.getName());
                if (entry.isDirectory()) {
                    entry.setSize(0);
                    return new EntryOutputStream(entry);
                }
                final Entry remote = getRemoteTarget();
                long size;
                if (null != remote && UNKNOWN != (size = remote.getSize(DATA))) {
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
        out.close();
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
            super(TarOutputService.this);
            putArchiveEntry(entry);
            entries.put(entry.getName(), entry);
            busy = true;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
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
        final IOBuffer<?> buffer;
        final TarDriverEntry entry;
        boolean closed;

        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        BufferedEntryOutputStream(
                final IOBuffer<?> buffer,
                final TarDriverEntry entry)
        throws IOException {
            super(buffer.getOutputSocket().newStream());
            this.buffer = buffer;
            this.entry = entry;
            entries.put(entry.getName(), entry);
            busy = true;
        }

        @Override
        public void close() throws IOException {
            if (closed)
                return;
            out.close();
            closed = true;
            store();
        }

        void store() throws IOException {
            final IOBuffer<?> buffer = this.buffer;
            assert null != buffer;

            final TarDriverEntry entry = this.entry;
            assert null != entry;

            TarOutputService.this.busy = false;
            try {
                try (final InputStream in = buffer.getInputSocket().newStream()) {
                    entry.setSize(buffer.getSize(DATA));
                    if (UNKNOWN == entry.getModTime().getTime())
                        entry.setModTime(System.currentTimeMillis());
                    putArchiveEntry(entry);
                    try {
                        Streams.cat(in, TarOutputService.this);
                    } finally {
                        closeArchiveEntry();
                    }
                }
            } finally {
                buffer.release();
            }
        }
    } // BufferedEntryOutputStream
}
