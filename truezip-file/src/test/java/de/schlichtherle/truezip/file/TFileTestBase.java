/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.file;

import de.schlichtherle.truezip.fs.FsController;
import static de.schlichtherle.truezip.fs.FsOutputOption.GROW;
import de.schlichtherle.truezip.fs.FsSyncException;
import static de.schlichtherle.truezip.fs.FsSyncOptions.SYNC;
import de.schlichtherle.truezip.fs.FsSyncWarningException;
import de.schlichtherle.truezip.fs.archive.FsArchiveDriver;
import de.schlichtherle.truezip.io.FileBusyException;
import de.schlichtherle.truezip.io.OutputClosedException;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import de.schlichtherle.truezip.socket.spi.ByteArrayIOPoolService;
import de.schlichtherle.truezip.util.ArrayHelper;
import static de.schlichtherle.truezip.util.ConcurrencyUtils.NUM_IO_THREADS;
import de.schlichtherle.truezip.util.ConcurrencyUtils.TaskFactory;
import de.schlichtherle.truezip.util.ConcurrencyUtils.TaskJoiner;
import static de.schlichtherle.truezip.util.ConcurrencyUtils.runConcurrent;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import static java.io.File.separatorChar;
import java.io.*;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Performs integration testing of a particular {@link FsArchiveDriver}
 * by using the API of the TrueZIP File* module.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public abstract class TFileTestBase<D extends FsArchiveDriver<?>>
