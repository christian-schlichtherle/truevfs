/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.tar;

import de.truezip.kernel.FsEntryName;
import static de.truezip.kernel.FsEntryName.SEPARATOR;
import static de.truezip.kernel.FsEntryName.SEPARATOR_CHAR;
import de.truezip.kernel.FsModel;
import de.truezip.kernel.cio.IOBuffer;
import de.truezip.kernel.cio.IOPool;
import de.truezip.kernel.cio.InputService;
import de.truezip.kernel.cio.InputSocket;
import de.truezip.kernel.io.Source;
import de.truezip.kernel.io.Streams;
import static de.truezip.kernel.util.Maps.initialCapacity;
import de.truezip.kernel.util.SuppressedExceptionBuilder;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.*;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.NoSuchFileException;
import java.util.*;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import static org.apache.commons.compress.archivers.tar.TarConstants.*;
import org.apache.commons.compress.archivers.tar.TarUtils;

/**
 * Presents a {@link TarArchiveInputStream} as a randomly accessible archive.
 * <p>
 * <b>Warning:</b> 
 * The constructor of this class extracts each entry in the archive to a
 * temporary file.
 * This may be very time and space consuming for large archives, but is
 * the fastest implementation for subsequent random access, since there
 * is no way the archive driver could predict the client application's
 * behaviour.
 *
 * @see    TarOutputService
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public final class TarInputService
implements InputService<TarDriverEntry> {

    /** Default record size */
    private static final int DEFAULT_RCDSIZE = 512;

    /** Default block size */
    public static final int DEFAULT_BLKSIZE = 20 * DEFAULT_RCDSIZE * 20;

    private static final byte[] NULL_RECORD = new byte[DEFAULT_RCDSIZE];

    private static final int CHECKSUM_OFFSET
            = NAMELEN + MODELEN + UIDLEN + GIDLEN + SIZELEN + MODTIMELEN;

    /** Maps entry names to I/O pool entries. */
    private final Map<String, TarDriverEntry>
            entries = new LinkedHashMap<>(
                    initialCapacity(TarOutputService.OVERHEAD_SIZE));

    /**
     * Extracts the entire TAR input stream into a temporary directory in order
     * to allow subsequent random access to its entries.
     */
    @CreatesObligation
    public TarInputService(
            final FsModel model,
            final Source source,
            final TarDriver driver)
    throws IOException {
        if (null == model)
            throw new NullPointerException();
        final InputStream in = source.stream();
        Throwable ex = null;
        try {
            unpack(newValidatedTarInputStream(in), driver);
        } catch (final Throwable ex2) {
            ex = ex2;
            try {
                close0();
            } catch (final IOException ex3) {
                ex2.addSuppressed(ex3);
            }
            throw ex2;
        } finally {
            try {
                in.close();
            } catch (final IOException ex2) {
                if (null == ex)
                    throw ex2;
                ex.addSuppressed(ex2);
            }
        }
    }

    private void unpack(final TarArchiveInputStream tin,
                        final TarDriver driver) throws IOException {
        final IOPool<?> pool = driver.getIOPool();
        for (   TarArchiveEntry tinEntry;
                null != (tinEntry = tin.getNextTarEntry()); ) {
            final String name = getName(tinEntry);
            TarDriverEntry entry = entries.get(name);
            if (null != entry)
                entry.release();
            entry = driver.newEntry(name, tinEntry);
            if (!tinEntry.isDirectory()) {
                final IOBuffer<?> temp = pool.allocate();
                entry.setTemp(temp);
                try {
                    Throwable ex = null;
                    final OutputStream out = temp.outputSocket().stream();
                    try {
                        Streams.cat(tin, out);
                    } catch (final Throwable ex2) {
                        ex = ex2;
                        throw ex2;
                    } finally {
                        try {
                            out.close();
                        } catch (final IOException ex2) {
                            if (null == ex)
                                throw ex2;
                            ex.addSuppressed(ex2);
                        }
                    }
                } catch (final Throwable ex) {
                    try {
                        temp.release();
                    } catch (final IOException ex2) {
                        ex.addSuppressed(ex2);
                    }
                    throw ex;
                }
            }
            entries.put(name, entry);
        }
    }

    /**
     * Returns the fixed name of the given TAR entry, ensuring that it ends
     * with a {@link FsEntryName#SEPARATOR} if it's a directory.
     *
     * @param entry the TAR entry.
     * @return the fixed name of the given TAR entry.
     * @see <a href="http://java.net/jira/browse/TRUEZIP-62">Issue TRUEZIP-62</a>
     */
    private static String getName(ArchiveEntry entry) {
        final String name = entry.getName();
        return entry.isDirectory() && !name.endsWith(SEPARATOR) ? name + SEPARATOR_CHAR : name;
    }

    /**
     * Returns a newly created and validated {@link TarArchiveInputStream}.
     * This method performs a simple validation by computing the checksum
     * for the first record only.
     * This method is required because the {@code TarArchiveInputStream}
     * unfortunately does not do any validation!
     */
    private static TarArchiveInputStream
    newValidatedTarInputStream(final InputStream in)
    throws IOException {
        final byte[] buf = new byte[DEFAULT_RCDSIZE];
        final InputStream vin = readAhead(in, buf);
        // If the record is the null record, the TAR file is empty and we're
        // done with validating.
        if (!Arrays.equals(buf, NULL_RECORD)) {
            final long expected;
            try {
                expected = TarUtils.parseOctal(buf, CHECKSUM_OFFSET, 8);
            } catch (final IllegalArgumentException ex) {
                throw new TarException("Illegal initial record in TAR file!", ex);
            }
            for (int i = 0; i < 8; i++)
                buf[CHECKSUM_OFFSET + i] = ' ';
            final long actual = TarUtils.computeCheckSum(buf);
            if (expected != actual)
                throw new TarException(
                        "Illegal initial record in TAR file: Expected / actual checksum := "
                        + expected + " / " + actual + "!");
        }
        return new TarArchiveInputStream(vin, DEFAULT_BLKSIZE, DEFAULT_RCDSIZE);
    }

    /**
     * Fills {@code buf} with data from the given input stream and
     * returns an input stream from which you can still read all data,
     * including the data in buf.
     *
     * @param  in The stream to read from. May <em>not</em> be {@code null}.
     * @param  buf The buffer to fill entirely with data.
     * @return A stream which holds all the data {@code in} did.
     * @throws IOException If {@code buf} couldn't get filled entirely.
     */
    private static InputStream readAhead(final InputStream in, final byte[] buf)
    throws IOException {
        if (in.markSupported()) {
            in.mark(buf.length);
            new DataInputStream(in).readFully(buf);
            in.reset();
            return in;
        } else {
            final PushbackInputStream
                    pin = new PushbackInputStream(in, buf.length);
            new DataInputStream(pin).readFully(buf);
            pin.unread(buf);
            return pin;
        }
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public Iterator<TarDriverEntry> iterator() {
        return Collections.unmodifiableCollection(entries.values()).iterator();
    }

    @Override
    public @CheckForNull TarDriverEntry entry(String name) {
        return entries.get(name);
    }

    @Override
    public InputSocket<TarDriverEntry> inputSocket(final String name) {
        if (null == name)
            throw new NullPointerException();
        final class Input extends InputSocket<TarDriverEntry> {
            @Override
            public TarDriverEntry localTarget() throws IOException {
                final TarDriverEntry entry = entry(name);
                if (null == entry)
                    throw new NoSuchFileException(name, null, "Entry not found!");
                if (entry.isDirectory())
                    throw new NoSuchFileException(name, null, "Cannot read directory entries!");
                return entry;
            }

            @Override
            public InputStream stream()
            throws IOException {
                return getInputSocket().stream();
            }

            @Override
            public SeekableByteChannel channel() throws IOException {
                return getInputSocket().channel();
            }

            InputSocket<? extends IOBuffer<?>> getInputSocket()
            throws IOException {
                return localTarget().getTemp().inputSocket();
            }
        } // Input

        return new Input();
    }

    @Override
    public void close() throws IOException {
        close0();
    }

    private void close0() throws IOException {
        final SuppressedExceptionBuilder<IOException>
                builder = new SuppressedExceptionBuilder<>();
        Collection<TarDriverEntry> values = entries.values();
        for (final Iterator<TarDriverEntry> i = values.iterator();
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
