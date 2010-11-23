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
package de.schlichtherle.truezip.io.filesystem;

import de.schlichtherle.truezip.io.archive.controller.Archives;
import de.schlichtherle.truezip.io.entry.CommonEntry;
import de.schlichtherle.truezip.io.entry.CommonEntry.Access;
import de.schlichtherle.truezip.io.entry.CommonEntry.Type;
import de.schlichtherle.truezip.io.socket.InputOption;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputOption;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import java.io.IOException;
import javax.swing.Icon;

/**
 * A file system controller provides thread-safe read/write access
 * to a federation of one or more file systems which is addressed by the
 * {@link FileSystemModel#getMountPoint() mount point} of its
 * {@link #getModel() file system model}.
 * <p>
 * All method implementations of this abstract base class must be reentrant on
 * exceptions - so client applications may repeatedly call them.
 * <p>
 * Where the methods of this abstract base class accept a path name string as a
 * parameter, this must be a relative, hierarchical URI which is resolved
 * against the {@link FileSystemModel#getMountPoint() mount point} of this
 * controller's federated file system.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class FileSystemController<CE extends CommonEntry> {

    /** Returns the non-{@code null} file system model. */
    public abstract FileSystemModel getModel();

    public abstract Icon getOpenIcon() throws FileSystemException;

    public abstract Icon getClosedIcon() throws FileSystemException;

    public abstract boolean isReadOnly() throws FileSystemException;

    public abstract FileSystemEntry<? extends CE> getEntry(String path)
    throws FileSystemException;

    public abstract boolean isReadable(String path) throws FileSystemException;

    public abstract boolean isWritable(String path) throws FileSystemException;

    public abstract void setReadOnly(String path) throws IOException;

    public abstract boolean setTime(String path,
                                    BitField<Access> types, long value)
    throws IOException;

    /**
     * Returns an input socket for reading the given entry from the file
     * system.
     *
     * @param  path a non-{@code null} relative path name.
     * @return A non-{@code null} {@code InputSocket}.
     */
    public abstract InputSocket<? extends CE> getInputSocket(
            String path, BitField<InputOption> options);

    /**
     * Returns an output socket for writing the given entry to the file
     * system.
     *
     * @param  path a non-{@code null} relative path name.
     * @return A non-{@code null} {@code OutputSocket}.
     */
    public abstract OutputSocket<? extends CE> getOutputSocket(
            String path, BitField<OutputOption> options, CommonEntry template);

    /**
     * Creates or replaces and finally links a chain of one or more entries
     * for the given {@code path} into the file system.
     *
     * @param  path a non-{@code null} relative path name.
     * @param  type a non-{@code null} common entry type.
     * @param  template if not {@code null}, then the file system entry
     *         at the end of the chain shall inherit as much properties from
     *         this common entry as possible - with the exception of its name
     *         and type.
     * @param  options if {@code CREATE_PARENTS} is set, any missing parent
     *         directories will be created and linked into this file
     *         system with its last modification time set to the system's
     *         current time.
     * @throws NullPointerException if {@code path} or {@code type} are
     *         {@code null}.
     * @throws IOException for some other I/O related reason, including but
     *         not exclusively upon one or more of the following conditions:
     *         <ul>
     *         <li>The file system is read only.</li>
     *         <li>{@code path} contains characters which are not
     *             supported by the file system.</li>
     *         <li>TODO: type is not {@code FILE} or {@code DIRECTORY}.</li>
     *         <li>The new entry already exists as a directory.</li>
     *         <li>The new entry shall be a directory, but already exists.</li>
     *         <li>A parent entry exists but is not a directory.</li>
     *         <li>A parent entry is missing and {@code createParents} is
     *             {@code false}.</li>
     *         </ul>
     */
    public abstract boolean mknod(  String path, Type type,
                                    BitField<OutputOption> options,
                                    CommonEntry template)
    throws IOException;

    public abstract void unlink(String path) throws IOException;

    /**
     * Writes all changes to the contents of this file system to its
     * parent file system.
     *
     * @param  <E> the type of the assembled {@code IOException} to throw.
     * @param  builder the non-{@code null} exception builder to use for the
     *         assembly of an {@code IOException} from the given
     *         {@code SyncException}s.
     * @param  options the non-{@code null} synchronization options.
     * @throws IOException if any exceptional condition occurs throughout the
     *         synchronization of this file system.
     * @see    FileSystemModel#isTouched
     * @see    Archives#sync
     */
    public abstract <E extends IOException>
    void sync(  ExceptionBuilder<? super SyncException, E> builder,
                BitField<SyncOption> options)
    throws E, FileSystemException;

    @Override
    public final String toString() {
        return new StringBuilder()
                .append(getClass().getName())
                .append(":")
                .append(getModel().getMountPoint().toString())
                .toString();
    }
}
