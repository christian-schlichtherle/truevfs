/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.insight;

import global.namespace.truevfs.ext.insight.stats.SyncStats;

/**
 * A controller for {@link SyncStats}.
 *
 * @author Christian Schlichtherle
 */
final class I5tSyncStatsController extends I5tStatsController {

    I5tSyncStatsController(I5tMediator mediator, int offset) {
        super(mediator, offset);
    }

    @Override
    I5tStatsView newView() {
        return new I5tSyncStatsView(this);
    }
}
