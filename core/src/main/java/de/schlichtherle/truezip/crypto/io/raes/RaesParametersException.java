/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.crypto.io.raes;

import java.io.FileNotFoundException;

/**
 * Thrown to indicate that no suitable RAES parameters have been provided or
 * something is wrong with the parameters.
 * This is a subclass of {@link FileNotFoundException} to indicate that this
 * is considered to be an issue when opening an RAES file rather than accessing
 * it contents (which would usually throw an {@code IOException},
 * but not a {@code FileNotFoundException}).
 *
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.0
 */
public class RaesParametersException extends FileNotFoundException {
    private static final long serialVersionUID = 1605398165986459281L;

    /**
     * Creates a new instance of {@code RaesParametersException} with a
     * detail message indicating that no suitable {@link RaesParameters} have
     * been found.
     */
    public RaesParametersException() {
        super("No suitable RaesParameters provided!");
    }

    /**
     * Constructs an instance of {@code RaesParametersException} with the
     * specified detail message.
     *
     * @param msg The detail message.
     */
    public RaesParametersException(String msg) {
        super(msg);
    }

    /**
     * Constructs an instance of {@code RaesParametersException} with the
     * specified cause.
     *
     * @param cause The cause.
     */
    public RaesParametersException(Throwable cause) {
        super(cause != null ? cause.toString() : null);
        initCause(cause);
    }
}
