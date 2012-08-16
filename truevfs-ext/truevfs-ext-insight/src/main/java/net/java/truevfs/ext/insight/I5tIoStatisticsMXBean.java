/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.ext.insight.stats.IoStatistics;

/**
 * An MXBean interface for {@linkplain IoStatistics I/O statistics}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public interface I5tIoStatisticsMXBean {
    int    getReadBytesPerOperation();
    long   getReadBytesTotal();
    long   getReadKilobytesPerSecond();
    long   getReadNanosecondsPerOperation();
    long   getReadNanosecondsTotal();
    long   getReadOperations();
    int    getReadThreadsTotal();
    String getSubject();
    String getTimeCreatedDate();
    long   getTimeCreatedMillis();
    String getTimeUpdatedDate();
    long   getTimeUpdatedMillis();
    int    getWriteBytesPerOperation();
    long   getWriteBytesTotal();
    long   getWriteKilobytesPerSecond();
    long   getWriteNanosecondsPerOperation();
    long   getWriteNanosecondsTotal();
    long   getWriteOperations();
    int    getWriteThreadsTotal();

    void rotate();
}
