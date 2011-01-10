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

import java.util.logging.Logger;
import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.logging.Level;
import de.schlichtherle.truezip.io.fs.archive.driver.DummyArchiveDriver;
import de.schlichtherle.truezip.io.fs.FsPath;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static de.schlichtherle.truezip.io.file.ArchiveDetector.NULL;
import static de.schlichtherle.truezip.io.fs.FsEntryName.*;
import static java.io.File.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Tests archive type independent features of the {@link File} class.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FileTest {

    private static final Logger logger
            = Logger.getLogger(FileTest.class.getName());

    private static final DefaultArchiveDetector DETECTOR
            = new DefaultArchiveDetector(
                "ear|exe|jar|odb|odf|odg|odm|odp|ods|odt|otg|oth|otp|ots|ott|tar|tar.bz2|tar.gz|tbz2|tgz|tzp|war|zip|zip.rae|zip.raes",
                new DummyArchiveDriver());

    private File archive;
    private String suffix;

    @Before
    public void setUp() {
        File.setDefaultArchiveDetector(DETECTOR);
        suffix = ".zip";
        archive = new File("archive.zip");
    }

    @After
    public void tearDown() {
        assertFalse(new java.io.File("archive.zip").exists());
    }

    @Test
    public void testValidPathConstructor() {
        for (final String[] params : new String[][] {
            { "jar:tar.gz:file:/app.tar.gz!/app.jar!/META-INF/MANIFEST.MF", "/app.tar.gz/app.jar/META-INF/MANIFEST.MF", "/app.tar.gz/app.jar", "/app.tar.gz/app.jar", "META-INF/MANIFEST.MF", },
            { "jar:tar.gz:file:/app.tar.gz!/app.jar!/", "/app.tar.gz/app.jar", "/app.tar.gz/app.jar", "/app.tar.gz", "app.jar", },
            { "tar.gz:file:/archive.tar.gz!/META-INF/MANIFEST.MF", "/archive.tar.gz/META-INF/MANIFEST.MF", "/archive.tar.gz", "/archive.tar.gz", "META-INF/MANIFEST.MF", },
            { "tar.gz:file:/archive.tar.gz!/", "/archive.tar.gz", "/archive.tar.gz", null, null, },
            { "zip:file:/archive.zip!/META-INF/MANIFEST.MF", "/archive.zip/META-INF/MANIFEST.MF", "/archive.zip", "/archive.zip", "META-INF/MANIFEST.MF", },
            { "zip:file:/archive.zip!/", "/archive.zip", "/archive.zip", null, null, },
            { "file:/foo", "/foo", null, null, null, },
            { "file:/", "/", null, null, null, },
        }) {
            final File file = new File(FsPath.create(params[0]));
            assertThat(file.getPath(), equalTo(params[1].replace('/', separatorChar)));
            if (null != params[2]) {
                assertThat(file.getInnerArchive().getPath(), equalTo(params[2].replace('/', separatorChar)));
            } else {
                assertThat(file.getInnerArchive(), nullValue());
            }
            if (null != params[3]) {
                assertThat(file.getEnclArchive().getPath(), equalTo(params[3].replace('/', separatorChar)));
                assertThat(file.getEnclEntryName(), equalTo(params[4]));
            } else {
                assertThat(file.getEnclArchive(), nullValue());
                assertThat(file.getEnclEntryName(), nullValue());
            }
            assertThat(new File(file.toFsPath()), equalTo(file.getNormalizedAbsoluteFile()));
            assertThat(new File(file.toURI()), equalTo(file.getAbsoluteFile()));
        }
    }

    @Test
    public void testURIConstructor() throws Exception {
        File file;
        final String fs = separator;

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
        assertSame(ROOT, file.getInnerEntryName0());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());

        file = new File(new URI("jar", "file:/a " + suffix + "!/b " + suffix + "", null));
        assertSame(file.getInnerArchive(), file.getEnclArchive());
        assertSame(file.getInnerEntryName(), file.getEnclEntryName());
        assertEquals(fs + "a " + suffix + "", file.getEnclArchive().getPath());
        assertEquals("b " + suffix + "", file.getEnclEntryName());

        // One ZIP file in path with one redundant jar: scheme.

        try {
            file = new File(new URI("jar", "jar:file:/a " + suffix + "/b " + suffix + "!/", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        try {
            file = new File(new URI("jar", "jar:file:/a " + suffix + "/b " + suffix + "!", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        try {
            file = new File(new URI("jar", "jar:file:/a " + suffix + "!/b " + suffix + "/", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        try {
            file = new File(new URI("jar", "jar:file:/a " + suffix + "!/b " + suffix + "", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        // One ZIP file in path with two redundant jar: schemes.

        try {
            file = new File(new URI("jar", "jar:jar:file:/a " + suffix + "/b " + suffix + "!/", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        try {
            file = new File(new URI("jar", "jar:jar:file:/a " + suffix + "/b " + suffix + "!", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        try {
            file = new File(new URI("jar", "jar:jar:file:/a " + suffix + "!/b " + suffix + "/", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        try {
            file = new File(new URI("jar", "jar:jar:file:/a " + suffix + "!/b " + suffix + "", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        // Two ZIP files in path.

        file = new File(new URI("jar", "jar:file:/a " + suffix + "!/b " + suffix + "!/", null));
        assertSame(file, file.getInnerArchive());
        assertSame(ROOT, file.getInnerEntryName0());
        assertEquals(fs + "a " + suffix + "", file.getEnclArchive().getPath());
        assertEquals("b " + suffix + "", file.getEnclEntryName());

        // One ZIP file in path with one misleading '!' in path.

        file = new File(new URI("jar", "file:/a " + suffix + "!/b " + suffix + "!/", null));
        assertSame(file, file.getInnerArchive());
        assertSame(ROOT, file.getInnerEntryName0());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());

        // Three ZIP files in path with one ZIP file removed by normalization
        // and hence one redundant jar: scheme.

        try {
            file = new File(new URI("jar", "jar:jar:file:/a " + suffix + "!/b " + suffix + "!/../c " + suffix + "!/", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        try {
            file = new File(new URI("jar", "jar:jar:file:/a " + suffix + "!/b " + suffix + "!/../c " + suffix + "!", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        // One ZIP file in path which is removed by normalization.

        try {
            file = new File(new URI("jar", "file:/a " + suffix + "!/../b " + suffix + "/", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        try {
            file = new File(new URI("jar", "file:/a " + suffix + "!/../b " + suffix + "", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }
    }

    @Test
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

        final String fs = separator;

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
        c = new File(a, b.getPath(), NULL);
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
    
    @Test
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

    @Test
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

    @Test
    public void testGetTopLevelArchive() {
        File file = new File("abc/def" + suffix + "/efg" + suffix + "/hij" + suffix + "/test.txt");
        assertEquals(new java.io.File("abc/def" + suffix), file.getTopLevelArchive());
    }

    @Test
    public void testURIandFsPath() {
        for (final String[] params : new String[][] {
            { "/file", "file:/file" },
            { "/archive.zip", "zip:file:/archive.zip!/" },
            { "/archive.zip/entry", "zip:file:/archive.zip!/entry" },
            { "/app.tar.gz/app.jar", "jar:tar.gz:file:/app.tar.gz!/app.jar!/" },
            { "/app.tar.gz/app.jar/META-INF/MANIFEST.MF", "jar:tar.gz:file:/app.tar.gz!/app.jar!/META-INF/MANIFEST.MF" },
        }) {
            final String name = params[0];
            final URI uri = URI.create(params[1]);
            final FsPath path = FsPath.create(params[1]);
            final File file = new File(name);
            assertThat(new File(name), equalTo(file));
            assertThat(new File(uri), equalTo(file));
            assertThat(new File(path), equalTo(file));
            assertThat(new File(name).toURI(), equalTo(file.toURI()));
            assertThat(new File(uri).toURI(), equalTo(file.toURI()));
            assertThat(new File(path).toURI(), equalTo(file.toURI()));
            assertThat(new File(name).toFsPath(), equalTo(file.toFsPath()));
            assertThat(new File(uri).toFsPath(), equalTo(file.toFsPath()));
            assertThat(new File(path).toFsPath(), equalTo(file.toFsPath()));
            assertThat(new File(new File(name).toURI()), equalTo(file.getAbsoluteFile()));
            assertThat(new File(new File(uri).toURI()), equalTo(file.getAbsoluteFile()));
            assertThat(new File(new File(path).toURI()), equalTo(file.getAbsoluteFile()));
            assertThat(new File(new File(name).toFsPath()), equalTo(file.getAbsoluteFile()));
            assertThat(new File(new File(uri).toFsPath()), equalTo(file.getAbsoluteFile()));
            assertThat(new File(new File(path).toFsPath()), equalTo(file.getAbsoluteFile()));
        }
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        final ExceptionListener listener = new ExceptionListener() {
            @Override
            public void exceptionThrown(Exception ex) {
                throw new UndeclaredThrowableException(ex);
            }
        };

        for (final String[] params : new String[][] {
            { "file:/file" },
            { "zip:file:/archive.zip!/" },
            { "zip:file:/archive.zip!/entry" },
            { "jar:tar.gz:file:/app.tar.gz!/app.jar!/" },
            { "jar:tar.gz:file:/app.tar.gz!/app.jar!/META-INF/MANIFEST.MF" },
            { "zip:zip:file:/föö%20bär.zip!/föö%20bär.zip!/föö%20bär" },
            { "zip:file:/föö%20bär.zip!/föö%20bär" },
            { "file:/föö%20bär/föö%20bär" },
            { "zip:file:/foo.zip!/bar" },
            { "file:/foo/bar" },
            { "file:/foo/bar" },
        }) {
            final File original = new File(URI.create(params[0]));

            {
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                final ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(original);
                oos.close();

                logger.log(Level.FINE, "Number of serialized bytes: {0}", bos.size());

                final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
                final ObjectInputStream ois = new ObjectInputStream(bis);
                final File clone = (File) ois.readObject();
                ois.close();

                assertThat(clone, not(sameInstance(original)));
                assertThat(clone, equalTo(original.getAbsoluteFile()));
            }

            {
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                final XMLEncoder enc = new XMLEncoder(bos);
                enc.setExceptionListener(listener);
                enc.writeObject(original);
                enc.close();

                logger.log(Level.FINE, bos.toString("UTF-8"));

                final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
                final XMLDecoder dec = new XMLDecoder(bis);
                final File clone = (File) dec.readObject();
                dec.close();

                assertThat(clone, not(sameInstance(original)));
                assertThat(clone, equalTo(original.getAbsoluteFile()));
            }
        }
    }
}
