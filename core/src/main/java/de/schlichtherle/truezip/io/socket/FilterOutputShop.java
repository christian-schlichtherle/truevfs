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

package de.schlichtherle.truezip.io.socket;

import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.entry.FilterEntryContainer;
import java.io.IOException;

/**
 * Decorates an {@code OutputShop}.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client applications.
 *
 * @param <E> The type of the entries.
 * @see FilterInputShop
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FilterOutputShop<
        E extends Entry,
        CO extends OutputShop<E>>
extends FilterEntryContainer<E, CO>
implements OutputShop<E> {

    protected FilterOutputShop(final CO output) {
        super(output);
    }

    @Override
    public OutputSocket<? extends E> getOutputSocket(E entry) {
        if (null == entry)
            throw new NullPointerException();

        return target.getOutputSocket(entry);
    }

    @Override
    public void close() throws IOException {
        target.close();
    }
}
