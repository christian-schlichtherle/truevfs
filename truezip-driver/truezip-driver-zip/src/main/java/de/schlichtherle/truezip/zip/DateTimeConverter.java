/*
 * Copyright (C) 2009-2011 Schlichtherle IT Services
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

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import net.jcip.annotations.ThreadSafe;

/**
 * Converts Java time values to DOS date/time values and vice versa.
 * This class has been introduced in order to enhance interoperability
 * between different flavours of the ZIP file format specification when
 * converting date/time from the serialized DOS format in a ZIP file to
 * the local system time, which is represented by a UNIX-like encoding
 * by the Java API.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public enum DateTimeConverter {

    /**
     * This instance applies the schedule for Daylight Saving Time (DST),
     * i.e. all time conversions will apply DST where appropriate to a
     * particular date.
     * <p>
     * This behaviour provides best interoperability with:
     * <ul>
     * <li>Java SE: {@code jar} utility
     *     and {@code java.util.zip} package</li>
     * <li>Info-ZIP: {@code unzip}</li>
     * </ul>
     */
    JAR {
        @Override
        TimeZone newTimeZone() {
            return TimeZone.getDefault();
        }

        @Override
        boolean roundUp(long jTime) {
            return false;
        }
    },

    /**
     * This instance ignores the schedule for Daylight Saving Time (DST),
     * i.e. all time conversions will use the same raw offset and current
     * DST savings, regardless of whether DST savings should be applied to
     * a particular date or not.
     * <p>
     * This behavior provides best interoperability with:
     * <ul>
     * <li>Windows Vista Explorer (as of June 30<sup>th</sup>, 2009)</li>
     * <li>WinZip 12.0</li>
     * <li>7-Zip 4.65</li>
     * </ul>
     */
    ZIP {
        @Override
        TimeZone newTimeZone() {
            TimeZone tz = TimeZone.getDefault();
            tz = new SimpleTimeZone(
                    tz.getRawOffset() + tz.getDSTSavings(),
                    tz.getID());
            assert !tz.useDaylightTime();
            return tz;
        }

        @Override
        boolean roundUp(long jTime) {
            return true;
        }
    };

    /**
     * Smallest supported DOS date/time value in a ZIP file,
     * which is January 1<sup>st</sup>, 1980 AD 00:00:00 local time.
     */
    static final long MIN_DOS_TIME = (1 << 21) | (1 << 16); // 0x210000;

    /**
     * Largest supported DOS date/time value in a ZIP file,
     * which is December 31<sup>st</sup>, 2107 AD 23:59:58 local time.
     */
    static final long MAX_DOS_TIME =
            ((long) (2107 - 1980) << 25)
            | (12 << 21)
            | (31 << 16)
            | (23 << 11)
            | (59 << 5)
            | (58 >> 1);

    /**
     * A thread local lenient gregorian calendar for date/time
     * conversion which has its timezone set to the return value of
     * {@link #newTimeZone()}.
     */
    private final ThreadLocal<GregorianCalendar>
            calendar = new ThreadLocalGregorianCalendar();

    /**
     * Returns a new timezone to use for date/time conversion.
     * All returned instances must have the same
     * {@link TimeZone#hasSameRules(TimeZone) rules}.
     *
     * @return A new timezone for date/time conversion - never {@code null}.
     */
    abstract TimeZone newTimeZone();

    /**
     * Returns whether the given Java time should be rounded up or down to the
     * next two second interval when converting it to a DOS date/time.
     *
     * @param  jTime The number of milliseconds since midnight, January 1st,
     *         1970 AD UTC (called <i>epoch</i> alias <i>Java time</i>).
     * @return {@code true} for round-up, {@code false} for round-down.
     */
    abstract boolean roundUp(long jTime);

    /**
     * Returns a thread local lenient gregorian calendar for date/time
     * conversion which has its timezone set to the return value of
     * {@link #newTimeZone()}.
     *
     * @return A thread local lenient gregorian calendar.
     */
    private GregorianCalendar getGregorianCalendar() {
        return calendar.get();
    }

    /**
     * Converts a Java time value to a DOS date/time value.
     * The returned value is rounded up or down to even seconds,
     * depending on {@link #roundUp}.
     * If the Java time value is earlier than January 1<sup>st</sup>,
     * 1980 AD 00:00:00 local time, then this value is returned instead.
     * <p>
     * This method uses a lenient {@link GregorianCalendar} for the date/time
     * conversion which has its timezone set to the return value of
     * {@link #newTimeZone()}.
     *
     * @param  jTime The number of milliseconds since midnight, January 1st,
     *         1970 AD UTC (called <i>the epoch</i> alias Java time).
     * @return A DOS date/time value reflecting the local time zone and
     *         rounded down to even seconds which is minimum
     *         January 1<sup>st</sup>, 1980 AD 00:00:00.
     * @throws RuntimeException If {@code jTime} is negative
     *         or later than 2107 AD.
     * @see    #toJavaTime(long)
     * @see    #newTimeZone()
     */
    final long toDosTime(final long jTime) {
        if (jTime < 0)
            throw new IllegalArgumentException("Java time is negative: 0x"
                    + Long.toHexString(jTime).toUpperCase(Locale.ENGLISH));
        final GregorianCalendar cal = getGregorianCalendar();
        cal.setTimeInMillis(roundUp(jTime) ? jTime + 1999 : jTime);
        long dTime = cal.get(Calendar.YEAR) - 1980;
        if (dTime < 0)
            return MIN_DOS_TIME;
        dTime = (dTime << 25)
                | ((cal.get(Calendar.MONTH) + 1) << 21)
                | (cal.get(Calendar.DAY_OF_MONTH) << 16)
                | (cal.get(Calendar.HOUR_OF_DAY) << 11)
                | (cal.get(Calendar.MINUTE) << 5)
                | (cal.get(Calendar.SECOND) >> 1);
        if (MAX_DOS_TIME < dTime)
            throw new IllegalArgumentException(
                    "Java time is later than 2107 AD: 0x"
                    + Long.toHexString(jTime).toUpperCase(Locale.ENGLISH));
        assert MIN_DOS_TIME <= dTime && dTime <= MAX_DOS_TIME;
        return dTime;
    }

    /**
     * Converts a 32 bit integer encoded DOS date/time value to a Java time
     * value.
     * <p>
     * Note that not all 32 bit integers are valid DOS date/time values.
     * If an invalid DOS date/time value is provided, it gets adjusted by
     * overflowing the respective field value as if using a
     * {@link Calendar#setLenient lenient calendar}.
     * This feature is provided in order to read bogus ZIP archive files
     * created by third party tools.
     * However, the returned Java time may differ from its intended value at
     * the time of the creation of the ZIP archive file and when converting
     * it back again, the resulting DOS date/time value will not be the same as
     * {@code dTime}.
     * <p>
     * This method uses a lenient {@link GregorianCalendar} for the date/time
     * conversion which has its timezone set to the return value of
     * {@link #newTimeZone()}.
     *
     * @param  dTime The DOS date/time value.
     * @return The number of milliseconds since midnight, January 1st,
     *         1970 AD UTC (called <i>epoch</i> alias <i>Java time</i>).
     * @throws IllegalArgumentException If {@code dTime} is earlier
     *         than 1980 AD or greater than {@code 0xffffffffL}.
     * @see    #toDosTime(long)
     * @see    #newTimeZone()
     */
    final long toJavaTime(final long dTime) {
        if (dTime < MIN_DOS_TIME)
            throw new IllegalArgumentException(
                    "DOS date/time is earlier than 1980 AD: 0x"
                    + Long.toHexString(dTime).toUpperCase(Locale.ENGLISH));
        if (MAX_DOS_TIME < dTime)
            throw new IllegalArgumentException(
                    "DOS date/time is later than 2107 AD: 0x"
                    + Long.toHexString(dTime).toUpperCase(Locale.ENGLISH));
        final int time = (int) dTime;
        final GregorianCalendar cal = getGregorianCalendar();
        cal.set(Calendar.YEAR, 1980 + ((time >> 25) & 0x7f));
        cal.set(Calendar.MONTH, ((time >> 21) & 0x0f) - 1);
        cal.set(Calendar.DAY_OF_MONTH, (time >> 16) & 0x1f);
        cal.set(Calendar.HOUR_OF_DAY, (time >> 11) & 0x1f);
        cal.set(Calendar.MINUTE, (time >> 5) & 0x3f);
        cal.set(Calendar.SECOND, (time << 1) & 0x3e);
        // DOS date/time has only two seconds granularity.
        // Make calendar return only total seconds in order to make this
        // work correctly.
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /** @see #getGregorianCalendar() */
    private final class ThreadLocalGregorianCalendar
    extends ThreadLocal<GregorianCalendar> {
        @Override
        protected GregorianCalendar initialValue() {
            return new GregorianCalendar(newTimeZone());
        }
    };
}
