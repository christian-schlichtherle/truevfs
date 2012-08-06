/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.jmx;

import javax.annotation.concurrent.Immutable;
import net.java.truevfs.component.instrumentation.InstrumentingInputSocket;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.InputSocket;

/**
 * @see    JmxOutputSocket
 * @author Christian Schlichtherle
 */
@Immutable
final class JmxInputSocket<E extends Entry>
extends InstrumentingInputSocket<JmxDirector, E>
implements JmxIoStatisticsProvider {
    final JmxIoStatistics stats;

    JmxInputSocket(
            JmxDirector director,
            InputSocket<? extends E> socket,
            JmxIoStatistics stats) {
        super(director, socket);
        assert null != stats;
        this.stats = stats;
    }

    @Override
    public JmxIoStatistics getStatistics() {
        return stats;
    }
}
