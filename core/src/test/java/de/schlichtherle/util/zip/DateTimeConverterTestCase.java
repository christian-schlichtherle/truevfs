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

package de.schlichtherle.util.zip;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 *
 * @author Christian Schlichtherle
 * @version $Revision$
 * @since TrueZIP 6.7
 */
public abstract class DateTimeConverterTestCase extends TestCase {

    private static final long MIN_DOS_TIME = DateTimeConverter.MIN_DOS_TIME;

    private DateTimeConverter instance;
    private Calendar cal;

    public DateTimeConverterTestCase(String testName) {
        super(testName);
    }

    protected void setUp()
    throws Exception {
        super.setUp();
        instance = getInstance();
        cal = new GregorianCalendar(instance.createTimeZone());
        cal.set(Calendar.MILLISECOND, 0);
    }

    protected void tearDown()
    throws Exception {
        super.tearDown();
    }

    protected abstract DateTimeConverter getInstance();

    /**
     * Test of toJavaTime method, of class DateTimeConverter.
     */
    public void testToJavaTime() {
        try {
            instance.toJavaTime(-1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        try {
            instance.toJavaTime(0);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        try {
            instance.toJavaTime(MIN_DOS_TIME - 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        boolean ea = false;
        assert ea = true; // NOT ea == true !
        try {
            instance.toJavaTime(UInt.MAX_VALUE);
            assertFalse("Expected RuntimeException if assertions are enabled", ea);
        } catch (RuntimeException ex) {
            if (!ea) {
                final Error afe = new AssertionFailedError(
                        "Did not expect a RuntimeException if assertions are disabled");
                afe.initCause(ex);
                throw afe;
            }
        }

        try {
            instance.toJavaTime(UInt.MAX_VALUE + 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        // Check MIN_DOS_TIME constant.
        cal.set(1980, Calendar.JANUARY, 1, 0, 0, 0);
        assertEquals(cal.getTimeInMillis(),
                instance.toJavaTime(MIN_DOS_TIME));
    }

    /**
     * Test of toDosTime method, of class DateTimeConverter.
     */
    public void testToDosTime() {
        try {
            instance.toDosTime(-1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        assertEquals(MIN_DOS_TIME, instance.toDosTime(0));

        // Check MIN_DOS_TIME constant.
        cal.set(1980, Calendar.JANUARY, 1, 0, 0, 0);
        assertEquals(MIN_DOS_TIME, instance.toDosTime(cal.getTimeInMillis()));
    }

    /**
     * Test of createTimeZone method, of class DateTimeConverter.
     */
    public void testCreateTimeZone() {
        final TimeZone tz1 = instance.createTimeZone();
        assertNotNull(tz1);
        final TimeZone tz2 = instance.createTimeZone();
        assertNotNull(tz1);
        assertNotSame(tz1, tz2);
        assertTrue(tz1.hasSameRules(tz2));
    }

    /**
     * Test of roundUp method, of class DateTimeConverter.
     */
    public void testRoundUp() {
        instance.roundUp(System.currentTimeMillis()); // check for RuntimeException
    }
}
