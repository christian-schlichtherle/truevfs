/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx;

import javax.annotation.concurrent.Immutable;
import net.java.truevfs.comp.inst.InstrumentingInputSocket;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.InputSocket;

/**
 * A controller for an {@linkplain InputSocket input socket}.
 * 
 * @param  <M> the type of the JMX mediator.
 * @param  <E> the type of the {@linkplain #target() target entry} for I/O
 *         operations.
 * @see    JmxOutputSocket
 * @author Christian Schlichtherle
 */
@Immutable
public class JmxInputSocket<M extends JmxMediator<M>, E extends Entry>
extends InstrumentingInputSocket<M, E> implements JmxColleague {

    public JmxInputSocket(M director, InputSocket<? extends E> socket) {
        super(director, socket);
    }

    @Override
    public void start() { }
}
