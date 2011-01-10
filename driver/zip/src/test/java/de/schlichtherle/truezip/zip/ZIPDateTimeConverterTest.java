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

package de.schlichtherle.truezip.zip;

import de.schlichtherle.truezip.zip.DateTimeConverter;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ZIPDateTimeConverterTest extends DateTimeConverterTestCase {

    public static Test suite() throws Exception {
        TestSuite suite = new TestSuite(ZIPDateTimeConverterTest.class);

        return suite;
    }

    public ZIPDateTimeConverterTest(String testName) {
        super(testName);
    }

    @Override
	protected DateTimeConverter getInstance() {
        return DateTimeConverter.ZIP;
    }

    // TODO: Add behavioral tests here.
}
