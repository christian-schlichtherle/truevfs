/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.logging;

import javax.annotation.concurrent.Immutable;
import net.java.truevfs.component.instrumentation.InstrumentingDirector;
import net.java.truevfs.kernel.spec.cio.Entry;
import net.java.truevfs.kernel.spec.cio.InputSocket;
import net.java.truevfs.kernel.spec.cio.IoBufferPool;
import net.java.truevfs.kernel.spec.cio.OutputSocket;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public final class LogDirector extends InstrumentingDirector<LogDirector> {
    public static final LogDirector SINGLETON = new LogDirector();

    private LogDirector() { }

    @Override
    public IoBufferPool instrument(
            IoBufferPool pool) {
        return new LogIoBufferPool(this, pool);
    }

    @Override
    protected <E extends Entry> InputSocket<E> instrument(
            InputSocket<E> input) {
        return new LogInputSocket<>(this, input);
    }

    @Override
    protected <E extends Entry> OutputSocket<E> instrument(
            OutputSocket<E> output) {
        return new LogOutputSocket<>(this, output);
    }
}
