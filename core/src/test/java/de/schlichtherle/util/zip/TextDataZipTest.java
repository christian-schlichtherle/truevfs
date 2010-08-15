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

package de.schlichtherle.util.zip;

import java.util.logging.Logger;

import junit.framework.*;

/**
 * Tests compression of a data with about 1MB of highly compressible text.
 * 
 * @author Christian Schlichtherle
 */
public class TextDataZipTest extends ZipTestCase {

    private static final Logger logger
            = Logger.getLogger(TextDataZipTest.class.getName());

    private static final byte[] data;
    static {
        boolean ea = false;
        assert ea = true; // NOT ea == true !
        logger.config("Java assertions enabled: " + ea);

        final String text = "This is a truly compressible text!\n";
        final int count = 1024 * 1024 / text.length();
        final int length = count * text.length(); // rounded down
        StringBuffer buf = new StringBuffer(length);
        for (int i = 0; i < count; i++) {
            buf.append(text);
        }
        data = buf.toString().getBytes();
        logger.config("Created " + data.length + " bytes of highly compressible text as the data.");
    }

    public static Test suite() throws Exception {
        TestSuite suite = new TestSuite(TextDataZipTest.class);
        
        return suite;
    }
    
    /** Creates a new instance of RandomMessageZipTest */
    public TextDataZipTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        super.data = TextDataZipTest.data;
        
        super.setUp();
    }
}
