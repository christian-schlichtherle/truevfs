/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.io;

import de.truezip.driver.zip.io.DateTimeConverter;
import static de.truezip.driver.zip.io.DateTimeConverter.MAX_DOS_TIME;
import static de.truezip.driver.zip.io.DateTimeConverter.MIN_DOS_TIME;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 */
public abstract class DateTimeConverterTestSuite {

    private DateTimeConverter instance;
    private long minJavaTime, maxJavaTime;

    @Before
    public void setUp() {
        instance = getInstance();
        GregorianCalendar calendar = new GregorianCalendar(instance.newTimeZone());
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(1980, Calendar.JANUARY, 1, 0, 0, 0);
        minJavaTime = calendar.getTimeInMillis();
        calendar.set(2107, Calendar.DECEMBER, 31, 23, 59, 58); // 58 seconds!!!
        maxJavaTime = calendar.getTimeInMillis();
    }

    abstract DateTimeConverter getInstance();

    @Test
    public final void testNewTimeZone() {
        final TimeZone tz1 = instance.newTimeZone();
        assertNotNull(tz1);
        final TimeZone tz2 = instance.newTimeZone();
        assertNotNull(tz1);
        assertNotSame(tz1, tz2);
        assertTrue(tz1.hasSameRules(tz2));
    }

    @Test
    public final void testToJavaTime() {
        assertThat(instance.toJavaTime(Long.MIN_VALUE), is(minJavaTime));
        assertThat(instance.toJavaTime(MIN_DOS_TIME - 1), is(minJavaTime));
        assertThat(instance.toJavaTime(MIN_DOS_TIME), is(minJavaTime));
        assertThat(instance.toJavaTime(MAX_DOS_TIME), is(maxJavaTime));
        assertThat(instance.toJavaTime(MAX_DOS_TIME + 1), is(maxJavaTime));
        assertThat(instance.toJavaTime(Long.MAX_VALUE), is(maxJavaTime));
    }

    @Test
    public final void testToDosTime() {
        try {
            instance.toDosTime(Long.MIN_VALUE);
            fail();
        } catch (IllegalArgumentException expected) {
        }
        try {
            instance.toDosTime(-1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertThat(instance.toDosTime(0), is(MIN_DOS_TIME));
        assertThat(instance.toDosTime(minJavaTime - 1), is(MIN_DOS_TIME));
        assertThat(instance.toDosTime(minJavaTime), is(MIN_DOS_TIME));
        assertThat(instance.toDosTime(maxJavaTime), is(MAX_DOS_TIME));
        assertThat(instance.toDosTime(maxJavaTime + 1), is(MAX_DOS_TIME));
        assertThat(instance.toDosTime(Long.MAX_VALUE), is(MAX_DOS_TIME));
    }

    @Test
    public void testRoundTripConversion() {
        for (long dTime : new long[] {
            MIN_DOS_TIME,
            MAX_DOS_TIME,
        }) {
            assertThat(instance.toDosTime(instance.toJavaTime(dTime)), is(dTime));
        }
    }

    @Test
    public void testGranularity() {
        final long jTime = System.currentTimeMillis();
        final long dTime = instance.toDosTime(jTime);
        assertThat(instance.toDosTime(jTime - 2000), not(is(dTime)));
        assertThat(instance.toDosTime(jTime + 2000), not(is(dTime)));
        assertThat(instance.toJavaTime(instance.toDosTime(jTime - 2000)), not(is(jTime)));
        assertThat(instance.toJavaTime(instance.toDosTime(jTime + 2000)), not(is(jTime)));
    }
}