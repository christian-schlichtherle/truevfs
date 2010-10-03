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

import de.schlichtherle.truezip.io.archive.descriptor.ArchiveDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;

/**
 * Indicates that an <i>archive entry</i>
 * does not exist or is not accessible.
 * <p>
 * May be thrown by {@link ArchiveController#newInputSocket} or
 * {@link ArchiveController#newOutputSocket}.
 */
public class EntryNotFoundException extends FileNotFoundException {

    private static final long serialVersionUID = 2972350932856838564L;

    private final URI mountPoint;
    private final String path;

    EntryNotFoundException(
            final ArchiveDescriptor archive,
            final String path,
            final String msg) {
        super(msg);
        assert path != null;
        assert msg != null;
        this.mountPoint = archive.getMountPoint();
        this.path = path;
    }

    EntryNotFoundException(
            final ArchiveDescriptor archive,
            final String path,
            final IOException cause) {
        super(cause == null ? null : cause.toString());
        assert path != null;
        assert cause != null;
        super.initCause(cause);
        this.mountPoint = archive.getMountPoint();
        this.path = path;
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

    @Override
    public String getLocalizedMessage() {
        final String msg = getMessage();
        return msg != null
                ? new StringBuilder(getCanonicalPath()).append(" (").append(msg).append(")").toString()
                : getCanonicalPath();
    }
}
