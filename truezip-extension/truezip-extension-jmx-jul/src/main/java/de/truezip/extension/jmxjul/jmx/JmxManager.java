/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul.jmx;

import de.truezip.extension.jmxjul.InstrumentingManager;
import de.truezip.kernel.FsManager;
import de.truezip.kernel.FsSyncException;
import de.truezip.kernel.option.SyncOption;
import de.truezip.kernel.util.BitField;
import de.truezip.kernel.util.ExceptionHandler;
import java.io.IOException;
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
    public <X extends IOException> void
    sync(   BitField<SyncOption> options,
            ExceptionHandler<? super FsSyncException, X> handler)
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