/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.jmx;

import java.io.IOException;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.component.instrumentation.InstrumentingIoBufferPool;
import net.java.truevfs.kernel.spec.cio.IoBuffer;
import net.java.truevfs.kernel.spec.cio.IoBufferPool;

/**
 * @author Christian Schlichtherle
 */
@Immutable
final class JmxIoBufferPool extends InstrumentingIoBufferPool<JmxDirector> {

    JmxIoBufferPool(JmxDirector director, IoBufferPool model) {
        super(director, model);
    }

    @Override
    public IoBuffer allocate() throws IOException {
        return new JmxIoBuffer(director, pool.allocate());
    }
}
