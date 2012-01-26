/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.entry.Entry;
import static de.schlichtherle.truezip.entry.Entry.Size.DATA;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.archive.FsMultiplexedOutputShop;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.io.OutputBusyException;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputShop;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.JointIterator;
import de.schlichtherle.truezip.zip.RawZipOutputStream;
import de.schlichtherle.truezip.zip.ZipCryptoParameters;
import static de.schlichtherle.truezip.zip.ZipEntry.STORED;
import static de.schlichtherle.truezip.zip.ZipEntry.UNKNOWN;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import net.jcip.annotations.NotThreadSafe;

/**
 * An output shop for writing ZIP files.
 * This output shop can only write one entry at a time.
 * Archive drivers may wrap this class in a
 * {@link FsMultiplexedOutputShop} to overcome this limitation.
 * 
 * @see     ZipInputShop
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public class ZipOutputShop
extends RawZipOutputStream<ZipArchiveEntry>
implements OutputShop<ZipArchiveEntry> {

    private final ZipDriver driver;
    private final FsModel model;
    private @CheckForNull IOPool.Entry<?> postamble;
    private @CheckForNull ZipArchiveEntry tempEntry;
    private ZipCryptoParameters param;

    public ZipOutputShop(   final ZipDriver driver,
                            final FsModel model,
                            final OutputStream out,
                            final @CheckForNull ZipInputShop source)
    throws IOException {
        super(  out,
                null != source && source.isAppendee() ? source : null,
                driver);
        if (null == model)
            throw new NullPointerException();
        this.driver = driver;
        this.model = model;
        if (null != source) {
            if (!source.isAppendee()) {
                // Retain comment and preamble of input ZIP archive.
                super.setComment(source.getComment());
                if (0 < source.getPreambleLength()) {
                    final InputStream in = source.getPreambleInputStream();
                    try {
                        Streams.cat(in,
                                source.offsetsConsiderPreamble() ? this : out);
                    } finally {
                        in.close();
                    }
                }
            }
            // Retain postamble of input ZIP file.
            if (0 < source.getPostambleLength()) {
                this.postamble = getPool().allocate();
                Streams.copy(   source.getPostambleInputStream(),
                                postamble.getOutputSocket().newOutputStream());
            }
        }
    }

    /**
     * Returns the file system model provided to the constructor.
     * 
     * @return The file system model provided to the constructor.
     * @since  TrueZIP 7.3
     */
    public FsModel getModel() {
        return model;
    }

    private IOPool<?> getPool() {
        return driver.getPool();
    }

    @Override
    protected ZipCryptoParameters getCryptoParameters() {
        ZipCryptoParameters param = this.param;
        if (null == param)
            this.param = param = driver.zipCryptoParameters(this);
        return param;
    }

    @Override
    public int getSize() {
        return super.size() + (null != this.tempEntry ? 1 : 0);
    }

    @Override
    public Iterator<ZipArchiveEntry> iterator() {
        final ZipArchiveEntry tempEntry = this.tempEntry;
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
        return null != entry && name.equals(entry.getName()) ? entry : null;
    }

    @Override
    public OutputSocket<ZipArchiveEntry> getOutputSocket(final ZipArchiveEntry entry) { // local target
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
                final Entry peer;
                final long size;
                boolean process = true;
                if (entry.isDirectory()) {
                    entry.setMethod(STORED);
                    entry.setCrc(0);
                    entry.setCompressedSize(0);
                    entry.setSize(0);
                    return new EntryOutputStream(entry, true);
                } else if (null != (peer = getPeerTarget())
                        && UNKNOWN != (size = peer.getSize(DATA))) {
                    entry.setSize(size);
                    if (peer instanceof ZipArchiveEntry) {
                        // Set up entry attributes for Direct Data Copying (DDC).
                        // A preset method in the entry takes priority.
                        // The ZIP.RAES drivers use this feature to enforce
                        // deflation for enhanced authentication security.
                        final ZipArchiveEntry zpt = (ZipArchiveEntry) peer;
                        process = driver.process(ZipOutputShop.this, entry, zpt);
                        if (!process) {
                            entry.setPlatform(zpt.getPlatform());
                            entry.setEncrypted(zpt.isEncrypted());
                            entry.setMethod(zpt.getMethod());
                            entry.setCompressedSize(zpt.getCompressedSize());
                            entry.setCrc(zpt.getCrc());
                            entry.setExtra(zpt.getExtra());
                        }
                    }
                }
                if (STORED == entry.getMethod()) {
                    if (0 == entry.getSize()) {
                        entry.setCompressedSize(0);
                        entry.setCrc(0);
                    } else if (UNKNOWN == entry.getSize()
                            || UNKNOWN == entry.getCompressedSize()
                            || UNKNOWN == entry.getCrc()) {
                        assert process : "The CRC-32, compressed size and size properties should be set in the peer target!";
                        return new BufferedEntryOutputStream(
                                getPool().allocate(), entry, process);
                    }
                }
                return new EntryOutputStream(entry, process);
            }
        } // Output

        return new Output();
    }

    /**
     * Returns whether this output archive is busy writing an archive entry
     * or not.
     */
    @Override
    public final boolean isBusy() {
        return super.isBusy() || null != this.tempEntry;
    }

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

    /**
     * This entry output stream writes directly to this output shop.
     * It can only be used if this output shop is not currently busy with
     * writing another entry and the entry holds enough information to write
     * the entry header.
     * These preconditions are checked by
     * {@link #getOutputSocket(ZipArchiveEntry)}.
     */
    private final class EntryOutputStream extends DecoratingOutputStream {
        EntryOutputStream(ZipArchiveEntry entry, boolean process)
        throws IOException {
            super(ZipOutputShop.this);
            putNextEntry(entry, process);
        }

        @Override
        public void close() throws IOException {
            closeEntry();
        }
    } // EntryOutputStream

    /**
     * This entry output stream writes the ZIP archive entry to an
     * {@link de.schlichtherle.truezip.socket.IOPool.Entry I/O pool entry}.
     * When the stream gets closed, the I/O pool entry is then copied to this
     * output shop and finally deleted.
     */
    private final class BufferedEntryOutputStream extends CheckedOutputStream {
        final IOPool.Entry<?> temp;
        final boolean process;
        boolean closed;

        BufferedEntryOutputStream(
                final IOPool.Entry<?> temp,
                final ZipArchiveEntry entry,
                final boolean process)
        throws IOException {
            super(temp.getOutputSocket().newOutputStream(), new CRC32());
            assert STORED == entry.getMethod();
            this.temp = temp;
            ZipOutputShop.this.tempEntry = entry;
            this.process = process;
        }

        @Override
        public void close() throws IOException {
            if (closed)
                return;
            closed = true;
            try {
                try {
                    super.close();
                } finally {
                    final long length = temp.getSize(DATA);
                    final ZipArchiveEntry tempEntry = ZipOutputShop.this.tempEntry;
                    assert null != tempEntry;
                    assert STORED == tempEntry.getMethod();
                    tempEntry.setCrc(getChecksum().getValue());
                    tempEntry.setCompressedSize(length);
                    tempEntry.setSize(length);
                    store();
                }
            } finally {
                ZipOutputShop.this.tempEntry = null;
            }
        }

        void store() throws IOException {
            try {
                final InputStream in = temp.getInputSocket().newInputStream();
                try {
                    final ZipArchiveEntry tempEntry = ZipOutputShop.this.tempEntry;
                    assert null != tempEntry;
                    putNextEntry(tempEntry, this.process);
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
    } // BufferedEntryOutputStream
}
