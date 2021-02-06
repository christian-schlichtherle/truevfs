/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.logging;

import global.namespace.truevfs.comp.cio.*;
import global.namespace.truevfs.comp.inst.*;
import global.namespace.truevfs.kernel.api.FsCompositeDriver;
import global.namespace.truevfs.kernel.api.FsController;
import global.namespace.truevfs.kernel.api.FsManager;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;

final class LogMediator extends Mediator<LogMediator> {

    static final Mediator<?> SINGLETON = new LogMediator();

    private LogMediator() {
    }

    @Override
    public FsManager instrument(FsManager subject) {
        return new InstrumentingManager<>(this, subject);
    }

    @Override
    public IoBufferPool instrument(IoBufferPool subject) {
        return new InstrumentingBufferPool<>(this, subject);
    }

    @Override
    public FsCompositeDriver instrument(InstrumentingManager<LogMediator> context, FsCompositeDriver subject) {
        return new InstrumentingCompositeDriver<>(this, subject);
    }

    @Override
    public IoBuffer instrument(InstrumentingBufferPool<LogMediator> context, IoBuffer subject) {
        return new LogBuffer(this, subject);
    }

    @Override
    public FsController instrument(InstrumentingCompositeDriver<LogMediator> context, FsController subject) {
        return new InstrumentingController<>(this, subject);
    }

    @Override
    public <E extends Entry> InputSocket<E> instrument(InstrumentingController<LogMediator> context, InputSocket<E> subject) {
        return new InstrumentingInputSocket<>(this, subject);
    }

    @Override
    public <E extends Entry> OutputSocket<E> instrument(InstrumentingController<LogMediator> context, OutputSocket<E> subject) {
        return new InstrumentingOutputSocket<>(this, subject);
    }

    @Override
    public <B extends IoBuffer> InputSocket<B> instrument(InstrumentingBuffer<LogMediator> context, InputSocket<B> subject) {
     return   new InstrumentingInputSocket<>(this, subject);
    }

    @Override
    public <B extends IoBuffer> OutputSocket<B> instrument(InstrumentingBuffer<LogMediator> context, OutputSocket<B> subject) {
        return new InstrumentingOutputSocket<>(this, subject);
    }

    @Override
    public <E extends Entry> InputStream instrument(InstrumentingInputSocket<LogMediator,E> context, InputStream subject) {
        return new LogInputStream(context, subject);
    }

    @Override
    public <E extends Entry> SeekableByteChannel instrument(InstrumentingInputSocket<LogMediator,E> context, SeekableByteChannel subject) {
        return new LogInputChannel(context, subject);
    }

    @Override
    public <E extends Entry> OutputStream instrument(InstrumentingOutputSocket<LogMediator,E> context, OutputStream subject) {
        return new LogOutputStream(context, subject);
    }

    @Override
    public <E extends Entry> SeekableByteChannel instrument(InstrumentingOutputSocket<LogMediator,E> context, SeekableByteChannel subject) {
        return new LogOutputChannel(context, subject);
    }
}
