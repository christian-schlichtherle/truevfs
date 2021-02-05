/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.tardriver;

import global.namespace.truevfs.commons.cio.*;
import global.namespace.truevfs.commons.cio.Entry.Type;
import global.namespace.truevfs.commons.io.Source;
import global.namespace.truevfs.commons.io.Streams;
import global.namespace.truevfs.commons.shed.ExceptionBuilder;
import global.namespace.truevfs.commons.shed.SuppressedExceptionBuilder;
import global.namespace.truevfs.kernel.api.FsArchiveDriver;
import global.namespace.truevfs.kernel.api.FsModel;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarUtils;

import java.io.*;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.NoSuchFileException;
import java.util.*;

import static global.namespace.truevfs.commons.cio.Entry.Type.DIRECTORY;
import static global.namespace.truevfs.commons.cio.Entry.Type.FILE;
import static global.namespace.truevfs.commons.shed.HashMaps.OVERHEAD_SIZE;
import static global.namespace.truevfs.commons.shed.HashMaps.initialCapacity;
import static org.apache.commons.compress.archivers.tar.TarConstants.*;

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
 * @author Christian Schlichtherle
 * @see TarOutputContainer
 */
public final class TarInputContainer implements InputContainer<TarDriverEntry> {

    private static final byte[] NULL_RECORD = new byte[DEFAULT_RCDSIZE];

    private static final int CHECKSUM_OFFSET = NAMELEN + MODELEN + UIDLEN + GIDLEN + SIZELEN + MODTIMELEN;

    /**
     * Maps entry names to I/O pool entries.
     */
    private final Map<String, TarDriverEntry> entries = new LinkedHashMap<>(initialCapacity(OVERHEAD_SIZE));

    private final TarDriver driver;

    public TarInputContainer(
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

    private void unpack(final TarArchiveInputStream tain)
            throws IOException {
        final TarDriver driver = this.driver;
        final IoBufferPool pool = driver.getPool();
        for (TarArchiveEntry tinEntry;
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
                    try (OutputStream out = buffer.output().stream(Optional.empty())) {
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
     * @param in the stream to read from.
     * @return A stream which holds all the data {@code in} did.
     * @throws IOException on any I/O error.
     */
    private TarArchiveInputStream newValidatedTarArchiveInputStream(
            final InputStream in)
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
        return new TarArchiveInputStream(vin,
                DEFAULT_BLKSIZE,
                DEFAULT_RCDSIZE,
                driver.getEncoding());
    }

    /**
     * Fills {@code buf} with data from the given input stream and
     * returns an input stream from which you can still read all data,
     * including the data in buf.
     *
     * @param in  The stream to read from.
     * @param buf The buffer to fill entirely with data.
     * @return A stream which holds all the data {@code in} did.
     * @throws EOFException on unexpected end-of-file.
     * @throws IOException  on any I/O error.
     */
    private static InputStream readAhead(
            final InputStream in,
            final byte[] buf
    ) throws IOException {
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
    public Collection<TarDriverEntry> entries() {
        return new AbstractCollection<TarDriverEntry>() {

            @Override
            public Iterator<TarDriverEntry> iterator() {
                return Collections.unmodifiableCollection(entries.values()).iterator();
            }

            @Override
            public int size() {
                return entries.size();
            }
        };
    }

    @Override
    public Optional<TarDriverEntry> entry(String name) {
        return Optional.ofNullable(entries.get(name));
    }

    @Override
    public InputSocket<TarDriverEntry> input(final String name) {
        Objects.requireNonNull(name);

        final class Input implements InputSocket<TarDriverEntry> {

            @Override
            public TarDriverEntry getTarget() throws IOException {
                final Optional<TarDriverEntry> entry = entry(name);
                if (!entry.isPresent()) {
                    throw new NoSuchFileException(name, null, "Entry not found!");
                }
                if (entry.get().isDirectory()) {
                    throw new NoSuchFileException(name, null, "Cannot read directory entries!");
                }
                return entry.get();
            }

            @Override
            public InputStream stream(Optional<? extends OutputSocket<? extends Entry>> peer) throws IOException {
                return socket().stream(peer);
            }

            @Override
            public SeekableByteChannel channel(Optional<? extends OutputSocket<? extends Entry>> peer) throws IOException {
                return socket().channel(peer);
            }

            InputSocket<? extends IoBuffer> socket() throws IOException {
                return getTarget().getBuffer().input();
            }
        }

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
