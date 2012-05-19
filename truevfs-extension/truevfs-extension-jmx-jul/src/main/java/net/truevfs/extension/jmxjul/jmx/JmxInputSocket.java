/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul.jmx;

import net.truevfs.extension.jmxjul.InstrumentingInputSocket;
import net.truevfs.kernel.cio.Entry;
import net.truevfs.kernel.cio.InputSocket;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 */
@Immutable
final class JmxInputSocket<E extends Entry>
extends InstrumentingInputSocket<E> {

    final JmxIOStatistics stats;

    JmxInputSocket(InputSocket<? extends E> model, JmxDirector director, JmxIOStatistics stats) {
        super(model, director);
        assert null != stats;
        this.stats = stats;
    }

    @Override
    public InputStream stream() throws IOException {
        return new JmxInputStream(boundSocket().stream(), stats);
    }

    @Override
    public SeekableByteChannel channel() throws IOException {
        return new JmxSeekableChannel(boundSocket().channel(), stats);
    }
}