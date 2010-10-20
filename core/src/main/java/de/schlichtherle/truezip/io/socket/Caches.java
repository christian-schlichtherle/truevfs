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

/**
 * Utility class containing static methods to obtain a cache for input and
 * output sockets.
 * Using a cache for input and output sockets has the following effects:
 * <ul>
 * <li>Upon the first read operation, the data will be read and from the
 *     local target and stored in the cache.
 *     Subsequent or concurrent read operations will be served from the cache
 *     without re-reading the data from the local target again until the cache
 *     gets cleared.
 * <li>Any data written to the cache will get written to the local target if
 *     and only if the cache gets flushed.
 * <li>After a write operation, the data will be stored in the cache for
 *     subsequent read operations until the cache gets cleared.
 * </ul>
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class Caches {

    public static <LT extends CommonEntry>
    InputCache<LT> newInstance(InputSocket <? extends LT> input) {
        return new DefaultCache<LT>(input, null);
    }

    public static <LT extends CommonEntry>
    OutputCache<LT> newInstance(OutputSocket <? extends LT> output) {
        return new DefaultCache<LT>(null, output);
    }

    public static <LT extends CommonEntry>
    Cache<LT> newInstance(InputSocket<? extends LT> input,
                          OutputSocket<? extends LT> output) {
        /*if (null == input || null == output)
            throw new NullPointerException();*/
        return new DefaultCache<LT>(input, output);
    }
}
