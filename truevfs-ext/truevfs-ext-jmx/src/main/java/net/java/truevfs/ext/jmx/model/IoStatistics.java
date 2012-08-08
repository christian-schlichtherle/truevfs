/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx.model;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class IoStatistics {
    private static final AtomicInteger counter = new AtomicInteger();

    private final int id = counter.getAndIncrement();
    private final AtomicLong read = new AtomicLong();
    private final AtomicLong written = new AtomicLong();
    private final long time = System.currentTimeMillis();
    private final String kind;

    public IoStatistics(final String kind) {
        this.kind = Objects.requireNonNull(kind);
    }

    public int getId() {
        return id;
    }

    public String getKind() {
        return kind;
    }

    public long getTimeCreatedMillis() {
        return time;
    }

    public long getBytesRead() {
        return read.get();
    }

    public void addBytesRead(int inc) {
        read.addAndGet(inc);
    }

    public long getBytesWritten() {
        return written.get();
    }

    public void addBytesWritten(int inc) {
        written.addAndGet(inc);
    }
}
