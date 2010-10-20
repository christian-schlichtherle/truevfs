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

import de.schlichtherle.truezip.io.ChainableIOException;
import java.io.IOException;

/**
 * Indicates an exceptional condition in an archive controller.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
abstract class ArchiveControllerException extends ChainableIOException {
    private static final long serialVersionUID = 2947623946725372554L;

    /** For exclusive use by {@link DefaultSyncExceptionBuilder}. */
    public ArchiveControllerException(String message) {
        super(message);
    }

    ArchiveControllerException(ArchiveModel model) {
        super();
    }

    ArchiveControllerException(ArchiveModel model, Throwable cause) {
        super(model.getMountPoint().getPath(), cause);
    }

    ArchiveControllerException(ArchiveModel model, Throwable cause, int priority) {
        super(model.getMountPoint().getPath(), cause, priority);
    }

    /** Returns the nullable cause of this exception. */
    @Override
    public IOException getCause() {
        return (IOException) super.getCause();
    }
}
