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

package de.schlichtherle.truezip.io.archive.spi;

import de.schlichtherle.truezip.io.ArchiveEntryMetaData;
import java.io.File;
import javax.swing.Icon;

/**
 * A utility class which adapts a {@link File} instance to an
 * {@link ArchiveEntry} (RFS means Real File System).
 *
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.5
 */
public class RfsEntry implements ArchiveEntry {
    private final String entryName;
    private final File file;

    /**
     * Constructs a new {@code RfsEntry}.
     * This constructor uses the file's path to build a valid entry name.
     * 
     * @param file A valid {@code File} instance.
     * @throws NullPointerException If {@code file} is {@code null}.
     */
    public RfsEntry(final File file) {
        this(file, getName(file));
    }

    /**
     * Constructs a new {@code RfsEntry}.
     * 
     * @param file A valid {@code File} instance.
     * @param entryName A valid archive entry name.
     * @see <a href="ArchiveEntry.html#entryName">Requirements for Archive Entry Names</a>
     * @throws NullPointerException If any parameter is {@code null}.
     */
    public RfsEntry(final File file, final String entryName) {
        if (entryName == null || file == null)
            throw new NullPointerException();
        this.entryName = entryName;
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
    public String getName() {
        return entryName;
    }

    /** Returns whether the file is a directory or not. */
    public boolean isDirectory() {
        return file.isDirectory();
    }

    /** Returns the file size. */
    public long getSize() {
        return file.length();
    }

    /** Returns the file's last modification time. */
    public long getTime() {
        return file.lastModified();
    }

    /** Sets the file's last modification time. */
    public void setTime(long time) {
        file.setLastModified(time);
    }

    /** Returns {@code null}. */
    public Icon getOpenIcon() {
        return null;
    }

    /** Returns {@code null}. */
    public Icon getClosedIcon() {
        return null;
    }

    /** Returns {@code null}. */
    public ArchiveEntryMetaData getMetaData() {
        return null;
    }

    /** A no-op: Does nothing. */
    public void setMetaData(ArchiveEntryMetaData metaData) {
    }
}
