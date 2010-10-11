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
import de.schlichtherle.truezip.io.rof.ReadOnlyFileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Creates input streams and read only files for reading bytes from its
 * <i>local target</i>.
 * <p>
 * Note that the entity relationship between input sockets and output sockets
 * is n:1, i.e. an input socket can have at most one peer output socket, but
 * it may be the peer of many other output sockets.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client classes.
 *
 * @param   <CE> the type of the {@link #getTarget() local target} common entry.
 * @see     OutputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class InputSocket<CE extends CommonEntry>
extends IOSocket<CE, CommonEntry> {

    private OutputSocket<?> peer;

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
     * Makes this input socket sharing the peering with the given input
     * socket.
     * Note that this method does <em>not</em> change the peer's peer.
     *
     * @param  with the non-{@code null} input socket which has a peering to
     *         share.
     * @throws NullPointerException if {@code with} is {@code null}.
     * @return This input socket, cast to {@code IS}.
     * @see    #beforePeering
     * @see    #afterPeering
     */
	public final InputSocket<CE> share(final InputSocket<?> with) {
        final OutputSocket<?> newPeer = with.peer;
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
     * Connects this input socket to the given output socket.
     * Note that this method <em>does</em> change the peer's peer to this
     * instance.
     *
     * @param  newPeer the nullable output socket to connect to.
     * @return This input socket, cast to {@code IS}.
     * @see    #beforePeering
     * @see    #afterPeering
     */
	public final InputSocket<CE> connect(final OutputSocket<?> newPeer) {
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
     * Returns a new input stream for reading bytes from the
     * {@link #getTarget() local target}.
     * <p>
     * Implementations must enable calling this method any number of times.
     * Furthermore, the returned input stream should <em>not</em> be buffered.
     * Buffering should be addressed by client applications instead.
     *
     * @throws CommonInputBusyException if the local target is currently busy
     *         on input.
     *         This exception is guaranteed to be recoverable, meaning it
     *         should be possible to write the common entry again as soon as
     *         the local target is not busy anymore.
     * @throws FileNotFoundException if the local target is not accessible
     *         for some reason.
     * @throws IOException on any other exceptional condition.
     * @return A new input stream.
     */
    public InputStream newInputStream() throws IOException {
        return new ReadOnlyFileInputStream(newReadOnlyFile());
    }

    /**
     * <b>Optional:</b> Returns a new read only file for reading bytes from the
     * {@link #getTarget() local target} in arbitrary order.
     * <p>
     * If this method is supported, implementations must enable calling it
     * any number of times.
     * Furthermore, the returned read only file should <em>not</em> be buffered.
     * Buffering should be addressed by client applications instead.
     *
     * @throws UnsupportedOperationException to indicate that this operation
     *         is not supported.
     * @throws CommonInputBusyException if the local target is currently busy
     *         on input.
     *         This exception is guaranteed to be recoverable, meaning it
     *         should be possible to write the common entry again as soon as
     *         the local target is not busy anymore.
     * @throws FileNotFoundException if the local target is not accessible
     *         for some reason.
     * @throws IOException on any other exceptional condition.
     * @return A new read only file.
     */
    public abstract ReadOnlyFile newReadOnlyFile() throws IOException;
}
