/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.log;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.comp.inst.AbstractDirector;
import net.java.truevfs.comp.inst.InstrumentingInputSocket;
import net.java.truevfs.comp.inst.InstrumentingBufferPool;
import net.java.truevfs.comp.inst.InstrumentingOutputSocket;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.IoBuffer;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class LogDirector extends AbstractDirector<LogDirector> {
    static final LogDirector SINGLETON = new LogDirector();

    private LogDirector() { }

    @Override
    public IoBuffer instrument(
            InstrumentingBufferPool<LogDirector> origin,
            IoBuffer object) {
        return new LogBuffer(this, object);
    }

    @Override
    public InputStream instrument(
            InstrumentingInputSocket<LogDirector, ? extends Entry> origin,
            InputStream object) {
        return new LogInputStream(origin, object);
    }

    @Override
    public SeekableByteChannel instrument(
            InstrumentingInputSocket<LogDirector, ? extends Entry> origin,
            SeekableByteChannel object) {
        return new LogInputChannel(origin, object);
    }

    @Override
    public OutputStream instrument(
            InstrumentingOutputSocket<LogDirector, ? extends Entry> origin,
            OutputStream object) {
        return new LogOutputStream(origin, object);
    }

    @Override
    public SeekableByteChannel instrument(
            InstrumentingOutputSocket<LogDirector, ? extends Entry> origin,
            SeekableByteChannel object) {
        return new LogOutputChannel(origin, object);
    }
}
