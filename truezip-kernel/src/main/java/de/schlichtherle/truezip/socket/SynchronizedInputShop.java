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
import java.nio.channels.SeekableByteChannel;
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
public class SynchronizedInputShop<E extends Entry>
extends DecoratingInputShop<E, InputShop<E>> {

    /**
     * Constructs a concurrent input shop.
     *
     * @param input the shop to decorate.
     */
    public SynchronizedInputShop(InputShop<E> input) {
        super(input);
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
        throw new UnsupportedOperationException("This returned iterator would not be thread-safe!");
    }

    @Override
    public InputSocket<? extends E> getInputSocket(final String name) {
        class Input extends DecoratingInputSocket<E> {
            Input() {
                super(SynchronizedInputShop.super.getInputSocket(name));
            }

            @Override
            public E getLocalTarget() throws IOException {
                final InputShop<E> delegate = SynchronizedInputShop.this.delegate;
                synchronized (delegate) {
                    return getBoundSocket().getLocalTarget();
                }
            }

            @Override
            public Entry getPeerTarget() throws IOException {
                final InputShop<E> delegate = SynchronizedInputShop.this.delegate;
                synchronized (delegate) {
                    return getBoundSocket().getPeerTarget();
                }
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                final InputShop<E> delegate = SynchronizedInputShop.this.delegate;
                synchronized (delegate) {
                    return new SynchronizedReadOnlyFile(
                            getBoundSocket().newReadOnlyFile(),
                            delegate); // sync on delegate
                }
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel() throws IOException {
                throw new UnsupportedOperationException("TODO: Implement this!");
            }

            @Override
            public InputStream newInputStream() throws IOException {
                final InputShop<E> delegate = SynchronizedInputShop.this.delegate;
                synchronized (delegate) {
                    return new SynchronizedInputStream(
                            getBoundSocket().newInputStream(),
                            delegate); // sync on delegate
                }
            }
        } // Input

        return new Input();
    }
}
