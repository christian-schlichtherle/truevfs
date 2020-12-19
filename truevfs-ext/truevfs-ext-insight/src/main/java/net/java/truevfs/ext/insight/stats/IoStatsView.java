/*
 * Copyright (C) 2005-2020 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight.stats;

import javax.annotation.concurrent.ThreadSafe;

/**
 * An MXBean interface for {@link IoStats}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
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
