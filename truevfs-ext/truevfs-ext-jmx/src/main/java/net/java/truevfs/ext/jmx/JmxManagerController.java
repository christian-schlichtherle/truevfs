/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import net.java.truevfs.ext.jmx.model.IoStatistics;
import net.java.truevfs.comp.jmx.JmxController;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.ObjectName;
import net.java.truevfs.comp.inst.InstrumentingManager;
import net.java.truevfs.comp.jmx.JmxManagerMXBean;
import static net.java.truevfs.comp.jmx.JmxUtils.*;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsSyncException;
import net.java.truevfs.kernel.spec.FsSyncOptions;
import net.java.truevfs.kernel.spec.sl.FsManagerLocator;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxManagerController
extends InstrumentingManager<JmxDirector> implements JmxController {
    private static final String APPLICATION = "Application I/O Statistics";
    private static final String KERNEL = "Kernel I/O Statistics";
    private static final String BUFFER = "Buffer I/O Statistics";

    public JmxManagerController(JmxDirector director, FsManager manager) {
        super(director, manager);
    }

    @Override
    public void init() {
        final JmxDirector d = director;
        d.setApplicationStatistics(new IoStatistics(APPLICATION));
        d.setKernelStatistics(new IoStatistics(KERNEL));
        d.setBufferStatistics(new IoStatistics(BUFFER));
        register(newView(), name());
    }

    protected JmxManagerMXBean newView() {
        return new JmxManagerView(this);
    }

    private ObjectName name() {
        return director.nameBuilder(FsManager.class).name();
    }

    void sync() throws FsSyncException {
        FsManagerLocator.SINGLETON.get().sync(FsSyncOptions.NONE);
        init();
    }

    void clearStatistics() {
        for (final IoStatistics stats : new IoStatistics[] {
            director.getApplicationStatistics(),
            director.getKernelStatistics(),
            director.getBufferStatistics(),
        }) {
            final ObjectName pattern = director.nameBuilder(IoStatistics.class)
                    .put("kind", stats.getKind())
                    .put("time", "*")
                    .name();
            for (final ObjectName found : query(pattern)) {
                final JmxStatisticsMXBean proxy =
                        proxy(found, JmxStatisticsMXBean.class);
                if (stats.getTimeCreatedMillis()
                        != proxy.getTimeCreatedMillis())
                    deregister(found);
            }
        }
    }
}
