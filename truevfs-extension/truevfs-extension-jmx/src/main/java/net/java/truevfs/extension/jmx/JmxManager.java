/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.jmx;

import javax.annotation.concurrent.Immutable;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.component.instrumentation.InstrumentingManager;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsSyncException;
import net.java.truevfs.kernel.spec.FsSyncOption;
import net.java.truevfs.kernel.spec.FsSyncWarningException;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class JmxManager extends InstrumentingManager<JmxDirector> {

    JmxManager(JmxDirector director, FsManager manager) {
        super(director, manager);
        assert null != director;
        director.setAppStatistics(new JmxIoStatistics());
        director.setKernelStatistics(new JmxIoStatistics());
        director.setBufferStatistics(new JmxIoStatistics());
        JmxManagerView.register(manager);
    }

    /**
     * {@inheritDoc}
     * <p>
     * After the synchronization, this implementation creates a new statistics
     * object which can get accessed by calling
     * {@link JmxDirector#getKernelStatistics}.
     */
    @Override
    public void sync(final BitField<FsSyncOption> options)
    throws FsSyncWarningException, FsSyncException {
        try {
            manager.sync(options);
        } finally {
            final JmxDirector d = director;
            d.setAppStatistics(new JmxIoStatistics());
            d.setKernelStatistics(new JmxIoStatistics());
            d.setBufferStatistics(new JmxIoStatistics());
        }
    }
}
