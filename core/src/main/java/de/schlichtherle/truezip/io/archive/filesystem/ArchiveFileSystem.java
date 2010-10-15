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

import de.schlichtherle.truezip.io.filesystem.FileSystemEntry;
import de.schlichtherle.truezip.io.IOOperation;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.entry.CommonEntry;
import de.schlichtherle.truezip.io.entry.CommonEntry.Access;
import de.schlichtherle.truezip.io.entry.CommonEntry.Type;
import de.schlichtherle.truezip.io.entry.CommonEntryContainer;
import de.schlichtherle.truezip.util.Link;
import de.schlichtherle.truezip.util.BitField;

/**
 * A virtual file system for archive entries.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client classes.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface ArchiveFileSystem<CE extends CommonEntry>
extends CommonEntryContainer<ArchiveFileSystem.Entry<CE>> {

    /**
     * A marker interface which distinguishes entries created by an archive
     * file system from any other common entries, in particular those created
     * by the {@link ArchiveDriver#newEntry factory method} of an archive
     * driver.
     * With the help of this marker interface, an archive file system ensures
     * that when a new archive entry is created, the {@code template} parameter
     * is <i>not</i> an instance of this interface, but possibly a product of
     * the archive entry factory in the archive driver.
     * This enables an archive driver to copy properties specific to its type
     * of archive entries, e.g. the compressed size of ZIP entries.
     */
    interface Entry<CE extends CommonEntry> extends FileSystemEntry<CE> {
    }

    /**
     * Represents an operation on a chain of one or more archive file system
     * entries.
     * The operation is run by its {@link #run} method and the head of the
     * chain can be obtained by its {@link #getTarget} method.
     * <p>
     * Note that the state of the archive file system will not change until
     * the {@link #run} method is called!
     *
     * @see #mknod
     */
    interface Operation<CE extends CommonEntry>
    extends IOOperation, Link<Entry<CE>> {

        /** Executes this archive file system entry chain operation. */
        @Override
        void run() throws ArchiveFileSystemException;
    }

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
     * Begins a <i>transaction</i> to create or replace and finally link a
     * chain of one or more archive entries for the given {@code path} into
     * this archive file system.
     * <p>
     * To commit the transaction, call {@link Operation#run} on the
     * returned object, which will mark this archive file system as
     * {@link #isTouched() touched} and set the last modification time of the
     * created and linked archive file system entries to the system's current
     * time at the moment of the call to this method.
     *
     * @param  path a non-{@code null} relative path name.
     * @param  type a non-{@code null} common entry type.
     * @param  template if not {@code null}, then the archive file system entry
     *         at the end of the chain shall inherit as much properties from
     *         this common entry as possible - with the exception of its name
     *         and type.
     * @param  createParents if {@code true}, any missing parent directories
     *         will be created and linked into this archive file system with
     *         its last modification time set to the system's current time.
     * @throws NullPointerException if {@code path} or {@code type} are
     *         {@code null}.
     * @throws ArchiveReadOnlyExceptionn If this archive file system is read
     *         only.
     * @throws ArchiveFileSystemException If one of the following is true:
     *         <ul>
     *         <li>{@code path} contains characters which are not
     *             supported by the archive file.</li>
     *         <li>TODO: type is not {@code FILE} or {@code DIRECTORY}.</li>
     *         <li>The new entry already exists as a directory.</li>
     *         <li>The new entry shall be a directory, but already exists.</li>
     *         <li>A parent entry exists but is not a directory.</li>
     *         <li>A parent entry is missing and {@code createParents} is
     *             {@code false}.</li>
     *         </ul>
     * @return A new I/O operation on a chain of one or more archive file
     *         system entries for the given path name which will be linked
     *         into this archive file system upon a call to its
     *         {@link Operation#run} method.
     */
    Operation<CE> mknod(String path, Type type, CommonEntry template, boolean createParents)
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

    boolean setTime(String path, BitField<Access> types, long value)
    throws ArchiveFileSystemException;

    boolean isWritable(String path);

    void setReadOnly(String path)
    throws ArchiveFileSystemException;
}
