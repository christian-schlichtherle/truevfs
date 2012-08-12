/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx;

import java.io.OutputStream;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truevfs.comp.inst.InstrumentingOutputStream;

/**
 * A controller for an {@linkplain OutputStream output stream}.
 * 
 * @param  <M> the type of the JMX mediator.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class JmxOutputStream<M extends JmxMediator<M>>
extends InstrumentingOutputStream<M> implements JmxColleague {

    public JmxOutputStream(M mediator, @WillCloseWhenClosed OutputStream out) {
        super(mediator, out);
    }

    @Override
    public void start() { }
}
