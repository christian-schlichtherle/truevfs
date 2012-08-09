/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import net.java.truevfs.ext.jmx.model.IoStatistics;
import net.java.truevfs.comp.jmx.JmxController;
import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truecommons.io.DecoratingOutputStream;

/**
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class JmxOutputStreamController
extends DecoratingOutputStream implements JmxController {
    private final IoStatistics stats;

    JmxOutputStreamController(@WillCloseWhenClosed OutputStream out, IoStatistics stats) {
        super(out);
        assert null != stats;
        this.stats = stats;
    }

    @Override
    public void init() {
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
        stats.addBytesWritten(1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        stats.addBytesWritten(len);
    }    
}
