/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import net.java.truevfs.ext.jmx.model.IoStatistics;
import net.java.truevfs.comp.jmx.JmxController;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.comp.inst.InstrumentingInputSocket;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.InputSocket;

/**
 * @param  <E> the type of the {@linkplain #target() target entry} for I/O
 *         operations.
 * @see    JmxOutputSocket
 * @author Christian Schlichtherle
 */
@Immutable
public class JmxInputSocketController<E extends Entry>
extends InstrumentingInputSocket<JmxDirector, E>
implements JmxController, JmxStatisticsProvider {
    final IoStatistics stats;

    JmxInputSocketController(
            JmxDirector director,
            InputSocket<? extends E> socket,
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
