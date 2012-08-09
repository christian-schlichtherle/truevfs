/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import javax.annotation.concurrent.Immutable;
import javax.inject.Provider;
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
public class JmxInputSocket<E extends Entry>
extends InstrumentingInputSocket<JmxMediator, E>
implements JmxColleague, Provider<JmxStatistics.Kind> {
    private final JmxStatistics.Kind kind;

    JmxInputSocket(
            final JmxMediator director,
            final InputSocket<? extends E> socket,
            final JmxStatistics.Kind kind) {
        super(director, socket);
        assert null != kind;
        this.kind = kind;
    }

    @Override
    public void start() {
    }

    @Override
    public JmxStatistics.Kind get() {
        return kind;
    }
}
