/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.util;

import java.io.File;
import junit.framework.TestCase;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class PathUtilsTest extends TestCase {

    public PathUtilsTest(String testName) {
        super(testName);
    }

    @SuppressWarnings("RedundantStringConstructorCall")
    public void testCutTrailingSeparators() {
        String path;

        path = "";
        assertSame(path, PathUtils.cutTrailingSeparators(path, '/'));

        path = "dir";
        assertSame(path, PathUtils.cutTrailingSeparators(path, '/'));
        assertEquals("dir", PathUtils.cutTrailingSeparators("dir/", '/'));
        assertEquals("dir", PathUtils.cutTrailingSeparators("dir//", '/'));
        assertEquals("dir", PathUtils.cutTrailingSeparators("dir///", '/'));

        path = "/dir";
        assertSame(path, PathUtils.cutTrailingSeparators(path, '/'));
        assertEquals("/dir", PathUtils.cutTrailingSeparators("/dir/", '/'));
        assertEquals("/dir", PathUtils.cutTrailingSeparators("/dir//", '/'));
        assertEquals("/dir", PathUtils.cutTrailingSeparators("/dir///", '/'));

        path = new String("/");
        assertSame(path, PathUtils.cutTrailingSeparators(path, '/'));
        assertEquals("/", PathUtils.cutTrailingSeparators("//", '/'));
        assertEquals("/", PathUtils.cutTrailingSeparators("///", '/'));
        assertEquals("/", PathUtils.cutTrailingSeparators("////", '/'));
    }

    public void testSplitPathName() {
        final String fs = File.separator;

        testSplit(fs + "dir" + fs + "file" + fs);
        testSplit(fs + "dir" + fs + "file");
        testSplit(fs + "dir" + fs);
        testSplit(fs + "dir");
        testSplit(fs);

        testSplit("dir" + fs + "file" + fs);
        testSplit("dir" + fs + "file");
        testSplit("dir" + fs);
        testSplit("dir");
        testSplit("d");
        testSplit("");

        testSplit(".");
        testSplit("..");
        //testSplit(fs);

        testSplit("dir");
        testSplit("dir" + fs);
        testSplit("dir" + fs + fs);
        testSplit("dir" + fs + fs + fs);

        testSplit("dir" + fs + "file");
        testSplit("dir" + fs + "file" + fs);
        testSplit("dir" + fs + "file" + fs + fs);
        testSplit("dir" + fs + "file" + fs + fs + fs);

        testSplit("dir" + fs + fs + "file");
        testSplit("dir" + fs + fs + fs + "file");

        testSplit("dir" + fs + fs + fs + "file" + fs);
        testSplit("dir" + fs + fs + fs + "file" + fs + fs);
        testSplit("dir" + fs + fs + fs + "file" + fs + fs + fs);

        if (File.separatorChar == '\\') { // Windoze?
            testSplit("\\\\\\host");
            testSplit("\\\\\\\\host");
            testSplit("\\\\\\\\\\host");

            testSplit("\\\\host\\share\\\\file\\\\");
            testSplit("\\\\host\\share\\file\\");
            testSplit("\\\\host\\share\\file");
            testSplit("\\\\host\\share\\");
            testSplit("\\\\host\\share");
            testSplit("\\\\host\\");
            testSplit("\\\\host");
            testSplit("\\\\h");
            testSplit("\\\\");

            testSplit("C:\\dir\\\\file\\\\");
            testSplit("C:\\dir\\file\\");
            testSplit("C:\\dir\\file");
            testSplit("C:\\dir\\");
            testSplit("C:\\dir");
            testSplit("C:\\d");
            testSplit("C:\\");

            testSplit("C:dir\\\\file\\\\");
            testSplit("C:dir\\file\\");
            testSplit("C:dir\\file");
            testSplit("C:dir\\");
            testSplit("C:dir");
            testSplit("C:d");
            testSplit("C:");
        }
    }

    /**
     * Test of split method, of class de.schlichtherle.truezip.io.util.PathUtils.
     */
    public void testSplit(final String path) {
        final java.io.File file = new java.io.File(path);
        final String parent = file.getParent();
        final String base = file.getName();

        final String[] split = PathUtils.split(path, File.separatorChar);
        assertEquals(2, split.length);
        assertEquals(parent, split[0]);
        assertEquals(base, split[1]);
    }

    public void testNormalize() {
        testNormalize(".", "");
        testNormalize("a/b/c/d", "a/b/c/d");

        testNormalize("a", "./a");

        testNormalize(".",        ".");
        testNormalize("..",       "./..");
        testNormalize("../..",    "./../..");
        testNormalize("../../..", "./../../..");
        testNormalize("../../..", "./.././.././..");
        testNormalize("../../..", "././../././../././..");
        testNormalize("../../..", "./././.././././.././././..");

        testNormalize("..",          "..");
        testNormalize("../..",       "../..");
        testNormalize("../../..",    "../../..");
        testNormalize("../../../..", "../../../..");
        testNormalize("../../../..", "../.././.././..");
        testNormalize("../../../..", ".././../././../././..");
        testNormalize("../../../..", "../././.././././.././././..");

        testNormalize("a",     "a");
        testNormalize(".",     "a/..");
        testNormalize("..",    "a/../..");
        testNormalize("../..", "a/../../..");
        testNormalize("../..", "a/./.././.././..");
        testNormalize("../..", "a/././../././../././..");
        testNormalize("../..", "a/./././.././././.././././..");

        testNormalize("a/b", "a/b");
        testNormalize("a",   "a/b/..");
        testNormalize(".",   "a/b/../..");
        testNormalize("..",  "a/b/../../..");
        testNormalize("..",  "a/b/./.././.././..");
        testNormalize("..",  "a/b/././../././../././..");
        testNormalize("..",  "a/b/./././.././././.././././..");

        testNormalize("a/b/c", "a/b/c");
        testNormalize("a/b",   "a/b/c/..");
        testNormalize("a",     "a/b/c/../..");
        testNormalize(".",     "a/b/c/../../..");
        testNormalize(".",     "a/b/c/./.././.././..");
        testNormalize(".",     "a/b/c/././../././../././..");
        testNormalize(".",     "a/b/c/./././.././././.././././..");

        testNormalize("a/b/c/d", "a/b/c/d");
        testNormalize("a/b/c",   "a/b/c/d/..");
        testNormalize("a/b",     "a/b/c/d/../..");
        testNormalize("a",       "a/b/c/d/../../..");
        testNormalize("a",       "a/b/c/d/./.././.././..");
        testNormalize("a",       "a/b/c/d/././../././../././..");
        testNormalize("a",       "a/b/c/d/./././.././././.././././..");

        testNormalize("a/b/c/d", "a//b//c//d");
        testNormalize("a/b/c/d", "a///b///c///d");
        testNormalize("a/b/c/d", "a////b////c////d");
        testNormalize("a/b/c",   "a////b////c////d////..");
        testNormalize("a/b",     "a////b////c////d////..////..");
        testNormalize("a/b",     "a//.//b/.///c///./d//.//.././//..");
        testNormalize("a/b",     "a/////b/////c/////d/////../////..");

        testNormalize("a",       "x/../a");
        testNormalize("a/b",     "x/../a/y/../b");
        testNormalize("a/b/c",   "x/../a/y/../b/z/../c");

        testNormalize("../a",       "x/../../a");
        testNormalize("../a/b",     "x/../../a/y/../b");
        testNormalize("../a/b/c",   "x/../../a/y/../b/z/../c");

        testNormalize("../a",       "x/.././../a");
        testNormalize("../a/b",     "x/.././../a/y/../b");
        testNormalize("../a/b/c",   "x/.././../a/y/../b/z/../c");

        testNormalize("../a",       "x/..//../a");
        testNormalize("../a/b",     "x/..//../a/y/../b");
        testNormalize("../a/b/c",   "x/..//../a/y/../b/z/../c");

        testNormalize("../../a",       "x/../../../a");
        testNormalize("../../a/b",     "x/../../../a/y/../b");
        testNormalize("../../a/b/c",   "x/../../../a/y/../b/z/../c");

        testNormalize("../../a",       "x/.././.././../a");
        testNormalize("../../a/b",     "x/.././.././../a/y/../b");
        testNormalize("../../a/b/c",   "x/.././.././../a/y/../b/z/../c");

        testNormalize("../../a",       "x/..//..//../a");
        testNormalize("../../a/b",     "x/..//..//../a/y/../b");
        testNormalize("../../a/b/c",   "x/..//..//../a/y/../b/z/../c");

        testNormalize("a",       "x/x/../../a");
        testNormalize("a/b",     "x/x/../../a/y/y/../../b");
        testNormalize("a/b/c",   "x/x/../../a/y/y/../../b/z/z/../../c");

        testNormalize("/", "/");
        //testNormalize("/", "//");
        testNormalize("/", "/.");
        testNormalize("/", "/./");

        testNormalize("/..", "/..");
        testNormalize("/..", "/../.");
        testNormalize("/../..", "/.././..");
        testNormalize("/../..", "/.././../.");

        testNormalize("\\\\host", "\\\\host", '\\');
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
        testNormalize("C:dir", "C:.\\dir\\.\\.", '\\');

        testNormalize(".", ".");
        testNormalize("./", "./");
        testNormalize("..", "..");
        testNormalize("../", "../");
        testNormalize("a", "./a");
        testNormalize("a/", "./a/");
        testNormalize("../a", "../a");
        testNormalize("../a/", "../a/");
        testNormalize("a/b", "./a/./b");
        testNormalize("a/b/", "./a/./b/");
        testNormalize("../a/b", "../a/./b");
        testNormalize("../a/b/", "../a/./b/");
        testNormalize("b", "./a/../b");
        testNormalize("b/", "./a/../b/");
        testNormalize("../b", "../a/../b");
        testNormalize("../b/", "../a/../b/");

        testNormalize("./", ".//");
        testNormalize("./", ".///");
        testNormalize("./", ".////");
        testNormalize("../", "..//");
        testNormalize("a/", ".//a//");
        testNormalize("../a", "..//a");
        testNormalize("../a/", "..//a//");
        testNormalize("a/b", ".//a//.//b");
        testNormalize("a/b/", ".//a//.//b//");
        testNormalize("../a/b", "..//a//.//b");
        testNormalize("../a/b/", "..//a//.//b//");
        testNormalize("b/", ".//a//..//b//");
        testNormalize("../b", "..//a//..//b");
        testNormalize("../b/", "..//a//..//b//");
    }

    private void testNormalize(String result, final String path) {
        testNormalize(result, path, '/');
    }

    private void testNormalize(String expected, final String path, final char separatorChar) {
        final String result = PathUtils.normalize(path, separatorChar);
        assertEquals(expected, result);
        assertTrue(!result.equals(path) || result == path);
        if (path.length() > 0 && !path.endsWith(separatorChar + "")) {
            String appended = path;
            for (int i = 0; i < 3; i++) {
                appended += separatorChar + ".";
                assertEquals(expected, PathUtils.normalize(appended, separatorChar));
            }
        }
    }
}
