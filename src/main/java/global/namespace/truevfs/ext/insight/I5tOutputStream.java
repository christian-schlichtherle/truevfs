/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.insight;

import global.namespace.truevfs.commons.inst.InstrumentingOutputStream;
import global.namespace.truevfs.commons.jmx.JmxComponent;
import lombok.val;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A controller for an {@link java.io.OutputStream}.
 *
 * @author Christian Schlichtherle
 */
final class I5tOutputStream extends InstrumentingOutputStream<I5tMediator> implements JmxComponent {

    I5tOutputStream(I5tMediator mediator, OutputStream out) {
        super(mediator, out);
    }

    @Override
    public void activate() {
    }

    @Override
    public void write(final int b) throws IOException {
        val start = System.nanoTime();
        out.write(b);
        mediator.logWrite(System.nanoTime() - start, 1);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        val start = System.nanoTime();
        out.write(b, off, len);
        mediator.logWrite(System.nanoTime() - start, len);
    }
}
