/*
 * Copyright (C) 2007-2011 Schlichtherle IT Services
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

import java.io.File;
import java.util.logging.Logger;
import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.logging.Level;
import de.schlichtherle.truezip.fs.archive.DummyArchiveDriver;
import de.schlichtherle.truezip.fs.FsPath;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static de.schlichtherle.truezip.file.TDefaultArchiveDetector.NULL;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import static java.io.File.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Tests archive type independent features of the {@link TFile} class.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class TFileTest {

    private static final Logger logger
            = Logger.getLogger(TFileTest.class.getName());

    private static final TDefaultArchiveDetector DETECTOR
            = new TDefaultArchiveDetector(
                "ear|exe|jar|odb|odf|odg|odm|odp|ods|odt|otg|oth|otp|ots|ott|tar|tar.bz2|tar.gz|tbz2|tgz|tzp|war|zip|zip.rae|zip.raes",
                new DummyArchiveDriver());

    private TFile archive;
    private String scheme;

    @Before
    public void setUp() {
        TFile.setDefaultArchiveDetector(DETECTOR);
        scheme = "zip";
        archive = new TFile("archive.zip");
    }

    @After
    public void tearDown() {
        assertFalse(new File("archive.zip").exists());
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
            final TFile file = new TFile(FsPath.create(params[0]));
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
            assertThat(new TFile(file.toFsPath()), equalTo(file.getNormalizedAbsoluteFile()));
            assertThat(new TFile(file.toURI()), equalTo(file.getAbsoluteFile()));
        }
    }

    @Test
    public void testURIConstructor() throws Exception {
        TFile file;
        final String fs = separator;

        // Whitespace in path. See issue #1. Thanks to mrudat for submitting this!
        file = new TFile(new URI("file", "/with a space", null));
        assertEquals("with a space", file.getName());
        assertNull(file.getInnerArchive());
        assertNull(file.getInnerEntryName());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());

        // No ZIP file in path.

        file = new TFile(new URI("file", "/a ." + scheme + "/b ." + scheme + "/", null));
        assertNull(file.getInnerArchive());
        assertNull(file.getInnerEntryName());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());

        file = new TFile(new URI("file", "/a ." + scheme + "/b ." + scheme + "", null));
        assertNull(file.getInnerArchive());
        assertNull(file.getInnerEntryName());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());

        // One ZIP file in path.
        file = new TFile(new URI(scheme, "file:/a ." + scheme + "/b ." + scheme + "!/", null));
        assertSame(file, file.getInnerArchive());
        assertSame(ROOT, file.getInnerEntryName0());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());

        file = new TFile(new URI(scheme, "file:/a ." + scheme + "!/b ." + scheme + "", null));
        assertSame(file.getInnerArchive(), file.getEnclArchive());
        assertSame(file.getInnerEntryName(), file.getEnclEntryName());
        assertEquals(fs + "a ." + scheme + "", file.getEnclArchive().getPath());
        assertEquals("b ." + scheme + "", file.getEnclEntryName());

        // One ZIP file in path with one redundant jar: scheme.

        try {
            file = new TFile(new URI(scheme, scheme + ":file:/a ." + scheme + "/b ." + scheme + "!/", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        try {
            file = new TFile(new URI(scheme, scheme + ":file:/a ." + scheme + "/b ." + scheme + "!", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        try {
            file = new TFile(new URI(scheme, scheme + ":file:/a ." + scheme + "!/b ." + scheme + "/", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        try {
            file = new TFile(new URI(scheme, scheme + ":file:/a ." + scheme + "!/b ." + scheme + "", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        // One ZIP file in path with two redundant jar: schemes.

        try {
            file = new TFile(new URI(scheme, scheme + ":" + scheme + ":file:/a ." + scheme + "/b ." + scheme + "!/", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        try {
            file = new TFile(new URI(scheme, scheme + ":" + scheme + ":file:/a ." + scheme + "/b ." + scheme + "!", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        try {
            file = new TFile(new URI(scheme, scheme + ":" + scheme + ":file:/a ." + scheme + "!/b ." + scheme + "/", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        try {
            file = new TFile(new URI(scheme, scheme + ":" + scheme + ":file:/a ." + scheme + "!/b ." + scheme + "", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        // Two ZIP files in path.

        file = new TFile(new URI(scheme, scheme + ":file:/a ." + scheme + "!/b ." + scheme + "!/", null));
        assertSame(file, file.getInnerArchive());
        assertSame(ROOT, file.getInnerEntryName0());
        assertEquals(fs + "a ." + scheme + "", file.getEnclArchive().getPath());
        assertEquals("b ." + scheme + "", file.getEnclEntryName());

        // One ZIP file in path with one misleading '!' in path.

        file = new TFile(new URI(scheme, "file:/a ." + scheme + "!/b ." + scheme + "!/", null));
        assertSame(file, file.getInnerArchive());
        assertSame(ROOT, file.getInnerEntryName0());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());

        // Three ZIP files in path with one ZIP file removed by normalization
        // and hence one redundant jar: scheme.

        try {
            file = new TFile(new URI(scheme, scheme + ":" + scheme + ":file:/a ." + scheme + "!/b ." + scheme + "!/../c ." + scheme + "!/", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        try {
            file = new TFile(new URI(scheme, scheme + ":" + scheme + ":file:/a ." + scheme + "!/b ." + scheme + "!/../c ." + scheme + "!", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        // One ZIP file in path which is removed by normalization.

        try {
            file = new TFile(new URI(scheme, "file:/a ." + scheme + "!/../b ." + scheme + "/", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        try {
            file = new TFile(new URI(scheme, "file:/a ." + scheme + "!/../b ." + scheme + "", null));
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
                new TFile("x", (String) null);
                fail("Expected NullPointerException!");
            } catch (NullPointerException expected) {
            }

            try {
                new TFile(new TFile("x"), (String) null);
                fail("Expected NullPointerException!");
            } catch (NullPointerException expected) {
            }
        }

        final String fs = separator;

        {
            final TFile[] files = {
                new TFile(archive, ""),
                new TFile(archive, "."),
                new TFile(archive, "." + fs),
                new TFile(archive, "." + fs + "."),
                new TFile(archive, "." + fs + "." + fs),
                new TFile(archive, "." + fs + "." + fs + "."),
            };
            for (int i = 0; i < files.length; i++) {
                final TFile file = files[i];
                assertSame(file, file.getInnerArchive());
                assertEquals("", file.getInnerEntryName());
                assertNull(file.getEnclArchive());
                assertNull(file.getEnclEntryName());
            }
        }

        {
            final String innerName = "inner." + scheme;
            final TFile inner = new TFile(archive, innerName);
            final TFile[] files = {
                new TFile(inner, ""),
                new TFile(inner, "."),
                new TFile(inner, "." + fs),
                new TFile(inner, "." + fs + "."),
                new TFile(inner, "." + fs + "." + fs),
                new TFile(inner, "." + fs + "." + fs + "."),
            };
            for (int i = 0; i < files.length; i++) {
                final TFile file = files[i];
                assertSame(file, file.getInnerArchive());
                assertEquals("", file.getInnerEntryName());
                assertSame(archive, file.getEnclArchive());
                assertEquals(innerName, file.getEnclEntryName());
            }
        }

        {
            final String entryName = "entry";
            final TFile entry = new TFile(archive, entryName);
            final TFile[] files = {
                new TFile(entry, ""),
                new TFile(entry, "."),
                new TFile(entry, "." + fs),
                new TFile(entry, "." + fs + "."),
                new TFile(entry, "." + fs + "." + fs),
                new TFile(entry, "." + fs + "." + fs + "."),
            };
            for (int i = 0; i < files.length; i++) {
                final TFile file = files[i];
                assertSame(archive, file.getInnerArchive());
                assertEquals(entryName, file.getInnerEntryName());
                assertSame(archive, file.getEnclArchive());
                assertEquals(entryName, file.getEnclEntryName());
            }
        }

        final TFile a = new TFile("outer." + scheme + "/removed." + scheme);
        TFile b, c;

        b = new TFile("../removed.dir/removed.dir/../../dir/./inner." + scheme);
        c = new TFile(a, b.getPath());
        assertTrue(c.isArchive());
        assertTrue(c.isEntry());
        assertEquals("outer." + scheme,
                c.getEnclArchive().getPath());
        assertEquals("dir/inner." + scheme,
                c.getEnclEntryName());

        b = new TFile("../removed.dir/removed.dir/../../dir/./inner." + scheme);
        c = new TFile(a, b.getPath(), NULL);
        assertFalse(c.isArchive());
        assertTrue(c.isEntry());
        assertEquals("outer." + scheme,
                c.getInnerArchive().getPath());
        assertEquals("dir/inner." + scheme,
                c.getInnerEntryName());

        b = new TFile("../removed.dir/removed.dir/../../dir/./inner."
                + scheme + "/removed.dir/removed.dir/../../dir/./test.txt");
        c = new TFile(a, b.getPath());
        assertFalse(c.isArchive());
        assertTrue(c.isEntry());
        assertEquals("outer." + scheme + fs + "removed." + scheme + fs + ".."
                + fs + "removed.dir" + fs + "removed.dir" + fs + ".." + fs
                + ".." + fs + "dir" + fs + "." + fs + "inner." + scheme,
                c.getInnerArchive().getPath());
        assertEquals("dir/inner." + scheme,
                c.getInnerArchive().getEnclEntryName());
    }

    @Test
    public void testGetParentFile() {
        TFile abcdefgh = new TFile("a/b." + scheme + "/c/d/e." + scheme + "/f." + scheme + "/g/h." + scheme + "");
        TFile abcdefg  = abcdefgh.getParentFile();
        TFile abcdef   = abcdefg .getParentFile();
        TFile abcde    = abcdef  .getParentFile();
        TFile abcd     = abcde   .getParentFile();
        TFile abc      = abcd    .getParentFile();
        TFile ab       = abc     .getParentFile();
        TFile a        = ab      .getParentFile();
        TFile n        = a       .getParentFile();
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
        File resultFile = new File(result).getCanonicalFile();
        File pathFile = new File(new TFile(path).getNormalizedAbsoluteFile().getPath());
        assertEquals(resultFile, pathFile);
    }

    @Test
    public void testGetTopLevelArchive() {
        TFile file = new TFile("abc/def." + scheme + "/efg." + scheme + "/hij." + scheme + "/test.txt");
        assertEquals(new File("abc/def." + scheme), file.getTopLevelArchive());
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
            final TFile file = new TFile(name);
            assertThat(new TFile(name), equalTo(file));
            assertThat(new TFile(uri), equalTo(file));
            assertThat(new TFile(path), equalTo(file));
            assertThat(new TFile(name).toURI(), equalTo(file.toURI()));
            assertThat(new TFile(uri).toURI(), equalTo(file.toURI()));
            assertThat(new TFile(path).toURI(), equalTo(file.toURI()));
            assertThat(new TFile(name).toFsPath(), equalTo(file.toFsPath()));
            assertThat(new TFile(uri).toFsPath(), equalTo(file.toFsPath()));
            assertThat(new TFile(path).toFsPath(), equalTo(file.toFsPath()));
            assertThat(new TFile(new TFile(name).toURI()), equalTo(file.getAbsoluteFile()));
            assertThat(new TFile(new TFile(uri).toURI()), equalTo(file.getAbsoluteFile()));
            assertThat(new TFile(new TFile(path).toURI()), equalTo(file.getAbsoluteFile()));
            assertThat(new TFile(new TFile(name).toFsPath()), equalTo(file.getAbsoluteFile()));
            assertThat(new TFile(new TFile(uri).toFsPath()), equalTo(file.getAbsoluteFile()));
            assertThat(new TFile(new TFile(path).toFsPath()), equalTo(file.getAbsoluteFile()));
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
            final TFile original = new TFile(URI.create(params[0]));

            {
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                final ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(original);
                oos.close();

                logger.log(Level.FINE, "Number of serialized bytes: {0}", bos.size());

                final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
                final ObjectInputStream ois = new ObjectInputStream(bis);
                final TFile clone = (TFile) ois.readObject();
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
                final TFile clone = (TFile) dec.readObject();
                dec.close();

                assertThat(clone, not(sameInstance(original)));
                assertThat(clone, equalTo(original.getAbsoluteFile()));
            }
        }
    }
}
