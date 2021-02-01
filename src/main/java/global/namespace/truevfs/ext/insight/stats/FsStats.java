/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.insight.stats;

import lombok.*;

import java.io.Serializable;

import static java.util.Objects.requireNonNull;

/**
 * An immutable record of statistics for file system operations.
 *
 * @author Christian Schlichtherle
 */
@EqualsAndHashCode
@Getter
@ToString
public final class FsStats implements Serializable {

    private static final long serialVersionUID = 0;

    /**
     * Returns file system statistics with all properties set to zero.
     */
    public static FsStats getInstance() {
        val io = IoStats.getInstance();
        val sync = SyncStats.getInstance();
        return new FsStats(io, io, sync); // cannot cache because of timeMillis!
    }

    private final IoStats readStats, writeStats;
    private final SyncStats syncStats;
    private final long timeMillis;

    private FsStats(IoStats readStats, IoStats writeStats, SyncStats syncStats) {
        this(readStats, writeStats, syncStats, System.currentTimeMillis());
    }

    private FsStats(
            final IoStats readStats,
            final IoStats writeStats,
            final SyncStats syncStats,
            final long timeMillis
    ) {
        if (0 > timeMillis) {
            throw new IllegalArgumentException();
        }
        this.readStats = requireNonNull(readStats);
        this.writeStats = requireNonNull(writeStats);
        this.syncStats = requireNonNull(syncStats);
        this.timeMillis = timeMillis;
    }

    /**
     * Logs a read operation with the given sample data and returns a new
     * object to reflect the updated statistics.
     *
     * @param nanosDelta the execution time in nanoseconds.
     * @param bytesDelta the number of bytes read.
     * @return A new object which reflects the updated statistics.
     * @throws IllegalArgumentException if any parameter value is negative.
     */
    public FsStats logRead(long nanosDelta, long bytesDelta, int threadsTotal) {
        return new FsStats(getReadStats().log(nanosDelta, bytesDelta, threadsTotal), getWriteStats(), getSyncStats(), getTimeMillis());
    }

    /**
     * Logs a write operation with the given sample data and returns a new
     * object to reflect the updated statistics.
     *
     * @param nanosDelta the execution time in nanoseconds.
     * @param bytesDelta the number of bytes written.
     * @return A new object which reflects the updated statistics.
     * @throws IllegalArgumentException if any parameter is negative.
     */
    public FsStats logWrite(long nanosDelta, long bytesDelta, int threadsTotal) {
        return new FsStats(getReadStats(), getWriteStats().log(nanosDelta, bytesDelta, threadsTotal), getSyncStats(), getTimeMillis());
    }

    /**
     * Logs a sync operation with the given sample data and returns a new
     * object to reflect the updated statistics.
     *
     * @param nanosDelta the execution time in nanoseconds.
     * @return A new object which reflects the updated statistics.
     * @throws IllegalArgumentException if any parameter value is negative.
     */
    public FsStats logSync(long nanosDelta, int threadsTotal) {
        return new FsStats(getReadStats(), getWriteStats(), getSyncStats().log(nanosDelta, threadsTotal), getTimeMillis());
    }

    public boolean equalsIgnoreTime(FsStats that) {
        return this.getReadStats().equalsIgnoreTime(that.getReadStats()) &&
                this.getWriteStats().equalsIgnoreTime(that.getWriteStats()) &&
                this.getSyncStats().equalsIgnoreTime(that.getSyncStats());
    }
}
