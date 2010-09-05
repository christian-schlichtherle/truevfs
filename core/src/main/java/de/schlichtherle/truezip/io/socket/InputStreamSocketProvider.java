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

import java.io.FileNotFoundException;

/**
 * Provides {@link InputStreamSocket}s for accessing targets for I/O
 * operations.
 *
 * @param   <LT> The type of the <i>local target</i> for I/O operations.
 * @param   <PT> The minimum required type of the <i>peer targets</i> for
 *          for I/O operations.
 * @see     OutputStreamSocketProvider
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface InputStreamSocketProvider<LT, PT> {

    /**
     * Returns a non-{@code null} input stream socket for reading from the
     * given target for I/O operations.
     * Multiple invocations with the same parameter may return the same
     * object again.
     * <p>
     * The method {@link InputStreamSocket#get()} must return an object which
     * {@link Object#equals(Object) compares equal} to the given target for
     * I/O operations when called on the returned input stream socket.
     *
     * @param  target a non-{@code null} target for I/O operations.
     * @return A non-{@code null} input stream socket for reading from the
     *         given target for I/O operations.
     * @throws FileNotFoundException If the target does not exist or is
     *         not accessible for some reason.
     * @throws NullPointerException if {@code target} is {@code null}.
     */
    InputStreamSocket<LT, PT> getInputStreamSocket(LT target)
    throws FileNotFoundException;
}
