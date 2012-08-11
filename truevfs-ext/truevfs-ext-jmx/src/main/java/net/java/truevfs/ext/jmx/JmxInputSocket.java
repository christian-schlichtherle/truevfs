/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import javax.annotation.concurrent.Immutable;
import net.java.truevfs.comp.inst.InstrumentingInputSocket;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.InputSocket;

/**
 * A controller for an {@linkplain InputSocket input socket}.
 * 
 * @param  <E> the type of the {@linkplain #target() target entry} for I/O
 *         operations.
 * @see    JmxOutputSocket
 * @author Christian Schlichtherle
 */
@Immutable
public class JmxInputSocket<E extends Entry>
extends InstrumentingInputSocket<JmxMediator, E> implements JmxColleague {

    JmxInputSocket(
            JmxMediator director,
            InputSocket<? extends E> socket) {
        super(director, socket);
    }

    @Override
    public void start() { }
}
