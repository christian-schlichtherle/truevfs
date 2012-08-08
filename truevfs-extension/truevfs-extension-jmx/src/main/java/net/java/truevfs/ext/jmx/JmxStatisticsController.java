/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import java.util.Date;
import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.ObjectName;
import net.java.truevfs.comp.jmx.JmxController;
import static net.java.truevfs.comp.jmx.JmxUtils.deregister;
import static net.java.truevfs.comp.jmx.JmxUtils.register;
import net.java.truevfs.ext.jmx.model.IoStatistics;

/**
 * Controls JMX I/O statistics.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxStatisticsController implements JmxController {
    private final JmxDirector director;
    private final IoStatistics stats;

    public JmxStatisticsController(
            final JmxDirector director,
            final IoStatistics stats) {
        this.director = Objects.requireNonNull(director);
        this.stats = Objects.requireNonNull(stats);
    }

    @Override
    public void init() {
        register(newView(), name());
    }

    protected JmxStatisticsMXBean newView() {
        return new JmxStatisticsView(this);
    }

    private ObjectName name() {
        return director.nameBuilder(IoStatistics.class)
                .put("kind", getKind())
                .put("time", ObjectName.quote(getTimeCreated()))
                .name();
    }

    String getKind() {
        return stats.getKind();
    }

    String getTimeCreated() {
        return new Date(getTimeCreatedMillis()).toString();
    }

    long getTimeCreatedMillis() {
        return stats.getTimeCreatedMillis();
    }

    long getBytesRead() {
        return stats.getBytesRead();
    }

    long getBytesWritten() {
        return stats.getBytesWritten();
    }

    void close() {
        deregister(name());
    }
}
