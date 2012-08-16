/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight;

import java.io.IOException;
import java.io.InputStream;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truevfs.comp.inst.InstrumentingInputStream;
import net.java.truevfs.comp.jmx.JmxColleague;

/**
 * A controller for an {@linkplain InputStream input stream}.
 * 
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class I5tInputStream
extends InstrumentingInputStream<I5tMediator> implements JmxColleague {

    I5tInputStream(
            I5tMediator mediator,
            @WillCloseWhenClosed InputStream in) {
        super(mediator, in);
    }

    @Override
    public void start() { }

    @Override
    public int read() throws IOException {
        final long start = System.nanoTime();
        final int ret = in.read();
        if (0 <= ret) mediator.logRead(System.nanoTime() - start, 1);
        return ret;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        final long start = System.nanoTime();
        final int ret = in.read(b, off, len);
        if (0 <= ret) mediator.logRead(System.nanoTime() - start, ret);
        return ret;
    }
}