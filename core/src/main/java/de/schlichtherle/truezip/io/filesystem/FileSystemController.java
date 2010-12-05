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

import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.entry.Entry.Access;
import de.schlichtherle.truezip.io.entry.Entry.Type;
import de.schlichtherle.truezip.io.socket.InputOption;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputOption;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import java.io.IOException;
import javax.swing.Icon;

/**
 * Provides read/write access to a file system.
 * The file system is addressed by the
 * {@link FileSystemModel#getMountPoint() mount point} of the
 * {@link #getModel() file system model}.
 * <p>
 * Where the methods of this interface accept a
 * {@link FileSystemEntry#getName path name} string as a parameter, this will
 * be resolved against the
 * {@link FileSystemModel#getMountPoint() mount point} URI of this
 * controller's file system.
 * <p>
 * All method implementations of this interface must be reentrant on
 * exceptions - so client applications may repeatedly call them.
 * However, there is no requirement for an implementation to be thread-safe.
 * Therefore, all implementations should clearly state their level of
 * thread-safety.
 * <p>
 * <b>Warning:</b> You should <em>not</em> implement this interface directly,
 * but rather extend one of the provided implementation classes instead.
 * This is required so that future versions of TrueZIP can add more methods to
 * this interface without breaking binary compatibility to your existing
 * application!
 *
 * @param   <E> The type of the entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface FileSystemController<E extends Entry> {

    /** Returns the non-{@code null} file system model. */
    FileSystemModel getModel();

    /**
     * Returns the controller for the parent federated file system or
     * {@code null} if and only if this file system is not a member of another
     * file system.
     */
    FederatedFileSystemController<?> getParent();

    Icon getOpenIcon() throws FileSystemException;

    Icon getClosedIcon() throws FileSystemException;

    boolean isReadOnly() throws FileSystemException;

    FileSystemEntry<? extends E> getEntry(String path)
    throws FileSystemException;

    boolean isReadable(String path) throws FileSystemException;

    boolean isWritable(String path) throws FileSystemException;

    void setReadOnly(String path) throws IOException;

    boolean setTime(String path, BitField<Access> types, long value)
    throws IOException;

    /**
     * Returns an input socket for reading the given entry from the file
     * system.
     *
     * @param  path a non-{@code null} relative path name.
     * @return A non-{@code null} {@code InputSocket}.
     */
    InputSocket<? extends E> getInputSocket(
            String path, BitField<InputOption> options);

    /**
     * Returns an output socket for writing the given entry to the file
     * system.
     *
     * @param  path a non-{@code null} relative path name.
     * @return A non-{@code null} {@code OutputSocket}.
     */
    OutputSocket<? extends E> getOutputSocket(
            String path, BitField<OutputOption> options, Entry template);

    /**
     * Creates or replaces and finally links a chain of one or more entries
     * for the given {@code path} into the file system.
     *
     * @param  path a non-{@code null} relative path name.
     * @param  type a non-{@code null} entry type.
     * @param  template if not {@code null}, then the file system entry
     *         at the end of the chain shall inherit as much properties from
     *         this entry as possible - with the exception of its name and type.
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
    boolean mknod(  String path, Type type, BitField<OutputOption> options,
                    Entry template)
    throws IOException;

    void unlink(String path) throws IOException;

    /**
     * Writes all changes to the contents of this file system to its
     * parent file system.
     *
     * @param  <X> the type of the assembled {@code IOException} to throw.
     * @param  builder the non-{@code null} exception builder to use for the
     *         assembly of an {@code IOException} from the given
     *         {@code SyncException}s.
     * @param  options the non-{@code null} synchronization options.
     * @throws IOException if any exceptional condition occurs throughout the
     *         synchronization of this file system.
     * @see    FileSystemModel#isTouched
     * @see    FileSystemManager#sync
     */
    <X extends IOException>
    void sync(  ExceptionBuilder<? super SyncException, X> builder,
                BitField<SyncOption> options)
    throws X, FileSystemException;
}
