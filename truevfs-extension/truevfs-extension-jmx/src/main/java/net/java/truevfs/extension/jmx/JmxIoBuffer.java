/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.jmx;

import java.io.IOException;
import net.java.truevfs.component.instrumentation.InstrumentingIoBuffer;
import net.java.truevfs.kernel.spec.cio.IoBuffer;

/**
 * @author Christian Schlichtherle
 */
final class JmxIoBuffer extends InstrumentingIoBuffer<JmxDirector> {

    JmxIoBuffer(JmxDirector director, IoBuffer entry) {
        super(director, entry);
        JmxIoBufferView.register(entry);
    }

    @Override
    public void release() throws IOException {
        try {
            entry.release();
        } finally {
            JmxIoBufferView.unregister(entry);
        }
    }
}
