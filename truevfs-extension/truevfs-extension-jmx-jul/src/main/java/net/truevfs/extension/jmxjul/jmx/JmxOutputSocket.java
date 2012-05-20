/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul.jmx;

import net.truevfs.extension.jmxjul.InstrumentingOutputSocket;
import net.truevfs.kernel.cio.Entry;
import net.truevfs.kernel.cio.OutputSocket;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 */
@Immutable
final class JmxOutputSocket<E extends Entry>
extends InstrumentingOutputSocket<E> {

    final JmxIOStatistics stats;

    JmxOutputSocket(OutputSocket<? extends E> model, JmxDirector director, JmxIOStatistics stats) {
        super(model, director);
        assert null != stats;
        this.stats = stats;
    }

    @Override
    public SeekableByteChannel channel() throws IOException {
        return new JmxSeekableChannel(boundSocket().channel(), stats);
    }

    @Override
    public OutputStream stream() throws IOException {
        return new JmxOutputStream(boundSocket().stream(), stats);
    }
}