/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io;

import java.io.FileNotFoundException;

/**
 * Indicates that a file is temporarily not accessible, e.g. if a key for
 * decryption is not available.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@SuppressWarnings("serial") // serializing an exception for a temporary event is nonsense!
public class TemporarilyNotFoundException extends FileNotFoundException {

    /**
     * Constructs a temporarily-not-found-exception.
     */
    public TemporarilyNotFoundException() {
    }

    /**
     * Constructs a temporarily-not-found-exception.
     *
     * @param cause The nullable temporary cause of this exception.
     */
    public TemporarilyNotFoundException(Throwable cause) {
        super.initCause(cause);
    }
}
