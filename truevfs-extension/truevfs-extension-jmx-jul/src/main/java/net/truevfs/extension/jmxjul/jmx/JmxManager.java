/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul.jmx;

import net.truevfs.extension.jmxjul.InstrumentingManager;
import net.truevfs.kernel.FsManager;
import net.truevfs.kernel.FsSyncException;
import net.truevfs.kernel.FsSyncOption;
import net.truevfs.kernel.FsSyncWarningException;
import net.truevfs.kernel.util.BitField;
import javax.annotation.concurrent.Immutable;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class JmxManager extends InstrumentingManager {

    @SuppressWarnings("LeakingThisInConstructor")
    JmxManager(FsManager model, JmxDirector director) {
        super(model, director);
        assert null != director;
        director.setApplicationIOStatistics(new JmxIOStatistics());
        director.setKernelIOStatistics(new JmxIOStatistics());
        director.setTempIOStatistics(new JmxIOStatistics());
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
            d.setApplicationIOStatistics(new JmxIOStatistics());
            d.setKernelIOStatistics(new JmxIOStatistics());
            d.setTempIOStatistics(new JmxIOStatistics());
        }
    }
}
