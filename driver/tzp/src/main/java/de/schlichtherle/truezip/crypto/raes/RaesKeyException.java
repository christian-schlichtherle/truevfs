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
package de.schlichtherle.truezip.crypto.raes;

/**
 * Thrown to indicate that retrieving a key to encrypt some pay load data to
 * an RAES file has been disabled or cancelled.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class RaesKeyException extends RaesParametersException {
    private static final long serialVersionUID = 1375629384612351398L;

    /**
     * Creates a new instance of {@code RaesKeyException} with a
     * detail message indicating that key retrieval has been disabled
     * or cancelled.
     */
    public RaesKeyException() {
        super("Key retrieval has been disabled or cancelled!");
    }
    
    /**
     * Constructs an instance of {@code RaesKeyException} with the
     * specified detail message.
     * 
     * @param msg The detail message.
     */
    public RaesKeyException(String msg) {
        super(msg);
    }

    /**
     * Constructs an instance of {@code RaesKeyException} with the
     * specified cause.
     * 
     * @param cause The cause.
     */
    public RaesKeyException(Throwable cause) {
        super(cause);
    }
}
