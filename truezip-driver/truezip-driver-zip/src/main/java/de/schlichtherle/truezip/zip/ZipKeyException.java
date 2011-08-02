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
 * Thrown to indicate that retrieving a key to encrypt or decrypt a ZIP entry
 * has failed for some reason.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class ZipKeyException extends ZipCryptoParametersException {
    private static final long serialVersionUID = 5762312735142938698L;

    /**
     * Creates a ZIP key exception with
     * a detail message indicating that ZIP entry key retrieval has failed.
     */
    public ZipKeyException() {
        super("ZIP entry key retrieval has failed!");
    }
    
    /**
     * Creates a ZIP key exception with
     * the given detail message.
     * 
     * @param msg the detail message.
     */
    public ZipKeyException(String msg) {
        super(msg);
    }

    /**
     * Creates a ZIP key exception with
     * the given cause.
     * 
     * @param cause the cause for this exception to get thrown.
     */
    public ZipKeyException(Throwable cause) {
        super(cause);
    }
}
