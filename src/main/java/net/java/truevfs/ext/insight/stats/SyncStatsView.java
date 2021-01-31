/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight.stats;

/**
 * An MXBean interface for {@link SyncStats}.
 *
 * @author Christian Schlichtherle
 */
public interface SyncStatsView {

    String getSubject();

    long getSyncNanosecondsPerOperation();

    long getSyncNanosecondsTotal();

    long getSyncOperations();

    int getSyncThreadsTotal();

    String getTimeCreatedDate();

    long getTimeCreatedMillis();

    String getTimeUpdatedDate();

    long getTimeUpdatedMillis();

    void rotate();
}
