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
import java.io.IOException;
import java.io.OutputStream;

/**
 * @param   <LT> The type of the {@link #getLocalTarget() local target}.
 * @see     ProxyInputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class ProxyingOutputSocket<LT extends CommonEntry>
extends OutputSocket<LT> {

    private final LT target;
    private OutputSocket<?> output;

    /**
     * Constructs a proxy output socket.
     * 
     * @param target the non-{@code null} local target.
     * @param output the nullable proxied output socket.
     */
    public ProxyingOutputSocket(   final LT target,
                                final OutputSocket<?> output) {
        if (null == target)
            throw new NullPointerException();
        this.target = target;
        setOutputSocket(output);
    }

    protected final OutputSocket<?> getOutputSocket() {
        return output.bind(this);
    }

    protected final void setOutputSocket(final OutputSocket<?> output) {
        if (null == output)
            throw new NullPointerException();
        this.output = output;
    }

    @Override
    public final LT getLocalTarget() {
        return target;
    }

    @Override
    public CommonEntry getRemoteTarget() throws IOException {
        return getOutputSocket().getRemoteTarget();
    }

    @Override
    public OutputStream newOutputStream() throws IOException {
        return getOutputSocket().newOutputStream();
    }
}
