/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.archive;

import java.io.IOException;
import net.jcip.annotations.ThreadSafe;

/**
 * Thrown to indicate an exceptional condition in an {@link FsArchiveFileSystem}.
 */
@ThreadSafe
public class FsArchiveFileSystemException extends IOException {

    private static final long serialVersionUID = 4652084652223428651L;

    /** The entry's path name. */
    private final String path;

    FsArchiveFileSystemException(String path, String message) {
        super(message);
        this.path = path;
    }

    FsArchiveFileSystemException(String path, IOException cause) {
        super(cause != null ? cause.toString() : null);
        this.path = path;
        super.initCause(cause);
    }

    FsArchiveFileSystemException(String path, String message, IOException cause) {
        super(message);
        this.path = path;
        super.initCause(cause);
    }

    @Override
    public String getLocalizedMessage() {
        if (path != null)
            return new StringBuilder(path).append(" (").append(getMessage()).append(")").toString();
        return getMessage();
    }
}
