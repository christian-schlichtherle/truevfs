/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.logging;

import javax.annotation.concurrent.Immutable;
import net.truevfs.component.instrumentation.InstrumentingCompositeDriver;
import net.truevfs.component.instrumentation.InstrumentingController;
import net.truevfs.component.instrumentation.InstrumentingDirector;
import net.truevfs.component.instrumentation.InstrumentingManager;
import net.truevfs.kernel.spec.FsController;
import net.truevfs.kernel.spec.FsModel;
import net.truevfs.kernel.spec.cio.*;

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
