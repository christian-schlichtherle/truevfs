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
package de.schlichtherle.truezip.io.fs.archive.driver.zip;

import de.schlichtherle.truezip.io.socket.IOPool;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.io.fs.archive.MultiplexedArchiveOutputShop;
import de.schlichtherle.truezip.io.socket.OutputShop;
import de.schlichtherle.truezip.io.OutputBusyException;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.io.fs.file.TempFilePool;
import de.schlichtherle.truezip.io.zip.RawZipOutputStream;
import de.schlichtherle.truezip.util.JointIterator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Iterator;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;

import static de.schlichtherle.truezip.io.fs.archive.driver.zip.ZipDriver.TEMP_FILE_PREFIX;
import static de.schlichtherle.truezip.io.entry.Entry.Size.DATA;
import static de.schlichtherle.truezip.io.zip.ZipEntry.DEFLATED;
import static de.schlichtherle.truezip.io.zip.ZipEntry.STORED;
import static de.schlichtherle.truezip.io.zip.ZipEntry.UNKNOWN;

/**
 * An implementation of {@link OutputShop} to write ZIP archives.
 * <p>
 * This output archive can only write one entry at a time.
 * Archive drivers may wrap this class in a
 * {@link MultiplexedArchiveOutputShop}
 * to overcome this limitation.
 * 
 * @see ZipDriver
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ZipOutputShop
extends RawZipOutputStream<ZipEntry>
implements OutputShop<ZipEntry> {

    private IOPool.Entry<?> postamble;
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
    public ZipOutputShop(
            final OutputStream out,
            final Charset charset,
            final int level,
            final ZipInputShop source)
    throws  NullPointerException,
            UnsupportedEncodingException,
            IOException {
        // super(out, source); // TODO: Support append strategy!
        super(out, charset);
        super.setLevel(level);

        if (null != source) {
            // Retain comment and preamble of input ZIP archive.
            super.setComment(source.getComment());
            if (0 < source.getPreambleLength()) {
                final InputStream in = source.getPreambleInputStream();
                try {
                    Streams.cat(in, source.offsetsConsiderPreamble() ? this : out);
                } finally {
                    in.close();
                }
            }
            if (0 < source.getPostambleLength()) {
                postamble = TempFilePool.get().allocate();
                Streams.copy(   source.getPostambleInputStream(),
                                postamble.getOutputSocket().newOutputStream());
            } else {
                postamble = null;
            }
        } else {
            postamble = null;
        }
    }

    @Override
    public int getSize() {
        return super.size() + (null != tempEntry ? 1 : 0);
    }

    @Override
    public Iterator<ZipEntry> iterator() {
        if (null == tempEntry)
            return super.iterator();
        return new JointIterator<ZipEntry>(
                super.iterator(),
                Collections.singletonList(tempEntry).iterator());
    }

    @Override
    public ZipEntry getEntry(final String name) {
        ZipEntry entry = super.getEntry(name);
        if (null != entry)
            return entry;
        entry = tempEntry;
        return entry != null && name.equals(entry.getName()) ? entry : null;
    }

    @Override
    public OutputSocket<ZipEntry> getOutputSocket(final ZipEntry entry) {
        if (null == entry)
            throw new NullPointerException();

        class Output extends OutputSocket<ZipEntry> {
            @Override
            public ZipEntry getLocalTarget() {
                return entry;
            }

            @Override
            public OutputStream newOutputStream()
            throws IOException {
                if (isBusy())
                    throw new OutputBusyException(entry.getName());
                if (entry.isDirectory()) {
                    entry.setMethod(STORED);
                    entry.setCrc(0);
                    entry.setCompressedSize(0);
                    entry.setSize(0);
                    return new EntryOutputStream(entry);
                }
                final Entry peer = getPeerTarget();
                long size;
                if (null != peer && UNKNOWN != (size = peer.getSize(DATA))) {
                    entry.setSize(size);
                    if (peer instanceof ZipEntry) {
                        // Set up entry attributes for Direct Data Copying (DDC).
                        // A preset method in the entry takes priority.
                        // The ZIP.RAES drivers use this feature to enforce
                        // deflation for enhanced authentication security.
                        final ZipEntry zipPeer = (ZipEntry) peer;
                        if (entry.getMethod() == UNKNOWN)
                            entry.setMethod(zipPeer.getMethod());
                        if (entry.getMethod() == zipPeer.getMethod())
                            entry.setCompressedSize(zipPeer.getCompressedSize());
                        entry.setCrc(zipPeer.getCrc());
                        return new EntryOutputStream(
                                entry, zipPeer.getMethod() != DEFLATED);
                    }
                }
                switch (entry.getMethod()) {
                    case UNKNOWN:
                        entry.setMethod(DEFLATED);
                        break;

                    case STORED:
                        if (entry.getCrc() == UNKNOWN
                                || entry.getCompressedSize() == UNKNOWN
                                || entry.getSize() == UNKNOWN)
                            return new TempEntryOutputStream(
                                    File.createTempFile(TEMP_FILE_PREFIX, null), // TODO: Use TempFilePool!
                                    entry);
                        break;

                    case DEFLATED:
                        break;

                    default:
                        assert false : "unsupported method";
                }
                return new EntryOutputStream(entry);
            }
        } // class Output

        return new Output();
    }

    /**
     * Returns whether this output archive is busy writing an archive entry
     * or not.
     */
    @Override
    public final boolean isBusy() {
        return super.isBusy() || null != tempEntry;
    }

    /**
     * This entry output stream writes directly to our subclass.
     * It can only be used if this output stream is not currently busy
     * writing another entry and the entry holds enough information to
     * write the entry header.
     * These preconditions are checked by {@link #getOutputSocket(ZipEntry) t}.
     */
    private class EntryOutputStream extends DecoratingOutputStream {
        EntryOutputStream(ZipEntry entry) throws IOException {
            this(entry, true);
        }

        EntryOutputStream(ZipEntry entry, boolean deflate)
        throws IOException {
            super(ZipOutputShop.this);
            putNextEntry(entry, deflate);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            delegate.write(b, off, len);
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
                if (!temp.delete()) {
                    // Windoze: Most probably in.close() failed.
                    throw new IOException(temp + " (could not delete)");
                }
            }
        }
    } // class TempEntryOutputStream

    /**
     * Retains the postamble of the source source ZIP file, if any.
     */
    @Override
    public void close() throws IOException {
        try {
            final IOPool.Entry<?> postamble = this.postamble;
            if (null != postamble) {
                this.postamble = null;
                try {
                    final InputSocket<?> input = postamble.getInputSocket();
                    final InputStream in = input.newInputStream();
                    try {
                    // Second, if the output ZIP compatible file differs in length from
                    // the input ZIP compatible file pad the output to the next four byte
                    // boundary before appending the postamble.
                    // This might be required for self extracting files on some platforms
                    // (e.g. Wintel).
                    final long ol = length();
                    final long ipl = input.getLocalTarget().getSize(DATA);
                    if ((ol + ipl) % 4 != 0)
                        write(new byte[4 - (int) (ol % 4)]);

                        Streams.cat(in, this);
                    } finally {
                        in.close();
                    }
                } finally {
                    postamble.release();
                }
            }
        } finally {
            super.close();
        }
    }
}
