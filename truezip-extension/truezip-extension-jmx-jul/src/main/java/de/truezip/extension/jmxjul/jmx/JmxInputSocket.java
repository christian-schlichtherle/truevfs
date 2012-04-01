/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul.jmx;

import de.truezip.extension.jmxjul.InstrumentingInputSocket;
import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.cio.InputSocket;
import de.truezip.kernel.rof.ReadOnlyFile;
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
    public ReadOnlyFile newReadOnlyFile() throws IOException {
        return new JmxReadOnlyFile(getBoundDelegate().newReadOnlyFile(), stats);
    }

    @Override
    public SeekableByteChannel newSeekableByteChannel() throws IOException {
        return new JmxSeekableByteChannel(getBoundDelegate().newSeekableByteChannel(), stats);
    }

    @Override
    public InputStream newInputStream() throws IOException {
        return new JmxInputStream(getBoundDelegate().newInputStream(), stats);
    }
}