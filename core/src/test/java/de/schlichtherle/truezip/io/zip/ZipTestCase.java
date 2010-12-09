/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.zip;

import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.rof.SimpleReadOnlyFile;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.UnsupportedCharsetException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipException;
import junit.framework.TestCase;

import static de.schlichtherle.truezip.io.zip.ZipConstants.*;

/**
 * Tests compression of data.
 * Subclasses must override {@link #setUp}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class ZipTestCase extends TestCase {

    private static final Logger logger
            = Logger.getLogger(ZipTestCase.class.getName());

    /** May be used by some tests or sub classes. */
    protected static final Random rnd = new SecureRandom();

    /** The data to get compressed. */
    protected byte[] data;

    /** The temporary file to use as a ZIP file. */
    protected File zip;
    
    public ZipTestCase(String testName) {
        super(testName);
    }

    /**
     * A subclass must override this method to create the {@link #data}
     * to be zipped.
     * It must also finally call this superclass implementation to create
     * the temporary file to be used as a ZIP file.
     */
    @Override
    protected void setUp() throws Exception {
        if (data == null)
            throw new IllegalStateException("'data' hasn't been initialized!");

        zip = File.createTempFile("tmp", ".zip", null);
        assertTrue(zip.delete());
    }

    @Override
    protected void tearDown() throws Exception {
        final boolean deleted = zip.delete();
        if (!deleted && zip.exists())
            logger.log(Level.WARNING, "{0} (could not delete)", zip);
        zip = null;

        data = null;
    }

    protected ZipOutputStream newZipOutputStream(OutputStream out)
    throws IOException {
        return new ZipOutputStream(out);
    }

    protected ZipOutputStream newZipOutputStream(
            OutputStream out, String charset)
    throws IOException {
        return new ZipOutputStream(out, charset);
    }

    protected ZipOutputStream newZipOutputStream(
            OutputStream out,
            ZipFile appendee)
    throws ZipException {
        return new ZipOutputStream(out, appendee);
    }

    protected ZipFile newZipFile(String name)
    throws IOException {
        return new ZipFile(name);
    }

    protected ZipFile newZipFile(
            String name, String charset)
    throws IOException {
        return new ZipFile(name, charset);
    }

    protected ZipFile newZipFile(File file)
    throws IOException {
        return new ZipFile(file);
    }

    protected ZipFile newZipFile(
            File file, String charset)
    throws IOException {
        return new ZipFile(file, charset);
    }

    protected ZipFile newZipFile(ReadOnlyFile file)
    throws IOException {
        return new ZipFile(file);
    }

    protected ZipFile newZipFile(
            ReadOnlyFile file, String charset)
    throws IOException {
        return new ZipFile(file, charset);
    }

    public void testConstructors() throws Exception {
        {
            final OutputStream os = new FileOutputStream(zip);
            os.write(data);
            os.close();
        }

        final ReadOnlyFile rof = new SimpleReadOnlyFile(zip);

        try {
            newZipOutputStream(null, (String) null);
            fail("Use of null argument must throw a NullPointerException!");
        } catch (NullPointerException npe) {
        }

        try {
            newZipOutputStream(null, (ZipFile) null);
            fail("Use of null arguments should throw a NullPointerException!");
        } catch (NullPointerException npe) {
        }

        try {
            newZipOutputStream(new ByteArrayOutputStream(), (String) null);
            fail("Use of null argument must throw a NullPointerException!");
        } catch (NullPointerException npe) {
        }

        try {
            newZipOutputStream(new ByteArrayOutputStream(), (ZipFile) null);
            fail("Use of null argument must throw a NullPointerException!");
        } catch (NullPointerException npe) {
        }

        try {
            newZipOutputStream(null, "UTF-8");
            fail("Use of null argument must throw a NullPointerException!");
        } catch (NullPointerException npe) {
        }

        try {
            newZipOutputStream(new ByteArrayOutputStream(), "unknown");
            fail("Use of unknown encoding should throw an UnsupportedEncodingException!");
        } catch (UnsupportedCharsetException uce) {
        }

        try {
            newZipFile((String) null);
            fail("Use of null argument must throw a NullPointerException!");
        } catch (NullPointerException npe) {
        }

        try {
            newZipFile((String) null, null);
            fail("Use of null argument must throw a NullPointerException!");
        } catch (NullPointerException npe) {
        }

        try {
            newZipFile((String) null, "UTF-8");
            fail("Use of null argument must throw a NullPointerException!");
        } catch (NullPointerException npe) {
        }

        try {
            newZipFile(zip.getPath(), null);
            fail("Use of null argument must throw a NullPointerException!");
        } catch (NullPointerException npe) {
        }

        try {
            newZipFile((File) null);
            fail("Use of null argument must throw a NullPointerException!");
        } catch (NullPointerException npe) {
        }

        try {
            newZipFile((File) null, null);
            fail("Use of null argument must throw a NullPointerException!");
        } catch (NullPointerException npe) {
        }

        try {
            newZipFile((File) null, "UTF-8");
            fail("Use of null argument must throw a NullPointerException!");
        } catch (NullPointerException npe) {
        }

        try {
            newZipFile(zip, null);
            fail("Use of null argument must throw a NullPointerException!");
        } catch (NullPointerException npe) {
        }

        try {
            newZipFile((ReadOnlyFile) null);
            fail("Use of null argument must throw a NullPointerException!");
        } catch (NullPointerException npe) {
        }

        try {
            newZipFile((ReadOnlyFile) null, null);
            fail("Use of null argument must throw a NullPointerException!");
        } catch (NullPointerException npe) {
        }

        try {
            newZipFile((ReadOnlyFile) null, "UTF-8");
            fail("Use of null argument must throw a NullPointerException!");
        } catch (NullPointerException npe) {
        }

        try {
            newZipFile(rof, null);
            fail("Use of null argument must throw a NullPointerException!");
        } catch (NullPointerException npe) {
        }

        /*try {
            newZipFile(zip, "unknown");
            fail("Use of unknown encoding should throw an UnsupportedEncodingException!");
        } catch (UnsupportedCharsetException uce) {
        }*/

        try {
            newZipFile(zip.getPath());
            fail("This is not a valid ZIP file!");
        } catch (IOException ioe) {
        }

        try {
            newZipFile(zip);
            fail("This is not a valid ZIP file!");
        } catch (IOException ioe) {
        }

        try {
            newZipFile(rof);
            fail("This is not a valid ZIP file!");
        } catch (IOException ioe) {
        }

        try {
            newZipFile(zip, "UTF-8");
            fail("This is not a valid ZIP file!");
        } catch (IOException ioe) {
        }

        try {
            newZipFile(rof, "UTF-8");
            fail("This is not a valid ZIP file!");
        } catch (IOException ioe) {
        }

        rof.close();
        assertTrue(zip.delete());
    }

    public void testPreambleOfEmptyZipFile() throws IOException {
        // Create empty ZIP file.
        newZipOutputStream(new FileOutputStream(zip)).close();

        // Assert that the empty ZIP file has no preamble.
        final ZipFile zipIn = newZipFile(zip);
        try {
            assertEquals(0, zipIn.getPreambleLength());
            final InputStream in = zipIn.getPreambleInputStream();
            try {
                assertEquals(-1, in.read());
            } finally {
                in.close();
            }
        } finally {
            zipIn.close();
        }
    }

    public void testGetInputStream() throws IOException {
        final ZipOutputStream zipOut
                = newZipOutputStream(new FileOutputStream(zip));
        try {
            zipOut.putNextEntry(new ZipEntry("foo"));
        } finally {
            zipOut.close();
        }

        final ZipFile zipIn = newZipFile(zip);
        try {
            zipIn.getInputStream("foo").close();
            assertNull(zipIn.getInputStream("bar"));
        } finally {
            zipIn.close();
        }
    }

    public void testWriteAndReadSingleBytes() throws IOException {
        final ZipOutputStream zipOut
                = newZipOutputStream(new FileOutputStream(zip));
        zipOut.putNextEntry(new ZipEntry("file"));
        for (int i = 0; i < data.length; i++)
            zipOut.write(data[i]);
        zipOut.close();

        final ZipFile zipIn = newZipFile(zip);
        InputStream in = zipIn.getInputStream("file");
        for (int i = 0, c; (c = in.read()) != -1; i++) {
            assertEquals(data[i] & 0xFF, c);
        }
        in.close();
        zipIn.close();
    }

    public void testMultithreading()
    throws Exception {
        testMultithreading(20, 40);
    }

    /**
     * Creates a test ZIP file with the given number of entries and then
     * creates the given number of threads where each of them read all these
     * entries.
     *
     * @param nEntries The number of ZIP file entries to be created.
     * @param nThreads The number of threads to be created.
     */
    private void testMultithreading(final int nEntries, final int nThreads)
    throws Exception {
        createTestZipFile(nEntries);

        final ZipFile zipIn = newZipFile(zip);

        // Define thread class to check all entries.
        class CheckAllEntriesThread extends Thread {
            Throwable failure;

            CheckAllEntriesThread() {
                setDaemon(true);
            }

            @Override
            public void run() {
                try {
                    // Retrieve list of entries and randomize their order.
                    @SuppressWarnings("unchecked")
                    final List<ZipEntry> entries = Collections.list((Enumeration<ZipEntry>) zipIn.entries());
                    assert entries.size() == nEntries; // this would be a programming error in the test - not the testlet!
                    for (int i = 0; i < nEntries; i++) {
                        final int j = rnd.nextInt(nEntries);
                        final ZipEntry temp = entries.get(i);
                        entries.set(i, entries.get(j));
                        entries.set(j, temp);
                    }

                    // Now read in the entries in the randomized order.
                    final byte[] buf = new byte[4096];
                    for (final ZipEntry entry : entries) {
                        // Read full entry and check the contents.
                        final InputStream in = zipIn.getInputStream(entry.getName());
                        try {
                            int off = 0;
                            int read;
                            do {
                                read = in.read(buf);
                                if (read < 0)
                                    break;
                                assertTrue(read > 0);
                                assertTrue(de.schlichtherle.truezip.util.Arrays.equals(
                                        data, off, buf, 0, read));
                                off += read;
                            } while (true);
                            assertEquals(-1, read);
                            assertEquals(off, data.length);
                            assertEquals(0, in.read(new byte[0]));
                        } finally {
                            in.close();
                        }
                    }
                } catch (Throwable t) {
                    failure = t;
                }
            }
        }

        try {
            // Create and start all threads.
            final CheckAllEntriesThread[] threads = new CheckAllEntriesThread[nThreads];
            for (int i = 0; i < nThreads; i++) {
                final CheckAllEntriesThread thread = new CheckAllEntriesThread();
                thread.start();
                threads[i] = thread;
            }

            // Wait for all threads until done.
            for (int i = 0; i < nThreads; ) {
                final CheckAllEntriesThread thread = threads[i];
                try {
                    thread.join();
                } catch (InterruptedException ignored) {
                    continue;
                }
                if (thread.failure != null)
                    throw new Exception(thread.failure);
                i++;
            }
        } finally {
            zipIn.close();
        }
    }

    /**
     * Creates test ZIP file with {@code nEntries} and returns the
     * entry names in a set.
     * The field {@code zip} is used to determine the ZIP file.
     */
    private void createTestZipFile(final int nEntries) throws IOException {
        final HashSet<String> set = new HashSet<String>();

        ZipOutputStream zipOut
                = newZipOutputStream(new FileOutputStream(zip));
        try {
            for (int i = 0; i < nEntries; i++) {
                String name = i + ".txt";
                zipOut.putNextEntry(new ZipEntry(name));
                zipOut.write(data);
                assertTrue(set.add(name));
            }
        } finally {
            zipOut.close();
        }
        zipOut = null; // allow GC

        ZipFile zipIn = newZipFile(zip);
        try {
            // Check that zipIn correctly enumerates all entries.
            for (final Enumeration<? extends ZipEntry> e = zipIn.entries(); e.hasMoreElements(); ) {
                final ZipEntry entry = e.nextElement();
                assertEquals(data.length, entry.getSize());
                assertTrue(set.remove(entry.getName()));
            }
            assertTrue(set.isEmpty());
        } finally {
            zipIn.close();
        }
    }

    public void testGoodGetCheckedInputStream() throws IOException {
        // Create test ZIP file.
        final String name = "entry";
        final ZipOutputStream zipOut
                = newZipOutputStream(new FileOutputStream(zip));
        zipOut.putNextEntry(new ZipEntry(name));
        zipOut.write(data);
        zipOut.close();

        final ZipFile zipIn = newZipFile(zip);

        // Open checked input stream and close immediately.
        InputStream in = zipIn.getCheckedInputStream(name);
        in.close();

        // Open checked input stream and read fully, using multiple methods.
        in = zipIn.getCheckedInputStream(name);
        final int n = data.length / 4;
        in.skip(n);
        in.read(new byte[n]);
        in.read(new byte[n], 0, n);
        while (in.read() != -1)
            ; // read until EOF
        in.close();

        zipIn.close();
    }

    public void testBadGetCheckedInputStream() throws IOException {
        if (ZIP64_EXT)
            fail("TODO: Adapt this test so that it works when ZIP64 extensions have been forced to use!");

        for (int i = 0; i < 4; i++) {
            // Create test ZIP file.
            final String name = "entry";
            final ZipOutputStream zipOut
                    = new ZipOutputStream(new FileOutputStream(zip)); // NOT newZipOutputStream(...) !
            zipOut.putNextEntry(new ZipEntry(name));
            zipOut.write(data);
            zipOut.close();

            final boolean tweakDD = (i & 1) != 0;
            final boolean tweakCFH = (i & 2) != 0;

            // Modify ZIP file to contain an incorrect CRC32 value in the
            // Central File Header.
            // TODO: This is a hack which works with a plain ZIP file only
            // (not an RAES encrypted ZIP file) and may easily break if the
            // ZipOutputStream class changes its implementation!
            final byte[] crc = new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
            final RandomAccessFile raf = new RandomAccessFile(zip, "rw");
            if (tweakDD) {
                raf.seek(raf.length() - 57 - 28); // CRC-32 position in Data Descriptor
                raf.write(crc);
            }
            if (tweakCFH) {
                raf.seek(raf.length() - 57); // CRC-32 position in Central File Header
                raf.write(crc);
            }
            raf.close();

            final ZipFile zipIn = new ZipFile(zip); // NOT newZipFile(...) !

            try {
                InputStream in = zipIn.getCheckedInputStream(name);
                if (tweakDD ^ tweakCFH)
                    fail("Expected CRC32Exception!");

                // Open checked input stream and close immediately.
                in.close();

                if (tweakDD & tweakCFH)
                    fail("Expected CRC32Exception!");
            } catch (CRC32Exception ex) {
                assertTrue(tweakDD | tweakCFH);
            }

            try {
                InputStream in = zipIn.getCheckedInputStream(name);
                if (tweakDD ^ tweakCFH)
                    fail("Expected CRC32Exception!");

                // Open checked input stream and read fully, using multiple methods.
                final int n = data.length / 4;
                in.skip(n);
                in.read(new byte[n]);
                in.read(new byte[n], 0, n);
                while (in.read() != -1)
                    ; // read until EOF
                in.close();

                if (tweakDD & tweakCFH)
                    fail("Expected CRC32Exception!");
            } catch (CRC32Exception ex) {
                assertTrue(tweakDD | tweakCFH);
            }

            zipIn.close();
        }
    }
}
