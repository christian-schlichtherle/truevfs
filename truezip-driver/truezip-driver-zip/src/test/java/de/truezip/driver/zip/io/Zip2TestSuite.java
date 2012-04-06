/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.io;

import static de.truezip.driver.zip.io.Constants.FORCE_ZIP64_EXT;
import de.truezip.kernel.util.ArrayUtils;
import static de.truezip.kernel.util.ConcurrencyUtils.NUM_IO_THREADS;
import de.truezip.kernel.util.ConcurrencyUtils.TaskFactory;
import static de.truezip.kernel.util.ConcurrencyUtils.runConcurrent;
import de.truezip.kernel.util.Maps;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
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
public abstract class Zip2TestSuite implements ZipEntryFactory<ZipEntry> {

    private static final Logger
            logger = Logger.getLogger(Zip2TestSuite.class.getName());

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

    protected final File getFile() {
        return file;
    }

    protected final byte[] getData() {
        return data.clone();
    }

    @After
    public void tearDown() {
        try {
            if (file.exists() && !file.delete())
                throw new IOException(file + " (could not delete)");
        } catch (final IOException ex) {
            logger.log(Level.FINEST,
                    "Failed to clean up test file (this may be just an aftermath):",
                    ex);
        }
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
            ZipReadOnlyChannel appendee)
    throws ZipException {
        return new ZipOutputStream(out, appendee);
    }

    protected ZipReadOnlyChannel newZipReadOnlyChannel(String name)
    throws IOException {
        return new ZipReadOnlyChannel(name);
    }

    protected ZipReadOnlyChannel newZipReadOnlyChannel(
            String name, Charset charset)
    throws IOException {
        return new ZipReadOnlyChannel(name, charset);
    }

    protected ZipReadOnlyChannel newZipReadOnlyChannel(File file)
    throws IOException {
        return new ZipReadOnlyChannel(file);
    }

    protected ZipReadOnlyChannel newZipReadOnlyChannel(
            File file, Charset charset)
    throws IOException {
        return new ZipReadOnlyChannel(file, charset);
    }

    protected ZipReadOnlyChannel newZipReadOnlyChannel(SeekableByteChannel channel)
    throws IOException {
        return new ZipReadOnlyChannel(channel);
    }

    protected ZipReadOnlyChannel newZipReadOnlyChannel(
            SeekableByteChannel channel, Charset charset)
    throws IOException {
        return new ZipReadOnlyChannel(channel, charset);
    }

    @Test
    public final void testConstructors() throws Exception {
        Files.write(file.toPath(), data);

        try (final SeekableByteChannel channel = FileChannel.open(file.toPath())) {
            try {
                newZipOutputStream(null, (Charset) null);
                fail();
            } catch (NullPointerException ex) {
            }

            try {
                newZipOutputStream(null, (ZipReadOnlyChannel) null);
                fail();
            } catch (NullPointerException ex) {
            }

            try {
                newZipOutputStream(new ByteArrayOutputStream(), (Charset) null);
                fail();
            } catch (NullPointerException ex) {
            }

            try {
                newZipOutputStream(new ByteArrayOutputStream(), (ZipReadOnlyChannel) null);
                fail();
            } catch (NullPointerException ex) {
            }

            try {
                newZipOutputStream(null, Charset.forName("UTF-8"));
                fail();
            } catch (NullPointerException ex) {
            }

            try {
                newZipReadOnlyChannel((String) null);
                fail();
            } catch (NullPointerException ex) {
            }

            try {
                newZipReadOnlyChannel((String) null, null);
                fail();
            } catch (NullPointerException ex) {
            }

            try {
                newZipReadOnlyChannel((String) null, Charset.forName("UTF-8"));
                fail();
            } catch (NullPointerException ex) {
            }

            try {
                newZipReadOnlyChannel(file.getPath(), null);
                fail();
            } catch (NullPointerException ex) {
            }

            try {
                newZipReadOnlyChannel((File) null);
                fail();
            } catch (NullPointerException ex) {
            }

            try {
                newZipReadOnlyChannel((File) null, null);
                fail();
            } catch (NullPointerException ex) {
            }

            try {
                newZipReadOnlyChannel((File) null, Charset.forName("UTF-8"));
                fail();
            } catch (NullPointerException ex) {
            }

            try {
                newZipReadOnlyChannel(file, null);
                fail();
            } catch (NullPointerException ex) {
            }

            try {
                newZipReadOnlyChannel((SeekableByteChannel) null);
                fail();
            } catch (NullPointerException ex) {
            }

            try {
                newZipReadOnlyChannel((SeekableByteChannel) null, null);
                fail();
            } catch (NullPointerException ex) {
            }

            try {
                newZipReadOnlyChannel((SeekableByteChannel) null, Charset.forName("UTF-8"));
                fail();
            } catch (NullPointerException ex) {
            }

            try {
                newZipReadOnlyChannel(channel, null);
                fail();
            } catch (NullPointerException ex) {
            }

            try {
                newZipReadOnlyChannel(file.getPath());
                fail();
            } catch (IOException ex) {
            }

            try {
                newZipReadOnlyChannel(file);
                fail();
            } catch (IOException ex) {
            }

            try {
                newZipReadOnlyChannel(channel);
                fail();
            } catch (IOException ex) {
            }

            try {
                newZipReadOnlyChannel(file, Charset.forName("UTF-8"));
                fail();
            } catch (IOException ex) {
            }

            try {
                newZipReadOnlyChannel(channel, Charset.forName("UTF-8"));
                fail();
            } catch (IOException ex) {
            }
        }
        assertTrue(file.delete());
    }

