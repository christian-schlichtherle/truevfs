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

package de.schlichtherle.io;

import de.schlichtherle.io.archive.spi.*;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.util.regex.*;

import junit.framework.*;

/**
 * Tests the VFS implementation for a particular archive type.
 * Subclasses must override {@link #setUp}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class FileTestCase extends TestCase {

    private static final Logger logger = Logger.getLogger(
            FileTestCase.class.getName());

    private static final java.io.File _tempDir = new java.io.File(
            System.getProperty("java.io.tmpdir"));

    private static final Matcher _tempMatcher
            = Pattern.compile(UpdatingArchiveController.TEMP_FILE_PREFIX
            + ".*\\" + UpdatingArchiveController.TEMP_FILE_SUFFIX).matcher("");

    private static final Set totalTemps = new HashSet();

    private static final java.io.File _baseDir = _tempDir;

    /** The data to get compressed. */
    private static final byte[] _data = new byte[100 * 1024]; // enough to waste some heat on CPU cycles
    static {
        boolean ea = false;
        assert ea = true; // NOT ea == true !
        logger.config("Java assertions " + (ea ? "enabled." : "disabled!"));
        if (!ea)
            logger.warning("Please enable assertions for additional white box testing.");

        new Random().nextBytes(_data);
        logger.config("Created " + _data.length + " bytes of random data.");
        logger.config("Temp dir for TrueZIP API: " + _tempDir.getPath());
        logger.config("Default temp dir for unit tests: " + _baseDir.getPath());
        logger.config("Free memory: " + mb(Runtime.getRuntime().freeMemory()));
        logger.config("Total memory: " + mb(Runtime.getRuntime().totalMemory()));
        logger.config("Max memory: " + mb(Runtime.getRuntime().maxMemory()));
    }

    private static final String mb(long value) {
        return ((value - 1 + 1024 * 1024) / (1024 * 1024)) + " MB"; // round up
    }
    
    protected byte[] data;
    
    protected java.io.File baseDir;
    
    protected String prefix;
    
    /**
     * The lowercase suffix including the dot which shall be used when
     * creating archive files.
     */
    protected String suffix;
    
    /** The temporary file to use as an archive file. */
    protected File archive;
    
    protected FileTestCase(String testName) {
        super(testName);
        
        File.setDefaultArchiveDetector(ArchiveDetector.DEFAULT);
    }
    
    /**
     * A subclass must override this method to create the {@link #data}
     * to be archived.
     * It must also finally call this superclass implementation to create
     * the temporary file to be used as an archive file.
     */
    protected void setUp() throws Exception {
        if (data == null)
            data = _data; // (byte[]) _data.clone();
        if (baseDir == null)
            baseDir = _baseDir;
        if (prefix == null)
            prefix = "tzp-test";
        if (suffix == null)
            suffix = ".zip";
        if (archive == null) {
            archive = new File(createTempFile(prefix, suffix));
            assertTrue(archive.delete());
        }
        
        File.setLenient(true); // Restore default
    }
    
    protected void tearDown() throws Exception {
        data = null;
        baseDir = null;
        prefix = null;
        suffix = null;
        
        final boolean deleted = archive.delete();
        if (!deleted && archive.exists())
            logger.warning(archive + " (could not delete)");
        archive = null;
        
        // umount now to delete temps and free memory.
        // This prevents subsequent warnings about left over temporary files
        // and removes cached data from the memory, so it helps to start on a
        // clean sheet of paper with subsequent tests.
        try {
            File.umount();
        } catch (ArchiveException ignored) {
            // You should never (!) ignore all exceptions thrown by this method.
            // The reason we do it here is that they are usually after effects
            // of failed tests and we don't want any exception from the tests
            // to be overridden by an exception thrown in this clean up method.
        }
        
        final String[] temps = _tempDir.list(new FilenameFilter() {
            public boolean accept(java.io.File dir, String name) {
                _tempMatcher.reset(name);
                return _tempMatcher.matches();
            }
        });
        assert temps != null;
        for (int i = 0; i < temps.length; i++) {
            if (totalTemps.add(temps[i])) {
                // If the TrueZIP API itself (rather than this test code)
                // leaves a temporary file, then that's considered a bug!
                logger.warning("Bug in TrueZIP API: Temp file found: " + temps[i]);
            }
        }
        
    }

    private static final File createNonArchiveFile(File file) {
        return ArchiveDetector.NULL.createFile(
                file.getParentFile(), file.getName());
    }
    
    public void testParentConstructor() throws Exception {
        // Test normalization and parent+child constructors.
        // This is not yet a comprehensive test.
        
        {
            try {
                new File("x", (String) null);
                fail("Expected NullPointerException!");
            } catch (NullPointerException expected) {
            }
            
            try {
                new File(new File("x"), (String) null);
                fail("Expected NullPointerException!");
            } catch (NullPointerException expected) {
            }
        }
        
        final String fs = File.separator;
        
        {
            final File[] files = {
                new File(archive, ""),
                new File(archive, "."),
                new File(archive, "." + fs),
                new File(archive, "." + fs + "."),
                new File(archive, "." + fs + "." + fs),
                new File(archive, "." + fs + "." + fs + "."),
            };
            for (int i = 0; i < files.length; i++) {
                final File file = files[i];
                assertSame(file, file.getInnerArchive());
                assertEquals("", file.getInnerEntryName());
                assertNull(file.getEnclArchive());
                assertNull(file.getEnclEntryName());
            }
        }
        
        {
            final String innerName = "inner" + suffix;
            final File inner = new File(archive, innerName);
            final File[] files = {
                new File(inner, ""),
                new File(inner, "."),
                new File(inner, "." + fs),
                new File(inner, "." + fs + "."),
                new File(inner, "." + fs + "." + fs),
                new File(inner, "." + fs + "." + fs + "."),
            };
            for (int i = 0; i < files.length; i++) {
                final File file = files[i];
                assertSame(file, file.getInnerArchive());
                assertEquals("", file.getInnerEntryName());
                assertSame(archive, file.getEnclArchive());
                assertEquals(innerName, file.getEnclEntryName());
            }
        }
        
        {
            final String entryName = "entry";
            final File entry = new File(archive, entryName);
            final File[] files = {
                new File(entry, ""),
                new File(entry, "."),
                new File(entry, "." + fs),
                new File(entry, "." + fs + "."),
                new File(entry, "." + fs + "." + fs),
                new File(entry, "." + fs + "." + fs + "."),
            };
            for (int i = 0; i < files.length; i++) {
                final File file = files[i];
                assertSame(archive, file.getInnerArchive());
                assertEquals(entryName, file.getInnerEntryName());
                assertSame(archive, file.getEnclArchive());
                assertEquals(entryName, file.getEnclEntryName());
            }
        }
        
        final File a = new File("outer" + suffix + "/removed" + suffix);
        File b, c;
        
        b = new File("../removed.dir/removed.dir/../../dir/./inner" + suffix);
        c = new File(a, b.getPath());
        assertTrue(c.isArchive());
        assertTrue(c.isEntry());
        assertEquals("outer" + suffix,
                c.getEnclArchive().getPath());
        assertEquals("dir/inner" + suffix,
                c.getEnclEntryName());
        
        b = new File("../removed.dir/removed.dir/../../dir/./inner" + suffix);
        c = new File(a, b.getPath(), ArchiveDetector.NULL);
        assertFalse(c.isArchive());
        assertTrue(c.isEntry());
        assertEquals("outer" + suffix,
                c.getInnerArchive().getPath());
        assertEquals("dir/inner" + suffix,
                c.getInnerEntryName());
        
        b = new File("../removed.dir/removed.dir/../../dir/./inner"
                + suffix + "/removed.dir/removed.dir/../../dir/./test.txt");
        c = new File(a, b.getPath());
        assertFalse(c.isArchive());
        assertTrue(c.isEntry());
        assertEquals("outer" + suffix + fs + "removed" + suffix + fs + ".."
                + fs + "removed.dir" + fs + "removed.dir" + fs + ".." + fs
                + ".." + fs + "dir" + fs + "." + fs + "inner" + suffix,
                c.getInnerArchive().getPath());
        assertEquals("dir/inner" + suffix,
                c.getInnerArchive().getEnclEntryName());
    }
    
    public void testSerialization() throws IOException, ClassNotFoundException {
        // Preamble.
        final File inner = new File(archive, "inner" + suffix);
        assertTrue(archive.isArchive());
        assertTrue(inner.isArchive());
        
        // Serialize.
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(inner);
        out.close();
        
        // Deserialize.
        final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        final ObjectInputStream in = new ObjectInputStream(bis);
        final File inner2 = (File) in.readObject();
        final File archive2 = (File) inner2.getParentFile();
        in.close();
        
        assertNotSame(inner, inner2);
        assertNotSame(archive, archive2);
        
        //
        // Test details of the persistet object graph - part of this is
        // repeated in the tests for DefaultArchiveDetector.
        //
        
        // Assert that controllers haven't been persistet.
        final ArchiveController innerController = inner.getArchiveController();
        final ArchiveController archiveController = archive.getArchiveController();
        final ArchiveController inner2Controller = inner2.getArchiveController();
        final ArchiveController archive2Controller = archive2.getArchiveController();
        assertSame(innerController, inner2Controller);
        assertSame(archiveController, archive2Controller);
        
        // Assert that detectors have been persistet.
        final ArchiveDetector innerDetector = inner.getArchiveDetector();
        final ArchiveDetector archiveDetector = archive.getArchiveDetector();
        final ArchiveDetector inner2Detector = inner2.getArchiveDetector();
        final ArchiveDetector archive2Detector = archive2.getArchiveDetector();
        assertNotSame(innerDetector, inner2Detector);
        assertNotSame(archiveDetector, archive2Detector);
        
        // Assert that drivers have been persistet.
        final ArchiveDriver innerDriver = innerDetector.getArchiveDriver(inner.getPath());
        final ArchiveDriver archiveDriver = archiveDetector.getArchiveDriver(archive.getPath());
        final ArchiveDriver inner2Driver = inner2Detector.getArchiveDriver(inner2.getPath());
        final ArchiveDriver archive2Driver = archive2Detector.getArchiveDriver(archive2.getPath());
        assertNotSame(innerDriver, inner2Driver);
        assertNotSame(archiveDriver, archive2Driver);
        
        // Test that the controllers have been reconfigured with the new drivers.
        // Note that this is only possible because the file systems haven't
        // been touched yet (well, they haven't even been mounted).
        final ArchiveDriver innerControllerDriver = innerController.getDriver();
        final ArchiveDriver archiveControllerDriver = archiveController.getDriver();
        assertSame(innerControllerDriver, inner2Driver);
        assertSame(archiveControllerDriver, archive2Driver);
    }
    
    public void testGetOutermostArchive() {
        File file = new File("abc/def" + suffix + "/efg" + suffix + "/hij" + suffix + "/test.txt");
        assertEquals(new java.io.File("abc/def" + suffix), file.getTopLevelArchive());
    }

    public void testFalsePositives() throws IOException {
        testFalsePositive(archive);

        // Dito for entry.
        final File entry = new File(archive, "entry" + suffix);

        assertTrue(archive.mkdir());
        testFalsePositive(entry);
        assertTrue(archive.delete());

        assertTrue(createNonArchiveFile(archive).mkdir());
        testFalsePositive(entry);
        assertTrue(archive.delete());
    }

    void testFalsePositive(File file) throws IOException {
        assert file.isArchive();

        // Note that file's parent directory may be a false positive directory!

        // Create false positive file.

        OutputStream os = new FileOutputStream(file);
        try {
            os.write(data);
        } finally {
            os.close();
        }

        assertTrue(file.exists());
        assertFalse(file.isDirectory());
        assertTrue(file.isFile());
        assertEquals(data.length, file.length());
        assertTrue(file.lastModified() > 0);

        testDelete(file);

        // Create false positive directory.

        assertTrue(createNonArchiveFile(file).mkdir());
        assertTrue(file.exists());
        assertTrue(file.isDirectory());
        assertFalse(file.isFile());
        //assertEquals(0, file.length());
        assertTrue(file.lastModified() > 0);

        testDelete(file);

        // Create regular archive file.

        assertTrue(file.mkdir());
        assertTrue(createNonArchiveFile(file).isFile());
        assertTrue(file.exists());
        assertTrue(file.isDirectory());
        assertFalse(file.isFile());
        //assertEquals(0, file.length());
        assertTrue(file.lastModified() > 0);

        testDelete(file);
    }

    void testDelete(File file) throws IOException {
        assertTrue(file.delete());
        assertFalse(file.exists());
        assertFalse(file.isDirectory());
        assertFalse(file.isFile());
        assertEquals(0, file.length());
        assertFalse(file.lastModified() > 0);
    }

    public void testCreateNewFile()
    throws IOException{
        createNewPlainFile();
        createNewSmartFile();
    }
    
    void createNewPlainFile()
    throws IOException {
        final java.io.File archive = createTempFile(prefix, suffix);
        assertTrue(archive.delete());
        final java.io.File file1 = new java.io.File(archive, "test.txt");
        final java.io.File file2 = new java.io.File(file1, "test.txt");
        try {
            file1.createNewFile();
            fail("Creating a file in a non-existent directory should throw an IOException!");
        } catch (IOException expected) {
        }
        testCreateNewFile(archive, file1, file2);
    }
    
    void createNewSmartFile()
    throws IOException {
        final java.io.File file1 = new File(archive, "test.txt");
        final java.io.File file2 = new File(file1, "test.txt");
        
        File.setLenient(false);
        try {
            file1.createNewFile();
            fail("Creating a file in a non-existent directory should throw an IOException!");
        } catch (IOException ok) {
            // This is exactly what we expect here!
        }
        testCreateNewFile(archive, file1, file2);
        
        File.setLenient(true);
        testCreateNewFile(archive, file1, file2);
    }
    
    void testCreateNewFile(
            final java.io.File dir,
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

    public void testIllegalDirectoryOperations() throws IOException {
        final String[] names = {
            "inner" + suffix,
            "dir",
        };
        File file = archive;
        for (int i = 0; i <= names.length; i++) {
            final File file2 = createNonArchiveFile(file);
            assertTrue(file2.mkdir());
            testIllegalDirectoryOperations(file2);
            assertTrue(file2.delete());
            assertTrue(file.mkdir());
            testIllegalDirectoryOperations(file);
            if (i < names.length)
                file = new File(file, names[i]);
        }
        assertTrue(archive.deleteAll());
    }

    private void testIllegalDirectoryOperations(final File dir) throws IOException {
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
        java.io.File tmp = createTempFile(prefix, ".tmp");
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
                tmp.deleteOnExit();
        }
    }

    public void testStrictFileOutputStream()
    throws IOException {
        File file = new File(archive, "test.txt");

        File.setLenient(false);
        try {
            testFileOutputStream(file);
            fail("Creating ghost directories should not be allowed when File.isLenient() is false!");
        } catch (FileNotFoundException expected) {
        }

        assertTrue(archive.mkdir());
        testFileOutputStream(file);
        assertTrue(archive.delete());
    }
    
    public void testLenientFileOutputStream()
    throws IOException {
        File file = new File(archive, "dir/inner" + suffix + "/dir/test.txt");
        
        testFileOutputStream(file);
        
        assertFalse(archive.delete()); // directory not empty!
        File.umount(); // allow external modifications!
        assertTrue(new java.io.File(archive.getPath()).delete()); // use plain file to delete instead!
        assertFalse(archive.exists());
        assertFalse(archive.isDirectory());
        assertFalse(archive.isFile());
        assertEquals(0, archive.length());
    }
    
    void testFileOutputStream(File file)
    throws IOException {
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
        
        assertTrue(file.delete());
        assertFalse(file.exists());
        assertFalse(file.isDirectory());
        assertFalse(file.isFile());
        assertEquals(0, file.length());
    }
    
    public void testBusyFileInputStream()
    throws IOException {
        File file1 = new File(archive, "file1");
        File file2 = new File(archive, "file2");
        
        // Test open output streams.
        assertTrue(file1.createNewFile());
        File.update(); // ensure file1 is really present in the archive file
        assertTrue(file2.createNewFile());
        FileInputStream fisA = new FileInputStream(file1);
        try {
            FileInputStream fisB = new FileInputStream(file2);
            fail("Accessing file2 was expected to fail because an auto update needs to be done but the archive file is busy on input for fis1!");
        } catch (FileBusyException expected) {
        }
        assertFalse(file2.catFrom(fisA)); // fails for same reason.
        
        // fis1 is still open!
        try {
            File.update(); // forces closing of fis1
            fail("ArchiveWarningException expected!");
        } catch (ArchiveBusyWarningException expected) {
            // Warning about fis1 still being used.
        }
        assertTrue(file2.isFile());
        if (!file2.catFrom(fisA)) // fis1 may be invalidated after update!
            assertFalse(file2.exists()); // previous op has removed file2!
        
        // Open file2 as stream and let the garbage collection close the stream automatically.
        FileInputStream fisB = new FileInputStream(file1);
        //fis2.close();
        fisB = null;
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
        
        // This update should complete without any exception if the garbage
        // collector did his job.
        try {
            File.umount(); // allow external modifications!
        } catch (ArchiveBusyWarningException failure) {
            fail("The garbage collector hasn't been collecting an open stream. If this is only happening occasionally, you can safely ignore it.");
        }
        
        assertTrue(createNonArchiveFile(archive).delete());
        // Closing the invalidated stream explicitly should be OK.
        fisA.close();
        
        // Cleanup.
        assertFalse(file2.delete()); // already deleted externally
        assertFalse(file2.exists());
        assertFalse(file1.delete());
        assertFalse(file1.exists());
    }
    
    public void testBusyFileOutputStream()
    throws IOException {
        File file1 = new File(archive, "file1");
        File file2 = new File(archive, "file2");
        
        // Ensure that there are two entries in the archive.
        // This is used later to check whether the update operation knows
        // how to deal with updating an archive for which there is still
        // an open output stream.
        FileOutputStream fosA = new FileOutputStream(file1);
        File.cat(new ByteArrayInputStream(data), fosA);
        fosA.close();
        
        fosA = new FileOutputStream(file2);
        File.cat(new ByteArrayInputStream(data), fosA);
        fosA.close();
        
        File.update(); // ensure two entries in the archive
        
        fosA = new FileOutputStream(file1);
        File.cat(new ByteArrayInputStream(data), fosA);
        
        // fosA is still open!
        try {
            FileOutputStream fosB = new FileOutputStream(file1);
        } catch (FileBusyException busy) {
            // This is actually an implementation detail which may change in
            // a future version.
            assertTrue(busy.getCause() instanceof ArchiveBusyException);
        }
        
        // fosA is still open!
        try {
            FileOutputStream fosB = new FileOutputStream(file2);
        } catch (FileBusyException busy) {
            logger.warning("This archive driver does NOT support concurrent writing of different entries in the same archive file.");
        }
        
        // fosA is still open!
        File.cat(new ByteArrayInputStream(data), fosA); // write again
        
        try {
            File.update(); // forces closing of all streams
            fail("Output stream should have been forced to close!");
        } catch (ArchiveBusyWarningException expected) {
        }
        
        try {
            File.cat(new ByteArrayInputStream(data), fosA); // write again
            fail("Output stream should have been forcibly closed!");
        } catch (IOException expected) {
        }
        
        // The stream has been forcibly closed by File.update().
        // Another close is OK, though!
        fosA.close();
        
        // Reopen stream and let the garbage collection close the stream automatically.
        fosA = new FileOutputStream(file1);
        fosA = null;
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
        
        // This update should complete without any exception if the garbage
        // collector did his job.
        try {
            File.update();
        } catch (ArchiveBusyWarningException failure) {
            fail("The garbage collector hasn't been collecting an open stream. If this is only happening occasionally, you can safely ignore it.");
        }
        
        // Cleanup.
        assertTrue(file2.delete());
        assertFalse(file2.exists());
        assertTrue(file1.delete());
        assertFalse(file1.exists());
    }
    
    public void testMkdir()
    throws IOException {
        final File dir1 = archive;
        final File dir2 = new File(dir1, "dir");
        final File dir3 = new File(dir2, "inner" + suffix);
        final File dir4 = new File(dir3, "dir");
        final File dir5 = new File(dir4, "nuts" + suffix);
        final File dir6 = new File(dir5, "dir");
        
        File.setLenient(true);
        
        assertTrue(dir6.mkdir()); // create all at once! note archive is in current directory!
        
        assertFalse(dir6.mkdir()); // exists already!
        assertFalse(dir5.mkdir()); // exists already!
        assertFalse(dir4.mkdir()); // exists already!
        assertFalse(dir3.mkdir()); // exists already!
        assertFalse(dir2.mkdir()); // exists already!
        assertFalse(dir1.mkdir()); // exists already!
        
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
    
    public void testDirectoryTree()
    throws IOException {
        testDirectoryTree(
                new File(System.getProperty("java.io.tmpdir")), // base directory
                new File("dir/inner" + suffix + "/dir/outer" + suffix + "/" + archive.getName())); // this path is reversed!!!
    }
    
    void testDirectoryTree(File basePath, File reversePath)
    throws IOException {
        if (reversePath == null) {
            // We're at the leaf of the directory tree.
            final File test = new File(basePath, "test.txt");
            //testCreateNewFile(basePath, test);
            testFileOutputStream(test);
            return;
        }
        assertFalse(".".equals(reversePath.getPath()));
        assertFalse("..".equals(reversePath.getPath()));
        
        final File member = new File(basePath, reversePath.getName());
        final boolean created = member.mkdir();
        final File children = (File) reversePath.getParentFile();
        testDirectoryTree(member, children);
        testListFiles(basePath, member.getName());
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
    
    void testListFiles(File dir, String entry) {
        java.io.File[] files = dir.listFiles();
        assertTrue(files instanceof File[]);
        boolean found = false;
        for (int i = 0, l = files.length; i < l; i++) {
            File file = (File) files[i];
            assertTrue(file instanceof File);
            if (file.getName().equals(entry))
                found = true;
        }
        if (!found)
            fail("No such entry: " + entry);
    }
    
    public void testCat()
    throws IOException {
        try {
            testCat(archive);
            fail("Writing to files with an archive file suffix should not be possible!");
        } catch (AssertionFailedError ok) {
            // This is exactly what we expect here!
        }
        
        final File archiveTest = new File(archive, "test");
        testCat(archiveTest);
        
        final File archive2 = new File(archive, "inner" + suffix);
        final File archive2Test = new File(archive2, "test");
        testCat(archive2Test);
        assertTrue(archive2.delete());
        assertTrue(archive.delete());
    }
    
    void testCat(final File file)
    throws IOException {
        testCatFrom(file);
        testCatTo(file);
        assertTrue(file.delete());
    }
    
    void testCatFrom(final File file)
    throws IOException {
        final InputStream in = new ByteArrayInputStream(data);
        try {
            assertTrue(file.catFrom(in));
        } finally {
            in.close();
        }
        assertEquals(data.length, file.length());
    }
    
    void testCatTo(final File file)
    throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
        try {
            assertTrue(file.catTo(out));
        } finally {
            out.close();
        }
        assertTrue(Arrays.equals(data, out.toByteArray()));
    }
    
    public void testCopyContainingOrSameFiles() throws IOException {
        assert !archive.exists();
        
        final File dir = (File) archive.getParentFile();
        assertNotNull(dir);
        final File entry = new File(archive, "entry");
        
        testCopyContainingOrSameFiles1(dir, archive);
        testCopyContainingOrSameFiles1(archive, entry);
        
        assertTrue(entry.catFrom(new ByteArrayInputStream(data)));
        
        testCopyContainingOrSameFiles1(dir, archive);
        testCopyContainingOrSameFiles1(archive, entry);
        
        assertTrue(archive.deleteAll());
    }
    
    public void testCopyContainingOrSameFiles1(
            final File a,
            final File b)
            throws IOException {
        testCopyContainingOrSameFiles2(a, b);
        testCopyContainingOrSameFiles2(a.getCanOrAbsFile(), b);
        testCopyContainingOrSameFiles2(a, b.getCanOrAbsFile());
        testCopyContainingOrSameFiles2(a.getCanOrAbsFile(), b.getCanOrAbsFile());
    }
    
    public void testCopyContainingOrSameFiles2(
            final File a,
            final File b)
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

    public void testCopyDelete() throws IOException {
        final String[] names = {
            "0" + suffix,
            "1" + suffix,
            "2" + suffix,
        };

        assertTrue(archive.mkdir());
        testCopyDelete(archive, names, 0);
        assertTrue(archive.delete());

        assertTrue(createNonArchiveFile(archive).mkdir());
        testCopyDelete(archive, names, 0);
        assertTrue(archive.delete());
    }

    public void testCopyDelete(final File parent, String[] names, int off)
    throws IOException {
        if (off >= names.length)
            return;
        
        final File dir = new File(parent, names[off]);

        assertTrue(dir.mkdir()); // create valid archive file
        testCopyDelete(parent, dir);
        testCopyDelete(dir, names, off + 1); // continue recursion
        assertTrue(dir.delete());

        assertTrue(createNonArchiveFile(dir).mkdir()); // create false positive archive file
        testCopyDelete(parent, dir);
        testCopyDelete(dir, names, off + 1); // continue recursion
        assertTrue(dir.delete());
    }

    private void testCopyDelete(final File parent, final File dir) throws IOException {
        final File parentA = new File(parent, "a");
        final File parentB = new File(parent, "b" + suffix);
        final File dirA = new File(dir, "a");
        final File dirB = new File(dir, "b" + suffix);

        testCopyDelete0(dirA, dirB);
        testCopyDelete0(dirA, parentA);
        testCopyDelete0(dirA, parentB);
        testCopyDelete0(parentA, dirA);
        testCopyDelete0(parentA, dirB);
        testCopyDelete0(parentB, dirA);
        testCopyDelete0(parentB, dirB);
        testCopyDelete0(dirB, dirA);
        testCopyDelete0(dirB, parentA);
        testCopyDelete0(dirB, parentB);
    }

    final void testCopyDelete0(File a, File b)
    throws IOException {
        testCopyDelete0(a, b, 2000); // works in all archive types currently supported
    }

    void testCopyDelete0(final File a, final File b, final long granularity)
    throws IOException {
        // Create file a with old timestamp.
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
    
    public void testListPerformance()
    throws IOException {
        assertTrue(archive.mkdir());
        
        int i, j;
        long time;
        
        time = System.currentTimeMillis();
        for (i = 0; i < 100; i++) {
            File file = new File(archive, "" + i);
            assertTrue(file.createNewFile());
        }
        time = System.currentTimeMillis() - time;
        logger.finer("Time required to create " + i + " archive file entries: " + time + "ms");
        
        time = System.currentTimeMillis();
        for (j = 0; j < 100; j++) {
            archive.listFiles((FilenameFilter) null);
        }
        time = System.currentTimeMillis() - time;
        logger.finer("Time required to list these entries " + j + " times using a nullary FilenameFilter: " + time + "ms");
        
        time = System.currentTimeMillis();
        for (j = 0; j < 100; j++) {
            archive.listFiles((FileFilter) null);
        }
        time = System.currentTimeMillis() - time;
        logger.finer("Time required to list these entries " + j + " times using a nullary FileFilter: " + time + "ms");
        
        assertFalse(archive.delete()); // directory not empty!
        File.umount(); // allow external modifications!
        assertTrue(new java.io.File(archive.getPath()).delete()); // use plain file to delete instead!
        assertFalse(archive.exists());
        assertFalse(archive.isDirectory());
        assertFalse(archive.isFile());
        assertEquals(0, archive.length());
    }
    
    // This test needs to be redesigned: It primarily lacks a clear intention.
    /*public void testGarbageCollection()
    throws Throwable {
        logger.fine("testGarbageCollection");
     
        // Preamble:
        {
            Object obj = new Object();
            Reference ref = new WeakReference(obj);
            obj = null;
            System.gc(); // Assumption doesn't work without this!
            //System.runFinalization(); // doesn't work!
            assert ref.get() == null;
        }
     
        // Test:
        try {
            File file = new File(archive);
            for (int i = 0; i < 100; i++) {
                file = new File(file, i + suffix);
                assertTrue(file.mkdir());
            }
        } catch (Throwable failure) {
            // This could be an OOME with CBZip2OutputStream from Ant 1.7.0RC1!
            File.umount(); // exceptions thrown here take precedence!
            throw failure;
        }
        // Note that though the file chain is now eligible for garbage
        // collection, the associated archive controllers are not since they
        // have dirty file systems.
     
        // Now set the system under stress so that the garbage collector will
        // most likely reclaim the chain of file objects and the archive
        // controllers once they have been updated.
        // Note that ArchiveController.finalize() is called in no particular
        // order, i.e. the object graph is completely ignored! :-o
        byte[] buf1 = new byte[10 * 1024 * 1024];
        System.gc();
        byte[] buf2 = new byte[10 * 1024 * 1024];
        File.umount(); // allow external modifications!
        byte[] buf3 = new byte[10 * 1024 * 1024];
        System.gc();
        byte[] buf4 = new byte[10 * 1024 * 1024];
     
        assertTrue(archive.deleteAll());
    }*/
    
    public void testIllegalDeleteEntryWithOpenStream() throws IOException {
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
        assertFalse(getPlainFile(archive).exists());
    }
    
    public void testRenameValidArchive()
    throws IOException {
        // Create a regular archive with a single archive entry which
        // contains a creative greeting message.
        final OutputStream out = new FileOutputStream(new File(archive, "entry"));
        try {
            new PrintStream(out, true).println("Hello World!");
        } finally {
            out.close(); // ALWAYS close streams!
        }
        
        testRenameArchiveToTemp(archive);
    }
    
    public void testRenameFalsePositive()
    throws IOException {
        // Create false positive archive.
        // Note that archive is a File instance which returns isArchive()
        // == true, so we must create a new File instance which is guaranteed
        // to ignore the archive suffix in the path.
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
        
        testRenameArchiveToTemp(archive);
    }
    
    public void testRenameArchiveToTemp(final File archive)
    throws IOException {
        assert archive.isArchive(); // regular archive or false positive
        assert !archive.isEntry(); // not enclosed in another archive
        
        // Create a temporary path.
        File tmp = new File(createTempFile(prefix, null));
        assertTrue(tmp.delete());
        assertFalse(tmp.exists());
        assertFalse(getPlainFile(tmp).exists());
        
        // Now rename the archive to the temporary path.
        // Depending on the true state of the object "archive", this will
        // either create a directory (iff archive is a regular archive) or a
        // plain file (iff archive is false positive).
        assertTrue(archive.renameTo(tmp));
        assertFalse(archive.exists());
        assertFalse(getPlainFile(archive).exists());
        
        // Now delete resulting temporary file.
        assertTrue(tmp.deleteAll());
        assertFalse(tmp.exists());
        assertFalse(getPlainFile(tmp).exists());
    }
    
    public void testRenameRecursively()
    throws IOException {
        final File temp = new File(createTempFile(prefix, suffix));
        final File archive2 = new File(archive, "inner" + suffix);
        final File archive3 = new File(archive2, "nuts" + suffix);
        final File archive1a = new File(archive, "a");
        final File archive1b = new File(archive, "b");
        final File archive2a = new File(archive2, "a");
        final File archive2b = new File(archive2, "b");
        final File archive3a = new File(archive3, "a");
        final File archive3b = new File(archive3, "b");
        
        assertTrue(temp.delete());
        
        testCatFrom(archive1a);
        
        for (int i = 2; i >= 1; i--) {
            testRenameTo(archive1a, archive1b);
            testRenameTo(archive1b, archive2a);
            testRenameTo(archive2a, archive2b);
            testRenameTo(archive2b, archive3a);
            testRenameTo(archive3a, archive3b);
            testRenameTo(archive3b, archive3a);
            testRenameTo(archive3a, archive2b);
            testRenameTo(archive2b, archive2a);
            testRenameTo(archive2a, archive1b);
            testRenameTo(archive1b, archive1a);
        }
        
        testRenameTo(archive, temp);
        testRenameTo(temp, archive);
        assertTrue(archive3.delete());
        assertTrue(archive2.delete());
        testCatTo(archive1a);
        assertTrue(archive1a.delete());
        assertTrue(archive.delete());
    }
    
    void testRenameTo(File src, File dst) {
        assertTrue(src.exists());
        if (!src.isEntry())
            assertTrue(getPlainFile(src).exists());
        assertFalse(dst.exists());
        if (!dst.isEntry())
            assertFalse(getPlainFile(dst).exists());
        assertTrue(src.renameTo(dst)); // !strict
        assertFalse(src.exists());
        if (!src.isEntry())
            assertFalse(getPlainFile(src).exists());
        assertTrue(dst.exists());
        if (!dst.isEntry())
            assertTrue(getPlainFile(dst).exists());
    }
    
    private static java.io.File getPlainFile(final File file) {
        return new java.io.File(file.getPath());
    }
    
    private static final String[] members = {
        "A directory member",
        "Another directory member",
        "Yet another directory member",
    };
    
    public void testListFiles() throws IOException {
        java.io.File dir = createTempFile(prefix, suffix);
        File dir2 = new File(dir);
        
        assertTrue(dir.delete());
        assertTrue(dir.mkdir());
        
        for (int i = members.length; --i >= 0; ) {
            assertTrue(new java.io.File(dir, members[i]).createNewFile());
        }
        
        java.io.File[] files = dir.listFiles();
        java.io.File[] files2 = dir2.listFiles();
        
        assertEquals(files.length, files2.length);
        
        for (int i = 0, l = files.length; i < l; i++) {
            assertTrue(!(files[i] instanceof File));
            assertTrue(files2[i] instanceof File);
            assertEquals(files[i].getPath(), files2[i].getPath());
        }
        
        assertTrue(dir2.deleteAll());
    }
    
    public void testMultithreadedSingleArchiveMultipleEntriesReading()
    throws Exception {
        testMultithreadedSingleArchiveMultipleEntriesReading(20, 20);
    }
    
    /**
     * Creates a test archive file with the given number of entries and then
     * creates the given number of threads where each of them reads all these
     * entries.
     *
     * @param nEntries The number of archive file entries to be created.
     * @param nThreads The number of threads to be created.
     */
    private void testMultithreadedSingleArchiveMultipleEntriesReading(final int nEntries, final int nThreads)
    throws Exception {
        // Create test archive file.
        createTestArchive(nEntries);
        
        // Define thread class to enumerate and read all entries.
        class CheckAllEntriesThread extends Thread {
            Throwable failure;
            
            public void run() {
                try {
                    checkArchiveEntries(archive, nEntries);
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
    
    private void checkArchiveEntries(final File archive, int nEntries)
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
                    assertTrue(de.schlichtherle.util.Arrays.equals(
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
    
    public void testMultithreadedSingleArchiveMultipleEntriesWriting()
    throws Exception {
        testMultithreadedSingleArchiveMultipleEntriesWriting(archive, 20, false);
        testMultithreadedSingleArchiveMultipleEntriesWriting(archive, 20, true);
    }
    
    private void testMultithreadedSingleArchiveMultipleEntriesWriting(
            final File archive,
            final int nThreads,
            final boolean wait)
            throws Exception {
        assertTrue(File.isLenient());
        
        class WritingThread extends Thread {
            final int i;
            Throwable failure;
            
            WritingThread(int i) {
                this.i = i;
            }
            
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
                    } catch (ArchiveBusyException mayHappen) {
                        // Some other thread is busy updating an archive.
                        // If we are waiting, then this could never happen.
                        // Otherwise, silently ignore this exception and
                        // accept that the archive may not have been
                        // updated to disk.
                        // Note that no data is lost, this exception just
                        // signals that the corresponding archive hasn't
                        // been updated - a future call may still succeed.
                        if (wait)
                            throw new AssertionError(mayHappen);
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
        
        checkArchiveEntries(archive, nThreads);
        assertTrue(archive.deleteAll());
    }
    
    public void testMultithreadedMultipleArchivesSingleEntryWriting()
    throws Exception {
        testMultithreadedMultipleArchivesSingleEntryWriting(20, false);
        testMultithreadedMultipleArchivesSingleEntryWriting(20, true);
    }
    
    private void testMultithreadedMultipleArchivesSingleEntryWriting(
            final int nThreads, final boolean updateIndividually)
            throws Exception {
        assertTrue(File.isLenient());
        
        class WritingThread extends Thread {
            Throwable failure;
            
            public void run() {
                try {
                    final File archive = new File(createTempFile(prefix, suffix));
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
                        } catch (ArchiveBusyException mayHappen) {
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
                                throw new AssertionError(mayHappen);
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
    
    private java.io.File createTempFile(
            String prefix,
            String suffix)
            throws IOException {
        return File.createTempFile(prefix, suffix, baseDir).getCanonicalFile();
    }
}
