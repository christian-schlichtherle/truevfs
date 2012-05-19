/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul.jul;

import net.truevfs.extension.jmxjul.InstrumentingCompositeDriver;
import net.truevfs.extension.jmxjul.InstrumentingController;
import net.truevfs.extension.jmxjul.InstrumentingDirector;
import net.truevfs.extension.jmxjul.InstrumentingManager;
import net.truevfs.kernel.FsController;
import net.truevfs.kernel.FsModel;
import net.truevfs.kernel.cio.*;
import javax.annotation.concurrent.Immutable;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public final class JulDirector extends InstrumentingDirector<JulDirector> {
    public static final JulDirector SINGLETON = new JulDirector();

    /** Can't touch this - hammer time! */
    private JulDirector() { }

    @Override
    public <B extends IOBuffer<B>> IOPool<B> instrument(IOPool<B> pool) {
        return new JulIOPool<>(pool, this);
    }

    @Override
    protected FsController<? extends FsModel> instrument(FsController<? extends FsModel> controller, InstrumentingManager context) {
        return controller;
    }

    @Override
    protected FsController<? extends FsModel> instrument(FsController<? extends FsModel> controller, InstrumentingCompositeDriver context) {
        return new InstrumentingController<>(controller, this);
    }

    @Override
    protected <E extends Entry> InputSocket<E> instrument(InputSocket<E> input) {
        return new JulInputSocket<>(input, this);
    }

    @Override
    protected <E extends Entry> OutputSocket<E> instrument(OutputSocket<E> output) {
        return new JulOutputSocket<>(output, this);
    }
}
