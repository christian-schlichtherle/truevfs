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

import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.rof.ReadOnlyFileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Creates input streams and read only files for reading bytes from its
 * <i>local target</i>.
 * <p>
 * Note that the entity relationship between input sockets and output sockets
 * is n:1, i.e. any input socket can have at most one peer output socket, but
 * it may be the peer of many other output sockets.
 * <p>
 * In general, implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client classes.
 *
 * @param   <LT> the type of the {@link #getLocalTarget() local target}
 *          for I/O operations.
 * @see     OutputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class InputSocket<LT extends Entry>
extends IOSocket<LT, Entry> {

    private OutputSocket<?> peer;

    @Override
    public Entry getPeerTarget() throws IOException {
        return null == peer ? null : peer.getLocalTarget();
    }

    /**
     * Makes the given input socket share its peer target with this input
     * socket.
     * Note that this method does <em>not</em> change the peer input socket of
     * the given input socket's peer output socket to this instance, i.e. this
     * input socket is not connected to the peer output socket.
     *
     * @param  to the non-{@code null} input socket which has a remote
     *         target to share.
     * @throws NullPointerException if {@code with} is {@code null}.
     * @return This input socket.
     * @see    #beforePeering
     * @see    #afterPeering
     */
	public final InputSocket<LT> bind(final InputSocket<?> to) {
        final OutputSocket<?> newPeer = to.peer;
        final OutputSocket<?> oldPeer = peer;
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
     * Connects this input socket to the given peer output socket.
     * Note that this method changes the peer input socket of
     * the given peer output socket to this instance.
     *
     * @param  newPeer the nullable peer output socket to connect to.
     * @return This input socket.
     * @see    #beforePeering
     * @see    #afterPeering
     */
	final InputSocket<LT> connect(final OutputSocket<?> newPeer) {
        final OutputSocket<?> oldPeer = peer;
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
     * Called by {@link #bind} and {@link #connect} after a peering has been
     * initiated, but before the peer output socket has been changed.
     */
    protected void beforePeering() {
    }

    /**
     * Called by {@link #bind} and {@link #connect} after a peering has been
     * completed and the peer output socket has been successfully changed.
     */
    protected void afterPeering() {
    }

    /**
     * <b>Optional:</b> Returns a new read only file for reading bytes from
     * the {@link #getLocalTarget() local target} in arbitrary order.
     * <p>
     * If this method is supported, implementations must enable calling it
     * any number of times.
     * Furthermore, the returned read only file should <em>not</em> be buffered.
     * Buffering should be addressed by client applications instead.
     *
     * @throws UnsupportedOperationException if this operation is not supported
     *         by the implementation.
     * @throws FileNotFoundException if the local target does not exist or is
     *         not accessible for some reason.
     * @throws IOException on any other exceptional condition.
     * @return A new read only file.
     */
    public abstract ReadOnlyFile newReadOnlyFile() throws IOException;

    /**
     * Returns a new input stream for reading bytes from the
     * {@link #getLocalTarget() local target}.
     * <p>
     * Implementations must enable calling this method any number of times.
     * Furthermore, the returned input stream should <em>not</em> be buffered.
     * Buffering should be addressed by client applications instead.
     * <p>
     * The implementation in the class {@link InputSocket} calls
     * {@link #newReadOnlyFile()} and wraps the resulting object in a new
     * {@link ReadOnlyFileInputStream} as an adapter.
     *
     * @throws FileNotFoundException if the local target does not exist or is
     *         not accessible for some reason.
     * @throws IOException on any other exceptional condition.
     * @return A new input stream.
     */
    public InputStream newInputStream() throws IOException {
        return new ReadOnlyFileInputStream(newReadOnlyFile());
    }
}
