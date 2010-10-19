/*
 * Copyright (C) 2010 Schlichtherle IT Services
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

import de.schlichtherle.truezip.io.entry.CommonEntry;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import java.io.IOException;
import java.io.InputStream;

/**
 * @param   <LT> The type of the {@link #getLocalTarget() local target}.
 * @see     ProxyingOutputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class ProxyingInputSocket<LT extends CommonEntry>
extends InputSocket<LT> {

    private final LT target;
    private InputSocket<?> input;

    public ProxyingInputSocket( final LT target,
                                final InputSocket<?> input) {
        if (null == target)
            throw new NullPointerException();
        this.target = target;
        setInputSocket(input);
    }

    /**
     * Binds the proxied socket to this socket and returns it.
     * If you override this method, you must make sure to bind the returned
     * socket to this socket!
     *
     * @throws IOException at the discretion of an overriding method.
     * @return The bound proxied socket.
     */
    protected InputSocket<?> getInputSocket() throws IOException {
        return input.bind(this);
    }

    protected final void setInputSocket(final InputSocket<?> input) {
        if (null == input)
            throw new NullPointerException();
        this.input = input;
    }

    @Override
    public final LT getLocalTarget() {
        return target;
    }

    @Override
    public CommonEntry getPeerTarget() throws IOException {
        return getInputSocket().getPeerTarget();
    }

    @Override
    public InputStream newInputStream() throws IOException {
        return getInputSocket().newInputStream();
    }

    @Override
    public ReadOnlyFile newReadOnlyFile() throws IOException {
        return getInputSocket().newReadOnlyFile();
    }
}
