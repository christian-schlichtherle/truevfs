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
implements JmxColleague, Provider<JmxStatistics.Kind> {
    private final JmxStatistics.Kind kind;

    JmxOutputSocket(
            final JmxMediator director,
            final OutputSocket<? extends E> socket,
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
