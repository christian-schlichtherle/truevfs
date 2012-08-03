/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.component.instrumentation;

import java.io.IOException;
import java.util.Objects;
import net.java.truevfs.kernel.spec.cio.DecoratingEntry;
import net.java.truevfs.kernel.spec.cio.InputSocket;
import net.java.truevfs.kernel.spec.cio.IoBuffer;
import net.java.truevfs.kernel.spec.cio.OutputSocket;

/**
 * @param  <D> the type of the instrumenting director.
 * @param  <B> the type of the instrumented I/O buffer.
 * @author Christian Schlichtherle
 */
public class InstrumentingIoBuffer<
        D extends InstrumentingDirector<D>,
        B extends IoBuffer<B>>
extends DecoratingEntry<IoBuffer<B>>
implements IoBuffer<B> {
    protected final D director;

    public InstrumentingIoBuffer(
            final D director,
            final IoBuffer<B> buffer) {
        super(buffer);
        this.director = Objects.requireNonNull(director);
    }

    @Override
    public InputSocket<B> input() {
        return director.instrument(entry.input(), this);
    }

    @Override
    public OutputSocket<B> output() {
        return director.instrument(entry.output(), this);
    }

    @Override
    public void release() throws IOException {
        entry.release();
    }
}
