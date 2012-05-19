/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul.jmx;

import net.truevfs.kernel.io.DecoratingOutputStream;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author  Christian Schlichtherle
 */
@NotThreadSafe
final class JmxOutputStream extends DecoratingOutputStream {
    private final JmxIOStatistics stats;

    @CreatesObligation
    JmxOutputStream(@WillCloseWhenClosed OutputStream out, JmxIOStatistics stats) {
        super(out);
        assert null != stats;
        this.stats = stats;
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
        stats.incBytesWritten(1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        stats.incBytesWritten(len);
    }    
}