    @Test
    public final void testPreambleOfEmptyZipFile() throws IOException {
        // Create empty ZIP file.
        newZipOutputStream(new FileOutputStream(file)).close();

        try (final ZipReadOnlyChannel zipIn = newZipReadOnlyChannel(file)) {
            assertEquals(0, zipIn.getPreambleLength());
            try (final InputStream in = zipIn.getPreambleInputStream()) {
                assertEquals(-1, in.read());
            }
        }
    }

    @Test
    public final void testGetInputStream() throws IOException {
        try (final ZipOutputStream zipOut = newZipOutputStream(new FileOutputStream(file))) {
            zipOut.putNextEntry(newEntry("foo"));
        }

        try (final ZipReadOnlyChannel zipIn = newZipReadOnlyChannel(file)) {
            zipIn.getInputStream("foo").close();
            assertNull(zipIn.getInputStream("bar"));
        }
    }

    @Test
    public final void testWriteAndReadSingleBytes() throws IOException {
        try (final ZipOutputStream zipOut = newZipOutputStream(new FileOutputStream(file))) {
            zipOut.putNextEntry(newEntry("file"));
            for (int i = 0; i < data.length; i++)
                zipOut.write(data[i]);
        }

        try (   final ZipReadOnlyChannel zipIn = newZipReadOnlyChannel(file);
                final InputStream in = zipIn.getInputStream("file")) {
            for (int c, i = 0; 0 <= (c = in.read()); i++)
                assertEquals(data[i] & 0xff, c);
        }
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

        try (final ZipReadOnlyChannel zin = newZipReadOnlyChannel(file)) {
            final class CheckAllEntriesFactory implements TaskFactory {
                @Override
                public Callable<?> newTask(int threadNum) {
                    return new CheckAllEntries();
                }

                final class CheckAllEntries implements Callable<Void> {
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
                            try (final InputStream in = zin.getInputStream(entry.getName())) {
                                int off = 0;
                                int read;
                                while (true) {
                                    read = in.read(buf);
                                    if (read < 0)
                                        break;
                                    assertTrue(read > 0);
                                    assertTrue(ArrayUtils.equals(data, off, buf, 0, read));
                                    off += read;
                                }
                                assertEquals(-1, read);
                                assertEquals(off, data.length);
                                assertTrue(0 >= in.read(new byte[0]));
                            }
                        }
                        return null;
                    }
                } // CheckAllEntries
            } // CheckAllEntriesFactory

