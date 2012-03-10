/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

import de.schlichtherle.truezip.rof.DefaultReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.util.ArrayHelper;
import static de.schlichtherle.truezip.util.ConcurrencyUtils.NUM_IO_THREADS;
import de.schlichtherle.truezip.util.ConcurrencyUtils.TaskFactory;
import static de.schlichtherle.truezip.util.ConcurrencyUtils.runConcurrent;
import de.schlichtherle.truezip.util.Maps;
import static de.schlichtherle.truezip.zip.Constants.FORCE_ZIP64_EXT;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipException;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Performs an integration test for reading and writing ZIP files.
 *
 * @author Christian Schlichtherle
 */
public abstract class ZipTestSuite implements ZipEntryFactory<ZipEntry> {

    private static final Logger
            logger = Logger.getLogger(ZipTestSuite.class.getName());

    protected static final String TEMP_FILE_PREFIX = "tzp";

    private static final Random rnd = new Random();

    /** The data to get compressed. */
    private static final byte[] DATA;
    static {
        final String text = "This is a truly compressible text!\n";
        final int count = 1024 / text.length(); // round down
        final StringBuilder buf = new StringBuilder(count * text.length());
        for (int i = 0; i < count; i++)
            buf.append(text);
        DATA = buf.toString().getBytes();
    }

    /** The temporary file to use as a ZIP file. */
    private File file;
    private byte[] data;

    /**
     * A subclass must override this method to create the {@link #data}
     * to be zipped.
     * It must also finally call this superclass implementation to create
     * the temporary file to be used as a ZIP file.
     * 
     * @throws IOException On any I/O failure.
     */
    @Before
    public void setUp() throws IOException {
        file = File.createTempFile(TEMP_FILE_PREFIX, null);
        assertTrue(file.delete());
        data = DATA.clone();
    }

    protected final File getZip() {
        return file;
    }

    protected final byte[] getData() {
        return data.clone();
    }

    @After
    public void tearDown() {
        if (file.exists() && !file.delete())
            logger.log(Level.WARNING, "{0} (could not delete)", file);
    }

    @Override
    public ZipEntry newEntry(String name) {
        return new ZipEntry(name);
    }

    protected ZipOutputStream newZipOutputStream(OutputStream out)
    throws IOException {
        return new ZipOutputStream(out);
    }

