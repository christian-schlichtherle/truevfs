/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import javax.annotation.concurrent.ThreadSafe;
import javax.management.ObjectName;
import static net.java.truevfs.comp.jmx.JmxUtils.*;
import net.java.truevfs.ext.jmx.stats.FsStatistics;

/**
 * A controller for statistics.
 * 
 * @param  <View> the type of the view to register with the platform MBean
 *         server.
 * @author Christian Schlichtherle
 */
@ThreadSafe
abstract class JmxStatistics<View> implements JmxColleague {

    private final JmxMediator mediator;
    private final int offset;

    JmxStatistics(final JmxMediator mediator, final int offset) {
        assert 0 <= offset;
        this.mediator = mediator;
        this.offset = offset;
    }

    String getSubject() { return mediator.getSubject(); }

    FsStatistics getStats() { return mediator.stats(offset); }

    private ObjectName name() {
        return mediator.nameBuilder(FsStatistics.class)
                .put("subject", getSubject())
                .put("offset", mediator.formatOffset(offset))
                .get();
    }

    abstract View newView();

    @Override
    public void start() { register(name(), newView()); }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[subject=%s, offset=%d, mediator=%s]",
                getClass().getName(), getSubject(), offset, mediator);
    }
}
