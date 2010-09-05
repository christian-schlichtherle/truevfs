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

import de.schlichtherle.truezip.io.archive.Archive;
import de.schlichtherle.truezip.io.archive.driver.TransientIOException;
import java.io.IOException;

/**
 * Indicates that the target file of an archive controller is a false
 * positive archive file which actually exists as a plain file or directory
 * in the real file system or in an enclosing archive file.
 * <p>
 * Instances of this class are always associated with an
 * {@code IOException} as their cause.
 */
public class FalsePositiveException
extends Exception {

    private static final long serialVersionUID = 947139561381472363L;

    private final String canonicalPath;

    private final boolean cacheable;

    FalsePositiveException(Archive archive, IOException cause) {
        //super(archive);
        //super.initPredecessor(null);
        // This exception type is never passed to the client application,
        // so a descriptive message would be waste of performance.
        //super(cause.toString());
        assert cause != null;
        canonicalPath = archive.getCanonicalPath();
        // A transient I/O exception is just a wrapper exception to mark
        // the real transient cause, therefore we can safely throw it away.
        // We must do this in order to allow an archive controller to inspect
        // the real transient cause and act accordingly.
        final boolean trans = cause instanceof TransientIOException;
        super.initCause(trans ? cause.getCause() : cause);
        cacheable = !trans;
    }

    /**
     * Returns {@code true} if and only if there is no cause
     * associated with this exception or it is safe to cache it.
     */
    final boolean isCacheable() {
        return cacheable;
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
    public final String getCanonicalPath() {
        return canonicalPath;
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
