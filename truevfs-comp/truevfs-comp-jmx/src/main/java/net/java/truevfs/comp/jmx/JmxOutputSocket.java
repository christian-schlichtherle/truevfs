/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx;

import javax.annotation.concurrent.Immutable;
import net.java.truevfs.comp.inst.InstrumentingOutputSocket;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.OutputSocket;

/**
 * A controller for an {@linkplain OutputSocket output socket}.
 * 
 * @param  <M> the type of the JMX mediator.
 * @param  <E> the type of the {@linkplain #target() target entry} for I/O
 *         operations.
 * @see    JmxInputSocket
 * @author Christian Schlichtherle
 */
@Immutable
public class JmxOutputSocket<M extends JmxMediator<M>, E extends Entry>
extends InstrumentingOutputSocket<M, E> implements JmxColleague {

    public JmxOutputSocket(M director, OutputSocket<? extends E> socket) {
        super(director, socket);
    }

    @Override
    public void start() { }
}
