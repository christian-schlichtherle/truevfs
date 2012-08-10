/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truevfs.comp.inst.InstrumentingOutputStream;
import net.java.truevfs.ext.jmx.model.IoLogger;

/**
 * The MXBean controller for an {@linkplain OutputStream output stream}.
 * 
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class JmxOutputStream
extends InstrumentingOutputStream<JmxMediator> implements JmxColleague {
    private IoLogger logger;

    JmxOutputStream(
            final JmxMediator mediator,
            final @WillCloseWhenClosed OutputStream out) {
        super(mediator, out);
        this.logger = mediator.getLogger();
    }

    @Override
    public void start() {
    }

    @Override
    public void write(int b) throws IOException {
        final long start = System.nanoTime();
        out.write(b);
        for (final long time = System.nanoTime() - start;
                !logger.tryLogWrite(1, time); )
            logger = mediator.nextLogger();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        final long start = System.nanoTime();
        out.write(b, off, len);
        for (final long time = System.nanoTime() - start;
                !logger.tryLogWrite(len, time); )
            logger = mediator.nextLogger();
    }    
}
