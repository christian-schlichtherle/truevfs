/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.archive.driver.tar;

import de.schlichtherle.truezip.io.FilterOutputStream;
import de.schlichtherle.truezip.io.socket.CommonEntry;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.io.archive.output.MultiplexedArchiveOutputShop;
import de.schlichtherle.truezip.io.socket.OutputShop;
import de.schlichtherle.truezip.io.socket.OutputBusyException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.tools.tar.TarOutputStream;

import static de.schlichtherle.truezip.io.archive.driver.tar.TarDriver.TEMP_FILE_PREFIX;
import static de.schlichtherle.truezip.io.Files.createTempFile;
import static de.schlichtherle.truezip.io.socket.CommonEntry.Size.DATA;

/**
 * An implementation of {@link OutputShop} to write TAR archives.
 * <p>
 * Because the TAR file format needs to know each entry's length in advance,
 * entries from an unknown source (such as entries created with
 * {@link FileOutputStream}) are actually written to temp files and copied
 * to the underlying {@code TarOutputStream} upon a call to their
 * {@link OutputStream#close} method.
 * Note that this implies that the {@code close()} method may fail with
 * an {@link IOException}.
 * <p>
 * If the size of an entry is known in advance it's directly written to the
 * underlying {@link TarOutputStream} instead.
 * <p>
 * This output archive can only write one entry concurrently.
 * Archive drivers may wrap this class in a {@link MultiplexedArchiveOutputShop}
 * to overcome this limitation.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class TarOutputShop
extends TarOutputStream
implements OutputShop<TarEntry> {

    /** Maps entry names to tar entries [String -> TarEntry]. */
    private final Map<String, TarEntry> entries
            = new LinkedHashMap<String, TarEntry>();

    private boolean busy;

    public TarOutputShop(OutputStream out) {
        super(out);
        super.setLongFileMode(LONGFILE_GNU);
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public Iterator<TarEntry> iterator() {
        return entries.values().iterator();
    }

    @Override
    public TarEntry getEntry(String name) {
        return entries.get(name);
    }

    @Override
    public OutputSocket<TarEntry> getOutputSocket(final TarEntry entry)
    throws FileNotFoundException {
        class Output extends OutputSocket<TarEntry> {
            @Override
            public TarEntry getLocalTarget() {
                return entry;
            }

            @Override
			public OutputStream newOutputStream() throws IOException {
                if (isBusy())
                    throw new OutputBusyException(entry);
                if (entry.isDirectory()) {
                    entry.setSize(0);
                    return new EntryOutputStream(entry);
                }
                final CommonEntry peer = getRemoteTarget();
                if (null != peer) {
                    entry.setSize(peer.getSize(DATA));
                    return new EntryOutputStream(entry);
                }
                // The source entry does not exist or cannot support DDC
                // to the destination entry.
                // So we need to buffer the output in a temporary file and write
                // it upon close().
                return new TempEntryOutputStream(
                        createTempFile(TEMP_FILE_PREFIX), entry);
            }
        }
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
     * These preconditions are checked by {@link #getOutputSocket(TarEntry)}.
     */
    private class EntryOutputStream extends FilterOutputStream {
        private boolean closed;

        EntryOutputStream(final TarEntry entry)
        throws IOException {
            super(TarOutputShop.this);
            putNextEntry(entry);
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

            // Order is important here!
            closed = true;
            busy = false;
            closeEntry();
        }
    } // class EntryOutputStream

    /**
     * This entry output stream writes the entry to a temporary file.
     * When the stream is closed, the temporary file is then copied to this
     * output stream and finally deleted.
     */
    private class TempEntryOutputStream extends FilterOutputStream {
        private final File temp;
        private final TarEntry entry;
        private boolean closed;

        TempEntryOutputStream(final File temp, final TarEntry entry)
        throws IOException {
            super(new FileOutputStream(temp)); // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
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
                entry.setSize(temp.length());
                store();
            }
        }

        void store() throws IOException {
            try {
                final InputStream in = new FileInputStream(temp);
                try {
                    putNextEntry(entry);
                    try {
                        Streams.cat(in, TarOutputShop.this);
                    } finally {
                        closeEntry();
                    }
                } finally {
                    in.close();
                }
            } finally {
                if (!temp.delete()) // may fail on Windoze if in.close() failed!
                    temp.deleteOnExit(); // we're bullish never to leavy any temps!
            }
        }
    } // class TempEntryOutputStream
}
