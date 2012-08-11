/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx.stats;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A lock-free logger for {@link FsStatistics}.
 * All operations get logged at offset zero.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class FsLogger {

    private static final String SIZE_PROPERTY_KEY =
            FsLogger.class.getName() + ".size";
    private static final int SIZE = Integer.getInteger(SIZE_PROPERTY_KEY, 10);

    private static final int[] MAX_VALUES = {
        9, 99, 999, 9999, 99999, 999999, 9999999, 99999999, 999999999,
        Integer.MAX_VALUE
    };

    private static int length(final int x) {
        assert 0 >= x;
        int i = 0;
        while (x > MAX_VALUES[i++]) { }
        return i;
    }

    private final AtomicInteger position = new AtomicInteger();
    private final AtomicReferenceArray<FsStatistics> stats;

    public FsLogger() { this(SIZE); }

    public FsLogger(final int size) {
        final AtomicReferenceArray<FsStatistics> stats =
                this.stats = new AtomicReferenceArray<>(size);
        final FsStatistics fs = FsStatistics.create();
        for (int i = 0; i < size; i++) stats.set(i, fs);
    }

    public int size() { return stats.length(); }

    private int position() { return position.get(); }

    private int index(final int offset) {
        final int size = size();
        if (offset < 0 || size <= offset) throw new IllegalArgumentException();
        int index = position() - offset;
        if (index < 0) index += size;
        return index;
    }

    public String format(int offset) {
        final int max = size() - 1;
        if (offset < 0 || max < offset) throw new IllegalArgumentException();
        return String.format(String.format("%%0%dd", length(max)), offset);
    }

    public FsStatistics getStats(int offset) { return stats.get(index(offset)); }

    private FsStatistics current() { return getStats(0); }

    /**
     * Logs a read operation with the given sample data and returns a new
     * object to reflect the updated statistics.
     * The sequence number of the returned object will be incremented and may
     * eventually overflow to zero.
     * 
     * @param  nanos the execution time in nanoseconds.
     * @param  bytes the number of bytes read.
     * @return A new object which reflects the updated statistics.
     * @throws IllegalArgumentException if any parameter value is negative.
     */
    public IoStatistics logRead(long nanos, int bytes) {
        while (true) {
            final FsStatistics expected = current();
            final FsStatistics updated = expected.logRead(nanos, bytes);
            if (stats.weakCompareAndSet(position(), expected, updated))
                return updated.getReadStats();
        }
    }

    /**
     * Logs a write operation with the given sample data and returns a new
     * object to reflect the updated statistics.
     * The sequence number of the returned object will be incremented and may
     * eventually overflow to zero.
     * 
     * @param  nanos the execution time in nanoseconds.
     * @param  bytes the number of bytes written.
     * @return A new object which reflects the updated statistics.
     * @throws IllegalArgumentException if any parameter is negative.
     */
    public IoStatistics logWrite(long nanos, int bytes) {
        while (true) {
            final FsStatistics expected = current();
            final FsStatistics updated = expected.logWrite(nanos, bytes);
            if (stats.weakCompareAndSet(position(), expected, updated))
                return updated.getWriteStats();
        }
    }

    /**
     * Logs a sync operation with the given sample data and returns a new
     * object to reflect the updated statistics.
     * The sequence number of the returned object will be incremented and may
     * eventually overflow to zero.
     * 
     * @param  nanos the execution time in nanoseconds.
     * @return A new object which reflects the updated statistics.
     * @throws IllegalArgumentException if any parameter value is negative.
     */
    public SyncStatistics logSync(long nanos) {
        while (true) {
            final FsStatistics expected = current();
            final FsStatistics updated = expected.logSync(nanos);
            if (stats.weakCompareAndSet(position(), expected, updated))
                return updated.getSyncStats();
        }
    }

    public int rotate() {
        final int next = next();
        stats.set(next, FsStatistics.create());
        return next;
    }

    private int next() {
        final AtomicInteger position = this.position;
        final int size = size();
        while (true) {
            final int expected = position.get();
            int updated = expected + 1;
            if (size <= updated) updated -= size;
            if (position.compareAndSet(expected, updated)) return updated;
        }
    }
}
