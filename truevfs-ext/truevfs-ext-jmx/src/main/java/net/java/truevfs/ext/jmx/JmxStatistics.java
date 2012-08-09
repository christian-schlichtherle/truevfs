/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.ObjectName;
import static net.java.truevfs.comp.jmx.JmxUtils.deregister;
import static net.java.truevfs.comp.jmx.JmxUtils.register;
import net.java.truevfs.ext.jmx.model.IoLogger;
import net.java.truevfs.ext.jmx.model.IoStatistics;

/**
 * Controls JMX I/O statistics.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxStatistics implements JmxColleague {
    private final JmxMediator mediator;
    private final Kind kind;
    private final IoLogger logger;

    public JmxStatistics(
            final JmxMediator mediator,
            final Kind kind,
            final IoLogger logger) {
        this.mediator = Objects.requireNonNull(mediator);
        this.logger = Objects.requireNonNull(logger);
        this.kind = Objects.requireNonNull(kind);
    }

    @Override
    public void start() {
        register(newView(), name());
    }

    protected JmxStatisticsMXBean newView() {
        return new JmxStatisticsView(this);
    }

    private ObjectName name() {
        return mediator.nameBuilder(IoStatistics.class)
                .put("kind", getKindString())
                .put("seqno", getSequenceNumberString())
                .get();
    }

    Kind getKind() {
        return kind;
    }

    String getKindString() {
        return kind.toString();
    }

    int getSequenceNumber() {
        return logger.getSequenceNumber();
    }

    String getSequenceNumberString() {
        return Integer.toString(getSequenceNumber());
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

    @SuppressWarnings("PublicInnerClass")
    public enum Kind {
        APPLICATION {
            @Override 
            public String toString() { return "Application I/O Statistics"; }
        },
        KERNEL {
            @Override 
            public String toString() { return "Kernel I/O Statistics"; }
        },
        BUFFER {
            @Override 
            public String toString() { return "Buffer I/O Statistics"; }
        };
    }
}
