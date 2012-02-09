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
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Iterator;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Decorates another output shop to add synchronization for all output streams
 * created by the decorated output shop in a multithreaded environment.
 * Mind that all synchronization is performed on the decorated output shop!
 *
 * @see        SynchronizedInputShop
 * @param      <E> The type of the entries.
 * @deprecated Use {@link ConcurrentOutputShop} instead.
 * @author     Christian Schlichtherle
 * @version    $Id$
 */
@Deprecated
@ThreadSafe
public class SynchronizedOutputShop<E extends Entry>
extends DecoratingOutputShop<E, OutputShop<E>> {

    /**
     * Constructs a synchronized output shop.
     * 
     * @param output the shop to decorate.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public SynchronizedOutputShop(@WillCloseWhenClosed OutputShop<E> output) {
        super(output);
    }

    /**
     * Returns the decorated output shop.
     * 
     * @return     The decorated output shop.
     * @deprecated This method enables unsynchronized access to the decorated
     *             resource, which is inherently unsafe.
     */
    public OutputShop<E> getDelegate() {
        return delegate;
    }

    @Override
    @DischargesObligation
    public void close() throws IOException {
        synchronized (delegate) {
            delegate.close();
        }
    }

    @Override
    public @CheckForNull E getEntry(String name) {
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
        throw new UnsupportedOperationException("The returned iterator would not be thread-safe!");
    }

    @Override
    public final OutputSocket<? extends E> getOutputSocket(final E entry) {
        class Output extends DecoratingOutputSocket<E> {
            Output() {
                super(SynchronizedOutputShop.super.getOutputSocket(entry));
            }

            @Override
            public E getLocalTarget() throws IOException {
                final OutputShop<E> delegate = SynchronizedOutputShop.this.delegate;
                synchronized (delegate) {
                    return entry;
                }
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel() throws IOException {
                throw new UnsupportedOperationException("TODO: Implement this!");
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                final OutputShop<E> delegate = SynchronizedOutputShop.this.delegate;
                synchronized (delegate) {
                    return new de.schlichtherle.truezip.io.SynchronizedOutputStream(
                            getBoundSocket().newOutputStream(),
                            delegate); // sync on delegate
                }
            }
        } // Output

        return new Output();
    }
}
