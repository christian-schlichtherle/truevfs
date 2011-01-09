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
package de.schlichtherle.truezip.io.file;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import de.schlichtherle.truezip.io.fs.FsController;
import de.schlichtherle.truezip.io.FileBusyException;
import de.schlichtherle.truezip.io.fs.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.fs.FsScheme;
import de.schlichtherle.truezip.io.socket.OutputClosedException;
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
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Performs an integration test of a particular ArchiveDriver by using the
 * File* API.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class FileTestCase {

    private static final Logger logger = Logger.getLogger(
            FileTestCase.class.getName());

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
    
    private final FsScheme scheme;
    private final ArchiveDriver<?> driver;

    private java.io.File temp;
    private File archive;
    private byte[] data;

    protected FileTestCase( final @NonNull FsScheme scheme,
                            final @NonNull ArchiveDriver<?> driver) {
        if (null == scheme || null == driver)
            throw new NullPointerException();
        this.scheme = scheme;
        this.driver = driver;
    }

    /**
     * A subclass must override this method to create the {@link #data}
     * to be archived.
     * It must also finally call this superclass implementation to create
     * the temporary file to be used as an archive file.
     */
    @Before
    public void setUp() throws IOException {
        File.setLenient(true); // Restore default
        File.setDefaultArchiveDetector(
                new DefaultArchiveDetector(scheme.toString(), driver));
        File.umount();
        temp = createTempFile();
        assertTrue(temp.delete());
        archive = new File(temp);
        data = DATA.clone();
    }

    private java.io.File createTempFile() throws IOException {
        return File.createTempFile(TEMP_FILE_PREFIX, getSuffix()).getCanonicalFile();
    }

    protected final File getArchive() {
        return archive;
    }

    protected final String getSuffix() {
        return "." + scheme.toString();
    }

    @After
    public void tearDown() throws IOException {
        this.archive = null;

        // sync now to delete temps and free memory.
        // This prevents subsequent warnings about left over temporary files
        // and removes cached data from the memory, so it helps to start on a
        // clean sheet of paper with subsequent tests.
        try {
            File.umount();
        } catch (ArchiveException ignored) {
            // Normally, you should NOT ignore all exceptions thrown by this
            // method.
            // The reason we do it here is that they are usually after effects
            // of failed tests and we don't want any exception from the tests
            // to be overridden by an exception thrown in this clean up method.
        }

        if (temp.exists() && !temp.delete())
            logger.log(Level.WARNING, "{0} (could not delete)", temp);

        File.setDefaultArchiveDetector(ArchiveDetector.ALL); // restore default
        File.setLenient(true); // Restore default
    }

    private static File newNonArchiveFile(File file) {
        return new File(file.getParentFile(),
                        file.getName(),
                        ArchiveDetector.NULL);
    }

    @Test
    public void testArchiveControllerStateWithInputStream()
    throws IOException, InterruptedException {
        final String path = archive.getPath() + "/test";
        archive = null;
        assertTrue(new File(path).createNewFile());
        File.umount();
        InputStream in = new FileInputStream(path);
        Reference<FsController<?>> ref = new WeakReference<FsController<?>>(new File(path).getInnerArchive().getController());
        gc();
        assertNotNull(ref.get());
        in.close();
        gc();
        assertNotNull(ref.get());
        assertSame(ref.get(), new File(path).getInnerArchive().getController());
        in = null; // leaves file!
        File.umount();
        gc();
        assertNull(ref.get());
    }

    @Test
    public void testArchiveControllerStateWithOutputStream()
    throws IOException, InterruptedException {
        final String path = archive.getPath() + "/test";
        archive = null;
        assertTrue(new File(path).createNewFile());
        File.umount();
        OutputStream out = new FileOutputStream(path);
        Reference<FsController<?>> ref = new WeakReference<FsController<?>>(new File(path).getInnerArchive().getController());
        gc();
        assertNotNull(ref.get());
        out.close();
        out = null; // leaves file!
        gc();
        assertNotNull(ref.get());
        assertSame(ref.get(), new File(path).getInnerArchive().getController());
        File.umount();
        gc();
        assertNull(ref.get());
    }

    protected static void gc() {
        System.gc();
        try {
            Thread.sleep(50);
        } catch (InterruptedException ex) {
            Logger.getLogger(FileTestCase.class.getName()).log(Level.WARNING, "Current thread was interrupted while waiting!", ex);
        }
    }

    @Test
    public final void testFalsePositives() throws IOException {
        assertFalsePositive(archive);

        // Dito for entry.
        final File entry = new File(archive, "entry" + getSuffix());

        assertTrue(archive.mkdir());
        assertFalsePositive(entry);
        assertTrue(archive.delete());

        assertTrue(newNonArchiveFile(archive).mkdir());
        assertFalsePositive(entry);
        assertTrue(archive.delete());
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    private void assertFalsePositive(final File file) throws IOException {
        assert file.isArchive();

        // Note that file's parent directory may be a directory in the host file system!

        // Create file false positive.
        {
            OutputStream out = new FileOutputStream(file);
            try {
                out.write(data);
            } finally {
                out.close();
            }
        }

        // Overwrite.
        {
            OutputStream out = new FileOutputStream(file);
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
            InputStream in = new FileInputStream(file);
            try {
                byte[] buf = new byte[data.length];
                assertTrue(de.schlichtherle.truezip.util.Arrays.equals(data, 0, buf, 0, in.read(buf)));
            } finally {
                in.close();
            }
        }
        assertDelete(file);

        // Create directory false positive.

        assertTrue(newNonArchiveFile(file).mkdir());
        assertTrue(file.exists());
        assertTrue(file.isDirectory());
        assertFalse(file.isFile());
        //assertEquals(0, file.getLength());
        assertTrue(file.lastModified() > 0);

        try {
            new FileInputStream(archive);
            fail("Expected FileNotFoundException");
        } catch (FileNotFoundException expected) {
        }

        try {
            new FileOutputStream(archive);
            fail("Expected FileNotFoundException");
        } catch (FileNotFoundException expected) {
        }

        assertDelete(file);

        // Create regular archive file.

        assertTrue(file.mkdir());
        assertTrue(newNonArchiveFile(file).isFile());
        assertTrue(file.exists());
        assertTrue(file.isDirectory());
        assertFalse(file.isFile());
        //assertEquals(0, file.getLength());
        assertTrue(file.lastModified() > 0);

        try {
            new FileInputStream(archive);
            fail("Expected FileNotFoundException");
        } catch (FileNotFoundException expected) {
        }

        try {
            new FileOutputStream(archive);
            fail("Expected FileNotFoundException");
        } catch (FileNotFoundException expected) {
        }

        assertDelete(file);
    }

    private void assertDelete(final File file) throws IOException {
        assertTrue(file.delete());
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
        final java.io.File archive = createTempFile();
        assertTrue(archive.delete());
        final java.io.File file1 = new java.io.File(archive, "test.txt");
        final java.io.File file2 = new java.io.File(file1, "test.txt");
        try {
            file1.createNewFile();
            fail("Creating a file in a non-existent directory should throw an IOException!");
        } catch (IOException expected) {
        }
        assertCreateNewFile(archive, file1, file2);
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    private void assertCreateNewEnhancedFile() throws IOException {
        final java.io.File file1 = new File(archive, "test.txt");
        final java.io.File file2 = new File(file1, "test.txt");

        File.setLenient(false);
        try {
            file1.createNewFile();
            fail("Creating a file in a non-existent directory should throw an IOException!");
        } catch (IOException ok) {
            // This is exactly what we expect here!
        }
        assertCreateNewFile(archive, file1, file2);

        File.setLenient(true);
        assertCreateNewFile(archive, file1, file2);
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    private void assertCreateNewFile(   final java.io.File dir,
                                        final java.io.File file1,
                                        final java.io.File file2)
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
        } catch (IOException ok) {
            // This is exactly what we expect here!
        }
        
        assertTrue(file1.delete()); // OK now!
        assertFalse(file1.exists());
        assertFalse(file1.isDirectory());
        assertFalse(file1.isFile());
        assertEquals(0, file1.length());
        
        assertTrue(dir.delete());
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
        File file = archive;
        for (int i = 0; i <= names.length; i++) {
            final File file2 = newNonArchiveFile(file);
            assertTrue(file2.mkdir());
            assertIllegalDirectoryOperations(file2);
            assertTrue(file2.delete());
            assertTrue(file.mkdir());
            assertIllegalDirectoryOperations(file);
            if (i < names.length)
                file = new File(file, names[i]);
        }
        assertTrue(archive.deleteAll());
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    private void assertIllegalDirectoryOperations(final File dir)
    throws IOException {
        assert dir.isDirectory();
        try {
            new FileInputStream(dir);
            fail("Expected FileNotFoundException!");
        } catch (FileNotFoundException expected) {
        }
        try {
            new FileOutputStream(dir);
            fail("Expected FileNotFoundException!");
        } catch (FileNotFoundException expected) {
        }
        java.io.File tmp = File.createTempFile(TEMP_FILE_PREFIX, null);
        try {
            try {
                File.cp(tmp, dir);
                fail("Expected FileNotFoundException!");
            } catch (FileNotFoundException expected) {
            }
            try {
                File.cp(dir, tmp);
                fail("Expected FileNotFoundException!");
            } catch (FileNotFoundException expected) {
            }
        } finally {
            if (!tmp.delete())
                throw new IOException(tmp + " (could not delete)");
        }
    }

    @Test
    public final void testStrictFileOutputStream() throws IOException {
        File file = new File(archive, "test.txt");

        File.setLenient(false);
        try {
            assertFileOutputStream(file);
            fail("Creating ghost directories should not be allowed when File.isLenient() is false!");
        } catch (FileNotFoundException expected) {
        }

        assertTrue(archive.mkdir());
        assertFileOutputStream(file);
        assertTrue(archive.delete());
    }
    
    @Test
    public final void testLenientFileOutputStream() throws IOException {
        File file = new File(archive, "dir/inner" + getSuffix() + "/dir/test.txt");

        assertFileOutputStream(file);

        assertFalse(archive.delete()); // directory not empty!
        File.umount(); // allow external modifications!
        assertTrue(new java.io.File(archive.getPath()).delete()); // use plain file to delete instead!
        assertFalse(archive.exists());
        assertFalse(archive.isDirectory());
        assertFalse(archive.isFile());
        assertEquals(0, archive.length());
    }

    private void assertFileOutputStream(File file) throws IOException {
        final byte[] message = "Hello World!\r\n".getBytes();
        
        final FileOutputStream fos = new FileOutputStream(file);
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
        
        assertTrue(file.delete());
        assertFalse(file.exists());
        assertFalse(file.isDirectory());
        assertFalse(file.isFile());
        assertEquals(0, file.length());
    }
    
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OS_OPEN_STREAM")
    @Test
    public final void testBusyFileInputStream() throws IOException {
        final File file1 = new File(archive, "file1");
        final File file2 = new File(archive, "file2");

        // Test open output streams.
        assertTrue(file1.createNewFile());
        File.update(); // ensure file1 is really present in the archive file
        assertTrue(file2.createNewFile());
        FileInputStream fis1 = new FileInputStream(file1);
        try {
            new FileInputStream(file2);
            fail("Accessing file2 was expected to fail because an auto update needs to be done but the archive file is busy on input for fis1!");
        } catch (FileBusyException expected) {
        }
        assertTrue(file2.catFrom(fis1)); // fails for same reason.

        // fis1 is still open!
        try {
            File.update(); // forces closing of fisA
            fail("ArchiveFileBusyWarningException expected!");
        } catch (ArchiveWarningException ex) {
            // Warning about fisA still being used.
            if (!(ex.getCause() instanceof FileBusyException))
                throw ex;
        }
        assertTrue(file2.isFile());
        if (!file2.catFrom(fis1)) // fisA may be invalidated after update!
            assertFalse(file2.exists()); // previous op has removed file2!

        // Open file2 as stream and let the garbage collection close the stream automatically.
        new FileInputStream(file1);
        gc();

        // This update should complete without any exception if the garbage
        // collector did his job.
        try {
            File.umount(); // allow external modifications!
        } catch (ArchiveWarningException ex) {
            fail("The garbage collector hasn't been collecting an open stream. If this is only happening occasionally, you can safely ignore it.");
        }

        assertTrue(newNonArchiveFile(archive).delete());
        // Closing the invalidated stream explicitly should be OK.
        fis1.close();

        // Cleanup.
        assertFalse(file2.delete()); // already deleted externally
        assertFalse(file2.exists());
        assertFalse(file1.delete());
        assertFalse(file1.exists());
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OS_OPEN_STREAM")
    @Test
    public final void testBusyFileOutputStream() throws IOException {
        File file1 = new File(archive, "file1");
        File file2 = new File(archive, "file2");
        
        // Ensure that there are two entries in the archive.
        // This is used later to check whether the update operation knows
        // how to deal with updating an archive for which there is still
        // an open output stream.
        FileOutputStream fos1 = new FileOutputStream(file1);
        File.cat(new ByteArrayInputStream(data), fos1);
        fos1.close();
        
        fos1 = new FileOutputStream(file2);
        File.cat(new ByteArrayInputStream(data), fos1);
        fos1.close();
        
        File.update(); // ensure two entries in the archive
        
        fos1 = new FileOutputStream(file1);
        File.cat(new ByteArrayInputStream(data), fos1);
        
        // fos1 is still open!
        try {
            new FileOutputStream(file1);
        } catch (FileBusyException expected) {
        }
        
        // fos1 is still open!
        try {
            new FileOutputStream(file2);
        } catch (FileBusyException busy) {
            logger.warning("This archive driver does NOT support concurrent writing of different entries in the same archive file.");
        }

        // fos1 is still open!
        File.cat(new ByteArrayInputStream(data), fos1); // write again
        
        try {
            File.update(); // forces closing of all streams
            fail("Output stream should have been forced to close!");
        } catch (ArchiveWarningException ex) {
            if (!(ex.getCause() instanceof FileBusyException))
                throw ex;
        }
        
        try {
            File.cat(new ByteArrayInputStream(data), fos1); // write again
            fail("Output stream should have been forcibly closed!");
        } catch (OutputClosedException expected) {
        }
        
        // The stream has been forcibly closed by File.update().
        // Another close is OK, though!
        fos1.close();
        
        // Reopen stream and let the garbage collection close the stream automatically.
        fos1 = new FileOutputStream(file1);
        fos1 = null;
        gc();
        
        // This update should complete without any exception if the garbage
        // collector did his job.
        try {
            File.update();
        } catch (ArchiveWarningException ex) {
            fail("The garbage collector hasn't been collecting an open stream. If this is only happening occasionally, you can safely ignore it.");
        }
        
        // Cleanup.
        assertTrue(file2.delete());
        assertFalse(file2.exists());
        assertTrue(file1.delete());
        assertFalse(file1.exists());
    }
    
    @Test
    public final void testMkdir() throws IOException {
        final File dir1 = archive;
        final File dir2 = new File(dir1, "dir");
        final File dir3 = new File(dir2, "inner" + getSuffix());
        final File dir4 = new File(dir3, "dir");
        final File dir5 = new File(dir4, "nuts" + getSuffix());
        final File dir6 = new File(dir5, "dir");
        
        File.setLenient(true);
        
        assertTrue(dir6.mkdir()); // create all at once! note archive is in current directory!
        
        assertFalse(dir6.mkdir()); // isExisting already!
        assertFalse(dir5.mkdir()); // isExisting already!
        assertFalse(dir4.mkdir()); // isExisting already!
        assertFalse(dir3.mkdir()); // isExisting already!
        assertFalse(dir2.mkdir()); // isExisting already!
        assertFalse(dir1.mkdir()); // isExisting already!
        
        assertTrue(dir6.delete());
        assertTrue(dir5.delete());
        assertTrue(dir4.delete());
        assertTrue(dir3.delete());
        assertTrue(dir2.delete());
        assertTrue(dir1.delete());
        
        File.setLenient(false);
        
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
        
        assertTrue(dir6.delete());
        assertTrue(dir5.delete());
        assertTrue(dir4.delete());
        assertTrue(dir3.delete());
        assertTrue(dir2.delete());
        assertTrue(dir1.delete());
    }
    
    @Test
    public final void testDirectoryTree() throws IOException {
        assertDirectoryTree(
                new File(System.getProperty("java.io.tmpdir")), // base directory
                new File("dir/inner" + getSuffix() + "/dir/outer" + getSuffix() + "/" + archive.getName())); // this path is reversed!!!
    }

    private void assertDirectoryTree(File basePath, File reversePath)
    throws IOException {
        if (reversePath == null) {
            // We're at the leaf of the directory tree.
            final File test = new File(basePath, "test.txt");
            //testCreateNewFile(basePath, test);
            assertFileOutputStream(test);
            return;
        }
        assertFalse(".".equals(reversePath.getPath()));
        assertFalse("..".equals(reversePath.getPath()));
        
        final File member = new File(basePath, reversePath.getName());
        final boolean created = member.mkdir();
        final File children = reversePath.getParentFile();
        assertDirectoryTree(member, children);
        assertListFiles(basePath, member.getName());
        assertTrue(member.exists());
        assertTrue(member.isDirectory());
        assertFalse(member.isFile());
        if (member.isArchive())
            assertEquals(0, member.length());
        if (created) {
            assertTrue(member.delete());
            assertFalse(member.exists());
            assertFalse(member.isDirectory());
            assertFalse(member.isFile());
            assertEquals(0, member.length());
        }
    }

    private void assertListFiles(File dir, String entry) {
        final File[] files = dir.listFiles();
        boolean found = false;
        for (int i = 0, l = files.length; i < l; i++) {
            final File file = files[i];
            if (file.getName().equals(entry))
                found = true;
        }
        if (!found)
            fail("No such entry: " + entry);
    }

    @Test
    public final void testCat() throws IOException {
        assertCat(archive);
        
        final File archiveTest = new File(archive, "test");
        assertCat(archiveTest);
        
        final File archive2 = new File(archive, "inner" + getSuffix());
        final File archive2Test = new File(archive2, "test");
        assertCat(archive2Test);
        assertTrue(archive2.delete());
        assertTrue(archive.delete());
    }
    
    private void assertCat(final File file) throws IOException {
        assertCatFrom(file);
        assertCatTo(file);
        assertTrue(file.delete());
    }
    
    private void assertCatFrom(final File file) throws IOException {
        final InputStream in = new ByteArrayInputStream(data);
        try {
            assertTrue(file.catFrom(in));
        } finally {
            in.close();
        }
        assertEquals(data.length, file.length());
    }
    
    private void assertCatTo(final File file) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
        try {
            assertTrue(file.catTo(out));
        } finally {
            out.close();
        }
        assertTrue(Arrays.equals(data, out.toByteArray()));
    }

    @Test
    public final void testCopyContainingOrSameFiles() throws IOException {
        assert !archive.exists();
        
        final File dir = archive.getParentFile();
        assertNotNull(dir);
        final File entry = new File(archive, "entry");
        
        assertCopyContainingOrSameFiles0(dir, archive);
        assertCopyContainingOrSameFiles0(archive, entry);
        
        assertTrue(entry.catFrom(new ByteArrayInputStream(data)));
        
        assertCopyContainingOrSameFiles0(dir, archive);
        assertCopyContainingOrSameFiles0(archive, entry);
        
        assertTrue(archive.deleteAll());
    }
    
    private void assertCopyContainingOrSameFiles0(final File a, final File b)
    throws IOException {
        assertCopyContainingOrSameFiles1(a, b);
        assertCopyContainingOrSameFiles1(a.getCanOrAbsFile(), b);
        assertCopyContainingOrSameFiles1(a, b.getCanOrAbsFile());
        assertCopyContainingOrSameFiles1(a.getCanOrAbsFile(), b.getCanOrAbsFile());
    }
    
    private void assertCopyContainingOrSameFiles1(final File a, final File b)
    throws IOException {
        try {
            File.cp(a, a);
            fail("Expected ContainsFileException");
        } catch (ContainsFileException sfe) {
        }
        try {
            File.cp(a, b);
            fail("Expected FileNotFoundException");
        } catch (FileNotFoundException fnfe) {
        }
        try {
            File.cp(b, a);
            fail("Expected FileNotFoundException");
        } catch (FileNotFoundException fnfe) {
        }
        try {
            File.cp(b, b);
            fail("Expected SameFileException");
        } catch (ContainsFileException sfe) {
        }
    }

    @Test
    public final void testCopyDelete() throws IOException {
        final String[] names = {
            "0" + getSuffix(),
            "1" + getSuffix(),
            //"2" + getSuffix(),
        };

        assertTrue(archive.mkdir());
        assertCopyDelete(archive, names, 0);
        assertTrue(archive.delete());

        assertTrue(newNonArchiveFile(archive).mkdir());
        assertCopyDelete(archive, names, 0);
        assertTrue(archive.delete());
    }

    private void assertCopyDelete(final File parent, String[] names, int off)
    throws IOException {
        if (off >= names.length)
            return;
        
        final File dir = new File(parent, names[off]);

        assertTrue(dir.mkdir()); // create valid archive file
        assertCopyDelete(parent, dir);
        assertCopyDelete(dir, names, off + 1); // continue recursion
        assertTrue(dir.delete());

        assertTrue(newNonArchiveFile(dir).mkdir()); // create false positive archive file
        assertCopyDelete(parent, dir);
        assertCopyDelete(dir, names, off + 1); // continue recursion
        assertTrue(dir.delete());
    }

    private void assertCopyDelete(final File parent, final File dir)
    throws IOException {
        final File parentA = new File(parent, "a");
        final File parentB = new File(parent, "b" + getSuffix());
        final File dirA = new File(dir, "a");
        final File dirB = new File(dir, "b" + getSuffix());

        assertCopyDelete0(dirA, dirB);
        assertCopyDelete0(dirA, parentA);
        assertCopyDelete0(dirA, parentB);
        assertCopyDelete0(parentA, dirA);
        assertCopyDelete0(parentA, dirB);
        assertCopyDelete0(parentB, dirA);
        assertCopyDelete0(parentB, dirB);
        assertCopyDelete0(dirB, dirA);
        assertCopyDelete0(dirB, parentA);
        assertCopyDelete0(dirB, parentB);
    }

    private void assertCopyDelete0(File a, File b) throws IOException {
        assertCopyDelete0(a, b, 2000); // works in all archive types currently supported
    }

    private void assertCopyDelete0(   final File a,
                                final File b,
                                final long granularity)
    throws IOException {
        // Create file an with old timestamp.
        {
            final OutputStream out = new FileOutputStream(a);
            try {
                out.write(data);
            } finally {
                out.close();
            }
            assertTrue(a.setLastModified(System.currentTimeMillis() - granularity));
        }

        // Test copyFrom.
        assertTrue(b.copyFrom(a));
        assertEquals(a.length(), b.length());
        assertTrue(a.lastModified() != b.lastModified());
        assertTrue(b.archiveCopyFrom(a));
        assertEquals(a.length(), b.length());
        long almd = a.lastModified() / granularity * granularity;
        long blmd = b.lastModified() / granularity * granularity;
        long almu = (a.lastModified() + granularity - 1) / granularity * granularity;
        long blmu = (b.lastModified() + granularity - 1) / granularity * granularity;
        assertTrue(almd == blmd || almu == blmu);

        // Test copyTo.
        assertTrue(b.copyTo(a)); // updates timestamp
        assertEquals(a.length(), b.length());
        assertTrue(a.lastModified() != b.lastModified());
        assertTrue(b.archiveCopyTo(a));
        assertEquals(a.length(), b.length());
        almd = a.lastModified() / granularity * granularity;
        blmd = b.lastModified() / granularity * granularity;
        almu = (a.lastModified() + granularity - 1) / granularity * granularity;
        blmu = (b.lastModified() + granularity - 1) / granularity * granularity;
        assertTrue(almd == blmd || almu == blmu);

        // Check result.
        {
            final ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
            assertTrue(a.copyTo(out));
            assertTrue(Arrays.equals(data, out.toByteArray()));
        }

        // Cleanup.
        assertTrue(a.delete());
        assertTrue(b.delete());
    }

    @Test
    public final void testListPerformance() throws IOException {
        assertTrue(archive.mkdir());
        
        int i, j;
        long time;
        
        time = System.currentTimeMillis();
        for (i = 0; i < 100; i++) {
            File file = new File(archive, "" + i);
            assertTrue(file.createNewFile());
        }
        time = System.currentTimeMillis() - time;
        logger.log(Level.FINER, "Time required to create {0} archive file entries: {1}ms", new Object[]{i, time});
        
        time = System.currentTimeMillis();
        for (j = 0; j < 100; j++) {
            archive.listFiles((FilenameFilter) null);
        }
        time = System.currentTimeMillis() - time;
        logger.log(Level.FINER, "Time required to list these entries {0} times using a nullary FilenameFilter: {1}ms", new Object[]{j, time});
        
        time = System.currentTimeMillis();
        for (j = 0; j < 100; j++) {
            archive.listFiles((FileFilter) null);
        }
        time = System.currentTimeMillis() - time;
        logger.log(Level.FINER, "Time required to list these entries {0} times using a nullary FileFilter: {1}ms", new Object[]{j, time});
        
        assertFalse(archive.delete()); // directory not empty!
        File.umount(); // allow external modifications!
        assertTrue(new java.io.File(archive.getPath()).delete()); // use plain file to delete instead!
        assertFalse(archive.exists());
        assertFalse(archive.isDirectory());
        assertFalse(archive.isFile());
        assertEquals(0, archive.length());
    }
    
    // This test needs to be redesigned: It primarily lacks a clear intention.
    /*@Test
    public final void testGarbageCollection() throws Throwable {
        logger.fine("testGarbageCollection");
     
        // Preamble:
        {
            Object obj = new Object();
            Reference ref = new WeakReference(obj);
            obj = null;
            gc(); // Assumption doesn't work without this!
            //System.runFinalization(); // doesn't work!
            assert ref.get() == null;
        }
     
        // Test:
        try {
            File file = new File(archive);
            for (int i = 0; i < 100; i++) {
                file = new File(file, i + getSuffix());
                assertTrue(file.mkdir());
            }
        } catch (Throwable failure) {
            // This could be an OOME with CBZip2OutputStream from Ant 1.7.0RC1!
            File.sync(); // exceptions thrown here take precedence!
            throw failure;
        }
        // Note that though the file chain is now eligible for garbage
        // collection, the associated archive controllers are not since they
        // have dirty file systems.
     
        // Now set the system under stress so that the garbage collector will
        // most likely reclaim the chain of file objects and the archive
        // controllers once they have been updated.
        // Note that FederatedFileSystemController.finalize() is called in no particular
        // order, i.e. the object graph is completely ignored! :-o
        byte[] buf1 = new byte[10 * 1024 * 1024];
        gc();
        byte[] buf2 = new byte[10 * 1024 * 1024];
        File.sync(); // allow external modifications!
        byte[] buf3 = new byte[10 * 1024 * 1024];
        gc();
        byte[] buf4 = new byte[10 * 1024 * 1024];
     
        assertTrue(archive.deleteAll());
    }*/
    
    @Test
    public final void testIllegalDeleteEntryWithOpenStream()
    throws IOException {
        final File entry1 = new File(archive, "entry1");
        final File entry2 = new File(archive, "entry2");
        
        final OutputStream out1 = new FileOutputStream(entry1);
        try {
            assertFalse(entry1.delete());
            out1.write(data);
            assertFalse(archive.deleteAll());
        } finally {
            out1.close();
        }
        
        final OutputStream out2 = new FileOutputStream(entry2);
        try {
            assertFalse(entry2.delete());
            out2.write(data);
            assertFalse(archive.deleteAll());
        } finally {
            out2.close();
        }
        
        final InputStream in1 = new FileInputStream(entry1); // does an auto update!
        try {
            final InputStream in2 = new FileInputStream(entry2);
            try {
                assertTrue(entry2.delete());
                final ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
                try {
                    File.cat(in2, out);
                } finally {
                    out.close();
                }
                assertTrue(Arrays.equals(data, out.toByteArray()));
                assertFalse(archive.deleteAll());
            } finally {
                in2.close();
            }
            
            assertFalse(entry1.delete()); // deleted within archive.deleteAll()!
            final ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
            try {
                File.cat(in1, out);
            } finally {
                out.close();
            }
            assertTrue(Arrays.equals(data, out.toByteArray()));
            assertFalse(archive.deleteAll());
        } finally {
            in1.close();
        }
        
        assertTrue(archive.deleteAll());
        assertFalse(newPlainFile(archive).exists());
    }
    
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OS_OPEN_STREAM")
    @Test
    public final void testRenameValidArchive() throws IOException {
        // Create a regular archive with a single archive entry which
        // contains a creative greeting message.
        final OutputStream out = new FileOutputStream(new File(archive, "entry"));
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
        // Note that archive is a File instance which returns isArchive()
        // == true, so we must create a new File instance which is guaranteed
        // to ignore the archive getSuffix() in the path.
        // Furthermore, data is an array containing random data
        // - not a regular archive.
        // So upon completion of this step, the object "archive" refers to a
        // false positive.
        final File tmp = new File(archive.getPath(), ArchiveDetector.NULL);
        final InputStream in = new ByteArrayInputStream(data);
        try {
            assertTrue(tmp.copyFrom(in));
        } finally {
            in.close(); // ALWAYS close streams!
        }
        
        assertRenameArchiveToTemp(archive);
    }
    
    private void assertRenameArchiveToTemp(final File archive) throws IOException {
        assert archive.isArchive(); // regular archive or false positive
        assert !archive.isEntry(); // not contained in another archive file

        // Create a temporary file.
        File tmp = new File(File.createTempFile(TEMP_FILE_PREFIX, null));
        assertTrue(tmp.delete());
        assertFalse(tmp.exists());
        assertFalse(newPlainFile(tmp).exists());

        // Now rename the archive to the temporary path.
        // Depending on the true state of the object "archive", this will
        // either create a directory (iff archive is a regular archive) or a
        // plain file (iff archive is false positive).
        assertTrue(archive.renameTo(tmp));
        assertFalse(archive.exists());
        assertFalse(newPlainFile(archive).exists());

        // Now delete resulting temporary file.
        assertTrue(tmp.deleteAll());
        assertFalse(tmp.exists());
        assertFalse(newPlainFile(tmp).exists());
    }

    @Test
    public final void testRenameRecursively() throws IOException {
        final File temp = new File(createTempFile());
        final File archive2 = new File(archive, "inner" + getSuffix());
        final File archive3 = new File(archive2, "nuts" + getSuffix());
        final File archive1a = new File(archive, "a");
        final File archive1b = new File(archive, "b");
        final File archive2a = new File(archive2, "a");
        final File archive2b = new File(archive2, "b");
        final File archive3a = new File(archive3, "a");
        final File archive3b = new File(archive3, "b");
        
        assertTrue(temp.delete());
        
        assertCatFrom(archive1a);
        
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
        assertTrue(archive3.delete());
        assertTrue(archive2.delete());
        assertCatTo(archive1a);
        assertTrue(archive1a.delete());
        assertTrue(archive.delete());
    }
    
    private void assertRenameTo(File src, File dst) {
        assertTrue(src.exists());
        if (!src.isEntry())
            assertTrue(newPlainFile(src).exists());
        assertFalse(dst.exists());
        if (!dst.isEntry())
            assertFalse(newPlainFile(dst).exists());
        assertTrue(src.renameTo(dst)); // lenient!
        assertFalse(src.exists());
        if (!src.isEntry())
            assertFalse(newPlainFile(src).exists());
        assertTrue(dst.exists());
        if (!dst.isEntry())
            assertTrue(newPlainFile(dst).exists());
    }
    
    private static java.io.File newPlainFile(final File file) {
        return new java.io.File(file.getPath());
    }
    
    private static final String[] MEMBERS = {
        "A directory member",
        "Another directory member",
        "Yet another directory member",
    };
    
    @Test
    public final void testList() throws IOException {
        final java.io.File dir = createTempFile();
        final File dir2 = new File(dir);

        assertTrue(dir.delete());

        // Create regular directory for testing.
        assertTrue(dir.mkdir());
        for (int i = MEMBERS.length; --i >= 0; )
            assertTrue(new java.io.File(dir, MEMBERS[i]).createNewFile());
        java.io.File[] files = dir.listFiles();
        Arrays.sort(files);
        assertList(files, dir2);
        assertTrue(dir2.deleteAll());

        // Repeat test with regular archive file.
        assertTrue(dir2.mkdir());
        for (int i = MEMBERS.length; --i >= 0; )
            assertTrue(new File(dir2, MEMBERS[i]).createNewFile());
        assertList(files, dir2);
        assertTrue(dir2.deleteAll());
    }

    private void assertList(final java.io.File[] refs, final File dir) {
        final File[] files = dir.listFiles();
        Arrays.sort(files);
        assertEquals(refs.length, files.length);
        for (int i = 0, l = refs.length; i < l; i++) {
            final java.io.File ref = refs[i];
            final File file2 = files[i];
            assertTrue(!(ref instanceof File));
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
        
        assertTrue(archive.deleteAll());
    }
    
    private void createTestArchive(final int nEntries) throws IOException {
        for (int i = 0; i < nEntries; i++) {
            final File entry = new File(archive + File.separator + i);
            final OutputStream out = new FileOutputStream(entry);
            try {
                out.write(data);
            } finally {
                out.close();
            }
        }
    }
    
    private void assertArchiveEntries(final File archive, int nEntries)
    throws IOException {
        final java.io.File[] entries = archive.listFiles();
        assertEquals(nEntries, entries.length);
        final byte[] buf = new byte[4096];
        for (int i = 0, l = entries.length; i < l; i++) {
            final File entry = (File) entries[i];
            // Read full entry and check the contents.
            final InputStream in = new FileInputStream(entry);
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
    }
    
    @Test
    public final void testMultithreadedSingleArchiveMultipleEntriesWriting()
    throws Exception {
        assertMultithreadedSingleArchiveMultipleEntriesWriting(archive, 20, false);
        assertMultithreadedSingleArchiveMultipleEntriesWriting(archive, 20, true);
    }
    
    private void assertMultithreadedSingleArchiveMultipleEntriesWriting(
            final File archive,
            final int nThreads,
            final boolean wait)
            throws Exception {
        assertTrue(File.isLenient());
        
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
                    final File file = new File(archive, i + "");
                    OutputStream out;
                    while (true) {
                        try {
                            out = new FileOutputStream(file);
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
                        File.update(wait, false, wait, false);
                    } catch (ArchiveException ex) {
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
        assertTrue(archive.deleteAll());
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
        assertTrue(File.isLenient());
        
        class WritingThread extends Thread {
            Throwable failure;

            WritingThread() {
                setDaemon(true);
            }

            @Override
            public void run() {
                try {
                    final File archive = new File(createTempFile());
                    assertTrue(archive.delete());
                    final File file = new File(archive, "entry");
                    try {
                        final OutputStream out = new FileOutputStream(file);
                        try {
                            out.write(data);
                        } finally {
                            out.close();
                        }
                        try {
                            if (updateIndividually)
                                File.update(archive);
                            else
                                File.update(false);
                        } catch (ArchiveException ex) {
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
                        assertTrue(archive.deleteAll());
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
