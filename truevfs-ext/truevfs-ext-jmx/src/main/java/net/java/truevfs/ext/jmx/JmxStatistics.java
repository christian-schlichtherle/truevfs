/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import javax.annotation.concurrent.ThreadSafe;
import javax.management.ObjectName;
import static net.java.truevfs.comp.jmx.JmxUtils.*;
import net.java.truevfs.ext.jmx.stats.FsLogger;
import net.java.truevfs.ext.jmx.stats.FsStatistics;

/**
 * The JMX controller for statistics.
 * 
 * @param  <View> the type of the view to register with the platform MBean
 *         server.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class JmxStatistics<View> implements JmxColleague {

    private final JmxMediator mediator;
    private final FsLogger logger;
    private final int offset;

    public JmxStatistics(final JmxMediator mediator, final int offset) {
        logger = (this.mediator = mediator).getLogger();
        if (offset < 0 || logger.size() <= offset)
            throw new IllegalArgumentException();
        this.offset = offset;
    }

    String getSubject() {
        return mediator.getSubject();
    }

    FsStatistics getStats() {
        return logger.getStats(offset);
    }

    private ObjectName name() {
        return mediator.nameBuilder(FsStatistics.class)
                .put("subject", getSubject())
                .put("offset", logger.format(offset))
                .get();
    }

    protected abstract View newView();

    @Override
    public void start() {
        register(name(), newView());
    }
}
