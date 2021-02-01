/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.insight;

import global.namespace.truevfs.ext.insight.stats.IoStats;

/**
 * A controller for {@link IoStats}.
 *
 * @author Christian Schlichtherle
 */
final class I5tIoStatsController extends I5tStatsController {

    I5tIoStatsController(I5tMediator mediator, int offset) {
        super(mediator, offset);
    }

    @Override
    I5tStatsView newView() {
        return new I5tIoStatsView(this);
    }
}
