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

package de.schlichtherle.io;

/**
 * Tests the TrueZIP API in de.schlichtherle.io with the JAR driver.
 * 
 * @author Christian Schlichtherle
 * @version $Revision$
 */
public class JarFileTest extends FileTestCase {
    
    /**
     * Creates a new instance of <code>JarFileTest</code>.
     */
    public JarFileTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        suffix = ".jar";
        File.setDefaultArchiveDetector(new DefaultArchiveDetector("jar"));

        super.setUp();
    }
}
