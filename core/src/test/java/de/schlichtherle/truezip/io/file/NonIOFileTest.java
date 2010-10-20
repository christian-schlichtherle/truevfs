/*
 * Copyright (C) 2007-2010 Schlichtherle IT Services
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

import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import de.schlichtherle.truezip.io.filesystem.FileSystemController;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import java.io.IOException;
import java.net.URI;
import junit.framework.TestCase;

import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.ROOT;

/**
 * Tests archive type independent features of the {@link File} class.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class NonIOFileTest extends TestCase {

    private File archive;
    private String suffix;

    public NonIOFileTest(String testName) {
        super(testName);
        File.setDefaultArchiveDetector(ArchiveDetector.DEFAULT);
    }

    @Override
    protected void setUp() {
        suffix = ".zip";
        archive = new File("archive.zip");
    }

    @Override
    protected void tearDown() {
        assertFalse(new java.io.File("archive.zip").exists());
    }

    public void testURIConstructor() throws Exception {
        File file;
        final String fs = File.separator;

        // Whitespace in path. See issue #1. Thanks to mrudat for submitting this!
        file = new File(new URI("file", "/with a space", null));
        assertEquals("with a space", file.getName());
        assertNull(file.getInnerArchive());
        assertNull(file.getInnerEntryName());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());

        // No ZIP file in path.

        file = new File(new URI("file", "/a " + suffix + "/b " + suffix + "/", null));
        assertNull(file.getInnerArchive());
        assertNull(file.getInnerEntryName());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());

        file = new File(new URI("file", "/a " + suffix + "/b " + suffix + "", null));
        assertNull(file.getInnerArchive());
        assertNull(file.getInnerEntryName());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());

        // One ZIP file in path.
        file = new File(new URI("jar", "file:/a " + suffix + "/b " + suffix + "!/", null));
        assertSame(file, file.getInnerArchive());
        assertSame(ROOT, file.getInnerEntryName());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());

        file = new File(new URI("jar", "file:/a " + suffix + "/b " + suffix + "!", null));
        assertSame(file, file.getInnerArchive());
        assertSame(ROOT, file.getInnerEntryName());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());

        file = new File(new URI("jar", "file:/a " + suffix + "!/b " + suffix + "/", null));
        assertSame(file.getInnerArchive(), file.getEnclArchive());
        assertSame(file.getInnerEntryName(), file.getEnclEntryName());
        assertEquals(fs + "a " + suffix + "", file.getEnclArchive().getPath());
        assertEquals("b " + suffix + "", file.getEnclEntryName());

        file = new File(new URI("jar", "file:/a " + suffix + "!/b " + suffix + "", null));
        assertSame(file.getInnerArchive(), file.getEnclArchive());
        assertSame(file.getInnerEntryName(), file.getEnclEntryName());
        assertEquals(fs + "a " + suffix + "", file.getEnclArchive().getPath());
        assertEquals("b " + suffix + "", file.getEnclEntryName());

        // One ZIP file in path with one redundant jar: scheme.

        file = new File(new URI("jar", "jar:file:/a " + suffix + "/b " + suffix + "!/", null));
        assertSame(file, file.getInnerArchive());
        assertSame(ROOT, file.getInnerEntryName());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());

        file = new File(new URI("jar", "jar:file:/a " + suffix + "/b " + suffix + "!", null));
        assertSame(file, file.getInnerArchive());
        assertSame(ROOT, file.getInnerEntryName());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());

        file = new File(new URI("jar", "jar:file:/a " + suffix + "!/b " + suffix + "/", null));
        assertSame(file.getInnerArchive(), file.getEnclArchive());
        assertSame(file.getInnerEntryName(), file.getEnclEntryName());
        assertEquals(fs + "a " + suffix + "", file.getEnclArchive().getPath());
        assertEquals("b " + suffix + "", file.getEnclEntryName());

        file = new File(new URI("jar", "jar:file:/a " + suffix + "!/b " + suffix + "", null));
        assertSame(file.getInnerArchive(), file.getEnclArchive());
        assertSame(file.getInnerEntryName(), file.getEnclEntryName());
        assertEquals(fs + "a " + suffix + "", file.getEnclArchive().getPath());
        assertEquals("b " + suffix + "", file.getEnclEntryName());

        // One ZIP file in path with two redundant jar: schemes.

        file = new File(new URI("jar", "jar:jar:file:/a " + suffix + "/b " + suffix + "!/", null));
        assertSame(file, file.getInnerArchive());
        assertSame(ROOT, file.getInnerEntryName());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());

        file = new File(new URI("jar", "jar:jar:file:/a " + suffix + "/b " + suffix + "!", null));
        assertSame(file, file.getInnerArchive());
        assertSame(ROOT, file.getInnerEntryName());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());

        file = new File(new URI("jar", "jar:jar:file:/a " + suffix + "!/b " + suffix + "/", null));
        assertSame(file.getInnerArchive(), file.getEnclArchive());
        assertSame(file.getInnerEntryName(), file.getEnclEntryName());
        assertEquals(fs + "a " + suffix + "", file.getEnclArchive().getPath());
        assertEquals("b " + suffix + "", file.getEnclEntryName());

        file = new File(new URI("jar", "jar:jar:file:/a " + suffix + "!/b " + suffix + "", null));
        assertSame(file.getInnerArchive(), file.getEnclArchive());
        assertSame(file.getInnerEntryName(), file.getEnclEntryName());
        assertEquals(fs + "a " + suffix + "", file.getEnclArchive().getPath());
        assertEquals("b " + suffix + "", file.getEnclEntryName());

        // Two ZIP files in path.

        file = new File(new URI("jar", "jar:file:/a " + suffix + "!/b " + suffix + "!/", null));
        assertSame(file, file.getInnerArchive());
        assertSame(ROOT, file.getInnerEntryName());
        assertEquals(fs + "a " + suffix + "", file.getEnclArchive().getPath());
        assertEquals("b " + suffix + "", file.getEnclEntryName());

        file = new File(new URI("jar", "jar:file:/a " + suffix + "!/b " + suffix + "!", null));
        assertSame(file, file.getInnerArchive());
        assertSame(ROOT, file.getInnerEntryName());
        assertEquals(fs + "a " + suffix + "", file.getEnclArchive().getPath());
        assertEquals("b " + suffix + "", file.getEnclEntryName());

        // One ZIP file in path with one misleading '!' in path.

        file = new File(new URI("jar", "file:/a " + suffix + "!/b " + suffix + "!/", null));
        assertSame(file, file.getInnerArchive());
        assertSame(ROOT, file.getInnerEntryName());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());

        file = new File(new URI("jar", "file:/a " + suffix + "!/b " + suffix + "!", null));
        assertSame(file, file.getInnerArchive());
        assertSame(ROOT, file.getInnerEntryName());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());

        // Three ZIP files in path with one ZIP file removed by normalization.

        file = new File(new URI("jar", "jar:file:/a " + suffix + "!/b " + suffix + "!/../c " + suffix + "!/", null));
        assertSame(file, file.getInnerArchive());
        assertSame(ROOT, file.getInnerEntryName());
        assertEquals(fs + "a " + suffix + "", file.getEnclArchive().getPath());
        assertEquals("c " + suffix + "", file.getEnclEntryName());

        file = new File(new URI("jar", "jar:file:/a " + suffix + "!/b " + suffix + "!/../c " + suffix + "!", null));
        assertSame(file, file.getInnerArchive());
        assertSame(ROOT, file.getInnerEntryName());
        assertEquals(fs + "a " + suffix + "", file.getEnclArchive().getPath());
        assertEquals("c " + suffix + "", file.getEnclEntryName());

        // Three ZIP files in path with one ZIP file removed by normalization
        // and hence one redundant jar: scheme.

        file = new File(new URI("jar", "jar:jar:file:/a " + suffix + "!/b " + suffix + "!/../c " + suffix + "!/", null));
        assertSame(file, file.getInnerArchive());
        assertSame(ROOT, file.getInnerEntryName());
        assertEquals(fs + "a " + suffix + "", file.getEnclArchive().getPath());
        assertEquals("c " + suffix + "", file.getEnclEntryName());

        file = new File(new URI("jar", "jar:jar:file:/a " + suffix + "!/b " + suffix + "!/../c " + suffix + "!", null));
        assertSame(file, file.getInnerArchive());
        assertSame(ROOT, file.getInnerEntryName());
        assertEquals(fs + "a " + suffix + "", file.getEnclArchive().getPath());
        assertEquals("c " + suffix + "", file.getEnclEntryName());

        // One ZIP file in path which is removed by normalization.

        file = new File(new URI("jar", "file:/a " + suffix + "!/../b " + suffix + "/", null));
        assertNull(file.getInnerArchive());
        assertNull(file.getInnerEntryName());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());

        file = new File(new URI("jar", "file:/a " + suffix + "!/../b " + suffix + "", null));
        assertNull(file.getInnerArchive());
        assertNull(file.getInnerEntryName());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
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
    
    public void testGetParentFile() {
        File abcdefgh = new File("a/b" + suffix + "/c/d/e" + suffix + "/f" + suffix + "/g/h" + suffix + "");
        File abcdefg  = abcdefgh.getParentFile();
        File abcdef   = abcdefg .getParentFile();
        File abcde    = abcdef  .getParentFile();
        File abcd     = abcde   .getParentFile();
        File abc      = abcd    .getParentFile();
        File ab       = abc     .getParentFile();
        File a        = ab      .getParentFile();
        File n        = a       .getParentFile();
        assertEquals(abcdefgh.getInnerArchive(), abcdefgh);
        assertEquals(abcdefgh.getEnclArchive() , abcdef);
        assertEquals(abcdefg .getInnerArchive(), abcdef);
        assertEquals(abcdefg .getEnclArchive() , abcdef);
        assertEquals(abcdef  .getInnerArchive(), abcdef);
        assertEquals(abcdef  .getEnclArchive() , abcde);
        assertEquals(abcde   .getInnerArchive(), abcde);
        assertEquals(abcde   .getEnclArchive() , ab);
        assertEquals(abcd    .getInnerArchive(), ab);
        assertEquals(abcd    .getEnclArchive() , ab);
        assertEquals(abc     .getInnerArchive(), ab);
        assertEquals(abc     .getEnclArchive() , ab);
        assertEquals(ab      .getInnerArchive(), ab);
        assertEquals(ab      .getEnclArchive() , null);
        assertEquals(a       .getInnerArchive(), null);
        assertEquals(a       .getEnclArchive() , null);
        assertNull(n);
    }

    public void testNormalizedAbsoluteFile() throws IOException {
        testNormalizedAbsoluteFile("",   "");
        testNormalizedAbsoluteFile(".",  ".");
        testNormalizedAbsoluteFile("..", "..");
        
        testNormalizedAbsoluteFile("a", "a");
        testNormalizedAbsoluteFile("a", "a/.");
        testNormalizedAbsoluteFile("a b", "a b"); // test issue #38 on truezip.dev.java.net
        testNormalizedAbsoluteFile(".", "a/..");
        testNormalizedAbsoluteFile("b", "a/../b");
        testNormalizedAbsoluteFile("b", "a/../b/.");
        testNormalizedAbsoluteFile(".", "a/../b/..");
        testNormalizedAbsoluteFile("c", "a/../b/../c");
        testNormalizedAbsoluteFile("c", "a/../b/../c/.");
        testNormalizedAbsoluteFile(".", "a/../b/../c/..");
        
        testNormalizedAbsoluteFile("../a", "../a");
        testNormalizedAbsoluteFile("../a", "../a/.");
        testNormalizedAbsoluteFile("..",   "../a/..");
        testNormalizedAbsoluteFile("../b", "../a/../b");
        testNormalizedAbsoluteFile("../b", "../a/../b/.");
        testNormalizedAbsoluteFile("..",   "../a/../b/..");
        testNormalizedAbsoluteFile("../c", "../a/../b/../c");
        testNormalizedAbsoluteFile("../c", "../a/../b/../c/.");
        testNormalizedAbsoluteFile("..",   "../a/../b/../c/..");
        
        testNormalizedAbsoluteFile("../a",   "../a");
        testNormalizedAbsoluteFile("../a",   "../a/.");
        testNormalizedAbsoluteFile("../a/b", "../a/b");
        testNormalizedAbsoluteFile("../a/b", "../a/b/.");
        testNormalizedAbsoluteFile("../a",   "../a/b/..");
        testNormalizedAbsoluteFile("../a/c", "../a/b/../c");
        testNormalizedAbsoluteFile("../a/c", "../a/b/../c/.");
        testNormalizedAbsoluteFile("../a",   "../a/b/../c/..");
    }
    
    void testNormalizedAbsoluteFile(final String result, final String path)
    throws IOException {
        java.io.File resultFile = new java.io.File(result).getCanonicalFile();
        java.io.File pathFile = new java.io.File(new File(path).getNormalizedAbsoluteFile().getPath());
        assertEquals(resultFile, pathFile);
    }

    /**
     * Test of equals method, of class de.schlichtherle.truezip.io.File.
     */
    public void testEqualsAndHashCode() {
        final boolean win = File.separatorChar == '\\'; // Windoze?
        
        assertFalse(new File("dir/test.txt").equals(new File("dir" + suffix + "/test.txt")));
        assertFalse(new File("dir" + suffix + "/test.txt").equals(new File("dir/test.txt")));
        assertEquals(new File("dir" + suffix + "/test.txt", ArchiveDetector.NULL), new File("dir" + suffix + "/test.txt"));
        assertEquals(new File("dir" + suffix + "/test.txt"), new File("dir" + suffix + "/test.txt", ArchiveDetector.NULL));
        testEqualsAndHashCode(
                new File(win ? "c:\\any.txt" : "/any.txt"),
                new File(win ? "C:\\ANY.TXT" : "/ANY.TXT"));
        testEqualsAndHashCode(
                new File(win ? "c:\\any" + suffix + "\\test.txt" : "/any" + suffix + "/test.txt"),
                new File(win ? "C:\\ANY" + suffix.toUpperCase() + "\\test.txt" : "/ANY" + suffix.toUpperCase() + "/test.txt"));
        testEqualsAndHashCode(
                new File(win ? "c:/any" + suffix + "/test.txt" : "/any" + suffix + "/test.txt"),
                new File(win ? "C:\\ANY" + suffix.toUpperCase() + "\\test.txt" : "/ANY" + suffix.toUpperCase() + "/test.txt"));
        testEqualsAndHashCode(
                new File(win ? "c:\\any" + suffix + "\\test.txt" : "/any" + suffix + "/test.txt"),
                new File(win ? "C:/ANY" + suffix.toUpperCase() + "/test.txt" : "/ANY" + suffix.toUpperCase() + "/test.txt"));
        testEqualsAndHashCode(
                new File(win ? "c:/any" + suffix + "/test.txt" : "/any" + suffix + "/test.txt"),
                new File(win ? "C:/ANY" + suffix.toUpperCase() + "/test.txt" : "/ANY" + suffix.toUpperCase() + "/test.txt"));
        if (win) {
            // These tests may take very long, depending on the network
            // configuration of the local system.
            testEqualsAndHashCode(
                    new File("\\\\localhost\\any" + suffix + "\\test.txt"),
                    new File("\\\\LOCALHOST\\ANY" + suffix.toUpperCase() + "\\test.txt"));
            testEqualsAndHashCode(
                    new File("//localhost/any" + suffix + "/test.txt"),
                    new File("\\\\LOCALHOST\\ANY" + suffix.toUpperCase() + "\\test.txt"));
            testEqualsAndHashCode(
                    new File("\\\\localhost\\any" + suffix + "\\test.txt"),
                    new File("//LOCALHOST/ANY" + suffix.toUpperCase() + "/test.txt"));
            testEqualsAndHashCode(
                    new File("//localhost/any" + suffix + "/test.txt"),
                    new File("//LOCALHOST/ANY" + suffix.toUpperCase() + "/test.txt"));
        }
        final File l = new File(win ? "c:\\any" + suffix + "\\test.txt" : "/any" + suffix + "/test.txt");
        final File u = new File(win ? "c:\\any" + suffix + "\\TEST.TXT" : "/any" + suffix + "/TEST.TXT");
        assertFalse(l.equals(u));
        assertFalse(u.equals(l));
    }

    void testEqualsAndHashCode(File a, File b) {
        if (File.separatorChar == '\\') {
            assertTrue(a.equals(b));
            assertTrue(b.equals(a));
            assertEquals(a.hashCode(), b.hashCode());
        } else {
            assertFalse(a.equals(b));
            assertFalse(b.equals(a));
        }
        assertEquals(a.hashCode(), a.hashCode()); // multiple calls need to yield the same value
        assertEquals(b.hashCode(), b.hashCode());
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
        final File archive2 = inner2.getParentFile();
        in.close();

        assertNotSame(inner, inner2);
        assertNotSame(archive, archive2);

        //
        // Test details of the persistet object graph - part of this is
        // repeated in the tests for DefaultArchiveDetector.
        //

        // Assert that detectors have been persistet.
        final ArchiveDetector archiveDetector = archive.getArchiveDetector();
        final ArchiveDetector archive2Detector = archive2.getArchiveDetector();
        assertNotSame(archiveDetector, archive2Detector);
        final ArchiveDetector innerDetector = inner.getArchiveDetector();
        final ArchiveDetector inner2Detector = inner2.getArchiveDetector();
        assertNotSame(innerDetector, inner2Detector);

        // Assert that drivers have been persistet.
        final ArchiveDriver<?> archiveDriver = archiveDetector.getArchiveDriver(archive.getPath());
        final ArchiveDriver<?> archive2Driver = archive2Detector.getArchiveDriver(archive2.getPath());
        assertNotSame(archiveDriver, archive2Driver);
        final ArchiveDriver<?> innerDriver = innerDetector.getArchiveDriver(inner.getPath());
        final ArchiveDriver<?> inner2Driver = inner2Detector.getArchiveDriver(inner2.getPath());
        assertNotSame(innerDriver, inner2Driver);

        // Assert that the controllers haven't been persistet.
        final FileSystemController<?> archiveController = archive.getController();
        final FileSystemController<?> archive2Controller = archive2.getController();
        assertSame(archiveController, archive2Controller);
        final FileSystemController<?> innerController = inner.getController();
        final FileSystemController<?> inner2Controller = inner2.getController();
        assertSame(innerController, inner2Controller);
    }

    public void testGetOutermostArchive() {
        File file = new File("abc/def" + suffix + "/efg" + suffix + "/hij" + suffix + "/test.txt");
        assertEquals(new java.io.File("abc/def" + suffix), file.getTopLevelArchive());
    }
}
