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
import java.io.IOException;
import java.io.InputStream;

/**
 * Creates input streams and read only files for reading bytes from its
 * {@link #getTarget local target}.
 * An input socket can also get {@link #connect connected} to a
 * {@link #getPeerTarget peer target} for {@link IOSocket#copy data copying}.
 * 
 * @param   <LT> The type of the {@link #getTarget local target} for I/O
 *          operations.
 * @param   <PT> The type of the {@link #getPeerTarget peer target} for I/O
 *          operations.
 * @see     OutputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class InputSocket<LT, PT> extends IOSocket<LT> {

    private OutputSocket<? extends PT, ? super LT> peer;

    public InputSocket<LT, PT> chain(InputSocket<? super LT, ? extends PT> from) {
        chain0(from.peer);
        return this;
    }

    private void chain0(final OutputSocket<? extends PT, ? super LT> newPeer) {
        final OutputSocket<? extends PT, ? super LT> oldPeer = peer;
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
    }

    public InputSocket<LT, PT> connect(
            final OutputSocket<? extends PT, ? super LT> newPeer) {
        connect0(newPeer);
        return this;
    }

    void connect0(final OutputSocket<? extends PT, ? super LT> newPeer) {
        final OutputSocket<? extends PT, ? super LT> oldPeer = peer;
        if (!equal(oldPeer, newPeer)) {
            try {
                peer = null;
                if (null != oldPeer)
                    oldPeer.connect0(null);
                beforePeering();
                peer = newPeer;
                if (null != newPeer)
                    newPeer.connect0(this);
                afterPeering();
            } catch (RuntimeException ex) {
                peer = oldPeer;
                if (null != oldPeer)
                    oldPeer.connect0(this);
                throw ex;
            }
        }
    }

    protected void beforePeering() {
    }

    protected void afterPeering() {
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
     * @return A new read only file.
     * @throws UnsupportedOperationException to indicate that this operation
     *         is not supported.
     */
    public abstract ReadOnlyFile newReadOnlyFile() throws IOException;
}