extends TestBase<D> {

    private static final Logger
            logger = Logger.getLogger(TFileTestBase.class.getName());

    /**
     * The prefix for temporary files, which is {@value}.
     * This value should identify the TrueZIP File* module in order to
     * ensure that no two temporary files are shared between tests of the
     * TrueZIP Path API and the TrueZIP File* API.
     */
    private static final String TEMP_FILE_PREFIX = "tzp-file";

    /** The data to get compressed. */
    private static final byte[] DATA = new byte[1024]; // enough to waste some heat on CPU cycles
    static {
        new Random().nextBytes(DATA);
    }

    protected static final IOPoolProvider
            IO_POOL_PROVIDER = new ByteArrayIOPoolService(4 * DATA.length / 3); // account for archive file type specific overhead

    private File temp;
    private TFile archive;
    private byte[] data;

    @Override
    public void setUp() throws IOException {
        super.setUp();
        temp = createTempFile();
        TFile.rm(temp);
        archive = new TFile(temp);
        data = DATA.clone();
    }

    private File createTempFile() throws IOException {
        // TODO: Removing .getCanonicalFile() causes archive.rm_r() to
        // fail in testCopyContainingOrSameFiles() - explain why!
        return File.createTempFile(TEMP_FILE_PREFIX, getSuffix()).getCanonicalFile();
    }

    @Override
    public void tearDown() throws IOException {
        try {
            try {
                umount();
            } finally {
                archive = null;
                if (temp.exists() && !temp.delete())
                    throw new IOException(temp + " (could not delete)");
            }
        } finally {
            super.tearDown();
        }
    }

    private void umount() throws FsSyncException {
        if (null != archive)
            TFile.umount(archive);
    }

    private void createTestFile(final TFile file) throws IOException {
        final OutputStream out = new TFileOutputStream(file);
        try {
            out.write(data);
        } finally {
            out.close();
        }
    }

    protected final TFile getArchive() {
        return archive;
    }

    protected static TFile newNonArchiveFile(TFile file) {
        return file.getNonArchiveFile();
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
        final String entry = archive.getPath() + "/entry";
        archive = null;
        assertTrue(new TFile(entry).createNewFile());
        TFile.umount(new TFile(entry).getTopLevelArchive());
        Closeable resource = factory.create(entry);
        final ReferenceQueue<FsController<?>> queue
                = new ReferenceQueue<FsController<?>>();
        final Reference<FsController<?>> exp
                = new WeakReference<FsController<?>>(
                    new TFile(entry).getInnerArchive().getController(), queue);
        System.gc();
        assertNull(queue.remove(TIMEOUT_MILLIS));
        assertSame(exp.get(), new TFile(entry).getInnerArchive().getController());
        resource.close();
        resource = null; // leave now!
        System.gc();
        assertNull(queue.remove(TIMEOUT_MILLIS));
        assertSame(exp.get(), new TFile(entry).getInnerArchive().getController());
        TFile.umount(new TFile(entry).getTopLevelArchive());
        Reference<? extends FsController<?>> ref;
        do {
            System.gc(); // triggering GC in a loop seems to help with concurrency!
        } while (null == (ref = queue.remove(TIMEOUT_MILLIS)));
        assert exp == ref;
        assert null == exp.get();
    }

    @Test
    public final void testFalsePositives() throws IOException {
        assertFalsePositive(archive);

        // Dito for entry.
        final TFile entry = new TFile(archive, "entry" + getSuffix());

        assertTrue(archive.mkdir());
        assertFalsePositive(entry);
        archive.rm();

        assertTrue(newNonArchiveFile(archive).mkdir());
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
        assertEquals(data.length, file.length());
        assertTrue(file.lastModified() > 0);

        // Read back portion
        {
            InputStream in = new TFileInputStream(file);
            try {
                byte[] buf = new byte[data.length];
                assertTrue(ArrayHelper.equals(data, 0, buf, 0, in.read(buf)));
            } finally {
                in.close();
            }
        }
        assertRm(file);

        // Create directory false positive.

        assertTrue(newNonArchiveFile(file).mkdir());
        assertTrue(file.exists());
        assertTrue(file.isDirectory());
        assertFalse(file.isFile());
        //assertEquals(0, file.getLength());
        assertTrue(file.lastModified() > 0);

        try {
            new TFileInputStream(archive).close();
            if ('\\' == separatorChar)
                fail();
        } catch (FileNotFoundException ex) {
            if ('\\' != separatorChar && !archive.isArchive() && !archive.isEntry())
                throw ex;
        }

        try {
            new TFileOutputStream(archive).close();
            fail();
        } catch (FileNotFoundException expected) {
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
        } catch (FileNotFoundException ex) {
            if ('\\' != separatorChar && !archive.isArchive())
                throw ex;
        }

        try {
            new TFileOutputStream(archive).close();
            fail();
        } catch (FileNotFoundException expected) {
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
    public final void testCreateNewFile() throws IOException{
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
        TConfig config = TConfig.push();
        try {
            config.setLenient(false);
            try {
                file1.createNewFile();
                fail("Creating a file in a non-existent directory should throw an IOException!");
            } catch (IOException expected) {
            }
            assertCreateNewFile(archive, file1, file2);
        } finally {
            config.close();
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
    public final void testIllegalDirectoryOperations() throws IOException {
        try {
            final String[] names = {
                "inner" + getSuffix(),
                "dir",
            };
            TFile file = archive;
            for (int i = 0; i <= names.length; i++) {
                final TFile file2 = newNonArchiveFile(file);
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
        } catch (FileNotFoundException ex) {
            if ('\\' != separatorChar && !dir.isArchive() && !dir.isEntry())
                throw ex;
        }
        try {
            new TFileOutputStream(dir).close();
            fail();
        } catch (FileNotFoundException expected) {
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
    public final void testStrictFileOutputStream() throws IOException {
        TFile file = new TFile(archive, "test.txt");

        TConfig config = TConfig.push();
        try {
            config.setLenient(false);
            try {
                assertFileOutputStream(file);
                fail("Creating ghost directories should not be allowed when File.isLenient() is false!");
            } catch (FileNotFoundException expected) {
            }
            assertTrue(archive.mkdir());
            assertFileOutputStream(file);
            archive.rm();
        } finally {
            config.close();
        }
    }
    
    @Test
    public final void testLenientFileOutputStream() throws IOException {
        TFile file = new TFile(archive, "dir/inner" + getSuffix() + "/dir/test.txt");

        assertFileOutputStream(file);

        try {
            archive.rm();
            fail("directory not empty");
        } catch (IOException expected) {
        }
        umount(); // allow external modifications!
        TFile.rm(newNonArchiveFile(archive)); // use plain file to delete instead!
        assertFalse(archive.exists());
        assertFalse(archive.isDirectory());
        assertFalse(archive.isFile());
        assertEquals(0, archive.length());
    }

    private void assertFileOutputStream(final TFile file) throws IOException {
        final byte[] message = "Hello World!\r\n".getBytes();
        
        final OutputStream out = new TFileOutputStream(file);
        try {
            assertTrue(file.exists());
            assertFalse(file.isDirectory());
            assertTrue(file.isFile());
            assertEquals(0, file.length());
            out.write(message);
            assertEquals(0, file.length());
            out.flush();
            assertEquals(0, file.length());
        } finally {
            out.close();
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
    public final void testBusyFileInputStream()
    throws IOException, InterruptedException {
        final TFile file1 = new TFile(archive, "file1");
        final TFile file2 = new TFile(archive, "file2");

        // Test open output streams.
        assertTrue(file1.createNewFile());
        umount(); // ensure file1 is really present in the archive file
        assertTrue(file2.createNewFile());
        final InputStream in1 = new TFileInputStream(file1);
        try {
            try {
                new TFileInputStream(file2).close();
                fail("Expected exception when reading an uncommitted entry of a busy archive file!");
            } catch (FileNotFoundException ex) {
                if (!(ex.getCause() instanceof FsSyncException)
                        || !(ex.getCause().getCause() instanceof FileBusyException))
                    throw ex;
            }
            file2.input(in1);

            // in1 is still open!
            try {
                umount(); // forces closing of in1
                fail("Expected warning exception when synchronizing a busy archive file!");
            } catch (FsSyncWarningException ex) {
                if (!(ex.getCause() instanceof FileBusyException))
                    throw ex;
            }
            assertTrue(file2.isFile());
            try {
                file2.input(in1);
                fail("Expected exception when reading from entry input stream of an unmounted archive file!");
            } catch (IOException expected) {
                assertFalse(file2.exists()); // previous op has removed file2!
            }

            // Open file1 as stream and let the garbage collection join the stream automatically.
            new TFileInputStream(file1);

            try {
                // This operation may succeed without any exception if
                // the garbage collector did its job.
                umount(); // allow external modifications!
            } catch (FsSyncWarningException ex) {
                // It may fail once if a stream was busy!
                if (!(ex.getCause() instanceof FileBusyException))
                    throw ex;
            }
            umount(); // It must not fail twice for the same reason!

            TFile.rm(newNonArchiveFile(archive));
        } finally {
            // Closing the invalidated stream explicitly should be OK.
            in1.close();
        }

        // Cleanup.
        try {
            file2.rm();
            fail("already deleted externally");
        } catch (IOException expected) {
        }
        assertFalse(file2.exists());
        try {
            file1.rm();
            fail("already deleted externally");
        } catch (IOException expected) {
        }
        assertFalse(file1.exists());
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OS_OPEN_STREAM")
    @Test
    public final void testBusyFileOutputStream()
    throws IOException, InterruptedException {
        TFile file1 = new TFile(archive, "file1");
        TFile file2 = new TFile(archive, "file2");
        
        // Ensure that there are two entries in the archive.
        // This is used later to check whether the update operation knows
        // how to deal with updating an archive for which there is still
        // an open output stream.
        OutputStream out = new TFileOutputStream(file1);
        try {
            TFile.cat(new ByteArrayInputStream(data), out);
        } finally {
            out.close();
        }
        
        out = new TFileOutputStream(file2);
        try {
            TFile.cat(new ByteArrayInputStream(data), out);
        } finally {
            out.close();
        }
        
        umount(); // ensure two entries in the archive
        
        out = new TFileOutputStream(file1);
        TFile.cat(new ByteArrayInputStream(data), out);
        
        // out is still open!
        try {
            new TFileOutputStream(file1).close();
            fail("Expected synchronization exception when overwriting an unsynchronized entry of a busy archive file!");
        } catch (FileNotFoundException ex) {
            if (!(ex.getCause() instanceof FsSyncException)
                    || !(ex.getCause().getCause() instanceof FileBusyException))
                    throw ex;
        }

        // out is still open!
        try {
            new TFileOutputStream(file2).close();
        } catch (FileNotFoundException ex) {
            if (!(ex.getCause() instanceof FsSyncException)
                    || !(ex.getCause().getCause() instanceof FileBusyException))
                    throw ex;
            logger.warning("This archive driver does NOT support concurrent writing of different entries in the same archive file.");
        }

        // out is still open!
        TFile.cat(new ByteArrayInputStream(data), out); // write again
        
        // out is still open!
        try {
            umount(); // forces closing of all streams
            fail("Expected warning exception when synchronizing a busy archive file!");
        } catch (FsSyncWarningException ex) {
            if (!(ex.getCause() instanceof FileBusyException))
                throw ex;
        }
        
        try {
            TFile.cat(new ByteArrayInputStream(data), out); // write again
            fail("Expected exception when writing to entry output stream of an unmounted archive file!");
        } catch (OutputClosedException expected) {
        }
        
        // The stream has been forcibly closed by TFile.update().
        // Another join is OK, though!
        out.close();
        
        // Reopen stream and let the garbage collection join the stream automatically.
        new TFileOutputStream(file1);
        out = null;
        
        try {
            // This operation may succeed without any exception if
            // the garbage collector did its job.
            umount(); // allow external modifications!
        } catch (FsSyncWarningException ex) {
            // It may fail once if a stream was busy!
            if (!(ex.getCause() instanceof FileBusyException))
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
    public final void testMkdir() throws IOException {
        final TFile dir1 = archive;
        final TFile dir2 = new TFile(dir1, "dir");
        final TFile dir3 = new TFile(dir2, "inner" + getSuffix());
        final TFile dir4 = new TFile(dir3, "dir");
        final TFile dir5 = new TFile(dir4, "nuts" + getSuffix());
        final TFile dir6 = new TFile(dir5, "dir");
        
        assert TFile.isLenient();
        
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

        final TConfig config = TConfig.push();
        try {
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
        } finally {
            config.close();
        }

        dir6.rm();
        dir5.rm();
        dir4.rm();
        dir3.rm();
        dir2.rm();
        dir1.rm();
    }
    
    @Test
    public final void testDirectoryTree() throws IOException {
        assertDirectoryTree(
                new TFile(System.getProperty("java.io.tmpdir")), // base directory
                new TFile("dir/inner" + getSuffix() + "/dir/outer" + getSuffix() + "/" + archive.getName())); // this path is reversed!!!
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
    public final void testInputOutput() throws IOException {
        assertInputOutput(archive);
        
        final TFile archiveTest = new TFile(archive, "test");
        assertInputOutput(archiveTest);
        
        final TFile archiveInner = new TFile(archive, "inner" + getSuffix());
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
        final InputStream in = new ByteArrayInputStream(data);
        try {
            file.input(in);
        } finally {
            in.close();
        }
        assertEquals(data.length, file.length());
    }

    private void assertOutput(final TFile file) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
        try {
            file.output(out);
        } finally {
            out.close();
        }
        assertTrue(Arrays.equals(data, out.toByteArray()));
    }

    @Test
    public final void testCopyContainingOrSameFiles() throws IOException {
        assert !archive.exists();
        
        final TFile dir = archive.getParentFile();
        assertNotNull(dir);
        final TFile entry = new TFile(archive, "entry");
        
        assertCopyContainingOrSameFiles0(dir, archive);
        assertCopyContainingOrSameFiles0(archive, entry);
        
        entry.input(new ByteArrayInputStream(data));
        
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
    public final void testCopyDelete() throws IOException {
        final String[] names = {
            "0" + getSuffix(),
            "1" + getSuffix(),
            //"2" + getSuffix(),
        };

        assertTrue(archive.mkdir()); // create valid archive file
        assertCopyDelete(archive, names, 0);
        archive.rm();

        assertTrue(newNonArchiveFile(archive).mkdir()); // create false positive archive file
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

        assertTrue(newNonArchiveFile(dir).mkdir()); // create false positive archive file
        assertCopyDelete(parent, dir);
        assertCopyDelete(dir, names, off + 1); // continue recursion
        dir.rm();
    }

    private void assertCopyDelete(final TFile parent, final TFile dir)
    throws IOException {
        final TFile parentFile = new TFile(parent, "file");
        final TFile parentArchive = new TFile(parent, "archive" + getSuffix());
        final TFile dirFile = new TFile(dir, "file");
        final TFile dirArchive = new TFile(dir, "archive" + getSuffix());

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
        assertThat(b.lastModified(), not(is(a.lastModified())));
        TFile.cp_p(a, b);
        assertThat(b.length(), is(a.length()));
        long almd = a.lastModified() / granularity * granularity;
        long blmd = b.lastModified() / granularity * granularity;
        long almu = (a.lastModified() + granularity - 1) / granularity * granularity;
        long blmu = (b.lastModified() + granularity - 1) / granularity * granularity;
        assertTrue("almd (" + almd + ") != blmd (" + blmd + ") && almu (" + almu + ") != blmu (" + blmu + ")", almd == blmd || almu == blmu);

        // Test copy b to a.
        TFile.cp(b, a);
        assertThat(a.length(), is(b.length()));
        assertThat(a.lastModified(), not(is(b.lastModified())));
        TFile.cp_p(b, a);
        assertThat(a.length(), is(b.length()));
        almd = a.lastModified() / granularity * granularity;
        blmd = b.lastModified() / granularity * granularity;
        almu = (a.lastModified() + granularity - 1) / granularity * granularity;
        blmu = (b.lastModified() + granularity - 1) / granularity * granularity;
        assertTrue("almd (" + almd + ") != blmd (" + blmd + ") && almu (" + almu + ") != blmu (" + blmu + ")", almd == blmd || almu == blmu);

        // Check result.
        {
            final ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
            TFile.cp(a, out);
            assertTrue(Arrays.equals(data, out.toByteArray()));
        }

        // Cleanup.
        a.rm();
        b.rm();
    }

    @Test
    public final void testListPerformance() throws IOException {
        assertTrue(archive.mkdir());

        int i, j;
        long time;

        time = System.currentTimeMillis();
        for (i = 0; i < 100; i++) {
            TFile file = new TFile(archive, "" + i);
            assertTrue(file.createNewFile());
        }
        time = System.currentTimeMillis() - time;
        logger.log(Level.FINER, "Time required to create {0} archive file entries: {1}ms", new Object[]{ i, time });

        time = System.currentTimeMillis();
        for (j = 0; j < 100; j++)
            archive.listFiles((FilenameFilter) null);
        time = System.currentTimeMillis() - time;
        logger.log(Level.FINER, "Time required to list these entries {0} times using a nullary FilenameFilter: {1}ms", new Object[]{ j, time });

        time = System.currentTimeMillis();
        for (j = 0; j < 100; j++)
            archive.listFiles((FileFilter) null);
        time = System.currentTimeMillis() - time;
        logger.log(Level.FINER, "Time required to list these entries {0} times using a nullary FileFilter: {1}ms", new Object[]{ j, time });

        try {
            archive.rm();
            fail("directory not empty");
        } catch (IOException expected) {
        }
        umount(); // allow external modifications!
        TFile.rm(new File(archive.getPath())); // use plain file to delete instead!
        assertFalse(archive.exists());
        assertFalse(archive.isDirectory());
        assertFalse(archive.isFile());
        assertEquals(0, archive.length());
    }

    @Test
    public final void testIllegalDeleteEntryWithOpenStream()
    throws IOException {
        final TFile entry1 = new TFile(archive, "entry1");
        final TFile entry2 = new TFile(archive, "entry2");
        final OutputStream out1 = new TFileOutputStream(entry1);
        try {
            try {
                entry1.rm();
                fail();
            } catch (IOException expected) {
            }
            out1.write(data);
            try {
                archive.rm_r();
                fail();
            } catch (IOException expected) {
            }
        } finally {
            out1.close();
        }
        final OutputStream out2 = new TFileOutputStream(entry2);
        try {
            try {
                entry2.rm();
                fail();
            } catch (IOException expected) {
            }
            out2.write(data);
            try {
                archive.rm_r();
                fail();
            } catch (IOException expected) {
            }
        } finally {
            out2.close();
        }
        final InputStream in1 = new TFileInputStream(entry1); // does an auto update!
        try {
            final InputStream in2 = new TFileInputStream(entry2);
            try {
                entry2.rm();
                final ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
                try {
                    TFile.cat(in2, out);
                } finally {
                    out.close();
                }
                assertTrue(Arrays.equals(data, out.toByteArray()));
                try {
                    archive.rm_r();
                    fail();
                } catch (IOException expected) {
                }
            } finally {
                in2.close();
            }
            try {
                entry1.rm();
                fail("deleted within archive.rm_r()");
            } catch (IOException expected) {
            }
            final ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
            try {
                TFile.cat(in1, out);
            } finally {
                out.close();
            }
            assertTrue(Arrays.equals(data, out.toByteArray()));
            try {
                archive.rm_r();
                fail();
            } catch (IOException expected) {
            }
        } finally {
            in1.close();
        }
        archive.rm_r();
        assertFalse(newNonArchiveFile(archive).exists());
    }
    
    @Test
    public final void testRenameValidArchive() throws IOException {
        // Create a regular archive with a single archive entry which
        // contains a creative greeting message.
        PrintStream out = new PrintStream(
                new TFileOutputStream(new TFile(archive, "entry")));
        try {
            out.println("Hello World!");
        } finally {
            out.close(); // ALWAYS join streams!
        }
        assertRenameArchiveToTemp(archive);
    }
    
    @Test
    public final void testRenameFalsePositive() throws IOException {
        // Create false positive archive.
        // Note that archive is a TFile instance which returns isArchive()
        // == true, so we must create a new TFile instance which is guaranteed
        // to ignore the archive getSuffix() in the path.
        // Furthermore, data is an array containing random data
        // - not a regular archive.
        // So upon completion of this step, the object "archive" refers to a
        // false positive.
        final TFile tmp = newNonArchiveFile(archive);
        final InputStream in = new ByteArrayInputStream(data);
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
        assertFalse(newNonArchiveFile(tmp).exists());

        // Now rename the archive to the temporary path.
        // Depending on the true state of the object "archive", this will
        // either create a directory (iff archive is a regular archive) or a
        // plain file (iff archive is a false positive).
        archive.mv(tmp);
        assertFalse(archive.exists());
        assertFalse(newNonArchiveFile(archive).exists());

        // Now delete resulting temporary file or directory.
        tmp.rm_r();
        assertFalse(tmp.exists());
        assertFalse(newNonArchiveFile(tmp).exists());
    }

    @Test
    public final void testRenameRecursively() throws IOException {
        final TFile temp = new TFile(createTempFile());
        final TFile archive2 = new TFile(archive, "inner" + getSuffix());
        final TFile archive3 = new TFile(archive2, "nuts" + getSuffix());
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
        assertOutput(archive1a);
        archive1a.rm();
        archive.rm();
    }
    
    private void assertRenameTo(TFile src, TFile dst) throws IOException {
        assertTrue(src.exists());
        assertFalse(dst.exists());
        assertFalse(newNonArchiveFile(dst).exists());
        assert TFile.isLenient();
        src.mv(dst);
        assertFalse(src.exists());
        assertFalse(newNonArchiveFile(src).exists());
        assertTrue(dst.exists());
    }

    private static final String[] MEMBERS = {
        "A directory member",
        "Another directory member",
        "Yet another directory member",
    };
    
    @Test
    public final void testList() throws IOException {
        final File dir = createTempFile();
        final TFile dir2 = new TFile(dir);

        assertNull(dir.listFiles());
        assertNull(dir2.listFiles());
        assertNull(newNonArchiveFile(dir2).listFiles());

        TFile.rm(dir);

        // Create regular directory for testing.
        assertTrue(dir.mkdir());
        for (int i = MEMBERS.length; --i >= 0; )
            assertTrue(new File(dir, MEMBERS[i]).createNewFile());
        File[] files = dir.listFiles();
        Arrays.sort(files);
        assertList(files, dir2);
        TFile.rm_r(dir2);

        // Repeat test with regular archive file.
        assertTrue(dir2.mkdir());
        for (int i = MEMBERS.length; --i >= 0; )
            assertTrue(new TFile(dir2, MEMBERS[i]).createNewFile());
        assertList(files, dir2);
        TFile.rm_r(dir2);
    }

    private void assertList(final File[] refs, final TFile dir) {
        final TFile[] files = dir.listFiles();
        Arrays.sort(files);
        assertEquals(refs.length, files.length);
        for (int i = 0, l = refs.length; i < l; i++) {
            final File ref = refs[i];
            final TFile file = files[i];
            assertTrue(!(ref instanceof TFile));
            assertEquals(ref.getPath(), file.getPath());
            assertNull(file.list());
            assertNull(file.list(null));
            assertNull(file.listFiles());
            assertNull(file.listFiles(file.getArchiveDetector()));
            assertNull(file.listFiles((FileFilter) null));
            assertNull(file.listFiles((FilenameFilter) null));
            assertNull(file.listFiles((FileFilter) null, file.getArchiveDetector()));
            assertNull(file.listFiles((FilenameFilter) null, file.getArchiveDetector()));
        }
    }
    
    @Test
    public final void testMultithreadedSingleArchiveMultipleEntriesReading()
    throws Exception {
        assertMultithreadedSingleArchiveMultipleEntriesReading(NUM_IO_THREADS, NUM_IO_THREADS);
    }

    private void assertMultithreadedSingleArchiveMultipleEntriesReading(
            final int nEntries,
            final int nThreads)
    throws Exception {
        // Create test archive file.
        createTestArchive(nEntries);
        
        class CheckAllEntriesTask implements Callable<Void> {
            @Override
            public Void call() throws IOException {
                assertArchiveEntries(archive, nEntries);
                return null;
            }
        } // CheckAllEntriesTask

        class CheckAllEntriesTaskFactory implements TaskFactory {
            @Override
            public Callable<Void> newTask(int threadNum) {
                return new CheckAllEntriesTask();
            }
        } // CheckAllEntriesTaskFactory

        try {
            runConcurrent(nThreads, new CheckAllEntriesTaskFactory()).join();
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
        final byte[] buf = new byte[data.length];
        for (final TFile entry : entries) {
            // Read full entry and check the contents.
            final InputStream in = new TFileInputStream(entry);
            try {
                int off = 0;
                int read;
                while (true) {
                    read = in.read(buf);
                    if (0 > read)
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
    }
    
    @Test
    public final void testMultithreadedSingleArchiveMultipleEntriesWriting()
    throws Exception {
        assertMultithreadedSingleArchiveMultipleEntriesWriting(false);
        assertMultithreadedSingleArchiveMultipleEntriesWriting(true);
    }
    
    private void assertMultithreadedSingleArchiveMultipleEntriesWriting(
            final boolean wait)
    throws Exception {
        assertTrue(TFile.isLenient());

        class WritingTask implements Callable<Void> {
            final TFile entry;

            WritingTask(final int no) {
                this.entry = new TFile(archive, no + "");
            }

            @Override
            public Void call() throws IOException {
                createTestFile(entry);
                try {
                    TFile.umount(archive, wait, false, wait, false);
                } catch (FsSyncException ex) {
                    if (!(ex.getCause() instanceof FileBusyException))
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
        } // WritingTask

        class WritingTaskFactory implements TaskFactory {
            @Override
            public Callable<Void> newTask(int no) {
                return new WritingTask(no);
            }
        } // WritingTaskFactory

        try {
            runConcurrent(NUM_IO_THREADS, new WritingTaskFactory()).join();
        } finally {
            assertArchiveEntries(archive, NUM_IO_THREADS);
            TFile.rm_r(archive);
        }
    }

    @Test
    public final void testMultithreadedMultipleArchivesSingleEntryWriting()
    throws Exception {
        assertMultithreadedMultipleArchivesSingleEntryWriting(false);
        assertMultithreadedMultipleArchivesSingleEntryWriting(true);
    }
    
    private void assertMultithreadedMultipleArchivesSingleEntryWriting(
            final boolean updateIndividually)
    throws Exception {
        assertTrue(TFile.isLenient());

        class WritingTask implements Callable<Void> {
            @Override
            public Void call() throws IOException {
                final TFile archive = new TFile(createTempFile());
                archive.rm();
                final TFile entry = new TFile(archive, "entry");
                try {
                    createTestFile(entry);
                    try {
                        if (updateIndividually)
                            TFile.umount(archive);
                        else
                            TFile.sync(SYNC); // DON'T flush all caches!
                    } catch (FsSyncException ex) {
                        if (!(ex.getCause() instanceof FileBusyException))
                            throw ex;
                        // Some other thread is busy updating an archive.
                        // If we are updating individually, then this
                        // could never happen.
                        // Otherwise, silently ignore this exception and
                        // accept that the archive may not have been
                        // updated to disk.
                        // Note that no data is lost, this exception just
                        // signals that the corresponding archive hasn't
                        // been updated - a future call may still succeed.
                        if (updateIndividually)
                            throw new AssertionError(ex);
                    }
                } finally {
                    TFile.rm_r(archive);
                }
                return null;
            }
        } // WritingTask

        class WritingTaskFactory implements TaskFactory {
            @Override
            public Callable<Void> newTask(int no) {
                return new WritingTask();
            }
        } // WritingTaskFactory

        runConcurrent(NUM_IO_THREADS, new WritingTaskFactory()).join();
    }

    /**
     * Test for http://java.net/jira/browse/TRUEZIP-192 .
     */
    @Test
    public void testMultithreadedMutualArchiveCopying() throws Exception {
        assertTrue(TFile.isLenient());

        class CopyingTask implements Callable<Void> {
            final TFile src, dst;

            CopyingTask(final TFile src, final TFile dst, final int no) {
                this.src = new TFile(src, "src/" + no);
                this.dst = new TFile(dst, "dst/" + no);
            }

            @Override
            public Void call() throws IOException {
                createTestFile(src);
                src.cp(dst);
                return null;
            }
        } // CopyingTask

        class CopyingTaskFactory implements TaskFactory {
            final TFile src, dst;

            CopyingTaskFactory(final TFile src, final TFile dst) {
                this.src = src;
                this.dst = dst;
            }

            @Override
            public Callable<Void> newTask(final int no) {
                return new CopyingTask(src, dst, no);
            }
        } // CopyingTaskFactory

        final TFile src = archive;
        try {
            final TFile dst = new TFile(createTempFile());
            dst.rm();
            try {
                try {
                    final TaskJoiner join = runConcurrent(NUM_IO_THREADS,
                            new CopyingTaskFactory(src, dst));
                    try {
                        runConcurrent(NUM_IO_THREADS,
                                new CopyingTaskFactory(dst, src)
                                ).join();
                    } finally {
                        join.join();
                    }
                } finally {
                    TFile.umount(dst);
                }
            } finally {
                newNonArchiveFile(dst).rm();
            }
        } finally {
            TFile.umount(src);
        }
        // src alias archive gets deleted by the test fixture.
    }

    @Test
    public void testGrowing() throws IOException {
        final TFile file = newNonArchiveFile(archive);
        final TFile entry1 = new TFile(archive, "entry1");
        final TFile entry2 = new TFile(archive, "entry2");

        TConfig config = TConfig.push();
        try {
            config.setOutputPreferences(config.getOutputPreferences().set(GROW));

            createTestFile(entry1);
            createTestFile(entry2);

            umount();
            assertTrue(file.length() > 2 * data.length); // two entries plus one central directory

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
            assertTrue(file.length() > 6 * data.length); // six entries plus two central directories
        } finally {
            config.close();
        }

        assertThat(archive.list().length, is(0));

        config = TConfig.push();
        try {
            config.setOutputPreferences(config.getOutputPreferences().set(GROW));

            archive.rm();
            umount();
        } finally {
            config.close();
        }

        assertNull(archive.list());
    }
}
