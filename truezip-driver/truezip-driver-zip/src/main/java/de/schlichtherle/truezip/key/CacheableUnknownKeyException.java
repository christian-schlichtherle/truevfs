/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.key;

import net.jcip.annotations.ThreadSafe;

/**
 * Thrown to indicate that the retrieval of the key to open or create a
 * protected resource has failed and that this exception is cacheable.
 * The subclass provides more information.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class CacheableUnknownKeyException extends UnknownKeyException {
    private static final long serialVersionUID = 2463586348235337265L;

    CacheableUnknownKeyException() {
    }

    CacheableUnknownKeyException(String message) {
        super(message);
    }
}
