/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.ext.jul;

import javax.annotation.concurrent.Immutable;
import net.truevfs.comp.inst.InstrumentingCompositeDriver;
import net.truevfs.comp.inst.InstrumentingController;
import net.truevfs.comp.inst.InstrumentingDirector;
import net.truevfs.comp.inst.InstrumentingManager;
import net.truevfs.kernel.spec.FsController;
import net.truevfs.kernel.spec.FsModel;
import net.truevfs.kernel.spec.cio.*;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public final class JulDirector extends InstrumentingDirector<JulDirector> {
    public static final JulDirector SINGLETON = new JulDirector();

    private JulDirector() { }

    @Override
    public <B extends IoBuffer<B>> IoBufferPool<B> instrument(IoBufferPool<B> pool) {
        return new JulIoBufferPool<>(this, pool);
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
        return new JulInputSocket<>(this, input);
    }

    @Override
    protected <E extends Entry> OutputSocket<E> instrument(OutputSocket<E> output) {
        return new JulOutputSocket<>(this, output);
    }
}
