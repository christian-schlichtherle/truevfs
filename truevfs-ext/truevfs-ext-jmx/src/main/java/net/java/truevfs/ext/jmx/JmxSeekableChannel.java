/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truevfs.comp.inst.InstrumentingSeekableChannel;
import net.java.truevfs.ext.jmx.model.IoLogger;

/**
 * The MXBean controller for a
 * {@linkplain SeekableByteChannel seekable byte channel}.
 * 
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class JmxSeekableChannel
extends InstrumentingSeekableChannel<JmxMediator> implements JmxColleague {
    private IoLogger logger;

    JmxSeekableChannel(
            final JmxMediator mediator,
            final @WillCloseWhenClosed SeekableByteChannel channel) {
        super(mediator, channel);
        this.logger = mediator.getLogger();
    }

    @Override
    public void start() {
    }

    @Override
    public int read(ByteBuffer buf) throws IOException {
        final long start = System.nanoTime();
        final int ret = channel.read(buf);
        if (0 <= ret)
            for (final long time = System.nanoTime() - start;
                    !logger.tryLogRead(ret, time); )
                logger = mediator.nextLogger();
        return ret;
    }

    @Override
    public int write(ByteBuffer buf) throws IOException {
        final long start = System.nanoTime();
        final int ret = channel.write(buf);
        for (final long time = System.nanoTime() - start;
                logger.tryLogWrite(ret, time); )
            logger = mediator.nextLogger();
        return ret;
    }
}
