/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul.jmx;

import de.truezip.extension.jmxjul.InstrumentingManager;
import de.truezip.kernel.FsManager;
import de.truezip.kernel.FsSyncException;
import de.truezip.kernel.FsSyncOption;
import de.truezip.kernel.FsSyncWarningException;
import de.truezip.kernel.util.BitField;
import de.truezip.kernel.util.ExceptionHandler;
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
    public void
    sync(   final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, ? extends FsSyncException> handler)
    throws FsSyncWarningException, FsSyncException {
        try {
            manager.sync(options, handler);
        } finally {
            JmxDirector d = ((JmxDirector) director);
            d.setApplicationIOStatistics(new JmxIOStatistics());
            d.setKernelIOStatistics(new JmxIOStatistics());
            d.setTempIOStatistics(new JmxIOStatistics());
        }
    }
}