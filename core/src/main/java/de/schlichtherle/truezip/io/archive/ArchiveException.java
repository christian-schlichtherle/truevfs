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

package de.schlichtherle.truezip.io.archive;

import de.schlichtherle.truezip.io.util.ChainableIOException;

/**
 * Indicates an exceptional condition when processing archive files.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class ArchiveException extends ChainableIOException {

    private static final long serialVersionUID = 4893232550396764539L;

    private final String path;

    protected ArchiveException(Archive archive) {
        this.path = archive.getCanonicalPath();
    }

    protected ArchiveException(Archive archive, String message) {
        super(message);
        this.path = archive.getCanonicalPath();
    }

    protected ArchiveException(Archive archive, Throwable cause) {
        super(cause);
        this.path = archive.getCanonicalPath();
    }

    protected ArchiveException(Archive archive, String message, Throwable cause) {
        super(message, cause);
        this.path = archive.getCanonicalPath();
    }

    protected ArchiveException(Archive archive, int priority) {
        super(priority);
        this.path = archive.getCanonicalPath();
    }

    protected ArchiveException(Archive archive, String message, int priority) {
        super(message, priority);
        this.path = archive.getCanonicalPath();
    }

    protected ArchiveException(Archive archive, Throwable cause, int priority) {
        super(cause, priority);
        this.path = archive.getCanonicalPath();
    }

    protected ArchiveException(Archive archive, String message, Throwable cause, int priority) {
        super(message, cause, priority);
        this.path = archive.getCanonicalPath();
    }

    /**
     * Equivalent to
     * {@code return (ArchiveException) super.initCause(cause);}.
     */
    @Override
    public ArchiveException initCause(final Throwable cause) {
        return (ArchiveException) super.initCause(cause);
    }

    /**
     * Returns the <em>canonical</em> path name of the archive file which's
     * processing caused this exception to be created.
     * A canonical path is both absolute and unique within the virtual file
     * system.
     * The precise definition depends on the platform, but all elements in
     * a canonical path are separated by {@link java.io.File#separator}s.
     * <p>
     * This property may be used to determine some archive file specific
     * parameters, such as passwords or similar.
     * However, implementations must not assume that the file denoted by the
     * path actually exists as a file in the real file system!
     *
     * @return A string representing the canonical path of this archive
     *         - never {@code null}.
     */
    public String getCanonicalPath() {
        return path;
    }

    @Override
    public String getLocalizedMessage() {
        final String msg = getMessage();
        if (msg != null)
            return new StringBuilder(getCanonicalPath())
                    .append(" (")
                    .append(msg)
                    .append(")")
                    .toString();
        return getCanonicalPath();
    }
}
