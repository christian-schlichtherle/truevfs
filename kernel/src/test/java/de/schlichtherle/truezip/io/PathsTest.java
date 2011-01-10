/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io;

import java.net.URI;
import java.io.File;
import org.junit.Test;

import static de.schlichtherle.truezip.io.Paths.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class PathsTest {

    @SuppressWarnings("RedundantStringConstructorCall")
    @Test
    public void testCutTrailingSeparators() {
        String path;

        path = "";
        assertSame(path, cutTrailingSeparators(path, '/'));

        path = "dir";
        assertSame(path, cutTrailingSeparators(path, '/'));
        assertEquals("dir", cutTrailingSeparators("dir/", '/'));
        assertEquals("dir", cutTrailingSeparators("dir//", '/'));
        assertEquals("dir", cutTrailingSeparators("dir///", '/'));

        path = "/dir";
        assertSame(path, cutTrailingSeparators(path, '/'));
        assertEquals("/dir", cutTrailingSeparators("/dir/", '/'));
        assertEquals("/dir", cutTrailingSeparators("/dir//", '/'));
        assertEquals("/dir", cutTrailingSeparators("/dir///", '/'));

        path = new String("/"); // need new object!
        assertSame(path, cutTrailingSeparators(path, '/'));
        assertEquals("/", cutTrailingSeparators("//", '/'));
        assertEquals("/", cutTrailingSeparators("///", '/'));
        assertEquals("/", cutTrailingSeparators("////", '/'));
    }

    @Test
    public void testSplitPathName() {
        final String fs = File.separator;

        assertSplit(fs + "dir" + fs + "file" + fs);
        assertSplit(fs + "dir" + fs + "file");
        assertSplit(fs + "dir" + fs);
        assertSplit(fs + "dir");
        assertSplit(fs);

        assertSplit("dir" + fs + "file" + fs);
        assertSplit("dir" + fs + "file");
        assertSplit("dir" + fs);
        assertSplit("dir");
        assertSplit("d");
        assertSplit("");

        assertSplit(".");
        assertSplit("..");
        //testSplit(fs);

        assertSplit("dir");
        assertSplit("dir" + fs);
        assertSplit("dir" + fs + fs);
        assertSplit("dir" + fs + fs + fs);

        assertSplit("dir" + fs + "file");
        assertSplit("dir" + fs + "file" + fs);
        assertSplit("dir" + fs + "file" + fs + fs);
        assertSplit("dir" + fs + "file" + fs + fs + fs);

        assertSplit("dir" + fs + fs + "file");
        assertSplit("dir" + fs + fs + fs + "file");

        assertSplit("dir" + fs + fs + fs + "file" + fs);
        assertSplit("dir" + fs + fs + fs + "file" + fs + fs);
        assertSplit("dir" + fs + fs + fs + "file" + fs + fs + fs);

        if (File.separatorChar == '\\') { // Windoze?
            assertSplit("\\\\\\host");
            assertSplit("\\\\\\\\host");
            assertSplit("\\\\\\\\\\host");

            assertSplit("\\\\host\\share\\\\file\\\\");
            assertSplit("\\\\host\\share\\file\\");
            assertSplit("\\\\host\\share\\file");
            assertSplit("\\\\host\\share\\");
            assertSplit("\\\\host\\share");
            assertSplit("\\\\host\\");
            assertSplit("\\\\host");
            assertSplit("\\\\h");
            assertSplit("\\\\");

            assertSplit("C:\\dir\\\\file\\\\");
            assertSplit("C:\\dir\\file\\");
            assertSplit("C:\\dir\\file");
            assertSplit("C:\\dir\\");
            assertSplit("C:\\dir");
            assertSplit("C:\\d");
            assertSplit("C:\\");

            assertSplit("C:dir\\\\file\\\\");
            assertSplit("C:dir\\file\\");
            assertSplit("C:dir\\file");
            assertSplit("C:dir\\");
            assertSplit("C:dir");
            assertSplit("C:d");
            assertSplit("C:");
        }
    }

    private void assertSplit(final String path) {
        final java.io.File file = new java.io.File(path);
        final String parent = file.getParent();
        final String base = file.getName();

        final Splitter splitter = Paths.split(path, File.separatorChar);
        assertEquals(parent, splitter.getParentPath());
        assertEquals(base, splitter.getMemberName());
    }

    @Test
    public void testNormalize() {
        assertNormalize("", "");
        assertNormalize("a/b/c/d", "a/b/c/d");

        assertNormalize("a", "./a");

        assertNormalize("",        ".");
        assertNormalize("..",       "./..");
        assertNormalize("../..",    "./../..");
        assertNormalize("../../..", "./../../..");
        assertNormalize("../../..", "./.././.././..");
        assertNormalize("../../..", "././../././../././..");
        assertNormalize("../../..", "./././.././././.././././..");

        assertNormalize("..",          "..");
        assertNormalize("../..",       "../..");
        assertNormalize("../../..",    "../../..");
        assertNormalize("../../../..", "../../../..");
        assertNormalize("../../../..", "../.././.././..");
        assertNormalize("../../../..", ".././../././../././..");
        assertNormalize("../../../..", "../././.././././.././././..");

        assertNormalize("a",     "a");
        assertNormalize("",     "a/..");
        assertNormalize("..",    "a/../..");
        assertNormalize("../..", "a/../../..");
        assertNormalize("../..", "a/./.././.././..");
        assertNormalize("../..", "a/././../././../././..");
        assertNormalize("../..", "a/./././.././././.././././..");

        assertNormalize("a/b", "a/b");
        assertNormalize("a/",   "a/b/..");
        assertNormalize("",   "a/b/../..");
        assertNormalize("..",  "a/b/../../..");
        assertNormalize("..",  "a/b/./.././.././..");
        assertNormalize("..",  "a/b/././../././../././..");
        assertNormalize("..",  "a/b/./././.././././.././././..");

        assertNormalize("a/b/c", "a/b/c");
        assertNormalize("a/b/",   "a/b/c/..");
        assertNormalize("a/",     "a/b/c/../..");
        assertNormalize("",     "a/b/c/../../..");
        assertNormalize("",     "a/b/c/./.././.././..");
        assertNormalize("",     "a/b/c/././../././../././..");
        assertNormalize("",     "a/b/c/./././.././././.././././..");

        assertNormalize("a/b/c/d", "a/b/c/d");
        assertNormalize("a/b/c/",   "a/b/c/d/..");
        assertNormalize("a/b/",     "a/b/c/d/../..");
        assertNormalize("a/",       "a/b/c/d/../../..");
        assertNormalize("a/",       "a/b/c/d/./.././.././..");
        assertNormalize("a/",       "a/b/c/d/././../././../././..");
        assertNormalize("a/",       "a/b/c/d/./././.././././.././././..");

        assertNormalize("a/b/c/d", "a//b//c//d");
        assertNormalize("a/b/c/d", "a///b///c///d");
        assertNormalize("a/b/c/d", "a////b////c////d");
        assertNormalize("a/b/c/",   "a////b////c////d////..");
        assertNormalize("a/b/",     "a////b////c////d////..////..");
        assertNormalize("a/b/",     "a//.//b/.///c///./d//.//.././//..");
        assertNormalize("a/b/",     "a/////b/////c/////d/////../////..");

        assertNormalize("a",       "x/../a");
        assertNormalize("a/b",     "x/../a/y/../b");
        assertNormalize("a/b/c",   "x/../a/y/../b/z/../c");

        assertNormalize("../a",       "x/../../a");
        assertNormalize("../a/b",     "x/../../a/y/../b");
        assertNormalize("../a/b/c",   "x/../../a/y/../b/z/../c");

        assertNormalize("../a",       "x/.././../a");
        assertNormalize("../a/b",     "x/.././../a/y/../b");
        assertNormalize("../a/b/c",   "x/.././../a/y/../b/z/../c");

        assertNormalize("../a",       "x/..//../a");
        assertNormalize("../a/b",     "x/..//../a/y/../b");
        assertNormalize("../a/b/c",   "x/..//../a/y/../b/z/../c");

        assertNormalize("../../a",       "x/../../../a");
        assertNormalize("../../a/b",     "x/../../../a/y/../b");
        assertNormalize("../../a/b/c",   "x/../../../a/y/../b/z/../c");

        assertNormalize("../../a",       "x/.././.././../a");
        assertNormalize("../../a/b",     "x/.././.././../a/y/../b");
        assertNormalize("../../a/b/c",   "x/.././.././../a/y/../b/z/../c");

        assertNormalize("../../a",       "x/..//..//../a");
        assertNormalize("../../a/b",     "x/..//..//../a/y/../b");
        assertNormalize("../../a/b/c",   "x/..//..//../a/y/../b/z/../c");

        assertNormalize("a",       "x/x/../../a");
        assertNormalize("a/b",     "x/x/../../a/y/y/../../b");
        assertNormalize("a/b/c",   "x/x/../../a/y/y/../../b/z/z/../../c");

        assertNormalize("/", "/");
        //testNormalize("/", "//");
        assertNormalize("/", "/.");
        assertNormalize("/", "/./");

        assertNormalize("/..", "/..");
        assertNormalize("/../", "/../.");
        assertNormalize("/../..", "/.././..");
        assertNormalize("/../../", "/.././../.");

        /*testNormalize("\\\\host", "\\\\host", '\\');
        testNormalize("\\\\host", "\\\\\\host", '\\');
        testNormalize("\\\\host", "\\\\\\\\host", '\\');

        testNormalize("C:\\dir", "C:\\dir", '\\');
        testNormalize("C:\\dir", "C:\\\\dir", '\\');
        testNormalize("C:\\dir", "C:\\\\\\dir", '\\');

        testNormalize("C:dir", "C:dir", '\\');
        testNormalize("C:dir", "C:dir\\.", '\\');
        testNormalize("C:dir", "C:dir\\.\\.", '\\');

        testNormalize("C:dir", "C:.\\dir", '\\');
        testNormalize("C:dir", "C:.\\dir\\.", '\\');
        testNormalize("C:dir", "C:.\\dir\\.\\.", '\\');*/

        assertNormalize("", ".");
        assertNormalize("", "./");
        assertNormalize("..", "..");
        assertNormalize("../", "../");
        assertNormalize("a", "./a");
        assertNormalize("a/", "./a/");
        assertNormalize("../a", "../a");
        assertNormalize("../a/", "../a/");
        assertNormalize("a/b", "./a/./b");
        assertNormalize("a/b/", "./a/./b/");
        assertNormalize("../a/b", "../a/./b");
        assertNormalize("../a/b/", "../a/./b/");
        assertNormalize("b", "./a/../b");
        assertNormalize("b/", "./a/../b/");
        assertNormalize("../b", "../a/../b");
        assertNormalize("../b/", "../a/../b/");

        assertNormalize("", ".//");
        assertNormalize("", ".///");
        assertNormalize("", ".////");
        assertNormalize("../", "..//");
        assertNormalize("a/", ".//a//");
        assertNormalize("../a", "..//a");
        assertNormalize("../a/", "..//a//");
        assertNormalize("a/b", ".//a//.//b");
        assertNormalize("a/b/", ".//a//.//b//");
        assertNormalize("../a/b", "..//a//.//b");
        assertNormalize("../a/b/", "..//a//.//b//");
        assertNormalize("b/", ".//a//..//b//");
        assertNormalize("../b", "..//a//..//b");
        assertNormalize("../b/", "..//a//..//b//");
    }

    private void assertNormalize(String result, final String path) {
        assertNormalize(result, path, '/');
    }

    private void assertNormalize(
            final String expected,
            final String path,
            final char separatorChar) {
        assertEquals(URI
                .create(path.replace(separatorChar, '/'))
                .normalize()
                .toString()
                .replace('/', separatorChar), expected);
        final String result = Paths.normalize(path, separatorChar);
        assertEquals(expected, result);
        assertTrue(!result.equals(path) || result == path); // mind contract!
    }
}
