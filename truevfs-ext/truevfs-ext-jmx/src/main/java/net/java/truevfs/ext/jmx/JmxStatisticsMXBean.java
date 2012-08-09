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
    String getKind();
    int    getSequenceNumber();
    String getTimeCreated();
    long   getTimeCreatedMillis();
    long   getReadSumOfBytes();
    int    getReadBytesPerOperation();
    long   getReadKilobytesPerSecond();
    int    getReadNumberOfOperations();
    long   getWriteSumOfBytes();
    int    getWriteBytesPerOperation();
    long   getWriteKilobytesPerSecond();
    int    getWriteNumberOfOperations();
    void   close();
}
