/*
 * Copyright (C) 2011 Schlichtherle IT Services
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

import static de.schlichtherle.truezip.fs.FsEntryName.*;
import de.schlichtherle.truezip.fs.FsPath;
import java.io.File;
import java.net.URI;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class TPathTest extends TestBase {

    @Test
    public void testConstructorWithStrings() {
        if ('\\' == File.separatorChar) {
            for (Object[] params : new Object[][] {
                // $first, $more, $uri, $address
                //{ "c:foo", NO_MORE, "c%3Afoo", "file:/c:foo" },
                { "c:\\foo", NO_MORE, "c%3A/foo", "file:/c:/foo" },
                { "//", NO_MORE, "/", ROOT_DIRECTORY },
                { "//foo", new String[] { "bar", "baz" }, "//foo/bar/baz", ROOT_DIRECTORY + "/foo/bar/baz" },
                { "///foo//", new String[] { "//bar//", "//", "//baz//" }, "//foo/bar/baz", ROOT_DIRECTORY + "/foo/bar/baz" },
            })
                assertConstructorWithStrings(params);
        }
        for (Object[] params : new Object[][] {
            // $first, $more, $uri, $address
            { "/", NO_MORE, "/", ROOT_DIRECTORY },
            { "/foo", NO_MORE, "/foo", ROOT_DIRECTORY + "foo" },
            { "/foo", new String[] { "" }, "/foo", ROOT_DIRECTORY + "foo"},
            { "/foo", new String[] { "bar" }, "/foo/bar", ROOT_DIRECTORY + "foo/bar"},
            { "///", NO_MORE, "/", ROOT_DIRECTORY },
            { "/foo", new String[] { "/bar" }, "/foo/bar", ROOT_DIRECTORY + "foo/bar"},
            { "/foo//", new String[] { "//", "//bar//", "" }, "/foo/bar", ROOT_DIRECTORY + "foo/bar"},
            { "/foo", new String[] { "" }, "/foo", ROOT_DIRECTORY + "foo"},
            { "/foo", new String[] { ".." }, "/foo/..", ROOT_DIRECTORY },
            { "/foo", new String[] { "/bar" }, "/foo/bar", ROOT_DIRECTORY + "foo/bar" },
            { "/foo.mok", new String[] { "/bar" }, "/foo.mok/bar", "mok:" + ROOT_DIRECTORY + "foo.mok!/bar" },
            { "/foo", new String[] { "/bar.mok" }, "/foo/bar.mok", "mok:" + ROOT_DIRECTORY + "foo/bar.mok!/" },
            { "/foo.mok", new String[] { "/bar.mok" }, "/foo.mok/bar.mok", "mok:mok:" + ROOT_DIRECTORY + "foo.mok!/bar.mok!/" },
            { "", NO_MORE, "", CURRENT_DIRECTORY },
            { ".", NO_MORE, ".", CURRENT_DIRECTORY },
            { "foo", NO_MORE, "foo", CURRENT_DIRECTORY + "foo" },
            { "foo", new String[] { "" }, "foo", CURRENT_DIRECTORY + "foo"},
            { "foo", new String[] { ".." }, "foo/..", CURRENT_DIRECTORY },
            { "foo", new String[] { "bar" }, "foo/bar", CURRENT_DIRECTORY + "foo/bar" },
            { "foo.mok", new String[] { "bar" }, "foo.mok/bar", "mok:" + CURRENT_DIRECTORY + "foo.mok!/bar" },
            { "foo", new String[] { "bar.mok" }, "foo/bar.mok", "mok:" + CURRENT_DIRECTORY + "foo/bar.mok!/" },
            { "foo.mok", new String[] { "bar.mok" }, "foo.mok/bar.mok", "mok:mok:" + CURRENT_DIRECTORY + "foo.mok!/bar.mok!/" },
        })
            assertConstructorWithStrings(params);
    }

    private static void assertConstructorWithStrings(Object... params) {
        final String first = params[0].toString();
        final String[] more = (String[]) params[1];
        final URI uri = URI.create(params[2].toString());
        final FsPath address = FsPath.create(URI.create(params[3].toString()));
        final TPath path = new TPath(first, more);
        assertThat(path.getUri(), equalTo(uri));
        assertThat(path.toString(), equalTo(uri.getSchemeSpecificPart().replace(SEPARATOR, path.getFileSystem().getSeparator())));
        assertThat(path.getAddress(), equalTo(address));
    }

    @Test
    public void testResolve() {
        if ('\\' == File.separatorChar) {
            for (Object[] params : new Object[][] {
                // $parent, $first, $more, $uri, $address
                { "x", "//foo", new String[] { "bar", "baz" }, "//foo/bar/baz", ROOT_DIRECTORY + "/foo/bar/baz" },
                { "x", "///foo//", new String[] { "//bar//", "//", "//baz//" }, "//foo/bar/baz", ROOT_DIRECTORY + "/foo/bar/baz" },
            })
                assertResolve(params);
        }
        for (Object[] params : new Object[][] {
            // $parent, $first, $more, $uri, $address
            { "", "/", NO_MORE, "/", ROOT_DIRECTORY },
            { "x", "/foo", NO_MORE, "/foo", ROOT_DIRECTORY + "foo" },
            { "x", "/foo", new String[] { "" }, "/foo", ROOT_DIRECTORY + "foo"},
            { "x", "/foo", new String[] { "bar" }, "/foo/bar", ROOT_DIRECTORY + "foo/bar"},
            { "x", "//", NO_MORE, "/", ROOT_DIRECTORY },
            { "x", "///", NO_MORE, "/", ROOT_DIRECTORY },
            { "x", "/foo", new String[] { "/bar" }, "/foo/bar", ROOT_DIRECTORY + "foo/bar"},
            { "x", "/foo//", new String[] { "//", "//bar//", "" }, "/foo/bar", ROOT_DIRECTORY + "foo/bar"},
            { "x", "/foo", new String[] { "" }, "/foo", ROOT_DIRECTORY + "foo"},
            { "x", "/foo", new String[] { ".." }, "/foo/..", ROOT_DIRECTORY },
            { "x", "/foo", new String[] { "/bar" }, "/foo/bar", ROOT_DIRECTORY + "foo/bar" },
            { "x", "/foo.mok", new String[] { "/bar" }, "/foo.mok/bar", "mok:" + ROOT_DIRECTORY + "foo.mok!/bar" },
            { "x", "/foo", new String[] { "/bar.mok" }, "/foo/bar.mok", "mok:" + ROOT_DIRECTORY + "foo/bar.mok!/" },
            { "x", "/foo.mok", new String[] { "/bar.mok" }, "/foo.mok/bar.mok", "mok:mok:" + ROOT_DIRECTORY + "foo.mok!/bar.mok!/" },
            { "/", "", NO_MORE, "/", ROOT_DIRECTORY },
            { "/", "foo", NO_MORE, "/foo", ROOT_DIRECTORY + "foo" },
            { "/", ".", NO_MORE, "/", ROOT_DIRECTORY },
            { "", "bar", NO_MORE, "bar", CURRENT_DIRECTORY + "bar" },
            { ".", "bar", NO_MORE, "bar", CURRENT_DIRECTORY + "bar" },
            { "foo", "bar", NO_MORE, "foo/bar", CURRENT_DIRECTORY + "foo/bar" },
            { "foo", "bar", new String[] { "" }, "foo/bar", CURRENT_DIRECTORY + "foo/bar"},
            { "", "bar", new String[] { ".." }, "bar/..", CURRENT_DIRECTORY },
            { "foo.mok", "bar", NO_MORE, "foo.mok/bar", "mok:" + CURRENT_DIRECTORY + "foo.mok!/bar" },
            { "foo", "bar.mok", NO_MORE, "foo/bar.mok", "mok:" + CURRENT_DIRECTORY + "foo/bar.mok!/" },
            { "foo.mok", "bar.mok", NO_MORE, "foo.mok/bar.mok", "mok:mok:" + CURRENT_DIRECTORY + "foo.mok!/bar.mok!/" },
            { "foo.mok", "..", NO_MORE, "", CURRENT_DIRECTORY },
            { "foo.mok", "..", new String[] { "bar.mok" }, "bar.mok", "mok:" + CURRENT_DIRECTORY + "bar.mok!/"},
        })
            assertResolve(params);
    }

    private static void assertResolve(Object... params) {
        final TPath parent = new TPath(params[0].toString());
        final String first = params[1].toString();
        final String[] more = (String[]) params[2];
        final URI uri = URI.create(params[3].toString());
        final FsPath address = FsPath.create(URI.create(params[4].toString()));
        final TPath member = new TPath(first, more);
        final TPath path = parent.resolve(member);
        assertThat(path.getUri(), equalTo(uri));
        assertThat(path.toString(), equalTo(uri.getSchemeSpecificPart().replace(SEPARATOR, path.getFileSystem().getSeparator())));
        assertThat(path.getAddress(), equalTo(address));
    }
}