    protected ZipOutputStream newZipOutputStream(
            OutputStream out, Charset charset)
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
            String name, Charset charset)
    throws IOException {
        return new ZipFile(name, charset);
    }

    protected ZipFile newZipFile(File file)
    throws IOException {
        return new ZipFile(file);
    }

    protected ZipFile newZipFile(
            File file, Charset charset)
    throws IOException {
        return new ZipFile(file, charset);
    }

    protected ZipFile newZipFile(ReadOnlyFile file)
    throws IOException {
        return new ZipFile(file);
    }

    protected ZipFile newZipFile(
            ReadOnlyFile file, Charset charset)
    throws IOException {
        return new ZipFile(file, charset);
    }

    @Test
    public final void testConstructors() throws Exception {
        {
            OutputStream os = new FileOutputStream(file);
            try {
                os.write(data);
            } finally {
                os.close();
            }
        }

        final ReadOnlyFile rof = new DefaultReadOnlyFile(file);

        try {
            newZipOutputStream(null, (Charset) null);
            fail();
        } catch (NullPointerException ex) {
        }

        try {
            newZipOutputStream(null, (ZipFile) null);
            fail();
        } catch (NullPointerException ex) {
        }

        try {
            newZipOutputStream(new ByteArrayOutputStream(), (Charset) null);
            fail();
        } catch (NullPointerException ex) {
        }

        try {
            newZipOutputStream(new ByteArrayOutputStream(), (ZipFile) null);
            fail();
        } catch (NullPointerException ex) {
        }

        try {
            newZipOutputStream(null, Charset.forName("UTF-8"));
            fail();
        } catch (NullPointerException ex) {
        }

        try {
            newZipFile((String) null);
            fail();
        } catch (NullPointerException ex) {
        }

        try {
            newZipFile((String) null, null);
            fail();
        } catch (NullPointerException ex) {
        }

        try {
            newZipFile((String) null, Charset.forName("UTF-8"));
            fail();
        } catch (NullPointerException ex) {
        }

        try {
            newZipFile(file.getPath(), null);
            fail();
        } catch (NullPointerException ex) {
        }

        try {
            newZipFile((File) null);
            fail();
        } catch (NullPointerException ex) {
        }

        try {
            newZipFile((File) null, null);
            fail();
        } catch (NullPointerException ex) {
        }

        try {
            newZipFile((File) null, Charset.forName("UTF-8"));
            fail();
        } catch (NullPointerException ex) {
        }

        try {
            newZipFile(file, null);
            fail();
        } catch (NullPointerException ex) {
        }

        try {
            newZipFile((ReadOnlyFile) null);
            fail();
        } catch (NullPointerException ex) {
        }

        try {
            newZipFile((ReadOnlyFile) null, null);
            fail();
        } catch (NullPointerException ex) {
        }

        try {
            newZipFile((ReadOnlyFile) null, Charset.forName("UTF-8"));
            fail();
        } catch (NullPointerException ex) {
        }

        try {
            newZipFile(rof, null);
            fail();
        } catch (NullPointerException ex) {
        }

        try {
            newZipFile(file.getPath());
            fail();
        } catch (IOException ex) {
        }

        try {
            newZipFile(file);
            fail();
        } catch (IOException ex) {
        }

        try {
            newZipFile(rof);
            fail();
        } catch (IOException ex) {
        }

        try {
            newZipFile(file, Charset.forName("UTF-8"));
            fail();
        } catch (IOException ex) {
        }

        try {
            newZipFile(rof, Charset.forName("UTF-8"));
            fail();
        } catch (IOException ex) {
        }

        rof.close();
        assertTrue(file.delete());
    }

    @Test
    public final void testPreambleOfEmptyZipFile() throws IOException {
        // Create empty ZIP file.
        newZipOutputStream(new FileOutputStream(file)).close();

        // Assert that the empty ZIP file has no preamble.
        final ZipFile zipIn = newZipFile(file);
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

    @Test
    public final void testGetInputStream() throws IOException {
        final ZipOutputStream zipOut
                = newZipOutputStream(new FileOutputStream(file));
        try {
            zipOut.putNextEntry(newEntry("foo"));
        } finally {
            zipOut.close();
        }

        final ZipFile zipIn = newZipFile(file);
        try {
            zipIn.getInputStream("foo").close();
            assertNull(zipIn.getInputStream("bar"));
        } finally {
            zipIn.close();
        }
    }

    @Test
    public final void testWriteAndReadSingleBytes() throws IOException {
        final ZipOutputStream zipOut
                = newZipOutputStream(new FileOutputStream(file));
        zipOut.putNextEntry(newEntry("file"));
        for (int i = 0; i < data.length; i++)
            zipOut.write(data[i]);
        zipOut.close();

        final ZipFile zipIn = newZipFile(file);
        InputStream in = zipIn.getInputStream("file");
        for (int i = 0, c; (c = in.read()) != -1; i++)
            assertEquals(data[i] & 0xFF, c);
        in.close();
        zipIn.close();
    }

    @Test
    public final void testMultithreading()
    throws Exception {
        multithreading(NUM_IO_THREADS, NUM_IO_THREADS);
    }

    /**
     * Creates a test ZIP file with the given number of entries and then
     * creates the given number of threads where each of these threads reads
     * all of those entries.
     *
     * @param nEntries The number of ZIP file entries to be created.
     * @param nThreads The number of threads to be created.
     */
    private void multithreading(final int nEntries, final int nThreads)
    throws Exception {
        createTestZipFile(nEntries);

        final ZipFile zin = newZipFile(file);

        class CheckAllEntriesFactory implements TaskFactory {
            @Override
            public Callable<?> newTask(int threadNum) {
                return new CheckAllEntries();
            }

            class CheckAllEntries implements Callable<Void> {
                @Override
                public Void call() throws IOException {
                    // Retrieve list of entries and shuffle their order.
                    @SuppressWarnings("unchecked")
                    final List<ZipEntry> entries = Collections.list((Enumeration<ZipEntry>) zin.entries());
                    assert entries.size() == nEntries; // this would be a programming error in the test class itself - not the class under test!
                    Collections.shuffle(entries, rnd);

                    // Now read in the entries in the shuffled order.
                    final byte[] buf = new byte[data.length];
                    for (final ZipEntry entry : entries) {
                        // Read full entry and check the contents.
                        final InputStream in = zin.getInputStream(entry.getName());
                        try {
                            int off = 0;
                            int read;
                            while (true) {
                                read = in.read(buf);
                                if (read < 0)
                                    break;
                                assertTrue(read > 0);
                                assertTrue(ArrayHelper.equals(data, off, buf, 0, read));
                                off += read;
                            }
                            assertEquals(-1, read);
                            assertEquals(off, data.length);
                            assertTrue(0 >= in.read(new byte[0]));
                        } finally {
                            in.close();
                        }
                    }
                    return null;
                }
            } // CheckAllEntries
        } // CheckAllEntriesFactory

        try {
            runConcurrent(nThreads, new CheckAllEntriesFactory()).join();
        } finally {
            zin.close();
        }
    }

    /**
     * Creates test ZIP file with {@code nEntries}.
     * The field {@code file} is used to determine the ZIP file.
     */
    private void createTestZipFile(final int nEntries) throws IOException {
        final HashSet<String>
                set = new HashSet<String>(Maps.initialCapacity(nEntries));

        {
            ZipOutputStream zout
                    = newZipOutputStream(new FileOutputStream(file));
            try {
                for (int i = 0; i < nEntries; i++) {
                    String name = i + ".txt";
                    zout.putNextEntry(newEntry(name));
                    zout.write(data);
                    assertTrue(set.add(name));
                }
            } finally {
                zout.close();
            }
        }

        ZipFile zin = newZipFile(file);
        try {
            // Check that zipIn correctly enumerates all entries.
            for (ZipEntry entry : zin) {
                assertEquals(data.length, entry.getSize());
                assertTrue(set.remove(entry.getName()));
            }
            assertTrue(set.isEmpty());
        } finally {
            zin.close();
        }
    }

    @Test
    public final void testGoodGetCheckedInputStream() throws IOException {
        // Create test ZIP file.
        final String name = "entry";
        final ZipOutputStream zipOut
                = newZipOutputStream(new FileOutputStream(file));
        zipOut.putNextEntry(newEntry(name));
        zipOut.write(data);
        zipOut.close();

        final ZipFile zipIn = newZipFile(file);

        // Open checked input stream and join immediately.
        InputStream in = zipIn.getCheckedInputStream(name);
        in.close();

        // Open checked input stream and read fully, using multiple methods.
        in = zipIn.getCheckedInputStream(name);
        final int n = data.length / 4;
        in.skip(n);
        in.read(new byte[n]);
        in.read(new byte[n], 0, n);
        while (in.read() != -1) { // read until EOF
        }
        in.close();

        zipIn.close();
    }

    /**
     * This test modifies ZIP files to contain an incorrect CRC32 value in the
     * Central File Header.
     * <p>
     * Note that this is a hack which works with plain ZIP files only (e.g. not
     * with RAES encrypted ZIP files) and may easily break if the
     * ZipOutputStream class changes its implementation!
     * 
     * @throws IOException On any I/O failure.
     */
    @Test
    public void testBadGetCheckedInputStream() throws IOException {
        if (FORCE_ZIP64_EXT)
            fail("TODO: Adapt this test so that it works when ZIP64 extensions have been forced to use!");

        for (int i = 0; i < 4; i++) {
            // Create test ZIP file.
            final String name = "entry";
            final ZipOutputStream zipOut = newZipOutputStream(
                    new FileOutputStream(file));
            try {
                zipOut.putNextEntry(newEntry(name));
                zipOut.write(data);
            } finally {
                zipOut.close();
            }

            final boolean tweakDD = (i & 1) != 0;
            final boolean tweakCFH = (i & 2) != 0;

            // Modify ZIP file to contain an incorrect CRC32 value in the
            // Central File Header.
            final byte[] crc = new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
            final RandomAccessFile raf = new RandomAccessFile(file, "rw");
            if (tweakDD) {
                raf.seek(raf.length() - 57 - 28); // CRC-32 position in Data Descriptor
                raf.write(crc);
            }
            if (tweakCFH) {
                raf.seek(raf.length() - 57); // CRC-32 position in Central File Header
                raf.write(crc);
            }
            raf.close();

            final ZipFile zipIn = new ZipFile(file); // NOT newZipFile(...) !

            try {
                InputStream in = zipIn.getCheckedInputStream(name);
                if (tweakDD ^ tweakCFH)
                    fail("Expected CRC32Exception!");

                // Open checked input stream and join immediately.
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
                while (in.read() != -1) { // read until EOF
                }
                in.close();

                if (tweakDD & tweakCFH)
                    fail("Expected CRC32Exception!");
            } catch (CRC32Exception ex) {
                assertTrue(tweakDD | tweakCFH);
            }

            zipIn.close();
        }
    }

    /**
     * This test appends more entries to an existing ZIP file.
     * <p>
     * Note that this may work with plain ZIP files only (e.g. not
     * with RAES encrypted ZIP files).
     * 
     * @throws IOException On any I/O failure.
     */
    @Test
    public void testAppending() throws IOException {
        // Setup data.
        final byte[] data1 = getData();
        final byte[] data2 = new byte[data1.length];
        rnd.nextBytes(data2);

        // Create and append.
        append(0, 20, data1);
        append(10, 20, data2);

        final ZipFile zipIn = newZipFile(file);
        assertEquals(30, zipIn.size());
        try {
            // Check that zipIn correctly enumerates all entries.
            final byte[] buf = new byte[data1.length];
            for (int i = 0; i < 30; i++) {
                final String name = i + ".txt";
                final ZipEntry entry = zipIn.getEntry(name);
                assertEquals(data1.length, entry.getSize());
                final InputStream in = zipIn.getInputStream(name);
                try {
                    int off = 0;
                    int read;
                    do {
                        read = in.read(buf, off, buf.length - off);
                        if (read < 0)
                            throw new EOFException();
                        assertTrue(read > 0);
                        off += read;
                    } while (off < buf.length);
                    assertEquals(-1, in.read());
                    assertTrue(Arrays.equals(i < 10 ? data1 : data2, buf));
                } finally {
                    in.close();
                }
            }
        } finally {
            zipIn.close();
        }
    }

    private void append(
            final int off,
            final int len,
            final byte[] data)
    throws IOException {
        final ZipOutputStream out;
        if (file.exists()) {
            final ZipFile in = newZipFile(file);
            in.close();
            out = newZipOutputStream(new FileOutputStream(file, true), in);
        } else {
            out = newZipOutputStream(new FileOutputStream(file));
        }
        try {
            for (int i = 0; i < len; i++) {
                final String name = off + i + ".txt";
                out.putNextEntry(newEntry(name));
                out.write(data);
            }
        } finally {
            out.close();
        }
    }
}
