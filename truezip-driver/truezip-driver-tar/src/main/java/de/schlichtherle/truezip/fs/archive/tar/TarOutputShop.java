/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.fs.archive.tar;

import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.fs.archive.FsMultiplexedOutputShop;
import de.schlichtherle.truezip.socket.OutputShop;
import de.schlichtherle.truezip.io.OutputBusyException;
import de.schlichtherle.truezip.socket.IOPool;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import static de.schlichtherle.truezip.entry.Entry.Size.DATA;
import static de.schlichtherle.truezip.entry.Entry.UNKNOWN;

/**
 * An implementation of {@link OutputShop} to write TAR archives.
 * <p>
 * Because the TAR file format needs to know each entry's length in advance,
 * entries from an unknown source are actually written to temp files and copied
 * to the underlying {@code TarOutputStream} upon a call to their
 * {@link OutputStream#close} method.
 * Note that this implies that the {@code close()} method may fail with
 * an {@link IOException}.
 * <p>
 * If the size of an entry is known in advance it's directly written to the
 * underlying {@link TarOutputStream} instead.
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

    /** Maps entry names to tar entries [String -> TTarArchiveEntry]. */
    private final Map<String, TTarArchiveEntry> entries
            = new LinkedHashMap<String, TTarArchiveEntry>();

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
    public TTarArchiveEntry getEntry(String name) {
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
        } // class Output

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
