/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import javax.annotation.concurrent.ThreadSafe;
import javax.management.ObjectName;
import static net.java.truevfs.comp.jmx.JmxUtils.*;
import net.java.truevfs.ext.jmx.stats.FsStatistics;
import net.java.truevfs.ext.jmx.stats.SyncStatistics;

/**
 * The JMX controller for {@linkplain SyncStatistics sync statistics}.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxSyncStatistics implements JmxColleague {

    private final JmxMediator mediator;
    private final int offset;

    public JmxSyncStatistics(final JmxMediator mediator, final int offset) {
        if (offset < 0 || mediator.getLoggerSize() <= offset)
            throw new IllegalArgumentException();
        this.mediator = mediator;
        this.offset = offset;
    }

    String getSubject() {
        return "Sync Operations";
    }

    FsStatistics getStats() {
        return mediator.getSyncStats(offset);
    }

    private ObjectName name() {
        return mediator.nameBuilder(FsStatistics.class)
                .put("subject", getSubject())
                .put("offset", mediator.formatLoggerOffset(offset))
                .get();
    }

    protected JmxSyncStatisticsMXBean newView() {
        return new JmxSyncStatisticsView(this);
    }

    @Override
    public void start() {
        register(name(), newView());
    }
}
