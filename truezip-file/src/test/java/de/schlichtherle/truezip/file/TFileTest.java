/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.file;

import static de.schlichtherle.truezip.file.TArchiveDetector.NULL;
import static de.schlichtherle.truezip.fs.FsEntryName.ROOT;
import de.schlichtherle.truezip.fs.FsPath;
import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import static java.io.File.separator;
import static java.io.File.separatorChar;
import java.io.*;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.util.ServiceConfigurationError;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests archive type independent features of the {@link TFile} class.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class TFileTest extends MockArchiveDriverTestBase {

    private static final Logger logger
            = Logger.getLogger(TFileTest.class.getName());

    private TFile archive;

    @Override
    public void setUp() throws IOException {
        super.setUp();
        archive = new TFile("archive" + getSuffix());
    }

    @Override
    public void tearDown() {
        try {
            assert !new File("archive" + getSuffix()).exists();
        } finally {
            super.tearDown();
        }
    }

    @Test
    public void testFileExtension() {
        assert !new TFile("test.file").isArchive();
    }

    @Test
    public void testValidPathConstructor() {
        for (final String[] params : new String[][] {
            { "mok2:mok1:file:/foo.mok1!/bar.mok2!/META-INF/MANIFEST.MF", "/foo.mok1/bar.mok2/META-INF/MANIFEST.MF", "/foo.mok1/bar.mok2", "/foo.mok1/bar.mok2", "META-INF/MANIFEST.MF", },
            { "mok2:mok1:file:/foo.mok1!/bar.mok2!/", "/foo.mok1/bar.mok2", "/foo.mok1/bar.mok2", "/foo.mok1", "bar.mok2", },
            { "mok1:file:/foo.mok1!/META-INF/MANIFEST.MF", "/foo.mok1/META-INF/MANIFEST.MF", "/foo.mok1", "/foo.mok1", "META-INF/MANIFEST.MF", },
            { "mok1:file:/foo.mok1!/", "/foo.mok1", "/foo.mok1", null, null, },
            { "mok2:file:/foo.mok2!/META-INF/MANIFEST.MF", "/foo.mok2/META-INF/MANIFEST.MF", "/foo.mok2", "/foo.mok2", "META-INF/MANIFEST.MF", },
            { "mok2:file:/foo.mok2!/", "/foo.mok2", "/foo.mok2", null, null, },
            { "file:/foo", "/foo", null, null, null, },
            { "file:/", "/", null, null, null, },
        }) {
            final TFile file = new TFile(FsPath.create(URI.create(params[0])));
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
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testUriConstructor() throws Exception {
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

        file = new TFile(new URI("file", "/a .mok/b .mok/", null));
        assertNull(file.getInnerArchive());
        assertNull(file.getInnerEntryName());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());

        file = new TFile(new URI("file", "/a .mok/b .mok", null));
        assertNull(file.getInnerArchive());
        assertNull(file.getInnerEntryName());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());

        // One ZIP file in path.
        file = new TFile(new URI("mok", "file:/a .mok/b .mok!/", null));
        assertSame(file, file.getInnerArchive());
        assertSame(ROOT, file.getInnerFsEntryName());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());

        file = new TFile(new URI("mok", "file:/a .mok!/b .mok", null));
        assertSame(file.getInnerArchive(), file.getEnclArchive());
        assertSame(file.getInnerEntryName(), file.getEnclEntryName());
        assertEquals(fs + "a .mok", file.getEnclArchive().getPath());
        assertEquals("b .mok", file.getEnclEntryName());

        // One ZIP file in path with one redundant mok: scheme.

        try {
            new TFile(new URI("mok", "mok:file:/a .mok/b .mok!/", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        try {
            new TFile(new URI("mok", "mok:file:/a .mok/b .mok!", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        try {
            new TFile(new URI("mok", "mok:file:/a .mok!/b .mok/", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        try {
            new TFile(new URI("mok", "mok:file:/a .mok!/b .mok", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        // One ZIP file in path with two redundant mok: schemes.

        try {
            new TFile(new URI("mok", "mok:mok:file:/a .mok/b .mok!/", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        try {
            new TFile(new URI("mok", "mok:mok:file:/a .mok/b .mok!", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        try {
            new TFile(new URI("mok", "mok:mok:file:/a .mok!/b .mok/", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        try {
            new TFile(new URI("mok", "mok:mok:file:/a .mok!/b .mok", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        // Two ZIP files in path.

        file = new TFile(new URI("mok", "mok:file:/a .mok!/b .mok!/", null));
        assertSame(file, file.getInnerArchive());
        assertSame(ROOT, file.getInnerFsEntryName());
        assertEquals(fs + "a .mok", file.getEnclArchive().getPath());
        assertEquals("b .mok", file.getEnclEntryName());

        // One ZIP file in path with one misleading '!' in path.

        file = new TFile(new URI("mok", "file:/a .mok!/b .mok!/", null));
        assertSame(file, file.getInnerArchive());
        assertSame(ROOT, file.getInnerFsEntryName());
        assertNull(file.getEnclArchive());
        assertNull(file.getEnclEntryName());

        // Three ZIP files in path with one ZIP file removed by normalization
        // and hence one redundant mok: scheme.

        try {
            new TFile(new URI("mok", "mok:mok:file:/a .mok!/b .mok!/../c .mok!/", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        try {
            new TFile(new URI("mok", "mok:mok:file:/a .mok!/b .mok!/../c .mok!", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        // One ZIP file in path which is removed by normalization.

        try {
            new TFile(new URI("mok", "file:/a .mok!/../b .mok/", null));
            fail();
        } catch (IllegalArgumentException ex) {
        }

        try {
            new TFile(new URI("mok", "file:/a .mok!/../b .mok", null));
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
            final String innerName = "inner.mok";
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

        final TFile a = new TFile("outer.mok/removed.mok");
        TFile b, c;

        b = new TFile("../removed.dir/removed.dir/../../dir/./inner.mok");
        c = new TFile(a, b.getPath());
        assertTrue(c.isArchive());
        assertTrue(c.isEntry());
        assertEquals("outer.mok",
                c.getEnclArchive().getPath());
        assertEquals("dir/inner.mok",
                c.getEnclEntryName());

        b = new TFile("../removed.dir/removed.dir/../../dir/./inner.mok");
        c = new TFile(a, b.getPath(), NULL);
        assertFalse(c.isArchive());
        assertTrue(c.isEntry());
        assertEquals("outer.mok",
                c.getInnerArchive().getPath());
        assertEquals("dir/inner.mok",
                c.getInnerEntryName());

        b = new TFile("../removed.dir/removed.dir/../../dir/./inner.mok"
                + "/removed.dir/removed.dir/../../dir/./test.txt");
        c = new TFile(a, b.getPath());
        assertFalse(c.isArchive());
        assertTrue(c.isEntry());
        assertEquals("outer.mok" + fs + "removed.mok" + fs + ".."
                + fs + "removed.dir" + fs + "removed.dir" + fs + ".." + fs
                + ".." + fs + "dir" + fs + "." + fs + "inner.mok",
                c.getInnerArchive().getPath());
        assertEquals("dir/inner.mok",
                c.getInnerArchive().getEnclEntryName());
    }

    @Test
    public void testGetParentFile() {
        TFile abcdefgh = new TFile("a/b.mok/c/d/e.mok/f.mok/g/h.mok");
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
        TFile file = new TFile("abc/def.mok/efg.mok/hij.mok/test.txt");
        assertEquals(new File("abc/def.mok"), file.getTopLevelArchive());
    }

    @Test
    public void testUriAndFsPath() {
        for (final String[] params : new String[][] {
            { "/file", "file:/file" },
            { "/archive.mok", "mok:file:/archive.mok!/" },
            { "/archive.mok/entry", "mok:file:/archive.mok!/entry" },
            { "/foo.mok1/bar.mok2", "mok2:mok1:file:/foo.mok1!/bar.mok2!/" },
            { "/dist.mok1/app.mok2/META-INF/MANIFEST.MF", "mok2:mok1:file:/dist.mok1!/app.mok2!/META-INF/MANIFEST.MF" },
        }) {
            final String name = params[0];
            final URI uri = URI.create(params[1]);
            final FsPath path = FsPath.create(URI.create(params[1]));
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
            { "mok:file:/archive.mok!/" },
            { "mok:file:/archive.mok!/entry" },
            { "mok2:mok1:file:/foo.mok1!/bar.mok2!/" },
            { "mok2:mok1:file:/foo.mok1!/bar.mok2!/META-INF/MANIFEST.MF" },
            { "mok2:mok1:file:/föö%20bär.mok1!/föö%20bär.mok2!/föö%20bär" },
            { "mok:file:/föö%20bär.mok!/föö%20bär" },
            { "file:/föö%20bär/föö%20bär" },
            { "mok:file:/foo.mok!/bar" },
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

    /**
     * Tests issue #TRUEZIP-154.
     * 
     * @see    <a href="http://java.net/jira/browse/TRUEZIP-154">ServiceConfigurationError: Unknown file system scheme for path without a suffix</a>
     */
    @Test
    public void testIssue154() {
        for (String param : new String[] {
            "mok:file:/foo!/",
            "mok:mok:file:/foo!/bar!/",
        }) {
            FsPath path = FsPath.create(URI.create(param));
            try {
                assertIssue154(new TFile(path));
                assertIssue154(new TFile(path.toUri()));
            } catch (ServiceConfigurationError error) {
                throw new AssertionError(param, error);
            }
        }
    }

    private void assertIssue154(TFile file) {
        for (; null != file; file = file.getEnclArchive()) {
            assertTrue(file.isArchive());
            file.exists(); // don't care for the result
        }
    }
}
