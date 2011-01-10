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
import java.io.IOException;
import java.io.OutputStream;

/**
 * A decorator for an output socket.
 * 
 * @see     DecoratingInputSocket
 * @param   <E> The type of the {@link #getLocalTarget() local target}.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class DecoratingOutputSocket<E extends Entry>
extends OutputSocket<E> {

    private final OutputSocket<? extends E> delegate;

    protected DecoratingOutputSocket(final OutputSocket<? extends E> output) {
        if (null == output)
            throw new NullPointerException();
        this.delegate = output;
    }

    /**
     * Binds the decorated socket to this socket and returns it.
     *
     * @return The bound decorated socket.
     */
    protected OutputSocket<? extends E> getBoundSocket() throws IOException {
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
    public OutputStream newOutputStream() throws IOException {
        return getBoundSocket().newOutputStream();
    }
}
