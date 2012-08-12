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
 * @see    JmxSyncStatistics
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class JmxIoStatistics extends JmxStatistics {

    JmxIoStatistics(JmxMediator mediator, int offset) {
        super(mediator, offset);
    }

    @Override
    Object newView() { return new JmxIoStatisticsView(this); }
}
