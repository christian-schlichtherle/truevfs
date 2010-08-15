/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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
 * Tests the TrueZIP API in de.schlichtherle.io with the ZIP driver.
 * 
 * @author Christian Schlichtherle
 * @version $Revision$
 */
public class ZipFileTest extends FileTestCase {

    /**
     * Creates a new instance of <code>ZipFileTest</code>.
     */
    public ZipFileTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        suffix = ".zip";
        File.setDefaultArchiveDetector(new DefaultArchiveDetector("zip"));

        super.setUp();
    }
}
