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
import java.io.InputStream;

/**
 * Creates input streams for reading bytes from its local target.
 * 
 * @param   <LT> The type of the <i>local target</i> for I/O operations,
 *          i.e. the {@link #get() target} of this instance.
 * @param   <PT> The minimum required type of the <i>peer targets</i>.
 * @see     InputStreamSocketProvider
 * @see     OutputStreamSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface InputStreamSocket<LT, PT> extends IOReference<LT> {

    /**
     * Returns the non-{@code null} local target for I/O operations.
     * <p>
     * The result of changing the state of the returned object is undefined.
     * In particular, a subsequent call to {@link #newInputStream(IOReference)}
     * may not reflect any changes or may even fail.
     * However, this term may be overridden by sub-interfaces or
     * implementations.
     *
     * @return The non-{@code null} local target for I/O operations.
     */
    @Override
    LT get();

    /**
     * Returns a new {@code InputStream} for reading bytes from the
     * {@link #get() local target}.
     * <p>
     * Implementations must support calling this method any number of times.
     * Furthermore, the returned stream should <em>not</em> be buffered.
     * Buffering should be addressed by client applications instead.
     *
     * @param  peer the nullable peer target.
     *         If this is {@code null}, then there is no peer target.
     * @return A new {@code InputStream}.
     * @see    IOReferences#ref(Object) How to create a nullable I/O reference.
     */
    InputStream newInputStream(PT peer) throws IOException;
}
