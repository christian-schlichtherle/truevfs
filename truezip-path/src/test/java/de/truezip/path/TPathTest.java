/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.path;

import de.truezip.file.MockArchiveDriverTestBase;
import de.truezip.file.TArchiveDetector;
import de.truezip.file.TConfig;
import static de.truezip.kernel.addr.FsEntryName.SEPARATOR;
import static de.truezip.kernel.addr.FsEntryName.SEPARATOR_CHAR;
import de.truezip.kernel.addr.FsPath;
import de.truezip.kernel.io.Paths;
import static java.io.File.separatorChar;
import java.net.URI;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class TPathTest extends MockArchiveDriverTestBase {

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testIllegalConstructorParameters() {
        if ('\\' == separatorChar) {
            for (Object[] params : new Object[][] {
                // $first, $more
                { "c:", NO_STRINGS },
                { "c:foo", NO_STRINGS },
            }) {
                try {
                    new TPath(params[0].toString(), (String[]) params[1]);
                    fail();
                } catch (IllegalArgumentException expected) {
                }
            }
        }
    }

    @Test
    public void testStringConstructor() {
        if ('\\' == separatorChar) {
            for (Object[] params : new Object[][] {
                // $first, $more, $name, $address
                //{ "c:foo", NO_STRINGS, "c:foo", "file:/c:foo" },
                { "c:/foo", NO_STRINGS, "c:/foo", "file:/c:/foo" },
                //{ "//", NO_STRINGS, "/", ROOT_DIRECTORY },
                { "//foo", new String[] { "bar", "baz" }, "//foo/bar/baz", ROOT_DIRECTORY + "/foo/bar/baz" },
                { "///foo//", new String[] { "//bar//", "//", "//baz//" }, "//foo/bar/baz", ROOT_DIRECTORY + "/foo/bar/baz" },
            }) {
                assertConstructorWithStrings(params);
            }
        }
        for (Object[] params : new Object[][] {
            // $first, $more, $name, $address
            { "/", NO_STRINGS, "/", ROOT_DIRECTORY },
            { "/foo", NO_STRINGS, "/foo", ROOT_DIRECTORY + "foo" },
            { "/foo", new String[] { "" }, "/foo", ROOT_DIRECTORY + "foo"},
            { "/foo", new String[] { "bar" }, "/foo/bar", ROOT_DIRECTORY + "foo/bar"},
            //{ "///", NO_STRINGS, "/", ROOT_DIRECTORY },
            { "/foo", new String[] { "/bar" }, "/foo/bar", ROOT_DIRECTORY + "foo/bar"},
            { "/foo//", new String[] { "//", "//bar//", "" }, "/foo/bar", ROOT_DIRECTORY + "foo/bar"},
            { "/foo", new String[] { "" }, "/foo", ROOT_DIRECTORY + "foo"},
            { "/foo", new String[] { ".." }, "/foo/..", ROOT_DIRECTORY },
            { "/foo", new String[] { "/bar" }, "/foo/bar", ROOT_DIRECTORY + "foo/bar" },
            { "/foo.mok", new String[] { "/bar" }, "/foo.mok/bar", "mok:" + ROOT_DIRECTORY + "foo.mok!/bar" },
            { "/foo", new String[] { "/bar.mok" }, "/foo/bar.mok", "mok:" + ROOT_DIRECTORY + "foo/bar.mok!/" },
            { "/foo.mok", new String[] { "/bar.mok" }, "/foo.mok/bar.mok", "mok:mok:" + ROOT_DIRECTORY + "foo.mok!/bar.mok!/" },
            { "", new String[] { "/foo" }, "foo", CURRENT_DIRECTORY + "foo" },
            { "", new String[] { "foo" }, "foo", CURRENT_DIRECTORY + "foo" },
            { "", NO_STRINGS, "", CURRENT_DIRECTORY },
            { ".", NO_STRINGS, ".", CURRENT_DIRECTORY },
            { "foo", NO_STRINGS, "foo", CURRENT_DIRECTORY + "foo" },
            { "foo", new String[] { "" }, "foo", CURRENT_DIRECTORY + "foo"},
            { "foo", new String[] { ".." }, "foo/..", CURRENT_DIRECTORY },
            { "foo", new String[] { "bar" }, "foo/bar", CURRENT_DIRECTORY + "foo/bar" },
            { "foo.mok", new String[] { "bar" }, "foo.mok/bar", "mok:" + CURRENT_DIRECTORY + "foo.mok!/bar" },
            { "foo", new String[] { "bar.mok" }, "foo/bar.mok", "mok:" + CURRENT_DIRECTORY + "foo/bar.mok!/" },
            { "foo.mok", new String[] { "bar.mok" }, "foo.mok/bar.mok", "mok:mok:" + CURRENT_DIRECTORY + "foo.mok!/bar.mok!/" },
        }) {
            assertConstructorWithStrings(params);
        }
    }

    private static void assertConstructorWithStrings(Object... params) {
        final String first = params[0].toString();
        final String[] more = (String[]) params[1];
        final String name = params[2].toString();
        final FsPath address = FsPath.create(URI.create(params[3].toString()));
        final TPath path = new TPath(first, more);
        assertThat(path.toString(), equalTo(name.replace(SEPARATOR, path.getFileSystem().getSeparator())));
        assertThat(path.getMountPoint(), equalTo(address.getMountPoint()));
        assertThat(path.getEntryName(), equalTo(address.getEntryName()));
    }

    @Test
    public void testUriConstructor() {
        for (final Object[] params : new Object[][] {
            // $uri, $scheme, $succeeds
            { "", "file", true },
            { "/", "file", true },
            { "file:/", "file", true },
            { "foo:/", "foo", false },
        }) {
            final URI uri = URI.create(params[0].toString());
            final String scheme = params[1].toString();
            final boolean succeeds = (Boolean) params[2];
            final TPath path = new TPath(uri);
            final URI result = path.toUri();
            assertThat(result.getScheme(), is(scheme));
            assert !result.isOpaque();
            try {
                assertThat(path.getFileSystem().provider().getScheme(), is(scheme));
                if (!succeeds)
                    fail();
            } catch (ServiceConfigurationError ex) {
                if (succeeds)
                    throw ex;
            }
        }
    }

    @Test
    public void testResolve() {
        if ('\\' == separatorChar) {
            for (Object[] params : new Object[][] {
                // $parent, $first, $name, $address
                { "x", "c:/foo", "c:/foo", ROOT_DIRECTORY + "c:/foo" },
                { "x", "//foo/bar/baz", "//foo/bar/baz", ROOT_DIRECTORY + "/foo/bar/baz" },
                { "x", "///foo//bar//baz//", "//foo/bar/baz", ROOT_DIRECTORY + "/foo/bar/baz" },
            }) {
                assertResolve(params);
            }
        }
        for (Object[] params : new Object[][] {
            // $parent, $first, $name, $address
            { "", "/", "/", ROOT_DIRECTORY },
            { "x", "/foo", "/foo", ROOT_DIRECTORY + "foo" },
            { "x", "/foo/", "/foo", ROOT_DIRECTORY + "foo"},
            { "x", "/foo/bar", "/foo/bar", ROOT_DIRECTORY + "foo/bar"},
            { "x", "/foo//bar//", "/foo/bar", ROOT_DIRECTORY + "foo/bar"},
            { "x", "/foo/..", "/foo/..", ROOT_DIRECTORY },
            { "x", "/foo/../", "/foo/..", ROOT_DIRECTORY },
            { "x", "/foo.mok/bar", "/foo.mok/bar", "mok:" + ROOT_DIRECTORY + "foo.mok!/bar" },
            { "x", "/foo.mok/bar/", "/foo.mok/bar", "mok:" + ROOT_DIRECTORY + "foo.mok!/bar" },
            { "x", "/foo/bar.mok", "/foo/bar.mok", "mok:" + ROOT_DIRECTORY + "foo/bar.mok!/" },
            { "x", "/foo/bar.mok/", "/foo/bar.mok", "mok:" + ROOT_DIRECTORY + "foo/bar.mok!/" },
            { "x", "/foo.mok/bar.mok", "/foo.mok/bar.mok", "mok:mok:" + ROOT_DIRECTORY + "foo.mok!/bar.mok!/" },
            { "x", "/foo.mok/bar.mok/", "/foo.mok/bar.mok", "mok:mok:" + ROOT_DIRECTORY + "foo.mok!/bar.mok!/" },
            { "/", "", "/", ROOT_DIRECTORY },
            { "/", "foo", "/foo", ROOT_DIRECTORY + "foo" },
            { "/", "foo/", "/foo", ROOT_DIRECTORY + "foo" },
            { "/", ".", "/", ROOT_DIRECTORY },
            { "/", "./", "/", ROOT_DIRECTORY },
            { "", "bar", "bar", CURRENT_DIRECTORY + "bar" },
            { ".", "bar/", "bar", CURRENT_DIRECTORY + "bar" },
            { "foo", "bar", "foo/bar", CURRENT_DIRECTORY + "foo/bar" },
            { "foo", "bar/", "foo/bar", CURRENT_DIRECTORY + "foo/bar"},
            { "", "bar/..", "bar/..", CURRENT_DIRECTORY },
            { "", "bar/../", "bar/..", CURRENT_DIRECTORY },
            { "foo.mok", "bar", "foo.mok/bar", "mok:" + CURRENT_DIRECTORY + "foo.mok!/bar" },
            { "foo.mok", "bar/", "foo.mok/bar", "mok:" + CURRENT_DIRECTORY + "foo.mok!/bar" },
            { "foo", "bar.mok", "foo/bar.mok", "mok:" + CURRENT_DIRECTORY + "foo/bar.mok!/" },
            { "foo", "bar.mok/", "foo/bar.mok", "mok:" + CURRENT_DIRECTORY + "foo/bar.mok!/" },
            { "foo.mok", "bar.mok", "foo.mok/bar.mok", "mok:mok:" + CURRENT_DIRECTORY + "foo.mok!/bar.mok!/" },
            { "foo.mok", "bar.mok/", "foo.mok/bar.mok", "mok:mok:" + CURRENT_DIRECTORY + "foo.mok!/bar.mok!/" },
            { "foo.mok", "..", "", CURRENT_DIRECTORY },
            { "foo.mok", "../", "", CURRENT_DIRECTORY },
            { "foo.mok", "../bar.mok", "bar.mok", "mok:" + CURRENT_DIRECTORY + "bar.mok!/"},
            { "foo.mok", "../bar.mok/", "bar.mok", "mok:" + CURRENT_DIRECTORY + "bar.mok!/"},
        }) {
            assertResolve(params);
        }
    }

    private static void assertResolve(Object... params) {
        final TPath parent = new TPath(params[0].toString());
        final String first = params[1].toString();
        final String name = params[2].toString();
        final FsPath address = FsPath.create(URI.create(params[3].toString()));
        final TPath member = new TPath(first);
        final TPath path = parent.resolve(member);
        assertThat(path.toString(), equalTo(name.replace(SEPARATOR, path.getFileSystem().getSeparator())));
        assertThat(path.getMountPoint(), equalTo(address.getMountPoint()));
        assertThat(path.getEntryName(), equalTo(address.getEntryName()));
    }

    @Test
    public void testResolveSibling() {
        if ('\\' == separatorChar) {
            for (Object[] params : new Object[][] {
                // $parent, $first, $more, $name, $address
                { "x", "c:/foo", "c:/foo", ROOT_DIRECTORY + "c:/foo" },
                { "x", "//foo/bar/baz", "//foo/bar/baz", ROOT_DIRECTORY + "/foo/bar/baz" },
                { "x", "///foo//bar//baz//", "//foo/bar/baz", ROOT_DIRECTORY + "/foo/bar/baz" },
            }) {
                assertResolveSibling(params);
            }
        }
        for (Object[] params : new Object[][] {
            // $parent, $first, $name, $address
            { "", "/", "/", ROOT_DIRECTORY },
            { "x", "/foo", "/foo", ROOT_DIRECTORY + "foo" },
            { "x", "/foo/", "/foo", ROOT_DIRECTORY + "foo"},
            { "x", "/foo/bar", "/foo/bar", ROOT_DIRECTORY + "foo/bar"},
            { "x", "/foo//bar//", "/foo/bar", ROOT_DIRECTORY + "foo/bar"},
            { "x", "/foo/..", "/foo/..", ROOT_DIRECTORY },
            { "x", "/foo/../", "/foo/..", ROOT_DIRECTORY },
            { "x", "/foo.mok/bar", "/foo.mok/bar", "mok:" + ROOT_DIRECTORY + "foo.mok!/bar" },
            { "x", "/foo.mok/bar/", "/foo.mok/bar", "mok:" + ROOT_DIRECTORY + "foo.mok!/bar" },
            { "x", "/foo/bar.mok", "/foo/bar.mok", "mok:" + ROOT_DIRECTORY + "foo/bar.mok!/" },
            { "x", "/foo/bar.mok/", "/foo/bar.mok", "mok:" + ROOT_DIRECTORY + "foo/bar.mok!/" },
            { "x", "/foo.mok/bar.mok", "/foo.mok/bar.mok", "mok:mok:" + ROOT_DIRECTORY + "foo.mok!/bar.mok!/" },
            { "x", "/foo.mok/bar.mok/", "/foo.mok/bar.mok", "mok:mok:" + ROOT_DIRECTORY + "foo.mok!/bar.mok!/" },
            { "/", "", "", CURRENT_DIRECTORY },
            { "/", "foo", "foo", CURRENT_DIRECTORY + "foo" },
            { "/", "foo/", "foo", CURRENT_DIRECTORY + "foo" },
            { "/", ".", ".", CURRENT_DIRECTORY },
            { "/", "./", ".", CURRENT_DIRECTORY },
            { "", "bar", "bar", CURRENT_DIRECTORY + "bar" },
            { ".", "bar/", "bar", CURRENT_DIRECTORY + "bar" },
            { "foo", "bar", "bar", CURRENT_DIRECTORY + "bar" },
            { "foo", "bar/", "bar", CURRENT_DIRECTORY + "bar"},
            { "", "bar/..", "bar/..", CURRENT_DIRECTORY },
            { "", "bar/../", "bar/..", CURRENT_DIRECTORY },
            { "foo.mok", "bar", "bar", CURRENT_DIRECTORY + "bar" },
            { "foo.mok", "bar/", "bar", CURRENT_DIRECTORY + "bar" },
            { "foo", "bar.mok", "bar.mok", "mok:" + CURRENT_DIRECTORY + "bar.mok!/" },
            { "foo", "bar.mok/", "bar.mok", "mok:" + CURRENT_DIRECTORY + "bar.mok!/" },
            { "foo.mok", "bar.mok", "bar.mok", "mok:" + CURRENT_DIRECTORY + "bar.mok!/" },
            { "foo.mok", "bar.mok/", "bar.mok", "mok:" + CURRENT_DIRECTORY + "bar.mok!/" },
            { "foo.mok", ".", ".", CURRENT_DIRECTORY },
            { "foo.mok", "./", ".", CURRENT_DIRECTORY },
            { "foo.mok", "./bar.mok", "./bar.mok", "mok:" + CURRENT_DIRECTORY + "bar.mok!/"},
            { "foo.mok", "./bar.mok/", "./bar.mok", "mok:" + CURRENT_DIRECTORY + "bar.mok!/"},
        }) {
            assertResolveSibling(params);
        }
    }

    private static void assertResolveSibling(Object... params) {
        final TPath parent = new TPath(params[0].toString());
        final String first = params[1].toString();
        final String name = params[2].toString();
        final FsPath address = FsPath.create(URI.create(params[3].toString()));
        final TPath member = new TPath(first);
        final TPath path = parent.resolveSibling(member);
        assertThat(path.toString(), equalTo(name.replace(SEPARATOR, path.getFileSystem().getSeparator())));
        assertThat(path.getMountPoint(), equalTo(address.getMountPoint()));
        assertThat(path.getEntryName(), equalTo(address.getEntryName()));
    }

    @Test
    public void testGetParent() {
        if ('\\' == separatorChar) {
            for (String[] params : new String[][] {
                // $path, $parent
                { "c:/", null },
                { "c:/foo", "c:/" },
                { "c:/foo/", "c:/" },
            }) {
                assertGetParent(params);
            }
        }
        for (String[] params : new String[][] {
            // $path, $parent
            { "", null },
            { ".", null },
            { "./", null },
            { "..", null },
            { "../", null },
            { "foo", null },
            { "foo/", null },
            { "foo/.", "foo" },
            { "foo/./", "foo" },
            { "foo/..", "foo" },
            { "foo/../", "foo" },
            { "./foo", null },
            { "./foo/", null },
            { "../foo", ".." },
            { "../foo/", ".." },
            { "/", null },
            { "/foo", "/" },
            { "/foo/", "/" },
            { "/foo/..", "/foo" },
            { "/foo/../", "/foo" },
            //{ "/../foo", "/.." },
            //{ "/../foo/", "/.." },
        }) {
            assertGetParent(params);
        }
    }

    private static void assertGetParent(final String... params) {
        final Path path = new TPath(params[0]);
        final Path parent = null == params[1] ? null : new TPath(params[1]);
        assertThat(path.getParent(), is(parent));
    }

    @Test
    public void testGetRoot() {
        if ('\\' == separatorChar) {
            for (String[] params : new String[][] {
                // $test, $root
                //{ "c:", null },
                //{ "c:foo", null },
                { "c://", "c:/" },
                { "c:/", "c://" },
                { "c:/foo", "c:/" },
                { "//foo/bar/", "//foo/bar/" },
                { "//foo/bar/baz", "//foo/bar/" },
            }) {
                assertGetRoot(params);
            }
        }
        for (String[] params : new String[][] {
            // $test, $root
            { "", null },
            { "foo", null },
            { "/", "/" },
            { "/foo", "/" },
        }) {
            assertGetRoot(params);
        }
    }

    private static void assertGetRoot(String... params) {
        final String test = params[0];
        final String root = params[1];
        final TPath testPath = new TPath(test);
        final TPath rootPath = root == null ? null : new TPath(root);
        assertThat(testPath.getRoot(), is(rootPath));
    }

    @Test
    public void testGetFileName() {
        if ('\\' == separatorChar) {
            for (String[] params : new String[][] {
                // $test, $root
                //{ "c:", null },
                //{ "c:foo", "foo" },
                { "c:/", null },
                { "c:/foo", "foo" },
                { "c:/foo/bar", "bar" },
                { "//foo/bar/", null },
                { "//foo/bar/baz", "baz" },
            }) {
                assertGetFileName(params);
            }
        }
        for (String[] params : new String[][] {
            // $test, $root
            { "", null },
            { "foo", "foo" },
            { "foo/bar", "bar" },
            { "/", null },
            { "/foo", "foo" },
            { "/foo/bar", "bar" },
        }) {
            assertGetFileName(params);
        }
    }

    private static void assertGetFileName(String... params) {
        final String test = params[0];
        final String fileName = params[1];
        final TPath testPath = new TPath(test);
        final TPath fileNamePath = fileName == null ? null : new TPath(fileName);
        assertThat(testPath.getFileName(), is(fileNamePath));
    }

    @Test
    public void testCutTrailingSeparators() {
        assertThat(TPath.cutTrailingSeparators("c://", 3), is("c:/"));
        assertThat(TPath.cutTrailingSeparators("///", 2), is("//"));
        assertThat(TPath.cutTrailingSeparators("//", 1), is("/"));
    }

    @Test
    public void testElements() {
        if ('\\' == separatorChar) {
            for (Object[] params : new Object[][] {
                // $first, $more
                { "c:/foo", NO_STRINGS },
                { "c:/foo", new String[] { "bar" } },
                { "//foo/bar/boom", NO_STRINGS },
                { "//foo/bar/boom", new String[] { "bang" } },
            }) {
                assertElements(params);
            }
        }
        for (Object[] params : new Object[][] {
            // $first, $more
            { "", null },
            { "foo", NO_STRINGS },
            { "foo", new String[] { "bar" } },
            { "/", null },
            { "/foo", NO_STRINGS },
            { "/foo", new String[] { "bar" } },
        }) {
            assertElements(params);
        }
    }

    private static void assertElements(Object... params) {
        final String first = params[0].toString();
        final String[] more = (String[]) params[1];
        final TPath path;
        int count;
        if (null == more) {
            path = new TPath(first);
            count = 0;
        } else {
            path = new TPath(first, more);
            count = more.length + 1;
        }
        assertThat(path.getNameCount(), is(count));
        final Iterator<Path> it = path.iterator();
        if (0 < count--) {
            assertThat(path.getName(0).toString(), is(stripPrefix(first)));
            assertThat(it.next().toString(), is(stripPrefix(first)));
            for (int i = 0; i < count; ) {
                final String m = more[i];
                final String p = path.getName(++i).toString();
                assertThat(p, is(m));
                assertThat(it.next().toString(), is(m));
            }
        }
        assertThat(it.hasNext(), is(false));
    }

    private static String stripPrefix(final String s) {
        return s.substring(Paths.prefixLength(s, SEPARATOR_CHAR, true));
    }

    @Test
    public void testConfiguration() {
        // Create reference to the current directory.
        TPath directory = new TPath("");
        // This is how you would detect a prospective archive file.
        TPath archive = directory.resolve("archive.mok");
        TPath file;
        try (final TConfig config = TConfig.push()) {
            config.setArchiveDetector(TArchiveDetector.NULL);
            // Ignore prospective archive file here.
            file = directory.resolve("archive.mok");
        }
        // Once created, the prospective archive file detection does not change
        // because a TPath is immutable.
        assert archive.getArchiveDetector() == getArchiveDetector();
        assert archive.isArchive();
        assert file.getArchiveDetector() == TArchiveDetector.NULL;
        assert !file.isArchive();
    }
}
