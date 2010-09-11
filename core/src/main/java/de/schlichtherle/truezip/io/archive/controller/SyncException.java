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

import de.schlichtherle.truezip.io.archive.ArchiveDescriptor;
import de.schlichtherle.truezip.io.ChainableIOException;
import java.net.URI;

/**
 * Indicates an exceptional condition when synchronizing the changes for an archive file to the underlying file system.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class SyncException extends ChainableIOException {

    private static final long serialVersionUID = 4893219420357369739L;

    private final URI mountPoint;

    SyncException(ArchiveDescriptor archive) {
        this.mountPoint = archive.getMountPoint();
    }

    SyncException(ArchiveDescriptor archive, String message) {
        super(message);
        this.mountPoint = archive.getMountPoint();
    }

    SyncException(ArchiveDescriptor archive, Throwable cause) {
        super(cause);
        this.mountPoint = archive.getMountPoint();
    }

    SyncException(ArchiveDescriptor archive, String message, Throwable cause) {
        super(message, cause);
        this.mountPoint = archive.getMountPoint();
    }

    SyncException(ArchiveDescriptor archive, int priority) {
        super(priority);
        this.mountPoint = archive.getMountPoint();
    }

    SyncException(ArchiveDescriptor archive, String message, int priority) {
        super(message, priority);
        this.mountPoint = archive.getMountPoint();
    }

    SyncException(ArchiveDescriptor archive, Throwable cause, int priority) {
        super(cause, priority);
        this.mountPoint = archive.getMountPoint();
    }

    SyncException(ArchiveDescriptor archive, String message, Throwable cause, int priority) {
        super(message, cause, priority);
        this.mountPoint = archive.getMountPoint();
    }

    /**
     * Equivalent to
     * {@code return (SyncException) super.initCause(cause);}.
     */
    @Override
    public SyncException initCause(final Throwable cause) {
        return (SyncException) super.initCause(cause);
    }

    /** @see ArchiveDescriptor#getMountPoint() */
    public final URI getMountPoint() {
        return mountPoint;
    }

    @Override
    public String getLocalizedMessage() {
        final String msg = getMessage();
        return msg != null
                ? new StringBuilder(getMountPoint().toString()).append(" (").append(msg).append(")").toString()
                : getMountPoint().toString();
    }
}
