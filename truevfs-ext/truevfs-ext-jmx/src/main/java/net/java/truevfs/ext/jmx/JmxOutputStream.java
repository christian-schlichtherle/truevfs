/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truecommons.io.DecoratingOutputStream;
import net.java.truevfs.ext.jmx.model.IoLogger;

/**
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class JmxOutputStream
extends DecoratingOutputStream implements JmxColleague {
    private final IoLogger logger;

    JmxOutputStream(
            final @WillCloseWhenClosed OutputStream out,
            final IoLogger logger) {
        super(out);
        assert null != logger;
        this.logger = logger;
    }

    @Override
    public void start() {
    }

    @Override
    public void write(int b) throws IOException {
        final long start = System.nanoTime();
        out.write(b);
        logger.write(1, System.nanoTime() - start);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        final long start = System.nanoTime();
        out.write(b, off, len);
        logger.write(len, System.nanoTime() - start);
    }    
}
