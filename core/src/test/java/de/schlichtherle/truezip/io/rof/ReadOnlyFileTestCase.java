/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.rof;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;

/**
 * Test implementations of {@link ReadOnlyFile}.
 * Subclasses must override {@link #setUp}.
 * Some tests use Las Vegas algorithms, so the run time may vary.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.5
 */
public abstract class ReadOnlyFileTestCase extends TestCase {

    private static final Logger logger = Logger.getLogger(
            BufferedReadOnlyFileTest.class.getName());

    private static final Random rnd = new SecureRandom();

    static {
        boolean ea = false;
        assert ea = true; // NOT ea == true !
        logger.log(Level.CONFIG, "Java assertions {0}", (ea ? "enabled." : "disabled!"));
        if (!ea)
            logger.warning("Please enable assertions for additional white box testing.");
    }

    /** The test data. */
    protected byte[] data;

    /** The temporary file with the test data. */
    protected File file;

    /** The {@code ReadOnlyFile} implementation to use as a reference. */
    private ReadOnlyFile rrof;

    /** The {@code ReadOnlyFile} implementation to test. */
    protected ReadOnlyFile trof;

    protected ReadOnlyFileTestCase(String testName) {
        super(testName);
    }

    /**
     * Subclasses must override this method and initialize {@link #trof}
     * after a call to the superclass implementation, which initializes
     * all other fields.
     */
    @Override
    protected void setUp()
    throws IOException {
        data = new byte[1024 * 1024];
        rnd.nextBytes(data);
        file = File.createTempFile("tmp", null);
        try {
            final OutputStream out = new FileOutputStream(file);
            try {
                out.write(data);
            } finally {
                out.close();
            }
            assert data.length == file.length();
            rrof = new SimpleReadOnlyFile(file);
        } catch (IOException ex) {
            if (!file.delete())
                file.deleteOnExit();
            throw ex;
        }
    }

    @Override
    protected void tearDown()
    throws IOException {
        try {
            try {
                try {
                    if (trof != null)
                        trof.close();
                } finally {
                    if (rrof != null)
                        rrof.close();
                }
            } finally {
                if (!file.delete() && file.exists()) {
                    file.deleteOnExit();
                    throw new IOException(file + ": could not delete");
                }
            }
        } finally {
            rrof = trof = null;
            file = null;
            data = null;
            System.gc();
            System.runFinalization();
        }
    }

    public void testClose()
    throws IOException {
        testClose(rrof);
        testClose(trof);
    }

    public static void testClose(final ReadOnlyFile rof)
    throws IOException {
        rof.close();

        try {
            rof.length();
            fail("Expected IOException!");
        } catch (IOException expected) {
        }

        try {
            rof.getFilePointer();
            fail("Expected IOException!");
        } catch (IOException expected) {
        }

        try {
            rof.seek(0);
            fail("Expected IOException!");
        } catch (IOException expected) {
        }

        try {
            rof.read();
            fail("Expected IOException!");
        } catch (IOException expected) {
        }

        try {
            assertEquals(0, rof.read(new byte[0]));
        } catch (IOException mayHappen) {
        }

        try {
            rof.read(new byte[1]);
            fail("Expected IOException!");
        } catch (IOException expected) {
        }

        try {
            assertEquals(0, rof.read(new byte[0], 0, 0));
        } catch (IOException mayHappen) {
        }

        try {
            rof.read(new byte[1], 0, 1);
            fail("Expected IOException!");
        } catch (IOException expected) {
        }

        try {
            rof.readFully(new byte[0]);
        } catch (IOException mayHappen) {
        }

        try {
            rof.readFully(new byte[1]);
            fail("Expected IOException!");
        } catch (IOException expected) {
        }

        try {
            rof.readFully(new byte[0], 0, 0);
        } catch (IOException mayHappen) {
        }

        try {
            rof.readFully(new byte[1], 0, 1);
            fail("Expected IOException!");
        } catch (IOException expected) {
        }

        rof.close();
    }

