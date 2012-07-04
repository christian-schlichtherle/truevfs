/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst.jmx;

import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.FsSyncOption;
import de.schlichtherle.truezip.fs.inst.InstrumentingManager;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import java.io.IOException;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 */
@Immutable
final class JmxManager extends InstrumentingManager {

    @SuppressWarnings("LeakingThisInConstructor")
    JmxManager(FsManager manager, JmxDirector director) {
        super(manager, director);
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
    public <X extends IOException> void
    sync(   BitField<FsSyncOption> options,
            ExceptionHandler<? super IOException, X> handler)
    throws X {
        try {
            delegate.sync(options, handler);
        } finally {
            JmxDirector d = ((JmxDirector) director);
            d.setApplicationIOStatistics(new JmxIOStatistics());
            d.setKernelIOStatistics(new JmxIOStatistics());
            d.setTempIOStatistics(new JmxIOStatistics());
        }
    }
}
