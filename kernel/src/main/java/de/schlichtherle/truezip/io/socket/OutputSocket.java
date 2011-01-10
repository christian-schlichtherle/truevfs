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

import de.schlichtherle.truezip.entry.Entry;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import net.jcip.annotations.NotThreadSafe;

/**
 * Creates output streams for writing bytes to its <i>local target</i>.
 * <p>
 * Note that the entity relationship between output sockets and input sockets
 * is n:1, i.e. any output socket can have at most one peer input socket, but
 * it may be the peer of many other input sockets.
 * <p>
 * In general, implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client classes.
 *
 * @param   <E> the type of the {@link #getLocalTarget() local target}
 *          for I/O operations.
 * @see     InputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public abstract class OutputSocket<E extends Entry>
extends IOSocket<E, Entry> {

    @CheckForNull
    private InputSocket<?> peer;

    /**
     * {@inheritDoc}
     * <p>
     * The peer target is {@code null} if and only if this socket is not
     * {@link #connect}ed to another socket.
     */
    @Override
    @Nullable
    public Entry getPeerTarget() throws IOException {
        return null == peer ? null : peer.getLocalTarget();
    }

    /**
     * Binds the peer target of the given socket to this socket.
     * Note that this method does <em>not</em> change the state of the
     * given socket and does <em>not</em> connect this socket to the peer
     * socket, i.e. it does not set this socket as the peer of of the given
     * socket.
     *
     * @param  to the output socket which has a peer target to share.
     * @return {@code this}
     */
    @NonNull
    public final OutputSocket<E> bind(@CheckForNull final OutputSocket<?> to) {
        peer = null == to ? null : to.peer;
        return this;
    }

    /**
     * Connects this output socket to the given peer input socket.
     * Note that this method changes the peer output socket of
     * the given peer input socket to this instance.
     *
     * @param  newPeer the nullable peer input socket to connect to.
     * @return This output socket.
     */
    @NonNull
    final OutputSocket<E> connect(@CheckForNull final InputSocket<?> newPeer) {
        final InputSocket<?> oldPeer = peer;
        if (oldPeer != newPeer) {
            peer = null;
            if (null != oldPeer)
                oldPeer.connect(null);
            peer = newPeer;
            if (null != newPeer)
                newPeer.connect(this);
        }
        return this;
    }

    /**
     * Returns a new output stream for writing bytes to the
     * {@link #getLocalTarget() local target}.
     * <p>
     * Implementations must enable calling this method any number of times.
     * Furthermore, the returned output stream should <em>not</em> be buffered.
     * Buffering should be addressed by the caller instead - see
     * {@link IOSocket#copy}.
     *
     * @throws FileNotFoundException if the local target is not accessible
     *         for some reason.
     * @throws IOException on any other exceptional condition.
     * @return A new output stream.
     */
    @NonNull
    public abstract OutputStream newOutputStream() throws IOException;
}
