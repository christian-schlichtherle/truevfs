/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.jmx;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.component.instrumentation.InstrumentingInputSocket;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.InputSocket;
import net.java.truevfs.kernel.spec.cio.OutputSocket;

/**
 * @author  Christian Schlichtherle
 */
@Immutable
final class JmxInputSocket<E extends Entry>
extends InstrumentingInputSocket<JmxDirector, E> {

    final JmxIoStatistics stats;

    JmxInputSocket(JmxDirector director, InputSocket<? extends E> model, JmxIoStatistics stats) {
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