/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import net.java.truevfs.ext.insight.stats.SyncStats;
import net.java.truevfs.ext.insight.stats.SyncStatsView;

import javax.annotation.concurrent.ThreadSafe;
import javax.management.MBeanInfo;

/**
 * A view for {@link SyncStats}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class I5tSyncStatsView extends I5tStatsView {

    I5tSyncStatsView(I5tSyncStatsController controller) {
        super(controller, SyncStatsView.class, true);
    }

    @Override
    protected String getDescription(MBeanInfo info) {
        return "A log of sync statistics.";
    }
}
