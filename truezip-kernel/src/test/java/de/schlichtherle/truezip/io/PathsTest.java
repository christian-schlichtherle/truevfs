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

        path = "d";
        assertSame(path, cutTrailingSeparators(path, '/'));
        assertEquals("d", cutTrailingSeparators("d/", '/'));
        assertEquals("d", cutTrailingSeparators("d//", '/'));
        assertEquals("d", cutTrailingSeparators("d///", '/'));

        path = "/d";
        assertSame(path, cutTrailingSeparators(path, '/'));
        assertEquals("/d", cutTrailingSeparators("/d/", '/'));
        assertEquals("/d", cutTrailingSeparators("/d//", '/'));
        assertEquals("/d", cutTrailingSeparators("/d///", '/'));

        path = new String("/"); // need new object!
        assertSame(path, cutTrailingSeparators(path, '/'));
        assertEquals("/", cutTrailingSeparators("//", '/'));
        assertEquals("/", cutTrailingSeparators("///", '/'));
        assertEquals("/", cutTrailingSeparators("////", '/'));
    }

    @Test
    public void testSplitPathName() {
        final String fs = File.separator;

        assertSplit(fs + "d" + fs + "f" + fs);
        assertSplit(fs + "d" + fs + "f");
        assertSplit(fs + "d" + fs);
        assertSplit(fs + "d");
        assertSplit(fs);

        assertSplit("d" + fs + "f" + fs);
        assertSplit("d" + fs + "f");
        assertSplit("d" + fs);
        assertSplit("d");
        assertSplit("d");
        assertSplit("");

        assertSplit(".");
        assertSplit("..");

        assertSplit("d");
        assertSplit("d" + fs);
        assertSplit("d" + fs + fs);
        assertSplit("d" + fs + fs + fs);

        assertSplit("d" + fs + "f");
        assertSplit("d" + fs + "f" + fs);
        assertSplit("d" + fs + "f" + fs + fs);
        assertSplit("d" + fs + "f" + fs + fs + fs);

        assertSplit("d" + fs + fs + "f");
        assertSplit("d" + fs + fs + fs + "f");

        assertSplit("d" + fs + fs + fs + "f" + fs);
        assertSplit("d" + fs + fs + fs + "f" + fs + fs);
        assertSplit("d" + fs + fs + fs + "f" + fs + fs + fs);

        if ('\\' == File.separatorChar) { // Windoze?
            //assertSplit("\\\\\\host");
            //assertSplit("\\\\\\\\host");
            //assertSplit("\\\\\\\\\\host");

            assertSplit("\\\\host\\share\\\\path\\\\");
            assertSplit("\\\\host\\share\\path\\");
            assertSplit("\\\\host\\share\\path");
            assertSplit("\\\\host\\share\\");
            assertSplit("\\\\host\\share");
            assertSplit("\\\\host\\");
            assertSplit("\\\\host");
            assertSplit("\\\\host");
            assertSplit("\\\\");

            /*assertSplit("\\\\\\host\\share\\\\path\\\\");
            assertSplit("\\\\\\host\\share\\path\\");
            assertSplit("\\\\\\host\\share\\path");
            assertSplit("\\\\\\host\\share\\");
            assertSplit("\\\\\\host\\share");
            assertSplit("\\\\\\host\\");
            assertSplit("\\\\\\host");
            assertSplit("\\\\\\host");*/
            assertSplit("\\\\\\");

            assertSplit("C:\\d\\\\f\\\\");
            assertSplit("C:\\d\\f\\");
            assertSplit("C:\\d\\f");
            assertSplit("C:\\d\\");
            assertSplit("C:\\d");
            assertSplit("C:\\d");
            assertSplit("C:\\");

            /*assertSplit("C:\\\\d\\\\f\\\\");
            assertSplit("C:\\\\d\\f\\");
            assertSplit("C:\\\\d\\f");
            assertSplit("C:\\\\d\\");
            assertSplit("C:\\\\d");
            assertSplit("C:\\\\d");*/
            assertSplit("C:\\\\");

            assertSplit("C:d\\\\f\\\\");
            assertSplit("C:d\\f\\");
            assertSplit("C:d\\f");
            assertSplit("C:d\\");
            assertSplit("C:d");
            assertSplit("C:d");
            assertSplit("C:");
        }
    }

    private void assertSplit(final String path) {
        final File file = new File(path);
        final String parent = file.getParent();
        final String member = file.getName();

        final Splitter splitter = Paths.split(path, File.separatorChar, false);
        assertEquals(parent, splitter.getParentPath());
        assertEquals(member, splitter.getMemberName());
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

        /*testNormalize("\\\\h", "\\\\h", '\\');
        testNormalize("\\\\h", "\\\\\\h", '\\');
        testNormalize("\\\\h", "\\\\\\\\h", '\\');

        testNormalize("C:\\d", "C:\\d", '\\');
        testNormalize("C:\\d", "C:\\\\d", '\\');
        testNormalize("C:\\d", "C:\\\\\\d", '\\');

        testNormalize("C:d", "C:d", '\\');
        testNormalize("C:d", "C:d\\.", '\\');
        testNormalize("C:d", "C:d\\.\\.", '\\');

        testNormalize("C:d", "C:.\\d", '\\');
        testNormalize("C:d", "C:.\\d\\.", '\\');
        testNormalize("C:d", "C:.\\d\\.\\.", '\\');*/

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

    private void assertNormalize(String expected, final String path) {
        assertNormalize(expected, path, '/');
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
