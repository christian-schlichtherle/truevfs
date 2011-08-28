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
