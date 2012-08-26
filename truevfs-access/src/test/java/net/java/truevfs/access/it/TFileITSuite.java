/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access.it;

import java.io.*;
import static java.io.File.separatorChar;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import net.java.truecommons.io.ClosedInputException;
import net.java.truecommons.io.ClosedOutputException;
import net.java.truecommons.io.InputException;
import net.java.truecommons.shed.BitField;
import static net.java.truecommons.shed.ConcurrencyUtils.NUM_IO_THREADS;
import net.java.truecommons.shed.ConcurrencyUtils.TaskFactory;
import net.java.truecommons.shed.ConcurrencyUtils.TaskJoiner;
import static net.java.truecommons.shed.ConcurrencyUtils.start;
import net.java.truevfs.access.ConfiguredClientTestBase;
import net.java.truevfs.access.TConfig;
import net.java.truevfs.access.TFile;
import net.java.truevfs.access.TFileInputStream;
import net.java.truevfs.access.TFileOutputStream;
import net.java.truevfs.access.TVFS;
import static net.java.truevfs.kernel.spec.FsAccessOption.*;
import net.java.truevfs.kernel.spec.FsArchiveDriver;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsResourceOpenException;
import net.java.truevfs.kernel.spec.FsSyncException;
import static net.java.truevfs.kernel.spec.FsSyncOption.CLEAR_CACHE;
import static net.java.truevfs.kernel.spec.FsSyncOption.WAIT_CLOSE_IO;
import static net.java.truevfs.kernel.spec.FsSyncOptions.SYNC;
import net.java.truevfs.kernel.spec.FsSyncWarningException;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs integration testing of a particular {@link FsArchiveDriver}
 * by using the API of the TrueVFS Access File* module.
 *
 * @param  <D> the type of the archive driver.
 * @author Christian Schlichtherle
 */
