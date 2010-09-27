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
import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry.Type;
import de.schlichtherle.truezip.io.socket.entry.CommonEntryContainer;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry;
import de.schlichtherle.truezip.io.socket.IOReference;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry.Access;
import de.schlichtherle.truezip.io.socket.input.CommonInputService;
import de.schlichtherle.truezip.io.socket.output.CommonOutputService;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import java.io.IOException;

/**
 * A virtual file system for archive entries.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client classes.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface ArchiveFileSystem<AE extends ArchiveEntry>
extends CommonEntryContainer<ArchiveFileSystem.Entry<AE>> {

    /** An archive file system entry which refers to an archive entry. */
    public interface Entry<AE extends ArchiveEntry>
    extends ArchiveFileSystemEntry, IOReference<AE> {
    }

    /**
     * Represents an I/O operation on a chain of one or more archive file
     * system entries.
     * The operation is run by its {@link #run} method and the head of the
     * chain can be obtained by its {@link #getTarget} method.
     * <p>
     * Note that the state of the archive file system will not change until
     * the {@link #run} method is called!
     *
     * @see #mknod
     */
    interface EntryOperation<AE extends ArchiveEntry>
    extends IOOperation, IOReference<Entry<AE>> {

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
     * To commit the transaction, call {@link EntryOperation#run} on the
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
     *         {@link EntryOperation#run} method.
     */
    EntryOperation<AE> mknod(String path, Type type, CommonEntry template, boolean createParents)
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

    void setTime(String path, BitField<Access> types, long value)
    throws ArchiveFileSystemException;

    boolean isWritable(String path);

    void setReadOnly(String path)
    throws ArchiveFileSystemException;

    public <E extends Exception> void copy(
            CommonInputService<AE> input,
            CommonOutputService<AE> output,
            ExceptionHandler<? super IOException, E> handler)
    throws E;
}
