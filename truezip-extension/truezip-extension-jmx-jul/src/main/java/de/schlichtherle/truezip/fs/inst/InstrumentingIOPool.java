/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.inst;

import de.schlichtherle.truezip.entry.DecoratingEntry;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.IOPool.Entry;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import net.jcip.annotations.Immutable;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public class InstrumentingIOPool<E extends Entry<E>> implements IOPool<E> {

    protected final InstrumentingDirector director;
    protected final IOPool<E> delegate;

    public InstrumentingIOPool(final IOPool<E> pool, final InstrumentingDirector director) {
        if (null == pool)
            throw new NullPointerException();
        this.director = director.check();
        this.delegate = pool;
    }

    @Override
    public Entry<E> allocate() throws IOException {
        return new InstrumentingEntry(delegate.allocate());
    }

    @Override
    public void release(Entry<E> resource) throws IOException {
        resource.release();
    }

    public class InstrumentingEntry
    extends DecoratingEntry<Entry<E>>
    implements Entry<E> {

        protected InstrumentingEntry(Entry<E> delegate) {
            super(delegate);
        }

        @Override
        public InputSocket<E> getInputSocket() {
            return director.instrument(delegate.getInputSocket(), this);
        }

        @Override
        public OutputSocket<E> getOutputSocket() {
            return director.instrument(delegate.getOutputSocket(), this);
        }

        @Override
        public void release() throws IOException {
            delegate.release();
        }
    }
}
