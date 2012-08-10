/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.ObjectName;
import static net.java.truevfs.comp.jmx.JmxUtils.*;
import net.java.truevfs.ext.jmx.model.IoLogger;
import net.java.truevfs.ext.jmx.model.IoStatistics;

/**
 * Controls JMX I/O statistics.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxStatistics implements JmxColleague {
    private static final String MAX_STATISTICS_PROPERTY_KEY =
            JmxStatistics.class.getName() + ".maxStatistics";
    private static final int MAX_STATISTICS =
            Integer.getInteger(MAX_STATISTICS_PROPERTY_KEY, 10);

    private final JmxMediator mediator;
    private final IoLogger logger;

    public JmxStatistics(
            final JmxMediator mediator,
            final IoLogger logger) {
        this.mediator = Objects.requireNonNull(mediator);
        this.logger = Objects.requireNonNull(logger);
    }

    @Override
    public void start() {
        register(newView(), name());
        rotate(MAX_STATISTICS);
    }

    protected void rotate(final int max) {
        final IoLogger logger = mediator.getLogger();
        final int maxDiscard = logger.getSequenceNumber() - max;
        final ObjectName pattern = mediator.nameBuilder(IoStatistics.class)
                .put("subject", getSubject())
                .put("seqno", "*")
                .get();
        for (final ObjectName name : query(pattern)) {
            final JmxStatisticsMXBean proxy =
                    proxy(name, JmxStatisticsMXBean.class);
            if (proxy.getSequenceNumber() <= maxDiscard) deregister(name);
        }
    }

    protected JmxStatisticsMXBean newView() {
        return new JmxStatisticsView(this);
    }

    private ObjectName name() {
        return mediator.nameBuilder(IoStatistics.class)
                .put("subject", getSubject())
                .put("seqno", Integer.toString(getSequenceNumber()))
                .get();
    }

    String getSubject() {
        return mediator.getSubject();
    }

    int getSequenceNumber() {
        return logger.getSequenceNumber();
    }

    long getTimeCreatedMillis() {
        return logger.getTimeCreatedMillis();
    }

    IoStatistics getReadStats() {
        return logger.getReadStats();
    }

    IoStatistics getWriteStats() {
        return logger.getWriteStats();
    }

    void close() {
        deregister(name());
    }
}
