/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import de.truezip.kernel.rof.ReadOnlyFile;
import static de.truezip.kernel.io.SeekableByteChannels.readByte;
import static de.truezip.kernel.io.SeekableByteChannels.readFully;
import de.truezip.kernel.util.ArrayHelper;
import java.io.IOException;
import java.io.OutputStream;
import static java.lang.Math.max;
import java.nio.ByteBuffer;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import static java.nio.file.Files.*;
import java.nio.file.Path;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Performs an integration test for an implementation of {@link ReadOnlyFile}.
 * Some tests use Las Vegas algorithms, so the run time may vary.
 *
 * @author Christian Schlichtherle
 */
public abstract class SeekableByteChannelTestSuite {

    private static final Logger
            logger = Logger.getLogger(SeekableByteChannelTestSuite.class.getName());

    protected static final String TEMP_FILE_PREFIX = "tzp";

    private static final Random rnd = new Random();

    /** The data to get compressed. */
    protected static final byte[] DATA = new byte[1024]; // enough to waste some heat on CPU cycles
    static {
        rnd.nextBytes(DATA);
    }

    /** The temporary file with the test data. */
    private Path temp;

    /** The seekable byte channel to use as a reference. */
    private SeekableByteChannel rsbc;

    /** The seekable byte channel to test. */
    private SeekableByteChannel tsbc;

    /** The test data. */
    private byte[] data;

    @Before
    public void setUp() throws IOException {
        temp = createTempFile(TEMP_FILE_PREFIX, null);
        try {
            try (final OutputStream out = newOutputStream(temp)) {
                out.write(DATA);
            }
            assert DATA.length == size(temp);
            rsbc = newByteChannel(temp);
            tsbc = newChannel(temp);
        } catch (final Throwable ex) {
            try {
                delete(temp);
            } catch (final Throwable ex2) {
                ex.addSuppressed(ex2);
            }
            throw ex;
        }
        data = DATA.clone();
    }

    protected abstract SeekableByteChannel newChannel(Path path)
    throws IOException;

    @After
    public void tearDown() {
        try {
            try {
                try {
                    final SeekableByteChannel tsbc = this.tsbc;
                    this.tsbc = null;
                    if (tsbc != null)
                        tsbc.close();
                } finally {
                    final SeekableByteChannel rsbc = this.rsbc;
                    this.rsbc = null;
                    if (rsbc != null)
                        rsbc.close();
                }
            } finally {
                deleteIfExists(temp);
            }
        } catch (final IOException ex) {
            logger.log(Level.FINEST,
                    "Failed to clean up test file (this may be just an aftermath):",
                    ex);
        }
    }

    @Test
    public void testWrite() throws IOException {
        assertWrite(rsbc);
        assertWrite(tsbc);
    }

    private void assertWrite(final SeekableByteChannel sbc) throws IOException {
        try {
            sbc.write(ByteBuffer.allocate(1));
            fail();
        } catch (final NonWritableChannelException expected) {
        }
    }

    @Test
    public void testTruncate() throws IOException {
        assertTruncate(rsbc);
        assertTruncate(tsbc);
    }

    private void assertTruncate(final SeekableByteChannel sbc) throws IOException {
        try {
            sbc.truncate(0);
            fail();
        } catch (final NonWritableChannelException expected) {
        }
    }

    @Test
    public void testClose() throws IOException {
        close(rsbc);
        close(tsbc);
    }

    private static void close(final SeekableByteChannel sbc) throws IOException {
        sbc.close();

        try {
            sbc.size();
            fail();
        } catch (IOException expected) {
        }

        try {
            sbc.position();
            fail();
        } catch (IOException expected) {
        }

        try {
            sbc.position(0);
            fail();
        } catch (IOException expected) {
        }

        try {
            assertEquals(0, sbc.read(ByteBuffer.allocate(0)));
        } catch (IOException mayHappen) {
        }

        try {
            sbc.read(ByteBuffer.allocate(1));
            fail();
        } catch (IOException expected) {
        }

        sbc.close();
    }

    @Test
    public void testLength() throws Exception {
        assertEquals(data.length, rsbc.size());
        assertEquals(data.length, tsbc.size());
    }

    @Test
    public void testForwardReadBytes() throws IOException {
        assertForwardReadBytes(rsbc);
        assertForwardReadBytes(tsbc);
    }

