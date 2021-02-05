/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.insight;

import global.namespace.truevfs.commons.inst.InstrumentingSeekableChannel;
import global.namespace.truevfs.commons.jmx.JmxComponent;
import lombok.val;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

/**
 * A controller for a {@link java.nio.channels.SeekableByteChannel}.
 *
 * @author Christian Schlichtherle
 */
final class I5tSeekableChannel extends InstrumentingSeekableChannel<I5tMediator> implements JmxComponent {

    public I5tSeekableChannel(I5tMediator mediator, SeekableByteChannel channel) {
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
