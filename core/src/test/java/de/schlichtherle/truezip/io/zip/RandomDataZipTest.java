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

package de.schlichtherle.truezip.io.zip;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tests compression of a data with 1MB of random data.
 * 
 * @author Christian Schlichtherle
 */
public class RandomDataZipTest extends PlainZipTestCase {

    private static final Logger logger
            = Logger.getLogger(RandomDataZipTest.class.getName());

    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    private static final byte[] data = new byte[1024];
    static {
        boolean ea = false;
        assert ea = true; // NOT ea == true !
        logger.log(Level.CONFIG, "Java assertions enabled: {0}", ea);

        rnd.nextBytes(data);
        logger.log(Level.CONFIG, "Created {0} bytes of random data.", data.length);
    }
    
    /**
     * Creates a new instance of RandomDataZipTest 
     */
    public RandomDataZipTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.data = RandomDataZipTest.data;
        
        super.setUp();
    }
}
