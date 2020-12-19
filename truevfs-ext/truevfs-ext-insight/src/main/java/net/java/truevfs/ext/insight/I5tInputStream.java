/*
 * Copyright (C) 2005-2020 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import net.java.truevfs.comp.inst.InstrumentingInputStream;
import net.java.truevfs.comp.jmx.JmxComponent;

import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.io.InputStream;

/**
 * A controller for an {@link java.io.InputStream}.
 *
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class I5tInputStream extends InstrumentingInputStream<I5tMediator> implements JmxComponent {

    I5tInputStream(I5tMediator mediator, @WillCloseWhenClosed InputStream in) {
        super(mediator, in);
    }

    @Override
    public void activate() {
    }

    @Override
    public int read() throws IOException {
        final var start = System.nanoTime();
        final var ret = in.read();
        if (0 <= ret) {
            mediator.logRead(System.nanoTime() - start, 1);
        }
        return ret;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        final var start = System.nanoTime();
        final var ret = in.read(b, off, len);
        if (0 <= ret) {
            mediator.logRead(System.nanoTime() - start, ret);
        }
        return ret;
    }
}
