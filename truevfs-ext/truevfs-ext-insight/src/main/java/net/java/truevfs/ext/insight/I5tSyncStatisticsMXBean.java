/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.ext.insight.stats.SyncStatistics;

/**
 * An MXBean interface for {@linkplain SyncStatistics sync statistics}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public interface I5tSyncStatisticsMXBean {
    String getSubject();
    long   getSyncNanosecondsPerOperation();
    long   getSyncNanosecondsTotal();
    long   getSyncOperations();
    int    getSyncThreadsTotal();
    String getTimeCreatedDate();
    long   getTimeCreatedMillis();
    String getTimeUpdatedDate();
    long   getTimeUpdatedMillis();

    void rotate();
}
