/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
