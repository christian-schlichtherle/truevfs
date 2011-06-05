/*
 * Copyright (C) 2009-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.fs.archive.FsMultiplexedArchiveOutputShop;
import de.schlichtherle.truezip.socket.OutputShop;
import de.schlichtherle.truezip.io.OutputBusyException;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.zip.RawZipOutputStream;
import de.schlichtherle.truezip.util.JointIterator;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;

import static de.schlichtherle.truezip.entry.Entry.Size.DATA;
import static de.schlichtherle.truezip.zip.ZipEntry.DEFLATED;
import static de.schlichtherle.truezip.zip.ZipEntry.STORED;
import static de.schlichtherle.truezip.zip.ZipEntry.UNKNOWN;

/**
 * An implementation of {@link OutputShop} to write ZIP archives.
 * <p>
 * This output archive can only write one entry at a time.
 * Archive drivers may wrap this class in a
 * {@link FsMultiplexedArchiveOutputShop}
 * to overcome this limitation.
 * 
 * @see     ZipInputShop
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public class ZipOutputShop
extends RawZipOutputStream<ZipArchiveEntry>
implements OutputShop<ZipArchiveEntry> {

    private final IOPool<?> pool;
    private @CheckForNull IOPool.Entry<?> postamble;
    private @Nullable ZipArchiveEntry tempEntry;

    public ZipOutputShop(   final ZipDriver driver,
                            final OutputStream out,
                            final @CheckForNull ZipInputShop source)
    throws IOException {
        super(out, driver.getCharset());
        super.setMethod(driver.getMethod());
        super.setLevel(driver.getLevel());
        this.pool = driver.getPool();
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
                postamble = pool.allocate();
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
    public Iterator<ZipArchiveEntry> iterator() {
        if (null == tempEntry)
            return super.iterator();
        return new JointIterator<ZipArchiveEntry>(
                super.iterator(),
                Collections.singletonList(tempEntry).iterator());
    }

    @Override
    public @CheckForNull ZipArchiveEntry getEntry(final String name) {
        ZipArchiveEntry entry = super.getEntry(name);
        if (null != entry)
            return entry;
        entry = tempEntry;
        return entry != null && name.equals(entry.getName()) ? entry : null;
    }

    @Override
    public OutputSocket<ZipArchiveEntry> getOutputSocket(final ZipArchiveEntry entry) {
        if (null == entry)
            throw new NullPointerException();

        class Output extends OutputSocket<ZipArchiveEntry> {
            @Override
            public ZipArchiveEntry getLocalTarget() {
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
                    if (peer instanceof ZipArchiveEntry) {
                        // Set up entry attributes for Direct Data Copying (DDC).
                        // A preset method in the entry takes priority.
                        // The ZIP.RAES drivers use this feature to enforce
                        // deflation for enhanced authentication security.
                        final ZipArchiveEntry zipPeer = (ZipArchiveEntry) peer;
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
                        if (       UNKNOWN == entry.getCrc()
                                || UNKNOWN == entry.getCompressedSize()
                                || UNKNOWN == entry.getSize())
                            return new StoredEntryOutputStream(
                                    pool.allocate(),
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
     * These preconditions are checked by {@link #getOutputSocket(ZipArchiveEntry) t}.
     */
    private class EntryOutputStream extends DecoratingOutputStream {
        EntryOutputStream(ZipArchiveEntry entry) throws IOException {
            this(entry, true);
        }

        EntryOutputStream(ZipArchiveEntry entry, boolean deflate)
        throws IOException {
            super(ZipOutputShop.this);
            putNextEntry(entry, deflate);
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
    private class StoredEntryOutputStream extends CheckedOutputStream {
        private final IOPool.Entry<?> temp;
        private boolean closed;

        StoredEntryOutputStream(final IOPool.Entry<?> temp, final ZipArchiveEntry entry)
        throws IOException {
            super(temp.getOutputSocket().newOutputStream(), new CRC32());
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
                    final long length = temp.getSize(DATA);
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

        void store() throws IOException {
            assert tempEntry.getMethod() == STORED;
            assert tempEntry.getCrc() != UNKNOWN;
            assert tempEntry.getCompressedSize() != UNKNOWN;
            assert tempEntry.getSize() != UNKNOWN;

            try {
                final InputStream in = temp.getInputSocket().newInputStream();
                try {
                    putNextEntry(tempEntry);
                    try {
                        Streams.cat(in, ZipOutputShop.this);
                    } finally {
                        closeEntry();
                    }
                } finally {
                    in.close();
                }
            } finally {
                temp.release();
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
