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

import net.jcip.annotations.ThreadSafe;

/**
 * Thrown to indicate that a RAES file has been tampered with.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class RaesAuthenticationException extends RaesException {
    private static final long serialVersionUID = 2362389234686232732L;

    /**
     * Constructs a RAES exception with
     * a detail message indicating that a RAES file has been tampered with.
     */
    public RaesAuthenticationException() {
        super("Authenticated file content has been tampered with!");
    }
}
