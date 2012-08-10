/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Provides statistics for the federated file systems managed by a file system
 * manager.
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
    void   close();
}
