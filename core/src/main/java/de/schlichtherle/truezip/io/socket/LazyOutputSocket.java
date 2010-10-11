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

import java.io.IOException;
import java.io.OutputStream;

/**
 * @param   <CE> The type of the {@link #getLocalTarget() local target}.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class LazyOutputSocket<CE extends CommonEntry>
extends OutputSocket<CE> {

    private final OutputSocketFactory<CE> factory;
    private OutputSocket<CE> socket;

    protected LazyOutputSocket(final OutputSocketFactory<CE> factory) {
        if (null == factory)
            throw new NullPointerException();
        this.factory = factory;
    }

    protected final OutputSocket<CE> getOutputSocket() throws IOException {
        return (null == socket
                ? socket = factory.newOutputSocket(getLocalTarget())
                : socket).share(this);
    }

    @Override
    public CommonEntry getRemoteTarget() throws IOException {
        return getOutputSocket().getRemoteTarget();
    }

    @Override
    public final OutputStream newOutputStream() throws IOException {
        return getOutputSocket().newOutputStream();
    }
}
