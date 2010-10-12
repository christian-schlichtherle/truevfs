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

import java.io.IOException;

/**
 * Thrown if a file is read which is not RAES compatible.
 * This is a subclass of {@link IOException} to indicate that this
 * is considered to be an issue when accessing the contents of a file.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class RaesException extends IOException {
    private static final long serialVersionUID = 8564203786508562247L;

    /**
     * Constructs an instance of {@code RaesException} with the specified
     * detail message.
     *
     * @param msg The detail message.
     */
    public RaesException(String msg) {
        super(msg);
    }

    /**
     * Constructs an instance of {@code RaesException} with the specified
     * detail message and cause.
     *
     * @param msg The detail message.
     * @param cause The original cause for this exception to be thrown.
     */
    public RaesException(String msg, Throwable cause) {
        super(msg);
        super.initCause(cause);
    }
}
