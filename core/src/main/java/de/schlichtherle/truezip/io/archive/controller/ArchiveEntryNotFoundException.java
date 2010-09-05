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

import de.schlichtherle.truezip.io.file.File;
import de.schlichtherle.truezip.io.archive.Archive;
import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry;
import java.io.FileNotFoundException;

/**
 * Indicates that an <i>archive entry</i>
 * does not exist or is not accessible.
 * <p>
 * May be thrown by {@link ArchiveController#newInputStream} or
 * {@link ArchiveController#newOutputStream}.
 */
public final class ArchiveEntryNotFoundException
extends FileNotFoundException {

    private static final long serialVersionUID = 2972350932856838564L;

    private final String canonicalPath;

    ArchiveEntryNotFoundException(Archive archive, final String path, final String msg) {
        //super(archive, msg);
        //super.initPredecessor(null);
        assert msg != null;
        final StringBuilder result = new StringBuilder(archive.getCanonicalPath());
        if (!ArchiveController.isRoot(path))
            result.append(File.separator).append(path.replace(ArchiveEntry.SEPARATOR_CHAR, File.separatorChar));
        canonicalPath = result.toString();
    }

    /**
     * Returns the <em>canonical</em> path name of the archive entry which's
     * processing caused this exception to be created.
     * A canonical path is both absolute and unique within the virtual file
     * system.
     * The precise definition depends on the platform, but all elements in
     * a canonical path are separated by {@link java.io.File#separator}s.
     * <p>
     * This property may be used to determine some archive file specific
     * parameters, such as passwords or similar.
     * However, implementations must not assume that the file denoted by the
     * path actually isExisting as a file in the real file system!
     *
     * @return A string representing the canonical path of this archive
     *         - never {@code null}.
     */
    // TODO: Change to URIs.
    public final String getCanonicalPath() {
        return canonicalPath;
    }

    @Override
    public String getLocalizedMessage() {
        final String msg = getMessage();
        return msg != null
                ? new StringBuilder(getCanonicalPath()).append(" (").append(msg).append(")").toString()
                : getCanonicalPath();
    }
}
