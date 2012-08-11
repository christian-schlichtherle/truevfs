/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

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

    private final JmxMediator mediator;
    private final int offset;

    public JmxIoStatistics(final JmxMediator mediator, final int offset) {
        if (offset < 0 || mediator.getLoggerSize() <= offset)
            throw new IllegalArgumentException();
        this.mediator = mediator;
        this.offset = offset;
    }

    String getSubject() {
        return mediator.getSubject();
    }

    FsStatistics getStats() {
        return mediator.getIoStats(offset);
    }

    private ObjectName name() {
        return mediator.nameBuilder(FsStatistics.class)
                .put("subject", getSubject())
                .put("offset", mediator.formatLoggerOffset(offset))
                .get();
    }

    protected JmxIoStatisticsMXBean newView() {
        return new JmxIoStatisticsView(this);
    }

    @Override
    public void start() {
        register(name(), newView());
    }
}
