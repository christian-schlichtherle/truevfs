/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx.stats;

import java.beans.ConstructorProperties;
import java.io.Serializable;
import javax.annotation.concurrent.Immutable;

/**
 * Immutable statistics for sync operations.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public final class SyncStatistics implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Returns sync statistics with all properties set to zero.
     * 
     * @return sync statistics with all properties set to zero.
     */
    public static SyncStatistics create() {
        return new SyncStatistics(0, 0); // cannot cache because of time!
    }

    private final long time, seqno, nanos;

    private SyncStatistics(long seqno, long nanos) {
        this(System.currentTimeMillis(), seqno, nanos);
    }

    /** @deprecated Use {@link #create} instead. */
    @Deprecated
    @ConstructorProperties({ "timeMillis", "sequenceNumber", "nanosecondsTotal" })
    public SyncStatistics(final long time, final long seqno, final long nanos) {
        if (0 > (time | seqno | nanos)) throw new IllegalArgumentException();
        this.time = time;
        this.seqno = seqno;
        this.nanos = nanos;
    }

    /**
     * Returns the time these statistics have been created in milliseconds
     * since the epoch.
     * Note that this property gets updated when an operation gets logged.
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
    public long getSequenceNumber() {
        return seqno;
    }

    public long getNanosecondsTotal() {
        return nanos;
    }

    public long getNanosecondsPerOperation() {
        final long seqno = this.seqno;
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
        } catch (IllegalArgumentException overflow) {
            return new SyncStatistics(1, nanos);
        }
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) return true;
        if (!(other instanceof SyncStatistics)) return false;
        final SyncStatistics that = (SyncStatistics) other;
        return this.time == that.time
                && this.seqno == that.seqno
                && this.nanos == that.nanos;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + (int) (this.time ^ (this.time >>> 32));
        hash = 17 * hash + (int) (this.seqno ^ (this.seqno >>> 32));
        hash = 17 * hash + (int) (this.nanos ^ (this.nanos >>> 32));
        return hash;
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
