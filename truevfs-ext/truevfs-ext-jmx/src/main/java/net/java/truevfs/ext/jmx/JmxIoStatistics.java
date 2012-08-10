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

/**
 * The JMX controller for {@linkplain IoStatistics I/O statistics}.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxIoStatistics implements JmxColleague {
    /*private static final String SIZE_PROPERTY_KEY =
            JmxIoStatistics.class.getName() + ".size";
    private static final int SIZE =
            Integer.getInteger(SIZE_PROPERTY_KEY, 10);*/

    private final long time = System.currentTimeMillis();
    private final JmxMediator mediator;

    public JmxIoStatistics(final JmxMediator mediator) {
        this.mediator = Objects.requireNonNull(mediator);
    }

    long getTimeCreatedMillis() {
        return time;
    }

    String getSubject() {
        return mediator.getSubject();
    }

    IoStatistics getInputStats() {
        return mediator.getReadStats();
    }

    IoStatistics getOutputStats() {
        return mediator.getWriteStats();
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

    protected JmxIoStatisticsMXBean newView() {
        return new JmxIoStatisticsView(this);
    }

    /*protected void roll(final int size) {
        final int max = stats.sequenceNumber();
        final int min = max - size + 1;
        final ObjectName pattern = mediator.nameBuilder(FsStatistics.class)
                .put("subject", getSubject())
                .put("seqno", "*")
                .stats();
        for (final ObjectName name : query(pattern)) {
            final JmxIoStatisticsMXBean bean =
                    proxy(name, JmxIoStatisticsMXBean.class);
            final int seqno = bean.sequenceNumber();
            if (seqno < min || max < seqno) deregister(name);
        }
    }*/
}
