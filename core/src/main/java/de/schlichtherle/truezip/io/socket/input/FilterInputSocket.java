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
package de.schlichtherle.truezip.io.socket.input;

import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry;
import de.schlichtherle.truezip.io.socket.output.FilterOutputSocket;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @see FilterOutputSocket
 * @param   <CE> The type of the {@link #getTarget() local target} common entry.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class FilterInputSocket<CE extends CommonEntry>
extends CommonInputSocket<CE> {

    protected CommonInputSocket<? extends CE> target;

    protected FilterInputSocket(final CommonInputSocket<? extends CE> target) {
        this.target = target;
    }

    @Override
    public CE getTarget() {
        return target.chain(this).getTarget();
    }

    @Override
    public CommonEntry getPeerTarget() {
        return target.chain(this).getPeerTarget();
    }

    @Override
    public InputStream newInputStream() throws IOException {
        return target.chain(this).newInputStream();
    }

    @Override
    public ReadOnlyFile newReadOnlyFile() throws IOException {
        return target.chain(this).newReadOnlyFile();
    }
}
