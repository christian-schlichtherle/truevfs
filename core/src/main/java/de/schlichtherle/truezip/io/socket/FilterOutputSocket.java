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
 * @see FilterInputSocket
 * @param   <CE> The type of the {@link #getLocalTarget() local target} common entry.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class FilterOutputSocket<CE extends CommonEntry>
extends OutputSocket<CE> {

    private OutputSocket<? extends CE> output;

    protected FilterOutputSocket(final OutputSocket<? extends CE> output) {
        setOutputSocket(output);
    }

    protected final OutputSocket<? extends CE> getOutputSocket() {
        return output.bind(this);
    }

    protected final void setOutputSocket(final OutputSocket<? extends CE> output) {
        if (null == output)
            throw new NullPointerException();
        this.output = output;
    }

    @Override
    public CE getLocalTarget() throws IOException {
        return getOutputSocket().getLocalTarget();
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
