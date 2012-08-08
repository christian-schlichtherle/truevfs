/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import net.java.truevfs.ext.jmx.model.IoStatistics;
import net.java.truevfs.comp.jmx.JmxController;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truecommons.io.DecoratingSeekableChannel;

/**
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class JmxSeekableChannel
extends DecoratingSeekableChannel implements JmxController {
    private final IoStatistics stats;

    JmxSeekableChannel(
            @WillCloseWhenClosed SeekableByteChannel sbc,
            IoStatistics stats) {
        super(sbc);
        assert null != stats;
        this.stats = stats;
    }

    @Override
    public void init() {
    }

    @Override
    public int read(ByteBuffer buf) throws IOException {
        int ret = channel.read(buf);
        if (0 < ret)
            stats.addBytesRead(ret);
        return ret;
    }

    @Override
    public int write(ByteBuffer buf) throws IOException {
        int ret = channel.write(buf);
        stats.addBytesWritten(ret);
        return ret;
    }
}
