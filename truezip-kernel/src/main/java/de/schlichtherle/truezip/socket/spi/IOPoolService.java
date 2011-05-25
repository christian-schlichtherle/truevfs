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
package de.schlichtherle.truezip.socket.spi;

import de.schlichtherle.truezip.socket.IOPoolProvider;
import de.schlichtherle.truezip.socket.sl.IOPoolLocator;

/**
 * An abstract locatable service for an I/O pool.
 * Implementations of this abstract class are subject to service location
 * by the class {@link IOPoolLocator}.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class IOPoolService implements IOPoolProvider {

    /**
     * Returns a priority to help the I/O pool service locator.
     * The higher number wins!
     * 
     * @return {@code 0}, as by the implementation in the class
     *         {@link IOPoolService}.
     */
    public int getPriority() {
        return 0;
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return new StringBuilder()
                .append(getClass().getName())
                .append("[priority=")
                .append(getPriority())
                .append(']')
                .toString();
    }
}
