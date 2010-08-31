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

package de.schlichtherle.truezip.io.archive.driver;

import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.util.ChainableIOException;
import de.schlichtherle.truezip.io.util.InputException;
import de.schlichtherle.truezip.io.archive.controller.OutputArchiveMetaData;
import de.schlichtherle.truezip.io.archive.driver.tar.TarEntry;
import de.schlichtherle.truezip.io.archive.driver.zip.ZipEntry;
import de.schlichtherle.truezip.io.util.ChainableIOExceptionBuilder;
import de.schlichtherle.truezip.io.util.Streams;
import de.schlichtherle.truezip.util.JointEnumeration;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static de.schlichtherle.truezip.io.util.Files.createTempFile;

/**
 * A decorator for output archives which allows to write an unlimited number
 * of entries concurrently while actually only one entry is written at a time
 * to the target output archive.
 * If there is more than one entry to be written concurrently, the additional
 * entries are actually written to temp files and copied to the target
 * output archive upon a call to their {@link OutputStream#close} method.
 * Note that this implies that the {@code close()} method may fail with
 * an {@link IOException}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class MultiplexedOutputArchive implements OutputArchive {

    /** Prefix for temporary files created by the multiplexer. */
    static final String TEMP_FILE_PREFIX = "tzp-mux";

    /** The decorated output archive. */
    private final OutputArchive target;

    /**
     * The map of temporary archive entries which have not yet been written
     * to the target output archive.
     */
    private final Map<String, TempEntryOutputStream> temps
            = new LinkedHashMap<String, TempEntryOutputStream>();

    /** @see #isTargetBusy */
    private boolean targetBusy;

    /**
     * Constructs a new {@code MultiplexedOutputArchive}.
     * 
     * @param target The decorated output archive.
     * @throws NullPointerException Iff {@code target} is {@code null}.
     */
    public MultiplexedOutputArchive(final OutputArchive target) {
        if (target == null)
            throw new NullPointerException();
        this.target = target;
        
    }

    public int getNumArchiveEntries() {
        return target.getNumArchiveEntries() + temps.size();
    }

    public Enumeration<ArchiveEntry> getArchiveEntries() {
        return new JointEnumeration(target.getArchiveEntries(),
                                    new TempEntriesEnumeration());
    }

    private class TempEntriesEnumeration implements Enumeration<ArchiveEntry> {
        private final Iterator<TempEntryOutputStream> i
                = temps.values().iterator();

        public boolean hasMoreElements() {
            return i.hasNext();
        }

        public ArchiveEntry nextElement() {
            return i.next().entry;
        }
    }

    public ArchiveEntry getArchiveEntry(String entryName) {
        ArchiveEntry entry = target.getArchiveEntry(entryName);
        if (entry != null)
            return entry;
        final TempEntryOutputStream tempOut = temps.get(entryName);
        return tempOut != null ? tempOut.entry : null;
    }

    public OutputStream newOutputStream(
            final ArchiveEntry entry,
            final ArchiveEntry srcEntry)
    throws IOException {
        if (srcEntry != null)
            entry.setSize(srcEntry.getSize()); // data may be compressed!
        
        if (isTargetBusy()) {
            final File temp = createTempFile(TEMP_FILE_PREFIX);
            return new TempEntryOutputStream(entry, srcEntry, temp);
        }
        return new EntryOutputStream(entry, srcEntry);
    }

    /**
     * Returns whether the target output archive is busy writing an archive
     * entry or not.
     */
    public boolean isTargetBusy() {
        return targetBusy;
    }

    /**
     * This entry output stream writes directly to the target output archive.
     */
    private class EntryOutputStream extends FilterOutputStream {
        private boolean closed;

        private EntryOutputStream(
                final ArchiveEntry entry,
                final ArchiveEntry srcEntry)
        throws IOException {
            super(target.newOutputStream(entry, srcEntry));
            targetBusy = true;
        }

        @Override
        public void write(byte[] b) throws IOException {
            out.write(b, 0, b.length);
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
            targetBusy = false;
            super.close();

            storeTempEntries();
        }
    } // class EntryOutputStream

    /**
     * This entry output stream writes the entry to a temporary file.
     * When the stream is closed, the temporary file is then copied to the
     * target output archive and finally deleted unless the target is still
     * busy.
     */
    private class TempEntryOutputStream extends java.io.FileOutputStream {
        private final ArchiveEntry entry, srcEntry;
        private final File temp;
        private boolean closed;

        @SuppressWarnings("LeakingThisInConstructor")
        private TempEntryOutputStream(
                final ArchiveEntry entry,
                final ArchiveEntry srcEntry,
                final File temp)
        throws IOException {
            super(temp);
            this.entry = entry;
            this.srcEntry = srcEntry != null ? srcEntry : new RfsEntry(temp);
            this.temp = temp;
            temps.put(entry.getName(), this);
        }

        @Override
        public void close() throws IOException {
            if (closed)
                return;

            // Order is important here!
            closed = true;
            super.close();
            if (entry.getSize() == ArchiveEntry.UNKNOWN)
                entry.setSize(temp.length());
            if (entry.getTime() == ArchiveEntry.UNKNOWN)
                entry.setTime(temp.lastModified());

            // Note that this must be guarded by the closed flag: close() gets
            // called from the finalize() method in the super class, which
            // may cause a ConcurrentModificationException in this method.
            storeTempEntries();
        }
    } // class TempEntryOutputStream

    private void storeTempEntries() throws IOException {
        if (isTargetBusy())
            return;

        final ChainableIOExceptionBuilder<ChainableIOException> builder
                = new ChainableIOExceptionBuilder<ChainableIOException>();
        for (final Iterator i = temps.values().iterator(); i.hasNext(); ) {
            final TempEntryOutputStream tempOut
                    = (TempEntryOutputStream) i.next();
            if (!tempOut.closed)
                continue;
            try {
                final ArchiveEntry entry = tempOut.entry;
                final ArchiveEntry srcEntry = tempOut.srcEntry;
                final File temp = tempOut.temp;
                try {
                    final InputStream in = new FileInputStream(temp);
                    try {
                        final OutputStream out = target.newOutputStream(
                                entry, srcEntry);
                        try {
                            Streams.cat(in, out);
                        } finally {
                            out.close();
                        }
                    } finally {
                        in.close();
                    }
                } finally {
                    if (!temp.delete()) // may fail on Windoze if in.close() failed!
                        temp.deleteOnExit(); // be bullish never to leavy any temps!
                }
            } catch (FileNotFoundException ex) {
                // Input exception - let's continue!
                builder.warn(new ChainableIOException(ex));
            } catch (InputException ex) {
                // Input exception - let's continue!
                builder.warn(new ChainableIOException(ex));
            } catch (IOException ex) {
                // Something's wrong writing this MultiplexedOutputStream!
                throw builder.fail(new ChainableIOException(ex));
            } finally {
                i.remove();
            }
        }
        builder.check();
    }

    public void close() throws IOException {
        assert !isTargetBusy();
        try {
            storeTempEntries();
            assert temps.isEmpty();
        } finally {
            target.close();
        }
    }

    public OutputArchiveMetaData getMetaData() {
        return target.getMetaData();
    }

    public void setMetaData(OutputArchiveMetaData metaData) {
        target.setMetaData(metaData);
    }
}
