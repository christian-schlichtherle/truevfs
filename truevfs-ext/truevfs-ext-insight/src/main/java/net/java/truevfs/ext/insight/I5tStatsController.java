/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import net.java.truevfs.comp.jmx.JmxComponent;
import net.java.truevfs.ext.insight.stats.FsStats;
import net.java.truevfs.ext.insight.stats.IoStats;
import net.java.truevfs.ext.insight.stats.SyncStats;

import javax.annotation.concurrent.ThreadSafe;
import javax.management.ObjectName;

import static java.util.Locale.ENGLISH;

/**
 * A base controller for {@link IoStats} or {@link SyncStats}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
abstract class I5tStatsController implements JmxComponent {

    private final I5tMediator mediator;
    private final int offset;

    I5tStatsController(I5tMediator mediator, int offset) {
        assert (0 <= offset);
        this.mediator = mediator;
        this.offset = offset;
    }

    @Override
    public final void activate() {
        mediator.register(getObjectName(), newView());
    }

    private ObjectName getObjectName() {
        return mediator
                .nameBuilder(FsStats.class)
                .put("subject", getSubject())
                .put("offset", mediator.formatOffset(offset))
                .get();
    }

    final String getSubject() {
        return mediator.getSubject();
    }

    final FsStats getStats() {
        return mediator.stats(offset);
    }

    final void rotate() {
        mediator.rotateStats(this);
    }

    abstract I5tStatsView newView();

    @Override
    public final String toString() {
        return String.format(
                ENGLISH,
                "%s[subject=%s, offset=%d, mediator=%s]",
                getClass().getName(),
                getSubject(),
                offset,
                mediator);
    }
}
