/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.log;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.comp.inst.InstrumentingBuffer;
import net.java.truevfs.comp.inst.InstrumentingBufferPool;
import net.java.truevfs.comp.inst.InstrumentingController;
import net.java.truevfs.comp.inst.InstrumentingInputSocket;
import net.java.truevfs.comp.inst.InstrumentingManager;
import net.java.truevfs.comp.inst.InstrumentingMetaDriver;
import net.java.truevfs.comp.inst.InstrumentingOutputSocket;
import net.java.truevfs.comp.inst.Mediator;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsMetaDriver;
import net.java.truevfs.kernel.spec.FsModel;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.InputSocket;
import net.java.truevfs.kernel.spec.cio.IoBuffer;
import net.java.truevfs.kernel.spec.cio.IoBufferPool;
import net.java.truevfs.kernel.spec.cio.OutputSocket;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class LogMediator implements Mediator<LogMediator> {
    static final LogMediator SINGLETON = new LogMediator();

    private LogMediator() { }

    @Override
    public FsManager instrument(FsManager object) {
        return new InstrumentingManager<>(this, object);
    }

    @Override
    public IoBufferPool instrument(IoBufferPool object) {
        return new InstrumentingBufferPool<>(this, object);
    }

    @Override
    public FsMetaDriver instrument(
            InstrumentingManager<LogMediator> origin,
            FsMetaDriver object) {
        return new InstrumentingMetaDriver<>(this, object);
    }

    @Override
    public FsController instrument(
            InstrumentingManager<LogMediator> origin,
            FsController object) {
        return object;
    }

    @Override
    public IoBuffer instrument(
            InstrumentingBufferPool<LogMediator> origin,
            IoBuffer object) {
        return new LogBuffer(this, object);
    }

    @Override
    public FsModel instrument(
            InstrumentingMetaDriver<LogMediator> origin,
            FsModel object) {
        return object;
    }

    @Override
    public FsController instrument(
            InstrumentingMetaDriver<LogMediator> origin,
            FsController object) {
        return new InstrumentingController<>(this, object);
    }

    @Override
    public <E extends Entry> InputSocket<E> instrument(
            InstrumentingController<LogMediator> origin,
            InputSocket<E> object) {
        return new InstrumentingInputSocket<>(this, object);
    }

    @Override
    public <E extends Entry> OutputSocket<E> instrument(
            InstrumentingController<LogMediator> origin,
            OutputSocket<E> object) {
        return new InstrumentingOutputSocket<>(this, object);
    }

    @Override
    public <B extends IoBuffer> InputSocket<B> instrument(
            InstrumentingBuffer<LogMediator> origin,
            InputSocket<B> object) {
        return new InstrumentingInputSocket<>(this, object);
    }

    @Override
    public <B extends IoBuffer> OutputSocket<B> instrument(
            InstrumentingBuffer<LogMediator> origin,
            OutputSocket<B> object) {
        return new InstrumentingOutputSocket<>(this, object);
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
