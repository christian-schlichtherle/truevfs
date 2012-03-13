/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Iterator;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Decorates another input shop to add synchronization for all input streams
 * or read only files created by the decorated input shop in a multithreaded
 * environment.
 * Mind that all synchronization is performed on the decorated input shop!
 *
 * @see        SynchronizedOutputShop
 * @param      <E> the type of the entries.
 * @author     Christian Schlichtherle
 * @deprecated This class will be removed in TrueZIP 8.
 */
@Deprecated
@ThreadSafe
public class SynchronizedInputShop<E extends Entry>
extends DecoratingInputShop<E, InputShop<E>> {

    /**
     * Constructs a synchronized input shop.
     *
     * @param input the shop to decorate.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public SynchronizedInputShop(@WillCloseWhenClosed InputShop<E> input) {
        super(input);
    }

    /**
     * Returns the decorated input shop.
     * 
     * @return     The decorated input shop.
     * @deprecated This method enables unsynchronized access to the decorated
     *             resource, which is inherently unsafe.
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
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                final InputShop<E> delegate = SynchronizedInputShop.this.delegate;
                synchronized (delegate) {
                    return new de.schlichtherle.truezip.rof.SynchronizedReadOnlyFile(
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
                    return new de.schlichtherle.truezip.io.SynchronizedInputStream(
                            getBoundSocket().newInputStream(),
                            delegate); // sync on delegate
                }
            }
        } // Input

        return new Input();
    }
}
