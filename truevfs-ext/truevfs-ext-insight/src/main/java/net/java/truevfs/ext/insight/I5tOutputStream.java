/*
 * Copyright (C) 2005-2020 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import net.java.truevfs.comp.inst.InstrumentingOutputStream;
import net.java.truevfs.comp.jmx.JmxComponent;

import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A controller for an {@link java.io.OutputStream}.
 *
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class I5tOutputStream extends InstrumentingOutputStream<I5tMediator> implements JmxComponent {

    I5tOutputStream(I5tMediator mediator, @WillCloseWhenClosed OutputStream out) {
        super(mediator, out);
    }

    @Override
    public void activate() {
    }

    @Override
    public void write(final int b) throws IOException {
        final var start = System.nanoTime();
        out.write(b);
        mediator.logWrite(System.nanoTime() - start, 1);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        final var start = System.nanoTime();
        out.write(b, off, len);
        mediator.logWrite(System.nanoTime() - start, len);
    }
}
