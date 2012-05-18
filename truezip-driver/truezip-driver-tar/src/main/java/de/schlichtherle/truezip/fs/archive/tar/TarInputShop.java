/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.tar;

import de.schlichtherle.truezip.entry.Entry.Type;
import static de.schlichtherle.truezip.entry.Entry.Type.DIRECTORY;
import static de.schlichtherle.truezip.entry.Entry.Type.FILE;
import de.schlichtherle.truezip.fs.archive.FsCharsetArchiveDriver;
import de.schlichtherle.truezip.io.SequentialIOException;
import de.schlichtherle.truezip.io.SequentialIOExceptionBuilder;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.IOEntry;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.IOPool.Entry;
import de.schlichtherle.truezip.socket.InputShop;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.util.JSE7;
import static de.schlichtherle.truezip.util.HashMaps.OVERHEAD_SIZE;
import static de.schlichtherle.truezip.util.HashMaps.initialCapacity;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.*;
import java.nio.channels.SeekableByteChannel;
import java.util.*;
import javax.annotation.CheckForNull;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.NotThreadSafe;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import static org.apache.commons.compress.archivers.tar.TarConstants.*;
import org.apache.commons.compress.archivers.tar.TarUtils;

/**
 * An input shop for reading TAR files.
 * <p>
 * Note that the constructor of this class extracts each entry in the archive
 * to a temporary file.
 * This may be very time and space consuming for large archives, but is
 * the fastest implementation for subsequent random access, since there
 * is no way the archive driver could predict the client application's
 * behaviour.
 *
 * @see    TarOutputShop
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class TarInputShop
implements InputShop<TarDriverEntry> {

    /** Default record size */
    private static final int DEFAULT_RCDSIZE = 512;

    /** Default block size */
    public static final int DEFAULT_BLKSIZE = 20 * DEFAULT_RCDSIZE * 20;

    private static final byte[] NULL_RECORD = new byte[DEFAULT_RCDSIZE];

    private static final int CHECKSUM_OFFSET
            = NAMELEN + MODELEN + UIDLEN + GIDLEN + SIZELEN + MODTIMELEN;

    /** HashMaps entry names to I/O pool entries. */
    private final Map<String, TarDriverEntry>
            entries = new LinkedHashMap<String, TarDriverEntry>(
                    initialCapacity(OVERHEAD_SIZE));

    private final TarDriver driver;

    @CreatesObligation
    public TarInputShop(final TarDriver driver,
                        final @WillNotClose InputStream in)
    throws EOFException, IOException {
        if (null == (this.driver = driver))
            throw new NullPointerException();
        try {
            unpack(newValidatedTarArchiveInputStream(in));
        } catch (final IOException ex) {
            try {
                close0();
            } catch (final Throwable ex2) {
                if (JSE7.AVAILABLE) ex.addSuppressed(ex2);
            }
            throw ex;
        }
    }

    private void unpack(final @WillNotClose TarArchiveInputStream tin)
    throws IOException {
        final TarDriver driver = this.driver;
        final IOPool<?> pool = driver.getPool();
        for (   TarArchiveEntry tinEntry;
                null != (tinEntry = tin.getNextTarEntry()); ) {
            final String name = name(tinEntry);
            TarDriverEntry entry = entries.get(name);
            if (null != entry)
                entry.release();
            entry = driver.newEntry(name, tinEntry);
            if (!tinEntry.isDirectory()) {
                final Entry<?> temp = pool.allocate();
                entry.setTemp(temp);
                try {
                    final OutputStream out = temp.getOutputSocket().newOutputStream();
                    IOException ex = null;
                    try {
                        Streams.cat(tin, out);
                    } catch (final IOException ex2) {
                        ex = ex2;
                        throw ex2;
                    } finally {
                        try {
                            out.close();
                        } catch (final IOException ex2) {
                            if (null == ex)
                                throw ex2;
                            if (JSE7.AVAILABLE) ex.addSuppressed(ex2);
                        }
                    }
                } catch (final IOException ex) {
                    try {
                        temp.release();
                    } catch (final IOException ex2) {
                        if (JSE7.AVAILABLE) ex.addSuppressed(ex2);
                    }
                    throw ex;
                }
            }
            entries.put(name, entry);
        }
    }

    private static String name(final TarArchiveEntry entry) {
        final String name = entry.getName();
        final Type type = entry.isDirectory() ? DIRECTORY : FILE;
        return FsCharsetArchiveDriver.toZipOrTarEntryName(name, type);
    }

    /**
     * Returns a newly created and validated {@link TarArchiveInputStream}.
     * This method performs a simple validation by computing the checksum
     * for the first record only.
     * This method is required because the {@code TarArchiveInputStream}
     * unfortunately does not do any validation!
     * 
     * @param  in the stream to read from.
     * @return A stream which holds all the data {@code in} did.
     * @throws EOFException on unexpected end-of-file.
     * @throws IOException on any I/O error.
     */
    private TarArchiveInputStream
    newValidatedTarArchiveInputStream(final InputStream in)
    throws EOFException, IOException {
        final byte[] buf = new byte[DEFAULT_RCDSIZE];
        final InputStream vin = readAhead(in, buf);
        // If the record is the null record, the TAR file is empty and we're
        // done with validating.
        if (!Arrays.equals(buf, NULL_RECORD)) {
            final long expected;
            try {
                expected = TarUtils.parseOctal(buf, CHECKSUM_OFFSET, 8);
            } catch (IllegalArgumentException ex) {
                throw new IOException("Invalid initial record in TAR file!", ex);
            }
            for (int i = 0; i < 8; i++)
                buf[CHECKSUM_OFFSET + i] = ' ';
            final long actual = TarUtils.computeCheckSum(buf);
            if (expected != actual)
                throw new IOException(
                        "Invalid initial record in TAR file: Expected / actual checksum : "
                        + expected + " / " + actual + "!");
        }
        return new TarArchiveInputStream(   vin,
                                            DEFAULT_BLKSIZE,
                                            DEFAULT_RCDSIZE,
                                            driver.getEncoding());
    }

    /**
     * Fills {@code buf} with data from the given input stream and
     * returns an input stream from which you can still read all data,
     * including the data in buf.
     *
     * @param  in The stream to read from. May <em>not</em> be {@code null}.
     * @param  buf The buffer to fill entirely with data.
     * @return A stream which holds all the data {@code in} did.
     * @throws EOFException on unexpected end-of-file.
     * @throws IOException on any I/O error.
     */
    static InputStream readAhead(final InputStream in, final byte[] buf)
    throws EOFException, IOException {
        if (in.markSupported()) {
            in.mark(buf.length);
            new DataInputStream(in).readFully(buf);
            in.reset();
            return in;
        } else {
            final PushbackInputStream pin
                    = new PushbackInputStream(in, buf.length);
            new DataInputStream(pin).readFully(buf);
            pin.unread(buf);
            return pin;
        }
    }

    @Override
    public final int getSize() {
        return entries.size();
    }

    @Override
    public final Iterator<TarDriverEntry> iterator() {
        return Collections.unmodifiableCollection(entries.values()).iterator();
    }

    @Override
    public final @CheckForNull TarDriverEntry getEntry(String name) {
        return entries.get(name);
    }

    @Override
    public InputSocket<TarDriverEntry> getInputSocket(final String name) {
        if (null == name)
            throw new NullPointerException();

        final class Input extends InputSocket<TarDriverEntry> {
            @Override
            public TarDriverEntry getLocalTarget() throws IOException {
                final TarDriverEntry entry = getEntry(name);
                if (null == entry)
                    throw new FileNotFoundException(name + " (entry not found)");
                if (entry.isDirectory())
                    throw new FileNotFoundException(name + " (cannot read directory entries)");
                return entry;
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                return getInputSocket().newReadOnlyFile();
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel() throws IOException {
                return getInputSocket().newSeekableByteChannel();
            }

            @Override
            public InputStream newInputStream()
            throws IOException {
                return getInputSocket().newInputStream();
            }

            InputSocket<? extends IOEntry<?>> getInputSocket()
            throws IOException {
                return getLocalTarget().getTemp().getInputSocket();
            }
        } // Input

        return new Input();
    }

    @Override
    public void close() throws IOException {
        close0();
    }

    private void close0() throws IOException {
        SequentialIOExceptionBuilder<IOException, SequentialIOException>
                builder = SequentialIOExceptionBuilder.create(IOException.class);
        for (final Iterator<TarDriverEntry> i = entries.values().iterator();
                i.hasNext();
                i.remove()) {
            try {
                i.next().release();
            } catch (final IOException ex) {
                builder.warn(ex);
            }
        }
        builder.check();
    }
}
