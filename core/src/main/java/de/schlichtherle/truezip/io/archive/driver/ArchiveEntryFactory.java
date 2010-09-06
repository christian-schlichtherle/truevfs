/*
 * Copyright 2007-2010 Schlichtherle IT Services
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

import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry.Type;
import java.io.CharConversionException;

/**
 * An immutable, thread-safe factory for archive entries.
 *
 * @param <AE> The type of the created archive entries.
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface ArchiveEntryFactory<AE extends ArchiveEntry> {

    /**
     * Returns a new archive entry for the given <i>path name</i>.
     * A path name must meet the following common requirements:
     * <ol>
     * <li>A path name is a sequence of file or directory entity
     *     <i>base names</i> which are separated by one or more <i>separator
     *     characters</i> ({@link ArchiveEntry#SEPARATOR_CHAR}).
     *     This implies that a base name cannot contain separator characters.
     * <li>A path name may contain one or more dot ({@code "."}) or dot-dot
     *     ({@code ".."}) base names which represent the current or parent
     *     directory respectively.
     * <li>If a path name starts with one or more separator characters its said
     *     to be <i>absolute</i>.
     *     Otherwise, its said to be <i>relative</i>.
     * <li>A path name may end with one or more separator characters,
     *     e.g. to identify a directory entry.
     * </ol>
     * For example, {@code "foo/bar"} and
     * {@code "./abc/../foo/./def/./../bar/./"} are both valid path names
     * which refer to the same entity in the archive file.
     * <p>
     * Note that implementations usually have additional requirements for a
     * path name to represent an <i>entry name</i> for their particular archive
     * type.
     * For example the entry names in ZIP and TAR files must end with the
     * separator character {@code '/'} if and only if the entry is a directory.
     * Such archive type specific requirement are not met by the client class
     * of this interface, so implementations need to check and fix the given
     * path name in order to meet them.
     * This term implies that {@code path} does not need to
     * {@link Object#equals equal} the return value of the method
     * {@link ArchiveEntry#getName} of the returned archive entry.
     *
     * @param  path a non-{@code null} <i>path name</i>.
     *         This may need some fixing to convert it to an
     *         <i>entry name</i> for the implementation's particular archive
     *         type.
     * @param  type a non-{@code null} entry type.
     * @param  template if not {@code null}, then the new archive entry shall
     *         inherit as much properties from this archive entry as possible
     *         - with the exception of the path name.
     *         Furthermore, its {@link ArchiveEntry#getType()} method must
     *         return the {@code type} parameter.
     *         This parameter is typically used for copy operations.
     * @return A new archive entry.
     * @throws CharConversionException if {@code path} contains characters
     *         which are not valid in the archive file.
     */
    AE newArchiveEntry(String path, Type type, ArchiveEntry template)
    throws CharConversionException;
}
