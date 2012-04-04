/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.rof;

import de.truezip.kernel.util.ArrayUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
public abstract class ReadOnlyFileTestSuite {

    private static final Logger
            logger = Logger.getLogger(ReadOnlyFileTestSuite.class.getName());

    protected static final String TEMP_FILE_PREFIX = "tzp";

    private static final Random rnd = new Random();

    /** The data to get compressed. */
    protected static final byte[] DATA = new byte[1024]; // enough to waste some heat on CPU cycles
    static {
        rnd.nextBytes(DATA);
    }

    /** The temporary file with the test data. */
    private File temp;

    /** The {@code ReadOnlyFile} implementation to use as a reference. */
    private ReadOnlyFile rrof;

    /** The {@code ReadOnlyFile} implementation to test. */
    private ReadOnlyFile trof;

    /** The test data. */
    private byte[] data;

    @Before
    public void setUp() throws IOException {
        temp = File.createTempFile(TEMP_FILE_PREFIX, null);
        try {
            try (final OutputStream out = new FileOutputStream(temp)) {
                out.write(DATA);
            }
            assert DATA.length == temp.length();
            rrof = new DefaultReadOnlyFile(temp);
            trof = newReadOnlyFile(temp);
        } catch (final IOException ex) {
            if (!temp.delete())
                throw new IOException(temp + " (could not delete)", ex);
            throw ex;
        }
        data = DATA.clone();
    }

    protected abstract ReadOnlyFile newReadOnlyFile(File file)
    throws IOException;

    @After
    public void tearDown() {
        try {
            try {
                try {
                    final ReadOnlyFile trof = this.trof;
                    this.trof = null;
                    if (trof != null)
                        trof.close();
                } finally {
                    final ReadOnlyFile rrof = this.rrof;
                    this.rrof = null;
                    if (rrof != null)
                        rrof.close();
                }
            } finally {
                if (temp.exists() && !temp.delete())
                    throw new IOException(temp + " (could not delete)");
            }
        } catch (final IOException ex) {
            logger.log(Level.FINEST,
                    "Failed to clean up test file (this may be just an aftermath):",
                    ex);
        }
    }

    @Test
    public final void testClose() throws IOException {
        close(rrof);
        close(trof);
    }

    private static void close(final ReadOnlyFile rof) throws IOException {
        rof.close();

        try {
            rof.length();
            fail();
        } catch (IOException expected) {
        }

        try {
            rof.getFilePointer();
            fail();
        } catch (IOException expected) {
        }

        try {
            rof.seek(0);
            fail();
        } catch (IOException expected) {
        }

        try {
            rof.read();
            fail();
        } catch (IOException expected) {
        }

        try {
            assertEquals(0, rof.read(new byte[0]));
        } catch (IOException mayHappen) {
        }

        try {
            rof.read(new byte[1]);
            fail();
        } catch (IOException expected) {
        }

        try {
            assertEquals(0, rof.read(new byte[0], 0, 0));
        } catch (IOException mayHappen) {
        }

        try {
            rof.read(new byte[1], 0, 1);
            fail();
        } catch (IOException expected) {
        }

        try {
            rof.readFully(new byte[0]);
        } catch (IOException mayHappen) {
        }

        try {
            rof.readFully(new byte[1]);
            fail();
        } catch (IOException expected) {
        }

        try {
            rof.readFully(new byte[0], 0, 0);
        } catch (IOException mayHappen) {
        }

        try {
            rof.readFully(new byte[1], 0, 1);
            fail();
        } catch (IOException expected) {
        }

        rof.close();
    }

    @Test
    public final void testLength() throws Exception {
        assertEquals(data.length, rrof.length());
        assertEquals(data.length, trof.length());
    }

    @Test
    public final void testForwardReadBytes() throws IOException {
        assertForwardReadBytes(rrof);
        assertForwardReadBytes(trof);
    }

