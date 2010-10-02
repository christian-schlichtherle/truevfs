/*
 * Copyright 2007-2010 Schlichtherle IT Services
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

import de.schlichtherle.truezip.io.socket.entry.FilterCommonEntryContainer;
import de.schlichtherle.truezip.io.socket.output.FilterOutputShop;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry;
import java.io.IOException;

/**
 * Decorates an {@code CommonInputShop}.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client applications.
 *
 * @param <CE> The type of the common entries.
 * @see FilterOutputShop
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FilterInputShop<
        CE extends CommonEntry,
        CI extends CommonInputShop<CE>>
extends    FilterCommonEntryContainer<CE, CI>
implements CommonInputShop<CE> {

    protected FilterInputShop(final CI input) {
        super(input);
    }

    @Override
    public CommonInputSocket<CE> newInputSocket(CE entry)
    throws IOException {
        if (getEntry(entry.getName()) != entry)
            throw new IllegalArgumentException("interface contract violation");
        return target.newInputSocket(entry);
    }

    @Override
    public void close() throws IOException {
        target.close();
    }
}
