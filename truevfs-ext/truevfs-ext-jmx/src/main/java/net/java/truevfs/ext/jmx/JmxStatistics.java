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
 * The combined JMX controller for an {@linkplain IoLogger I/O logger}
 * and its {@linkplain IoStatistics I/O statistics}.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxStatistics implements JmxColleague {
    private static final String SIZE_PROPERTY_KEY =
            JmxStatistics.class.getName() + ".size";
    private static final int SIZE =
            Integer.getInteger(SIZE_PROPERTY_KEY, 10);

    private final JmxMediator mediator;
    private final IoLogger logger;

    public JmxStatistics(
            final JmxMediator mediator,
            final IoLogger logger) {
        this.mediator = Objects.requireNonNull(mediator);
        this.logger = Objects.requireNonNull(logger);
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

    @Override
    public void start() {
        register(newView(), name());
        roll(SIZE);
    }

    protected JmxStatisticsMXBean newView() {
        return new JmxStatisticsView(this);
    }

    private ObjectName name() {
        return mediator.nameBuilder(IoStatistics.class)
                .put("subject", getSubject())
                .put("seqno", String.format("%08x", getSequenceNumber() & 0xffff_ffffL))
                .get();
    }

    protected void roll(final int size) {
        final int max = logger.getSequenceNumber();
        final int min = max - size + 1;
        final ObjectName pattern = mediator.nameBuilder(IoStatistics.class)
                .put("subject", getSubject())
                .put("seqno", "*")
                .get();
        for (final ObjectName name : query(pattern)) {
            final JmxStatisticsMXBean bean =
                    proxy(name, JmxStatisticsMXBean.class);
            final int seqno = bean.getSequenceNumber();
            if (seqno < min || max < seqno) deregister(name);
        }
    }
}
