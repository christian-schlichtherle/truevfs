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

import de.schlichtherle.truezip.io.archive.ArchiveDescriptor;
import de.schlichtherle.truezip.io.archive.driver.TransientIOException;
import java.io.IOException;
import java.net.URI;

/**
 * Indicates a false positive archive entry which actually exists as a
 * file or directory entry in the real file system or in an enclosing archive
 * file.
 * <p>
 * Instances of this class are always associated with an {@code IOException}
 * as their cause.
 */
public class FalsePositiveException extends IOException {
    private static final long serialVersionUID = 947139561381472363L;

    private final URI mountPoint;
    private final String path;
    private final boolean cacheable;

    FalsePositiveException(
            final ArchiveDescriptor archive,
            final String path,
            final IOException cause) {
        super(cause.getMessage());
        assert cause != null;
        assert path != null;
        this.mountPoint = archive.getMountPoint();
        this.path = path;
        // A transient I/O exception is just a wrapper exception to mark
        // the real transient cause, therefore we can safely throw it away.
        // We must do this in order to allow an archive controller to inspect
        // the real transient cause and act accordingly.
        final boolean trans = cause instanceof TransientIOException;
        super.initCause(trans ? cause.getCause() : cause);
        cacheable = !trans;
    }

    /** @see ArchiveDescriptor#getMountPoint() */
    public final URI getMountPoint() {
        return mountPoint;
    }

    public final String getPath() {
        return path;
    }

    /**
     * Returns the <em>canonical path</em> of the target entity which caused
     * this exception to be created when processing it.
     * A canonical path is absolute, hierarchical and unique within the
     * federated file system.
     *
     * @return A non-{@code null} URI representing the canonical path of the
     *         target entity in the federated file system.
     */
    public final String getCanonicalPath() {
        return mountPoint.resolve(path).toString();
    }

    /**
     * Returns {@code true} if and only if there is no cause associated with
     * this exception or it is safe to cache it.
     */
    final boolean isCacheable() {
        return cacheable;
    }

    @Override
    public String getLocalizedMessage() {
        final String msg = getMessage();
        return msg != null
                ? new StringBuilder(getCanonicalPath()).append(" (").append(msg).append(")").toString()
                : getCanonicalPath();
    }
}
