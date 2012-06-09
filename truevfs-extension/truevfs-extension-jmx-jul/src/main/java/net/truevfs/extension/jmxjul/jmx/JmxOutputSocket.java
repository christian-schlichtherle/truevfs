/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul.jmx;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.Immutable;
import net.truevfs.extension.jmxjul.InstrumentingOutputSocket;
import net.truevfs.kernel.cio.Entry;
import net.truevfs.kernel.cio.InputSocket;
import net.truevfs.kernel.cio.OutputSocket;

/**
 * @author  Christian Schlichtherle
 */
@Immutable
final class JmxOutputSocket<E extends Entry>
extends InstrumentingOutputSocket<E> {

    final JmxIOStatistics stats;

    JmxOutputSocket(JmxDirector director, OutputSocket<? extends E> model, JmxIOStatistics stats) {
        super(director, model);
        assert null != stats;
        this.stats = stats;
    }

    @Override
    public SeekableByteChannel channel(InputSocket<? extends Entry> peer)
    throws IOException {
        return new JmxSeekableChannel(socket().channel(peer), stats);
    }

    @Override
    public OutputStream stream(InputSocket<? extends Entry> peer)
    throws IOException {
        return new JmxOutputStream(socket().stream(peer), stats);
    }
}
