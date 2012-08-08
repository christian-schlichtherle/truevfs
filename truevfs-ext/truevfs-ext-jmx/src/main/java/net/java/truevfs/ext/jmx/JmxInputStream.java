/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import net.java.truevfs.ext.jmx.model.IoStatistics;
import net.java.truevfs.comp.jmx.JmxController;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truecommons.io.DecoratingInputStream;

/**
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class JmxInputStream
extends DecoratingInputStream implements JmxController {
    private final IoStatistics stats;

    JmxInputStream(@WillCloseWhenClosed InputStream in, IoStatistics stats) {
        super(in);
        assert null != stats;
        this.stats = stats;
    }

    @Override
    public void init() {
    }

    @Override
    public int read() throws IOException {
        int ret = in.read();
        if (0 < ret) stats.addBytesRead(1);
        return ret;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int ret = in.read(b, off, len);
        if (0 < ret) stats.addBytesRead(ret);
        return ret;
    }
}
