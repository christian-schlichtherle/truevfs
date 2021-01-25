/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import lombok.val;
import net.java.truevfs.comp.inst.InstrumentingSeekableChannel;
import net.java.truevfs.comp.jmx.JmxComponent;

import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

/**
 * A controller for a {@link java.nio.channels.SeekableByteChannel}.
 *
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class I5tSeekableChannel extends InstrumentingSeekableChannel<I5tMediator> implements JmxComponent {

    public I5tSeekableChannel(I5tMediator mediator, @WillCloseWhenClosed SeekableByteChannel channel) {
        super(mediator, channel);
    }

    @Override
    public void activate() {
    }

    @Override
    public int read(final ByteBuffer dst) throws IOException {
        val start = System.nanoTime();
        val ret = channel.read(dst);
        if (0 <= ret) {
            mediator.logRead(System.nanoTime() - start, ret);
        }
        return ret;
    }

    @Override
    public int write(final ByteBuffer src) throws IOException {
        val start = System.nanoTime();
        val ret = channel.write(src);
        mediator.logWrite(System.nanoTime() - start, ret);
        return ret;
    }
}
