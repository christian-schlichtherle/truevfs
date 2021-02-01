/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.insight.stats;

import lombok.val;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.UnaryOperator;

import static java.util.Locale.ENGLISH;

/**
 * A logger for {@link FsStats}.
 * All operations get logged at offset zero.
 *
 * @author Christian Schlichtherle
 */
public final class FsLogger {

    private static final String defaultSizePropertyKey = FsLogger.class + ".defaultSize";

    private static final Integer defaultSize = Integer.getInteger(defaultSizePropertyKey, 10);

    private static final int[] maxValues = new int[]{
            9, 99, 999, 9999, 99999, 999999, 9999999, 99999999, 999999999, Integer.MAX_VALUE,
    };

    @SuppressWarnings("StatementWithEmptyBody")
    private static int length(final int x) {
        assert 0 <= x;
        int i = 0;
        while (x > maxValues[i++]) {
        }
        return i;
    }

    /**
     * Adds a hash value for the current thread to the given set and returns its size.
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private static int logCurrentThread(final Set<Integer> set) {
        synchronized (set) {
            set.add(System.identityHashCode(Thread.currentThread()));
            return set.size();
        }
    }

    private final int size;

    private final AtomicInteger position = new AtomicInteger();

    private final AtomicReferenceArray<FsStats> stats;

    private volatile Set<Integer>
            readThreads = new HashSet<>(),
            writeThreads = new HashSet<>(),
            syncThreads = new HashSet<>();

    public FsLogger() {
        this(defaultSize);
    }

    public FsLogger(final int size) {
        this.size = size;
        this.stats = new AtomicReferenceArray<>(size);
        for (int i = 0; i < size; i++) {
            stats.set(i, FsStats.getInstance());
        }
    }

    public int size() {
        return size;
    }

    public String format(final int offset) {
        val max = size() - 1;
        if (offset < 0 || max < offset) {
            throw new IllegalArgumentException();
        }
        return String.format(ENGLISH, String.format(ENGLISH, "%%0%dd", length(max)), offset);
    }

    private int position() {
        return position.get();
    }

    private int index(final int offset) {
        if (offset < 0 || size() <= offset) {
            throw new IllegalArgumentException();
        }
        int index = position() - offset;
        if (index < 0) {
            index += size();
        }
        return index;
    }

    public FsStats stats(int offset) {
        return stats.get(index(offset));
    }

    public FsStats current() {
        return stats(0);
    }

    private FsStats update(final UnaryOperator<FsStats> next) {
        FsStats expect, update;
        do {
            expect = current();
            update = next.apply(expect);
        } while (!stats.compareAndSet(position(), expect, update));
        return update;
    }

    private int next() {
        int expect, update;
        do {
            expect = position();
            update = expect + 1;
            if (size() <= update) {
                update -= size();
            }
        } while (!position.compareAndSet(expect, update));
        return update;
    }

    /**
     * Logs a read operation with the given sample data and returns a new
     * object to reflect the updated statistics.
     * The sequence number of the returned object will be incremented and may
     * eventually overflow to zero.
     *
     * @param nanos the execution time in nanoseconds.
     * @param bytes the number of bytes read.
     * @return A new object which reflects the updated statistics.
     * @throws IllegalArgumentException if any parameter value is negative.
     */
    public IoStats logRead(final long nanos, final int bytes) {
        val threads = logCurrentThread(readThreads);
        return update(stats -> stats.logRead(nanos, bytes, threads)).getReadStats();
    }

    /**
     * Logs a write operation with the given sample data and returns a new
     * object to reflect the updated statistics.
     * The sequence number of the returned object will be incremented and may
     * eventually overflow to zero.
     *
     * @param nanos the execution time in nanoseconds.
     * @param bytes the number of bytes written.
     * @return A new object which reflects the updated statistics.
     * @throws IllegalArgumentException if any parameter is negative.
     */
    public IoStats logWrite(final long nanos, final int bytes) {
        val threads = logCurrentThread(writeThreads);
        return update(stats -> stats.logWrite(nanos, bytes, threads)).getWriteStats();
    }

    /**
     * Logs a sync operation with the given sample data and returns a new
     * object to reflect the updated statistics.
     * The sequence number of the returned object will be incremented and may
     * eventually overflow to zero.
     *
     * @param nanos the execution time in nanoseconds.
     * @return A new object which reflects the updated statistics.
     * @throws IllegalArgumentException if any parameter value is negative.
     */
    public SyncStats logSync(final long nanos) {
        val threads = logCurrentThread(syncThreads);
        return update(stats -> stats.logSync(nanos, threads)).getSyncStats();
    }

    public int rotate() {
        val n = next();
        stats.set(n, FsStats.getInstance());
        readThreads = new HashSet<>();
        writeThreads = new HashSet<>();
        syncThreads = new HashSet<>();
        return n;
    }
}
