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
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.DecoratingEntryContainer;
import java.io.IOException;

/**
 * A decorator for an input shop.
 *
 * @param   <E> The type of the entries.
 * @see     DecoratingOutputShop
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class DecoratingInputShop<E extends Entry, I extends InputShop<E>>
extends DecoratingEntryContainer<E, I>
implements InputShop<E> {

    protected DecoratingInputShop(final I input) {
        super(input);
    }

    @Override
    public InputSocket<? extends E> getInputSocket(String name) {
        return delegate.getInputSocket(name);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
