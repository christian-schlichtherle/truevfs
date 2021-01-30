/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import net.java.truevfs.ext.insight.stats.IoStats;

import javax.annotation.concurrent.ThreadSafe;

/**
 * A controller for {@link IoStats}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class I5tIoStatsController extends I5tStatsController {

    I5tIoStatsController(I5tMediator mediator, int offset) {
        super(mediator, offset);
    }

    @Override
    I5tStatsView newView() {
        return new I5tIoStatsView(this);
    }
}
