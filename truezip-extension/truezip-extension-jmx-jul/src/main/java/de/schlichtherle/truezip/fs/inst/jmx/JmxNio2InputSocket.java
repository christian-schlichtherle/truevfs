/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst.jmx;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.socket.InputSocket;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 */
@Immutable
final class JmxNio2InputSocket<E extends Entry>
extends JmxInputSocket<E> {

    JmxNio2InputSocket(InputSocket<? extends E> model, JmxDirector director, JmxIOStatistics stats) {
        super(model, director, stats);
    }

    @Override
    public SeekableByteChannel newSeekableByteChannel() throws IOException {
        return new JmxSeekableByteChannel(getBoundSocket().newSeekableByteChannel(), stats);
    }
}