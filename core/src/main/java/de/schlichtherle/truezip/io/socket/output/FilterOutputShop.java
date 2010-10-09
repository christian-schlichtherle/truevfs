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

package de.schlichtherle.truezip.io.socket.output;

import de.schlichtherle.truezip.io.socket.entry.FilterCommonEntryContainer;
import de.schlichtherle.truezip.io.socket.input.FilterInputShop;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry;
import java.io.IOException;

/**
 * Decorates an {@code CommonOutputShop}.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client applications.
 *
 * @param <CE> The type of the common entries.
 * @see FilterInputShop
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FilterOutputShop<
        CE extends CommonEntry,
        CO extends CommonOutputShop<CE>>
extends FilterCommonEntryContainer<CE, CO>
implements CommonOutputShop<CE> {

    protected FilterOutputShop(final CO output) {
        super(output);
    }

    @Override
    public CommonOutputSocket<CE> newOutputSocket(CE entry)
    throws IOException {
        if (null == entry)
            throw new NullPointerException();
        return target.newOutputSocket(entry);
    }

    @Override
    public void close() throws IOException {
        target.close();
    }
}
