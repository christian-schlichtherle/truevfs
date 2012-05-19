/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul.jmx;

import javax.annotation.concurrent.ThreadSafe;

/**
 * @author  Christian Schlichtherle
 */
@ThreadSafe
final class JmxIOStatistics {

    private final long time;
    private volatile long read;
    private volatile long written;

    JmxIOStatistics() {
        time = System.currentTimeMillis();
    }

    long getTimeCreatedMillis() {
        return time;
    }
    
    long getBytesRead() {
        return read;
    }

    synchronized void incBytesRead(int inc) {
        read += inc;
    }

    long getBytesWritten() {
        return written;
    }

    synchronized void incBytesWritten(int inc) {
        written += inc;
    }
}