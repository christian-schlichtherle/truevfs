/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.socket.file;

import de.schlichtherle.truezip.io.socket.entry.CommonEntry;
import java.io.File;

import static de.schlichtherle.truezip.io.socket.entry.CommonEntry.Type.DIRECTORY;
import static de.schlichtherle.truezip.io.socket.entry.CommonEntry.Type.FILE;
import static de.schlichtherle.truezip.io.socket.entry.CommonEntry.Type.SPECIAL;

/**
 * Adapts a {@link File} instance to a {@link CommonEntry}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FileEntry extends File implements CommonEntry {
    private static final long serialVersionUID = 5263276267534643646L;

    /**
     * Constructs a new {@code FileEntry}.
     *
     * @param path A non-{@code null} path name.
     * @throws NullPointerException If {@code path} is {@code null}.
     */
    public FileEntry(final String path) {
        super(path);
    }

    /**
     * Constructs a new {@code FileEntry}.
     *
     * @param file A non-{@code null} file.
     * @throws NullPointerException If {@code file} is {@code null}.
     */
    public FileEntry(final File file) {
        super(file.getPath());
    }

    /** Returns whether the file is a directory or not. */
    @Override
    public Type getType() {
        return isDirectory() ? DIRECTORY
                : isFile() ? FILE
                : exists() ? SPECIAL
                : null;
    }

    /** Returns the file size. */
    @Override
    public long getSize() {
        return length();
    }

    /** Returns the file's last modification time. */
    @Override
    public long getTime(Access type) {
        return Access.WRITE == type ? lastModified() : UNKNOWN;
    }

    public void setTime(Access type, long value) {
        if (Access.WRITE == type)
            setLastModified(value);
    }
}
