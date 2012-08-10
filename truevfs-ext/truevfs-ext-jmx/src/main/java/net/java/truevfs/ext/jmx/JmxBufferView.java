/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import java.util.Date;
import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.StandardMBean;
import net.java.truevfs.comp.jmx.JmxBufferMXBean;
import static net.java.truevfs.kernel.spec.cio.Entry.Access.*;
import static net.java.truevfs.kernel.spec.cio.Entry.Size.DATA;
import static net.java.truevfs.kernel.spec.cio.Entry.Size.STORAGE;
import static net.java.truevfs.kernel.spec.cio.Entry.UNKNOWN;
import net.java.truevfs.kernel.spec.cio.IoBuffer;

/**
 * The MXBean implementation for an {@linkplain IoBuffer I/O buffer}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxBufferView
extends StandardMBean implements JmxBufferMXBean {
    protected final JmxBuffer buffer;

    public JmxBufferView(final JmxBuffer buffer) {
        super(JmxBufferMXBean.class, true);
        this.buffer = Objects.requireNonNull(buffer);
    }

    @Override
    protected String getDescription(MBeanInfo info) {
        return "An I/O pool entry.";
    }

    @Override
    protected String getDescription(MBeanAttributeInfo info) {
        switch (info.getName()) {
        case "Name":
            return "The name of this buffer.";
        case "SizeOfData":
            return "The data size of this buffer.";
        case "SizeOfStorage":
            return "The storage size of this buffer.";
        case "TimeWritten":
            return "The last write time of this buffer.";
        case "TimeRead":
            return "The last read or access time of this buffer.";
        case "TimeCreated":
            return "The creation time of this buffer.";
        default:
            return null;
        }
    }

    @Override
    public String getName() {
        return buffer.getName();
    }

    @Override
    public long getSizeOfData() {
        return buffer.getSize(DATA);
    }

    @Override
    public long getSizeOfStorage() {
        return buffer.getSize(STORAGE);
    }

    @Override
    public String getTimeWritten() {
        final long time = buffer.getTime(WRITE);
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    @Override
    public String getTimeRead() {
        final long time = buffer.getTime(READ);
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    @Override
    public String getTimeCreated() {
        final long time = buffer.getTime(CREATE);
        return UNKNOWN == time ? null : new Date(time).toString();
    }
}