/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.jmx;

import javax.annotation.concurrent.Immutable;
import net.java.truevfs.component.instrumentation.InstrumentingManager;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsSyncException;
import net.java.truevfs.kernel.spec.FsSyncOption;
import net.java.truevfs.kernel.spec.FsSyncWarningException;
import net.java.truecommons.shed.BitField;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class JmxManager extends InstrumentingManager {

    @SuppressWarnings("LeakingThisInConstructor")
    JmxManager(JmxDirector director, FsManager manager) {
        super(director, manager);
        assert null != director;
        director.setApplicationIoStatistics(new JmxIoStatistics());
        director.setKernelIoStatistics(new JmxIoStatistics());
        director.setTempIoStatistics(new JmxIoStatistics());
        JmxManagerView.register(this);
    }

    /**
     * {@inheritDoc}
     * <p>
     * After the synchronization, this implementation creates a new statistics
     * object which can get accessed by calling
     * {@link JmxDirector#getKernelIoStatistics}.
     */
    @Override
    public void sync(final BitField<FsSyncOption> options)
    throws FsSyncWarningException, FsSyncException {
        try {
            manager.sync(options);
        } finally {
            JmxDirector d = ((JmxDirector) director);
            d.setApplicationIoStatistics(new JmxIoStatistics());
            d.setKernelIoStatistics(new JmxIoStatistics());
            d.setTempIoStatistics(new JmxIoStatistics());
        }
    }
}
