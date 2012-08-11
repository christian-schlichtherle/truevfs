/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import javax.annotation.concurrent.Immutable;
import net.java.truevfs.comp.inst.InstrumentingOutputSocket;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.OutputSocket;

/**
 * A controller for an {@linkplain OutputSocket output socket}.
 * 
 * @param  <E> the type of the {@linkplain #target() target entry} for I/O
 *         operations.
 * @see    JmxInputSocket
 * @author Christian Schlichtherle
 */
@Immutable
public class JmxOutputSocket<E extends Entry>
extends InstrumentingOutputSocket<JmxMediator, E>
implements JmxColleague {

    JmxOutputSocket(
            final JmxMediator director,
            final OutputSocket<? extends E> socket) {
        super(director, socket);
    }

    @Override
    public void start() {
    }
}
