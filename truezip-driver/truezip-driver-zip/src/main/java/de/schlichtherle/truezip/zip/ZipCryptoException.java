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

import java.util.zip.ZipException;
import net.jcip.annotations.ThreadSafe;

/**
 * Thrown if there is an issue when reading or writing an encrypted ZIP file
 * or entry.
 *
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class ZipCryptoException extends ZipException {
    private static final long serialVersionUID = 2783693745683625471L;

    /**
     * Constructs a ZIP crypto exception with
     * no detail message.
     */
    public ZipCryptoException() {
    }

    /**
     * Constructs a ZIP crypto exception with
     * the given detail message.
     *
     * @param msg the detail message.
     */
    public ZipCryptoException(String msg) {
        super(msg);
    }

    /**
     * Constructs a ZIP crypto exception with
     * the given detail message and cause.
     *
     * @param msg the detail message.
     * @param cause the cause for this exception to be thrown.
     */
    public ZipCryptoException(String msg, Throwable cause) {
        super(msg);
        super.initCause(cause);
    }

    /**
     * Constructs a ZIP crypto exception with
     * the given cause.
     *
     * @param cause the cause for this exception to get thrown.
     */
    public ZipCryptoException(Throwable cause) {
        super.initCause(cause);
    }
}
