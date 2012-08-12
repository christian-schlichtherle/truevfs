/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx;

import java.util.Date;
import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.StandardMBean;
import static net.java.truevfs.kernel.spec.cio.Entry.Access.*;
import static net.java.truevfs.kernel.spec.cio.Entry.Size.DATA;
import static net.java.truevfs.kernel.spec.cio.Entry.Size.STORAGE;
import static net.java.truevfs.kernel.spec.cio.Entry.UNKNOWN;
import net.java.truevfs.kernel.spec.cio.IoBuffer;

/**
 * A view for an {@linkplain IoBuffer I/O buffer}.
 *
 * @param  <B> the type of the I/O buffer.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxBufferView<B extends IoBuffer>
extends StandardMBean implements JmxBufferMXBean {

    protected final B buffer;

    public JmxBufferView(B buffer) { this(JmxBufferMXBean.class, buffer); }

    protected JmxBufferView(
            final Class<? extends JmxBufferMXBean> type,
            final B buffer) {
        super(type, true);
        this.buffer = Objects.requireNonNull(buffer);
    }

    @Override
    protected String getDescription(MBeanInfo info) {
        return "An I/O buffer.";
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
        case "TimeCreatedDate":
            return "The time this buffer has been created.";
        case "TimeCreatedMillis":
            return "The time this buffer has been created in milliseconds.";
        case "TimeReadDate":
            return "The last time this buffer has been read or accessed.";
        case "TimeReadMillis":
            return "The last time this buffer has been read or accessed in milliseconds.";
        case "TimeWrittenDate":
            return "The last time this buffer has been written.";
        case "TimeWrittenMillis":
            return "The last time this buffer has been written in milliseconds.";
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
    public String getTimeCreatedDate() {
        final long time = buffer.getTime(CREATE);
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    @Override
    public Long getTimeCreatedMillis() {
        final long time = buffer.getTime(CREATE);
        return UNKNOWN == time ? null : time;
    }

    @Override
    public String getTimeReadDate() {
        final long time = buffer.getTime(READ);
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    @Override
    public Long getTimeReadMillis() {
        final long time = buffer.getTime(READ);
        return UNKNOWN == time ? null : time;
    }

    @Override
    public String getTimeWrittenDate() {
        final long time = buffer.getTime(WRITE);
        return UNKNOWN == time ? null : new Date(time).toString();
    }

    @Override
    public Long getTimeWrittenMillis() {
        final long time = buffer.getTime(WRITE);
        return UNKNOWN == time ? null : time;
    }
}
