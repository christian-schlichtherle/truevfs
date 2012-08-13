/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx.stats;

import java.math.BigInteger;
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.valueOf;
import javax.annotation.concurrent.Immutable;

/**
 * Immutable statistics for I/O operations.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public final class IoStatistics {
    private static final BigInteger TEN_POW_NINE = TEN.pow(9);
    private static final BigInteger VALUE_OF_1024 = valueOf(1024);

    /**
     * Returns I/O statistics with all properties set to zero.
     * 
     * @return I/O statistics with all properties set to zero.
     */
    public static IoStatistics create() {
        return new IoStatistics(0, 0, 0); // cannot cache because of time!
    }

    private final long time = System.currentTimeMillis();
    private final int seqno;
    private final long nanos, bytes;

    private IoStatistics(final int seqno, final long nanos, final long bytes) {
        if (0 > (seqno | nanos | bytes)) throw new ArithmeticException();
        this.seqno = seqno;
        this.nanos = nanos;
        this.bytes = bytes;
    }

    /**
     * Returns the time these statistics have been updated in milliseconds
     * since the epoch.
     * Note that this property gets updated whenever an operation gets logged.
     * 
     * @return The time these statistics have been updated in milliseconds
     *         since the epoch.
     */
    public long getTimeUpdated() {
        return time;
    }

    /**
     * Returns the non-negative sequence number.
     * This property reflects the total number of I/O operations.
     * 
     * @return The non-negative sequence number.
     */
    public int getSequenceNumber() {
        return seqno;
    }

    public long getNanosecondsTotal() {
        return nanos;
    }

    public long getNanosecondsPerOperation() {
        final int seqno = this.seqno;
        return 0 == seqno ? 0 : nanos / seqno;
    }

    public long getBytesTotal() {
        return bytes;
    }

    public int getBytesPerOperation() {
        final int seqno = this.seqno;
        return 0 == seqno ? 0 : (int) (bytes / seqno);
    }

    public long getKilobytesPerSecond() {
        final long nanos = this.nanos;
        if (0 == nanos) return 0;
        //return (bytes * 1000 * 1000 * 1000) / (nanos * 1024);
        return valueOf(bytes).multiply(TEN_POW_NINE)
                .divide(valueOf(nanos).multiply(VALUE_OF_1024))
                .longValue();
    }

    /**
     * Logs an I/O operation with the given sample data and returns a new
     * object to reflect the updated statistics.
     * If any property would overflow to a negative value as a result of the
     * update, then the returned object will simply have its sequence number
     * set to one (!) and its other properties will be reset to reflect only
     * the given parameter values.
     * In other words, the statistics would restart from fresh.
     * 
     * @param  nanos the execution time in nanoseconds.
     * @param  bytes the number of bytes read or written.
     * @return A new object which reflects the updated statistics.
     * @throws IllegalArgumentException if any parameter value is negative.
     */
    public IoStatistics log(final long nanos, final int bytes) {
        if (0 > (nanos | bytes)) throw new IllegalArgumentException();
        try {
            return new IoStatistics(seqno + 1, this.nanos + nanos, this.bytes + bytes);
        } catch (ArithmeticException overflow) {
            return new IoStatistics(1, nanos, bytes);
        }
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[timeMillis=%d, sequenceNumber=%d, nanosecondsTotal=%d, bytesTotal=%d]",
                getClass().getName(), time, seqno, nanos, bytes);
    }
}
