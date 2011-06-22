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
package de.schlichtherle.truezip.nio.file;

import de.schlichtherle.truezip.io.Streams;
import java.util.List;
import java.util.LinkedList;
import java.nio.file.DirectoryStream;
import de.schlichtherle.truezip.file.TFile;
import java.nio.file.Files;
import static java.nio.file.Files.*;
import java.nio.file.Path;
import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TConfig;
import de.schlichtherle.truezip.file.TFileTestSuite;
import de.schlichtherle.truezip.socket.spi.ByteArrayIOPoolService;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import de.schlichtherle.truezip.util.ArrayHelper;
import de.schlichtherle.truezip.fs.FsSyncException;
import de.schlichtherle.truezip.fs.FsSyncWarningException;
import de.schlichtherle.truezip.io.FileBusyException;
import de.schlichtherle.truezip.fs.archive.FsArchiveDriver;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.socket.OutputClosedException;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Performs a functional test of a particular FsArchiveDriver by using the
 * API of the TrueZIP Path* module.
 *
 * @see     TFileTestSuite Test suite for the TrueZIP File* API.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class TPathTestSuite extends TestBase {

    private static final Logger logger = Logger.getLogger(
            TPathTestSuite.class.getName());

    private static final String TEMP_FILE_PREFIX = "tzp";

    private static final Random rnd = new Random();

    /** The data to get compressed. */
    private static final byte[] DATA = new byte[1024]; // enough to waste some heat on CPU cycles
    static {
        rnd.nextBytes(DATA);
    }

    protected static final IOPoolProvider
            IO_POOL_PROVIDER = new ByteArrayIOPoolService(4 * DATA.length / 3); // account for archive file type specific overhead
    
    private final FsScheme scheme;

    private Path temp;
    private TPath archive;
    private byte[] data;

    protected TPathTestSuite(   final @NonNull FsScheme scheme,
                                final @NonNull FsArchiveDriver<?> driver) {
        if (null == scheme || null == driver)
            throw new NullPointerException();
        this.scheme = scheme;
        this.detector = new TArchiveDetector(scheme.toString(), driver);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        temp = createTempFile();
        delete(temp);
        archive = new TPath(temp);
        data = DATA.clone();
    }

    private Path createTempFile() throws IOException {
        // TODO: Removing .getCanonicalFile() causes archive.toFile().rm_r() to
        // fail in testCopyContainingOrSameFiles() - explain why!
        return Files.createTempFile(TEMP_FILE_PREFIX, getSuffix()).toRealPath();
    }

    protected final TPath getArchive() {
        return archive;
    }

    protected final String getSuffix() {
        return "." + scheme;
    }

    @After
    @Override
    public void tearDown() throws Exception {
        try {
            this.archive = null;

            // sync now to delete temps and free memory.
            // This prevents subsequent warnings about left over temporary files
            // and removes cached data from the memory, so it helps to start on a
            // clean sheet of paper with subsequent tests.
            try {
                TFileSystem.umount();
            } catch (FsSyncException ex) {
                logger.log(Level.WARNING, ex.toString(), ex);
            }

            if (exists(temp)) {
                try {
                    delete(temp);                
                } catch (IOException ex) {
                    logger.log(Level.WARNING, "{0} (could not delete)", temp);
                }
            }
        } finally {
            super.tearDown();
        }
    }

    protected static TPath newNonArchiveFile(TPath path) {
        TConfig config = TConfig.push();
        try {
            config.setArchiveDetector(TArchiveDetector.NULL);
            TPath parent = path.getParent();
            return null == parent
                    ? new TPath(path)
                    : parent.resolve(path.getFileName());
        } finally {
            config.close();
        }
    }

    private static void gc() {
        System.gc();
        try {
            Thread.sleep(50);
        } catch (InterruptedException ex) {
            Logger.getLogger(TPathTestSuite.class.getName()).log(Level.WARNING, "Current thread was interrupted while waiting!", ex);
        }
    }

    @Test
    public final void testFalsePositives() throws IOException {
        assertFalsePositive(archive);

        // Dito for entry.
        final TPath entry = archive.resolve("entry" + getSuffix());

        createDirectory(archive);
        assertFalsePositive(entry);
        delete(archive);

        createDirectory(newNonArchiveFile(archive));
        assertFalsePositive(entry);
        delete(archive);
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    private void assertFalsePositive(final TPath file) throws IOException {
        assert file.isArchive();

        // Note that file's parent directory may be a directory in the host file system!

        // Create file false positive.
        {
            OutputStream out = newOutputStream(file);
            try {
                out.write(data);
            } finally {
                out.close();
            }
        }

        // Overwrite.
        {
            OutputStream out = newOutputStream(file);
            try {
                out.write(data);
            } finally {
                out.close();
            }
        }

        assertTrue(exists(file));
        assertFalse(isDirectory(file));
        assertTrue(isRegularFile(file));
        assertEquals(data.length, size(file));
        assertTrue(getLastModifiedTime(file).toMillis() > 0);

        // Read back portion
        {
            InputStream in = newInputStream(file);
            try {
                byte[] buf = new byte[data.length];
                assertTrue(ArrayHelper.equals(data, 0, buf, 0, in.read(buf)));
            } finally {
                in.close();
            }
        }
        assertRm(file);

        // Create directory false positive.

        createDirectory(newNonArchiveFile(file));
        assertTrue(exists(file));
        assertTrue(isDirectory(file));
        assertFalse(isRegularFile(file));
        //assertEquals(0, file.getLength());
        assertTrue(getLastModifiedTime(file).toMillis() > 0);

        try {
            newInputStream(archive);
            fail();
        } catch (IOException expected) {
        }

        try {
            newOutputStream(archive);
            fail();
        } catch (IOException expected) {
        }

        assertRm(file);

        // Create regular archive file.

        createDirectory(file);
        assertTrue(isRegularFile(newNonArchiveFile(file)));
        assertTrue(exists(file));
        assertTrue(isDirectory(file));
        assertFalse(isRegularFile(file));
        //assertEquals(0, file.getLength());
        assertTrue(getLastModifiedTime(file).toMillis() > 0);

        try {
            newInputStream(archive);
            fail();
        } catch (IOException expected) {
        }

        try {
            newOutputStream(archive);
            fail();
        } catch (IOException expected) {
        }

        assertRm(file);
    }

    private void assertRm(final TPath file) throws IOException {
        delete(file);
        assertFalse(exists(file));
        assertFalse(isDirectory(file));
        assertFalse(isRegularFile(file));
        try {
            size(file);
            fail();
        } catch (NoSuchFileException expected) {
        }
        try {
            getLastModifiedTime(file);
            fail();
        } catch (NoSuchFileException expected) {
        }
    }

    @Test
    public final void testCreateNewFile() throws IOException{
        assertCreateNewPlainFile();
        assertCreateNewEnhancedFile();
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    private void assertCreateNewPlainFile() throws IOException {
        final Path archive = createTempFile();
        delete(archive);
        final Path file1 = archive.resolve("test.txt");
        final Path file2 = file1.resolve("test.txt");
        try {
            createFile(file1);
            fail("Creating a file in a non-existent directory should throw an IOException!");
        } catch (IOException expected) {
        }
        assertCreateNewFile(archive, file1, file2);
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    private void assertCreateNewEnhancedFile() throws IOException {
        final TPath file1 = archive.resolve("test.txt");
        final TPath file2 = file1.resolve("test.txt");
        TConfig config = TConfig.push();
        try {
            config.setLenient(false);
            try {
                createFile(file1);
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
    private void assertCreateNewFile(   final Path dir,
                                        final Path file1,
                                        final Path file2)
    throws IOException {
        assertFalse(exists(dir));
        
        createDirectory(dir);
        assertTrue(exists(dir));
        assertTrue(isDirectory(dir));
        assertFalse(isRegularFile(dir));
        assertEquals(0, size(dir));
        
        createFile(file1);
        assertTrue(exists(file1));
        assertFalse(isDirectory(file1));
        assertTrue(isRegularFile(file1));
        assertEquals(0, size(file1));
        
        try {
            createFile(file2);
            fail("Creating a file in another file should throw an IOException!");
        } catch (IOException expected) {
        }
        
        delete(file1); // OK now!
        assertFalse(exists(file1));
        assertFalse(isDirectory(file1));
        assertFalse(isRegularFile(file1));
        try {
            size(file1);
            fail();
        } catch (NoSuchFileException expected) {
        }
        
        delete(dir);
        assertFalse(exists(dir));
        assertFalse(isDirectory(dir));
        assertFalse(isRegularFile(dir));
        try {
            size(dir);
            fail();
        } catch (NoSuchFileException expected) {
        }
    }

    @Test
    public final void testIllegalDirectoryOperations() throws IOException {
        try {
            final String[] names = {
                "inner" + getSuffix(),
                "dir",
            };
            TPath file = archive;
            for (int i = 0; i <= names.length; i++) {
                final TPath file2 = newNonArchiveFile(file);
                createDirectory(file2);
                assertIllegalDirectoryOperations(file2);
                delete(file2);
                createDirectory(file);
                assertIllegalDirectoryOperations(file);
                if (i < names.length)
                    file = file.resolve(names[i]);
            }
        } finally {
            archive.toFile().rm_r();
        }
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    private void assertIllegalDirectoryOperations(final TPath dir)
    throws IOException {
        assert isDirectory(dir);
        try {
            newInputStream(dir);
            fail();
        } catch (IOException expected) {
        }
        try {
            newOutputStream(dir);
            fail();
        } catch (IOException expected) {
        }
        Path tmp = Files.createTempFile(TEMP_FILE_PREFIX, null);
        try {
            try {
                copy(tmp, dir);
                fail();
            } catch (FileAlreadyExistsException expected) {
            }
            try {
                copy(dir, tmp);
                fail();
            } catch (FileAlreadyExistsException expected) {
            }
        } finally {
            delete(tmp);
        }
    }

    @Test
    public final void testStrictFileOutputStream() throws IOException {
        TPath file = archive.resolve("test.txt");
        TConfig config = TConfig.push();
        try {
            config.setLenient(false);
            try {
                assertFileOutputStream(file);
                fail("Creating ghost directories should not be allowed when Path.isLenient() is false!");
            } catch (IOException expected) {
            }
            createDirectory(archive);
            assertFileOutputStream(file);
            delete(archive);
        } finally {
            config.close();
        }
    }
    
    @Test
    public final void testLenientFileOutputStream() throws IOException {
        TPath file = archive.resolve("dir/inner" + getSuffix() + "/dir/test.txt");

        assertFileOutputStream(file);

        try {
            delete(archive);
            fail("directory not empty");
        } catch (IOException expected) {
        }
        TFileSystem.umount(); // allow external modifications!
        delete(newNonArchiveFile(archive)); // use plain file to delete instead!
        assertFalse(exists(archive));
        assertFalse(isDirectory(archive));
        assertFalse(isRegularFile(archive));
        try {
            size(archive);
            fail();
        } catch (NoSuchFileException expected) {
        }
    }

    private void assertFileOutputStream(TPath file) throws IOException {
        final byte[] message = "Hello World!\r\n".getBytes();
        final OutputStream out = newOutputStream(file);
        try {
            assertTrue(exists(file));
            assertFalse(isDirectory(file));
            assertTrue(isRegularFile(file));
            assertEquals(0, size(file));
            out.write(message);
            assertEquals(0, size(file));
            out.flush();
            assertEquals(0, size(file));
        } finally {
            out.close();
        }
        assertTrue(exists(file));
        assertFalse(isDirectory(file));
        assertTrue(isRegularFile(file));
        assertEquals(message.length, size(file));
        
        try {
            createFile(file);
            fail();
        } catch (FileAlreadyExistsException expected) {
        }
        assertTrue(exists(file));
        assertFalse(isDirectory(file));
        assertTrue(isRegularFile(file));
        assertEquals(message.length, size(file));
        
        delete(file);
        assertFalse(exists(file));
        assertFalse(isDirectory(file));
        assertFalse(isRegularFile(file));
        try {
            size(file);
            fail();
        } catch (NoSuchFileException expected) {
        }
    }
    
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OS_OPEN_STREAM")
    @Test
    public final void testBusyFileInputStream() throws IOException {
        final TPath file1 = archive.resolve("file1");
        final TPath file2 = archive.resolve("file2");

        // Test open output streams.
        createFile(file1);
        TFileSystem.umount(); // ensure file1 is really present in the archive file
        createFile(file2); // uses FsOutputOption.CACHE!
        final InputStream in1 = newInputStream(file1);
        try {
            newInputStream(file2);
            gc();

            // This operation should complete without any exception if the garbage
            // collector did his job.
            try {
                copy(in1, file2, StandardCopyOption.REPLACE_EXISTING);
            } catch (FsSyncWarningException ex) {
                fail("The garbage collector hasn't been collecting an open stream. If this is only happening occasionally, you can safely ignore it.");
            }

            // in1 is still open!
            try {
                TFile.umount(); // forces closing of fis1
                fail("Expected warning exception when synchronizing a busy archive file!");
            } catch (FsSyncWarningException ex) {
                if (!(ex.getCause() instanceof FileBusyException))
                    throw ex;
            }
            assertTrue(isRegularFile(file2));
            try {
                copy(in1, file2, StandardCopyOption.REPLACE_EXISTING);
                fail("Expected exception when reading from entry input stream of an unmounted archive file!");
            } catch (IOException expected) {
            }

            // Open file1 as stream and let the garbage collection close the stream automatically.
            newInputStream(file1);
            gc();

            // This operation should complete without any exception if the garbage
            // collector did his job.
            try {
                TFileSystem.umount(); // allow external modifications!
            } catch (FsSyncWarningException ex) {
                fail("The garbage collector hasn't been collecting an open stream. If this is only happening occasionally, you can safely ignore it.");
            }

            delete(newNonArchiveFile(archive));
        } finally {
            // Closing the invalidated stream explicitly should be OK.
            in1.close();
        }

        // Cleanup.
        try {
            delete(file2);
            fail("already deleted externally");
        } catch (IOException expected) {
        }
        assertFalse(exists(file2));
        try {
            delete(file1);
            fail("already deleted externally");
        } catch (IOException expected) {
        }
        assertFalse(exists(file1));
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OS_OPEN_STREAM")
    @Test
    public final void testBusyFileOutputStream() throws IOException {
        TPath file1 = archive.resolve("file1");
        TPath file2 = archive.resolve("file2");
        
        // Ensure that there are two entries in the archive.
        // This is used later to check whether the update operation knows
        // how to deal with updating an archive for which there is still
        // an open output stream.
        OutputStream out = newOutputStream(file1);
        try {
            Streams.cat(new ByteArrayInputStream(data), out);
        } finally {
            out.close();
        }
        
        out = newOutputStream(file2);
        try {
            Streams.cat(new ByteArrayInputStream(data), out);
        } finally {
            out.close();
        }
        
        TFileSystem.umount(); // ensure two entries in the archive
        
        out = newOutputStream(file1);
        Streams.cat(new ByteArrayInputStream(data), out);
        
        // out is still open!
        try {
            newOutputStream(file1);
            fail("Expected synchronization exception when overwriting an unsynchronized entry of a busy archive file!");
        } catch (FsSyncException ex) {
            if (!(ex.getCause() instanceof FileBusyException))
                    throw ex;
        }

        // out is still open!
        try {
            newOutputStream(file2);
        } catch (FsSyncException ex) {
            if (!(ex.getCause() instanceof FileBusyException))
                    throw ex;
            logger.warning("This archive driver does NOT support concurrent writing of different entries in the same archive file.");
        }

        // out is still open!
        Streams.cat(new ByteArrayInputStream(data), out); // write again
        
        // out is still open!
        try {
            TFileSystem.umount(); // forces closing of all streams
            fail("Expected warning exception when synchronizing a busy archive file!");
        } catch (FsSyncWarningException ex) {
            if (!(ex.getCause() instanceof FileBusyException))
                throw ex;
        }
        
        try {
            Streams.cat(new ByteArrayInputStream(data), out); // write again
            fail("Expected exception when writing to entry output stream of an unmounted archive file!");
        } catch (OutputClosedException expected) {
        }
        
        // The stream has been forcibly closed by TPath.update().
        // Another close is OK, though!
        out.close();
        
        // Reopen stream and let the garbage collection close the stream automatically.
        out = newOutputStream(file1);
        out = null;
        gc();
        
        // This update should complete without any exception if the garbage
        // collector did his job.
        try {
            TFileSystem.umount();
        } catch (FsSyncWarningException ex) {
            fail("The garbage collector hasn't been collecting an open stream. If this is only happening occasionally, you can safely ignore it.");
        }
        
        // Cleanup.
        delete(file2);
        assertFalse(exists(file2));
        delete(file1);
        assertFalse(exists(file1));
    }
    
    @Test
    public final void testMkdir() throws IOException {
        final TPath dir1 = archive;
        final TPath dir2 = dir1.resolve("dir");
        final TPath dir3 = dir2.resolve("inner" + getSuffix());
        final TPath dir4 = dir3.resolve("dir");
        final TPath dir5 = dir4.resolve("nuts" + getSuffix());
        final TPath dir6 = dir5.resolve("dir");
        
        assert TConfig.get().isLenient();

        createDirectory(dir6); // create all at once! note archive is in current directory!

        try {
            createDirectory(dir6);
            fail();
        } catch (FileAlreadyExistsException expected) {
        }
        try {
            createDirectory(dir5);
            fail();
        } catch (FileAlreadyExistsException expected) {
        }
        try {
            createDirectory(dir4);
            fail();
        } catch (FileAlreadyExistsException expected) {
        }
        try {
            createDirectory(dir3);
            fail();
        } catch (FileAlreadyExistsException expected) {
        }
        try {
            createDirectory(dir2);
            fail();
        } catch (FileAlreadyExistsException expected) {
        }
        try {
            createDirectory(dir1);
            fail();
        } catch (FileAlreadyExistsException expected) {
        }

        delete(dir6);
        delete(dir5);
        delete(dir4);
        delete(dir3);
        delete(dir2);
        delete(dir1);

        final TConfig config = TConfig.push();
        try {
            config.setLenient(false);

            try {
                createDirectory(dir6);
                fail();
            } catch (IOException expected) {
            }
            try {
                createDirectory(dir5);
                fail();
            } catch (IOException expected) {
            }
            try {
                createDirectory(dir4);
                fail();
            } catch (IOException expected) {
            }
            try {
                createDirectory(dir3);
                fail();
            } catch (IOException expected) {
            }
            try {
                createDirectory(dir2);
                fail();
            } catch (IOException expected) {
            }

            createDirectory(dir1);
            createDirectory(dir2);
            createDirectory(dir3);
            createDirectory(dir4);
            createDirectory(dir5);
            createDirectory(dir6);
        } finally {
            config.close();
        }

        delete(dir6);
        delete(dir5);
        delete(dir4);
        delete(dir3);
        delete(dir2);
        delete(dir1);
    }

    @Test
    public final void testDirectoryTree() throws IOException {
        assertDirectoryTree(
                new TPath(System.getProperty("java.io.tmpdir")), // base directory
                new TPath("dir/inner" + getSuffix() + "/dir/outer" + getSuffix() + "/" + archive.getFileName())); // this path is reversed!!!
    }

    private void assertDirectoryTree(TPath basePath, TPath reversePath)
    throws IOException {
        if (reversePath == null) {
            // We're at the leaf of the directory tree.
            final TPath test = basePath.resolve("test.txt");
            //testCreateNewFile(basePath, test);
            assertFileOutputStream(test);
            return;
        }
        final TPath member = basePath.resolve(reversePath.getFileName());
        boolean created = false;
        try {
            createDirectory(member);
            created = true;
        } catch (FileAlreadyExistsException ex) {
        }
        final TPath children = reversePath.getParent();
        assertDirectoryTree(member, children);
        assertListFiles(basePath, member.getFileName().toString());
        assertTrue(exists(member));
        assertTrue(isDirectory(member));
        assertFalse(isRegularFile(member));
        if (member.isArchive())
            assertEquals(0, size(member));
        if (created) {
            delete(member);
            assertFalse(exists(member));
            assertFalse(isDirectory(member));
            assertFalse(isRegularFile(member));
            try {
                size(member);
                fail();
            } catch (NoSuchFileException expected) {
            }
        }
    }

    private void assertListFiles(final TPath dir, final String entry) throws IOException {
        final Path[] files = listFiles(dir);
        boolean found = false;
        for (Path file : files)
            if (file.getFileName().toString().equals(entry))
                found = true;
        if (!found)
            fail("No such entry: " + entry);
    }

    private static Path[] listFiles(final Path dir) throws IOException {
        try {
            final DirectoryStream<Path> stream = newDirectoryStream(dir);
            try {
                final List<Path> list = new LinkedList<Path>();
                for (Path path : stream)
                    list.add(path);
                return list.toArray(new Path[list.size()]);
            } finally {
                stream.close();
            }
        } catch (NotDirectoryException ex) {
            return null;
        }
    }

    @Test
    public final void testInputOutput() throws IOException {
        assertInputOutput(archive);
        
        final TPath archiveTest = archive.resolve("test");
        assertInputOutput(archiveTest);
        
        final TPath archive2 = archive.resolve("inner" + getSuffix());
        final TPath archive2Test = archive2.resolve("test");
        assertInputOutput(archive2Test);
        delete(archive2);
        delete(archive);
    }

    private void assertInputOutput(final TPath file) throws IOException {
        assertInput(file);
        assertOutput(file);
        delete(file);
    }

    private void assertInput(TPath file) throws IOException {
        InputStream in = new ByteArrayInputStream(data);
        try {
            copy(in, file);
        } finally {
            in.close();
        }
        assertEquals(data.length, size(file));
    }
    
    private void assertOutput(TPath file) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
        try {
            copy(file, out);
            assertTrue(Arrays.equals(data, out.toByteArray()));
        } finally {
            out.close();
        }
    }

    @Test
    public final void testCopyContainingOrSameFiles() throws IOException {
        assert !exists(archive);
        
        final TPath dir = archive.getParent();
        assertNotNull(dir);
        final TPath entry = archive.resolve("entry");
        
        assertCopyContainingOrSameFiles0(dir, archive);
        assertCopyContainingOrSameFiles0(archive, entry);
        
        copy(new ByteArrayInputStream(data), entry);
        
        assertCopyContainingOrSameFiles0(dir, archive);
        assertCopyContainingOrSameFiles0(archive, entry);
        
        archive.toFile().rm_r();
    }

    private void assertCopyContainingOrSameFiles0(final TPath a, final TPath b)
    throws IOException {
        assertCopyContainingOrSameFiles1(a, b);
        assertCopyContainingOrSameFiles1(a.toRealPath(), b);
        assertCopyContainingOrSameFiles1(a, b.toRealPath());
        assertCopyContainingOrSameFiles1(a.toRealPath(), b.toRealPath());
    }
    
    private void assertCopyContainingOrSameFiles1(final TPath a, final TPath b)
    throws IOException {
        try {
            copy(a, a, StandardCopyOption.REPLACE_EXISTING);
            fail();
        } catch (IOException expected) {
        }
        try {
            copy(a, b, StandardCopyOption.REPLACE_EXISTING);
            fail();
        } catch (IOException expected) {
        }
        try {
            copy(b, a, StandardCopyOption.REPLACE_EXISTING);
            fail();
        } catch (IOException expected) {
        }
        try {
            copy(b, b, StandardCopyOption.REPLACE_EXISTING);
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

        createDirectory(archive); // create valid archive file
        assertCopyDelete(archive, names, 0);
        delete(archive);

        createDirectory(newNonArchiveFile(archive)); // create false positive archive file
        assertCopyDelete(archive, names, 0);
        delete(archive);
    }

    private void assertCopyDelete(final TPath parent, String[] names, int off)
    throws IOException {
        if (off >= names.length)
            return;
        
        final TPath dir = parent.resolve(names[off]);

        createDirectory(dir); // create valid archive file
        assertCopyDelete(parent, dir);
        assertCopyDelete(dir, names, off + 1); // continue recursion
        delete(dir);

        createDirectory(newNonArchiveFile(dir)); // create false positive archive file
        assertCopyDelete(parent, dir);
        assertCopyDelete(dir, names, off + 1); // continue recursion
        delete(dir);
    }

    private void assertCopyDelete(final TPath parent, final TPath dir)
    throws IOException {
        final TPath parentFile = parent.resolve("file");
        final TPath parentArchive = parent.resolve("archive" + getSuffix());
        final TPath dirFile = dir.resolve("file");
        final TPath dirArchive = dir.resolve("archive" + getSuffix());

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

    private void assertCopyDelete0(TPath a, TPath b) throws IOException {
        // This must be the granularity of the tested file system type PLUS
        // the granularity of the parent file system, i.e. the platform file system!
        // Note that older platform file systems and even ext4 (!) have a granularity
        // of two seconds.
        // Plus the worst case of another two seconds for ZIP files results in
        // four seconds!
        assertCopyDelete0(a, b, 2000 + 2000);
    }

    private void assertCopyDelete0( final TPath a,
                                    final TPath b,
                                    final long granularity)
    throws IOException {
        // Create a file with an old timestamp.
        final long time = System.currentTimeMillis();
        {
            final OutputStream out = newOutputStream(a);
            try {
                out.write(data);
            } finally {
                out.close();
            }
        }
        setLastModifiedTime(a, FileTime.fromMillis(time - granularity));

        // Test copy.
        copy(a, b, StandardCopyOption.REPLACE_EXISTING);
        assertThat(size(b), is(size(a)));
        assertThat(getLastModifiedTime(b).toMillis(), not(is(getLastModifiedTime(a).toMillis())));
        copy(a, b, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        assertThat(size(b), is(size(a)));
        long almd = getLastModifiedTime(a).toMillis() / granularity * granularity;
        long blmd = getLastModifiedTime(b).toMillis() / granularity * granularity;
        long almu = (getLastModifiedTime(a).toMillis() + granularity - 1) / granularity * granularity;
        long blmu = (getLastModifiedTime(b).toMillis() + granularity - 1) / granularity * granularity;
        assertTrue(almd == blmd || almu == blmu);

        // Test copy to.
        copy(b, a, StandardCopyOption.REPLACE_EXISTING);
        assertThat(size(a), is(size(b)));
        assertThat(getLastModifiedTime(a).toMillis(), not(is(getLastModifiedTime(b).toMillis())));
        copy(b, a, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        assertThat(size(a), is(size(b)));
        almd = getLastModifiedTime(a).toMillis() / granularity * granularity;
        blmd = getLastModifiedTime(b).toMillis() / granularity * granularity;
        almu = (getLastModifiedTime(a).toMillis() + granularity - 1) / granularity * granularity;
        blmu = (getLastModifiedTime(b).toMillis() + granularity - 1) / granularity * granularity;
        assertTrue(almd == blmd || almu == blmu);

        // Check result.
        {
            final ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
            copy(a, out);
            assertTrue(Arrays.equals(data, out.toByteArray()));
        }

        // Cleanup.
        delete(a);
        delete(b);
    }

    @Test
    public final void testListPerformance() throws IOException {
        createDirectory(archive);

        int i, j;
        long time;

        time = System.currentTimeMillis();
        for (i = 0; i < 100; i++) {
            TPath file = archive.resolve("" + i);
            createFile(file);
        }
        time = System.currentTimeMillis() - time;
        logger.log(Level.FINER, "Time required to create {0} archive file entries: {1}ms", new Object[]{ i, time });

        time = System.currentTimeMillis();
        for (j = 0; j < 100; j++)
            listFiles(archive);
        time = System.currentTimeMillis() - time;
        logger.log(Level.FINER, "Time required to list these entries {0} times using a nullary FilenameFilter: {1}ms", new Object[]{ j, time });

        time = System.currentTimeMillis();
        for (j = 0; j < 100; j++)
            listFiles(archive);
        time = System.currentTimeMillis() - time;
        logger.log(Level.FINER, "Time required to list these entries {0} times using a nullary FileFilter: {1}ms", new Object[]{ j, time });

        try {
            delete(archive);
            fail("directory not empty");
        } catch (IOException expected) {
        }
        TFileSystem.umount(); // allow external modifications!
        delete(newNonArchiveFile(archive)); // use plain file to delete instead!
        assertFalse(exists(archive));
        assertFalse(isDirectory(archive));
        assertFalse(isRegularFile(archive));
        try {
            size(archive);
            fail();
        } catch (NoSuchFileException expected) {
        }
    }
    
    @Test
    public final void testIllegalDeleteEntryWithOpenStream()
    throws IOException {
        final TPath entry1 = archive.resolve("entry1");
        final TPath entry2 = archive.resolve("entry2");
        final OutputStream out1 = newOutputStream(entry1);
        try {
            try {
                delete(entry1);
                fail();
            } catch (IOException expected) {
            }
            out1.write(data);
            try {
                archive.toFile().rm_r();
                fail();
            } catch (IOException ex) {
            }
        } finally {
            out1.close();
        }
        final OutputStream out2 = newOutputStream(entry2);
        try {
            try {
                delete(entry2);
                fail();
            } catch (IOException expected) {
            }
            out2.write(data);
            try {
                archive.toFile().rm_r();
                fail();
            } catch (IOException ex) {
            }
        } finally {
            out2.close();
        }
        final InputStream in1 = newInputStream(entry1);
        try {
            final InputStream in2 = newInputStream(entry2);
            try {
                delete(entry2);
                final ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
                try {
                    Streams.cat(in2, out);
                } finally {
                    out.close();
                }
                assertTrue(Arrays.equals(data, out.toByteArray()));
                try {
                    archive.toFile().rm_r();
                    fail();
                } catch (IOException ex) {
                }
            } finally {
                in2.close();
            }
            try {
                delete(entry1);
                fail("deleted within archive.toFile().rm_r()");
            } catch (IOException expected) {
            }
            final ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
            try {
                Streams.cat(in1, out);
            } finally {
                out.close();
            }
            assertTrue(Arrays.equals(data, out.toByteArray()));
            try {
                archive.toFile().rm_r();
                fail();
            } catch (IOException ex) {
            }
        } finally {
            in1.close();
        }
        archive.toFile().rm_r();
        assertFalse(exists(newNonArchiveFile(archive)));
    }
    
    @Test
    public final void testRenameValidArchive() throws IOException {
        PrintStream out = new PrintStream(
                newOutputStream(archive.resolve("entry")));
        try {
            out.println("Hello World!");
        } finally {
            out.close(); // ALWAYS close streams!
        }
        assertRenameArchiveToTemp(archive);
    }
    
    @Test
    public final void testRenameFalsePositive() throws IOException {
        // Create false positive archive.
        // Note that archive is a TPath instance which returns isArchive()
        // == true, so we must create a new TPath instance which is guaranteed
        // to ignore the archive getSuffix() in the path.
        // Furthermore, data is an array containing random data
        // - not a regular archive.
        // So upon completion of this step, the object "archive" refers to a
        // false positive.
        final TPath tmp = newNonArchiveFile(archive);
        final InputStream in = new ByteArrayInputStream(data);
        copy(in, tmp);
        assertRenameArchiveToTemp(archive);
    }

    private void assertRenameArchiveToTemp(final TPath archive)
    throws IOException {
        assert archive.isArchive(); // regular archive or false positive
        assert !archive.isEntry(); // not contained in another archive file

        // Create a temporary file.
        TPath tmp = new TPath(Files.createTempFile(TEMP_FILE_PREFIX, null));
        delete(tmp);
        assertFalse(exists(tmp));
        assertFalse(exists(newNonArchiveFile(tmp)));

        // Now rename the archive to the temporary path.
        // Depending on the true state of the object "archive", this will
        // either create a directory (iff archive is a regular archive) or a
        // plain file (iff archive is a false positive).
        archive.toFile().mv(tmp.toFile());
        assertFalse(exists(archive));
        assertFalse(exists(newNonArchiveFile(archive)));

        // Now delete resulting temporary file or directory.
        tmp.toFile().rm_r();
        assertFalse(exists(tmp));
        assertFalse(exists(newNonArchiveFile(tmp)));
    }

    @Test
    public final void testRenameRecursively() throws IOException {
        final TPath temp = new TPath(createTempFile());
        final TPath archive2 = archive.resolve("inner" + getSuffix());
        final TPath archive3 = archive2.resolve("nuts" + getSuffix());
        final TPath archive1a = archive.resolve("a");
        final TPath archive1b = archive.resolve("b");
        final TPath archive2a = archive2.resolve("a");
        final TPath archive2b = archive2.resolve("b");
        final TPath archive3a = archive3.resolve("a");
        final TPath archive3b = archive3.resolve("b");
        
        delete(temp);
        
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
        delete(archive3);
        delete(archive2);
        assertOutput(archive1a);
        delete(archive1a);
        delete(archive);
    }
    
    private void assertRenameTo(TPath src, TPath dst) throws IOException {
        assertTrue(exists(src));
        if (!src.isEntry())
            assertTrue(exists(newNonArchiveFile(src)));
        assertFalse(exists(dst));
        if (!dst.isEntry())
            assertFalse(exists(newNonArchiveFile(dst)));
        assert TConfig.get().isLenient();
        src.toFile().mv(dst.toFile());
        assertFalse(exists(src));
        if (!src.isEntry())
            assertFalse(exists(newNonArchiveFile(src)));
        assertTrue(exists(dst));
        if (!dst.isEntry())
            assertTrue(exists(newNonArchiveFile(dst)));
    }

    private static final String[] MEMBERS = {
        "A directory member",
        "Another directory member",
        "Yet another directory member",
    };
    
    @Test
    public final void testList() throws IOException {
        final Path dir = createTempFile();
        final TPath dir2 = new TPath(dir);

        delete(dir);

        // Create regular directory for testing.
        createDirectory(dir);
        for (int i = MEMBERS.length; --i >= 0; )
            createFile(dir.resolve(MEMBERS[i]));
        Path[] files = listFiles(dir);
        Arrays.sort(files);
        assertList(files, dir2);
        dir2.toFile().rm_r();

        // Repeat test with regular archive file.
        createDirectory(dir2);
        for (int i = MEMBERS.length; --i >= 0; )
            createFile(dir2.resolve(MEMBERS[i]));
        assertList(files, dir2);
        dir2.toFile().rm_r();
    }

    private void assertList(final Path[] refs, final TPath dir)
    throws IOException {
        final Path[] files = listFiles(dir);
        Arrays.sort(files);
        assertEquals(refs.length, files.length);
        for (int i = 0, l = refs.length; i < l; i++) {
            final Path ref = refs[i];
            final TPath file = (TPath) files[i];
            assertTrue(!(ref instanceof TPath));
            assertEquals(ref.toString(), file.toString());
            assertNull(listFiles(file));
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
        class CheckAllEntriesThread extends IOThread {
            @Override
            public void work() throws IOException {
                assertArchiveEntries(archive, nEntries);
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
                throw new IOException(thread.failure);
        }
        
        archive.toFile().rm_r();
    }
    
    private void createTestArchive(final int nEntries) throws IOException {
        for (int i = 0; i < nEntries; i++) {
            final TPath entry = new TPath(archive + SEPARATOR + i);
            final OutputStream out = newOutputStream(entry);
            try {
                out.write(data);
            } finally {
                out.close();
            }
        }
    }
    
    private void assertArchiveEntries(final TPath archive, int nEntries)
    throws IOException {
        final Path[] entries = listFiles(archive);
        assertEquals(nEntries, entries.length);
        final byte[] buf = new byte[4096];
        for (int i = 0, l = entries.length; i < l; i++) {
            final TPath entry = (TPath) entries[i];
            final InputStream in = newInputStream(entry);
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
            final TPath archive,
            final int nThreads,
            final boolean wait)
            throws Exception {
        assertTrue(TConfig.get().isLenient());
        
        class WritingThread extends IOThread {
            final int i;
            
            WritingThread(int i) {
                this.i = i;
            }
            
            @Override
            public void work() throws IOException {
                final TPath file = archive.resolve(i + "");
                OutputStream out;
                while (true) {
                    try {
                        out = newOutputStream(file);
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
        archive.toFile().rm_r();
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
        assertTrue(TConfig.get().isLenient());
        
        class WritingThread extends IOThread {
            @Override
            public void work() throws IOException {
                final TPath archive = new TPath(createTempFile());
                delete(archive);
                final TPath file = archive.resolve("entry");
                try {
                    final OutputStream out = newOutputStream(file);
                    try {
                        out.write(data);
                    } finally {
                        out.close();
                    }
                    try {
                        if (updateIndividually)
                            archive.getFileSystem().close();
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
                    archive.toFile().rm_r();
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

    private abstract class IOThread extends Thread {
        Throwable failure;

        IOThread() {
            setDaemon(true);
        }

        @Override
        public final void run() {
            try {
                work();
            } catch (Throwable exception) {
                failure = exception;
            }
        }

        abstract void work() throws IOException;
    }
}
