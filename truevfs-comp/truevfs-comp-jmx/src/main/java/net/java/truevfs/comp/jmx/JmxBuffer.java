/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx;

import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.ObjectName;
import net.java.truevfs.comp.inst.InstrumentingBuffer;
import static net.java.truevfs.comp.jmx.JmxUtils.deregister;
import static net.java.truevfs.comp.jmx.JmxUtils.register;
import net.java.truevfs.kernel.spec.cio.IoBuffer;

/**
 * A controller for an {@linkplain IoBuffer I/O buffer}.
 * 
 * @param  <M> the type of the JMX mediator.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class JmxBuffer<M extends JmxMediator<M>>
extends InstrumentingBuffer<M> implements JmxColleague {

    public JmxBuffer(M mediator, IoBuffer entry) {
        super(mediator, entry);
    }

    private ObjectName getObjectName() {
        return mediator.nameBuilder(IoBuffer.class)
                .put("name", ObjectName.quote(getName()))
                .get();
    }

    protected Object newView() { return new JmxBufferView<>(entry); }

    @Override
    public void start() { register(getObjectName(), newView()); }

    @Override
    public void release() throws IOException {
        try {
            entry.release();
        } finally {
            deregister(getObjectName());
        }
    }
}