    private void assertForwardReadBytes(final ReadOnlyFile rof) throws IOException {
        final long length = rof.length();
        for (int off = 0; off < length; off++)
            assertEquals(data[off] & 0xff, rof.read());
        assertEquals(-1, rof.read());
    }

    @Test
    public final void testRandomReadBytes() throws IOException {
        assertRandomReadBytes(rrof);
        assertRandomReadBytes(trof);
    }

    private void assertRandomReadBytes(final ReadOnlyFile rof)
    throws IOException {
        assertEquals(0, rof.getFilePointer());

        assertRandomReadByte(rof, 0);

        final int length = (int) rof.length();
        for (int i = length; --i >= 0; ) {
            final int tooSmall = rnd.nextInt() | Integer.MIN_VALUE;
            try {
                assertRandomReadByte(rof, tooSmall);
                fail();
            } catch (IOException ex) {
            }

            // Seeking past the file length may or may not throw an
            // IOException, depending on the implementation.
            // In any case, we want to validate that it shows no after effects.
            final int tooLarge = Math.max(
                    length + 1, rnd.nextInt() & Integer.MAX_VALUE);
            try {
                assertRandomReadByte(rof, tooLarge);
            } catch (IOException mayHappen) {
            }

            final int justRight = rnd.nextInt(length);
            assertRandomReadByte(rof, justRight);
        }
    }

    private void assertRandomReadByte(
            final ReadOnlyFile rof,
            final int off)
    throws IOException {
        rof.seek(off);
        assertEquals(off, rof.getFilePointer());
        if (off < rof.length()) {
            assertEquals(data[off] & 0xff, rof.read());
            assertEquals(off + 1, rof.getFilePointer());
        } else {
            assertEquals(-1, rof.read());
            assertEquals(off, rof.getFilePointer());
        }
    }

    @Test
    public final void testBackwardReadBytes() throws IOException {
        assertBackwardReadBytes(rrof);
        assertBackwardReadBytes(trof);
    }

    private void assertBackwardReadBytes(final ReadOnlyFile rof)
    throws IOException {
        final int length = (int) rof.length();
        for (int off = length; --off >= 0; )
            assertRandomReadByte(rof, off);
    }

    /** Las Vegas algorithm. */
    @Test
    public final void testForwardReadChunks() throws IOException {
        final int length = (int) rrof.length();
        int off = 0;
        int read;
        while (true) {
            final byte[] buf = new byte[rnd.nextInt(length / 100)];
            read = rrof.read(buf);
            if (read < 0)
                break;
            if (buf.length > 0) {
                assertTrue(read > 0);
                assertTrue(ArrayUtils.equals(data, off, buf, 0, read));
                java.util.Arrays.fill(buf, (byte) 0);
                trof.readFully(buf, 0, read);
                assertTrue(ArrayUtils.equals(data, off, buf, 0, read));
            } else {
                assertTrue(read == 0);
                assertEquals(0, trof.read(buf));
            }
            off += read;
        }

        assertEquals(off, length);
        assertEquals(-1, read);
        assertEquals(-1, trof.read(new byte[1]));
        assertEquals( 0, rrof.read(new byte[0]));
        assertEquals( 0, trof.read(new byte[0]));
    }

    @Test
    public final void testRandomReadChunks() throws IOException {
        final int length = (int) rrof.length();
        for (int i = 100; --i >= 0; ) {
            int off = rnd.nextInt(length);
            assertRandomReadByte(rrof, off);
            assertRandomReadByte(trof, off);
            off++;
            final byte[] buf = new byte[rnd.nextInt(length / 100)];
            int read = rrof.read(buf);
            if (read < 0)
                continue;
            if (buf.length > 0) {
                assertTrue(read > 0);
                assertTrue(ArrayUtils.equals(data, off, buf, 0, read));
                java.util.Arrays.fill(buf, (byte) 0);
                trof.readFully(buf, 0, read);
                assertTrue(ArrayUtils.equals(data, off, buf, 0, read));
            } else {
                assertTrue(read == 0);
                assertEquals(0, trof.read(buf));
            }
        }
    }
}
