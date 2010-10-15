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
package de.schlichtherle.truezip.io.archive.controller;

import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem.Entry;
import de.schlichtherle.truezip.io.entry.CommonEntry;
import de.schlichtherle.truezip.io.entry.CommonEntry.Access;
import de.schlichtherle.truezip.io.entry.CommonEntry.Type;
import de.schlichtherle.truezip.io.filesystem.FileSystemController;
import de.schlichtherle.truezip.io.socket.InputOption;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputOption;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import java.io.IOException;
import javax.swing.Icon;

/**
 * @see     FileSystemController
 * @author  Christian Schlichtherle
 * @version $Id$
 */
interface ArchiveController<CE extends CommonEntry> {

    ArchiveModel getModel();

    boolean isTouched();

    /**
     * Writes all changes to the contents of the target archive file to the
     * underlying file system.
     * As a side effect,
     * all data structures get reset (filesystem, entries, streams etc.)!
     * This method requires synchronization on the write lock!
     *
     * @param  options The non-{@code null} options for processing.
     * @throws NullPointerException if {@code options} or {@code builder} is
     * {@code null}.
     * @throws SyncException if any exceptional condition occurs
     * throughout the processing of the target archive file.
     * @see    Controllers#sync(URI, ExceptionBuilder, BitField)
     */
    <E extends IOException>
    void sync(  ExceptionBuilder<? super SyncException, E> builder,
                BitField<SyncOption> options)
    throws E, NotWriteLockedException;

    Icon getOpenIcon()
    throws FalsePositiveException, NotWriteLockedException;

    Icon getClosedIcon()
    throws FalsePositiveException, NotWriteLockedException;

    boolean isReadOnly()
    throws FalsePositiveException, NotWriteLockedException;

    Entry<? extends CE> getEntry(String path)
    throws FalsePositiveException, NotWriteLockedException;

    boolean isReadable(String path)
    throws FalsePositiveException, NotWriteLockedException;

    boolean isWritable(String path)
    throws FalsePositiveException, NotWriteLockedException;

    void setReadOnly(String path)
    throws IOException, FalsePositiveException, NotWriteLockedException;

    boolean setTime(String path, BitField<Access> types, long value)
    throws IOException, FalsePositiveException, NotWriteLockedException;

    /**
     * Returns an input socket for reading the given entry from the file system.
     *
     * @param  path a non-{@code null} relative path name.
     * @throws IOException for some I/O related reason.
     * @return A non-{@code null} {@code InputSocket}.
     */
    InputSocket<? extends CE> getInputSocket(String path, BitField<InputOption> options)
    throws IOException, FalsePositiveException, NotWriteLockedException;

    /**
     * Returns an output socket for writing the given entry to the file system.
     *
     * @param  path a non-{@code null} relative path name.
     * @throws IOException for some I/O related reason.
     * @return A non-{@code null} {@code OutputSocket}.
     */
    OutputSocket<? extends CE> getOutputSocket(String path, BitField<OutputOption> options)
    throws IOException, FalsePositiveException, NotWriteLockedException;

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
    boolean mknod(  String path, Type type,
                    CommonEntry template,
                    BitField<OutputOption> options)
    throws IOException, FalsePositiveException, NotWriteLockedException;

    void unlink(String path)
    throws IOException, FalsePositiveException, NotWriteLockedException;
}
