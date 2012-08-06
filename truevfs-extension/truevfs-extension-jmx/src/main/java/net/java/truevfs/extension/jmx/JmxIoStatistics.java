/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.jmx;

import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class JmxIoStatistics {

    private final long time;
    private final AtomicLong read = new AtomicLong();
    private final AtomicLong written = new AtomicLong();

    JmxIoStatistics() {
        time = System.currentTimeMillis();
    }

    long getTimeCreatedMillis() {
        return time;
    }

    long getBytesRead() {
        return read.get();
    }

    void addBytesRead(int inc) {
        read.addAndGet(inc);
    }

    long getBytesWritten() {
        return written.get();
    }

    void addBytesWritten(int inc) {
        written.addAndGet(inc);
    }
}
