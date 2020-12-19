/*
 * Copyright (C) 2005-2020 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import net.java.truevfs.ext.insight.stats.SyncStats;

import javax.annotation.concurrent.ThreadSafe;

/**
 * A controller for {@link SyncStats}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class I5tSyncStatsController extends I5tStatsController {

    I5tSyncStatsController(I5tMediator mediator, int offset) {
        super(mediator, offset);
    }

    @Override
    I5tStatsView newView() {
        return new I5tSyncStatsView(this);
    }
}
