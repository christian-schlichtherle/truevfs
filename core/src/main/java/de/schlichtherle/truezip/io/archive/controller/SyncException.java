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

package de.schlichtherle.truezip.io.archive.controller;

import de.schlichtherle.truezip.io.ChainableIOException;

/**
 * Indicates an exceptional condition when synchronizing the changes in a
 * virtual file system to its underlying file system.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class SyncException extends ChainableIOException {

    private static final long serialVersionUID = 4893219420357369739L;

    /** For exclusive use by {@link DefaultSyncExceptionBuilder}. */
    public SyncException(String message) {
        super(message);
    }

    SyncException(ArchiveController<?> controller, Throwable cause) {
        super(controller.getModel().getMountPoint().getPath(), cause);
    }

    SyncException(ArchiveController<?> controller, Throwable cause, int priority) {
        super(controller.getModel().getMountPoint().getPath(), cause, priority);
    }
}
