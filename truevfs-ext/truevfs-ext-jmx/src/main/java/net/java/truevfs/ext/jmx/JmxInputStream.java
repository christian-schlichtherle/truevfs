/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import java.io.IOException;
import java.io.InputStream;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truevfs.comp.inst.InstrumentingInputStream;
import net.java.truevfs.ext.jmx.model.IoLogger;

/**
 * The MXBean controller for an {@linkplain InputStream input stream}.
 * 
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class JmxInputStream
extends InstrumentingInputStream<JmxMediator> implements JmxColleague {
    private IoLogger logger;

    JmxInputStream(
            final JmxMediator mediator,
            final @WillCloseWhenClosed InputStream in) {
        super(mediator, in);
        this.logger = mediator.getLogger();
    }

    @Override
    public void start() {
    }

    @Override
    public int read() throws IOException {
        final long start = System.nanoTime();
        final int ret = in.read();
        if (0 <= ret)
            for (final long time = System.nanoTime() - start;
                    !logger.tryLogRead(1, time); )
                logger = mediator.nextLogger();
        return ret;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        final long start = System.nanoTime();
        final int ret = in.read(b, off, len);
        if (0 <= ret)
            for (final long time = System.nanoTime() - start;
                    !logger.tryLogRead(ret, time); )
                logger = mediator.nextLogger();
        return ret;
    }
}
