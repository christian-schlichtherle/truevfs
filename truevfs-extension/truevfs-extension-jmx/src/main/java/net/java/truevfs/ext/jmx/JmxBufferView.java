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
    protected final JmxBufferController buffer;

    public JmxBufferView(final JmxBufferController buffer) {
        super(JmxBufferMXBean.class, true);
        this.buffer = Objects.requireNonNull(buffer);
    }

    @Override
    protected String getDescription(MBeanInfo info) {
        return "An I/O pool entry.";
    }

    @Override
    protected String getDescription(MBeanAttributeInfo info) {
        String description = null;
        switch (info.getName()) {
        case "Name":
            description = "The name of this I/O pool entry.";
            break;
        case "SizeOfData":
            description = "The data size of this I/O pool entry.";
            break;
        case "SizeOfStorage":
            description = "The storage size of this I/O pool entry.";
            break;
        case "TimeWritten":
            description = "The last write time of this I/O pool entry.";
            break;
        case "TimeRead":
            description = "The last read or access time of this I/O pool entry.";
            break;
        case "TimeCreated":
            description = "The creation time of this I/O pool entry.";
            break;
        }
        return description;
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