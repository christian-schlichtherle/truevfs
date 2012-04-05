/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul.jmx;

import de.truezip.extension.jmxjul.InstrumentingOutputSocket;
import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.cio.OutputSocket;
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
    public SeekableByteChannel newChannel() throws IOException {
        return new JmxSeekableChannel(getBoundSocket().newChannel(), stats);
    }

    @Override
    public OutputStream newStream() throws IOException {
        return new JmxOutputStream(getBoundSocket().newStream(), stats);
    }
}