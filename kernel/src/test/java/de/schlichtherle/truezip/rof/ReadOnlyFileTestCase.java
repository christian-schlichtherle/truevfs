/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.rof;

import de.schlichtherle.truezip.util.ArrayHelper;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Performs an integration test for an implementation of {@link ReadOnlyFile}.
 * Some tests use Las Vegas algorithms, so the run time may vary.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class ReadOnlyFileTestCase {

    private static final Logger logger = Logger.getLogger(
            ReadOnlyFileTestCase.class.getName());

    protected static final String TEMP_FILE_PREFIX = "tzp";

    private static final Random rnd = new Random();

    /** The data to get compressed. */
    private static final byte[] DATA = new byte[1024]; // enough to waste some heat on CPU cycles
    static {
        rnd.nextBytes(DATA);
    }

    private static String mb(long value) {
        return ((value - 1 + 1024 * 1024) / (1024 * 1024)) + " MB"; // round up
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
        data = DATA.clone();
        try {
            final OutputStream out = new FileOutputStream(temp);
            try {
                out.write(data);
            } finally {
                out.close();
            }
            assert data.length == temp.length();
            rrof = new SimpleReadOnlyFile(temp);
            trof = newReadOnlyFile(temp);
        } catch (IOException ex) {
            if (!temp.delete())
                logger.log(Level.WARNING, "{0} (File.delete() failed)", temp);
            throw ex;
        }
    }

    protected abstract @NonNull ReadOnlyFile newReadOnlyFile(@NonNull File file)
    throws IOException;

    @After
    public void tearDown() throws IOException {
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
                logger.log(Level.WARNING, "{0} (could not delete)", temp);
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
                fail("Expected IOException!");
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
        do {
            final byte[] buf = new byte[rnd.nextInt(length / 100)];
            read = rrof.read(buf);
            if (read < 0)
                break;
            if (buf.length > 0) {
                assertTrue(read > 0);
                assertTrue(ArrayHelper.equals(data, off, buf, 0, read));
                java.util.Arrays.fill(buf, (byte) 0);
                trof.readFully(buf, 0, read);
                assertTrue(ArrayHelper.equals(data, off, buf, 0, read));
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
                assertTrue(ArrayHelper.equals(data, off, buf, 0, read));
                java.util.Arrays.fill(buf, (byte) 0);
                trof.readFully(buf, 0, read);
                assertTrue(ArrayHelper.equals(data, off, buf, 0, read));
            } else {
                assertTrue(read == 0);
                assertEquals(0, trof.read(buf));
            }
        }
    }
}
