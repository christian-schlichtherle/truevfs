/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx.model;

import java.math.BigInteger;
import static java.math.BigInteger.*;
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
     * Returns statistics with all properties set to zero.
     * 
     * @return statistics with all properties set to zero.
     */
    public static IoStatistics get() {
        return new IoStatistics(0, 0, 0); // TODO: Consider caching.
    }

    private final int seqno;
    private final long bytes, nanos;

    private IoStatistics(final int seqno, final long bytes, final long nanos) {
        this.seqno = seqno;
        this.bytes = bytes;
        this.nanos = nanos;
    }

    public int getSequenceNumber() {
        return seqno;
    }

    public long getSumOfBytes() {
        return bytes;
    }

    public long getSumOfNanoseconds() {
        return nanos;
    }

    public long getKilobytesPerSecond() {
        final long nanos = this.nanos;
        if (0 == nanos) return 0;
        //return (bytes * 1000 * 1000 * 1000) / (nanos * 1024);
        return valueOf(bytes).multiply(TEN_POW_NINE)
                .divide(valueOf(nanos).multiply(VALUE_OF_1024))
                .longValue();
    }

    public int getAverageBytesPerOperation() {
        final int seqNo = this.seqno;
        return 0 == seqNo ? 0 : (int) (bytes / seqNo);
    }

    /**
     * Logs an I/O operation with the given metric data.
     * 
     * @param  bytes the number of bytes read or written.
     * @param  nanos the execution time in nanoseconds.
     * @return New I/O statistics with an incremented sequence number and
     *         updated properties to reflect the given metric data.
     */
    public IoStatistics log(int bytes, long nanos) {
        return new IoStatistics(seqno + 1, this.bytes + bytes, this.nanos + nanos);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) return true;
        if (!(other instanceof IoStatistics)) return false;
        final IoStatistics that = (IoStatistics) other;
        return this.seqno == that.seqno
                && this.bytes == that.bytes
                && this.nanos == that.nanos;
    }

    @Override
    public int hashCode() {
        // See Joshua Bloch, Effective Java (2nd Edition), item 9.
        long r = 17;
        r = 31 * r + seqno;
        r = 31 * r + bytes;
        r = 31 * r + nanos;
        return (int) r;
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[sequenceNumber=%d, sumOfBytes=%d, sumOfNanoseconds=%d]",
                getClass().getName(), seqno, bytes, nanos);
    }
}