    private void assertForwardReadBytes(final SeekableByteChannel sbc)
    throws IOException {
        final long size = sbc.size();
        for (int off = 0; off < size; off++)
            assertEquals(data[off] & 0xff, readByte(sbc));
        assertEquals(-1, readByte(sbc));
    }

    @Test
    public void testRandomReadBytes() throws IOException {
        assertRandomReadBytes(rsbc);
        assertRandomReadBytes(tsbc);
    }

    private void assertRandomReadBytes(final SeekableByteChannel sbc)
    throws IOException {
        assertEquals(0, sbc.position());

        assertRandomReadByte(sbc, 0);

        final int size = (int) sbc.size();
        for (int i = size; --i >= 0; ) {
            final int tooSmall = rnd.nextInt() | Integer.MIN_VALUE;
            try {
                assertRandomReadByte(sbc, tooSmall);
                fail();
            } catch (final IllegalArgumentException ex) {
            }

            // Seeking past the file length may or may not throw an
            // IOException, depending on the implementation.
            // In any case, we want to validate that it yields no side effects.
            final int tooLarge = max(size + 1, rnd.nextInt() & Integer.MAX_VALUE);
            try {
                assertRandomReadByte(sbc, tooLarge);
            } catch (final IOException mayHappen) {
            }

            final int justRight = rnd.nextInt(size);
            assertRandomReadByte(sbc, justRight);
        }
    }

    private void assertRandomReadByte(
            final SeekableByteChannel sbc,
            final int off)
    throws IOException {
        sbc.position(off);
        assertEquals(off, sbc.position());
        if (off < sbc.size()) {
            assertEquals(data[off] & 0xff, readByte(sbc));
            assertEquals(off + 1, sbc.position());
        } else {
            assertEquals(-1, readByte(sbc));
            assertEquals(off, sbc.position());
        }
    }
    
    @Test
    public void testBackwardReadBytes() throws IOException {
        assertBackwardReadBytes(rsbc);
        assertBackwardReadBytes(tsbc);
    }

    private void assertBackwardReadBytes(final SeekableByteChannel rof)
    throws IOException {
        final int size = (int) rof.size();
        for (int off = size; --off >= 0; )
            assertRandomReadByte(rof, off);
    }

    @Test
    public void testForwardReadChunks() throws IOException {
        // Las Vegas algorithm.
        final int size = (int) rsbc.size();
        int off = 0;
        int read;
        while (true) {
            final byte[] buf = new byte[rnd.nextInt(size / 100)];
            read = rsbc.read(ByteBuffer.wrap(buf));
            if (read < 0)
                break;
            if (buf.length > 0) {
                assertTrue(read > 0);
                assertTrue(ArrayHelper.equals(data, off, buf, 0, read));
                java.util.Arrays.fill(buf, (byte) 0);
                readFully(tsbc, ByteBuffer.wrap(buf, 0, read));
                assertTrue(ArrayHelper.equals(data, off, buf, 0, read));
            } else {
                assertEquals(0, read);
                assertEquals(0, tsbc.read(ByteBuffer.wrap(buf)));
            }
            off += read;
        }

        assertEquals(off, size);
        assertEquals(-1, read);
        assertEquals(-1, readByte(tsbc));
        assertEquals(0, rsbc.read(ByteBuffer.allocate(0)));
        assertEquals(0, tsbc.read(ByteBuffer.allocate(0)));
    }

    @Test
    public void testRandomReadChunks() throws IOException {
        final int size = (int) rsbc.size();
        for (int i = 100; --i >= 0; ) {
            int off = rnd.nextInt(size);
            assertRandomReadByte(rsbc, off);
            assertRandomReadByte(tsbc, off);
            off++;
            final byte[] buf = new byte[rnd.nextInt(size / 100)];
            int read = rsbc.read(ByteBuffer.wrap(buf));
            if (read < 0)
                continue;
            if (buf.length > 0) {
                assertTrue(read > 0);
                assertTrue(ArrayHelper.equals(data, off, buf, 0, read));
                java.util.Arrays.fill(buf, (byte) 0);
                readFully(tsbc, ByteBuffer.wrap(buf, 0, read));
                assertTrue(ArrayHelper.equals(data, off, buf, 0, read));
            } else {
                assertEquals(0, read);
                assertEquals(0, tsbc.read(ByteBuffer.wrap(buf)));
            }
        }
    }
}
