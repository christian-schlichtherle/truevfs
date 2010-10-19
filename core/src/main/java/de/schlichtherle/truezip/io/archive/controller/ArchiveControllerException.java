/*
 * Copyright (C) 2004-2010 Schlichtherle IT Services
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

import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import java.io.IOException;

/**
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
 * @author  Christian Schlichtherle
 * @version $Id$
 */
abstract class ArchiveControllerException extends IOException {
    private static final long serialVersionUID = 2947623946725372554L;

    ArchiveControllerException() {
        super.initCause(null);
    }

    ArchiveControllerException(IOException cause) {
        super.initCause(cause);
    }

    /** Returns the nullable cause of this exception. */
    @Override
    public IOException getCause() {
        return (IOException) super.getCause();
    }
}
