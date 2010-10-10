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

package de.schlichtherle.truezip.io.socket;

import de.schlichtherle.truezip.util.BitField;
import java.util.Collections;
import java.io.File;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import static de.schlichtherle.truezip.io.Paths.cutTrailingSeparators;
import static de.schlichtherle.truezip.io.socket.CommonEntry.Type.DIRECTORY;
import static de.schlichtherle.truezip.io.socket.CommonEntry.Type.FILE;
import static de.schlichtherle.truezip.io.socket.CommonEntry.Type.SPECIAL;
import static de.schlichtherle.truezip.io.socket.CommonEntry.Access.WRITE;

/**
 * Adapts a {@link File} instance to a {@link FileSystemEntry}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class FileEntry
implements FileSystemEntry, IOReference<File> {
    private static final long serialVersionUID = 5263276267534643646L;

    private final File file;
    private final String name;

    /**
     * Constructs a new {@code FileEntry}.
     *
     * @param path a non-{@code null} path name.
     * @throws NullPointerException if {@code path} is {@code null}.
     */
    public FileEntry(String path) {
        this.file = new File(path);
        this.name = cutTrailingSeparators(path, SEPARATOR_CHAR);
    }

    /**
     * Constructs a new {@code FileEntry}.
     *
     * @param uri a non-{@code null} {@code file:} URI
     * @throws NullPointerException if {@code uri} is {@code null}.
     */
    public FileEntry(URI uri) {
        this.file = new File(uri);
        this.name = cutTrailingSeparators(uri.getPath(), SEPARATOR_CHAR);
    }

    /**
     * Constructs a new {@code FileEntry}.
     *
     * @param file a non-{@code null} file.
     * @throws NullPointerException if {@code file} is {@code null}.
     */
    public FileEntry(File file) {
        this.file = file;
        this.name = file.getPath().replace(File.separatorChar, SEPARATOR_CHAR);
    }

    /**
     * Constructs a new {@code FileEntry}.
     *
     * @param file a non-{@code null} file.
     * @throws NullPointerException if {@code file} is {@code null}.
     */
    public FileEntry(File file, String path) {
        this.file = new File(file, path);
        this.name = cutTrailingSeparators(path, SEPARATOR_CHAR);
    }

    public InputSocket<FileEntry> newInputSocket(
            BitField<InputOption> options) {
        InputSocket<FileEntry> input
                = new FileInputSocket<FileEntry>(this, file);
        if (options.get(InputOption.BUFFER))
            input = new BufferingInputSocket<FileEntry>(input);
        return input;
    }

    public OutputSocket<FileEntry> newOutputSocket(
            BitField<OutputOption> options) {
        return new FileOutputSocket<FileEntry>(this, file, options);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    /** Returns the type of this file entry. */
    @Override
    public Type getType() {
        return file.isDirectory() ? DIRECTORY
                :   file.isFile() ? FILE
                :   file.exists() ? SPECIAL
                :                   null;
    }

    @Override
    public long getSize(final Size type) {
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
    public long getTime(Access type) {
        return WRITE == type && file.exists() ? file.lastModified() : UNKNOWN;
    }

    @Override
    @SuppressWarnings("ManualArrayToCollectionCopy")
    public Set<String> getMembers() {
        final String[] list = file.list();
        if (null == list)
            return null;
        final Set<String> set
                = new HashSet<String>((int) (list.length / .75f) + 1);
        for (String member : list)
            set.add(member);
        return Collections.unmodifiableSet(set);
    }

    /** Returns the decorated file. */
    @Override
    public final File getTarget() {
        return file;
    }
}
