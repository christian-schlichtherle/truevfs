/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.tardriver;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truecommons.cio.AbstractInputSocket;
import net.java.truecommons.cio.Entry;
import net.java.truecommons.cio.Entry.Type;
import static net.java.truecommons.cio.Entry.Type.DIRECTORY;
import static net.java.truecommons.cio.Entry.Type.FILE;
import net.java.truecommons.cio.InputService;
import net.java.truecommons.cio.InputSocket;
import net.java.truecommons.cio.IoBuffer;
import net.java.truecommons.cio.IoBufferPool;
import net.java.truecommons.cio.OutputSocket;
import net.java.truecommons.io.Source;
import net.java.truecommons.io.Streams;
import net.java.truecommons.shed.ExceptionBuilder;
import static net.java.truecommons.shed.HashMaps.OVERHEAD_SIZE;
import static net.java.truecommons.shed.HashMaps.initialCapacity;
import net.java.truecommons.shed.SuppressedExceptionBuilder;
import net.java.truevfs.kernel.spec.FsArchiveDriver;
import net.java.truevfs.kernel.spec.FsModel;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import static org.apache.commons.compress.archivers.tar.TarConstants.DEFAULT_BLKSIZE;
import static org.apache.commons.compress.archivers.tar.TarConstants.DEFAULT_RCDSIZE;
import static org.apache.commons.compress.archivers.tar.TarConstants.GIDLEN;
import static org.apache.commons.compress.archivers.tar.TarConstants.MODELEN;
import static org.apache.commons.compress.archivers.tar.TarConstants.MODTIMELEN;
import static org.apache.commons.compress.archivers.tar.TarConstants.NAMELEN;
import static org.apache.commons.compress.archivers.tar.TarConstants.SIZELEN;
import static org.apache.commons.compress.archivers.tar.TarConstants.UIDLEN;
import org.apache.commons.compress.archivers.tar.TarUtils;

/**
 * An input service for reading TAR files.
 * <p>
 * Note that the constructor of this class extracts each entry in the archive
 * to a temporary file!
 * This may be very time and space consuming for large archives, but is
 * the fastest implementation for subsequent random access, since there
 * is no way the archive driver could predict the client application's
 * behavior.
 *
 * @see    TarOutputService
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public final class TarInputService
implements InputService<TarDriverEntry> {

    private static final byte[] NULL_RECORD = new byte[DEFAULT_RCDSIZE];

    private static final int CHECKSUM_OFFSET
            = NAMELEN + MODELEN + UIDLEN + GIDLEN + SIZELEN + MODTIMELEN;

    /** Maps entry names to I/O pool entries. */
    private final Map<String, TarDriverEntry>
            entries = new LinkedHashMap<>(initialCapacity(OVERHEAD_SIZE));

    private final TarDriver driver;

    @CreatesObligation
    public TarInputService(
            final FsModel model,
            final Source source,
            final TarDriver driver)
    throws IOException {
        Objects.requireNonNull(model);
        this.driver = Objects.requireNonNull(driver);
        try (final InputStream in = source.stream()) {
            try {
                unpack(newValidatedTarArchiveInputStream(in));
            } catch (final Throwable ex) {
                try {
                    close0();
                } catch (final Throwable ex2) {
                    ex.addSuppressed(ex2);
                }
                throw ex;
            }
        }
    }

    private void unpack(final @WillNotClose TarArchiveInputStream tain)
    throws IOException {
        final TarDriver driver = this.driver;
        final IoBufferPool pool = driver.getPool();
        for (   TarArchiveEntry tinEntry;
                null != (tinEntry = tain.getNextTarEntry()); ) {
            final String name = name(tinEntry);
            TarDriverEntry entry = entries.get(name);
            if (null != entry)
                entry.release();
            entry = driver.newEntry(name, tinEntry);
            if (!tinEntry.isDirectory()) {
                final IoBuffer buffer = pool.allocate();
                entry.setBuffer(buffer);
                try {
                    try (OutputStream out = buffer.output().stream(null)) {
                        Streams.cat(tain, out);
                    }
                } catch (final Throwable ex) {
                    try {
                        buffer.release();
                    } catch (final Throwable ex2) {
                        ex.addSuppressed(ex2);
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
        return FsArchiveDriver.normalize(name, type);
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
     * @throws IOException on any I/O error.
     */
    private TarArchiveInputStream newValidatedTarArchiveInputStream(
            final @WillNotClose InputStream in)
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
                throw new TarException("Invalid initial record in TAR file!", ex);
            }
            for (int i = 0; i < 8; i++)
                buf[CHECKSUM_OFFSET + i] = ' ';
            final long actual = TarUtils.computeCheckSum(buf);
            if (expected != actual)
                throw new TarException(
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
     * @param  in The stream to read from.
     * @param  buf The buffer to fill entirely with data.
     * @return A stream which holds all the data {@code in} did.
     * @throws EOFException on unexpected end-of-file.
     * @throws IOException on any I/O error.
     */
    private static InputStream readAhead(
            final @WillNotClose InputStream in,
            final byte[] buf)
    throws EOFException, IOException {
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
    public InputSocket<TarDriverEntry> input(final String name) {
        Objects.requireNonNull(name);

        final class Input extends AbstractInputSocket<TarDriverEntry> {
            @Override
            public TarDriverEntry target() throws IOException {
                final TarDriverEntry entry = entry(name);
                if (null == entry)
                    throw new NoSuchFileException(name, null, "Entry not found!");
                if (entry.isDirectory())
                    throw new NoSuchFileException(name, null, "Cannot read directory entries!");
                return entry;
            }

            @Override
            public InputStream stream(OutputSocket<? extends Entry> peer)
            throws IOException {
                return socket().stream(peer);
            }

            @Override
            public SeekableByteChannel channel(OutputSocket<? extends Entry> peer)
            throws IOException {
                return socket().channel(peer);
            }

            InputSocket<? extends IoBuffer> socket() throws IOException {
                return target().getBuffer().input();
            }
        } // Input

        return new Input();
    }

    @Override
    public void close() throws IOException {
        close0();
    }

    private void close0() throws IOException {
        final ExceptionBuilder<IOException, IOException>
                    builder = new SuppressedExceptionBuilder<>();
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
