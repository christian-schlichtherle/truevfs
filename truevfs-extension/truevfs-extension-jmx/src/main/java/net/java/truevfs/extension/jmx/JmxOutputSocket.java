/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.jmx;

import javax.annotation.concurrent.Immutable;
import net.java.truevfs.component.instrumentation.InstrumentingOutputSocket;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.OutputSocket;

/**
 * @see    JmxInputSocket
 * @author Christian Schlichtherle
 */
@Immutable
final class JmxOutputSocket<E extends Entry>
extends InstrumentingOutputSocket<JmxDirector, E> implements WithIoStatistics {
    final JmxIoStatistics stats;

    JmxOutputSocket(
            JmxDirector director,
            OutputSocket<? extends E> socket,
            JmxIoStatistics stats) {
        super(director, socket);
        assert null != stats;
        this.stats = stats;
    }

    @Override
    public JmxIoStatistics getStats() {
        return stats;
    }
}
