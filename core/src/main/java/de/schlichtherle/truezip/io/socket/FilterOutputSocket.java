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

import de.schlichtherle.truezip.io.entry.CommonEntry;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @see     FilterInputSocket
 * @param   <LT> The type of the {@link #getLocalTarget() local target}.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class FilterOutputSocket<LT extends CommonEntry>
extends OutputSocket<LT> {

    private OutputSocket<? extends LT> output;

    protected FilterOutputSocket(final OutputSocket<? extends LT> output) {
        setOutputSocket(output);
    }

    /**
     * Binds the filtered socket to this socket and returns it.
     * If you override this method, you must make sure to bind the returned
     * socket to this socket!
     *
     * @throws IOException at the discretion of an overriding method.
     * @return The bound filtered socket.
     */
    protected OutputSocket<? extends LT> getOutputSocket() throws IOException {
        return output.bind(this);
    }

    protected final void setOutputSocket(final OutputSocket<? extends LT> output) {
        if (null == output)
            throw new NullPointerException();
        this.output = output;
    }

    @Override
    public LT getLocalTarget() throws IOException {
        return getOutputSocket().getLocalTarget();
    }

    @Override
    public CommonEntry getPeerTarget() throws IOException {
        return getOutputSocket().getPeerTarget();
    }

    @Override
    public OutputStream newOutputStream() throws IOException {
        return getOutputSocket().newOutputStream();
    }
}
