/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.path;

import de.truezip.file.ConfiguredClientTestBase;
import de.truezip.file.TConfig;
import de.truezip.file.TFileITSuite;
import de.truezip.file.TVFS;
import de.truezip.kernel.FsArchiveDriver;
import de.truezip.kernel.FsResourceOpenException;
import de.truezip.kernel.FsSyncException;
import de.truezip.kernel.FsSyncWarningException;
import de.truezip.kernel.io.InputClosedException;
import de.truezip.kernel.io.OutputClosedException;
import de.truezip.kernel.io.Streams;
import static de.truezip.kernel.option.AccessOption.GROW;
import static de.truezip.kernel.option.SyncOption.CLEAR_CACHE;
import static de.truezip.kernel.option.SyncOption.WAIT_CLOSE_IO;
import static de.truezip.kernel.option.SyncOptions.SYNC;
import de.truezip.kernel.util.ArrayUtils;
import de.truezip.kernel.util.BitField;
import static de.truezip.kernel.util.ConcurrencyUtils.NUM_IO_THREADS;
import de.truezip.kernel.util.ConcurrencyUtils.TaskFactory;
import de.truezip.kernel.util.ConcurrencyUtils.TaskJoiner;
import static de.truezip.kernel.util.ConcurrencyUtils.runConcurrent;
import static java.io.File.separatorChar;
import java.io.*;
import static java.nio.file.Files.*;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Performs integration testing of a particular {@link FsArchiveDriver}
 * by using the API of the TrueZIP Path module.
 *
 * @param  <D> the type of the archive driver.
 * @see    TFileITSuite Test suite for the TrueZIP File* API.
 * @author Christian Schlichtherle
 */
