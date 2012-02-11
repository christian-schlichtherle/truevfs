/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.inst.jmx;

import de.schlichtherle.truezip.fs.inst.InstrumentingIOPool;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.IOPool.Entry;
import java.io.IOException;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
final class JmxIOPool<E extends Entry<E>>
extends InstrumentingIOPool<E, JmxDirector> {

    JmxIOPool(IOPool<E> model, JmxDirector director) {
        super(model, director);
    }

    @Override
    public Entry<E> allocate() throws IOException {
        return new Buffer(delegate.allocate());
    }

    private final class Buffer
    extends InstrumentingIOPool<E, JmxDirector>.Buffer {

        @SuppressWarnings("LeakingThisInConstructor")
        Buffer(Entry<E> model) {
            super(model);
            JmxIOBufferView.register(this);
        }

        @Override
        public void release() throws IOException {
            try {
                delegate.release();
            } finally {
                JmxIOBufferView.unregister(this);
            }
        }
    } // Buffer
}
