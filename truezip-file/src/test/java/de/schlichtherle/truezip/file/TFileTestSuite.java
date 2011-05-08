/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.file;

import de.schlichtherle.truezip.socket.IOPoolProvider;
import de.schlichtherle.truezip.util.ArrayHelper;
import de.schlichtherle.truezip.fs.FsSyncException;
import de.schlichtherle.truezip.fs.FsSyncWarningException;
import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.io.FileBusyException;
import de.schlichtherle.truezip.fs.archive.FsArchiveDriver;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.socket.ByteArrayIOPool;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.OutputClosedException;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Performs a functional test of a particular FsArchiveDriver by using the
 * API of the TrueZIP File* module.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class TFileTestSuite {

    private static class ByteArrayIOPoolProvider implements IOPoolProvider {
        @Override
        public IOPool<?> get() {
            return new ByteArrayIOPool(2048);
        }
    }

    private static final Logger logger = Logger.getLogger(
            TFileTestSuite.class.getName());

    private static boolean lenientBackup;
    private static TArchiveDetector detectorBackup;

    private static final String TEMP_FILE_PREFIX = "tzp";

    private static final Random rnd = new Random();

    /** The data to get compressed. */
    private static final byte[] DATA = new byte[1024]; // enough to waste some heat on CPU cycles
    static {
        rnd.nextBytes(DATA);
    }

    protected static final IOPoolProvider
            IO_POOL_PROVIDER = new ByteArrayIOPoolProvider();
    
    private final FsScheme scheme;
    private final FsArchiveDriver<?> driver;

    private File temp;
    private TFile archive;
    private byte[] data;

    protected TFileTestSuite(   final @NonNull FsScheme scheme,
                                final @NonNull FsArchiveDriver<?> driver) {
        if (null == scheme || null == driver)
            throw new NullPointerException();
        this.scheme = scheme;
        this.driver = driver;
    }

    @BeforeClass
    public static void setUpClass() {
        lenientBackup = TFile.isLenient();
        detectorBackup = TFile.getDefaultArchiveDetector();
    }

    @Before
    public void setUp() throws IOException {
        TFile.setDefaultArchiveDetector(
                new TArchiveDetector(scheme.toString(), driver));
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

    protected final TFile getArchive() {
        return archive;
    }

    protected final String getSuffix() {
        return "." + scheme;
    }

    @After
    public void tearDown() {
        this.archive = null;

        // sync now to delete temps and free memory.
        // This prevents subsequent warnings about left over temporary files
        // and removes cached data from the memory, so it helps to start on a
        // clean sheet of paper with subsequent tests.
        try {
            TFile.umount();
        } catch (FsSyncException ex) {
            logger.log(Level.WARNING, ex.toString(), ex);
        }

        if (temp.exists() && !temp.delete())
            logger.log(Level.WARNING, "{0} (could not delete)", temp);

        TFile.setDefaultArchiveDetector(detectorBackup);
        TFile.setLenient(lenientBackup);
    }

    private static TFile newNonArchiveFile(TFile file) {
        return new TFile(file.getParentFile(),
                        file.getName(),
                        TArchiveDetector.NULL);
    }

    @Test
    public void testArchiveControllerStateWithInputStream()
    throws IOException, InterruptedException {
        final String path = archive.getPath() + "/test";
        archive = null;
        assertTrue(new TFile(path).createNewFile());
        TFile.umount();
        InputStream in = new TFileInputStream(path);
        Reference<FsController<?>> ref = new WeakReference<FsController<?>>(new TFile(path).getInnerArchive().getController());
        gc();
        assertNotNull(ref.get());
        in.close();
        gc();
        assertNotNull(ref.get());
        assertSame(ref.get(), new TFile(path).getInnerArchive().getController());
        in = null; // leaves file!
        TFile.umount();
        gc();
        assertNull(ref.get());
    }

    @Test
    public void testArchiveControllerStateWithOutputStream()
    throws IOException, InterruptedException {
        final String path = archive.getPath() + "/test";
        archive = null;
        assertTrue(new TFile(path).createNewFile());
        TFile.umount();
        OutputStream out = new TFileOutputStream(path);
        Reference<FsController<?>> ref = new WeakReference<FsController<?>>(new TFile(path).getInnerArchive().getController());
        gc();
        assertNotNull(ref.get());
        out.close();
        out = null; // leaves file!
        gc();
        assertNotNull(ref.get());
        assertSame(ref.get(), new TFile(path).getInnerArchive().getController());
        TFile.umount();
        gc();
        assertNull(ref.get());
    }

    protected static void gc() {
        System.gc();
        try {
            Thread.sleep(50);
        } catch (InterruptedException ex) {
            Logger.getLogger(TFileTestSuite.class.getName()).log(Level.WARNING, "Current thread was interrupted while waiting!", ex);
        }
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

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    private void assertFalsePositive(final TFile file) throws IOException {
        assert file.isArchive();

        // Note that file's parent directory may be a directory in the host file system!

        // Create file false positive.
        {
            OutputStream out = new TFileOutputStream(file);
            try {
                out.write(data);
            } finally {
                out.close();
            }
        }

        // Overwrite.
        {
            OutputStream out = new TFileOutputStream(file);
            try {
                out.write(data);
            } finally {
                out.close();
            }
        }

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
            new TFileInputStream(archive);
            fail("Expected FileNotFoundException");
        } catch (FileNotFoundException expected) {
        }

        try {
            new TFileOutputStream(archive);
            fail("Expected FileNotFoundException");
        } catch (FileNotFoundException expected) {
        }

        assertRm(file);

        // Create regular archive file.

        assertTrue(file.mkdir());
        assertTrue(newNonArchiveFile(file).isFile());
        assertTrue(file.exists());
        assertTrue(file.isDirectory());
        assertFalse(file.isFile());
        //assertEquals(0, file.getLength());
        assertTrue(file.lastModified() > 0);

        try {
            new TFileInputStream(archive);
            fail("Expected FileNotFoundException");
        } catch (FileNotFoundException expected) {
        }

        try {
            new TFileOutputStream(archive);
            fail("Expected FileNotFoundException");
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
        TFile.setLenient(false);
        try {
            file1.createNewFile();
            fail("Creating a file in a non-existent directory should throw an IOException!");
        } catch (IOException expected) {
        }
        assertCreateNewFile(archive, file1, file2);
        TFile.setLenient(true);
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
        assertEquals(0, file1.length());
        
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
        archive.rm_r();
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    private void assertIllegalDirectoryOperations(final TFile dir)
    throws IOException {
        assert dir.isDirectory();
        try {
            new TFileInputStream(dir);
            fail("Expected FileNotFoundException!");
        } catch (FileNotFoundException expected) {
        }
        try {
            new TFileOutputStream(dir);
            fail("Expected FileNotFoundException!");
        } catch (FileNotFoundException expected) {
        }
        File tmp = TFile.createTempFile(TEMP_FILE_PREFIX, null);
        try {
            try {
                TFile.cp(tmp, dir);
                fail("Expected IOException!");
            } catch (IOException expected) {
            }
            try {
                TFile.cp(dir, tmp);
                fail("Expected IOException!");
            } catch (IOException expected) {
            }
        } finally {
            TFile.rm(tmp);
        }
    }

    @Test
    public final void testStrictFileOutputStream() throws IOException {
        TFile file = new TFile(archive, "test.txt");

        TFile.setLenient(false);
        try {
            assertFileOutputStream(file);
            fail("Creating ghost directories should not be allowed when File.isLenient() is false!");
        } catch (FileNotFoundException expected) {
        }

        assertTrue(archive.mkdir());
        assertFileOutputStream(file);
        archive.rm();
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
        TFile.umount(); // allow external modifications!
        TFile.rm(new File(archive.getPath())); // use plain file to delete instead!
        assertFalse(archive.exists());
        assertFalse(archive.isDirectory());
        assertFalse(archive.isFile());
        assertEquals(0, archive.length());
    }

    private void assertFileOutputStream(TFile file) throws IOException {
        final byte[] message = "Hello World!\r\n".getBytes();
        
        final TFileOutputStream fos = new TFileOutputStream(file);
        assertTrue(file.exists());
        assertFalse(file.isDirectory());
        assertTrue(file.isFile());
        assertEquals(0, file.length());
        fos.write(message);
        assertEquals(0, file.length());
        fos.flush();
        assertEquals(0, file.length());
        fos.close();
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
    public final void testBusyFileInputStream() throws IOException {
        final TFile file1 = new TFile(archive, "file1");
        final TFile file2 = new TFile(archive, "file2");

        // Test open output streams.
        assertTrue(file1.createNewFile());
        TFile.umount(); // ensure file1 is really present in the archive file
        assertTrue(file2.createNewFile());
        TFileInputStream fis1 = new TFileInputStream(file1);
        try {
            new TFileInputStream(file2);
            fail();
        } catch (FileNotFoundException ex) {
            if (!(ex.getCause() instanceof FsSyncException)
                    || !(ex.getCause().getCause() instanceof FileBusyException))
                    throw ex;
        }
        file2.input(fis1);

        // fis1 is still open!
        try {
            TFile.umount(); // forces closing of fisA
            fail();
        } catch (FsSyncWarningException ex) {
            // Warning about fisA still being used.
            if (!(ex.getCause() instanceof FileBusyException))
                throw ex;
        }
        assertTrue(file2.isFile());
        try {
            file2.input(fis1); // fisA may be invalidated after update!
            fail();
        } catch (IOException expected) {
            assertFalse(file2.exists()); // previous op has removed file2!
        }

        // Open file2 as stream and let the garbage collection close the stream automatically.
        new TFileInputStream(file1);
        gc();

        // This update should complete without any exception if the garbage
        // collector did his job.
        try {
            TFile.umount(); // allow external modifications!
        } catch (FsSyncWarningException ex) {
            fail("The garbage collector hasn't been collecting an open stream. If this is only happening occasionally, you can safely ignore it.");
        }

        TFile.rm(newNonArchiveFile(archive));
        // Closing the invalidated stream explicitly should be OK.
        fis1.close();

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
    public final void testBusyFileOutputStream() throws IOException {
        TFile file1 = new TFile(archive, "file1");
        TFile file2 = new TFile(archive, "file2");
        
        // Ensure that there are two entries in the archive.
        // This is used later to check whether the update operation knows
        // how to deal with updating an archive for which there is still
        // an open output stream.
        TFileOutputStream fos1 = new TFileOutputStream(file1);
        TFile.cat(new ByteArrayInputStream(data), fos1);
        fos1.close();
        
        fos1 = new TFileOutputStream(file2);
        TFile.cat(new ByteArrayInputStream(data), fos1);
        fos1.close();
        
        TFile.umount(); // ensure two entries in the archive
        
        fos1 = new TFileOutputStream(file1);
        TFile.cat(new ByteArrayInputStream(data), fos1);
        
        // fos1 is still open!
        try {
            new TFileOutputStream(file1);
        } catch (FileNotFoundException ex) {
            if (!(ex.getCause() instanceof FsSyncException)
                    || !(ex.getCause().getCause() instanceof FileBusyException))
                    throw ex;
        }

        // fos1 is still open!
        try {
            new TFileOutputStream(file2);
        } catch (FileNotFoundException ex) {
            if (!(ex.getCause() instanceof FsSyncException)
                    || !(ex.getCause().getCause() instanceof FileBusyException))
                    throw ex;
            logger.warning("This archive driver does NOT support concurrent writing of different entries in the same archive file.");
        }

        // fos1 is still open!
        TFile.cat(new ByteArrayInputStream(data), fos1); // write again
        
        try {
            TFile.umount(); // forces closing of all streams
            fail();
        } catch (FsSyncWarningException ex) {
            if (!(ex.getCause() instanceof FileBusyException))
                throw ex;
        }
        
        try {
            TFile.cat(new ByteArrayInputStream(data), fos1); // write again
            fail();
        } catch (OutputClosedException expected) {
        }
        
        // The stream has been forcibly closed by TFile.update().
        // Another close is OK, though!
        fos1.close();
        
        // Reopen stream and let the garbage collection close the stream automatically.
        fos1 = new TFileOutputStream(file1);
        fos1 = null;
        gc();
        
        // This update should complete without any exception if the garbage
        // collector did his job.
        try {
            TFile.umount();
        } catch (FsSyncWarningException ex) {
            fail("The garbage collector hasn't been collecting an open stream. If this is only happening occasionally, you can safely ignore it.");
        }
        
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
        
        TFile.setLenient(true);
        
        assertTrue(dir6.mkdir()); // create all at once! note archive is in current directory!
        
        assertFalse(dir6.mkdir()); // isExisting already!
        assertFalse(dir5.mkdir()); // isExisting already!
        assertFalse(dir4.mkdir()); // isExisting already!
        assertFalse(dir3.mkdir()); // isExisting already!
        assertFalse(dir2.mkdir()); // isExisting already!
        assertFalse(dir1.mkdir()); // isExisting already!
        
        dir6.rm();
        dir5.rm();
        dir4.rm();
        dir3.rm();
        dir2.rm();
        dir1.rm();
        
        TFile.setLenient(false);
        
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
        assertFalse(".".equals(reversePath.getPath()));
        assertFalse("..".equals(reversePath.getPath()));
        
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

    private void assertListFiles(TFile dir, String entry) {
        final TFile[] files = dir.listFiles();
        boolean found = false;
        for (int i = 0, l = files.length; i < l; i++) {
            final TFile file = files[i];
            if (file.getName().equals(entry))
                found = true;
        }
        if (!found)
            fail("No such entry: " + entry);
    }

    @Test
    public final void testInputOutput() throws IOException {
        assertInputOutput(archive);
        
        final TFile archiveTest = new TFile(archive, "test");
        assertInputOutput(archiveTest);
        
        final TFile archive2 = new TFile(archive, "inner" + getSuffix());
        final TFile archive2Test = new TFile(archive2, "test");
        assertInputOutput(archive2Test);
        archive2.rm();
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
            fail("Expected IOException");
        } catch (IOException expected) {
        }
        try {
            TFile.cp(a, b);
            fail("Expected IOException");
        } catch (IOException expected) {
        }
        try {
            TFile.cp(b, a);
            fail("Expected IOException");
        } catch (IOException expected) {
        }
        try {
            TFile.cp(b, b);
            fail("Expected IOException");
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
        // the granularity of the parent file system, i.e. the OS file system!
        // Note that older OS file systems and even ext4 (!) have a granularity
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
        {
            final OutputStream out = new TFileOutputStream(a);
            try {
                out.write(data);
            } finally {
                out.close();
            }
            assertTrue(a.setLastModified(time - granularity));
        }

        // Test copy from.
        TFile.cp(a, b);
        assertThat(b.length(), is(a.length()));
        assertThat(b.lastModified(), not(is(a.lastModified())));
        TFile.cp_p(a, b);
        assertThat(b.length(), is(a.length()));
        long almd = a.lastModified() / granularity * granularity;
        long blmd = b.lastModified() / granularity * granularity;
        long almu = (a.lastModified() + granularity - 1) / granularity * granularity;
        long blmu = (b.lastModified() + granularity - 1) / granularity * granularity;
        assertTrue(almd == blmd || almu == blmu);

        // Test copy to.
        TFile.cp(b, a);
        assertThat(a.length(), is(b.length()));
        assertThat(a.lastModified(), not(is(b.lastModified())));
        TFile.cp_p(b, a);
        assertThat(a.length(), is(b.length()));
        almd = a.lastModified() / granularity * granularity;
        blmd = b.lastModified() / granularity * granularity;
        almu = (a.lastModified() + granularity - 1) / granularity * granularity;
        blmu = (b.lastModified() + granularity - 1) / granularity * granularity;
        assertTrue(almd == blmd || almu == blmu);

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
        for (j = 0; j < 100; j++) {
            archive.listFiles((FilenameFilter) null);
        }
        time = System.currentTimeMillis() - time;
        logger.log(Level.FINER, "Time required to list these entries {0} times using a nullary FilenameFilter: {1}ms", new Object[]{ j, time });
        
        time = System.currentTimeMillis();
        for (j = 0; j < 100; j++) {
            archive.listFiles((FileFilter) null);
        }
        time = System.currentTimeMillis() - time;
        logger.log(Level.FINER, "Time required to list these entries {0} times using a nullary FileFilter: {1}ms", new Object[]{ j, time });
        
        try {
            archive.rm();
            fail("directory not empty");
        } catch (IOException expected) {
        }
        TFile.umount(); // allow external modifications!
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
            } catch (IOException ex) {
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
            } catch (IOException ex) {
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
                } catch (IOException ex) {
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
            } catch (IOException ex) {
            }
        } finally {
            in1.close();
        }
        
        archive.rm_r();
        assertFalse(newPlainFile(archive).exists());
    }
    
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OS_OPEN_STREAM")
    @Test
    public final void testRenameValidArchive() throws IOException {
        // Create a regular archive with a single archive entry which
        // contains a creative greeting message.
        final OutputStream out = new TFileOutputStream(new TFile(archive, "entry"));
        try {
            new PrintStream(out, true).println("Hello World!");
        } finally {
            out.close(); // ALWAYS close streams!
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
        final TFile tmp = new TFile(archive.getPath(), TArchiveDetector.NULL);
        final InputStream in = new ByteArrayInputStream(data);
        TFile.cp(in, tmp);
        assertRenameArchiveToTemp(archive);
    }
    
    private void assertRenameArchiveToTemp(final TFile archive) throws IOException {
        assert archive.isArchive(); // regular archive or false positive
        assert !archive.isEntry(); // not contained in another archive file

        // Create a temporary file.
        TFile tmp = new TFile(TFile.createTempFile(TEMP_FILE_PREFIX, null));
        tmp.rm();
        assertFalse(tmp.exists());
        assertFalse(newPlainFile(tmp).exists());

        // Now rename the archive to the temporary path.
        // Depending on the true state of the object "archive", this will
        // either create a directory (iff archive is a regular archive) or a
        // plain file (iff archive is false positive).
        archive.mv(tmp);
        assertFalse(archive.exists());
        assertFalse(newPlainFile(archive).exists());

        // Now delete resulting temporary file.
        tmp.rm_r();
        assertFalse(tmp.exists());
        assertFalse(newPlainFile(tmp).exists());
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
        if (!src.isEntry())
            assertTrue(newPlainFile(src).exists());
        assertFalse(dst.exists());
        if (!dst.isEntry())
            assertFalse(newPlainFile(dst).exists());
        src.mv(dst); // lenient!
        assertFalse(src.exists());
        if (!src.isEntry())
            assertFalse(newPlainFile(src).exists());
        assertTrue(dst.exists());
        if (!dst.isEntry())
            assertTrue(newPlainFile(dst).exists());
    }
    
    private static File newPlainFile(final TFile file) {
        return new File(file.getPath());
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
            final TFile file2 = files[i];
            assertTrue(!(ref instanceof TFile));
            assertEquals(ref.getPath(), file2.getPath());
            assertNull(file2.list());
            assertNull(file2.list(null));
            assertNull(file2.listFiles());
            assertNull(file2.listFiles(file2.getArchiveDetector()));
            assertNull(file2.listFiles((FileFilter) null));
            assertNull(file2.listFiles((FilenameFilter) null));
            assertNull(file2.listFiles((FileFilter) null, file2.getArchiveDetector()));
            assertNull(file2.listFiles((FilenameFilter) null, file2.getArchiveDetector()));
        }
    }
    
    @Test
    public final void testMultithreadedSingleArchiveMultipleEntriesReading()
    throws Exception {
        assertMultithreadedSingleArchiveMultipleEntriesReading(20, 20);
    }
    
    /**
     * Creates a test archive file with the given number of entries and then
     * creates the given number of threads where each of them reads all these
     * entries.
     *
     * @param nEntries The number of archive file entries to be created.
     * @param nThreads The number of threads to be created.
     */
    private void assertMultithreadedSingleArchiveMultipleEntriesReading(
            final int nEntries,
            final int nThreads)
    throws Exception {
        // Create test archive file.
        createTestArchive(nEntries);
        
        // Define thread class to enumerate and read all entries.
        class CheckAllEntriesThread extends Thread {
            Throwable failure;

            CheckAllEntriesThread() {
                setDaemon(true);
            }
            
            @Override
            public void run() {
                try {
                    assertArchiveEntries(archive, nEntries);
                } catch (Throwable t) {
                    failure = t;
                }
            }
        } // class CheckAllEntriesThread
        
        // Create and start all threads.
        final CheckAllEntriesThread[] threads = new CheckAllEntriesThread[nThreads];
        for (int i = 0; i < nThreads; i++) {
            final CheckAllEntriesThread thread = new CheckAllEntriesThread();
            thread.start();
            threads[i] = thread;
        }
        
        // Wait for all threads until done.
        for (int i = 0; i < nThreads; i++) {
            final CheckAllEntriesThread thread = threads[i];
            thread.join();
            if (thread.failure != null)
                throw new Exception(thread.failure);
        }
        
        TFile.rm_r(archive);
    }
    
    private void createTestArchive(final int nEntries) throws IOException {
        for (int i = 0; i < nEntries; i++) {
            final TFile entry = new TFile(archive + TFile.separator + i);
            final OutputStream out = new TFileOutputStream(entry);
            try {
                out.write(data);
            } finally {
                out.close();
            }
        }
    }
    
    private void assertArchiveEntries(final TFile archive, int nEntries)
    throws IOException {
        final File[] entries = archive.listFiles();
        assertEquals(nEntries, entries.length);
        final byte[] buf = new byte[4096];
        for (int i = 0, l = entries.length; i < l; i++) {
            final TFile entry = (TFile) entries[i];
            // Read full entry and check the contents.
            final InputStream in = new TFileInputStream(entry);
            try {
                int off = 0;
                int read;
                do {
                    read = in.read(buf);
                    if (read < 0)
                        break;
                    assertTrue(read > 0);
                    assertTrue(ArrayHelper.equals(
                            data, off, buf, 0, read));
                    off += read;
                } while (true);
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
        assertMultithreadedSingleArchiveMultipleEntriesWriting(archive, 20, false);
        assertMultithreadedSingleArchiveMultipleEntriesWriting(archive, 20, true);
    }
    
    private void assertMultithreadedSingleArchiveMultipleEntriesWriting(
            final TFile archive,
            final int nThreads,
            final boolean wait)
            throws Exception {
        assertTrue(TFile.isLenient());
        
        class WritingThread extends Thread {
            final int i;
            Throwable failure;
            
            WritingThread(int i) {
                setDaemon(true);
                this.i = i;
            }
            
            @Override
            public void run() {
                try {
                    final TFile file = new TFile(archive, i + "");
                    OutputStream out;
                    while (true) {
                        try {
                            out = new TFileOutputStream(file);
                            break;
                        } catch (FileBusyException busy) {
                            continue;
                        }
                    }
                    try {
                        out.write(data);
                    } finally {
                        out.close();
                    }
                    try {
                        TFile.umount(wait, false, wait, false);
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
                } catch (Throwable exception) {
                    failure = exception;
                }
            }
        } // class WritingThread
        
        // Create and start all threads.
        final WritingThread[] threads = new WritingThread[nThreads];
        for (int i = 0; i < nThreads; i++) {
            final WritingThread thread = new WritingThread(i);
            thread.start();
            threads[i] = thread;
        }
        
        // Wait for all threads to finish.
        for (int i = 0; i < nThreads; i++) {
            final WritingThread thread = threads[i];
            thread.join();
            if (thread.failure != null)
                throw new Exception(thread.failure);
        }
        
        assertArchiveEntries(archive, nThreads);
        TFile.rm_r(archive);
    }
    
    @Test
    public final void testMultithreadedMultipleArchivesSingleEntryWriting()
    throws Exception {
        assertMultithreadedMultipleArchivesSingleEntryWriting(20, false);
        assertMultithreadedMultipleArchivesSingleEntryWriting(20, true);
    }
    
    private void assertMultithreadedMultipleArchivesSingleEntryWriting(
            final int nThreads,
            final boolean updateIndividually)
    throws Exception {
        assertTrue(TFile.isLenient());
        
        class WritingThread extends Thread {
            Throwable failure;

            WritingThread() {
                setDaemon(true);
            }

            @Override
            public void run() {
                try {
                    final TFile archive = new TFile(createTempFile());
                    archive.rm();
                    final TFile file = new TFile(archive, "entry");
                    try {
                        final OutputStream out = new TFileOutputStream(file);
                        try {
                            out.write(data);
                        } finally {
                            out.close();
                        }
                        try {
                            if (updateIndividually)
                                TFile.umount(archive);
                            else
                                TFile.umount(false);
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
                } catch (Throwable exception) {
                    failure = exception;
                }
            }
        } // class WritingThread
        
        // Create and start all threads.
        final WritingThread[] threads = new WritingThread[nThreads];
        for (int i = 0; i < nThreads; i++) {
            final WritingThread thread = new WritingThread();
            thread.start();
            threads[i] = thread;
        }
        
        // Wait for all threads to finish.
        for (int i = 0; i < nThreads; i++) {
            final WritingThread thread = threads[i];
            thread.join();
            if (thread.failure != null)
                throw new Exception(thread.failure);
        }
    }
}
