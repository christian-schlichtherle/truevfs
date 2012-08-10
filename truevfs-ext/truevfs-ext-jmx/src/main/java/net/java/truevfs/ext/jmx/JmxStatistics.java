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
import net.java.truevfs.ext.jmx.stats.IoStatistics;
import net.java.truevfs.ext.jmx.stats.SyncStatistics;

/**
 * The combined JMX controller for an {@linkplain FsStatistics I/O logger}
 * and its {@linkplain IoStatistics I/O statistics}.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxStatistics implements JmxColleague {
    /*private static final String SIZE_PROPERTY_KEY =
            JmxStatistics.class.getName() + ".size";
    private static final int SIZE =
            Integer.getInteger(SIZE_PROPERTY_KEY, 10);*/

    private final long time = System.currentTimeMillis();
    private final JmxMediator mediator;

    public JmxStatistics(final JmxMediator mediator) {
        this.mediator = Objects.requireNonNull(mediator);
    }

    long getTimeCreatedMillis() {
        return time;
    }

    String getSubject() {
        return mediator.getSubject();
    }

    IoStatistics getInputStats() {
        return mediator.getInputStats();
    }

    IoStatistics getOutputStats() {
        return mediator.getOutputStats();
    }

    SyncStatistics getSyncStats() {
        return mediator.getSyncStats();
    }

    @Override
    public void start() {
        register(name(), newView());
        //roll(SIZE);
    }

    private ObjectName name() {
        return mediator.nameBuilder(FsStatistics.class)
                .put("subject", getSubject())
                //.put("seqno", String.format("%08x", sequenceNumber() & 0xffff_ffffL))
                .get();
    }

    protected JmxStatisticsMXBean newView() {
        return new JmxStatisticsView(this);
    }

    /*protected void roll(final int size) {
        final int max = stats.sequenceNumber();
        final int min = max - size + 1;
        final ObjectName pattern = mediator.nameBuilder(FsStatistics.class)
                .put("subject", getSubject())
                .put("seqno", "*")
                .stats();
        for (final ObjectName name : query(pattern)) {
            final JmxStatisticsMXBean bean =
                    proxy(name, JmxStatisticsMXBean.class);
            final int seqno = bean.sequenceNumber();
            if (seqno < min || max < seqno) deregister(name);
        }
    }*/
}
