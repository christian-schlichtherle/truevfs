/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.ext.jmx.stats.IoStatistics;

/**
 * An MXBean interface for {@linkplain IoStatistics I/O statistics}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public interface JmxIoStatisticsMXBean {
    int    getReadBytesPerOperation();
    long   getReadBytesTotal();
    long   getReadKilobytesPerSecond();
    long   getReadNanosecondsPerOperation();
    long   getReadNanosecondsTotal();
    long   getReadOperations();
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

    //JmxIoStatisticsMXBean snapshot();
    void rotate();
}
