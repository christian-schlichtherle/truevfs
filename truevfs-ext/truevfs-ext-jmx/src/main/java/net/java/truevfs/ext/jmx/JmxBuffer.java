/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx;

import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.ObjectName;
import net.java.truevfs.comp.inst.InstrumentingBuffer;
import net.java.truevfs.comp.jmx.JmxBufferMXBean;
import static net.java.truevfs.comp.jmx.JmxUtils.deregister;
import static net.java.truevfs.comp.jmx.JmxUtils.register;
import net.java.truevfs.kernel.spec.cio.IoBuffer;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxBuffer
extends InstrumentingBuffer<JmxMediator> implements JmxColleague {

    public JmxBuffer(JmxMediator director, IoBuffer entry) {
        super(director, entry);
    }

    @Override
    public void start() {
        register(newView(), name());
    }

    protected JmxBufferMXBean newView() {
        return new JmxBufferView(this);
    }

    private ObjectName name() {
        return mediator.nameBuilder(IoBuffer.class)
                .put("name", ObjectName.quote(getName()))
                .get();
    }

    @Override
    public void release() throws IOException {
        try {
            entry.release();
        } finally {
            deregister(name());
        }
    }
}