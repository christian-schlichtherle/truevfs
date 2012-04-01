/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul.jul;

import de.truezip.extension.jmxjul.InstrumentingCompositeDriver;
import de.truezip.extension.jmxjul.InstrumentingController;
import de.truezip.extension.jmxjul.InstrumentingDirector;
import de.truezip.extension.jmxjul.InstrumentingManager;
import de.truezip.kernel.FsController;
import de.truezip.kernel.cio.*;
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
    protected FsController<?> instrument(FsController<?> controller, InstrumentingManager context) {
        return controller;
    }

    @Override
    protected FsController<?> instrument(FsController<?> controller, InstrumentingCompositeDriver context) {
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
