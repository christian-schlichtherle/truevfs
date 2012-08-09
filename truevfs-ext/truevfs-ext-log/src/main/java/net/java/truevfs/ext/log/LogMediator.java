/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.log;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.comp.inst.InstrumentingBufferPool;
import net.java.truevfs.comp.inst.InstrumentingInputSocket;
import net.java.truevfs.comp.inst.InstrumentingOutputSocket;
import net.java.truevfs.comp.inst.Mediator;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.IoBuffer;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class LogMediator extends Mediator<LogMediator> {
    static final LogMediator SINGLETON = new LogMediator();

    private LogMediator() { }

    @Override
    public IoBuffer instrument(
            InstrumentingBufferPool<LogMediator> origin,
            IoBuffer object) {
        return new LogBuffer(this, object);
    }

    @Override
    public <E extends Entry> InputStream instrument(
            InstrumentingInputSocket<LogMediator, E> origin,
            InputStream object) {
        return new LogInputStream(origin, object);
    }

    @Override
    public <E extends Entry> SeekableByteChannel instrument(
            InstrumentingInputSocket<LogMediator, E> origin,
            SeekableByteChannel object) {
        return new LogInputChannel(origin, object);
    }

    @Override
    public <E extends Entry> OutputStream instrument(
            InstrumentingOutputSocket<LogMediator, E> origin,
            OutputStream object) {
        return new LogOutputStream(origin, object);
    }

    @Override
    public <E extends Entry> SeekableByteChannel instrument(
            InstrumentingOutputSocket<LogMediator, E> origin,
            SeekableByteChannel object) {
        return new LogOutputChannel(origin, object);
    }
}
