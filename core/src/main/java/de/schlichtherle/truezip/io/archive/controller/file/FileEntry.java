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

package de.schlichtherle.truezip.io.archive.controller.file;

import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import java.util.Collections;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystemEntry;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry;
import java.io.File;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import static de.schlichtherle.truezip.io.socket.entry.CommonEntry.Type.DIRECTORY;
import static de.schlichtherle.truezip.io.socket.entry.CommonEntry.Type.FILE;
import static de.schlichtherle.truezip.io.socket.entry.CommonEntry.Type.SPECIAL;
import static de.schlichtherle.truezip.io.socket.entry.CommonEntry.Access.WRITE;

/**
 * Adapts a {@link File} instance to a {@link CommonEntry}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class FileEntry
extends            File
implements         ArchiveEntry, ArchiveFileSystemEntry {
    private static final long serialVersionUID = 5263276267534643646L;

    /**
     * Constructs a new {@code FileEntry}.
     *
     * @param path a non-{@code null} path name.
     * @throws NullPointerException if {@code path} is {@code null}.
     */
    public FileEntry(String path) {
        super(path);
    }

    /**
     * Constructs a new {@code FileEntry}.
     *
     * @param uri a non-{@code null} {@code file:} URI
     * @throws NullPointerException if {@code uri} is {@code null}.
     */
    public FileEntry(URI uri) {
        super(uri);
    }

    /**
     * Constructs a new {@code FileEntry}.
     *
     * @param file a non-{@code null} file.
     * @throws NullPointerException if {@code file} is {@code null}.
     */
    public FileEntry(File file) {
        super(file.getPath());
    }

    /**
     * Constructs a new {@code FileEntry}.
     *
     * @param file a non-{@code null} file.
     * @throws NullPointerException if {@code file} is {@code null}.
     */
    public FileEntry(File file, String path) {
        super(file, path);
    }

    /** Returns the type of this file entry. */
    @Override
    public Type getType() {
        return isDirectory() ? DIRECTORY
                :   isFile() ? FILE
                :   exists() ? SPECIAL
                :              null;
    }

    @Override
    public long getSize(final Size type) {
        switch (type) {
            case DATA:
            case STORAGE:
                return length();
            default:
                return UNKNOWN;
        }
    }

    @Override
    public boolean setSize(Size type, long size) {
        return false;
    }

    /** Returns the file's last modification time. */
    @Override
    public long getTime(Access type) {
        return WRITE == type ? lastModified() : UNKNOWN;
    }

    @Override
    public boolean setTime(Access type, long time) {
        return WRITE == type && UNKNOWN != time && setLastModified(time);
    }

    @Override
    @SuppressWarnings("ManualArrayToCollectionCopy")
    public Set<String> getMembers() {
        String[] list = list();
        Set<String> set = new HashSet<String>((int) (list.length / .75f) + 1);
        for (String member : list)
            set.add(member);
        return null == list ? null : Collections.unmodifiableSet(set);
    }
}
