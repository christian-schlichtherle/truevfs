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

import de.schlichtherle.truezip.io.File;
import de.schlichtherle.truezip.io.archive.Archive;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
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
    private final String entryName;

    ArchiveEntryNotFoundException(Archive archive, final String entryName, final String msg) {
        //super(archive, msg);
        //super.initPredecessor(null);
        canonicalPath = archive.getCanonicalPath();
        assert entryName != null;
        assert msg != null;
        this.entryName = entryName;
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
    public String getMessage() {
        final StringBuilder result = new StringBuilder(getCanonicalPath());
        if (!ArchiveController.isRoot(entryName))
            result.append(File.separator).append(entryName.replace(ArchiveEntry.SEPARATOR_CHAR, File.separatorChar));
        final String msg = super.getMessage();
        if (msg != null)
            result.append(" (").append(msg).append(")");
        return result.toString();
    }
}
