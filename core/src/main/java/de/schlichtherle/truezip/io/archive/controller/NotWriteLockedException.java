/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.archive.controller;

import de.schlichtherle.truezip.io.TemporarilyNotFoundException;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import java.io.IOException;

/**
 * Indicates that a controller is not write locked and the lock cannot get
 * acquired by the current thread for some reason.
 * Note that the write lock is required for any change to the state of the
 * archive controller - not only the state of the archive file system.
 * <p>
 * While this exception could arguably be a {@link RuntimeException} too, it
 * has been decided to subclass {@link IOException} for the following reasons:
 * <ol>
 * <li>This exceptional condition is defined to be recoverable and hence
 *     indicates the use of a checked exception.
 *     In contrast, a runtime exception is not defined to be recoverable and
 *     accordingly most code is not designed to be reentrant once a runtime
 *     exception has occured.
 * <li>Exceptions of this class must pass calls to the methods of the
 *     {@link InputSocket} and {@link OutputSocket} classes.
 *     {@link IOException} is the only suitable exception type for this
 *     purpose.
 * </ol>
 *
 * @see     FalsePositiveException
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@SuppressWarnings("serial") // serializing an exception for a temporary event is nonsense!
final class NotWriteLockedException extends TemporarilyNotFoundException {

    NotWriteLockedException() {
    }

    NotWriteLockedException(NotWriteLockedException ex) {
        super(ex);
    }
}
