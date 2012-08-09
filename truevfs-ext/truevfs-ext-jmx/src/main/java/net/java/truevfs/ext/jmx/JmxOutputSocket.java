/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import javax.annotation.concurrent.Immutable;
import javax.inject.Provider;
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
public class JmxOutputSocket<E extends Entry>
extends InstrumentingOutputSocket<JmxMediator, E>
implements JmxColleague, Provider<JmxStatisticsKind> {
    private final JmxStatisticsKind kind;

    JmxOutputSocket(
            JmxMediator director,
            OutputSocket<? extends E> socket,
            JmxStatisticsKind kind) {
        super(director, socket);
        assert null != kind;
        this.kind = kind;
    }

    @Override
    public void start() {
    }

    @Override
    public JmxStatisticsKind get() {
        return kind;
    }
}