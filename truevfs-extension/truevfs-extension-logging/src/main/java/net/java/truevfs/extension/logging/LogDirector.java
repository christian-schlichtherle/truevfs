/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.logging;

import net.java.truevfs.kernel.spec.cio.IoBufferPool;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.IoBuffer;
import net.java.truevfs.kernel.spec.cio.InputSocket;
import net.java.truevfs.kernel.spec.cio.OutputSocket;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.component.instrumentation.InstrumentingCompositeDriver;
import net.java.truevfs.component.instrumentation.InstrumentingController;
import net.java.truevfs.component.instrumentation.InstrumentingDirector;
import net.java.truevfs.component.instrumentation.InstrumentingManager;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsModel;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public final class LogDirector extends InstrumentingDirector<LogDirector> {
    public static final LogDirector SINGLETON = new LogDirector();

    private LogDirector() { }

    @Override
    public <B extends IoBuffer<B>> IoBufferPool<B> instrument(IoBufferPool<B> pool) {
        return new LogIoBufferPool<>(this, pool);
    }

    @Override
    protected FsController<? extends FsModel> instrument(FsController<? extends FsModel> controller, InstrumentingManager context) {
        return controller;
    }

    @Override
    protected FsController<? extends FsModel> instrument(FsController<? extends FsModel> controller, InstrumentingCompositeDriver context) {
        return new InstrumentingController<>(this, controller);
    }

    @Override
    protected <E extends Entry> InputSocket<E> instrument(InputSocket<E> input) {
        return new LogInputSocket<>(this, input);
    }

    @Override
    protected <E extends Entry> OutputSocket<E> instrument(OutputSocket<E> output) {
        return new LogOutputSocket<>(this, output);
    }
}
