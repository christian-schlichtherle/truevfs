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
import java.io.IOException;
import java.io.OutputStream;

/**
 * Creates output streams for writing bytes to its <i>local target</i>.
 * <p>
 * Note that the entity relationship between output sockets and input sockets
 * is n:1, i.e. an output socket can have at most one peer input socket, but
 * it may be the peer of many other input sockets.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client classes.
 *
 * @param   <CE> the type of the {@link #getTarget() local target} common entry.
 * @see     InputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class OutputSocket<CE extends CommonEntry>
extends IOSocket<CE, CommonEntry> {

    private InputSocket<?> peer;

    /**
     * Returns the non-{@code null} local common entry target.
     *
     * @return The non-{@code null} local common entry target.
     */
    @Override
    public abstract CE getTarget();

    @Override
    public CommonEntry getPeerTarget() {
        return null == peer ? null : peer.getTarget();
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
	public final OutputSocket<CE> share(final OutputSocket<?> with) {
        final InputSocket<?> newPeer = with.peer;
        final InputSocket<?> oldPeer = peer;
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
        return this;
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
	public final OutputSocket<CE> connect(final InputSocket<?> newPeer) {
        final InputSocket<?> oldPeer = peer;
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
        return this;
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
     * @throws CommonOuputBusyException if the local target is currently busy
     *         on output.
     *         This exception is guaranteed to be recoverable, meaning it
     *         should be possible to write the common entry again as soon as
     *         the local target is not busy anymore.
     * @throws FileNotFoundException if the local target is not accessible
     *         for some reason.
     * @throws IOException on any other exceptional condition.
     * @return A new output stream.
     */
    public abstract OutputStream newOutputStream() throws IOException;
}
