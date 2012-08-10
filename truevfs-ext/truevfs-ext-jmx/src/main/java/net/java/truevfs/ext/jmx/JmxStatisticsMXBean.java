/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.ext.jmx.model.IoLogger;
import net.java.truevfs.ext.jmx.model.IoStatistics;

/**
 * The combined MXBean interface for an {@linkplain IoLogger I/O logger}
 * and its {@linkplain IoStatistics I/O statistics}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public interface JmxStatisticsMXBean {
    String getSubject();
    int    getSequenceNumber();
    String getTimeCreated();
    long   getTimeCreatedMillis();
    String getTimeUpdated();
    long   getTimeUpdatedMillis();
    int    getReadBytesPerOperation();
    long   getReadBytesTotal();
    long   getReadKilobytesPerSecond();
    long   getReadNanosecondsTotal();
    int    getReadOperationsTotal();
    int    getWriteBytesPerOperation();
    long   getWriteBytesTotal();
    long   getWriteKilobytesPerSecond();
    long   getWriteNanosecondsTotal();
    int    getWriteOperationsTotal();
}
