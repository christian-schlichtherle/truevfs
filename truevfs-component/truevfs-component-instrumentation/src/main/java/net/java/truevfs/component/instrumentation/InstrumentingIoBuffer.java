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
 * @param  <D> the type of the director.
 * @author Christian Schlichtherle
 */
public class InstrumentingIoBuffer<D extends Director<D>>
extends DecoratingEntry<IoBuffer> implements IoBuffer {
    protected final D director;

    public InstrumentingIoBuffer(final D director, final IoBuffer buffer) {
        super(buffer);
        this.director = Objects.requireNonNull(director);
    }

    @Override
    public InputSocket<? extends IoBuffer> input() {
        return director.instrument(this, entry.input());
    }

    @Override
    public OutputSocket<? extends IoBuffer> output() {
        return director.instrument(this, entry.output());
    }

    @Override
    public void release() throws IOException {
        entry.release();
    }
}
