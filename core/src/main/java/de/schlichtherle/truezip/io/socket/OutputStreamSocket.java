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
import java.io.OutputStream;

/**
 * Creates output streams for writing bytes to its target.
 *
 * @param   <LT> The type of the <i>local target</i>,
 *          i.e. the {@link #getTarget() target} of this instance.
 * @param   <PT> The minimum required type of the <i>peer targets</i> for
 *          writing to the local target.
 * @see OutputStreamSocketProvider
 * @see InputStreamSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface OutputStreamSocket<LT, PT> extends IOReference<LT> {

    /**
     * Returns a non-{@code null} reference to a new {@code OutputStream} for
     * writing bytes to the the {@link #getTarget() local target}.
     * <p>
     * The returned stream should <em>not</em> be buffered.
     * Buffering should be addressed by client applications instead.
     *
     * @param  source a non-{@code null} reference to the input peer
     *         target.
     * @return A non-{@code null} reference to a new {@code OutputStream}.
     */
    OutputStream newOutputStream(IOReference<? extends PT> source)
    throws IOException;
}
