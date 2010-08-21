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

/**
 * Thrown to indicate that an RAES file has been tampered with.
 * This is a subclass of {@link RaesException} to indicate that this
 * is considered to be an RAES specific issue when accessing the contents
 * of a file.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.0
 */
public class RaesAuthenticationException extends RaesException {
    private static final long serialVersionUID = 2362389234686232732L;

    public RaesAuthenticationException() {
        super("File has been tampered with!");
    }
}
