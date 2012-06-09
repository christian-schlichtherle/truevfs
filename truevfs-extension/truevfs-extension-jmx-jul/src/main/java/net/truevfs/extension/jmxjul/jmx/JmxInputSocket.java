/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul.jmx;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.Immutable;
import net.truevfs.extension.jmxjul.InstrumentingInputSocket;
import net.truevfs.kernel.cio.Entry;
import net.truevfs.kernel.cio.InputSocket;
import net.truevfs.kernel.cio.OutputSocket;

/**
 * @author  Christian Schlichtherle
 */
@Immutable
final class JmxInputSocket<E extends Entry>
extends InstrumentingInputSocket<E> {

    final JmxIOStatistics stats;

    JmxInputSocket(JmxDirector director, InputSocket<? extends E> model, JmxIOStatistics stats) {
        super(director, model);
        assert null != stats;
        this.stats = stats;
    }

    @Override
    public InputStream stream(OutputSocket<? extends Entry> peer)
    throws IOException {
        return new JmxInputStream(socket().stream(peer), stats);
    }

    @Override
    public SeekableByteChannel channel(OutputSocket<? extends Entry> peer)
    throws IOException {
        return new JmxSeekableChannel(socket().channel(peer), stats);
    }
}