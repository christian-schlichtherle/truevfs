/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx.stats;

/**
 * Immutable statistics for file system sync operations.
 * 
 * @author Christian Schlichtherle
 */
public class SyncStatistics {

    /**
     * Returns sync statistics with all properties set to zero.
     * 
     * @return sync statistics with all properties set to zero.
     */
    public static SyncStatistics zero() {
        return new SyncStatistics(0, 0); // cannot cache because of time!
    }

    private final long time = System.currentTimeMillis();
    private final int seqno;
    private final long nanos;
    
    private SyncStatistics(final int seqno, final long nanos) {
        if (0 > (seqno | nanos)) throw new ArithmeticException();
        this.seqno = seqno;
        this.nanos = nanos;
    }

    /**
     * Returns the time these statistics have been created in milliseconds
     * since the epoch.
     * 
     * @return The time these statistics have been created in milliseconds
     *         since the epoch.
     */
    public long getTimeMillis() {
        return time;
    }

    /**
     * Returns the non-negative sequence number.
     * This property reflects the total number of sync operations.
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

    /**
     * Logs a sync operation with the given sample data and returns a new
     * object to reflect the updated statistics.
     * If any property would overflow to a negative value as a result of the
     * update, then the returned object will simply have its sequence number
     * set to one (!) and its other properties will be reset to reflect only
     * the given parameter values.
     * In other words, the statistics would restart from fresh.
     * 
     * @param  nanos the execution time in nanoseconds.
     * @return A new object which reflects the updated statistics.
     * @throws IllegalArgumentException if any parameter value is negative.
     */
    public SyncStatistics log(final long nanos) {
        if (0 > nanos) throw new IllegalArgumentException();
        try {
            return new SyncStatistics(seqno + 1, this.nanos + nanos);
        } catch (ArithmeticException overflow) {
            return new SyncStatistics(1, nanos);
        }
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[timeMillis=%d, sequenceNumber=%d, nanosecondsTotal=%d]",
                getClass().getName(), time, seqno, nanos);
    }
}
