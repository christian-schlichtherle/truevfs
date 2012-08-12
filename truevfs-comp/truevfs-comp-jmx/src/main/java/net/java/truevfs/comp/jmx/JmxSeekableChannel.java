/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx;

import java.nio.channels.SeekableByteChannel;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truevfs.comp.inst.InstrumentingSeekableChannel;

/**
 * A controller for a
 * {@linkplain SeekableByteChannel seekable byte channel}.
 * 
 * @param  <M> the type of the JMX mediator.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class JmxSeekableChannel<M extends JmxMediator<M>>
extends InstrumentingSeekableChannel<M> implements JmxColleague {

    public JmxSeekableChannel(M mediator, @WillCloseWhenClosed SeekableByteChannel channel) {
        super(mediator, channel);
    }

    @Override
    public void start() { }
}
