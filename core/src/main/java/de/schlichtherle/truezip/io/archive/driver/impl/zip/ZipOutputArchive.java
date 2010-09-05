/*
 * Copyright (C) 2009-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.archive.driver.impl.zip;

import de.schlichtherle.truezip.io.socket.IOReferences;
import de.schlichtherle.truezip.io.socket.IOStreamSockets;
import de.schlichtherle.truezip.io.archive.driver.ArchiveOutputStreamSocket;
import de.schlichtherle.truezip.io.archive.controller.OutputArchiveMetaData;
import de.schlichtherle.truezip.io.archive.driver.spi.MultiplexedOutputArchive;
import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.driver.OutputArchive;
import de.schlichtherle.truezip.io.archive.driver.OutputArchiveBusyException;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.io.socket.InputStreamSocket;
import de.schlichtherle.truezip.io.socket.IOReference;
import de.schlichtherle.truezip.io.zip.BasicZipOutputStream;
import de.schlichtherle.truezip.util.JointIterator;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Iterator;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;

import static de.schlichtherle.truezip.io.archive.driver.impl.zip.ZipDriver.TEMP_FILE_PREFIX;
import static de.schlichtherle.truezip.io.Files.createTempFile;
import static de.schlichtherle.truezip.io.zip.ZipEntry.DEFLATED;
import static de.schlichtherle.truezip.io.zip.ZipEntry.STORED;
import static de.schlichtherle.truezip.io.zip.ZipEntry.UNKNOWN;

/**
 * An implementation of {@link OutputArchive} to write ZIP archives.
 * <p>
 * This output archive can only write one entry at a time.
 * Archive drivers may wrap this class in a
 * {@link MultiplexedOutputArchive}
 * to overcome this limitation.
 * 
 * @see ZipDriver
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ZipOutputArchive
extends BasicZipOutputStream<ZipEntry>
implements OutputArchive<ZipEntry> {

    private final ZipInputArchive source;
    private OutputArchiveMetaData metaData;
    private ZipEntry tempEntry;

    /**
     * Creates a new instance which uses the output stream, character set and
     * compression level.
     *
     * @param level The compression level to use.
     * @throws IllegalArgumentException If {@code level} is not in the
     *         range [{@value java.util.zip.Deflater#BEST_SPEED}..{@value java.util.zip.Deflater#BEST_COMPRESSION}]
     *         and is not {@value java.util.zip.Deflater#DEFAULT_COMPRESSION}.
     */
    public ZipOutputArchive(
            final OutputStream out,
            final String charset,
            final int level,
            final ZipInputArchive source)
    throws  NullPointerException,
            UnsupportedEncodingException,
            IOException {
        super(out, charset);
        super.setLevel(level);

        this.source = source;
        if (source != null) {
            // Retain comment and preamble of input ZIP archive.
            super.setComment(source.getComment());
            if (source.getPreambleLength() > 0) {
                final InputStream in = source.getPreambleInputStream();
                try {
                    de.schlichtherle.truezip.io.file.File.cat(
                            in, source.offsetsConsiderPreamble() ? this : out);
                } finally {
                    in.close();
                }
            }
        }
    }

    @Override
    public int size() {
        return size() + (tempEntry != null ? 1 : 0);
    }

    @Override
    public Iterator<ZipEntry> iterator() {
        if (tempEntry == null)
            return super.iterator();
        return new JointIterator<ZipEntry>(
                super.iterator(),
                Collections.singletonList(tempEntry).iterator());
    }

    @Override
    public ZipEntry getEntry(final String entryName) {
        ZipEntry e = super.getEntry(entryName);
        if (e != null)
            return e;
        e = tempEntry;
        return e != null && entryName.equals(e.getName()) ? e : null;
    }

    @Override
    public ArchiveOutputStreamSocket<ZipEntry> getOutputStreamSocket(
            final ZipEntry entry)
    throws FileNotFoundException {
        class OutputStreamProxy implements ArchiveOutputStreamSocket<ZipEntry> {
            @Override
            public ZipEntry get() {
                return entry;
            }

            @Override
            public OutputStream newOutputStream(
                    final IOReference<? extends ArchiveEntry> src)
            throws IOException {
                return ZipOutputArchive.this.newOutputStream(entry, src);
            }
        } // class OutputStreamProxy
        return new OutputStreamProxy();
    }

    protected OutputStream newOutputStream(
            final ZipEntry entry,
            final IOReference<? extends ArchiveEntry> src)
    throws IOException {
        if (isBusy())
            throw new OutputArchiveBusyException(entry);

        if (entry.isDirectory()) {
            entry.setMethod(STORED);
            entry.setCrc(0);
            entry.setCompressedSize(0);
            entry.setSize(0);
            return new EntryOutputStream(entry);
        }

        final ArchiveEntry srcEntry = IOReferences.deref(src);
        if (srcEntry != null) {
            entry.setSize(srcEntry.getSize());
            if (srcEntry instanceof ZipEntry) {
                // Set up entry attributes for Direct Data Copying (DDC).
                // A preset method in the entry takes priority.
                // The ZIP.RAES drivers use this feature to enforce deflation
                // for enhanced authentication security.
                final ZipEntry srcZipEntry = (ZipEntry) srcEntry;
                if (entry.getMethod() == UNKNOWN)
                    entry.setMethod(srcZipEntry.getMethod());
                if (entry.getMethod() == srcZipEntry.getMethod())
                    entry.setCompressedSize(srcZipEntry.getCompressedSize());
                entry.setCrc(srcZipEntry.getCrc());
                return new EntryOutputStream(
                        entry, srcZipEntry.getMethod() != ZipEntry.DEFLATED);
            }
        }

        switch (entry.getMethod()) {
            case UNKNOWN:
                entry.setMethod(DEFLATED);
                break;

            case STORED:
                if (entry.getCrc() == UNKNOWN
                        || entry.getCompressedSize() == UNKNOWN
                        || entry.getSize() == UNKNOWN) {
                    if (!(src instanceof InputStreamSocket))
                        return new TempEntryOutputStream(
                                createTempFile(TEMP_FILE_PREFIX), entry);
                    final InputStream in = ((InputStreamSocket) src)
                            .newInputStream(IOReferences.ref(null));
                    final Crc32OutputStream out = new Crc32OutputStream();
                    Streams.copy(in, out);
                    entry.setCrc(out.crc.getValue());
                    entry.setCompressedSize(srcEntry.getSize()); // STORED!
                    entry.setSize(srcEntry.getSize());
                }
                break;

            case DEFLATED:
                break;

            default:
                assert false : "unsupported method";
        }
        return new EntryOutputStream(entry);
    }

    /**
     * Returns whether this output archive is busy writing an archive entry
     * or not.
     */
    @Override
    public final boolean isBusy() {
        return super.isBusy() || tempEntry != null;
    }

    /**
     * This entry output stream writes directly to our subclass.
     * It can only be used if this output stream is not currently busy
     * writing another entry and the entry holds enough information to
     * write the entry header.
     * These preconditions are checked by {@link #newOutputStream}.
     */
    private class EntryOutputStream extends FilterOutputStream {
        EntryOutputStream(ZipEntry entry) throws IOException {
            this(entry, true);
        }

        EntryOutputStream(ZipEntry entry, boolean deflate)
        throws IOException {
            super(ZipOutputArchive.this);
            putNextEntry(entry, deflate);
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
            closeEntry();
        }
    } // class EntryOutputStream

    /**
     * This entry output stream writes the entry to a temporary file.
     * When the stream is closed, the temporary file is then copied to this
     * output stream and finally deleted.
     */
    private class TempEntryOutputStream extends CheckedOutputStream {
        private final File temp;
        private boolean closed;

        TempEntryOutputStream(final File temp, final ZipEntry entry)
        throws IOException {
            super(new java.io.FileOutputStream(temp), new CRC32());
            assert entry.getMethod() == STORED;
            this.temp = temp;
            tempEntry = entry;
        }

        @Override
        public void close() throws IOException {
            if (closed)
                return;

            // Order is important here!
            closed = true;
            try {
                try {
                    super.close();
                } finally {
                    final long length = temp.length();
                    if (length > Integer.MAX_VALUE)
                        throw new IOException("file too large");
                    tempEntry.setCrc(getChecksum().getValue());
                    tempEntry.setCompressedSize(length);
                    tempEntry.setSize(length);
                    store();
                }
            } finally {
                tempEntry = null;
            }
        }

        void store()
        throws IOException {
            assert tempEntry.getMethod() == STORED;
            assert tempEntry.getCrc() != UNKNOWN;
            assert tempEntry.getCompressedSize() != UNKNOWN;
            assert tempEntry.getSize() != UNKNOWN;

            try {
                final InputStream in = new FileInputStream(temp);
                try {
                    putNextEntry(tempEntry);
                    try {
                        Streams.cat(in, this);
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

    private static class Crc32OutputStream extends OutputStream {
        private final CRC32 crc = new CRC32();

        public void write(int b) {
            crc.update(b);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            crc.update(b, off, len);
        }
    } // class Crc32OutputStream

    /**
     * Retain the postamble of the source ZIP archive, if any.
     */
    @Override
    public void finish() throws IOException {
        super.finish();

        if (source == null)
            return;

        final long ipl = source.getPostambleLength();
        if (ipl <= 0)
            return;

        final long il = source.length();
        final long ol = length();

        final InputStream in = source.getPostambleInputStream();
        try {
            // Second, if the output ZIP compatible file differs in length from
            // the input ZIP compatible file pad the output to the next four byte
            // boundary before appending the postamble.
            // This might be required for self extracting files on some platforms
            // (e.g. Wintel).
            if (ol + ipl != il)
                write(new byte[(int) (ol % 4)]);

            // Finally, write the postamble.
            Streams.cat(in, this);
        } finally {
            in.close();
        }
    }

    public OutputArchiveMetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(final OutputArchiveMetaData metaData) {
        this.metaData = metaData;
    }
}
