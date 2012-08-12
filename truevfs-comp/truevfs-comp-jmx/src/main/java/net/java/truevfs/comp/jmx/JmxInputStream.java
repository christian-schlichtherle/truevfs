/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx;

import java.io.InputStream;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truevfs.comp.inst.InstrumentingInputStream;

/**
 * A controller for an {@linkplain InputStream input stream}.
 * 
 * @param  <M> the type of the JMX mediator.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class JmxInputStream<M extends JmxMediator<M>>
extends InstrumentingInputStream<M> implements JmxColleague {

    public JmxInputStream(M mediator, @WillCloseWhenClosed InputStream in) {
        super(mediator, in);
    }

    @Override
    public void start() { }
}
