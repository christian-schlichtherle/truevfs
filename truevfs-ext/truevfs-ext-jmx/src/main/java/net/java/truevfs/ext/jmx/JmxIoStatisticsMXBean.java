/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.ext.jmx.stats.IoStatistics;

/**
 * The MXBean interface for {@linkplain IoStatistics I/O statistics}.
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
    int    getReadOperations();
    String getSubject();
    long   getTimeCreatedMillis();
    String getTimeCreatedString();
    long   getTimeUpdatedMillis();
    String getTimeUpdatedString();
    int    getWriteBytesPerOperation();
    long   getWriteBytesTotal();
    long   getWriteKilobytesPerSecond();
    long   getWriteNanosecondsPerOperation();
    long   getWriteNanosecondsTotal();
    int    getWriteOperations();
}
