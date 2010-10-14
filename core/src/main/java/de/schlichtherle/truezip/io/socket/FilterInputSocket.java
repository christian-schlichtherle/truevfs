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
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import java.io.IOException;
import java.io.InputStream;

/**
 * @see FilterOutputSocket
 * @param   <CE> The type of the {@link #getLocalTarget() local target} common entry.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class FilterInputSocket<CE extends CommonEntry>
extends InputSocket<CE> {

    private InputSocket<? extends CE> input;

    protected FilterInputSocket(final InputSocket<? extends CE> input) {
        setInputSocket(input);
    }


    protected final InputSocket<? extends CE> getInputSocket() {
        return input.bind(this);
    }

    protected final void setInputSocket(final InputSocket<? extends CE> input) {
        if (null == input)
            throw new NullPointerException();
        this.input = input;
    }

    @Override
    public CE getLocalTarget() throws IOException {
        return getInputSocket().getLocalTarget();
    }

    @Override
    public CommonEntry getRemoteTarget() throws IOException {
        return getInputSocket().getRemoteTarget();
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
