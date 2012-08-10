/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import java.util.Objects;
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

    private final long time = System.currentTimeMillis();
    private final JmxMediator mediator;

    public JmxSyncStatistics(final JmxMediator mediator) {
        this.mediator = Objects.requireNonNull(mediator);
    }

    long getTimeCreatedMillis() {
        return time;
    }

    String getSubject() {
        return "Sync Operations";
    }

    SyncStatistics getSyncStats() {
        return mediator.getSyncStats();
    }

    @Override
    public void start() {
        register(name(), newView());
    }

    private ObjectName name() {
        return mediator.nameBuilder(FsStatistics.class)
                .put("subject", getSubject())
                .get();
    }

    protected JmxSyncStatisticsMXBean newView() {
        return new JmxSyncStatisticsView(this);
    }
}
