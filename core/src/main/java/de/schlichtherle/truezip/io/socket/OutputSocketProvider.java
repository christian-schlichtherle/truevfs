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
 * Provides {@link OutputSocket}s for write access to local targets.
 *
 * @param   <LT> The type of the <i>local targets</i> for I/O operations.
 * @param   <PT> The minimum required type of the <i>peer targets</i>.
 * @see     InputSocketProvider
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface OutputSocketProvider<LT, PT> {

    /**
     * Returns a non-{@code null} output socket for write access to the
     * given local target.
     * <p>
     * The implementation must not assume that the returned output socket will
     * ever be used and must tolerate changes to all settable properties of the
     * local target object.
     * <p>
     * Multiple invocations with the same parameter may return the same
     * object again.
     *
     * @param  target the non-{@code null} local target.
     * @return A non-{@code null} output socket for writing to the local target.
     * @throws IOException If the local target
     *         is not accessible for some reason.
     * @throws NullPointerException if {@code target} is {@code null}.
     */
    OutputSocket<LT, PT> getOutputSocket(LT target)
    throws IOException;
}
