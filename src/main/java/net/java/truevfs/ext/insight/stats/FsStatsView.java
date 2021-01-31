/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight.stats;

import java.util.Date;

import static java.lang.Math.max;

/**
 * A view for {@link IoStats} and {@link SyncStats}.
 *
 * @author Christian Schlichtherle
 */
public interface FsStatsView extends IoStatsView, SyncStatsView {

    FsStats getStats();

    default IoStats getReadStats() {
        return getStats().getReadStats();
    }

    default IoStats getWriteStats() {
        return getStats().getWriteStats();
    }

    default SyncStats getSyncStats() {
        return getStats().getSyncStats();
    }

    default long getTimeMillis() {
        return getStats().getTimeMillis();
    }

    default int getReadBytesPerOperation() {
        return getReadStats().getBytesPerOperation();
    }

    default long getReadBytesTotal() {
        return getReadStats().getBytesTotal();
    }

    default long getReadKilobytesPerSecond() {
        return getReadStats().getKilobytesPerSecond();
    }

    default long getReadNanosecondsPerOperation() {
        return getReadStats().getNanosecondsPerOperation();
    }

    default long getReadNanosecondsTotal() {
        return getReadStats().getNanosecondsTotal();
    }

    default long getReadOperations() {
        return getReadStats().getSequenceNumber();
    }

    default int getReadThreadsTotal() {
        return getReadStats().getThreadsTotal();
    }

    default int getWriteBytesPerOperation() {
        return getWriteStats().getBytesPerOperation();
    }

    default long getWriteBytesTotal() {
        return getWriteStats().getBytesTotal();
    }

    default long getWriteKilobytesPerSecond() {
        return getWriteStats().getKilobytesPerSecond();
    }

    default long getWriteNanosecondsPerOperation() {
        return getWriteStats().getNanosecondsPerOperation();
    }

    default long getWriteNanosecondsTotal() {
        return getWriteStats().getNanosecondsTotal();
    }

    default long getWriteOperations() {
        return getWriteStats().getSequenceNumber();
    }

    default int getWriteThreadsTotal() {
        return getWriteStats().getThreadsTotal();
    }

    default long getSyncNanosecondsPerOperation() {
        return getSyncStats().getNanosecondsPerOperation();
    }

    default long getSyncNanosecondsTotal() {
        return getSyncStats().getNanosecondsTotal();
    }

    default long getSyncOperations() {
        return getSyncStats().getSequenceNumber();
    }

    default int getSyncThreadsTotal() {
        return getSyncStats().getThreadsTotal();
    }

    default String getTimeCreatedDate() {
        return new Date(getTimeCreatedMillis()).toString();
    }

    default long getTimeCreatedMillis() {
        return getTimeMillis();
    }

    default String getTimeUpdatedDate() {
        return new Date(getTimeUpdatedMillis()).toString();
    }

    default long getTimeUpdatedMillis() {
        return max(max(getReadStats().getTimeMillis(), getWriteStats().getTimeMillis()), getSyncStats().getTimeMillis());
    }
}