    public void testLength()
    throws Exception {
        assertEquals(data.length, rrof.length());
        assertEquals(data.length, trof.length());
    }

    public void testForwardReadBytes()
    throws IOException {
        testForwardReadBytes(rrof);
        testForwardReadBytes(trof);
    }

    public void testForwardReadBytes(final ReadOnlyFile rof)
    throws IOException {
        final long length = rof.length();
        for (int off = 0; off < length; off++)
            assertEquals(data[off] & 0xff, rof.read());
        assertEquals(-1, rof.read());
    }

    public void testRandomReadBytes()
    throws IOException {
        testRandomReadBytes(rrof);
        testRandomReadBytes(trof);
    }

    public void testRandomReadBytes(final ReadOnlyFile rof)
    throws IOException {
        assertEquals(0, rof.getFilePointer());

        testRandomReadByte(rof, 0);

        final int length = (int) rof.length();
        for (int i = length; --i >= 0; ) {
            final int tooSmall = rnd.nextInt() | Integer.MIN_VALUE;
            try {
                testRandomReadByte(rof, tooSmall);
                fail("Expected IOException!");
            } catch (IOException ex) {
            }

            // Seeking past the file length may or may not throw an
            // IOException, depending on the implementation.
            // In any case, we want to validate that it shows no after effects.
            final int tooLarge = Math.max(
                    length + 1, rnd.nextInt() & Integer.MAX_VALUE);
            try {
                testRandomReadByte(rof, tooLarge);
            } catch (IOException mayHappen) {
            }

            final int justRight = rnd.nextInt(length);
            testRandomReadByte(rof, justRight);
        }
    }

    private void testRandomReadByte(
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

    public void testBackwardReadBytes()
    throws IOException {
        testBackwardReadBytes(rrof);
        testBackwardReadBytes(trof);
    }

    public void testBackwardReadBytes(final ReadOnlyFile rof)
    throws IOException {
        final int length = (int) rof.length();
        for (int off = length; --off >= 0; )
            testRandomReadByte(rof, off);
    }

    /** Las Vegas algorithm. */
    public void testForwardReadChunks()
    throws IOException {
        final int length = (int) rrof.length();
        int off = 0;
        int read;
        do {
            final byte[] buf = new byte[rnd.nextInt(length / 100)];
            read = rrof.read(buf);
            if (read < 0)
                break;
            if (buf.length > 0) {
                assertTrue(read > 0);
                assertTrue(de.schlichtherle.truezip.util.Arrays.equals(data, off, buf, 0, read));
                java.util.Arrays.fill(buf, (byte) 0);
                trof.readFully(buf, 0, read);
                assertTrue(de.schlichtherle.truezip.util.Arrays.equals(data, off, buf, 0, read));
            } else {
                assertTrue(read == 0);
                assertEquals(0, trof.read(buf));
            }
            off += read;
        } while (true);

        assertEquals(off, length);
        assertEquals(-1, read);
        assertEquals(-1, trof.read(new byte[1]));
        assertEquals( 0, rrof.read(new byte[0]));
        assertEquals( 0, trof.read(new byte[0]));
    }

    public void testRandomReadChunks()
    throws IOException {
        final int length = (int) rrof.length();
        for (int i = 100; --i >= 0; ) {
            int off = rnd.nextInt(length);
            testRandomReadByte(rrof, off);
            testRandomReadByte(trof, off);
            off++;
            final byte[] buf = new byte[rnd.nextInt(length / 100)];
            int read = rrof.read(buf);
            if (read < 0)
                continue;
            if (buf.length > 0) {
                assertTrue(read > 0);
                assertTrue(de.schlichtherle.truezip.util.Arrays.equals(data, off, buf, 0, read));
                java.util.Arrays.fill(buf, (byte) 0);
                trof.readFully(buf, 0, read);
                assertTrue(de.schlichtherle.truezip.util.Arrays.equals(data, off, buf, 0, read));
            } else {
                assertTrue(read == 0);
                assertEquals(0, trof.read(buf));
            }
        }
    }
}
