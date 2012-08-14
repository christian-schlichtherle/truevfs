/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import javax.annotation.concurrent.ThreadSafe;
import javax.management.ObjectName;
import net.java.truevfs.comp.jmx.JmxColleague;
import static net.java.truevfs.comp.jmx.JmxUtils.register;
import net.java.truevfs.ext.jmx.stats.FsStatistics;
import net.java.truevfs.ext.jmx.stats.SyncStatistics;

/**
 * A controller for {@linkplain FsStatistics file system statistics}.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
abstract class JmxStatistics implements JmxColleague {

    private final JmxMediator mediator;
    private final int offset;

    JmxStatistics(final JmxMediator mediator, final int offset) {
        assert 0 <= offset;
        this.mediator = mediator;
        this.offset = offset;
    }

    String getSubject() { return mediator.getSubject(); }

    FsStatistics getStats() { return mediator.getStats(offset); }

    void rotate() { mediator.rotateStats(this); }

    private ObjectName getObjectName() {
        return mediator.nameBuilder(FsStatistics.class)
                .put("subject", getSubject())
                .put("offset", mediator.formatOffset(offset))
                .get();
    }

    abstract Object newView();

    @Override
    public void start() { register(getObjectName(), newView()); }

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
