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

import de.schlichtherle.truezip.io.filesystem.FileSystemModel;
import de.schlichtherle.truezip.io.ChainableIOException;
import java.io.IOException;
import java.net.URI;

/**
 * Indicates an exceptional condition when synchronizing the changes in a
 * virtual file system to its underlying file system.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class SyncException extends ChainableIOException {

    private static final long serialVersionUID = 4893219420357369739L;

    private final URI mountPoint;

    SyncException(ArchiveController controller, Throwable cause) {
        super(cause);
        this.mountPoint = controller.getModel().getMountPoint();
    }

    SyncException(ArchiveController controller, Throwable cause, int priority) {
        super(cause, priority);
        this.mountPoint = controller.getModel().getMountPoint();
    }

    /**
     * Equivalent to
     * {@code return (SyncException) super.initCause(cause);}.
     */
    @Override
    public final SyncException initCause(final Throwable cause) {
        return (SyncException) super.initCause(cause);
    }

    /** @see FileSystemModel#getMountPoint() */
    public final URI getMountPoint() {
        return mountPoint;
    }

    @Override
    public final String getLocalizedMessage() {
        final String msg = getMessage();
        return msg != null
                ? new StringBuilder(getMountPoint().toString()).append(" (").append(msg).append(")").toString()
                : getMountPoint().toString();
    }
}
