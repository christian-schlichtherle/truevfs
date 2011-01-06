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
package de.schlichtherle.truezip.io;

import de.schlichtherle.truezip.io.Paths.Splitter;
import java.net.URI;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;

import static de.schlichtherle.truezip.io.Files.cutTrailingSeparators;
import static de.schlichtherle.truezip.io.Files.isCreatableOrWritable;
import static de.schlichtherle.truezip.io.Files.normalize;
import static de.schlichtherle.truezip.io.Files.split;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FilesTest extends TestCase {

    private static final Logger logger = Logger.getLogger(
            FilesTest.class.getName());

    public FilesTest(String testName) {
        super(testName);
    }

    @SuppressWarnings("RedundantStringConstructorCall")
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
     * Test of split method, of class de.schlichtherle.truezip.io.util.Paths.
     */
    public void testSplit(final String path) {
        final java.io.File file = new java.io.File(path);
        final String parent = file.getParent();
        final String base = file.getName();

        final Splitter splitter = split(path, File.separatorChar);
        assertEquals(parent, splitter.getParentPath());
        assertEquals(base, splitter.getMemberName());
    }

    public void testNormalize() {
        testNormalize("", "");
        testNormalize("a/b/c/d", "a/b/c/d");

        testNormalize("a", "./a");

        testNormalize("",        ".");
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
        testNormalize("",     "a/..");
        testNormalize("..",    "a/../..");
        testNormalize("../..", "a/../../..");
        testNormalize("../..", "a/./.././.././..");
        testNormalize("../..", "a/././../././../././..");
        testNormalize("../..", "a/./././.././././.././././..");

        testNormalize("a/b", "a/b");
        testNormalize("a/",   "a/b/..");
        testNormalize("",   "a/b/../..");
        testNormalize("..",  "a/b/../../..");
        testNormalize("..",  "a/b/./.././.././..");
        testNormalize("..",  "a/b/././../././../././..");
        testNormalize("..",  "a/b/./././.././././.././././..");

        testNormalize("a/b/c", "a/b/c");
        testNormalize("a/b/",   "a/b/c/..");
        testNormalize("a/",     "a/b/c/../..");
        testNormalize("",     "a/b/c/../../..");
        testNormalize("",     "a/b/c/./.././.././..");
        testNormalize("",     "a/b/c/././../././../././..");
        testNormalize("",     "a/b/c/./././.././././.././././..");

        testNormalize("a/b/c/d", "a/b/c/d");
        testNormalize("a/b/c/",   "a/b/c/d/..");
        testNormalize("a/b/",     "a/b/c/d/../..");
        testNormalize("a/",       "a/b/c/d/../../..");
        testNormalize("a/",       "a/b/c/d/./.././.././..");
        testNormalize("a/",       "a/b/c/d/././../././../././..");
        testNormalize("a/",       "a/b/c/d/./././.././././.././././..");

        testNormalize("a/b/c/d", "a//b//c//d");
        testNormalize("a/b/c/d", "a///b///c///d");
        testNormalize("a/b/c/d", "a////b////c////d");
        testNormalize("a/b/c/",   "a////b////c////d////..");
        testNormalize("a/b/",     "a////b////c////d////..////..");
        testNormalize("a/b/",     "a//.//b/.///c///./d//.//.././//..");
        testNormalize("a/b/",     "a/////b/////c/////d/////../////..");

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
        testNormalize("/../", "/../.");
        testNormalize("/../..", "/.././..");
        testNormalize("/../../", "/.././../.");

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

        testNormalize("", ".");
        testNormalize("", "./");
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

        testNormalize("", ".//");
        testNormalize("", ".///");
        testNormalize("", ".////");
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

    private void testNormalize(
            final String expected,
            final String path,
            final char separatorChar) {
        assertEquals(URI
                .create(path.replace(separatorChar, '/'))
                .normalize()
                .toString()
                .replace('/', separatorChar), expected);
        final String result = normalize(path, separatorChar);
        assertEquals(expected, result);
        assertTrue(!result.equals(path) || result == path); // mind contract!
    }

    public void testNormalize2() {
        testNormalize2("",         "");
        testNormalize2("",         ".");
        testNormalize2("..",       "./..");
        testNormalize2("../..",    "./../..");
        testNormalize2("../../..", "./../../..");
        testNormalize2("../../..", "./.././.././..");
        testNormalize2("../../..", "././../././../././..");
        testNormalize2("../../..", "./././.././././.././././..");

        testNormalize2("..",          "..");
        testNormalize2("../..",       "../..");
        testNormalize2("../../..",    "../../..");
        testNormalize2("../../../..", "../../../..");
        testNormalize2("../../../..", "../.././.././..");
        testNormalize2("../../../..", ".././../././../././..");
        testNormalize2("../../../..", "../././.././././.././././..");

        testNormalize2("a",     "a");
        testNormalize2("",      "a/..");
        testNormalize2("..",    "a/../..");
        testNormalize2("../..", "a/../../..");
        testNormalize2("../..", "a/./.././.././..");
        testNormalize2("../..", "a/././../././../././..");
        testNormalize2("../..", "a/./././.././././.././././..");

        testNormalize2("a/b", "a/b");
        testNormalize2("a",   "a/b/..");
        testNormalize2("",    "a/b/../..");
        testNormalize2("..",  "a/b/../../..");
        testNormalize2("..",  "a/b/./.././.././..");
        testNormalize2("..",  "a/b/././../././../././..");
        testNormalize2("..",  "a/b/./././.././././.././././..");

        testNormalize2("a/b/c", "a/b/c");
        testNormalize2("a/b",   "a/b/c/..");
        testNormalize2("a",     "a/b/c/../..");
        testNormalize2("",      "a/b/c/../../..");
        testNormalize2("",      "a/b/c/./.././.././..");
        testNormalize2("",      "a/b/c/././../././../././..");
        testNormalize2("",      "a/b/c/./././.././././.././././..");

        testNormalize2("a/b/c/d", "a/b/c/d");
        testNormalize2("a/b/c",   "a/b/c/d/..");
        testNormalize2("a/b",     "a/b/c/d/../..");
        testNormalize2("a",       "a/b/c/d/../../..");
        testNormalize2("a",       "a/b/c/d/./.././.././..");
        testNormalize2("a",       "a/b/c/d/././../././../././..");
        testNormalize2("a",       "a/b/c/d/./././.././././.././././..");

        testNormalize2("a/b/c/d", "a//b//c//d");
        testNormalize2("a/b/c/d", "a///b///c///d");
        testNormalize2("a/b/c/d", "a////b////c////d");
        testNormalize2("a/b/c",   "a////b////c////d////..");
        testNormalize2("a/b",     "a////b////c////d////..////..");
        testNormalize2("a/b",     "a//.//b/.///c///./d//.//.././//..");
        testNormalize2("a/b",     "a/////b/////c/////d/////../////..");

        testNormalize2("a",       "x/../a");
        testNormalize2("a/b",     "x/../a/y/../b");
        testNormalize2("a/b/c",   "x/../a/y/../b/z/../c");

        testNormalize2("../a",     "x/../../a");
        testNormalize2("../a/b",   "x/../../a/y/../b");
        testNormalize2("../a/b/c", "x/../../a/y/../b/z/../c");

        testNormalize2("../a",     "x/.././../a");
        testNormalize2("../a/b",   "x/.././../a/y/../b");
        testNormalize2("../a/b/c", "x/.././../a/y/../b/z/../c");

        testNormalize2("../a",     "x/..//../a");
        testNormalize2("../a/b",   "x/..//../a/y/../b");
        testNormalize2("../a/b/c", "x/..//../a/y/../b/z/../c");

        testNormalize2("../../a",     "x/../../../a");
        testNormalize2("../../a/b",   "x/../../../a/y/../b");
        testNormalize2("../../a/b/c", "x/../../../a/y/../b/z/../c");

        testNormalize2("../../a",     "x/.././.././../a");
        testNormalize2("../../a/b",   "x/.././.././../a/y/../b");
        testNormalize2("../../a/b/c", "x/.././.././../a/y/../b/z/../c");

        testNormalize2("../../a",     "x/..//..//../a");
        testNormalize2("../../a/b",   "x/..//..//../a/y/../b");
        testNormalize2("../../a/b/c", "x/..//..//../a/y/../b/z/../c");

        testNormalize2("a",     "x/x/../../a");
        testNormalize2("a/b",   "x/x/../../a/y/y/../../b");
        testNormalize2("a/b/c", "x/x/../../a/y/y/../../b/z/z/../../c");

        //testNormalize2("/", "/");
        //testNormalize2("/", "//");
        testNormalize2("/", "/.");
        testNormalize2("/", "/./");

        testNormalize2("/..", "/..");
        testNormalize2("/..", "/../.");
        testNormalize2("/../..", "/.././..");
        testNormalize2("/../..", "/.././../.");

        testNormalize2("", ".");
        testNormalize2("", "./");
        testNormalize2("..", "..");
        testNormalize2("..", "../");
        testNormalize2("a", "./a");
        testNormalize2("a", "./a/");
        testNormalize2("../a", "../a");
        testNormalize2("../a", "../a/");
        testNormalize2("a/b", "./a/./b");
        testNormalize2("a/b", "./a/./b/");
        testNormalize2("../a/b", "../a/./b");
        testNormalize2("../a/b", "../a/./b/");
        testNormalize2("b", "./a/../b");
        testNormalize2("b", "./a/../b/");
        testNormalize2("../b", "../a/../b");
        testNormalize2("../b", "../a/../b/");

        testNormalize2("", ".//");
        testNormalize2("", ".///");
        testNormalize2("", ".////");
        testNormalize2("..", "..//");
        testNormalize2("a", ".//a//");
        testNormalize2("../a", "..//a");
        testNormalize2("../a", "..//a//");
        testNormalize2("a/b", ".//a//.//b");
        testNormalize2("a/b", ".//a//.//b//");
        testNormalize2("../a/b", "..//a//.//b");
        testNormalize2("../a/b", "..//a//.//b//");
        testNormalize2("b", ".//a//..//b//");
        testNormalize2("../b", "..//a//..//b");
        testNormalize2("../b", "..//a//..//b//");
    }

    void testNormalize2(String result, final String path) {
        result = result.replace('/', File.separatorChar);
        java.io.File file = new java.io.File(path);
        assertEquals(result, normalize(file).getPath());
        if (path.length() <= 0)
            return;
        file = new java.io.File(path + '/');
        assertEquals(result, normalize(file).getPath());
        file = new java.io.File(path + File.separator);
        assertEquals(result, normalize(file).getPath());
        file = new java.io.File(path + File.separator + ".");
        assertEquals(result, normalize(file).getPath());
        file = new java.io.File(path + File.separator + "." + File.separator + ".");
        assertEquals(result, normalize(file).getPath());
        file = new java.io.File(path + File.separator + "." + File.separator + "." + File.separator + ".");
        assertEquals(result, normalize(file).getPath());
    }

    /**
     * Test of isCreatableOrWritable method, of class de.schlichtherle.truezip.io.ArchiveController.
     * <p>
     * Note that this test is not quite correctly designed: It tests the
     * operating system rather than the method.
     */
    public void testIsWritableOrCreatable() throws IOException {
        final java.io.File file = File.createTempFile("tzp-test", null);

        boolean result = isCreatableOrWritable(file);
        assertTrue(result);

        boolean total = true;
        final FileInputStream fin = new FileInputStream(file);
        try {
            result = isCreatableOrWritable(file);
            total &= result;
        } finally {
            fin.close();
        }
        if (!result)
            logger.finer("Overwriting a file which has an open FileInputStream is not tolerated!");

        final String[] modes = { "r", "rw", "rws", "rwd" };
        for (int i = 0, l = modes.length; i < l; i++) {
            final String mode = modes[i];
            final RandomAccessFile raf = new RandomAccessFile(file, mode);
            try {
                result = isCreatableOrWritable(file);
                total &= result;
            } finally {
                raf.close();
            }
            if (!result)
                logger.log(Level.FINER, "Overwriting a file which has an open RandomAccessFile in \"{0}\" mode is not tolerated!", mode);
        }

        if (!total)
            logger.finer(
                    "Applications should ALWAYS close their streams or you may face strange 'errors'.\n"
                    + "Note that this issue is NOT AT ALL specific to TrueZIP, but rather imposed by this platform!");

        assertTrue(file.delete());
    }
}
