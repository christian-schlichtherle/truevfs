/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import net.java.truevfs.ext.jmx.model.IoStatistics;
import net.java.truevfs.comp.jmx.JmxController;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.comp.inst.InstrumentingOutputSocket;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.OutputSocket;

/**
 * @param  <E> the type of the {@linkplain #target() target entry} for I/O
 *         operations.
 * @see    JmxInputSocket
 * @author Christian Schlichtherle
 */
@Immutable
public class JmxOutputSocketController<E extends Entry>
extends InstrumentingOutputSocket<JmxDirector, E>
implements JmxController, JmxStatisticsProvider {
    final IoStatistics stats;

    JmxOutputSocketController(
            JmxDirector director,
            OutputSocket<? extends E> socket,
            IoStatistics stats) {
        super(director, socket);
        assert null != stats;
        this.stats = stats;
    }

    @Override
    public void init() {
    }

    @Override
    public IoStatistics getStatistics() {
        return stats;
    }
}