            runConcurrent(nThreads, new CheckAllEntriesFactory()).join();
        }
    }

    /**
     * Creates test ZIP file with {@code nEntries}.
     * The field {@code file} is used to determine the ZIP file.
     */
    private void createTestZipFile(final int nEntries) throws IOException {
        final HashSet<String>
                set = new HashSet<>(Maps.initialCapacity(nEntries));

        try (final ZipOutputStream zout = newZipOutputStream(new FileOutputStream(file))) {
            for (int i = 0; i < nEntries; i++) {
                String name = i + ".txt";
                zout.putNextEntry(newEntry(name));
                zout.write(data);
                assertTrue(set.add(name));
            }
        }

        try (final ZipReadOnlyChannel zin = newZipReadOnlyChannel(file)) {
            // Check that zipIn correctly enumerates all entries.
            for (ZipEntry entry : zin) {
                assertEquals(data.length, entry.getSize());
                assertTrue(set.remove(entry.getName()));
            }
            assertTrue(set.isEmpty());
        }
    }

    @Test
    public final void testGoodGetCheckedInputStream() throws IOException {
        // Create test ZIP file.
        final String name = "entry";
        try (final ZipOutputStream zipOut = newZipOutputStream(new FileOutputStream(file))) {
            zipOut.putNextEntry(newEntry(name));
            zipOut.write(data);
        }

        try (final ZipReadOnlyChannel zipIn = newZipReadOnlyChannel(file)) {
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
        }
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
            try (final ZipOutputStream zipOut = newZipOutputStream(
                   new FileOutputStream(file))) {
                zipOut.putNextEntry(newEntry(name));
                zipOut.write(data);
            }

            final boolean tweakDD = (i & 1) != 0;
            final boolean tweakCFH = (i & 2) != 0;

            // Modify ZIP file to contain an incorrect CRC32 value in the
            // Central File Header.
            final byte[] crc = new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
            try (final RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
                if (tweakDD) {
                    raf.seek(raf.length() - 57 - 28); // CRC-32 position in Data Descriptor
                    raf.write(crc);
                }
                if (tweakCFH) {
                    raf.seek(raf.length() - 57); // CRC-32 position in Central File Header
                    raf.write(crc);
                }
            }

            try (final ZipFile zipIn = new ZipFile(file)) {
                try {
                    // Open checked input stream and join immediately.
                    try (final InputStream in = zipIn.getCheckedInputStream(name)) {
                        if (tweakDD ^ tweakCFH)
                            fail("Expected CRC32Exception!");
                    }
                    if (tweakDD & tweakCFH)
                        fail("Expected CRC32Exception!");
                } catch (CRC32Exception ex) {
                    assertTrue(tweakDD | tweakCFH);
                }

                try {
                    // Open checked input stream and read fully, using multiple methods.
                    try (final InputStream in = zipIn.getCheckedInputStream(name)) {
                        if (tweakDD ^ tweakCFH)
                            fail("Expected CRC32Exception!");

                        final int n = data.length / 4;
                        in.skip(n);
                        in.read(new byte[n]);
                        in.read(new byte[n], 0, n);
                        while (in.read() != -1) { // read until EOF
                        }
                    }
                    if (tweakDD & tweakCFH)
                        fail("Expected CRC32Exception!");
                } catch (CRC32Exception ex) {
                    assertTrue(tweakDD | tweakCFH);
                }
            }
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

        try (final ZipReadOnlyChannel zipIn = newZipReadOnlyChannel(file)) {
            assertEquals(30, zipIn.size());
            // Check that zipIn correctly enumerates all entries.
            final byte[] buf = new byte[data1.length];
            for (int i = 0; i < 30; i++) {
                final String name = i + ".txt";
                final ZipEntry entry = zipIn.getEntry(name);
                assertEquals(data1.length, entry.getSize());
                try (final InputStream in = zipIn.getInputStream(name)) {
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
                }
            }
        }
    }

    private void append(
            final int off,
            final int len,
            final byte[] data)
    throws IOException {
        final ZipOutputStream out;
        if (file.exists()) {
            final ZipReadOnlyChannel in = newZipReadOnlyChannel(file);
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
