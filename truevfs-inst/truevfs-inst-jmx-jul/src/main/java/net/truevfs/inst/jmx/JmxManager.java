/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.inst.jmx;

import javax.annotation.concurrent.Immutable;
import net.truevfs.inst.core.InstrumentingManager;
import net.truevfs.kernel.spec.FsManager;
import net.truevfs.kernel.spec.FsSyncException;
import net.truevfs.kernel.spec.FsSyncOption;
import net.truevfs.kernel.spec.FsSyncWarningException;
import net.truevfs.kernel.spec.util.BitField;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class JmxManager extends InstrumentingManager {

    @SuppressWarnings("LeakingThisInConstructor")
    JmxManager(JmxDirector director, FsManager model) {
        super(director, model);
        assert null != director;
        director.setApplicationIOStatistics(new JmxIoStatistics());
        director.setKernelIOStatistics(new JmxIoStatistics());
        director.setTempIOStatistics(new JmxIoStatistics());
        JmxManagerView.register(this);
    }

    /**
     * {@inheritDoc}
     * <p>
     * After the synchronization, this implementation creates a new statistics
     * object which can get accessed by calling
     * {@link JmxDirector#getKernelIOStatistics}.
     */
    @Override
    public void sync(final BitField<FsSyncOption> options)
    throws FsSyncWarningException, FsSyncException {
        try {
            manager.sync(options);
        } finally {
            JmxDirector d = ((JmxDirector) director);
            d.setApplicationIOStatistics(new JmxIoStatistics());
            d.setKernelIOStatistics(new JmxIoStatistics());
            d.setTempIOStatistics(new JmxIoStatistics());
        }
    }
}
