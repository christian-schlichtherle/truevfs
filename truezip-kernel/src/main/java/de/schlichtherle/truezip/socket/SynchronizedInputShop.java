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
import de.schlichtherle.truezip.io.SynchronizedInputStream;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.rof.SynchronizedReadOnlyFile;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import net.jcip.annotations.ThreadSafe;

/**
 * Decorates another input shop to add synchronization for all input streams
 * or read only files created by the decorated input shop in a multithreaded
 * environment.
 * Mind that all synchronization is performed on the decorated input shop!
 *
 * @see     SynchronizedOutputShop
 * @param   <E> The type of the entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public final class SynchronizedInputShop<E extends Entry>
extends DecoratingInputShop<E, InputShop<E>> {

    /**
     * Constructs a concurrent input shop.
     *
     * @param input the shop to decorate.
     */
    public SynchronizedInputShop(InputShop<E> input) {
        super(input);
    }

    /**
     * Returns the decorated input shop.
     * 
     * @return The decorated input shop.
     */
    public InputShop<E> getDelegate() {
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
    public InputSocket<? extends E> getInputSocket(final String name) {
        if (null == name)
            throw new NullPointerException();

        class Input extends DecoratingInputSocket<E> {
            Input() {
                super(SynchronizedInputShop.super.getInputSocket(name));
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                synchronized (SynchronizedInputShop.this.delegate) {
                    return new SynchronizedReadOnlyFile(
                            getBoundSocket().newReadOnlyFile(),
                            SynchronizedInputShop.this.delegate); // sync on delegate
                }
            }

            // TODO: Implement newSeekableByteChannel()

            @Override
            public InputStream newInputStream() throws IOException {
                synchronized (SynchronizedInputShop.this.delegate) {
                    return new SynchronizedInputStream(
                            getBoundSocket().newInputStream(),
                            SynchronizedInputShop.this.delegate); // sync on delegate
                }
            }
        } // Input

        return new Input();
    }
}
