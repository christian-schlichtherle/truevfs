/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.ext.jmx;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.Immutable;
import net.truevfs.comp.inst.InstrumentingOutputSocket;
import net.truevfs.kernel.spec.cio.Entry;
import net.truevfs.kernel.spec.cio.InputSocket;
import net.truevfs.kernel.spec.cio.OutputSocket;

/**
 * @author  Christian Schlichtherle
 */
@Immutable
final class JmxOutputSocket<E extends Entry>
extends InstrumentingOutputSocket<E> {

    final JmxIoStatistics stats;

    JmxOutputSocket(JmxDirector director, OutputSocket<? extends E> model, JmxIoStatistics stats) {
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
