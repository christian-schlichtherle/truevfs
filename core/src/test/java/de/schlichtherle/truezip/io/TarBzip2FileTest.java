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

package de.schlichtherle.truezip.io;

import de.schlichtherle.truezip.io.archive.driver.tar.TarBZip2Driver;

/**
 * Tests the TrueZIP API in de.schlichtherle.truezip.io with the TAR.BZ2 driver.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class TarBzip2FileTest extends FileTestCase {

    /**
     * Creates a new instance of TarBzip2FileTest
     */
    public TarBzip2FileTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        suffix = ".tar.bz2";
        // We want to test TrueZIP with this driver, not the memory
        // consumption of CBZip2(In|Out)putStream, so we set the block size
        // to its minimum in order to prevent OOME's with the default JVM max
        // memory settings.
        File.setDefaultArchiveDetector(new DefaultArchiveDetector(
                "tar.bz2", new TarBZip2Driver(1)));

        super.setUp();
    }
}
