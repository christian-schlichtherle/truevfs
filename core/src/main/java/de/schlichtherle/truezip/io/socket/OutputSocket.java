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
 * Creates output streams for writing bytes to its
 * {@link #getTarget local target}.
 * An output socket can also get {@link #connect connected} to a
 * {@link #getPeerTarget peer target} for {@link IOSocket#copy data copying}.
 *
 * @param   <LT> The type of the {@link #getTarget local target} for I/O
 *          operations.
 * @param   <PT> The type of the {@link #getPeerTarget peer target} for I/O
 *          operations.
 * @see     InputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class OutputSocket<LT, PT> extends IOSocket<LT> {

    private InputSocket<? extends PT, ? super LT> peer;

    public OutputSocket<LT, PT> chain(OutputSocket<? super LT, ? extends PT> output) {
        return connect(output.peer);
    }

    public OutputSocket<LT, PT> connect(
            final InputSocket<? extends PT, ? super LT> newPeer) {
        final InputSocket<? extends PT, ? super LT> oldPeer = peer;
        if (!equal(oldPeer, newPeer)) {
            peer = newPeer;
            beforeConnectComplete();
            if (null != newPeer)
                newPeer.connect(this);
            afterConnectComplete();
        }
        return this;
    }

    protected void beforeConnectComplete() {
    }

    protected void afterConnectComplete() {
    }

    private static boolean equal(Object o1, Object o2) {
        return o1 == o2 || null != o1 && o1.equals(o2);
    }

    protected final InputSocket<? extends PT, ? super LT> getPeer() {
        return peer;
    }

    /**
     * Returns the nullable <i>peer target</i> for I/O operations.
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
     * Returns a new output stream for writing bytes to the
     * {@link #getTarget() local target}.
     * <p>
     * Implementations must enable calling this method any number of times.
     * Furthermore, the returned output stream should <em>not</em> be buffered.
     * Buffering should be addressed by client applications instead.
     *
     * @return A new output stream.
     */
    public abstract OutputStream newOutputStream() throws IOException;
}
