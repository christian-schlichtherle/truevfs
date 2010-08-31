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

import de.schlichtherle.truezip.io.archive.Archive;
import de.schlichtherle.truezip.io.archive.controller.ArchiveController;
import de.schlichtherle.truezip.io.archive.driver.registry.ArchiveDriverRegistry;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.rof.ReadOnlyFileInputStream;
import java.io.CharConversionException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import javax.swing.Icon;

/**
 * This "driver" interface is used as an abstract factory which reads and
 * writes archives of a particular type, e.g. ZIP, TZP, JAR, TAR, TAR.GZ,
 * TAR.BZ2 or any other.
 * Archive drivers may be shared by their client applications.
 * <p>
 * The following requirements must be met by any implementation:
 * <ul>
 * <li>Implementations must be thread-safe.
 * <li>Implementations must be (at least virtually) immutable.
 * <li>Implementations must not assume that they are used as singletons:
 *     Multiple instances of an implementation may be used for the same
 *     archive type.
 * <li>If the driver shall be supported by the {@link ArchiveDriverRegistry},
 *     a no-arguments constructor must be provided.
 * <li>Although not required, it's recommended to implement the
 *     {@link Serializable} interface, so that objects which are referring to
 *     it can be serialized.
 * </ul>
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface ArchiveDriver {

    /**
     * Creates a new input archive for the given {@code archive} in order to
     * read the given read only file.
     * <p>
     * Note that if an exception is thrown, the method must be reentrant!
     * In addition, the exception type determines the behaviour of the
     * {@link ArchiveController} class as follows:
     * <table>
     * <tr>
     *   <th>Exception type</td>
     *   <th>{@link ArchiveController#isFile}</th>
     *   <th>{@link ArchiveController#isDirectory}</th>
     *   <th>{@link ArchiveController#exists}</th>
     *   <th>{@link ArchiveController#delete}</th>
     * </tr>
     * <tr>
     *   <td>{@link FileNotFoundException}</td>
     *   <td><b>{@code false}</b></td>
     *   <td>{@code false}</td>
     *   <td>{@code true}</td>
     *   <td>{@code true} (unless prohibited by the real file system)</td>
     * </tr>
     * <tr>
     *   <td>{@link IOException}</td>
     *   <td><b>{@code true}</b></td>
     *   <td>{@code false}</td>
     *   <td>{@code true}</td>
     *   <td>{@code true} (unless prohibited by the real file system)</td>
     * </tr>
     * </table>
     * 
     * @param archive The abstract archive representation which TrueZIP's
     *        internal {@link ArchiveController} is processing
     *        - never {@code null}.
     * @param rof The {@link ReadOnlyFile} to read the
     *        actual archive contents from - never {@code null}.
     *        Hint: If you'ld prefer to have an {@code InputStream},
     *        you could decorate this parameter with a
     *        {@link ReadOnlyFileInputStream}.
     * @return A new input archive instance.
     * @throws TransientIOException If calling this method for the same
     *         archive file again could possibly succeed.
     *         This exception is associated with another {@link IOException}
     *         as its cause which is unwrapped and interpreted as below.
     * @throws FileNotFoundException If the input archive is inaccessible
     *         for any reason and the implementation would like to mask the
     *         archive file like a special file which cannot get read, written
     *         or deleted.
     * @throws IOException On any other I/O or data format related issue
     *         when reading the input archive and the implementation would like
     *         to treat the archive file like a regular file which may be read,
     *         written or deleted.
     * @see InputArchive
     */
    InputArchive newInputArchive(
            Archive archive,
            ReadOnlyFile rof)
    throws IOException;

    /**
     * Creates a new archive entry for {@code entryName}
     * for use with an {@link OutputArchive}.
     * 
     * @param archive The abstract archive representation which TrueZIP's
     *        internal {@link ArchiveController} is processing
     *        - never {@code null}.
     * @param entryName A valid archive entry name  - never {@code null}.
     * @param template If not {@code null}, then the newly created entry
     *        shall inherit as much attributes from this object as possible
     *        (with the exception of the name).
     *        This is typically used for archive copy operations.
     *        Note that there is no guarantee on the runtime type of this
     *        object; it may have been created by other drivers.
     *        It is safe to ignore the {@code metaData} property when
     *        copying entries.
     * @return A new archive entry instance.
     * @throws CharConversionException If {@code name} contains
     *         illegal characters.
     * @see <a href="ArchiveEntry.html#entryName">Requirements for Archive Entry Names</a>
     */
    ArchiveEntry newArchiveEntry(
            Archive archive,
            String entryName,
            ArchiveEntry template)
    throws CharConversionException;

    /**
     * Creates a new output archive for {@code archive}
     * from the given output stream.
     * 
     * @param archive The abstract archive representation which TrueZIP's
     *        internal {@link ArchiveController} is processing
     *        - never {@code null}.
     * @param out The {@link OutputStream} to write the archive entries to
     *        - never {@code null}.
     * @param source The source {@link InputArchive} if
     *        {@code archive} is going to get updated.
     *        If not {@code null}, this is guaranteed to be a product
     *        of this driver's {@link #newInputArchive} method.
     *        This may be used to copy some meta data which is specific to
     *        the type of archive this driver supports.
     *        For example, this could be used to copy the comment of a ZIP
     *        file.
     * @return A new output archive instance.
     * @throws TransientIOException If calling this method for the same
     *         archive file again could possibly succeed.
     *         This exception is associated with another {@code IOException}
     *         as its cause which is unwrapped and interpreted as below.
     * @throws FileNotFoundException If the output archive is inaccessible
     *         for any reason.
     * @throws IOException On any other I/O or data format related issue
     *         when writing the output archive.
     * @see OutputArchive
     */
    OutputArchive newOutputArchive(
            Archive archive,
            OutputStream out,
            InputArchive source)
    throws IOException;
    
    /**
     * Returns the icon that {@link de.schlichtherle.truezip.io.swing.tree.FileTreeCellRenderer}
     * should display for the given archive.
     *
     * @param archive The abstract archive representation which TrueZIP's
     *        internal {@link ArchiveController} is processing
     *        - never {@code null}.
     * @return The icon that should be displayed for the given archive if is
     *         is open/expanded in the view.
     *         If {@code null} is returned, a default icon should be used.
     */
    Icon getOpenIcon(Archive archive);

    /**
     * Returns the icon that {@link de.schlichtherle.truezip.io.swing.FileSystemView}
     * and {@link de.schlichtherle.truezip.io.swing.tree.FileTreeCellRenderer} should
     * display for the given archive.
     *
     * @param archive The abstract archive representation which TrueZIP's
     *        internal {@link ArchiveController} is processing
     *        - never {@code null}.
     * @return The icon that should be displayed for the given archive if is
     *         is closed/collapsed in the view.
     *         If {@code null} is returned, a default icon should be used.
     */
    Icon getClosedIcon(Archive archive);

    /**
     * Archive drivers will be put into hash maps as keys,
     * so be sure to implement this properly.
     * Note that this is just a reinforcement of the general contract for
     * {@link Object#equals} and the best possible implementation is the
     * default implementation in {@code Object} which is most
     * discriminating.
     */
    @Override
    boolean equals(Object o);

    /**
     * Archive drivers will be put into hash maps as keys,
     * so be sure to implement this properly.
     * Note that this is just a reinforcement of the general contract for
     * {@link Object#hashCode} and the best possible implementation is the
     * default implementation in {@code Object} which is most
     * discriminating.
     */
    @Override
    int hashCode();
}
