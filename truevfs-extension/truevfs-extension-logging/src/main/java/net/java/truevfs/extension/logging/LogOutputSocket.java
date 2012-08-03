/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.logging;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.component.instrumentation.InstrumentingOutputSocket;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.InputSocket;
import net.java.truevfs.kernel.spec.cio.OutputSocket;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class LogOutputSocket<E extends Entry>
extends InstrumentingOutputSocket<LogDirector, E> {

    LogOutputSocket(LogDirector director, OutputSocket<? extends E> model) {
        super(director, model);
    }

    @Override
    public OutputStream stream(InputSocket<? extends Entry> peer)
    throws IOException {
        return new LogOutputStream(socket(), peer);
    }

    @Override
    public SeekableByteChannel channel(InputSocket<? extends Entry> peer)
    throws IOException {
        return new LogOutputChannel(socket(), peer);
    }
}
