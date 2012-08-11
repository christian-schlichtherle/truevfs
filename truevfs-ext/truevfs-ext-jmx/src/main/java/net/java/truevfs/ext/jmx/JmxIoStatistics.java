/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.ext.jmx.stats.IoStatistics;

/**
 * A controller for {@linkplain IoStatistics I/O statistics}.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxIoStatistics extends JmxStatistics<JmxIoStatisticsMXBean> {

    public JmxIoStatistics(JmxMediator mediator, int offset) {
        super(mediator, offset);
    }

    @Override
    protected JmxIoStatisticsMXBean newView() {
        return new JmxIoStatisticsView(this);
    }
}
