/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight.stats;

/**
 * An MXBean interface for {@link IoStats}.
 *
 * @author Christian Schlichtherle
 */
public interface IoStatsView {

    int getReadBytesPerOperation();

    long getReadBytesTotal();

    long getReadKilobytesPerSecond();

    long getReadNanosecondsPerOperation();

    long getReadNanosecondsTotal();

    long getReadOperations();

    int getReadThreadsTotal();

    String getSubject();

    String getTimeCreatedDate();

    long getTimeCreatedMillis();

    String getTimeUpdatedDate();

    long getTimeUpdatedMillis();

    int getWriteBytesPerOperation();

    long getWriteBytesTotal();

    long getWriteKilobytesPerSecond();

    long getWriteNanosecondsPerOperation();

    long getWriteNanosecondsTotal();

    long getWriteOperations();

    int getWriteThreadsTotal();

    void rotate();
}
