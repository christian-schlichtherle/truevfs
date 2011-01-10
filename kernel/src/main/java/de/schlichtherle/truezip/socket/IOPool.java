/*
 * Copyright (C) 2011 Schlichtherle IT Services
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

import de.schlichtherle.truezip.socket.IOPool.Entry;
import de.schlichtherle.truezip.util.Pool;
import java.io.IOException;
import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;

/**
 * A pool for I/O entries.
 * <p>
 * Note that any pool implementation is expected to be thread-safe!
 * However, this does not necessarily apply to the implementation of its
 * managed resources.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public interface IOPool<E extends IOEntry<E>>
extends Pool<Entry<E>, IOException> {

    @NotThreadSafe
    interface Entry<E extends IOEntry<E>>
    extends IOEntry<E>, Pool.Releasable<IOException> {
    }
}
