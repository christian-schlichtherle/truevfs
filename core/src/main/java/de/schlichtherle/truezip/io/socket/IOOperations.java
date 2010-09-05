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

import de.schlichtherle.truezip.io.util.InputException;
import de.schlichtherle.truezip.io.util.Streams;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Provides static utility methods for dealing with I/O operations.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class IOOperations {

    /** You cannot instantiate this class. */
    IOOperations() {
    }

    /**
     * Copies an input stream {@link InputStreamSocket#newInputStream created}
     * by the input stream socket {@code input} to an output stream
     * {@link OutputStreamSocket#newOutputStream created} by the output stream
     * socket {@code output}.
     *
     * @param  input a non-{@code null} reference to the stream socket of the
     *         input target.
     * @param  output a non-{@code null} reference to the stream socket of the
     *         output target.
     * @throws InputException if copying the data fails because of an
     *         {@code IOException} thrown by the <em>input</em> stream.
     * @throws IOException if copying the data fails because of an
     *         {@code IOException} thrown by the <em>output</em> stream.
     * @throws NullPointerException if any parameter is {@code null}.
     */
    public static <T> void copy(
            final InputStreamSocket<? extends T, ? super T> input,
            final OutputStreamSocket<? extends T, ? super T> output)
    throws IOException {
        final InputStream is;
        final OutputStream os;
        try {
            is = input.newInputStream(output);
        } catch (IOException ex) {
            throw new InputException(ex);
        }
        try {
            os = output.newOutputStream(input);
        } catch (IOException ex) {
            try {
                is.close(); // ignore any exception! // FIXME: Not executed on RuntimeException or Error!!!
            } finally {
                throw ex;
            }
        }
        Streams.copy(is, os);
    }
}
