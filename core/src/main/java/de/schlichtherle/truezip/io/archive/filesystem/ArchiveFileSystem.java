/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.archive.filesystem;

import de.schlichtherle.truezip.io.IOOperation;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.socket.common.CommonEntry.Type;
import de.schlichtherle.truezip.io.socket.common.CommonEntryContainer;
import de.schlichtherle.truezip.io.socket.common.CommonEntry;
import de.schlichtherle.truezip.io.socket.IOReference;
import java.util.Set;

/**
 * A virtual file system for archive entries.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client classes.
 *
 * @param   <AE> The type of the archive entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface ArchiveFileSystem<AE extends ArchiveEntry>
extends CommonEntryContainer<AE> {

    /**
     * Returns {@code true} if and only if this archive file system is
     * read-only.
     */
    boolean isReadOnly();

    /**
     * Returns {@code true} if and only if this archive file system has been
     * modified since its time of creation.
     */
    boolean isTouched();

    /**
     * An I/O operation for linking an entry into an archive file system.
     * The linked entry may replace an existing entry.
     *
     * @see #mknod
     */
    interface Link<AE extends ArchiveEntry>
    extends IOOperation, IOReference<AE> {

        /** Links an entry into an archive file system. */
        @Override
        void run()
        throws ArchiveFileSystemException;
    }

    /**
     * Begins a &quot;create and link target&quot; transaction to ensure that
     * either a new target for the given {@code path} will be created or an
     * existing target is replaced within this archive file system.
     * <p>
     * This is the first step of a two-step process to create an archive target
     * and link it into this virtual archive file system.
     * To commit the transaction, call {@link IOOperation#run} on the
     * returned object after you have successfully conducted the operations
     * which compose the transaction.
     * <p>
     * Upon a {@code run} operation, the last modification time of
     * the newly created and linked entries will be set to the system's
     * current time at the moment the transaction has begun and the file
     * system will be marked as touched at the moment the transaction has
     * been committed.
     * <p>
     * Note that there is no rollback operation: After this method returns,
     * nothing in the virtual file system has changed yet and all information
     * required to commit the transaction is contained in the returned object.
     * Hence, if the operations which compose the transaction fails, the
     * returned object may be safely collected by the garbage collector,
     *
     * @param  path The relative path name of the target to create or replace.
     * @param  template If not {@code null}, then the newly created or
     *         replaced target shall inherit as much properties from this
     *         instance as possible (with the exception of the name).
     *         This is typically used for archive copy operations and requires
     *         some support by the archive driver.
     * @param  createParents If {@code true}, any missing parent
     *         directory will be created in this file system with its last
     *         modification time set to the system's current time.
     * @return An I/O operation. You must call its {@link IOOperation#run}
     *         method in order to link the newly created target into this
     *         archive file system.
     * @throws ArchiveReadOnlyExceptionn If this virtual archive file system
     *         is read only.
     * @throws ArchiveFileSystemException If one of the following is true:
     *         <ul>
     *         <li>{@code path} contains characters which are not
     *             supported by the archive file.
     *         <li>The target name indicates a directory (trailing {@code /})
     *             and its target does already exist within this file system.
     *         <li>The target is a file or directory and does already exist as
     *             the respective other type within this file system.
     *         <li>The parent directory does not exist and
     *             {@code createParents} is {@code false}.
     *         <li>One of the target's parents denotes a file.
     *         </ul>
     */
    Link<AE> mknod(String path, Type type, CommonEntry template, boolean createParents)
    throws ArchiveFileSystemException;

    /**
     * If this method returns, the file system entry identified by the given
     * {@code path} has been successfully deleted from this archive file
     * system.
     * If the file system entry is a directory, it must be empty for successful
     * deletion.
     *
     * @throws ArchiveReadOnlyExceptionn If this virtual archive file system is
     *         read-only.
     * @throws ArchiveFileSystemException If the operation fails for some other
     *         reason.
     */
    void unlink(String path)
    throws ArchiveFileSystemException;

    boolean setLastModified(String path, long time)
    throws ArchiveFileSystemException;

    /**
     * Returns an unmodifiable set of the base names of the members
     * of the directory identified by {@code path} or {@code null} if no
     * directory entry exists for the given path name in this virtual archive
     * file system.
     */
    Set<String> list(final String path);

    boolean isWritable(String path);

    void setReadOnly(String path)
    throws ArchiveFileSystemException;
}
