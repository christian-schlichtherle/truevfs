/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul.jmx;

import net.truevfs.kernel.io.DecoratingSeekableChannel;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author  Christian Schlichtherle
 */
@NotThreadSafe
final class JmxSeekableChannel extends DecoratingSeekableChannel {
    private final JmxIOStatistics stats;

    @CreatesObligation
    JmxSeekableChannel(@WillCloseWhenClosed SeekableByteChannel sbc, JmxIOStatistics stats) {
        super(sbc);
        assert null != stats;
        this.stats = stats;
    }

    @Override
    public int read(ByteBuffer buf) throws IOException {
        int ret = channel.read(buf);
        if (0 < ret)
            stats.incBytesRead(ret);
        return ret;
    }

    @Override
    public int write(ByteBuffer buf) throws IOException {
        int ret = channel.write(buf);
        stats.incBytesWritten(ret);
        return ret;
    }
}