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
package de.schlichtherle.truezip.io.zip;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

/**
 * Converts Java time values to DOS date/time values and vice versa.
 * This class has been introduced in order to enhance interoperability
 * between different flavours of the ZIP file format specification when
 * converting date/time from the serialized DOS format in a ZIP file to
 * the local system time, which is represented by a UNIX-like encoding
 * by the Java API.
 * <p>
 * This base class is thread-safe if and only if a sub class is thread-safe,
 * too.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class DateTimeConverter {

    /**
     * Smallest supported DOS date/time value in a ZIP file,
     * which is January 1<sup>st</sup>, 1980 AD 00:00:00 local time.
     */
    static final long MIN_DOS_TIME = (1 << 21) | (1 << 16); // 0x210000;

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
    public static final DateTimeConverter JAR = new DateTimeConverter() {
        @Override
		protected TimeZone newTimeZone() {
            return TimeZone.getDefault();
        }

        @Override
		protected boolean roundUp(long jTime) {
            return false;
        }
    };

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
    public static final DateTimeConverter ZIP = new DateTimeConverter() {
        @Override
		protected TimeZone newTimeZone() {
            TimeZone tz = TimeZone.getDefault();
            tz = new SimpleTimeZone(
                    tz.getRawOffset() + tz.getDSTSavings(),
                    tz.getID());
            assert !tz.useDaylightTime();
            return tz;
        }

        @Override
		protected boolean roundUp(long jTime) {
            return true;
        }
    };

    /**
     * Converts a Java time value to a DOS date/time value.
     * The returned value is rounded up or down to even seconds,
     * depending on {@link #roundUp}.
     * If the Java time value is earlier than January 1<sup>st</sup>,
     * 1980 AD 00:00:00 local time, then this value is returned instead.
     * <p>
     * This method uses a {@link Calendar} for the date/time conversion
     * which has its timezone set to the return value of
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
            throw new IllegalArgumentException("Java time is negative: " + Long.toHexString(jTime));

        final Calendar cal = getCalendar();
        cal.setTimeInMillis(roundUp(jTime) ? jTime + 1999 : jTime);
        final int year = cal.get(Calendar.YEAR) - 1980;
        if (year < 0)
            return MIN_DOS_TIME;
        if (year > 0x7f)
            throw new IllegalArgumentException(
                    "Year of Java time is later than 2107 AD: " + (1980 + year));
        final long dTime = (year << 25)
                | ((cal.get(Calendar.MONTH) + 1) << 21)
                | (cal.get(Calendar.DAY_OF_MONTH) << 16)
                | (cal.get(Calendar.HOUR_OF_DAY) << 11)
                | (cal.get(Calendar.MINUTE) << 5)
                | (cal.get(Calendar.SECOND) >> 1);
        assert dTime >= MIN_DOS_TIME;
        return dTime;
    }

    /**
     * Converts a DOS date/time value to a Java time value.
     * DOS date/time values are encoded using 32 bit integers.
     * However, not all 32 bit integers make a valid DOS date/time value.
     * If an illegal 32 bit integer is provided,
     * the behaviour of this method depends on the assertion status:
     * <p>
     * This method uses a {@link Calendar} for the date/time conversion
     * which has its timezone set to the return value of
     * {@link #newTimeZone()}.
     * <p>
     * If assertions are <em>enabled</em>,
     * {@link Calendar#setLenient(boolean) Calendard.setLenient(false)}
     * is called in order to throw a {@link RuntimeException}
     * when parsing illegal DOS date/time field values.
     * This can be used in order to detect bogus ZIP archive files created
     * by third party tools.
     * <p>
     * If assertions are <em>disabled</em> however,
     * {@link Calendar#setLenient(boolean) Calendard.setLenient(true)}
     * is called in order to adjust illegal DOS date/time field values
     * by overflowing them into their adjacent fields.
     * This can be used in order to read bogus ZIP archive files created
     * by third party tools.
     * However, the returned Java time may differ from its intended value at
     * the time of the creation of the ZIP archive file and when converting
     * it back again, the resulting DOS date/time will not be the same as
     * {@code dTime}.
     * Hence, interoperability is negatively affected in this case.
     *
     * @param  dTime The DOS date/time value.
     * @throws RuntimeException If {@code dTime} is earlier
     *         than 1980 AD
     *         or greater than {@code 0xffffffffL}
     *         or holds an illegal DOS date/time field combination
     *         and assertions are enabled.
     * @return The number of milliseconds since midnight, January 1st,
     *         1970 AD UTC (called <i>epoch</i> alias <i>Java time</i>).
     * @see    #toDosTime(long)
     * @see    #newTimeZone()
     */
    final long toJavaTime(final long dTime) {
        if (dTime < MIN_DOS_TIME)
            throw new IllegalArgumentException(
                    "DOS date/time is earlier than 1980 AD: "
                    + Long.toHexString(dTime));
        if (dTime > UInt.MAX_VALUE)
            throw new IllegalArgumentException(
                    "DOS date/time value is greater than "
                    + Long.toHexString(UInt.MAX_VALUE) + ": "
                    + Long.toHexString(dTime));

        final int time = (int) dTime;
        final Calendar cal = getCalendar();
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
        /*if (cal.get(Calendar.YEAR) > 1980 + 0x7f) {
        assert cal.isLenient();
        assert 2108 == cal.get(Calendar.YEAR);
        throw new IllegalArgumentException(
        "An illegal DOS date/time field combination caused the calendar to overflow beyond year 2107 AD: "
        + Long.toHexString(dTime));
        }*/

        return cal.getTimeInMillis();
    }

    /**
     * Returns a <em>thread local</em> {@link Calendar} instance for the
     * date/time conversion which has its timezone set to the return value
     * of {@link #newTimeZone()}.
     *
     * @return A {@link Calendar} instance.
     */
    private Calendar getCalendar() {
        return calendar.get();
    }

    /** @see #getCalendar() */
    private final ThreadLocal<Calendar> calendar = new ThreadLocal<Calendar>() {
        @Override
        protected Calendar initialValue() {
            final Calendar cal = new GregorianCalendar(newTimeZone());
            boolean ea = false;
            assert ea = true; // NOT ea == true !
            cal.setLenient(!ea);
            return cal;
        }
    };

    /**
     * Returns a new timezone to use for date/time conversion.
     * All returned instances must have the same
     * {@link TimeZone#hasSameRules(TimeZone) rules}.
     *
     * @return A new timezone for date/time conversion - never {@code null}.
     */
    protected abstract TimeZone newTimeZone();

    /**
     * Returns whether the given Java time should be rounded up or down to the
     * next two second interval when converting it to a DOS date/time.
     *
     * @param  jTime The number of milliseconds since midnight, January 1st,
     *         1970 AD UTC (called <i>epoch</i> alias <i>Java time</i>).
     * @return {@code true} for round-up, {@code false} for round-down.
     */
    protected abstract boolean roundUp(long jTime);

    /*protected boolean isWritingPreciseTime() {
        return false;
    }*/
}
