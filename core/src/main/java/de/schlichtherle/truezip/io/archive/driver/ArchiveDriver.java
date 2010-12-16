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

import de.schlichtherle.truezip.io.TabuFileException;
import de.schlichtherle.truezip.io.archive.controller.ArchiveController;
import de.schlichtherle.truezip.io.archive.model.ArchiveModel;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.socket.OutputShop;
import de.schlichtherle.truezip.io.socket.InputShop;
import de.schlichtherle.truezip.io.entry.EntryFactory;
import de.schlichtherle.truezip.io.archive.driver.registry.ArchiveDriverRegistry;
import de.schlichtherle.truezip.io.filesystem.FileSystemController;
import de.schlichtherle.truezip.io.filesystem.FileSystemDriver;
import de.schlichtherle.truezip.io.filesystem.MountPoint;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import javax.swing.Icon;

/**
 * This "driver" interface is used as an abstract factory which reads and
 * writes archives of a particular type, e.g. ZIP, TZP, JAR, TAR, TAR.GZ,
 * TAR.BZ2 or any other.
 * FileSystemModel drivers may be shared by their client applications.
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
 * @param <E> The type of the archive entries.
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface ArchiveDriver<E extends ArchiveEntry>
extends FileSystemDriver, EntryFactory<E> {

    /**
     * Creates a new archive controller for the given archive file system's
     * mount point and parent file system controller.
     * <p>
     * This method is primarily provided to serve two purposes:
     * <ol>
     * <li>An archive driver could decorate the default chain of archive
     *     controllers in order to implement additional aspects such as
     *     managing authentication keys for encrypted archive files or logging.
     * <li>An archive driver could use the provided parent component file
     *     system controller to request an entry for the archive model's mount
     *     point and use its properties in order to resolve an appropriate
     *     update strategy.
     *     E.g. an archive driver for ZIP files could return an archive
     *     controller which uses an append-strategy rather than the usual
     *     full-update-strategy if the target archive file exceeds a
     *     predefined size threshold such as 128 MB for example.
     * </ol>
     *
     * @param  mountPoint the non-{@code null} mount point of the archive file
     *         system.
     * @param  parent the non-{@code null} parent file system controller.
     * @return A new archive controller for the given archive model and
     *         parent file system controller.
     */
    @Override
    ArchiveController<E> newController(
            MountPoint mountPoint,
            FileSystemController parent);

    /**
     * Creates a new input shop for reading the archive entries of the the
     * described {@code archive} from the given {@code input} socket's target.
     * 
     * @param  model the non-{@code null} archive model.
     * @param  input the non-{@code null} input socket for reading
     *         the contents of the described archive from its target.
     * @throws TabuFileException if the target archive file is temporarily not
     *         accessible, e.g. if a key for decryption is currently not
     *         available.
     *         The client application will recognize the target archive file
     *         as a <i>tabu file</i> until a subsequent repetition of this
     *         method call succeeds.
     *         A tabu file will not be accessible through the API although it
     *         exists.
     * @throws FileNotFoundException if the target archive file does not exist
     *         or is not accessible.
     *         The client application will recognize the target archive file
     *         as a <i>special file</i> until a subsequent repetition of this
     *         method call succeeds.
     * @throws IOException if the target archive file is a
     *         <i>false positive</i> archive file.
     *         The client application will recognize the target archive file
     *         as a <i>regular file</i> until the archive file system is
     *         synchronized with its parent file system.
     * @return A non-{@code null} input shop.
     */
    InputShop<E> newInputShop(ArchiveModel model, InputSocket<?> input)
    throws IOException;

    /**
     * Creates a new output shop for writing archive entries to the the
     * described {@code archive} to the given {@code output} socket's target.
     * 
     * @param  model the non-{@code null} archive model.
     * @param  output the non-{@code null} output socket for writing
     *         the contents of the described archive to its target.
     * @param  source the nullable {@link InputShop} if
     *         {@code archive} is going to get updated.
     *         If not {@code null}, this is guaranteed to be a product
     *         of this driver's {@link #newInputShop} factor method, which may
     *         be used to copy some meta data which is specific to the type of
     *         archive this driver supports.
     *         For example, this could be used to copy the comment of a ZIP
     *         file.
     * @throws TabuFileException if the target archive file is temporarily not
     *         accessible, e.g. if a key for decryption is currently not
     *         available.
     *         The client application will recognize the target archive file
     *         as a <i>tabu file</i> until a subsequent repetition of this
     *         method call succeeds.
     *         A tabu file will not be accessible through the API although it
     *         exists.
     * @throws FileNotFoundException if the target archive file does not exist
     *         or is not accessible.
     *         The client application will recognize the target archive file
     *         as a <i>special file</i> until a subsequent repetition of this
     *         method call succeeds.
     * @throws IOException if the target archive file is a
     *         <i>false positive</i> archive file.
     *         The client application will recognize the target archive file
     *         as a <i>regular file</i> until the archive file system is
     *         synchronized with its parent file system.
     * @return A non-{@code null} output shop.
     */
    OutputShop<E> newOutputShop(   ArchiveModel model,
                                    OutputSocket<?> output,
                                    InputShop<E> source)
    throws IOException;

    /**
     * Returns the icon that
     * {@link de.schlichtherle.truezip.io.swing.tree.FileTreeCellRenderer}
     * should display for the given archive file.
     *
     * @param  model the non-{@code null} archive model.
     * @return The icon that should be displayed for the given archive file
     *         if it's open/expanded in the view.
     *         If {@code null} is returned, a default icon should be displayed.
     */
    Icon getOpenIcon(ArchiveModel model);

    /**
     * Returns the icon that
     * {@link de.schlichtherle.truezip.io.swing.FileSystemView}
     * and
     * {@link de.schlichtherle.truezip.io.swing.tree.FileTreeCellRenderer}
     * should display for the given archive file.
     *
     * @param  model the non-{@code null} archive model.
     * @return The icon that should be displayed for the given archive file
     *         if it's closed/collapsed in the view.
     *         If {@code null} is returned, a default icon should be displayed.
     */
    Icon getClosedIcon(ArchiveModel model);
}
