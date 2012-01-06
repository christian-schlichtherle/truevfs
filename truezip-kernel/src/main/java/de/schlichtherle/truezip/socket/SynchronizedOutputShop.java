/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.io.SynchronizedOutputStream;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import net.jcip.annotations.ThreadSafe;

/**
 * Decorates another output shop to add synchronization for all output streams
 * created by the decorated output shop in a multithreaded environment.
 * Mind that all synchronization is performed on the decorated output shop!
 *
 * @see     SynchronizedInputShop
 * @param   <E> The type of the entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public final class SynchronizedOutputShop<E extends Entry>
extends DecoratingOutputShop<E, OutputShop<E>> {

    /**
     * Constructs a concurrent output shop.
     * 
     * @param output the shop to decorate.
     */
    public SynchronizedOutputShop(OutputShop<E> output) {
        super(output);
    }

    /**
     * Returns the decorated output shop.
     * 
     * @return The decorated output shop.
     */
    public OutputShop<E> getDelegate() {
        return delegate;
    }

    @Override
    public void close() throws IOException {
        synchronized (delegate) {
            delegate.close();
        }
    }

    @Override
    public E getEntry(String name) {
        synchronized (delegate) {
            return delegate.getEntry(name);
        }
    }

    @Override
    public int getSize() {
        synchronized (delegate) {
            return delegate.getSize();
        }
    }

    @Override
    public Iterator<E> iterator() {
        throw new UnsupportedOperationException("This method cannot be thread-safe - use getDelegate().iterator() instead!");
    }

    @Override
    public final OutputSocket<? extends E> getOutputSocket(final E entry) {
        if (null == entry)
            throw new NullPointerException();

        class Output extends DecoratingOutputSocket<E> {
            Output() {
                super(SynchronizedOutputShop.super.getOutputSocket(entry));
            }

            // TODO: Implement newSeekableByteChannel()

            @Override
            public OutputStream newOutputStream() throws IOException {
                synchronized (SynchronizedOutputShop.this.delegate) {
                    return new SynchronizedOutputStream(
                            getBoundSocket().newOutputStream(),
                            SynchronizedOutputShop.this.delegate); // sync on delegate
                }
            }
        } // Output

        return new Output();
    }
}
