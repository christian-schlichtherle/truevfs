/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.zip;

import net.jcip.annotations.ThreadSafe;

/**
 * Thrown to indicate that no suitable ZIP crypto parameters have been provided
 * or something is wrong with these parameters.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class ZipCryptoParametersException extends ZipCryptoException {
    private static final long serialVersionUID = 2036769236408934681L;

    /**
     * Constructs a RAES parameters exception with
     * a detail message indicating that no suitable {@link ZipCryptoParameters}
     * have been found.
     */
    public ZipCryptoParametersException() {
        super("No suitable crypto parameters available!");
    }

    /**
     * Constructs a RAES parameters exception with
     * the given detail message.
     *
     * @param msg the detail message.
     */
    public ZipCryptoParametersException(String msg) {
        super(msg);
    }

    /**
     * Constructs a RAES parameters exception with
     * the given cause.
     *
     * @param cause the cause for this exception to get thrown.
     */
    public ZipCryptoParametersException(Throwable cause) {
        super(cause);
    }
}
