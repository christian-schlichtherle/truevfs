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
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class JmxSeekableChannel
extends InstrumentingSeekableChannel<JmxMediator> implements JmxColleague {
    private final IoLogger logger;

    JmxSeekableChannel(
            final JmxMediator mediator,
            final @WillCloseWhenClosed SeekableByteChannel channel,
            final JmxStatistics.Kind kind) {
        super(mediator, channel);
        this.logger = mediator.logger(kind);
    }

    @Override
    public void start() {
    }

    @Override
    public int read(ByteBuffer buf) throws IOException {
        final long start = System.nanoTime();
        int ret = channel.read(buf);
        if (0 < ret) logger.logRead(ret, System.nanoTime() - start);
        return ret;
    }

    @Override
    public int write(ByteBuffer buf) throws IOException {
        final long start = System.nanoTime();
        int ret = channel.write(buf);
        logger.logWrite(ret, System.nanoTime() - start);
        return ret;
    }
}
