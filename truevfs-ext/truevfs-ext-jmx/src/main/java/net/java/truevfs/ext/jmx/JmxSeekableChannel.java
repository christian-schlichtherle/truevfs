/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import net.java.truevfs.comp.jmx.JmxColleague;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truevfs.comp.inst.InstrumentingSeekableChannel;

/**
 * A controller for a
 * {@linkplain SeekableByteChannel seekable byte channel}.
 * 
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class JmxSeekableChannel
extends InstrumentingSeekableChannel<JmxMediator> implements JmxColleague {

    JmxSeekableChannel(
            JmxMediator mediator,
            @WillCloseWhenClosed SeekableByteChannel channel) {
        super(mediator, channel);
    }

    @Override
    public void start() { }

    @Override
    public int read(ByteBuffer buf) throws IOException {
        final long start = System.nanoTime();
        final int ret = channel.read(buf);
        if (0 <= ret) mediator.logRead(System.nanoTime() - start, ret);
        return ret;
    }

    @Override
    public int write(ByteBuffer buf) throws IOException {
        final long start = System.nanoTime();
        final int ret = channel.write(buf);
        mediator.logWrite(System.nanoTime() - start, ret);
        return ret;
    }
}
