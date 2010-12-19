/*
 * Copyright 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.socket;

import de.schlichtherle.truezip.io.entry.Entry;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @see     DecoratingInputSocket
 * @param   <LT> The type of the {@link #getLocalTarget() local target}.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class DecoratingOutputSocket<LT extends Entry>
extends OutputSocket<LT> {

    private final OutputSocket<? extends LT> delegate;

    protected DecoratingOutputSocket(final OutputSocket<? extends LT> output) {
        if (null == output)
            throw new NullPointerException();
        this.delegate = output;
    }

    /**
     * Binds the decorated socket to this socket and returns it.
     *
     * @return The bound decorated socket.
     */
    protected OutputSocket<? extends LT> getBoundSocket() throws IOException {
        return delegate.bind(this);
    }

    @Override
    public LT getLocalTarget() throws IOException {
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
