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
 * Thrown to indicate that an authenticated ZIP entry has been tampered with.
 *
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class ZipAuthenticationException extends ZipCryptoException {
    private static final long serialVersionUID = 2403462923846291232L;

    /**
     * Constructs a ZIP authentication exception with the given detail message.
     */
    public ZipAuthenticationException(String msg) {
        super(msg);
    }
}
