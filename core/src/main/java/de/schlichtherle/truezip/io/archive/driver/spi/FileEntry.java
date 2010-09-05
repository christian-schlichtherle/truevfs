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

package de.schlichtherle.truezip.io.archive.driver.spi;

import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveEntryMetaData;
import java.io.File;

import static de.schlichtherle.truezip.io.archive.driver.ArchiveEntry.Type.DIRECTORY;
import static de.schlichtherle.truezip.io.archive.driver.ArchiveEntry.Type.FILE;
import static de.schlichtherle.truezip.io.archive.driver.ArchiveEntry.Type.SPECIAL;

/**
 * Adapts a {@link File} instance to an {@link ArchiveEntry}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FileEntry implements ArchiveEntry {
    private final String name;
    private final File file;

    /**
     * Constructs a new {@code FileEntry}.
     * This constructor uses the file's path to build a valid entry name.
     * 
     * @param file A valid {@code File} instance.
     * @throws NullPointerException If {@code file} is {@code null}.
     */
    public FileEntry(final File file) {
        this(file, getName(file));
    }

    /**
     * Constructs a new {@code FileEntry}.
     * 
     * @param file A valid {@code File} instance.
     * @param entryName A valid archive entry name.
     * @see <a href="ArchiveEntry.html#entryName">Requirements for Archive Entry Names</a>
     * @throws NullPointerException If any parameter is {@code null}.
     */
    public FileEntry(final File file, final String entryName) {
        if (entryName == null || file == null)
            throw new NullPointerException();
        this.name = entryName;
        this.file = file;
    }

    /** Returns the adapted file. */
    public File getFile() {
        return file;
    }

    private static String getName(File file) {
        String entryName = file.getPath().replace(
                File.separatorChar, SEPARATOR_CHAR);
        if (file.isDirectory())
            return entryName + SEPARATOR_CHAR;
        return entryName;
    }

    /** Returns the name provided to the constructor. */
    @Override
    public String getName() {
        return name;
    }

    /** Returns whether the file is a directory or not. */
    @Override
    public Type getType() {
        return file.isDirectory() ? DIRECTORY
                : file.isFile() ? FILE
                : file.exists() ? SPECIAL
                : null;
    }

    /** Returns the file size. */
    @Override
    public long getSize() {
        return file.length();
    }

    @Override
    public void setSize(long size) {
        throw new UnsupportedOperationException();
    }

    /** Returns the file's last modification time. */
    @Override
    public long getTime() {
        return file.lastModified();
    }

    /** Sets the file's last modification time. */
    @Override
    public void setTime(long time) {
        file.setLastModified(time);
    }

    /** Returns {@code null}. */
    @Override
    public ArchiveEntryMetaData getMetaData() {
        return null;
    }

    /** A no-op: Does nothing. */
    @Override
    public void setMetaData(ArchiveEntryMetaData metaData) {
        throw new UnsupportedOperationException();
    }
}
