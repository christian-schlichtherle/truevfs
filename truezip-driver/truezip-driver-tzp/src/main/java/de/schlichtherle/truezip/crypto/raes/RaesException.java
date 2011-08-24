/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
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

import java.io.IOException;
import net.jcip.annotations.ThreadSafe;

/**
 * Thrown if there is an issue when reading or writing a RAES file which is
 * specific to the RAES file format.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class RaesException extends IOException {
    private static final long serialVersionUID = 8564203786508562247L;

    /**
     * Constructs a RAES exception with
     * no detail message.
     */
    public RaesException() {
    }

    /**
     * Constructs a RAES exception with
     * the given detail message.
     *
     * @param msg the detail message.
     */
    public RaesException(String msg) {
        super(msg);
    }

    /**
     * Constructs a RAES exception with
     * the given detail message and cause.
     *
     * @param msg the detail message.
     * @param cause the cause for this exception to be thrown.
     */
    public RaesException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * Constructs a RAES exception with
     * the given cause.
     *
     * @param cause the cause for this exception to get thrown.
     */
    public RaesException(Throwable cause) {
        super(cause);
    }
}
