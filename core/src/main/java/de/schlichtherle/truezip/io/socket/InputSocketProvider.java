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

/**
 * Provides {@link InputSocket}s for read access to local targets.
 *
 * @param   <LT> The type of the <i>local targets</i> for I/O operations.
 * @param   <PT> The minimum required type of the <i>peer targets</i>.
 * @see     OutputSocketProvider
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface InputSocketProvider<LT, PT> {

    /**
     * Returns a non-{@code null} input socket for read access to the
     * given local target.
     * Multiple invocations with the same parameter may return the same
     * object again.
     * <p>
     * The method {@link InputSocket#getTarget()} must return an object
     * which {@link Object#equals(Object) compares equal} to the given local
     * target when called on the returned input socket.
     *
     * @param  target the non-{@code null} local target.
     * @return A non-{@code null} input socket for reading from the local
     *         target.
     * @throws IOException If the local target does not exist or
     *         is not accessible for some reason.
     * @throws NullPointerException if {@code target} is {@code null}.
     */
    InputSocket<LT, PT> getInputSocket(LT target)
    throws IOException;
}
