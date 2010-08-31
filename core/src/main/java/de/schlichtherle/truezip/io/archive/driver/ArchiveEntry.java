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

package de.schlichtherle.truezip.io.archive.driver;

import de.schlichtherle.truezip.io.archive.controller.ArchiveEntryMetaData;
import de.schlichtherle.truezip.io.swing.FileSystemView;
import de.schlichtherle.truezip.io.swing.tree.FileTreeCellRenderer;
import de.schlichtherle.truezip.io.util.Paths;

import javax.swing.Icon;

/**
 * A simple interface for entries in an archive.
 * Drivers need to implement this interface in order to allow TrueZIP to
 * read and write entries for the supported archive types.
 * <p>
 * Note that implementations do <em>not</em> need to be thread safe:
 * Multithreading is addressed in the package {@code de.schlichtherle.truezip.io}.
 *
 * <h3><a name="entryName">Requirements for Archive Entry Names</a></h3>
 * <p>
 * TrueZIP has the following requirements for archive entry names:
 * <ol>
 * <li>An entry name is a list of directory or file names whichs elements
 *     are separated by single separators ({@link #SEPARATOR}).
 * <li>A dot ({@code "."}) or a dot-dot ({@code ".."})
 *     is not permissible as a directory or file name in an entry name.
 * <li>If an entry name starts with a separator, it's said to be
 *     <i>absolute</i>.
 *     Absolute entries are not accessible by client applications, but are
 *     retained if the archive is updated.
 * <li>If an entry name ends with a separator, it denotes a directory
 *     and vice versa.
 * </ol>
 * For example, {@code "foo/bar"} denotes a valid entry
 * name for a file, but {@code "./abc/../foo/./def/./../bar/."}
 * would not, although both refer to the same entry.
 * <p>
 * As another example, {@code "dir/"} denotes a valid entry name for a
 * directory, but {@code "dir"} would not.
 * <p>
 * If an archive driver is to be written for an archive type which does not
 * support these requirements, an adapter for the entry name must be
 * implemented.
 * <p>
 * For example, the ZIP and TAR file formats conform to all but the second
 * requirement.
 * So the driver implementations for these archive types use
 * {@link Paths#normalize(String, char)} to remove any redundant elements from
 * the path.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface ArchiveEntry {

    /**
     * The relative path name of the virtual root directory, which is {@value}.
     * Note that this name is empty and hence does not contain a separator
     * character.
     */
    String ROOT = "";

    /**
     * The name separator as a character, which is {@value}.
     *
     * @see #SEPARATOR
     */
    char SEPARATOR_CHAR = '/';

    /**
     * The name separator as a string, which is {@value}.
     *
     * @see #SEPARATOR_CHAR
     */
    String SEPARATOR = "/";

    /**
     * The unknown value for numeric properties, which is {@value}.
     */
    byte UNKNOWN = -1;

    /**
     * Defines the type of archive entry.
     */
    // TODO: Provide getter!
    public enum Type {
        /**
         * Regular file.
         * A file usually has some content associated to it which can be read
         * and written using a stream.
         */
        FILE,

        /**
         * Regular directory.
         * A directory can have other archive entries as children.
         */
        DIRECTORY,

        /**
         * Symbolic (named) link.
         * A symbolic link refers to another file system node which could even
         * be located outside the current archive file.
         */
        SYMLINK,

        /**
         * Special file.
         * A special file is a byte or block oriented interface to an arbitrary
         * resource, e.g. a hard disk or a network service.
         */
        SPECIAL
    }
    
    /**
     * Returns the archive entry name.
     *
     * @return A valid archive entry name.
     * @see <a href="#entryName">Requirements for Archive Entry Names</a>
     */
    String getName();

    /**
     * Returns {@code true} if and only if this entry represents a
     * directory.
     */
    boolean isDirectory();

    /**
     * Returns the (uncompressed) size of the archive entry in bytes,
     * or {@code UNKNOWN} if not specified.
     * This method is not meaningful for directory entries.
     */
    long getSize();

    // TODO: Add this in TrueZIP 7.
    /**
     * Sets the size of this archive entry.
     *
     * @param size The size of this archive entry in bytes.
     * @see #getSize
     */
    //void setSize(long size);

    /**
     * Returns the last modification time of this archive entry since the
     * epoch, or {@code UNKNOWN} if not specified.
     *
     * @see #setTime
     */
    long getTime();

    /**
     * Sets the last modification time of this archive entry.
     *
     * @param time The last modification time of this archive entry in
     *             milliseconds since the epoch.
     * @see #getTime
     */
    void setTime(long time);

    /**
     * Returns the icon that {@link FileTreeCellRenderer}
     * should display for this entry if it is open/expanded in the view.
     * If {@code null} is returned, a default icon will be used,
     * depending on the type of this entry and its state in the view.
     */
    Icon getOpenIcon();

    /**
     * Returns the icon that {@link FileSystemView}
     * and {@link FileTreeCellRenderer} should
     * display for this entry if it is closed/collapsed in the view.
     * If {@code null} is returned, a default icon will be used,
     * depending on the type of this entry and its state in the view.
     */
    Icon getClosedIcon();

    /**
     * Returns the meta data for this archive entry.
     * The default value is {@code null}.
     */
    ArchiveEntryMetaData getMetaData();

    /**
     * Sets the meta data for this archive entry.
     *
     * @param metaData The meta data - may not be {@code null}.
     */
    void setMetaData(ArchiveEntryMetaData metaData);
}