public abstract class TPathITSuite<D extends FsArchiveDriver<?>>
extends ConfiguredClientTestBase<D> {

    private static final Logger
            logger = Logger.getLogger(TPathITSuite.class.getName());

    /**
     * The prefix for temporary files, which is {@value}.
     * This value should identify the TrueZIP Path module in order to
     * ensure that no two temporary files are shared between tests of the
     * TrueZIP Path API and the TrueZIP File* API.
     */
    private static final String TEMP_FILE_PREFIX = "tzp-path";

    private Path temp;
    private TPath archive;

    @Override
    public void setUp() throws IOException {
        super.setUp();
        temp = createTempFile();
        delete(temp);
        archive = new TPath(temp);
    }

    @Override
    public void tearDown() {
        try {
            try {
                umount();
            } finally {
                archive = null;
                final Path temp = this.temp;
                this.temp = null;
                if (null != temp && exists(temp))
                    delete(temp);                
            }
        } catch (final IOException ex) {
            logger.log(Level.FINEST,
                    "Failed to clean up test file (this may be just an aftermath):",
                    ex);
        } finally {
            super.tearDown();
        }
    }

    /** Returns the current archive file. */
    protected final TPath getArchive() {
        return archive;
    }

    /** Unmounts the {@linkplain #getArchive() current archive file}. */
    protected final void umount() throws FsSyncException {
        if (null != archive)
            archive.getFileSystem().close();
    }

    private Path createTempFile() throws IOException {
        // TODO: Removing .toRealPath() causes archive.toFile().rm_r() to
        // fail in testCopyContainingOrSameFiles() - explain why!
        return Files.createTempFile(TEMP_FILE_PREFIX, getExtension()).toRealPath();
    }

    private void createTestFile(final TPath path) throws IOException {
        final OutputStream out = newOutputStream(path);
        try {
            out.write(getData());
        } finally {
            out.close();
        }
    }

    @Test
    public final void testFalsePositives() throws IOException {
        assertFalsePositive(archive);

        // Dito for entry.
        final TPath entry = archive.resolve("entry" + getExtension());

        createDirectory(archive.toNonArchivePath());
        assertFalsePositive(entry);
        delete(archive);

        createDirectory(archive);
        assertFalsePositive(entry);
        delete(archive);
    }

    private void assertFalsePositive(final TPath file) throws IOException {
        assert file.isArchive();

        // Note that file's parent directory may be a directory in the host file system!

        // Create file false positive.
        createTestFile(file);

        // Overwrite.
        createTestFile(file);

        assertTrue(exists(file));
        assertFalse(isDirectory(file));
        assertTrue(isRegularFile(file));
        assertEquals(getDataLength(), size(file));
        assertTrue(getLastModifiedTime(file).toMillis() > 0);

        // Read back portion
        try (final InputStream in = newInputStream(file)) {
            byte[] buf = new byte[getDataLength()];
            assertTrue(ArrayUtils.equals(getData(), 0, buf, 0, in.read(buf)));
        }
        assertRm(file);

        // Create directory false positive.

        createDirectory(file.toNonArchivePath());
        assertTrue(exists(file));
        assertTrue(isDirectory(file));
        assertFalse(isRegularFile(file));
        //assertEquals(0, file.getLength());
        assertTrue(getLastModifiedTime(file).toMillis() > 0);

        try {
            newInputStream(archive).close();
            if ('\\' == separatorChar)
                fail();
        } catch (IOException ex) {
            if ('\\' != separatorChar && !archive.isArchive())
                throw ex;
        }

        try {
            newOutputStream(archive).close();
            fail();
        } catch (IOException expected) {
        }

        assertRm(file);

        // Create regular archive file.

        createDirectory(file);
        assertTrue(exists(file));
        assertTrue(isDirectory(file));
        assertFalse(isRegularFile(file));
        //assertEquals(0, file.getLength());
        assertTrue(getLastModifiedTime(file).toMillis() > 0);

        try {
            newInputStream(archive).close();
            if ('\\' == separatorChar)
                fail();
        } catch (IOException ex) {
            if ('\\' != separatorChar && !archive.isArchive() && !archive.isEntry())
                throw ex;
        }

        try {
            newOutputStream(archive).close();
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
        try (final TConfig config = TConfig.push()) {
            config.setLenient(false);
            try {
                createFile(file1);
                fail("Creating a file in a non-existent directory should throw an IOException!");
            } catch (IOException expected) {
            }
            assertCreateNewFile(archive, file1, file2);
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
        if (dir instanceof TPath) {
            final TPath tdir = (TPath) dir;
            if (tdir.isArchive() || tdir.isEntry())
                assertEquals(0, size(dir));
        }
        
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
                "inner" + getExtension(),
                "dir",
            };
            TPath file = archive;
            for (int i = 0; i <= names.length; i++) {
                final TPath file2 = file.toNonArchivePath();
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

    private void assertIllegalDirectoryOperations(final TPath dir)
    throws IOException {
        assert isDirectory(dir);
        try {
            newInputStream(dir).close();
            if ('\\' == separatorChar)
                fail();
        } catch (IOException ex) {
            if ('\\' != separatorChar && !dir.isArchive() && !dir.isEntry())
                throw ex;
        }
        try {
            newOutputStream(dir).close();
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
        try (final TConfig config = TConfig.push()) {
            config.setLenient(false);
            try {
                assertFileOutputStream(file);
                fail("Creating ghost directories should not be allowed when Path.isLenient() is false!");
            } catch (IOException expected) {
            }
            createDirectory(archive);
            assertFileOutputStream(file);
            delete(archive);
        }
    }
    
    @Test
    public final void testLenientFileOutputStream() throws IOException {
        TPath file = archive.resolve("dir/inner" + getExtension() + "/dir/test.txt");

        assertFileOutputStream(file);

        try {
            delete(archive);
            fail("directory not empty");
        } catch (IOException expected) {
        }
        umount(); // allow external modifications!
        delete(archive.toNonArchivePath()); // use plain file to delete instead!
        assertFalse(exists(archive));
        assertFalse(isDirectory(archive));
        assertFalse(isRegularFile(archive));
        try {
            size(archive);
            fail();
        } catch (NoSuchFileException expected) {
        }
    }

    private void assertFileOutputStream(final TPath file) throws IOException {
        final byte[] message = "Hello World!\r\n".getBytes();
        try (final OutputStream out = newOutputStream(file)) {
            assertTrue(exists(file));
            assertFalse(isDirectory(file));
            assertTrue(isRegularFile(file));
            assertEquals(0, size(file));
            out.write(message);
            assertEquals(0, size(file));
            out.flush();
            assertEquals(0, size(file));
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
    //@Test
    public final void testBusyFileInputStream()
    throws IOException, InterruptedException {
        final TPath file1 = archive.resolve("file1");
        final TPath file2 = archive.resolve("file2");

        createFile(file1); // uses AccessOption.CACHE!
        umount();
        final InputStream in1 = newInputStream(file1);
        createFile(file2); // uses AccessOption.CACHE!
        try {
            copy(in1, file2, StandardCopyOption.REPLACE_EXISTING);

            // in1 is still open!
            try {
                umount(); // forces closing of in1
                fail();
            } catch (final FsSyncWarningException ex) {
                if (!(ex.getCause() instanceof FsResourceOpenException))
                    throw ex;
            }
            assertTrue(isRegularFile(file2));
            try {
                copy(in1, file2, StandardCopyOption.REPLACE_EXISTING);
                fail();
            } catch (InputClosedException ex) {
            }

            // Open file1 as stream and let the garbage collection close the stream automatically.
            newInputStream(file1);

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

            delete(archive.toNonArchivePath());
        } finally {
            // Closing the invalidated stream explicitly should be OK.
            in1.close();
        }

        // Cleanup.
        try {
            delete(file2);
            fail();
        } catch (IOException alreadyDeletedExternally) {
        }
        assertFalse(exists(file2));
        try {
            delete(file1);
            fail();
        } catch (IOException alreadyDeletedExternally) {
        }
        assertFalse(exists(file1));
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OS_OPEN_STREAM")
    //@Test
    public final void testBusyFileOutputStream()
    throws IOException, InterruptedException {
        TPath file1 = archive.resolve("file1");
        TPath file2 = archive.resolve("file2");

        // Ensure that there are two entries in the archive.
        // This is used later to check whether the update operation knows
        // how to deal with updating an archive for which there is still
        // an open output stream.
        try (final OutputStream out = newOutputStream(file1)) {
            Streams.cat(new ByteArrayInputStream(getData()), out);
        }

        try (final OutputStream out = newOutputStream(file2)) {
            Streams.cat(new ByteArrayInputStream(getData()), out);
        }

        umount(); // ensure two entries in the archive

        OutputStream out = newOutputStream(file1);
        Streams.cat(new ByteArrayInputStream(getData()), out);

        // out is still open!
        try {
            newOutputStream(file1).close();
            fail();
        } catch (final FsSyncException ex) {
            if (!(ex.getCause() instanceof FsResourceOpenException))
                    throw ex;
        }

        // out is still open!
        try {
            newOutputStream(file2).close();
        } catch (final FsSyncException ex) {
            if (!(ex.getCause() instanceof FsResourceOpenException))
                throw ex;
            logger.log(Level.INFO,
                    getArchiveDriver().getClass()
                        + " does not support concurrent writing of different entries in the same archive file.",
                    ex);
        }

        // out is still open!
        Streams.cat(new ByteArrayInputStream(getData()), out); // write again

        // out is still open!
        try {
            umount(); // forces closing of all streams
            fail();
        } catch (final FsSyncWarningException ex) {
            if (!(ex.getCause() instanceof FsResourceOpenException))
                throw ex;
        }

        try {
            Streams.cat(new ByteArrayInputStream(getData()), out); // write again
            fail();
        } catch (OutputClosedException ex) {
        }

        // The stream has been forcibly closed by TPath.update().
        // Another close is OK, though!
        out.close();

        // Reopen stream and let the garbage collection close the stream automatically.
        newOutputStream(file1);
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
        delete(file2);
        assertFalse(exists(file2));
        delete(file1);
        assertFalse(exists(file1));
    }
    
    @Test
    public final void testMkdir() throws IOException {
        final TPath dir1 = archive;
        final TPath dir2 = dir1.resolve("dir");
        final TPath dir3 = dir2.resolve("inner" + getExtension());
        final TPath dir4 = dir3.resolve("dir");
        final TPath dir5 = dir4.resolve("nuts" + getExtension());
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

        try (final TConfig config = TConfig.push()) {
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
                new TPath("dir/inner" + getExtension() + "/dir/outer" + getExtension() + "/" + archive.getFileName())); // this path is reversed!!!
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
            try (final DirectoryStream<Path> stream = newDirectoryStream(dir)) {
                final List<Path> list = new LinkedList<>();
                for (Path path : stream)
                    list.add(path);
                return list.toArray(new Path[list.size()]);
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
        
        final TPath archiveInner = archive.resolve("inner" + getExtension());
        final TPath archiveInnerTest = archiveInner.resolve("test");
        assertInputOutput(archiveInnerTest);
        delete(archiveInner);
        delete(archive);
    }

    private void assertInputOutput(final TPath file) throws IOException {
        assertInput(file);
        assertOutput(file);
        delete(file);
    }

    private void assertInput(final TPath file) throws IOException {
        try (final InputStream in = new ByteArrayInputStream(getData())) {
            copy(in, file);
        }
        assertEquals(getDataLength(), size(file));
    }

    private void assertOutput(final TPath file) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(getDataLength());
        try {
            copy(file, out);
        } finally {
            out.close();
        }
        assertTrue(Arrays.equals(getData(), out.toByteArray()));
    }

    @Test
    public final void testCopyContainingOrSameFiles() throws IOException {
        assert !exists(archive);
        
        final TPath dir = archive.getParent();
        assertNotNull(dir);
        final TPath entry = archive.resolve("entry");
        
        assertCopyContainingOrSameFiles0(dir, archive);
        assertCopyContainingOrSameFiles0(archive, entry);
        
        copy(new ByteArrayInputStream(getData()), entry);
        
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
            "0" + getExtension(),
            "1" + getExtension(),
            //"2" + getExtension(),
        };

        createDirectory(archive); // create valid archive file
        assertCopyDelete(archive, names, 0);
        delete(archive);

        createDirectory(archive.toNonArchivePath()); // create false positive archive file
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

        createDirectory(dir.toNonArchivePath()); // create false positive archive file
        assertCopyDelete(parent, dir);
        assertCopyDelete(dir, names, off + 1); // continue recursion
        delete(dir);
    }

    private void assertCopyDelete(final TPath parent, final TPath dir)
    throws IOException {
        final TPath parentFile = parent.resolve("file");
        final TPath parentArchive = parent.resolve("archive" + getExtension());
        final TPath dirFile = dir.resolve("file");
        final TPath dirArchive = dir.resolve("archive" + getExtension());

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
        // the granularity of the parent file system, e.g. the platform file system!
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
        createTestFile(a);
        setLastModifiedTime(a, FileTime.fromMillis(time - granularity));

        // Test copy a to b.
        copy(a, b, StandardCopyOption.REPLACE_EXISTING);
        assertThat(size(b), is(size(a)));
        assertThat(getLastModifiedTime(b).toMillis(), not(is(getLastModifiedTime(a).toMillis())));
        copy(a, b, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        assertThat(size(b), is(size(a)));
        long almd = getLastModifiedTime(a).toMillis() / granularity * granularity;
        long blmd = getLastModifiedTime(b).toMillis() / granularity * granularity;
        long almu = (getLastModifiedTime(a).toMillis() + granularity - 1) / granularity * granularity;
        long blmu = (getLastModifiedTime(b).toMillis() + granularity - 1) / granularity * granularity;
        assertTrue(
                "almd (" + almd + ") != blmd (" + blmd + ") && almu (" + almu + ") != blmu (" + blmu + ")",
                almd == blmd || almu == blmu);

        // Test copy b to a.
        copy(b, a, StandardCopyOption.REPLACE_EXISTING);
        assertThat(size(a), is(size(b)));
        assertThat(getLastModifiedTime(a).toMillis(), not(is(getLastModifiedTime(b).toMillis())));
        copy(b, a, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        assertThat(size(a), is(size(b)));
        almd = getLastModifiedTime(a).toMillis() / granularity * granularity;
        blmd = getLastModifiedTime(b).toMillis() / granularity * granularity;
        almu = (getLastModifiedTime(a).toMillis() + granularity - 1) / granularity * granularity;
        blmu = (getLastModifiedTime(b).toMillis() + granularity - 1) / granularity * granularity;
        assertTrue(
                "almd (" + almd + ") != blmd (" + blmd + ") && almu (" + almu + ") != blmu (" + blmu + ")",
                almd == blmd || almu == blmu);

        // Check result.
        {
            final ByteArrayOutputStream out = new ByteArrayOutputStream(getDataLength());
            copy(a, out);
            assertTrue(Arrays.equals(getData(), out.toByteArray()));
        }

        // Cleanup.
        delete(a);
        delete(b);
    }

    //@Test
    public final void testIllegalDeleteOfEntryWithOpenStream()
    throws IOException {
        final TPath entry1 = archive.resolve("entry1");
        final TPath entry2 = archive.resolve("entry2");
        try (final OutputStream out1 = newOutputStream(entry1)) {
            try {
                delete(entry1);
                fail();
            } catch (IOException expected) {
            }
            out1.write(getData());
            try {
                archive.toFile().rm_r();
                fail();
            } catch (IOException expected) {
            }
        }
        try (final OutputStream out2 = newOutputStream(entry2)) {
            try {
                delete(entry2);
                fail();
            } catch (IOException expected) {
            }
            out2.write(getData());
            try {
                archive.toFile().rm_r();
                fail();
            } catch (IOException expected) {
            }
        }
        try (final InputStream in1 = newInputStream(entry1)) {
            try (final InputStream in2 = newInputStream(entry2)) {
                delete(entry2);
                final ByteArrayOutputStream out = new ByteArrayOutputStream(getDataLength());
                try {
                    Streams.cat(in2, out);
                } finally {
                    out.close();
                }
                assertTrue(Arrays.equals(getData(), out.toByteArray()));
                try {
                    archive.toFile().rm_r();
                    fail();
                } catch (IOException expected) {
                }
            }
            try {
                delete(entry1);
                fail("deleted within archive.toFile().rm_r()");
            } catch (IOException expected) {
            }
            final ByteArrayOutputStream out = new ByteArrayOutputStream(getDataLength());
            try {
                Streams.cat(in1, out);
            } finally {
                out.close();
            }
            assertTrue(Arrays.equals(getData(), out.toByteArray()));
            try {
                archive.toFile().rm_r();
                fail();
            } catch (IOException expected) {
            }
        }
        archive.toFile().rm_r();
        assertFalse(exists(archive.toNonArchivePath()));
    }
    
    @Test
    public final void testRenameValidArchive() throws IOException {
        try (final PrintStream out = new PrintStream(
                     newOutputStream(archive.resolve("entry")))) {
            out.println("Hello World!");
        }
        assertRenameArchiveToTemp(archive);
    }
    
    @Test
    public final void testRenameFalsePositive() throws IOException {
        // Create false positive archive.
        // Note that archive is a TPath instance which returns isArchive()
        // == true, so we must create a new TPath instance which is guaranteed
        // to ignore the archive getExtension() in the path.
        // Furthermore, data is an array containing random data
        // - not a regular archive.
        // So upon completion of this step, the object "archive" refers to a
        // false positive.
        final TPath tmp = archive.toNonArchivePath();
        final InputStream in = new ByteArrayInputStream(getData());
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
        assertFalse(exists(tmp.toNonArchivePath()));

        // Now rename the archive to the temporary path.
        // Depending on the true state of the object "archive", this will
        // either create a directory (iff archive is a regular archive) or a
        // plain file (iff archive is a false positive).
        archive.toFile().mv(tmp.toFile());
        assertFalse(exists(archive));
        assertFalse(exists(archive.toNonArchivePath()));

        // Now delete resulting temporary file or directory.
        tmp.toFile().rm_r();
        assertFalse(exists(tmp));
        assertFalse(exists(tmp.toNonArchivePath()));
    }

    @Test
    public final void testRenameRecursively() throws IOException {
        final TPath temp = new TPath(createTempFile());
        final TPath archive2 = archive.resolve("inner" + getExtension());
        final TPath archive3 = archive2.resolve("nuts" + getExtension());
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
        assertFalse(exists(dst));
        assertFalse(exists(dst.toNonArchivePath()));
        assert TConfig.get().isLenient();
        src.toFile().mv(dst.toFile());
        assertFalse(exists(src));
        assertFalse(exists(src.toNonArchivePath()));
        assertTrue(exists(dst));
    }

    private static final String[] MEMBERS = {
        "A directory member",
        "Another directory member",
        "Yet another directory member",
    };

    @Test
    public final void testList() throws IOException {
        final Path dir = createTempFile();
        final TPath tdir = new TPath(dir);

        assertNull(listFiles(dir));
        assertNull(listFiles(tdir));
        assertNull(listFiles(tdir.toNonArchivePath()));

        delete(dir);

        // Create regular directory for testing.
        createDirectory(dir);
        for (int i = MEMBERS.length; --i >= 0; )
            createFile(dir.resolve(MEMBERS[i]));
        final Path[] files = listFiles(dir);
        Arrays.sort(files);
        assertList(files, tdir);
        tdir.toFile().rm_r();

        // Repeat test with regular archive file.
        createDirectory(tdir);
        for (int i = MEMBERS.length; --i >= 0; )
            createFile(tdir.resolve(MEMBERS[i]));
        assertList(files, tdir);
        tdir.toFile().rm_r();
    }

    private void assertList(final Path[] expected, final TPath dir)
    throws IOException {
        final Path[] got = listFiles(dir);
        Arrays.sort(got);
        assertEquals(expected.length, got.length);
        for (int i = 0, l = expected.length; i < l; i++) {
            final Path e = expected[i];
            final TPath g = (TPath) got[i];
            assertTrue(!(e instanceof TPath));
            assertEquals(e.toString(), g.toString());
            assertNull(listFiles(g));
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
            runConcurrent(nThreads, new CheckAllEntriesFactory()).join();
        } finally {
            archive.toFile().rm_r();
        }
    }

    private void createTestArchive(final int nEntries) throws IOException {
        for (int i = 0; i < nEntries; i++)
            createTestFile(new TPath(archive.toString(), i + ""));
    }

    private void assertArchiveEntries(final TPath archive, int nEntries)
    throws IOException {
        // Retrieve list of entries and shuffle their order.
        final List<Path> entries = Arrays.asList(listFiles(archive));
        assert entries.size() == nEntries; // this would be a programming error in the test class itself - not the class under test!
        Collections.shuffle(entries, new Random());

        // Now read in the entries in the shuffled order.
        final byte[] buf = new byte[getDataLength()];
        for (final Path _entry : entries) {
            final TPath entry = (TPath) _entry;
            try (final InputStream in = newInputStream(entry)) {
                int off = 0;
                int read;
                while (true) {
                    read = in.read(buf);
                    if (0 > read)
                        break;
                    assertTrue(read > 0);
                    assertTrue(ArrayUtils.equals(getData(), off, buf, 0, read));
                    off += read;
                }
                assertEquals(-1, read);
                assertEquals(off, getDataLength());
                assertTrue(0 >= in.read(new byte[0]));
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
        assert TConfig.get().isLenient();

        class WriteFactory implements TaskFactory {
            @Override
            public Callable<?> newTask(final int threadNum) {
                class Write implements Callable<Void> {
                    @Override
                    public Void call() throws IOException {
                        final TPath entry = archive.resolve("" + threadNum);
                        createTestFile(entry);
                        try {
                            TVFS.sync(archive.getFileSystem().getMountPoint(),
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
            runConcurrent(NUM_IO_THREADS, new WriteFactory()).join();
        } finally {
            assertArchiveEntries(archive, NUM_IO_THREADS);
            archive.toFile().rm_r();
        }
    }

    @Test
    public final void testMultithreadedMultipleArchivesSingleEntryWriting()
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
                final TPath archive = new TPath(createTempFile());
                delete(archive);
                final TPath entry = archive.resolve("entry");
                try {
                    createTestFile(entry);
                    try {
                        if (syncIndividually)
                            archive.getFileSystem().close();
                        else
                            TVFS.sync(SYNC); // DON'T clear the cache!
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
                    archive.toFile().rm_r();
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

        runConcurrent(NUM_IO_THREADS, new WriteFactory()).join();
    }

    /** Test for http://java.net/jira/browse/TRUEZIP-192 . */
    @Test
    public void testMultithreadedMutualArchiveCopying() throws Exception {
        assert TConfig.get().isLenient();

        class CopyFactory implements TaskFactory {
            final TPath src, dst;

            CopyFactory(final TPath src, final TPath dst) {
                this.src = src;
                this.dst = dst;
            }

            @Override
            public Callable<?> newTask(final int threadNum) {
                class Copy implements Callable<Void> {
                    @Override
                    public Void call() throws IOException {
                        final TPath srcNo = src.resolve("src/" + threadNum);
                        final TPath dstNo = dst.resolve("dst/" + threadNum);
                        createTestFile(srcNo);
                        copy(srcNo, dstNo, StandardCopyOption.REPLACE_EXISTING);
                        return null;
                    }
                } // Copy

                return new Copy();
            }
        } // CopyFactory

        final TPath src = archive;
        try {
            final TPath dst = new TPath(createTempFile());
            delete(dst);
            try {
                try {
                    final TaskJoiner join = runConcurrent(NUM_IO_THREADS,
                            new CopyFactory(src, dst));
                    try {
                        runConcurrent(NUM_IO_THREADS,
                                new CopyFactory(dst, src)).join();
                    } finally {
                        join.join();
                    }
                } finally {
                    dst.getFileSystem().close();
                }
            } finally {
                delete(dst.toNonArchivePath());
            }
        } finally {
            src.getFileSystem().close();
        }
        // src alias archive gets deleted by the test fixture.
    }

    @Test
    public void testGrowing() throws IOException {
        final TPath path = archive.toNonArchivePath();
        final TPath entry1 = archive.resolve("entry1");
        final TPath entry2 = archive.resolve("entry2");

        TConfig config = TConfig.push();
        try {
            config.setAccessPreferences(config.getAccessPreferences().set(GROW));

            createTestFile(entry1);
            createTestFile(entry2);

            umount();
            assertTrue(size(path) > 2 * getDataLength()); // two entries plus one central directory

            createTestFile(entry1);
            createTestFile(entry2);
            createTestFile(entry1);
            createTestFile(entry2);

            // See http://java.net/jira/browse/TRUEZIP-144 .
            delete(entry1);
            delete(entry2);

            umount();
            assertTrue(size(path) > 6 * getDataLength()); // six entries plus two central directories
        } finally {
            config.close();
        }

        assertThat(listFiles(archive).length, is(0));

        config = TConfig.push();
        try {
            config.setAccessPreferences(config.getAccessPreferences().set(GROW));

            delete(archive);
            umount();
        } finally {
            config.close();
        }

        assertNull(listFiles(archive));
    }
}
