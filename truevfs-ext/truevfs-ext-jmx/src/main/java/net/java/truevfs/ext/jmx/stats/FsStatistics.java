/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx.stats;

import javax.annotation.concurrent.Immutable;

/**
 * Immutable statistics for file system operations.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public final class FsStatistics {

    /**
     * Returns file system statistics with all properties set to zero.
     * 
     * @return File system statistics with all properties set to zero.
     */
    public static FsStatistics create() {
        final IoStatistics io = IoStatistics.create();
        return new FsStatistics(System.currentTimeMillis(),
                io, io, SyncStatistics.create());
    }

    private final long time;
    private final IoStatistics read, write;
    private final SyncStatistics sync;

    private FsStatistics(
            final long time,
            final IoStatistics read,
            final IoStatistics write,
            final SyncStatistics sync) {
        assert 0 <= time;
        assert null != read;
        assert null != write;
        assert null != sync;
        this.time = time;
        this.read = read;
        this.write = write;
        this.sync = sync;
    }

    /**
     * Returns the time these statistics have been originally created in
     * milliseconds since the epoch.
     * Note that this property is not altered when an operation gets logged.
     * 
     * @return The time these statistics have been originally created in
     *         milliseconds since the epoch.
     */
    public long getTimeCreated() {
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
