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
 * Provides static utility methods for dealing with I/O operations.
 * This class cannot get instantiated outside its package.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class IOSockets {

    IOSockets() {
    }

    /**
     * Copies an input stream {@link InputSocket#newInputStream created}
     * by the input socket {@code input} to an output stream
     * {@link OutputSocket#newOutputStream created} by the output socket
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
    public static <T> void copy(
            final InputSocket <? extends T, T> input,
            final OutputSocket<? extends T, T> output)
    throws IOException {
        final InputStream in;
        //try {
            in = input.peer(output).newInputStream();
        /*} catch (IOException ex) {
            throw new InputException(ex); // DONT WRAP THE CAUSE: would break false positive archive file detection!
        }*/
        OutputStream out = null;
        try {
            out = output.peer(input).newOutputStream();
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
