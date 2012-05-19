/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul.jmx;

import net.truevfs.kernel.io.DecoratingInputStream;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author  Christian Schlichtherle
 */
@NotThreadSafe
final class JmxInputStream extends DecoratingInputStream {
    private final JmxIOStatistics stats;

    @CreatesObligation
    JmxInputStream(@WillCloseWhenClosed InputStream in, JmxIOStatistics stats) {
        super(in);
        assert null != stats;
        this.stats = stats;
    }

    @Override
    public int read() throws IOException {
        int ret = in.read();
        if (0 < ret)
            stats.incBytesRead(1);
        return ret;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int ret = in.read(b, off, len);
        if (0 < ret)
            stats.incBytesRead(ret);
        return ret;
    }
}