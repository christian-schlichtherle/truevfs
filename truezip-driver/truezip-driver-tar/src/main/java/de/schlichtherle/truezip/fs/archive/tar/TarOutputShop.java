/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.tar;

import de.schlichtherle.truezip.entry.Entry;
import static de.schlichtherle.truezip.entry.Entry.Size.DATA;
import static de.schlichtherle.truezip.entry.Entry.UNKNOWN;
import de.schlichtherle.truezip.fs.archive.FsArchiveFileSystem;
import de.schlichtherle.truezip.fs.archive.FsMultiplexedOutputShop;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.io.OutputBusyException;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.OutputShop;
import de.schlichtherle.truezip.socket.OutputSocket;
import static de.schlichtherle.truezip.util.Maps.initialCapacity;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
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
 * Archive drivers may wrap this class in a {@link FsMultiplexedOutputShop}
 * to overcome this limitation.
 *
 * @see     TarInputShop
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public class TarOutputShop
extends TarArchiveOutputStream
implements OutputShop<TTarArchiveEntry> {

    /**
     * The number of entries which can be initially accomodated by
     * the internal hash map without resizing it, which is {@value}.
     * 
     * @since  TrueZIP 7.3
     */
    public static final int OVERHEAD_SIZE = FsArchiveFileSystem.OVERHEAD_SIZE;

    /** Maps entry names to tar entries [String -> TTarArchiveEntry]. */
    private final Map<String, TTarArchiveEntry> entries
            = new LinkedHashMap<String, TTarArchiveEntry>(
                    initialCapacity(OVERHEAD_SIZE));

    private final IOPool<?> pool;
    private boolean busy;

    public TarOutputShop(final TarDriver driver, OutputStream out) {
        super(out);
        super.setLongFileMode(LONGFILE_GNU);
        this.pool = driver.getPool();
    }

    @Override
    public int getSize() {
        return entries.size();
    }

    @Override
    public Iterator<TTarArchiveEntry> iterator() {
        return entries.values().iterator();
    }

    @Override
    public @CheckForNull TTarArchiveEntry getEntry(String name) {
        return entries.get(name);
    }

    @Override
    public OutputSocket<TTarArchiveEntry> getOutputSocket(final TTarArchiveEntry entry) {
        if (null == entry)
            throw new NullPointerException();

        class Output extends OutputSocket<TTarArchiveEntry> {
            @Override
            public TTarArchiveEntry getLocalTarget() {
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
                // The source entry does not exist or cannot support DDC
                // to the destination entry.
                // So we need to buffer the output in a temporary file and
                // write it upon close().
                return new TempEntryOutputStream(
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

    /**
     * This entry output stream writes directly to our subclass.
     * It can only be used if this output stream is not currently busy
     * writing another entry and the entry holds enough information to
     * write the entry header.
     * These preconditions are checked by {@link #getOutputSocket(TTarArchiveEntry)}.
     */
    private class EntryOutputStream extends DecoratingOutputStream {
        private boolean closed;

        EntryOutputStream(final TTarArchiveEntry entry)
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

            // Order is important here!
            closed = true;
            busy = false;
            closeArchiveEntry();
        }
    } // class EntryOutputStream

    /**
     * This entry output stream writes the entry to a temporary file.
     * When the stream is closed, the temporary file is then copied to this
     * output stream and finally deleted.
     */
    private class TempEntryOutputStream extends DecoratingOutputStream {
        private final IOPool.Entry<?> temp;
        private final TTarArchiveEntry entry;
        private boolean closed;

        TempEntryOutputStream(final IOPool.Entry<?> temp, final TTarArchiveEntry entry)
        throws IOException {
            super(temp.getOutputSocket().newOutputStream());
            this.temp = temp;
            this.entry = entry;
            entries.put(entry.getName(), entry);
            busy = true;
        }

        @Override
        public void close() throws IOException {
            if (closed)
                return;

            // Order is important here!
            closed = true;
            busy = false;
            try {
                super.close();
            } finally {
                entry.setSize(temp.getSize(DATA));
                store();
            }
        }

        void store() throws IOException {
            try {
                final InputStream in = temp.getInputSocket().newInputStream();
                try {
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
                temp.release();
            }
        }
    } // class TempEntryOutputStream
}
