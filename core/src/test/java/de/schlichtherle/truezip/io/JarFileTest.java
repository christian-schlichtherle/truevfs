/*
 * Copyright (C) 2009-2010 Schlichtherle IT Services
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

/**
 * Tests the TrueZIP API in de.schlichtherle.truezip.io with the JAR driver.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class JarFileTest extends FileTestCase {
    private static final java.io.File _tempDir = new java.io.File(System.getProperty("java.io.tmpdir"));
    private static final java.io.File _baseDir = _tempDir;
    
    /**
     * Creates a new instance of {@code JarFileTest}.
     */
    public JarFileTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        suffix = ".jar";
        File.setDefaultArchiveDetector(new DefaultArchiveDetector("jar"));

        super.setUp();
    }
}
