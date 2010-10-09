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
 * @param   <LT> the type of the {@link #getTarget local target} for I/O
 *          operations.
 * @param   <PT> the type of the {@link #getPeerTarget peer target} for I/O
 *          operations.
 * @param   <OS> a subclass of this class to which {@code this} is cast upon
 *          return from the methods {@link #share} and {@link #connect}.
 * @see     InputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class OutputSocket<LT, PT, OS extends OutputSocket<LT, PT, OS>>
extends IOSocket<LT, PT> {

    private InputSocket<? extends PT, ? super LT, ?> peer;

    @Override
    public PT getPeerTarget() {
        return IOReferences.deref(peer);
    }

    /**
     * Makes this output socket sharing the peering with the given output
     * socket.
     * Note that this method does <em>not</em> change the peer's peer.
     *
     * @param  with the non-{@code null} output socket which has a peering to
     *         share.
     * @throws NullPointerException if {@code with} is {@code null}.
     * @return This output socket, cast to {@code OS}.
     * @see    #beforePeering
     * @see    #afterPeering
     */
    @SuppressWarnings("unchecked")
	public final OS share(OutputSocket<? super LT, ? extends PT, ?> with) {
        final InputSocket<? extends PT, ? super LT, ?> newPeer = with.peer;
        final InputSocket<? extends PT, ? super LT, ?> oldPeer = peer;
        if (!equal(oldPeer, newPeer)) {
            beforePeering();
            try {
                peer = newPeer;
                afterPeering();
            } catch (RuntimeException ex) {
                peer = oldPeer;
                throw ex;
            }
        }
        return (OS) this;
    }

    /**
     * Connects this output socket to the given input socket.
     * Note that this method <em>does</em> change the peer's peer to this
     * instance.
     *
     * @param  newPeer the nullable input socket to connect to.
     * @return This output socket, cast to {@code OS}.
     * @see    #beforePeering
     * @see    #afterPeering
     */
    @SuppressWarnings("unchecked")
	public final OS connect(
            final InputSocket<? extends PT, ? super LT, ?> newPeer) {
        final InputSocket<? extends PT, ? super LT, ?> oldPeer = peer;
        if (!equal(oldPeer, newPeer)) {
            try {
                peer = null;
                if (null != oldPeer)
                    oldPeer.connect(null);
                beforePeering();
                peer = newPeer;
                if (null != newPeer)
                    newPeer.connect(this);
                afterPeering();
            } catch (RuntimeException ex) {
                peer = oldPeer;
                if (null != oldPeer)
                    oldPeer.connect(this);
                throw ex;
            }
        }
        return (OS) this;
    }

    /**
     * Called by {@link #share} and {@link #connect} after a peering has been
     * initiated, but before it has been completed.
     */
    protected void beforePeering() {
    }

    /**
     * Called by {@link #share} and {@link #connect} after a peering has been
     * completed.
     */
    protected void afterPeering() {
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
