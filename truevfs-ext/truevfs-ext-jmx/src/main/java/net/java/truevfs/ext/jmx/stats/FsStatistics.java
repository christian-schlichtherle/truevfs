/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx.stats;

import java.beans.ConstructorProperties;
import java.io.Serializable;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;

/**
 * Immutable statistics for file system operations.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public final class FsStatistics implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Returns file system statistics with all properties set to zero.
     * 
     * @return File system statistics with all properties set to zero.
     */
    public static FsStatistics create() {
        final IoStatistics io = IoStatistics.create();
        final SyncStatistics sync = SyncStatistics.create();
        return new FsStatistics(System.currentTimeMillis(), io, io, sync);
    }

    private final long time;
    private final IoStatistics read, write;
    private final SyncStatistics sync;

    /** @deprecated Use {@link #create} instead. */
    @Deprecated
    @ConstructorProperties({ "timeMillis", "readStats", "writeStats", "syncStats" })
    public FsStatistics(
            final long time,
            final IoStatistics read,
            final IoStatistics write,
            final SyncStatistics sync) {
        if (0 > (this.time = time)) throw new IllegalArgumentException();
        this.read = Objects.requireNonNull(read);
        this.write = Objects.requireNonNull(write);
        this.sync = Objects.requireNonNull(sync);
    }

    /**
     * Returns the time these statistics have been created in milliseconds
     * since the epoch.
     * Note that this property remains unchanged when an operation gets logged.
     * 
     * @return The time these statistics have been created in milliseconds
     *         since the epoch.
     */
    public long getTimeMillis() {
        return time;
    }

    public IoStatistics getReadStats() {
        return read;
    }

    public IoStatistics getWriteStats() {
        return write;
    }

    public SyncStatistics getSyncStats() {
        return sync;
    }

    /**
     * Logs a read operation with the given sample data and returns a new
     * object to reflect the updated statistics.
     * 
     * @param  nanos the execution time in nanoseconds.
     * @param  bytes the number of bytes read.
     * @return A new object which reflects the updated statistics.
     * @throws IllegalArgumentException if any parameter value is negative.
     */
    public FsStatistics logRead(long nanos, int bytes) {
        return new FsStatistics(time, read.log(nanos, bytes), write, sync);
    }

    /**
     * Logs a write operation with the given sample data and returns a new
     * object to reflect the updated statistics.
     * 
     * @param  nanos the execution time in nanoseconds.
     * @param  bytes the number of bytes written.
     * @return A new object which reflects the updated statistics.
     * @throws IllegalArgumentException if any parameter is negative.
     */
    public FsStatistics logWrite(long nanos, int bytes) {
        return new FsStatistics(time, read, write.log(nanos, bytes), sync);
    }

    /**
     * Logs a sync operation with the given sample data and returns a new
     * object to reflect the updated statistics.
     * 
     * @param  nanos the execution time in nanoseconds.
     * @return A new object which reflects the updated statistics.
     * @throws IllegalArgumentException if any parameter value is negative.
     */
    public FsStatistics logSync(long nanos) {
        return new FsStatistics(time, read, write, sync.log(nanos));
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) return true;
        if (!(other instanceof FsStatistics)) return false;
        final FsStatistics that = (FsStatistics) other;
        return this.time == that.time
                && this.read.equals(that.read)
                && this.write.equals(that.write)
                && this.sync.equals(that.sync);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 13 * hash + (int) (this.time ^ (this.time >>> 32));
        hash = 13 * hash + Objects.hashCode(this.read);
        hash = 13 * hash + Objects.hashCode(this.write);
        hash = 13 * hash + Objects.hashCode(this.sync);
        return hash;
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[timeMillis=%d, readStats=%s, writeStats=%s, syncStats=%s]",
                getClass().getName(), time, read, write, sync);
    }
}
