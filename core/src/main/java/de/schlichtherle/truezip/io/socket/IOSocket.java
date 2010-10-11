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

import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.Streams;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Targets an object for I/O operations which are provided by sub classes.
 * <p>
 * A key feature of an I/O socket is that it's targets can be resolved eagerly
 * or lazily, i.e. the local or remote target may get resolved by a constructor
 * or a method of a sub class.
 *
 * @param   <LT> the type of the {@link #getLocalTarget() local target}
 *          for I/O operations.
 * @param   <RT> the type of the {@link #getRemoteTarget() remote target}
 *          for I/O operations.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class IOSocket<LT, RT> {

    /**
     * Returns the non-{@code null} <i>local target</i> for I/O operations.
     * <p>
     * The result of changing the state of the local target is undefined.
     * In particular, a subsequent I/O operation may not reflect the change
     * or may even fail.
     * This term may be overridden by sub-interfaces or implementations.
     *
     * @return The non-{@code null} local target for I/O operations.
     */
	public abstract LT getLocalTarget() throws IOException;

    /**
     * Returns the nullable <i>remote target</i> for I/O operations.
     * <p>
     * The result of changing the state of the remote target is undefined.
     * In particular, a subsequent I/O operation may not reflect the change
     * or may even fail.
     * This term may be overridden by sub-interfaces or implementations.
     *
     * @return The nullable remote target for I/O operations.
     */
    public abstract RT getRemoteTarget() throws IOException;

    /**
     * Returns a string representing a connection of the local and remote
     * targets.
     */
    @Override
    public final String toString() {
        // Note that the target actually must not be null, but this method
        // should work even if the interface contract is broken in order to
        // support debugging.
        String lts;
        try {
            final LT lt = getLocalTarget();
            lts = null == lt ? "(null)" : lt.toString();
        } catch (IOException ex) {
            lts = "(?)";
        }
        String rts;
        try {
            final RT rt = getRemoteTarget();
            rts = null == rt ? "(null)" : rt.toString();
        } catch (IOException ex) {
            rts = "(?)";
        }
        return lts + " <-> " + rts;
    }

    static boolean equal(Object o1, Object o2) {
        return o1 == o2 || null != o1 && o1.equals(o2);
    }

    /**
     * Copies an input stream {@link InputSocket#newInputStream created}
     * by the given input socket {@code input} to an output stream
     * {@link OutputSocket#newOutputStream created} by the given output socket
     * {@code output}.
     *
     * @param  input a non-{@code null} input socket for the input target.
     * @param  output a non-{@code null} output socket for the output target.
     * @throws InputException if copying the data fails because of an
     *         {@code IOException} thrown by the <em>input</em> stream.
     * @throws IOException if copying the data fails because of an
     *         {@code IOException} thrown by the <em>output</em> stream.
     * @throws NullPointerException if any parameter is {@code null}.
     */
    public static void copy(final InputSocket <?> input,
                            final OutputSocket<?> output)
    throws IOException {
        final InputStream in = input.connect(output).newInputStream();
        OutputStream out = null;
        try {
            out = output.connect(input).newOutputStream();
        } finally {
            if (out == null) { // exception?
                try {
                    in.close();
                } catch (IOException ex) {
                    throw new InputException(ex);
                }
            }
        }
        Streams.copy(in, out);
    }
}
