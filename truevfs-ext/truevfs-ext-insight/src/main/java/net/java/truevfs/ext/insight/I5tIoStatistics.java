/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.ext.insight.stats.IoStatistics;

/**
 * A controller for {@linkplain IoStatistics I/O statistics}.
 * 
 * @see    JmxSyncStatistics
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class I5tIoStatistics extends I5tStatistics {

    I5tIoStatistics(I5tMediator mediator, int offset) {
        super(mediator, offset);
    }

    @Override
    I5tStatisticsView newView() { return new I5tIoStatisticsView(this); }
}
