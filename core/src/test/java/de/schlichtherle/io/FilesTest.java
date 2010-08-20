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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;

/**
 * @author Christian Schlichtherle
 */
public class FilesTest extends TestCase {

    private static final Logger logger = Logger.getLogger(
            FilesTest.class.getName());

    public FilesTest(String testName) {
        super(testName);
    }

    public void testNormalize() {
        final java.io.File empty = new java.io.File("");
        assertEquals(".", Files.normalize(empty).getPath());

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

        //testNormalize("/", "/");
        //testNormalize("/", "//");
        testNormalize("/", "/.");
        testNormalize("/", "/./");

        testNormalize("/..", "/..");
        testNormalize("/..", "/../.");
        testNormalize("/../..", "/.././..");
        testNormalize("/../..", "/.././../.");

        testNormalize(".", ".");
        testNormalize(".", "./");
        testNormalize("..", "..");
        testNormalize("..", "../");
        testNormalize("a", "./a");
        testNormalize("a", "./a/");
        testNormalize("../a", "../a");
        testNormalize("../a", "../a/");
        testNormalize("a/b", "./a/./b");
        testNormalize("a/b", "./a/./b/");
        testNormalize("../a/b", "../a/./b");
        testNormalize("../a/b", "../a/./b/");
        testNormalize("b", "./a/../b");
        testNormalize("b", "./a/../b/");
        testNormalize("../b", "../a/../b");
        testNormalize("../b", "../a/../b/");

        testNormalize(".", ".//");
        testNormalize(".", ".///");
        testNormalize(".", ".////");
        testNormalize("..", "..//");
        testNormalize("a", ".//a//");
        testNormalize("../a", "..//a");
        testNormalize("../a", "..//a//");
        testNormalize("a/b", ".//a//.//b");
        testNormalize("a/b", ".//a//.//b//");
        testNormalize("../a/b", "..//a//.//b");
        testNormalize("../a/b", "..//a//.//b//");
        testNormalize("b", ".//a//..//b//");
        testNormalize("../b", "..//a//..//b");
        testNormalize("../b", "..//a//..//b//");
    }
    
    void testNormalize(String result, final String path) {
        result = result.replace('/', File.separatorChar);
        java.io.File file = new java.io.File(path);
        assertEquals(result, Files.normalize(file).getPath());
        file = new java.io.File(path + '/');
        assertEquals(result, Files.normalize(file).getPath());
        file = new java.io.File(path + File.separator);
        assertEquals(result, Files.normalize(file).getPath());
        file = new java.io.File(path + File.separator + ".");
        assertEquals(result, Files.normalize(file).getPath());
        file = new java.io.File(path + File.separator + "." + File.separator + ".");
        assertEquals(result, Files.normalize(file).getPath());
        file = new java.io.File(path + File.separator + "." + File.separator + "." + File.separator + ".");
        assertEquals(result, Files.normalize(file).getPath());
    }

    /**
     * Test of isWritableOrCreatable method, of class de.schlichtherle.io.ArchiveController.
     * <p>
     * Note that this test is not quite correctly designed: It tests the
     * operating system rather than the method.
     */
    public void testIsWritableOrCreatable() throws IOException {
        final java.io.File file = File.createTempFile("tzp-test", null);
        
        boolean result = Files.isWritableOrCreatable(file);
        assertTrue(result);

        boolean total = true;
        final FileInputStream fin = new FileInputStream(file);
        try {
            result = Files.isWritableOrCreatable(file);
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
                result = Files.isWritableOrCreatable(file);
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