public abstract class TFileITSuite<D extends FsArchiveDriver<?>>
extends ConfiguredClientTestBase<D> {

    private static final Logger
            logger = LoggerFactory.getLogger(TFileITSuite.class);

    /**
     * The prefix for temporary files, which is {@value}.
     * This value should identify the TrueVFS Access File* module in order to
     * ensure that no two temporary files are shared between tests of the
     * TrueVFS Access Path API and the TrueVFS Access File* API.
     */
    private static final String TEMP_FILE_PREFIX = "tzp-file";

    private File temp;
    private TFile archive;

    @Override
    public void setUp() throws IOException {
        super.setUp();
        temp = createTempFile();
        TFile.rm(temp);
        archive = new TFile(temp);
    }

    @Override
    public void tearDown() {
        try {
            try {
                umount();
            } finally {
                archive = null;
                final File temp = this.temp;
                this.temp = null;
                if (null != temp && temp.exists() && !temp.delete())
                    throw new IOException(temp + " (could not delete)");
            }
        } catch (final IOException ex) {
            logger.trace(
                    "Failed to clean up test file (this may be just an aftermath):",
                    ex);
        } finally {
            super.tearDown();
        }
    }

    /** Returns the current archive file. */
    protected final TFile getArchive() {
        return archive;
    }

    /** Unmounts the {@linkplain #getArchive() current archive file}. */
    protected final void umount() throws FsSyncException {
        if (null != archive) TVFS.umount(archive);
    }

    private File createTempFile() throws IOException {
        // TODO: Removing .getCanonicalFile() causes archive.rm_r() to
        // fail in testCopyContainingOrSameFiles() - explain why!
        return File.createTempFile(TEMP_FILE_PREFIX, getExtension()).getCanonicalFile();
    }

    protected final void createTestFile(final TFile file) throws IOException {
        try (final OutputStream out = new TFileOutputStream(file)) {
            out.write(getData());
        }
    }

    protected final void verifyTestFile(final TFile file) throws IOException {
        assertEquals(getDataLength(), file.length());
        final byte[] array = new byte[getDataLength()];
        try (final InputStream in = new TFileInputStream(file)) {
            new DataInputStream(in).readFully(array);
        }
        assertArrayEquals(getData(), array);
    }

    @Test
    public void testArchiveControllerStateWithInputStream()
    throws IOException, InterruptedException {
        assertArchiveControllerStateWithResource(
                new Factory<InputStream, String, IOException>() {
            @Override
            public InputStream create(String entry) throws IOException {
                return new TFileInputStream(entry);
            }
        });
    }

    @Test
    public void testArchiveControllerStateWithOutputStream()
    throws IOException, InterruptedException {
        assertArchiveControllerStateWithResource(
                new Factory<OutputStream, String, IOException>() {
            @Override
            public OutputStream create(String entry) throws IOException {
                return new TFileOutputStream(entry);
            }
        });
    }

    private interface Factory<O, P, E extends Exception> {
        O create(P param) throws E;
    }

    private void assertArchiveControllerStateWithResource(
            final Factory<? extends Closeable, ? super String, ? extends IOException> factory)
    throws IOException, InterruptedException {
        final String entry = archive + "/entry";
        archive = null;
        assertTrue(new TFile(entry).createNewFile());
        TVFS.umount(new TFile(entry).getTopLevelArchive());
        final ReferenceQueue<FsController> queue;
        final Reference<FsController> expected;
        try (final Closeable resource = factory.create(entry)) {
            queue = new ReferenceQueue<>();
            expected = new WeakReference<>(
                         controller(new TFile(entry).toNodePath()), queue);
            System.gc();
            assertNull(queue.remove(TIMEOUT_MILLIS));
            assertSame(expected.get(), controller(new TFile(entry).toNodePath()));
        }
        System.gc();
        assertNull(queue.remove(TIMEOUT_MILLIS));
        assertSame(expected.get(), controller(new TFile(entry).toNodePath()));
        TVFS.umount(new TFile(entry).getTopLevelArchive());
        Reference<? extends FsController> got;
        do {
            // triggering GC in a loop seems to help with concurrency!
            System.gc();
        } while (null == (got = queue.remove(TIMEOUT_MILLIS)));
        assert expected == got;
        assert null == expected.get();
    }

    @Test
    public void testFalsePositives() throws IOException {
        assertFalsePositive(archive);

        // Dito for entry.
        final TFile entry = new TFile(archive, "entry" + getExtension());

        assertTrue(archive.toNonArchiveFile().mkdir());
        assertFalsePositive(entry);
        archive.rm();

        assertTrue(archive.mkdir());
        assertFalsePositive(entry);
        archive.rm();
    }

    private void assertFalsePositive(final TFile file) throws IOException {
        assert file.isArchive();

        // Note that file's parent directory may be a directory in the host file system!

        // Create file false positive.
        createTestFile(file);

        // Overwrite.
        createTestFile(file);

        assertTrue(file.exists());
        assertFalse(file.isDirectory());
        assertTrue(file.isFile());
        assertEquals(getDataLength(), file.length());
        assertTrue(file.lastModified() > 0);

        // Read back portion
        try (final InputStream in = new TFileInputStream(file)) {
            byte[] buf = new byte[getDataLength()];
            final int read = in.read(buf);
            assertEquals(   ByteBuffer.wrap(getData(), 0, read),
                            ByteBuffer.wrap(buf, 0, read));
        }
        assertRm(file);

        // Create directory false positive.

        assertTrue(file.toNonArchiveFile().mkdir());
        assertTrue(file.exists());
        assertTrue(file.isDirectory());
        assertFalse(file.isFile());
        //assertEquals(0, file.getLength());
        assertTrue(file.lastModified() > 0);

        try {
            new TFileInputStream(archive).close();
            if ('\\' == separatorChar)
                fail();
        } catch (IOException ex) {
            if ('\\' != separatorChar && !archive.isArchive() && !archive.isEntry())
                throw ex;
        }

        try {
            new TFileOutputStream(archive).close();
            fail();
        } catch (IOException expected) {
        }

        assertRm(file);

        // Create regular archive file.

        assertTrue(file.mkdir());
        assertTrue(file.exists());
        assertTrue(file.isDirectory());
        assertFalse(file.isFile());
        //assertEquals(0, file.getLength());
        assertTrue(file.lastModified() > 0);

        try {
            new TFileInputStream(archive).close();
            if ('\\' == separatorChar)
                fail();
        } catch (IOException ex) {
            if ('\\' != separatorChar && !archive.isArchive())
                throw ex;
        }

        try {
            new TFileOutputStream(archive).close();
            fail();
        } catch (IOException expected) {
        }

        assertRm(file);
    }

    private void assertRm(final TFile file) throws IOException {
        file.rm();
        assertFalse(file.exists());
        assertFalse(file.isDirectory());
        assertFalse(file.isFile());
        assertEquals(0, file.length());
        assertFalse(file.lastModified() > 0);
    }

    @Test
    public void testCreateNewFile() throws IOException{
        assertCreateNewPlainFile();
        assertCreateNewEnhancedFile();
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    private void assertCreateNewPlainFile() throws IOException {
        final File archive = createTempFile();
        TFile.rm(archive);
        final File file1 = new File(archive, "test.txt");
        final File file2 = new File(file1, "test.txt");
        try {
            file1.createNewFile();
            fail("Creating a file in a non-existent directory should throw an IOException!");
        } catch (IOException expected) {
        }
        assertCreateNewFile(archive, file1, file2);
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    private void assertCreateNewEnhancedFile() throws IOException {
        final File file1 = new TFile(archive, "test.txt");
        final File file2 = new TFile(file1, "test.txt");
        try (final TConfig config = TConfig.open()) {
            config.setLenient(false);
            try {
                file1.createNewFile();
                fail("Creating a file in a non-existent directory should throw an IOException!");
            } catch (IOException expected) {
            }
            assertCreateNewFile(archive, file1, file2);
        }
        assertCreateNewFile(archive, file1, file2);
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    private void assertCreateNewFile(   final File dir,
                                        final File file1,
                                        final File file2)
    throws IOException {
        assertFalse(dir.exists());
        
        assertTrue(dir.mkdir());
        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());
        assertFalse(dir.isFile());
        if (dir instanceof TFile) {
            final TFile tdir = (TFile) dir;
            if (tdir.isArchive() || tdir.isEntry())
                assertEquals(0, dir.length());
        }
        
        assertTrue(file1.createNewFile());
        assertTrue(file1.exists());
        assertFalse(file1.isDirectory());
        assertTrue(file1.isFile());
        assertEquals(0, file1.length());
        
        try {
            file2.createNewFile();
            fail("Creating a file in another file should throw an IOException!");
        } catch (IOException expected) {
        }
        
        TFile.rm(file1); // OK now!
        assertFalse(file1.exists());
        assertFalse(file1.isDirectory());
        assertFalse(file1.isFile());
        assertEquals(0, file1.length());
        
        TFile.rm(dir);
        assertFalse(dir.exists());
        assertFalse(dir.isDirectory());
        assertFalse(dir.isFile());
        assertEquals(0, dir.length());
    }

    @Test
    public void testIllegalDirectoryOperations() throws IOException {
        try {
            final String[] names = {
                "inner" + getExtension(),
                "dir",
            };
            TFile file = archive;
            for (int i = 0; i <= names.length; i++) {
                final TFile file2 = file.toNonArchiveFile();
                assertTrue(file2.mkdir());
                assertIllegalDirectoryOperations(file2);
                file2.rm();
                assertTrue(file.mkdir());
                assertIllegalDirectoryOperations(file);
                if (i < names.length)
                    file = new TFile(file, names[i]);
            }
        } finally {
            archive.rm_r();
        }
    }

    private void assertIllegalDirectoryOperations(final TFile dir)
    throws IOException {
        assert dir.isDirectory();
        try {
            new TFileInputStream(dir).close();
            if ('\\' == separatorChar)
                fail();
        } catch (IOException ex) {
            if ('\\' != separatorChar && !dir.isArchive() && !dir.isEntry())
                throw ex;
        }
        try {
            new TFileOutputStream(dir).close();
            fail();
        } catch (IOException expected) {
        }
        File tmp = TFile.createTempFile(TEMP_FILE_PREFIX, null);
        try {
            try {
                TFile.cp(tmp, dir);
                fail();
            } catch (IOException expected) {
            }
            try {
                TFile.cp(dir, tmp);
                fail();
            } catch (IOException expected) {
            }
        } finally {
            TFile.rm(tmp);
        }
    }

    @Test
    public void testStrictFileOutputStream() throws IOException {
        TFile file = new TFile(archive, "test.txt");
        try (final TConfig config = TConfig.open()) {
            config.setLenient(false);
            try {
                assertFileOutputStream(file);
                fail("Creating ghost directories should not be allowed when File.isLenient() is false!");
            } catch (IOException expected) {
            }
            assertTrue(archive.mkdir());
            assertFileOutputStream(file);
            archive.rm();
        }
    }

    @Test
    public void testLenientFileOutputStream() throws IOException {
        TFile file = new TFile(archive, "dir/inner" + getExtension() + "/dir/test.txt");

        assertFileOutputStream(file);

        try {
            archive.rm();
            fail("directory not empty");
        } catch (IOException expected) {
        }
        umount(); // allow external modifications!
        TFile.rm(archive.toNonArchiveFile()); // use plain file to delete instead!
        assertFalse(archive.exists());
        assertFalse(archive.isDirectory());
        assertFalse(archive.isFile());
        assertEquals(0, archive.length());
    }

    private void assertFileOutputStream(final TFile file) throws IOException {
        final byte[] message = "Hello World!\r\n".getBytes();

        try (final OutputStream out = new TFileOutputStream(file)) {
            assertTrue(file.exists());
            assertFalse(file.isDirectory());
            assertTrue(file.isFile());
            assertEquals(0, file.length());
            out.write(message);
            assertEquals(0, file.length());
            out.flush();
            assertEquals(0, file.length());
        }
        assertTrue(file.exists());
        assertFalse(file.isDirectory());
        assertTrue(file.isFile());
        assertEquals(message.length, file.length());

        assertFalse(file.createNewFile());
        assertTrue(file.exists());
        assertFalse(file.isDirectory());
        assertTrue(file.isFile());
        assertEquals(message.length, file.length());

        file.rm();
        assertFalse(file.exists());
        assertFalse(file.isDirectory());
        assertFalse(file.isFile());
        assertEquals(0, file.length());
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OS_OPEN_STREAM")
    @Test
    public void testBusyFileInputStream()
    throws IOException, InterruptedException {
        final TFile file1 = new TFile(archive, "file1");
        final TFile file2 = new TFile(archive, "file2");

        assertTrue(file1.createNewFile());
        umount(); // redundant
        assertTrue(file2.createNewFile()); // calls FsController.mknod()
        final InputStream in1 = new TFileInputStream(file1);
        try {
            try {
                new TFileInputStream(file2).close();
                fail();
            } catch (final FsSyncException ex) {
                if (!(ex.getCause() instanceof FsResourceOpenException))
                    throw ex;
            }
            file2.input(in1);

            // in1 is still open!
            try {
                umount(); // forces closing of in1
                fail();
            } catch (final FsSyncWarningException ex) {
                if (!(ex.getCause() instanceof FsResourceOpenException))
                    throw ex;
            }
            assertTrue(file2.isFile());
            try {
                file2.input(in1);
                fail();
            } catch (final InputException ex) {
                if (!(ex.getCause() instanceof ClosedInputException))
                    throw ex;
                assertFalse(file2.exists()); // previous op has removed file2!
            }

            // Open file1 as stream and let the garbage collection close the stream automatically.
            new TFileInputStream(file1);

            try {
                // This operation may succeed without any exception if
                // the garbage collector did its job.
                umount(); // allow external modifications!
            } catch (final FsSyncWarningException ex) {
                // It may fail once if a stream was busy!
                if (!(ex.getCause() instanceof FsResourceOpenException))
                    throw ex;
            }
            umount(); // It must not fail twice for the same reason!

            TFile.rm(archive.toNonArchiveFile());
        } finally {
            // Closing the invalidated stream explicitly should be OK.
            in1.close();
        }

        // Cleanup.
        try {
            file2.rm();
            fail();
        } catch (IOException alreadyDeletedExternally) {
        }
        assertFalse(file2.exists());
        try {
            file1.rm();
            fail();
        } catch (IOException alreadyDeletedExternally) {
        }
        assertFalse(file1.exists());
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OS_OPEN_STREAM")
    @Test
    public void testBusyFileOutputStream()
    throws IOException, InterruptedException {
        TFile file1 = new TFile(archive, "file1");
        TFile file2 = new TFile(archive, "file2");

        // Ensure that there are two entries in the archive.
        // This is used later to check whether the update operation knows
        // how to deal with updating an archive for which there is still
        // an open output stream.
        try (final OutputStream out = new TFileOutputStream(file1)) {
            TFile.cat(new ByteArrayInputStream(getData()), out);
        }

        try (final OutputStream out = new TFileOutputStream(file2)) {
            TFile.cat(new ByteArrayInputStream(getData()), out);
        }

        umount(); // ensure two entries in the archive

        OutputStream out = new TFileOutputStream(file1);
        TFile.cat(new ByteArrayInputStream(getData()), out);

        // out is still open!
        try {
            new TFileOutputStream(file1).close();
            fail();
        } catch (final FsSyncException ex) {
            if (!(ex.getCause() instanceof FsResourceOpenException))
                throw ex;
        }

        // out is still open!
        try {
            new TFileOutputStream(file2).close();
        } catch (final FsSyncException ex) {
            if (!(ex.getCause() instanceof FsResourceOpenException))
                throw ex;
            logger.info(
                    getArchiveDriver().getClass()
                        + " does not support concurrent writing of different entries in the same archive file.",
                    ex);
        }

        // out is still open!
        TFile.cat(new ByteArrayInputStream(getData()), out); // write again

        // out is still open!
        try {
            umount(); // forces closing of all streams
            fail();
        } catch (final FsSyncWarningException ex) {
            if (!(ex.getCause() instanceof FsResourceOpenException))
                throw ex;
        }

        try {
            TFile.cat(new ByteArrayInputStream(getData()), out); // write again
            fail();
        } catch (ClosedOutputException ex) {
        }

        // The stream has been forcibly closed by TFile.update().
        // Another close is OK, though!
        out.close();

        // Reopen stream and let the garbage collection close the stream automatically.
        new TFileOutputStream(file1);
        out = null;

        try {
            // This operation may succeed without any exception if
            // the garbage collector did its job.
            umount(); // allow external modifications!
        } catch (final FsSyncWarningException ex) {
            // It may fail once if a stream was busy!
            if (!(ex.getCause() instanceof FsResourceOpenException))
                throw ex;
        }
        umount(); // It must not fail twice for the same reason!

        // Cleanup.
        file2.rm();
        assertFalse(file2.exists());
        file1.rm();
        assertFalse(file1.exists());
    }
    
    @Test
    public void testMkdir() throws IOException {
        final TFile dir1 = archive;
        final TFile dir2 = new TFile(dir1, "dir");
        final TFile dir3 = new TFile(dir2, "inner" + getExtension());
        final TFile dir4 = new TFile(dir3, "dir");
        final TFile dir5 = new TFile(dir4, "nuts" + getExtension());
        final TFile dir6 = new TFile(dir5, "dir");
        
        assert TConfig.get().isLenient();
        
        assertTrue(dir6.mkdir()); // create all at once! note archive is in current directory!
        
        assertFalse(dir6.mkdir()); // exists already!
        assertFalse(dir5.mkdir()); // exists already!
        assertFalse(dir4.mkdir()); // exists already!
        assertFalse(dir3.mkdir()); // exists already!
        assertFalse(dir2.mkdir()); // exists already!
        assertFalse(dir1.mkdir()); // exists already!
        
        dir6.rm();
        dir5.rm();
        dir4.rm();
        dir3.rm();
        dir2.rm();
        dir1.rm();

        try (final TConfig config = TConfig.open()) {
            config.setLenient(false);

            assertFalse(dir6.mkdir());
            assertFalse(dir5.mkdir());
            assertFalse(dir4.mkdir());
            assertFalse(dir3.mkdir());
            assertFalse(dir2.mkdir());

            assertTrue(dir1.mkdir());
            assertTrue(dir2.mkdir());
            assertTrue(dir3.mkdir());
            assertTrue(dir4.mkdir());
            assertTrue(dir5.mkdir());
            assertTrue(dir6.mkdir());
        }

        dir6.rm();
        dir5.rm();
        dir4.rm();
        dir3.rm();
        dir2.rm();
        dir1.rm();
    }
    
    @Test
    public void testDirectoryTree() throws IOException {
        assertDirectoryTree(
                new TFile(System.getProperty("java.io.tmpdir")), // base directory
                new TFile("dir/inner" + getExtension() + "/dir/outer" + getExtension() + "/" + archive.getName())); // this path is reversed!!!
    }

    private void assertDirectoryTree(TFile basePath, TFile reversePath)
    throws IOException {
        if (reversePath == null) {
            // We're at the leaf of the directory tree.
            final TFile test = new TFile(basePath, "test.txt");
            //testCreateNewFile(basePath, test);
            assertFileOutputStream(test);
            return;
        }
        final TFile member = new TFile(basePath, reversePath.getName());
        final boolean created = member.mkdir();
        final TFile children = reversePath.getParentFile();
        assertDirectoryTree(member, children);
        assertListFiles(basePath, member.getName());
        assertTrue(member.exists());
        assertTrue(member.isDirectory());
        assertFalse(member.isFile());
        if (member.isArchive())
            assertEquals(0, member.length());
        if (created) {
            member.rm();
            assertFalse(member.exists());
            assertFalse(member.isDirectory());
            assertFalse(member.isFile());
            assertEquals(0, member.length());
        }
    }

    private void assertListFiles(final TFile dir, final String entry) {
        final TFile[] files = dir.listFiles();
        boolean found = false;
        for (TFile file : files)
            if (file.getName().equals(entry))
                found = true;
        if (!found)
            fail("No such entry: " + entry);
    }

    @Test
    public void testInputOutput() throws IOException {
        assertInputOutput(archive);
        
        final TFile archiveTest = new TFile(archive, "test");
        assertInputOutput(archiveTest);
        
        final TFile archiveInner = new TFile(archive, "inner" + getExtension());
        final TFile archiveInnerTest = new TFile(archiveInner, "test");
        assertInputOutput(archiveInnerTest);
        archiveInner.rm();
        archive.rm();
    }

    private void assertInputOutput(final TFile file) throws IOException {
        assertInput(file);
        assertOutput(file);
        file.rm();
    }

    private void assertInput(final TFile file) throws IOException {
        try (final InputStream in = new ByteArrayInputStream(getData())) {
            file.input(in);
        }
        assertEquals(getDataLength(), file.length());
    }

    private void assertOutput(final TFile file) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(getDataLength());
        try {
            file.output(out);
        } finally {
            out.close();
        }
        assertArrayEquals(getData(), out.toByteArray());
    }

    @Test
    public void testCopyContainingOrSameFiles() throws IOException {
        assert !archive.exists();
        
        final TFile dir = archive.getParentFile();
        assertNotNull(dir);
        final TFile entry = new TFile(archive, "entry");
        
        assertCopyContainingOrSameFiles0(dir, archive);
        assertCopyContainingOrSameFiles0(archive, entry);
        
        entry.input(new ByteArrayInputStream(getData()));
        
        assertCopyContainingOrSameFiles0(dir, archive);
        assertCopyContainingOrSameFiles0(archive, entry);
        
        TFile.rm_r(archive);
    }
    
    private void assertCopyContainingOrSameFiles0(final TFile a, final TFile b)
    throws IOException {
        assertCopyContainingOrSameFiles1(a, b);
        assertCopyContainingOrSameFiles1(a.getCanOrAbsFile(), b);
        assertCopyContainingOrSameFiles1(a, b.getCanOrAbsFile());
        assertCopyContainingOrSameFiles1(a.getCanOrAbsFile(), b.getCanOrAbsFile());
    }
    
    private void assertCopyContainingOrSameFiles1(final TFile a, final TFile b)
    throws IOException {
        try {
            TFile.cp(a, a);
            fail();
        } catch (IOException expected) {
        }
        try {
            TFile.cp(a, b);
            fail();
        } catch (IOException expected) {
        }
        try {
            TFile.cp(b, a);
            fail();
        } catch (IOException expected) {
        }
        try {
            TFile.cp(b, b);
            fail();
        } catch (IOException expected) {
        }
    }

    @Test
    public void testCopyDelete() throws IOException {
        final String[] names = {
            "0" + getExtension(),
            "1" + getExtension(),
            //"2" + getExtension(),
        };

        assertTrue(archive.mkdir()); // create valid archive file
        assertCopyDelete(archive, names, 0);
        archive.rm();

        assertTrue(archive.toNonArchiveFile().mkdir()); // create false positive archive file
        assertCopyDelete(archive, names, 0);
        archive.rm();
    }

    private void assertCopyDelete(final TFile parent, String[] names, int off)
    throws IOException {
        if (off >= names.length)
            return;
        
        final TFile dir = new TFile(parent, names[off]);

        assertTrue(dir.mkdir()); // create valid archive file
        assertCopyDelete(parent, dir);
        assertCopyDelete(dir, names, off + 1); // continue recursion
        dir.rm();

        assertTrue(dir.toNonArchiveFile().mkdir()); // create false positive archive file
        assertCopyDelete(parent, dir);
        assertCopyDelete(dir, names, off + 1); // continue recursion
        dir.rm();
    }

    private void assertCopyDelete(final TFile parent, final TFile dir)
    throws IOException {
        final TFile parentFile = new TFile(parent, "file");
        final TFile parentArchive = new TFile(parent, "archive" + getExtension());
        final TFile dirFile = new TFile(dir, "file");
        final TFile dirArchive = new TFile(dir, "archive" + getExtension());

        assertCopyDelete0(dirFile, dirArchive);
        assertCopyDelete0(dirFile, parentFile);
        assertCopyDelete0(dirFile, parentArchive);
        assertCopyDelete0(parentFile, dirFile);
        assertCopyDelete0(parentFile, dirArchive);
        assertCopyDelete0(parentArchive, dirFile);
        assertCopyDelete0(parentArchive, dirArchive);
        assertCopyDelete0(dirArchive, dirFile);
        assertCopyDelete0(dirArchive, parentFile);
        assertCopyDelete0(dirArchive, parentArchive);
    }

    private void assertCopyDelete0(TFile a, TFile b) throws IOException {
        // This must be the granularity of the tested file system type PLUS
        // the granularity of the parent file system, e.g. the platform file system!
        // Note that older platform file systems and even ext4 (!) have a granularity
        // of two seconds.
        // Plus the worst case of another two seconds for ZIP files results in
        // four seconds!
        assertCopyDelete0(a, b, 2000 + 2000);
    }

    private void assertCopyDelete0( final TFile a,
                                    final TFile b,
                                    final long granularity)
    throws IOException {
        // Create a file with an old timestamp.
        final long time = System.currentTimeMillis();
        createTestFile(a);
        assertTrue(a.setLastModified(time - granularity));

        // Test copy a to b.
        TFile.cp(a, b);
        assertThat(b.length(), is(a.length()));
        assertThat(b.lastModified(), is(not(a.lastModified())));
        TFile.cp_p(a, b);
        assertThat(b.length(), is(a.length()));
        long almd = a.lastModified() / granularity * granularity;
        long blmd = b.lastModified() / granularity * granularity;
        long almu = (a.lastModified() + granularity - 1) / granularity * granularity;
        long blmu = (b.lastModified() + granularity - 1) / granularity * granularity;
        assertTrue(
                "almd (" + almd + ") != blmd (" + blmd + ") && almu (" + almu + ") != blmu (" + blmu + ")",
                almd == blmd || almu == blmu);

        // Test copy b to a.
        TFile.cp(b, a);
        assertThat(a.length(), is(b.length()));
        assertThat(a.lastModified(), is(not(b.lastModified())));
        TFile.cp_p(b, a);
        assertThat(a.length(), is(b.length()));
        almd = a.lastModified() / granularity * granularity;
        blmd = b.lastModified() / granularity * granularity;
        almu = (a.lastModified() + granularity - 1) / granularity * granularity;
        blmu = (b.lastModified() + granularity - 1) / granularity * granularity;
        assertTrue(
                "almd (" + almd + ") != blmd (" + blmd + ") && almu (" + almu + ") != blmu (" + blmu + ")",
                almd == blmd || almu == blmu);

        verifyTestFile(a);

        // Cleanup.
        a.rm();
        b.rm();
    }

    @Test
    public void testIllegalDeleteOfEntryWithOpenStream()
    throws IOException {
        final TFile entry1 = new TFile(archive, "entry1");
        final TFile entry2 = new TFile(archive, "entry2");
        try (final OutputStream out1 = new TFileOutputStream(entry1)) {
            try {
                entry1.rm();
                fail();
            } catch (IOException expected) {
            }
            out1.write(getData());
            try {
                archive.rm_r();
                fail();
            } catch (IOException expected) {
            }
        }
        try (final OutputStream out2 = new TFileOutputStream(entry2)) {
            try {
                entry2.rm();
                fail();
            } catch (IOException expected) {
            }
            out2.write(getData());
            try {
                archive.rm_r();
                fail();
            } catch (IOException expected) {
            }
        }
        try (final InputStream in1 = new TFileInputStream(entry1)) {
            try (final InputStream in2 = new TFileInputStream(entry2)) {
                entry2.rm();
                final ByteArrayOutputStream out = new ByteArrayOutputStream(getDataLength());
                try {
                    TFile.cat(in2, out);
                } finally {
                    out.close();
                }
                assertTrue(Arrays.equals(getData(), out.toByteArray()));
                try {
                    archive.rm_r();
                    fail();
                } catch (IOException expected) {
                }
            }
            try {
                entry1.rm();
                fail("deleted within archive.rm_r()");
            } catch (IOException expected) {
            }
            final ByteArrayOutputStream out = new ByteArrayOutputStream(getDataLength());
            try {
                TFile.cat(in1, out);
            } finally {
                out.close();
            }
            assertTrue(Arrays.equals(getData(), out.toByteArray()));
            try {
                archive.rm_r();
                fail();
            } catch (IOException expected) {
            }
        }
        archive.rm_r();
        assertFalse(archive.toNonArchiveFile().exists());
    }
    
    @Test
    public void testRenameValidArchive() throws IOException {
        try (final PrintStream out = new PrintStream(new TFileOutputStream(new TFile(archive, "entry")))) {
            out.println("Hello World!");
        }
        assertRenameArchiveToTemp(archive);
    }
    
    @Test
    public void testRenameFalsePositive() throws IOException {
        // Create false positive archive.
        // Note that archive is a TFile instance which returns isArchive()
        // == true, so we must create a new TFile instance which is guaranteed
        // to ignore the archive getExtension() in the path.
        // Furthermore, data is an array containing random data
        // - not a regular archive.
        // So upon completion of this step, the object "archive" refers to a
        // false positive.
        final TFile tmp = archive.toNonArchiveFile();
        final InputStream in = new ByteArrayInputStream(getData());
        TFile.cp(in, tmp);
        assertRenameArchiveToTemp(archive);
    }

    private void assertRenameArchiveToTemp(final TFile archive)
    throws IOException {
        assert archive.isArchive(); // regular archive or false positive
        assert !archive.isEntry(); // not contained in another archive file

        // Create a temporary file.
        TFile tmp = new TFile(TFile.createTempFile(TEMP_FILE_PREFIX, null));
        tmp.rm();
        assertFalse(tmp.exists());
        assertFalse(tmp.toNonArchiveFile().exists());

        // Now rename the archive to the temporary path.
        // Depending on the true state of the object "archive", this will
        // either create a directory (iff archive is a regular archive) or a
        // plain file (iff archive is a false positive).
        archive.mv(tmp);
        assertFalse(archive.exists());
        assertFalse(archive.toNonArchiveFile().exists());

        // Now delete resulting temporary file or directory.
        tmp.rm_r();
        assertFalse(tmp.exists());
        assertFalse(tmp.toNonArchiveFile().exists());
    }

    @Test
    public void testRenameRecursively() throws IOException {
        final TFile temp = new TFile(createTempFile());
        final TFile archive2 = new TFile(archive, "inner" + getExtension());
        final TFile archive3 = new TFile(archive2, "nuts" + getExtension());
        final TFile archive1a = new TFile(archive, "a");
        final TFile archive1b = new TFile(archive, "b");
        final TFile archive2a = new TFile(archive2, "a");
        final TFile archive2b = new TFile(archive2, "b");
        final TFile archive3a = new TFile(archive3, "a");
        final TFile archive3b = new TFile(archive3, "b");
        
        temp.rm();
        
        assertInput(archive1a);
        
        for (int i = 2; i >= 1; i--) {
            assertRenameTo(archive1a, archive1b);
            assertRenameTo(archive1b, archive2a);
            assertRenameTo(archive2a, archive2b);
            assertRenameTo(archive2b, archive3a);
            assertRenameTo(archive3a, archive3b);
            assertRenameTo(archive3b, archive3a);
            assertRenameTo(archive3a, archive2b);
            assertRenameTo(archive2b, archive2a);
            assertRenameTo(archive2a, archive1b);
            assertRenameTo(archive1b, archive1a);
        }
        
        assertRenameTo(archive, temp);
        assertRenameTo(temp, archive);
        archive3.rm();
        archive2.rm();
        verifyTestFile(archive1a);
        archive1a.rm();
        archive.rm();
    }
    
    private void assertRenameTo(TFile src, TFile dst) throws IOException {
        assertTrue(src.exists());
        assertFalse(dst.exists());
        assertFalse(dst.toNonArchiveFile().exists());
        assert TConfig.get().isLenient();
        src.mv(dst);
        assertFalse(src.exists());
        assertFalse(src.toNonArchiveFile().exists());
        assertTrue(dst.exists());
    }

    private static final String[] MEMBERS = {
        "A directory member",
        "Another directory member",
        "Yet another directory member",
    };
    
    @Test
    public void testList() throws IOException {
        final File dir = createTempFile();
        final TFile tdir = new TFile(dir);

        assertNull(dir.listFiles());
        assertNull(tdir.listFiles());
        assertNull(tdir.toNonArchiveFile().listFiles());

        TFile.rm(dir);

        // Create regular directory for testing.
        assertTrue(dir.mkdir());
        for (int i = MEMBERS.length; --i >= 0; )
            assertTrue(new File(dir, MEMBERS[i]).createNewFile());
        final File[] files = dir.listFiles();
        Arrays.sort(files);
        assertList(files, tdir);
        TFile.rm_r(tdir);

        // Repeat test with regular archive file.
        assertTrue(tdir.mkdir());
        for (int i = MEMBERS.length; --i >= 0; )
            assertTrue(new TFile(tdir, MEMBERS[i]).createNewFile());
        assertList(files, tdir);
        TFile.rm_r(tdir);
    }

    private void assertList(final File[] expected, final TFile dir) {
        final TFile[] got = dir.listFiles();
        Arrays.sort(got);
        assertEquals(expected.length, got.length);
        for (int i = 0, l = expected.length; i < l; i++) {
            final File file = expected[i];
            final TFile tfile = got[i];
            assertTrue(!(file instanceof TFile));
            assertEquals(file.getPath(), tfile.getPath());
            assertNull(tfile.list());
            assertNull(tfile.list(null));
            assertNull(tfile.listFiles());
            //assertNull(file.listFiles(file.getDetector()));
            assertNull(tfile.listFiles((FileFilter) null));
            assertNull(tfile.listFiles((FilenameFilter) null));
            //assertNull(file.listFiles((FileFilter) null, file.getDetector()));
            //assertNull(file.listFiles((FilenameFilter) null, file.getDetector()));
        }
    }

    @Test
    public void testMultithreadedSingleArchiveMultipleEntriesReading()
    throws Exception {
        assertMultithreadedSingleArchiveMultipleEntriesReading(NUM_IO_THREADS, NUM_IO_THREADS);
    }

    private void assertMultithreadedSingleArchiveMultipleEntriesReading(
            final int nEntries,
            final int nThreads)
    throws Exception {
        // Create test archive file.
        createTestArchive(nEntries);

        class CheckAllEntries implements Callable<Void> {
            @Override
            public Void call() throws IOException {
                assertArchiveEntries(archive, nEntries);
                return null;
            }
        } // CheckAllEntries

        class CheckAllEntriesFactory implements TaskFactory {
            @Override
            public Callable<?> newTask(int threadNum) {
                return new CheckAllEntries();
            }
        } // CheckAllEntriesFactory

        try {
            start(nThreads, new CheckAllEntriesFactory()).join();
        } finally {
            TFile.rm_r(archive);
        }
    }
    
    private void createTestArchive(final int nEntries) throws IOException {
        for (int i = 0; i < nEntries; i++)
            createTestFile(new TFile(archive, i + ""));
    }

    private void assertArchiveEntries(final TFile archive, int nEntries)
    throws IOException {
        // Retrieve list of entries and shuffle their order.
        final List<TFile> entries = Arrays.asList(archive.listFiles());
        assert entries.size() == nEntries; // this would be a programming error in the test class itself - not the class under test!
        Collections.shuffle(entries, new Random());

        // Now read in the entries in the shuffled order.
        final byte[] buf = new byte[getDataLength()];
        for (final TFile entry : entries) {
            try (final InputStream in = new TFileInputStream(entry)) {
                int off = 0;
                int read;
                while (true) {
                    read = in.read(buf);
                    if (0 > read)
                        break;
                    assertTrue(read > 0);
                    assertEquals(   ByteBuffer.wrap(getData(), off, read),
                                    ByteBuffer.wrap(buf, 0, read));
                    off += read;
                }
                assertEquals(-1, read);
                assertEquals(off, getDataLength());
                assertTrue(0 >= in.read(new byte[0]));
            }
        }
    }
    
    @Test
    public void testMultithreadedSingleArchiveMultipleEntriesWriting()
    throws Exception {
        assertMultithreadedSingleArchiveMultipleEntriesWriting(false);
        assertMultithreadedSingleArchiveMultipleEntriesWriting(true);
    }
    
    private void assertMultithreadedSingleArchiveMultipleEntriesWriting(
            final boolean wait)
    throws Exception {
        assert TConfig.get().isLenient();

        class WriteFactory implements TaskFactory {
            @Override
            public Callable<?> newTask(final int threadNum) {
                class Write implements Callable<Void> {
                    @Override
                    public Void call() throws IOException {
                        final TFile entry = new TFile(archive, "" + threadNum);
                        createTestFile(entry);
                        try {
                            TVFS.sync(archive,
                                    BitField.of(CLEAR_CACHE)
                                            .set(WAIT_CLOSE_IO, wait));
                        } catch (final FsSyncException ex) {
                            if (!(ex.getCause() instanceof FsResourceOpenException))
                                throw ex;
                            // Some other thread is busy updating an archive.
                            // If we are waiting, then this could never happen.
                            // Otherwise, silently ignore this exception and
                            // accept that the archive may not have been
                            // updated to disk.
                            // Note that no data is lost, this exception just
                            // signals that the corresponding archive hasn't
                            // been updated - a future call may still succeed.
                            if (wait)
                                throw new AssertionError(ex);
                        }
                        return null;
                    }
                } // Write

                return new Write();
            }
        } // WriteFactory

        try {
            start(NUM_IO_THREADS, new WriteFactory()).join();
        } finally {
            assertArchiveEntries(archive, NUM_IO_THREADS);
            TFile.rm_r(archive);
        }
    }

    @Test
    public void testMultithreadedMultipleArchivesSingleEntryWriting()
    throws Exception {
        assertMultithreadedMultipleArchivesSingleEntryWriting(false);
        assertMultithreadedMultipleArchivesSingleEntryWriting(true);
    }
    
    private void assertMultithreadedMultipleArchivesSingleEntryWriting(
            final boolean syncIndividually)
    throws Exception {
        assert TConfig.get().isLenient();

        class Write implements Callable<Void> {
            @Override
            public Void call() throws IOException {
                final TFile archive = new TFile(createTempFile());
                archive.rm();
                final TFile entry = new TFile(archive, "entry");
                try {
                    createTestFile(entry);
                    try {
                        if (syncIndividually)
                            TVFS.umount(archive);
                        else
                            TVFS.sync(SYNC); // DON'T clear cache!
                    } catch (final FsSyncWarningException ex) {
                        if (!(ex.getCause() instanceof FsResourceOpenException))
                            throw ex;
                        // Some other thread is busy updating an archive.
                        // If we are updating individually, then this
                        // could never happen.
                        // Otherwise, silently ignore this exception and
                        // accept that the archive may not have been
                        // synced to disk.
                        // Note that no data is lost, this exception just
                        // signals that the corresponding archive hasn't
                        // been updated - a future call may still succeed.
                        if (syncIndividually)
                            throw new AssertionError(ex);
                    }
                } finally {
                    TFile.rm_r(archive);
                }
                return null;
            }
        } // Write

        class WriteFactory implements TaskFactory {
            @Override
            public Callable<?> newTask(int threadNum) {
                return new Write();
            }
        } // WriteFactory

        start(NUM_IO_THREADS, new WriteFactory()).join();
    }

    /** Test for http://java.net/jira/browse/TRUEZIP-192 . */
    @Test
    public void testMultithreadedMutualArchiveCopying() throws Exception {
        assert TConfig.get().isLenient();

        class CopyFactory implements TaskFactory {
            final TFile src, dst;

            CopyFactory(final TFile src, final TFile dst) {
                this.src = src;
                this.dst = dst;
            }

            @Override
            public Callable<?> newTask(final int threadNum) {
                class Copy implements Callable<Void> {
                    @Override
                    public Void call() throws IOException {
                        final TFile srcNo = new TFile(src, "src/" + threadNum);
                        final TFile dstNo = new TFile(dst, "dst/" + threadNum);
                        createTestFile(srcNo);
                        srcNo.cp(dstNo);
                        return null;
                    }
                } // Copy

                return new Copy();
            }
        } // CopyFactory

        final TFile src = archive;
        try {
            final TFile dst = new TFile(createTempFile());
            dst.rm();
            try {
                try {
                    final TaskJoiner join = start(NUM_IO_THREADS,
                            new CopyFactory(src, dst));
                    try {
                        start(NUM_IO_THREADS,
                                new CopyFactory(dst, src)).join();
                    } finally {
                        join.join();
                    }
                } finally {
                    TVFS.umount(dst);
                }
            } finally {
                dst.toNonArchiveFile().rm();
            }
        } finally {
            TVFS.umount(src);
        }
        // src alias archive gets deleted by the test fixture.
    }

    @Test
    public void testGrowing() throws IOException {
        final TFile file = archive.toNonArchiveFile();
        final TFile entry1 = new TFile(archive, "entry1");
        final TFile entry2 = new TFile(archive, "entry2");

        try (final TConfig config = TConfig.open()) {
            config.setPreference(GROW, true);

            createTestFile(entry1);
            createTestFile(entry2);

            umount();
            assertTrue(file.length() > 2 * getDataLength()); // two entries plus one central directory

            createTestFile(entry1);
            createTestFile(entry2);
            createTestFile(entry1);
            createTestFile(entry2);

            assertTrue(entry1.setLastModified(System.currentTimeMillis()));
            assertTrue(entry2.setLastModified(System.currentTimeMillis()));

            // See http://java.net/jira/browse/TRUEZIP-144 .
            entry1.rm();
            entry2.rm();

            umount();
            assertTrue(file.length() > 6 * getDataLength()); // six entries plus two central directories
        }

        assertThat(archive.list().length, is(0));

        try (final TConfig config = TConfig.open()) {
            config.setPreference(GROW, true);

            archive.rm();
            umount();
        }

        assertNull(archive.list());
    }
}
