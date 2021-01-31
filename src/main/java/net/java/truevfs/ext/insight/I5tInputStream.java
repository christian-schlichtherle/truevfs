/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import lombok.val;
import net.java.truevfs.comp.inst.InstrumentingInputStream;
import net.java.truevfs.comp.jmx.JmxComponent;

import java.io.IOException;
import java.io.InputStream;

/**
 * A controller for an {@link java.io.InputStream}.
 *
 * @author Christian Schlichtherle
 */
final class I5tInputStream extends InstrumentingInputStream<I5tMediator> implements JmxComponent {

    I5tInputStream(I5tMediator mediator, InputStream in) {
        super(mediator, in);
    }

    @Override
    public void activate() {
    }

    @Override
    public int read() throws IOException {
        val start = System.nanoTime();
        val ret = in.read();
        if (0 <= ret) {
            mediator.logRead(System.nanoTime() - start, 1);
        }
        return ret;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        val start = System.nanoTime();
        val ret = in.read(b, off, len);
        if (0 <= ret) {
            mediator.logRead(System.nanoTime() - start, ret);
        }
        return ret;
    }
}
