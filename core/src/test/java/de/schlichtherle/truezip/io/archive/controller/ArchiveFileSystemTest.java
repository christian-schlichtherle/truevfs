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

package de.schlichtherle.truezip.io.archive.controller;

import junit.framework.TestCase;

/**
 * @author Christian Schlichtherle
 */
public class ArchiveFileSystemTest extends TestCase {
    
    public ArchiveFileSystemTest(String testName) {
        super(testName);
    }

    /**
     * Test of split method, of class de.schlichtherle.truezip.io.ArchiveFileSystem.
     */
    public void testSplitEntryName() {
        testSplitEntryName("a/", "b", "a/b/");
        testSplitEntryName("a/", "b", "a/b");
        testSplitEntryName("/", "a", "a/");
        testSplitEntryName("/", "a", "a");
        testSplitEntryName("/", "a", "/a/");
        testSplitEntryName("/", "a", "/a");
        testSplitEntryName(null, "", "/");
        testSplitEntryName(null, "", "");
    }

    public void testSplitEntryName(
            final String parent,
            final String base,
            final String path) {
        final String[] split = new String[2];
        ArchiveFileSystem.split(path, split);
        assertEquals(parent, split[0]);
        assertEquals(base, split[1]);
    }
    
}
