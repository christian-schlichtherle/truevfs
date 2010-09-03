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

public interface InputStreamSocketProvider<TT, PT> {

    /**
     * Returns a non-{@code null} reference to an input stream socket for
     * reading from the given target of I/O operations.
     * <p>
     * The implementation must not assume that the returned input stream
     * socket will ever be used and must tolerate changes to all settable
     * properties of the target object.
     * <p>
     * Multiple invocations with the same parameter may return the same
     * object again.
     *
     * @param  target a non-{@code null} reference to the target object.
     * @return A non-{@code null} reference to an input stream socket for
     *         reading from the given target of I/O operations.
     * @throws FileNotFoundException If the target does not exist or is
     *         not accessible for some reason.
     */
    InputStreamSocket<TT, PT> getInputStreamSocket(TT target)
    throws FileNotFoundException;
}
