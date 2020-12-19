/*
 * Copyright (C) 2005-2020 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight.stats;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * An immutable record of statistics for I/O operations.
 *
 * @author Christian Schlichtherle
 */
@EqualsAndHashCode
@Getter
@ToString
public final class IoStats implements Serializable {

    private static final long serialVersionUID = 0;

    private static final BigInteger tenPowNine = BigInteger.TEN.pow(9); // 1_000_000_000
    private static final BigInteger twoPowTen = BigInteger.TWO.pow(10); // 1_024

    /**
     * Returns I/O statistics with all properties set to zero.
     */
    public static IoStats getInstance() {
        return new IoStats(0, 0, 0, 0, 0); // cannot cache because of timeMillis!
    }

    private final long sequenceNumber, nanosecondsTotal, bytesTotal, timeMillis;
    private final int threadsTotal;

    private IoStats(long sequenceNumber, long nanosecondsTotal, long bytesTotal, int threadsTotal) {
        this(sequenceNumber, nanosecondsTotal, bytesTotal, threadsTotal, System.currentTimeMillis());
    }

    private IoStats(
            final long sequenceNumber,
            final long nanosecondsTotal,
            final long bytesTotal,
            final int threadsTotal,
            final long timeMillis
    ) {
        if (0 > (sequenceNumber | nanosecondsTotal | bytesTotal | threadsTotal | timeMillis)) {
            throw new IllegalArgumentException();
        }
        this.sequenceNumber = sequenceNumber;
        this.nanosecondsTotal = nanosecondsTotal;
        this.bytesTotal = bytesTotal;
        this.threadsTotal = threadsTotal;
        this.timeMillis = timeMillis;
    }

    public long getNanosecondsPerOperation() {
        return 0 == getSequenceNumber() ? 0 : getNanosecondsTotal() / getSequenceNumber();
    }

    public int getBytesPerOperation() {
        return 0 == getSequenceNumber() ? 0 : (int) (getBytesTotal() / getSequenceNumber());
    }

    public long getKilobytesPerSecond() {
        return 0 == getSequenceNumber()
                ? 0
                : BigInteger
                .valueOf(getBytesTotal())
                .multiply(tenPowNine)
                .divide(BigInteger.valueOf(getNanosecondsTotal()).multiply(twoPowTen))
                .longValue();
    }

    /**
     * Logs an I/O operation with the given sample data and returns a new
     * object to reflect the updated statistics at the current system time.
     * If any property would overflow to a negative value as a result of the
     * update, then the returned object will simply have its sequence number
     * set to one (!) and its other properties will be reset to reflect only
     * the given parameter values at the current system time.
     * In other words, the statistics would restart from fresh.
     *
     * @param nanosDelta the execution time.
     * @param bytesDelta the number of bytes read or written.
     * @return A new object which reflects the updated statistics at the current system time.
     * @throws IllegalArgumentException if any parameter value is negative.
     */
    public IoStats log(final long nanosDelta, final long bytesDelta, final int threadsTotal) {
        if (0 > (nanosDelta | bytesDelta | threadsTotal)) {
            throw new IllegalArgumentException();
        }
        try {
            return new IoStats(
                    getSequenceNumber() + 1,
                    getNanosecondsTotal() + nanosDelta,
                    getBytesTotal() + bytesDelta,
                    threadsTotal
            );
        } catch (IllegalArgumentException e) {
            return new IoStats(1, nanosDelta, bytesDelta, 1);
        }
    }

    public boolean equalsIgnoreTime(IoStats that) {
        return this.getSequenceNumber() == that.getSequenceNumber() &&
                this.getNanosecondsTotal() == that.getNanosecondsTotal() &&
                this.getBytesTotal() == that.getBytesTotal() &&
                this.getThreadsTotal() == that.getThreadsTotal();
    }
}
