/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.it.zip.base;

import global.namespace.truevfs.comp.util.ConcurrencyUtils.TaskFactory;
import global.namespace.truevfs.comp.zip.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;

import static global.namespace.truevfs.comp.util.ConcurrencyUtils.NUM_IO_THREADS;
import static global.namespace.truevfs.comp.util.ConcurrencyUtils.start;
import static global.namespace.truevfs.comp.util.HashMaps.initialCapacity;
import static java.nio.file.Files.*;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.Assert.*;

/**
 * Performs an integration test for reading and writing ZIP files.
 *
 * @author Christian Schlichtherle
 */
public abstract class ZipITSuite implements ZipEntryFactory<ZipEntry> {

    private static final Logger
            logger = LoggerFactory.getLogger(ZipITSuite.class);

    private static final boolean FORCE_ZIP64_EXT = Boolean.getBoolean(
            ZipEntry.class.getPackage().getName() + ".forceZip64Ext");

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
    private Path file;
    private byte[] data;

    @Before
    public void setUp() throws IOException {
        file = createTempFile(TEMP_FILE_PREFIX, null);
        delete(file);
        data = DATA.clone();
    }

    protected final Path getFile() {
        return file;
    }

    protected final byte[] getData() {
        return data.clone();
    }

    @After
    public void tearDown() {
        try {
            deleteIfExists(file);
        } catch (final IOException ex) {
            logger.trace(
                    "Failed to clean up test file (this may be just an implication of the root cause):",
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
            ZipFile appendee)
    throws IOException {
        return new ZipOutputStream(out, appendee);
    }

    protected ZipFile newZipFile(Path file)
    throws IOException {
        return new ZipFile(file).recoverLostEntries();
    }

    protected ZipFile newZipFile(
            Path file, Charset charset)
    throws IOException {
        return new ZipFile(file, charset).recoverLostEntries();
    }

    protected ZipFile newZipFile(SeekableByteChannel channel)
    throws IOException {
        return new ZipFile(channel).recoverLostEntries();
    }

    protected ZipFile newZipFile(
            SeekableByteChannel channel, Charset charset)
    throws IOException {
        return new ZipFile(channel, charset).recoverLostEntries();
    }

    @Test
    public final void testConstructors() throws Exception {
        write(file, data);

        try (SeekableByteChannel channel = newByteChannel(file)) {
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
                newZipFile(file, null);
                fail();
            } catch (NullPointerException ex) {
            }

            try {
                newZipFile((Path) null);
                fail();
            } catch (NullPointerException ex) {
            }

            try {
                newZipFile((Path) null, null);
                fail();
            } catch (NullPointerException ex) {
            }

            try {
                newZipFile((Path) null, Charset.forName("UTF-8"));
                fail();
            } catch (NullPointerException ex) {
            }

            try {
                newZipFile(file, null);
                fail();
            } catch (NullPointerException ex) {
            }

            try {
                newZipFile((SeekableByteChannel) null);
                fail();
            } catch (NullPointerException ex) {
            }

            try {
                newZipFile((SeekableByteChannel) null, null);
                fail();
            } catch (NullPointerException ex) {
            }

            try {
                newZipFile((SeekableByteChannel) null, Charset.forName("UTF-8"));
                fail();
            } catch (NullPointerException ex) {
            }

            try {
                newZipFile(channel, null);
                fail();
            } catch (NullPointerException ex) {
            }

            try {
                newZipFile(file);
                fail();
            } catch (IOException ex) {
            }

            try {
                newZipFile(file);
                fail();
            } catch (IOException ex) {
            }

            try {
                newZipFile(channel);
                fail();
            } catch (IOException ex) {
            }

            try {
                newZipFile(file, Charset.forName("UTF-8"));
                fail();
            } catch (IOException ex) {
            }

            try {
                newZipFile(channel, Charset.forName("UTF-8"));
                fail();
            } catch (IOException ex) {
            }
        }
        delete(file);
    }

    @Test
    public final void testPreambleOfEmptyZipFile() throws IOException {
        // Create empty ZIP file.
        newZipOutputStream(newOutputStream(file)).close();

        try (ZipFile zf = newZipFile(file)) {
            assertEquals(0, zf.getPreambleLength());
            try (InputStream in = zf.getPreambleInputStream()) {
                assertEquals(-1, in.read());
            }
        }
    }

    @Test
    public final void testGetInputStream() throws IOException {
        try (ZipOutputStream zos = newZipOutputStream(newOutputStream(file))) {
            zos.putNextEntry(newEntry("foo"));
        }

        try (ZipFile zf = newZipFile(file)) {
            zf.getInputStream("foo").close();
            assertNull(zf.getInputStream("bar"));
        }
    }

    @Test
    public final void testWriteAndReadSingleBytes() throws IOException {
        try (ZipOutputStream zos = newZipOutputStream(newOutputStream(file))) {
            zos.putNextEntry(newEntry("file"));
            for (int i = 0; i < data.length; i++)
                zos.write(data[i]);
        }

        try (ZipFile zf = newZipFile(file);
             InputStream in = zf.getInputStream("file")) {
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

        try (ZipFile zf = newZipFile(file)) {
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
                        final List<ZipEntry> entries = Collections.list((Enumeration<ZipEntry>) zf.entries());
                        assert entries.size() == nEntries; // this would be a programming error in the test class itself - not the class under test!
                        Collections.shuffle(entries, rnd);

                        // Now read in the entries in the shuffled order.
                        final byte[] buf = new byte[data.length];
                        for (ZipEntry entry : entries) {
                            // Read full entry and check the contents.
                            try (InputStream in = zf.getInputStream(entry.getName())) {
                                int off = 0;
                                int read;
                                while (true) {
                                    read = in.read(buf);
                                    if (read < 0)
                                        break;
                                    assertTrue(read > 0);
                                    assertEquals(   ByteBuffer.wrap(data, off, read),
                                                    ByteBuffer.wrap(buf, 0, read));
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

            start(nThreads, new CheckAllEntriesFactory()).join();
        }
    }

    /**
     * Creates test ZIP file with {@code nEntries}.
     * The field {@code file} is used to determine the ZIP file.
     */
    private void createTestZipFile(final int nEntries) throws IOException {
        final HashSet<String> set = new HashSet<>(initialCapacity(nEntries));

        try (ZipOutputStream zos = newZipOutputStream(newOutputStream(file))) {
            for (int i = 0; i < nEntries; i++) {
                String name = i + ".txt";
                zos.putNextEntry(newEntry(name));
                zos.write(data);
                assertTrue(set.add(name));
            }
        }

        try (ZipFile zf = newZipFile(file)) {
            // Check that zf correctly enumerates all entries.
            for (ZipEntry entry : zf) {
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
        try (ZipOutputStream zos = newZipOutputStream(newOutputStream(file))) {
            zos.putNextEntry(newEntry(name));
            zos.write(data);
        }

        try (ZipFile zf = newZipFile(file)) {
            InputStream in = zf.getCheckedInputStream(name);
            in.close();

            // Open checked input stream and read fully, using multiple methods.
            in = zf.getCheckedInputStream(name);
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
     * @throws IOException On any I/O error.
     */
    @Test
    @SuppressWarnings("try")
    public void testBadGetCheckedInputStream() throws IOException {
        if (FORCE_ZIP64_EXT)
            fail("TODO: Adapt this test so that it works when ZIP64 extensions have been forced to use!");

        for (int i = 0; i < 4; i++) {
            // Create test ZIP file.
            final String name = "entry";
            try (ZipOutputStream zos = newZipOutputStream(newOutputStream(file))) {
                zos.putNextEntry(newEntry(name));
                zos.write(data);
            }

            final boolean tweakDD = (i & 1) != 0;
            final boolean tweakCFH = (i & 2) != 0;

            // Modify ZIP file to contain an incorrect CRC32 value in the
            // Central File Header.
            final ByteBuffer crc = ByteBuffer.wrap(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF });
            try (SeekableByteChannel channel = newByteChannel(file, WRITE)) {
                if (tweakDD) {
                    channel.position(channel.size() - 57 - 28); // CRC-32 position in Data Descriptor
                    channel.write(crc);
                }
                if (tweakCFH) {
                    channel.position(channel.size() - 57); // CRC-32 position in Central File Header
                    channel.write(crc);
                }
            }

            try (ZipFile zf = new ZipFile(file)) {
                try {
                    // Open checked input stream and join immediately.
                    try (InputStream ignored = zf.getCheckedInputStream(name)) {
                        if (tweakDD ^ tweakCFH)
                            fail("Expected CRC32Exception!");
                    }
                    if (tweakDD & tweakCFH)
                        fail("Expected CRC32Exception!");
                } catch (Crc32Exception ex) {
                    assertTrue(tweakDD | tweakCFH);
                }

                try {
                    // Open checked input stream and read fully, using multiple methods.
                    try (InputStream in = zf.getCheckedInputStream(name)) {
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
                } catch (Crc32Exception ex) {
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
     * @throws IOException On any I/O error.
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

        try (ZipFile zf = newZipFile(file)) {
            assertEquals(30, zf.size());
            // Check that zf correctly enumerates all entries.
            final byte[] buf = new byte[data1.length];
            for (int i = 0; i < 30; i++) {
                final String name = i + ".txt";
                assertEquals(data1.length, zf.entry(name).get().getSize());
                try (InputStream in = zf.getInputStream(name)) {
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
        final ZipOutputStream zos;
        if (exists(file)) {
            final ZipFile zf = newZipFile(file);
            zf.close();
            zos = newZipOutputStream(newOutputStream(file, APPEND), zf);
        } else {
            zos = newZipOutputStream(newOutputStream(file));
        }
        try {
            for (int i = 0; i < len; i++) {
                final String name = off + i + ".txt";
                zos.putNextEntry(newEntry(name));
                zos.write(data);
            }
        } finally {
            zos.close();
        }
    }
}
