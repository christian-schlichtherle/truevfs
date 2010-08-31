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

import java.io.IOException;

/**
 * This exception is thrown when a client application tries to perform an
 * illegal operation on an archive file system.
 * <p>
 * This exception is private by intention: Clients applications should not
 * even know about the existence of virtual archive file systems.
 */
public class ArchiveFileSystemException
extends IOException {

    private static final long serialVersionUID = 4652084652223428651L;
    /** The entry's path name. */
    private final String entryName;

    ArchiveFileSystemException(String message) {
        super(message);
        this.entryName = null;
    }

    ArchiveFileSystemException(String path, String message) {
        super(message);
        this.entryName = path;
    }

    ArchiveFileSystemException(String path, IOException cause) {
        super(cause.toString());
        this.entryName = path;
        super.initCause(cause);
    }

    @Override
    public String getMessage() {
        if (entryName != null)
            return new StringBuilder(entryName).append(" (").append(super.getMessage()).append(")").toString();
        return super.getMessage();
    }
}
