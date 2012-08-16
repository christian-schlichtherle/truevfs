/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truevfs.comp.inst.InstrumentingOutputStream;
import net.java.truevfs.comp.jmx.JmxColleague;

/**
 * A controller for an {@linkplain OutputStream output stream}.
 * 
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class I5tOutputStream
extends InstrumentingOutputStream<I5tMediator> implements JmxColleague {

    I5tOutputStream(
            I5tMediator mediator,
            @WillCloseWhenClosed OutputStream out) {
        super(mediator, out);
    }

    @Override
    public void start() { }

    @Override
    public void write(int b) throws IOException {
        final long start = System.nanoTime();
        out.write(b);
        mediator.logWrite(System.nanoTime() - start, 1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        final long start = System.nanoTime();
        out.write(b, off, len);
        mediator.logWrite(System.nanoTime() - start, len);
    }    
}
