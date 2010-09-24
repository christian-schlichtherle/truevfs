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

import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import java.io.IOException;
import java.io.InputStream;

/**
 * Creates input streams for reading bytes from its local target.
 * 
 * @param   <LT> The type of the {@link #getTarget() local target} for I/O
 *          operations.
 * @param   <PT> The type of the {@link #getPeerTarget() peer target} for I/O
 *          operations.
 * @see     InputSocketProvider
 * @see     OutputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class InputSocket<LT, PT> extends IOSocket<LT> {

    private OutputSocket<? extends PT, ? super LT> peer;

    public InputSocket<LT, PT> chain(InputSocket<? super LT, ? extends PT> input) {
        return peer(input.peer);
    }

    public InputSocket<LT, PT> peer(
            final OutputSocket<? extends PT, ? super LT> newPeer) {
        final OutputSocket<? extends PT, ? super LT> oldPeer = peer;
        if (!equal(oldPeer, newPeer)) {
            peer = newPeer;
            beforePeeringComplete();
            if (null != newPeer)
                newPeer.peer(this);
            afterPeeringComplete();
        }
        return this;
    }

    protected void beforePeeringComplete() {
    }

    protected void afterPeeringComplete() {
    }

    private static boolean equal(IOSocket<?> o1, IOSocket<?> o2) {
        return o1 == o2 || null != o1 && o1.equals(o2);
    }

    /**
     * Returns the nullable peer target for I/O operations.
     * <p>
     * The result of changing the state of the peer target is undefined.
     * In particular, a subsequent I/O operation may not reflect the change
     * or may even fail.
     * This term may be overridden by sub-interfaces or implementations.
     *
     * @return The nullable peer target for I/O operations.
     */
    public PT getPeerTarget() {
        return IOReferences.deref(peer);
    }

    /**
     * Returns a new input stream for reading bytes from the
     * {@link #getTarget() local target}.
     * <p>
     * Implementations must enable calling this method any number of times.
     * Furthermore, the returned input stream should <em>not</em> be buffered.
     * Buffering should be addressed by client applications instead.
     *
     * @return A new input stream.
     */
    public abstract InputStream newInputStream() throws IOException;

    /**
     * <b>Optional:</b> Returns a new read only file for reading bytes from the
     * {@link #getTarget() local target} in arbitrary order.
     * <p>
     * If this method is supported, implementations must enable calling it
     * any number of times.
     * Furthermore, the returned read only file should <em>not</em> be buffered.
     * Buffering should be addressed by client applications instead.
     *
     * @return A new read only file.
     * @throws UnsupportedOperationException to indicate that this operation
     *         is not (yet) supported.
     */
    public ReadOnlyFile newReadOnlyFile() throws IOException {
        throw new UnsupportedOperationException();
    }
}
