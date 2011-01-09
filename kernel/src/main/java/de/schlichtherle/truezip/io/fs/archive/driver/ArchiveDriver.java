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

package de.schlichtherle.truezip.io.fs.archive.driver;

import de.schlichtherle.truezip.io.TabuFileException;
import de.schlichtherle.truezip.io.fs.archive.DefaultArchiveController;
import de.schlichtherle.truezip.io.fs.concurrent.FsConcurrentModel;
import de.schlichtherle.truezip.io.fs.archive.ArchiveEntry;
import de.schlichtherle.truezip.io.socket.OutputShop;
import de.schlichtherle.truezip.io.socket.InputShop;
import de.schlichtherle.truezip.io.entry.EntryFactory;
import de.schlichtherle.truezip.io.fs.FsController;
import de.schlichtherle.truezip.io.fs.FsMountPoint;
import de.schlichtherle.truezip.io.fs.archive.ArchiveDriverRegistry;
import de.schlichtherle.truezip.io.fs.concurrent.FsConcurrentController;
import de.schlichtherle.truezip.io.fs.concurrent.FsCachingController;
import de.schlichtherle.truezip.io.fs.file.TempFilePool;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import javax.swing.Icon;
import net.jcip.annotations.ThreadSafe;

/**
 * This "driver" interface is used as an abstract factory which reads and
 * writes archives of a particular type, e.g. ZIP, TZP, JAR, TAR, TAR.GZ,
 * TAR.BZ2 or any other.
 * FileSystemModel drivers may be shared by their client applications.
 * <p>
 * The following requirements must be met by any implementation:
 * <ul>
 * <li>The implementation must be thread-safe.
 * <li>The implementation must be serializable.
 * <li>Hence it would be an error to assume that it's a singleton.
 * <li>If the implementation shall get supported by the
 *     {@link ArchiveDriverRegistry}, a no-arguments constructor must be
 *     provided.
 * </ul>
 *
 * @param   <E> The type of the archive entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public abstract class ArchiveDriver<E extends ArchiveEntry>
implements EntryFactory<E>, Serializable {

    /**
     * Returns a new thread-safe file system controller for the given mount
     * point and parent file system controller.
     * <p>
     * When called, the following expression is a precondition:
     * {@code mountPoint.getParent().equals(parent.getModel().getMountPoint())}
     * <p>
     * Note that an archive file system is always federated and therefore
     * its parent file system controller is never {@code null}.
     * <p>
     * Furthermore, an archive driver implementation is <em>not</em> expected
     * to consider the scheme of the given mount point to determine the class
     * of the returned file system controller.
     * Consequently, it is an error to call this method with a mount point
     * which has a scheme which is not supported by this archive driver.
     * <p>
     * Note again that the returned file system controller must be thread-safe!
     *
     * @param  mountPoint the mount point of the file system.
     * @param  parent the parent file system controller.
     * @return A new thread-safe file system controller for the given mount
     *         point and parent file system controller.
     */
    public @NonNull FsController<?>
    newController(  @NonNull FsMountPoint mountPoint,
                    @NonNull FsController<?> parent) {
        return  new FsConcurrentController(
                   new FsCachingController(
                        new DefaultArchiveController<E>(
                            new FsConcurrentModel(mountPoint, parent.getModel()),
                            this, parent, false),
                        TempFilePool.get()));
    }

    /**
     * Creates a new input shop for reading the archive entries of the the
     * described {@code archive} from the given {@code input} socket's target.
     * 
     * @param  model the concurrent file system model.
     * @param  input the input socket for reading the contents of the archive
     *         from its target.
     * @return A new input shop.
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
     */
    public abstract @NonNull InputShop<E>
    newInputShop(   @NonNull FsConcurrentModel model,
                    @NonNull InputSocket<?> input)
    throws IOException;

    /**
     * Creates a new output shop for writing archive entries to the the
     * described {@code archive} to the given {@code output} socket's target.
     * 
     * @param  model the concurrent file system model.
     * @param  output the output socket for writing the contents of the
     *         archive to its target.
     * @param  source the {@link InputShop} if {@code archive} is going to get
     *         updated.
     *         If not {@code null}, this is guaranteed to be a product
     *         of this driver's {@link #newInputShop} factory method, which may
     *         be used to copy some meta data which is specific to the type of
     *         archive this driver supports.
     *         For example, this could be used to copy the comment of a ZIP
     *         file.
     * @return A new output shop.
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
     */
    public abstract @NonNull OutputShop<E>
    newOutputShop(  @NonNull FsConcurrentModel model,
                    @NonNull OutputSocket<?> output,
                    @Nullable InputShop<E> source)
    throws IOException;

    /**
     * Returns the icon that should be displayed for the given archive file
     * if it's open/expanded in the view.
     * <p>
     * The implementation in the abstract class {@code ArchiveDriver} simply
     * returns {@code null}.
     *
     * @param  model the concurrent file system model.
     * @return The icon that should be displayed for the given archive file
     *         if it's open/expanded in the view.
     *         If {@code null} is returned, a default icon should be displayed.
     */
    public @CheckForNull Icon
    getOpenIcon(@NonNull FsConcurrentModel model) {
        return null;
    }

    /**
     * Returns the icon that should be displayed for the given archive file
     * if it's closed/collapsed in the view.
     * <p>
     * The implementation in the abstract class {@code ArchiveDriver} simply
     * returns {@code null}.
     *
     * @param  model the concurrent file system model.
     * @return The icon that should be displayed for the given archive file
     *         if it's closed/collapsed in the view.
     *         If {@code null} is returned, a default icon should be displayed.
     */
    public @CheckForNull Icon
    getClosedIcon(@NonNull FsConcurrentModel model) {
        return null;
    }
}
