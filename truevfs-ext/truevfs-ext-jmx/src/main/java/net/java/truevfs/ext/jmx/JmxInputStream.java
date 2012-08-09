/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import java.io.IOException;
import java.io.InputStream;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truecommons.io.DecoratingInputStream;
import net.java.truevfs.ext.jmx.model.IoLogger;

/**
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class JmxInputStream
extends DecoratingInputStream implements JmxColleague {
    private final IoLogger logger;

    JmxInputStream(
            final @WillCloseWhenClosed InputStream in,
            final IoLogger logger) {
        super(in);
        assert null != logger;
        this.logger = logger;
    }

    @Override
    public void start() {
    }

    @Override
    public int read() throws IOException {
        final long start = System.nanoTime();
        int ret = in.read();
        if (0 < ret) logger.read(1, System.nanoTime() - start);
        return ret;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        final long start = System.nanoTime();
        int ret = in.read(b, off, len);
        if (0 < ret) logger.read(ret, System.nanoTime() - start);
        return ret;
    }
}
