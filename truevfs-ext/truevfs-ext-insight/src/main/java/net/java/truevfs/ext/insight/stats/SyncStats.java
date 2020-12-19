/*
 * Copyright (C) 2005-2020 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight.stats;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 * An immutable record of statistics for sync operations.
 *
 * @author Christian Schlichtherle
 */
@EqualsAndHashCode
@Getter
@ToString
public final class SyncStats implements Serializable {

    private static final long serialVersionUID = 0;

    /**
     * Returns sync statistics with all properties set to zero.
     */
    public static SyncStats getInstance() {
        return new SyncStats(0, 0, 0); // cannot cache because of timeMillis!
    }

    private final long sequenceNumber, nanosecondsTotal, timeMillis;
    private final int threadsTotal;

    private SyncStats(long sequenceNumber, long nanosecondsTotal, int threadsTotal) {
        this(sequenceNumber, nanosecondsTotal, threadsTotal, System.currentTimeMillis());
    }

    private SyncStats(
            final long sequenceNumber,
            final long nanosecondsTotal,
            final int threadsTotal,
            final long timeMillis
    ) {
        if (0 > (sequenceNumber | nanosecondsTotal | threadsTotal | timeMillis)) {
            throw new IllegalArgumentException();
        }
        this.sequenceNumber = sequenceNumber;
        this.nanosecondsTotal = nanosecondsTotal;
        this.threadsTotal = threadsTotal;
        this.timeMillis = timeMillis;
    }

    public long getNanosecondsPerOperation() {
        return 0 == getSequenceNumber() ? 0 : getNanosecondsTotal() / getSequenceNumber();
    }

    /**
     * Logs a sync operation with the given sample data and returns a new
     * object to reflect the updated statistics at the current system time.
     * If any property would overflow to a negative value as a result of the
     * update, then the returned object will simply have its sequence number
     * set to one (!) and its other properties will be reset to reflect only
     * the given parameter values at the current system time.
     * In other words, the statistics would restart from fresh.
     *
     * @param nanosDelta the execution time.
     * @return A new object which reflects the updated statistics at the
     * current system time.
     * @throws IllegalArgumentException if any parameter value is negative.
     */
    public SyncStats log(final long nanosDelta, final int threadsTotal) {
        if (0 > (nanosDelta | threadsTotal)) {
            throw new IllegalArgumentException();
        }
        try {
            return new SyncStats(
                    getSequenceNumber() + 1,
                    getNanosecondsTotal() + nanosDelta,
                    threadsTotal
            );
        } catch (IllegalArgumentException e) {
            return new SyncStats(1, nanosDelta, 1);
        }
    }

    public boolean equalsIgnoreTime(SyncStats that) {
        return this.getSequenceNumber() == that.getSequenceNumber() &&
                this.getNanosecondsTotal() == that.getNanosecondsTotal() &&
                this.getThreadsTotal() == that.getThreadsTotal();
    }
}
