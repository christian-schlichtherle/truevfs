/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx.model;

import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A logger for I/O operations.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class IoLogger {
    private final AtomicReference<IoStatistics> read =
            new AtomicReference<>(IoStatistics.get());
    private final AtomicReference<IoStatistics> write =
            new AtomicReference<>(IoStatistics.get());
    private final int seqno;
    private final long created = System.currentTimeMillis();

    public IoLogger() { this(0); }

    private IoLogger(final int seqno) {
        this.seqno = seqno;
    }

    public int getSequenceNumber() {
        return seqno;
    }

    public long getTimeCreatedMillis() {
        return created;
    }

    public IoStatistics getReadStats() {
        return read.get();
    }

    public IoStatistics getWriteStats() {
        return write.get();
    }

    /**
     * Logs a read operation with the given sample data.
     * 
     * @param  bytes the number of bytes read.
     * @param  nanos the execution time in nanoseconds.
     * @throws IllegalArgumentException if any parameter is negative.
     * @throws IoOverflowException if any statistical property would overflow.
     */
    public void logRead(int bytes, long nanos) throws IoOverflowException {
        log(read, bytes, nanos);
    }

    /**
     * Attempts to log a read operation with the given sample data.
     * 
     * @param  bytes the number of bytes read.
     * @param  nanos the execution time in nanoseconds.
     * @return {@cod false} if and only if the operation fails because any
     *         statistical property would overflow.
     * @throws IllegalArgumentException if any parameter is negative.
     */
    public boolean tryLogRead(int bytes, long nanos) {
        return tryLog(read, bytes, nanos);
    }

    /**
     * Logs a write operation with the given sample data.
     * 
     * @param  bytes the number of bytes written.
     * @param  nanos the execution time in nanoseconds.
     * @throws IllegalArgumentException if any parameter is negative.
     * @throws IoOverflowException if any statistical property would overflow.
     */
    public void logWrite(int bytes, long nanos) throws IoOverflowException {
        log(write, bytes, nanos);
    }

    /**
     * Attempts to log a write operation with the given sample data.
     * 
     * @param  bytes the number of bytes written.
     * @param  nanos the execution time in nanoseconds.
     * @return {@cod false} if and only if the operation fails because any
     *         statistical property would overflow.
     * @throws IllegalArgumentException if any parameter is negative.
     */
    public boolean tryLogWrite(int bytes, long nanos) {
        return tryLog(write, bytes, nanos);
    }

    private static boolean tryLog(
            final AtomicReference<IoStatistics> reference,
            final int bytes,
            final long nanos) {
        try {
            log(reference, bytes, nanos);
            return true;
        } catch (final IoOverflowException ex) {
            return false;
        }
    }

    private static void log(
            final AtomicReference<IoStatistics> reference,
            final int bytes,
            final long nanos)
    throws IoOverflowException {
        while (true) {
            final IoStatistics expected = reference.get();
            final IoStatistics updated = expected.log(bytes, nanos);
            if (reference.weakCompareAndSet(expected, updated)) break;
        }
    }

    /**
     * Returns a new logger with an incremented sequence number.
     * 
     * @return A new logger with an incremented sequence number.
     */
    public IoLogger next() { return new IoLogger(seqno + 1); }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[sequenceNumber=%d, timeCreatedMillis=%d, read=%s, write=%s]",
                getClass().getName(), seqno, created, read, write);
    }
}
