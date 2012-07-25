/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.logging;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.Immutable;
import net.truevfs.component.instrumentation.InstrumentingInputSocket;
import net.truevfs.kernel.spec.cio.Entry;
import net.truevfs.kernel.spec.cio.InputSocket;
import net.truevfs.kernel.spec.cio.OutputSocket;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class LogInputSocket<E extends Entry>
extends InstrumentingInputSocket<E> {

    LogInputSocket(LogDirector director, InputSocket<? extends E> model) {
        super(director, model);
    }

    @Override
    public InputStream stream(OutputSocket<? extends Entry> peer)
    throws IOException {
        return new LogInputStream(socket(), peer);
    }

    @Override
    public SeekableByteChannel channel(OutputSocket<? extends Entry> peer)
    throws IOException {
        return new LogInputChannel(socket(), peer);
    }
}