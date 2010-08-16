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

package de.schlichtherle.io;

import java.io.*;
import java.net.*;

import junit.framework.*;

/**
 * Tests archive type independent features of the {@link File} class.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class GeneralFileTest extends TestCase {

    private String suffix;

    public GeneralFileTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        suffix = ".zip";
    }

    protected void tearDown() throws Exception {
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
        assertSame(Entry.ROOT_NAME, file.getInnerEntryName());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());

        file = new File(new URI("jar", "file:/a " + suffix + "/b " + suffix + "!", null));
        assertSame(file, file.getInnerArchive());
        assertSame(Entry.ROOT_NAME, file.getInnerEntryName());
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
        assertSame(Entry.ROOT_NAME, file.getInnerEntryName());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());

        file = new File(new URI("jar", "jar:file:/a " + suffix + "/b " + suffix + "!", null));
        assertSame(file, file.getInnerArchive());
        assertSame(Entry.ROOT_NAME, file.getInnerEntryName());
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
        assertSame(Entry.ROOT_NAME, file.getInnerEntryName());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());

        file = new File(new URI("jar", "jar:jar:file:/a " + suffix + "/b " + suffix + "!", null));
        assertSame(file, file.getInnerArchive());
        assertSame(Entry.ROOT_NAME, file.getInnerEntryName());
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
        assertSame(Entry.ROOT_NAME, file.getInnerEntryName());
        assertEquals(fs + "a " + suffix + "", file.getEnclArchive().getPath());
        assertEquals("b " + suffix + "", file.getEnclEntryName());

        file = new File(new URI("jar", "jar:file:/a " + suffix + "!/b " + suffix + "!", null));
        assertSame(file, file.getInnerArchive());
        assertSame(Entry.ROOT_NAME, file.getInnerEntryName());
        assertEquals(fs + "a " + suffix + "", file.getEnclArchive().getPath());
        assertEquals("b " + suffix + "", file.getEnclEntryName());

        // One ZIP file in path with one misleading '!' in path.

        file = new File(new URI("jar", "file:/a " + suffix + "!/b " + suffix + "!/", null));
        assertSame(file, file.getInnerArchive());
        assertSame(Entry.ROOT_NAME, file.getInnerEntryName());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());

        file = new File(new URI("jar", "file:/a " + suffix + "!/b " + suffix + "!", null));
        assertSame(file, file.getInnerArchive());
        assertSame(Entry.ROOT_NAME, file.getInnerEntryName());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());

        // Three ZIP files in path with one ZIP file removed by normalization.

        file = new File(new URI("jar", "jar:file:/a " + suffix + "!/b " + suffix + "!/../c " + suffix + "!/", null));
        assertSame(file, file.getInnerArchive());
        assertSame(Entry.ROOT_NAME, file.getInnerEntryName());
        assertEquals(fs + "a " + suffix + "", file.getEnclArchive().getPath());
        assertEquals("c " + suffix + "", file.getEnclEntryName());

        file = new File(new URI("jar", "jar:file:/a " + suffix + "!/b " + suffix + "!/../c " + suffix + "!", null));
        assertSame(file, file.getInnerArchive());
        assertSame(Entry.ROOT_NAME, file.getInnerEntryName());
        assertEquals(fs + "a " + suffix + "", file.getEnclArchive().getPath());
        assertEquals("c " + suffix + "", file.getEnclEntryName());

        // Three ZIP files in path with one ZIP file removed by normalization
        // and hence one redundant jar: scheme.

        file = new File(new URI("jar", "jar:jar:file:/a " + suffix + "!/b " + suffix + "!/../c " + suffix + "!/", null));
        assertSame(file, file.getInnerArchive());
        assertSame(Entry.ROOT_NAME, file.getInnerEntryName());
        assertEquals(fs + "a " + suffix + "", file.getEnclArchive().getPath());
        assertEquals("c " + suffix + "", file.getEnclEntryName());

        file = new File(new URI("jar", "jar:jar:file:/a " + suffix + "!/b " + suffix + "!/../c " + suffix + "!", null));
        assertSame(file, file.getInnerArchive());
        assertSame(Entry.ROOT_NAME, file.getInnerEntryName());
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
    
    public void testGetParentFile() {
        File abcdefgh = new File("a/b" + suffix + "/c/d/e" + suffix + "/f" + suffix + "/g/h" + suffix + "");
        File abcdefg  = (File) abcdefgh.getParentFile();
        File abcdef   = (File) abcdefg .getParentFile();
        File abcde    = (File) abcdef  .getParentFile();
        File abcd     = (File) abcde   .getParentFile();
        File abc      = (File) abcd    .getParentFile();
        File ab       = (File) abc     .getParentFile();
        File a        = (File) ab      .getParentFile();
        File n        = (File) a       .getParentFile();
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
     * Test of equals method, of class de.schlichtherle.io.File.
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
}
