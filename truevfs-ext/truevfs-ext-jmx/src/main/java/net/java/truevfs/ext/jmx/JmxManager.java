/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import javax.annotation.concurrent.ThreadSafe;
import javax.management.ObjectName;
import net.java.truevfs.comp.inst.InstrumentingManager;
import net.java.truevfs.comp.jmx.JmxManagerMXBean;
import static net.java.truevfs.comp.jmx.JmxUtils.*;
import net.java.truevfs.ext.jmx.model.IoLogger;
import net.java.truevfs.ext.jmx.model.IoStatistics;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsSyncException;
import net.java.truevfs.kernel.spec.FsSyncOptions;
import net.java.truevfs.kernel.spec.sl.FsManagerLocator;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxManager
extends InstrumentingManager<JmxMediator> implements JmxColleague {
    public JmxManager(JmxMediator mediator, FsManager manager) {
        super(mediator, manager);
    }

    @Override
    public void start() {
        register(newView(), name());
        mediator.nextLoggers();
    }

    protected JmxManagerMXBean newView() {
        return new JmxManagerView(this);
    }

    private ObjectName name() {
        return mediator.nameBuilder(FsManager.class).get();
    }

    void sync() throws FsSyncException {
        FsManagerLocator.SINGLETON.get().sync(FsSyncOptions.NONE);
        mediator.nextLoggers();
    }

    void clearStatistics() {
        for (final JmxStatisticsKind kind : JmxStatisticsKind.values()) {
            final IoLogger logger = mediator.getLogger(kind);
            final int keep = logger.getSequenceNumber();
            final ObjectName pattern = mediator.nameBuilder(IoStatistics.class)
                    .put("kind", kind.toString())
                    .put("seqno", "*")
                    .get();
            for (final ObjectName found : query(pattern)) {
                final JmxStatisticsMXBean proxy =
                        proxy(found, JmxStatisticsMXBean.class);
                if (proxy.getSequenceNumber() != keep) deregister(found);
            }
        }
    }
}