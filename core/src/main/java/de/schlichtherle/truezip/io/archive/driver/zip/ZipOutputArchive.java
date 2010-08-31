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

package de.schlichtherle.truezip.io.archive.driver.zip;

import de.schlichtherle.truezip.io.archive.controller.OutputArchiveMetaData;
import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.driver.OutputArchive;
import de.schlichtherle.truezip.io.archive.driver.OutputArchiveBusyException;
import de.schlichtherle.truezip.io.archive.driver.RfsEntry;
import de.schlichtherle.truezip.util.JointEnumeration;
import de.schlichtherle.truezip.io.zip.BasicZipOutputStream;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;

import static de.schlichtherle.truezip.io.util.Files.createTempFile;

/**
 * An implementation of {@link OutputArchive} to write ZIP archives.
 * <p>
 * This output archive can only write one entry at a time.
 * Archive drivers may wrap this class in a
 * {@link de.schlichtherle.truezip.io.archive.driver.MultiplexedOutputArchive}
 * to overcome this limitation.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 * @see ZipDriver
 */
public class ZipOutputArchive
        extends BasicZipOutputStream
        implements OutputArchive {

    /** Prefix for temporary files created by the multiplexer. */
    private static final String TEMP_FILE_PREFIX = ZipDriver.TEMP_FILE_PREFIX;

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
                    de.schlichtherle.truezip.io.File.cat(
                            in, source.offsetsConsiderPreamble() ? this : out);
                } finally {
                    in.close();
                }
            }
        }
    }

    public int getNumArchiveEntries() {
        return size() + (tempEntry != null ? 1 : 0);
    }

    public Enumeration<? extends ZipEntry> getArchiveEntries() {
        if (tempEntry == null)
            return (Enumeration<? extends ZipEntry>) super.entries();
        return new JointEnumeration<ZipEntry>(
                (Enumeration<? extends ZipEntry>) super.entries(),
                Collections.enumeration(
                    Collections.singletonList(tempEntry)));
    }

    public ArchiveEntry getArchiveEntry(final String entryName) {
        ZipEntry e = (ZipEntry) getEntry(entryName);
        if (e != null)
            return e;
        e = tempEntry;
        return e != null && entryName.equals(e.getName()) ? e : null;
    }

    public OutputStream newOutputStream(
            final ArchiveEntry dstEntry,
            final ArchiveEntry srcEntry)
    throws IOException {
        final ZipEntry entry = (ZipEntry) dstEntry;

        if (isBusy())
            throw new OutputArchiveBusyException(entry);

        if (entry.isDirectory()) {
            entry.setMethod(ZipEntry.STORED);
            entry.setCrc(0);
            entry.setCompressedSize(0);
            entry.setSize(0);
            return new EntryOutputStream(entry);
        }

        if (srcEntry instanceof ZipEntry) {
            // Set up entry attributes for Direct Data Copying (DDC).
            // A preset method in the entry takes priority.
            // The ZIP.RAES drivers use this feature to enforce deflation
            // for enhanced authentication security.
            final ZipEntry srcZipEntry = (ZipEntry) srcEntry;
            if (entry.getMethod() == ZipEntry.UNKNOWN)
                entry.setMethod(srcZipEntry.getMethod());
            if (entry.getMethod() == srcZipEntry.getMethod())
                entry.setCompressedSize(srcZipEntry.getCompressedSize());
            entry.setCrc(srcZipEntry.getCrc());
            entry.setSize(srcZipEntry.getSize());
            return new EntryOutputStream(
                    entry, srcZipEntry.getMethod() != ZipEntry.DEFLATED);
        }

        if (srcEntry != null)
            entry.setSize(srcEntry.getSize());

        switch (entry.getMethod()) {
            case ZipEntry.UNKNOWN:
                entry.setMethod(ZipEntry.DEFLATED);
                break;

            case ZipEntry.STORED:
                if (entry.getCrc() == ZipEntry.UNKNOWN
                        || entry.getCompressedSize() == ZipEntry.UNKNOWN
                        || entry.getSize() == ZipEntry.UNKNOWN) {
                    if (!(srcEntry instanceof RfsEntry)) {
                        final File temp = createTempFile(TEMP_FILE_PREFIX);
                        return new TempEntryOutputStream(entry, temp);
                    }
                    final File file = ((RfsEntry) srcEntry).getFile();
                    final long length = file.length();
                    // No longer needed with ZIP64 support:
                    /*if (length > Integer.MAX_VALUE)
                        throw new IOException("file too large");*/
                    final InputStream in = new java.io.FileInputStream(file);
                    final Crc32OutputStream out = new Crc32OutputStream();
                    de.schlichtherle.truezip.io.File.cp(in, out);
                    entry.setCrc(out.crc.getValue());
                    entry.setCompressedSize(length);
                    entry.setSize(length);
                }
                break;

            case ZipEntry.DEFLATED:
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
        private EntryOutputStream(ZipEntry entry) throws IOException {
            this(entry, true);
        }

        private EntryOutputStream(ZipEntry entry, boolean deflate)
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

        public TempEntryOutputStream(
                final ZipEntry entry,
                final File temp)
        throws IOException {
            super(new java.io.FileOutputStream(temp), new CRC32());
            assert entry.getMethod() == ZipEntry.STORED;
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
                    storeTempEntry(tempEntry, temp);
                }
            } finally {
                tempEntry = null;
            }
        }
    } // class TempEntryOutputStream

    private void storeTempEntry(
            final ZipEntry entry,
            final File temp)
    throws IOException {
        assert entry.getMethod() == ZipEntry.STORED;
        assert entry.getCrc() != ZipEntry.UNKNOWN;
        assert entry.getCompressedSize() != ZipEntry.UNKNOWN;
        assert entry.getSize() != ZipEntry.UNKNOWN;

        try {
            final InputStream in = new java.io.FileInputStream(temp);
            try {
                putNextEntry(entry);
                try {
                    de.schlichtherle.truezip.io.File.cat(in, this);
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
            de.schlichtherle.truezip.io.File.cat(in, this);
        } finally {
            in.close();
        }
    }

    //
    // Metadata implementation.
    //

    public OutputArchiveMetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(final OutputArchiveMetaData metaData) {
        this.metaData = metaData;
    }
}
