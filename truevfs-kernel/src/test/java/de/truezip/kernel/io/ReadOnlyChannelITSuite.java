/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import java.io.IOException;
import static java.lang.Math.max;
import java.nio.ByteBuffer;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ReadableByteChannel;
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
 * Performs an integration test of a {@link SeekableByteChannel} with read-only
 * access.
 * Some tests use Las Vegas algorithms, so the run time may vary.
 *
 * @author Christian Schlichtherle
 */
public abstract class ReadOnlyChannelITSuite {

    private static final Logger
            logger = Logger.getLogger(ReadOnlyChannelITSuite.class.getName());

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
    private SeekableByteChannel rchannel;

    /** The seekable byte channel to test. */
    private SeekableByteChannel tchannel;

    /** The test data. */
    private byte[] data;

    @Before
    public void setUp() throws IOException {
        temp = createTempFile(TEMP_FILE_PREFIX, null);
        try {
            write(temp, DATA);
            assert DATA.length == size(temp);
            rchannel = newByteChannel(temp);
            tchannel = newChannel(temp);
        } catch (final Throwable ex) {
            try {
                delete(temp);
            } catch (final IOException ex2) {
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
                    final SeekableByteChannel tsbc = this.tchannel;
                    this.tchannel = null;
                    if (tsbc != null)
                        tsbc.close();
                } finally {
                    final SeekableByteChannel rsbc = this.rchannel;
                    this.rchannel = null;
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
        assertWrite(rchannel);
        assertWrite(tchannel);
    }

    private void assertWrite(final SeekableByteChannel channel) throws IOException {
        try {
            channel.write(ByteBuffer.allocate(1));
            fail();
        } catch (final NonWritableChannelException expected) {
        }
    }

    @Test
    public void testTruncate() throws IOException {
        assertTruncate(rchannel);
        assertTruncate(tchannel);
    }

    private void assertTruncate(final SeekableByteChannel channel) throws IOException {
        try {
            channel.truncate(0);
            fail();
        } catch (final NonWritableChannelException expected) {
        }
    }

    @Test
    public void testClose() throws IOException {
        close(rchannel);
        close(tchannel);
    }

    private static void close(final SeekableByteChannel channel) throws IOException {
        channel.close();

        try {
            channel.size();
            fail();
        } catch (IOException expected) {
        }

        try {
            channel.position();
            fail();
        } catch (IOException expected) {
        }

        try {
            channel.position(0);
            fail();
        } catch (IOException expected) {
        }

        try {
            assertEquals(0, channel.read(ByteBuffer.allocate(0)));
        } catch (IOException mayHappen) {
        }

        try {
            channel.read(ByteBuffer.allocate(1));
            fail();
        } catch (IOException expected) {
        }

        channel.close();
    }

    @Test
    public void testLength() throws Exception {
        assertEquals(data.length, rchannel.size());
        assertEquals(data.length, tchannel.size());
    }

    @Test
    public void testForwardReadBytes() throws IOException {
        assertForwardReadBytes(rchannel);
        assertForwardReadBytes(tchannel);
    }

    private void assertForwardReadBytes(final SeekableByteChannel channel)
    throws IOException {
        final long size = channel.size();
        for (int off = 0; off < size; off++)
            assertEquals(data[off] & 0xff, readByte(channel));
        assertEquals(-1, readByte(channel));
    }

    /**
     * Reads a single byte from the given seekable byte channel.
     * 
     * @param channel the readable byte channel.
     * @return the read byte or -1 on end-of-file.
     * @throws IOException on any I/O error.
     */
    private static int readByte(final ReadableByteChannel channel)
    throws IOException {
        final ByteBuffer buf = ByteBuffer.allocate(1);
        return 1 != channel.read(buf) ? -1 : buf.get(0) & 0xff;
    }

    @Test
    public void testRandomReadBytes() throws IOException {
        assertRandomReadBytes(rchannel);
        assertRandomReadBytes(tchannel);
    }

    private void assertRandomReadBytes(final SeekableByteChannel channel)
    throws IOException {
        assertEquals(0, channel.position());

        assertReadAtPosition(channel, 0);

        final int size = (int) channel.size();
        for (int i = size; --i >= 0; ) {
            final int tooSmall = rnd.nextInt() | Integer.MIN_VALUE;
            try {
                assertReadAtPosition(channel, tooSmall);
                fail();
            } catch (final IllegalArgumentException ex) {
            }

            // Seeking past the file length may or may not throw an
            // IOException, depending on the implementation.
            // In any case, we want to validate that it yields no side effects.
            final int tooLarge = max(size, rnd.nextInt() & Integer.MAX_VALUE);
            assertReadAtPosition(channel, tooLarge);

            final int justRight = rnd.nextInt(size);
            assertReadAtPosition(channel, justRight);
        }
    }

    private void assertReadAtPosition(
            final SeekableByteChannel channel,
            final int pos)
    throws IOException {
        channel.position(pos);
        assertEquals(pos, channel.position());
        if (pos < channel.size()) {
            assertEquals(data[pos] & 0xff, readByte(channel));
            assertEquals(pos + 1, channel.position());
        } else {
            assertEquals(-1, readByte(channel));
            assertEquals(pos, channel.position());
        }
    }
    
    @Test
    public void testBackwardReadBytes() throws IOException {
        assertBackwardReadBytes(rchannel);
        assertBackwardReadBytes(tchannel);
    }

    private void assertBackwardReadBytes(final SeekableByteChannel channel)
    throws IOException {
        final int size = (int) channel.size();
        for (int pos = size; --pos >= 0; )
            assertReadAtPosition(channel, pos);
    }

    @Test
    public void testForwardReadChunks() throws IOException {
        // Las Vegas algorithm.
        final int size = (int) rchannel.size();
        int pos = 0;
        int read;
        while (true) {
            final byte[] buf = new byte[rnd.nextInt(size / 100)];
            read = rchannel.read(ByteBuffer.wrap(buf));
            if (0 > read)
                break;
            if (0 < buf.length) {
                assertTrue(0 < read);
                assertEquals(   ByteBuffer.wrap(data, pos, read),
                                ByteBuffer.wrap(buf, 0, read));
                java.util.Arrays.fill(buf, (byte) 0);
                PowerBuffer.wrap(buf, 0, read).load(tchannel);
                assertEquals(   ByteBuffer.wrap(data, pos, read),
                                ByteBuffer.wrap(buf, 0, read));
            } else {
                assertEquals(0, read);
                assertEquals(0, tchannel.read(ByteBuffer.wrap(buf)));
            }
            pos += read;
        }

        assertEquals(pos, size);
        assertEquals(-1, read);
        assertEquals(-1, readByte(tchannel));
        assertEquals(0, rchannel.read(ByteBuffer.allocate(0)));
        assertEquals(0, tchannel.read(ByteBuffer.allocate(0)));
    }

    @Test
    public void testRandomReadChunks() throws IOException {
        final int size = (int) rchannel.size();
        for (int i = 100; --i >= 0; ) {
            int pos = rnd.nextInt(size);
            assertReadAtPosition(rchannel, pos);
            assertReadAtPosition(tchannel, pos);
            pos++;
            final byte[] buf = new byte[rnd.nextInt(size / 100)];
            int read = rchannel.read(ByteBuffer.wrap(buf));
            if (read < 0)
                continue;
            if (buf.length > 0) {
                assertTrue(read > 0);
                assertEquals(   ByteBuffer.wrap(data, pos, read),
                                ByteBuffer.wrap(buf, 0, read));
                java.util.Arrays.fill(buf, (byte) 0);
                PowerBuffer.wrap(buf, 0, read).load(tchannel);
                assertEquals(   ByteBuffer.wrap(data, pos, read),
                                ByteBuffer.wrap(buf, 0, read));
            } else {
                assertEquals(0, read);
                assertEquals(0, tchannel.read(ByteBuffer.wrap(buf)));
            }
        }
    }
}
