/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.entry;

import de.schlichtherle.truezip.io.filesystem.FileSystemEntry;
import java.util.Collections;
import java.io.File;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import static de.schlichtherle.truezip.io.Paths.cutTrailingSeparators;
import static de.schlichtherle.truezip.io.entry.CommonEntry.Type.DIRECTORY;
import static de.schlichtherle.truezip.io.entry.CommonEntry.Type.FILE;
import static de.schlichtherle.truezip.io.entry.CommonEntry.Type.SPECIAL;
import static de.schlichtherle.truezip.io.entry.CommonEntry.Access.WRITE;

/**
 * Adapts a {@link File} instance to a {@link FileSystemEntry}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FileEntry implements FileSystemEntry {

    /**
     * Returns a file entry for the given parameter.
     *
     * @param path a non-{@code null} path name.
     * @throws NullPointerException if {@code path} is {@code null}.
     */
    public static FileEntry get(String path) {
        return new FileEntry(path);
    }

    /**
     * Returns a file entry for the given parameter.
     *
     * @param uri a non-{@code null} {@code file:} URI
     * @throws NullPointerException if {@code uri} is {@code null}.
     */
    public static FileEntry get(final URI uri) {
        return new FileEntry(uri);
    }

    /**
     * Returns a file entry for the given parameter(s).
     *
     * @param file a non-{@code null} file.
     * @throws NullPointerException if {@code file} is {@code null}.
     */
    public static FileEntry get(File file) {
        return new FileEntry(file);
    }

    /**
     * Returns a file entry for the given parameter(s).
     *
     * @param file a non-{@code null} file.
     * @throws NullPointerException if {@code file} is {@code null}.
     */
    public static FileEntry get(File file, String path) {
        return new FileEntry(file, path);
    }

    private final File   file;
    private final String name;

    FileEntry(final String path) {
        this.file = new File(path);
        this.name = cutTrailingSeparators(path, SEPARATOR_CHAR);
    }

    FileEntry(final URI uri) {
        this.file = new File(uri);
        this.name = cutTrailingSeparators(uri.getPath(), SEPARATOR_CHAR);
    }

    FileEntry(final File file) {
        this.file = file;
        this.name = file.getPath().replace(File.separatorChar, SEPARATOR_CHAR);
    }

    FileEntry(final File file, final String path) {
        this.file = new File(file, path);
        this.name = cutTrailingSeparators(path, SEPARATOR_CHAR);
    }

    /** Returns the decorated file. */
    public final File getFile() {
        return file;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final String toString() {
        return name;
    }

    /** Returns the type of this file entry. */
    @Override
    public final CommonEntry.Type getType() {
        return file.isDirectory() ? DIRECTORY
                :   file.isFile() ? FILE
                :   file.exists() ? SPECIAL
                :                   null;
    }

    @Override
    public final long getSize(final Size type) {
        switch (type) {
            case DATA:
            case STORAGE:
                return file.exists() ? file.length() : UNKNOWN;
            default:
                return UNKNOWN;
        }
    }

    /** Returns the file's last modification time. */
    @Override
    public final long getTime(Access type) {
        return WRITE == type && file.exists() ? file.lastModified() : UNKNOWN;
    }

    @Override
    @SuppressWarnings("ManualArrayToCollectionCopy")
    public final Set<String> getMembers() {
        final String[] list = file.list();
        if (null == list)
            return null;
        final Set<String> set
                = new HashSet<String>((int) (list.length / .75f) + 1);
        for (String member : list)
            set.add(member);
        return Collections.unmodifiableSet(set);
    }
}
