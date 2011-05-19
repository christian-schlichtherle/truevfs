/*
 * Copyright 2011 Schlichtherle IT Services
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
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import java.io.IOException;
import java.io.InputStream;

/**
 * An abstract decorator for an input socket.
 * 
 * @see     DecoratingOutputSocket
 * @param   <E> The type of the {@link #getLocalTarget() local target}.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class DecoratingInputSocket<E extends Entry>
extends InputSocket<E> {

    private final InputSocket<? extends E> delegate;

    protected DecoratingInputSocket(final InputSocket<? extends E> input) {
        if (null == input)
            throw new NullPointerException();
        this.delegate = input;
    }

    /**
     * Binds the decorated socket to this socket and returns it.
     *
     * @return The bound decorated socket.
     */
    protected InputSocket<? extends E> getBoundSocket() throws IOException {
        return delegate.bind(this);
    }

    @Override
    public E getLocalTarget() throws IOException {
        return getBoundSocket().getLocalTarget();
    }

    @Override
    public Entry getPeerTarget() throws IOException {
        return getBoundSocket().getPeerTarget();
    }

    @Override
    public ReadOnlyFile newReadOnlyFile() throws IOException {
        return getBoundSocket().newReadOnlyFile();
    }

    @Override
    public InputStream newInputStream() throws IOException {
        return getBoundSocket().newInputStream();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return new StringBuilder()
                .append(getClass().getName())
                .append("[delegate=")
                .append(delegate)
                .append(']')
                .toString();
    }
}
