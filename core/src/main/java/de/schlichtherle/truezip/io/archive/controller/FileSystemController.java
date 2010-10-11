/*
 * Copyright (C) 2004-2010 Schlichtherle IT Services
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

import java.net.URI;
import de.schlichtherle.truezip.io.socket.OutputOption;
import de.schlichtherle.truezip.io.socket.InputOption;
import de.schlichtherle.truezip.io.socket.FileSystemEntry;
import de.schlichtherle.truezip.io.socket.CommonEntry;
import de.schlichtherle.truezip.io.socket.CommonEntry.Access;
import de.schlichtherle.truezip.io.socket.CommonEntry.Type;
import de.schlichtherle.truezip.io.socket.InputClosedException;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputClosedException;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import javax.swing.Icon;

import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.SEPARATOR;
import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.SEPARATOR_CHAR;
import static de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystems.isRoot;
import static de.schlichtherle.truezip.io.Paths.cutTrailingSeparators;

/**
 * Provides multi-threaded read/write access to its <i>target archive file</i>
 * as if it were a directory in the underlying file system.
 * <p>
 * Each instance of this class maintains a virtual file system, provides input
 * and output streams for the entries of the archive file and methods
 * to update the contents of the virtual file system to the target file
 * in the real file system.
 * In cooperation with the calling methods, it also knows how to deal with
 * nested archive files (such as {@code outer.zip/inner.tar.gz}
 * and <i>false positives</i>, i.e. plain files or directories or file or
 * directory entries in an enclosing archive file which have been incorrectly
 * recognized to be <i>prospective archive files</i>.
 * <p>
 * To ensure that for each archive file there is at most one
 * {code FileSystemController}, the path name of the archive file - called
 * <i>mount point</i> - must be canonicalized, so it doesn't matter whether a
 * target archive file is addressed as {@code archive.zip} or
 * {@code /dir/archive.zip} if {@code /dir} is the client application's
 * current directory.
 * <p>
 * Note that in general all of its methods are reentrant on exceptions - so
 * client applications may repeatedly call them.
 * However, depending on the context, some or all of the archive file's data
 * may be lost in this case - see the Javadoc for the respective exception.
 * <p>
 * Where the methods of this class accept a path name string as a parameter,
 * this must be a relative, hierarchical URI which is resolved against the
 * {@link FileSystemModel#getMountPoint() mount point} of this controller's
 * {@link #getModel() model}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class FileSystemController {

    protected abstract FileSystemModel getModel();

    /** Returns {@code "controller:" + }{@link #getMountPoint()}{@code .}{@link Object#toString()}. */
    @Override
    public final String toString() {
        return "controller:" + getModel().getMountPoint();
    }

    /**
     * Defines the available options for archive synchronization operations, i.e.
     * {@link FileSystemControllers#sync(URI, ArchiveSyncExceptionBuilder, BitField)}
     * and {@link FileSystemController#sync(ArchiveSyncExceptionBuilder, BitField)}.
     */
    public enum SyncOption {
        /**
         * Suppose there are any open input streams or read only files for any
         * archive entries of an archive controller's target archive file.
         * Then if this option is set, the archive controller waits until all
         * <em>other</em> threads have closed their archive entry input streams
         * and read only files before proceeding with the update of the target
         * archive file.
         * Archive input streams and read only files opened by the
         * <em>current</em> thread are always ignored.
         * If the current thread gets interrupted while waiting, it will
         * stop waiting and proceed normally as if this options wasn't set.
         * <p>
         * Beware: If a stream has not been closed because the client
         * application does not always properly close its streams, even on an
         * {@link IOException} (which is a typical bug in many Java
         * applications), then the respective archive controller will not
         * return from the update until the current thread gets interrupted!
         */
        WAIT_CLOSE_INPUT,

        /**
         * Suppose there are any open input streams or read only files for any
         * archive entries of an archive controller's target archive file.
         * Then if this option is set, the archive controller will proceed to
         * update the target archive file anyway and finally throw an
         * {@link ArchiveBusyWarningException} to indicate that any subsequent
         * operations on these streams will fail with an
         * {@link InputClosedException} because they have been forced to
         * close.
         * <p>
         * If this option is not set, the target archive file is <em>not</em>
         * updated and an {@link ArchiveBusyException} is thrown to indicate
         * that the application must close all entry input streams and read
         * only files first.
         */
        FORCE_CLOSE_INPUT,

        /**
         * Similar to {@link #WAIT_CLOSE_INPUT},
         * but applies to archive entry output streams instead.
         */
        WAIT_CLOSE_OUTPUT,

        /**
         * Similar to {@link #FORCE_CLOSE_INPUT},
         * but applies to archive entry output streams and may throw a
         * {@link OutputClosedException} instead.
         * <p>
         * If this option is set, then
         * {@link #FORCE_CLOSE_INPUT} must be set, too.
         * Otherwise, an {@code IllegalArgumentException} is thrown.
         */
        FORCE_CLOSE_OUTPUT,

        /**
         * If this option is set, all pending changes are aborted.
         * This option will leave a corrupted target archive file and is only
         * meaningful if the target archive file gets deleted immediately.
         */
        ABORT_CHANGES,

        /**
         * Suppose an archive controller has closed input or output buffers for
         * archive entries.
         * Then if this option is set, these closed buffers get written to the
         * target archive file when it gets synchronized.
         * Not that open buffers will never get reassembled.
         *
         * @deprecated TODO: Implement this!
         */
        REASSEMBLE_BUFFERS,
    }

    public abstract boolean isTouched();

    public abstract Icon getOpenIcon();

    public abstract Icon getClosedIcon();

    public abstract boolean isReadOnly();

    public abstract FileSystemEntry getEntry(String path);

    public abstract boolean isReadable(String path);

    public abstract boolean isWritable(String path);

    public abstract void setReadOnly(String path) throws IOException;

    public abstract boolean setTime(String path, BitField<Access> types, long value)
    throws IOException;

    /**
     * Returns an archive input socket for reading the given entry from the
     * target archive file.
     *
     * @param  path a non-{@code null} relative path name.
     * @throws FalsePositiveEntryException if the target archive file is a false
     *         positive.
     * @throws IOException for some other I/O related reason.
     * @return A non-{@code null} {@code InputSocket}.
     */
    public abstract InputSocket<?> newInputSocket(
            String path, BitField<InputOption> options)
    throws IOException;

    /**
     * Returns an archive output socket for writing the given entry to the
     * target archive file.
     *
     * @param  path a non-{@code null} relative path name.
     * @throws FalsePositiveEntryException if the target archive file is a false
     *         positive.
     * @throws IOException for some other I/O related reason.
     * @return A non-{@code null} {@code InputSocket}.
     */
    public abstract OutputSocket<?> newOutputSocket(
            String path, BitField<OutputOption> options)
    throws IOException;

    /**
     * Creates or replaces and finally links a chain of one or more archive
     * entries for the given {@code path} into the archive file system.
     *
     * @param  path a non-{@code null} relative path name.
     * @param  type a non-{@code null} common entry type.
     * @param  template if not {@code null}, then the archive file system entry
     *         at the end of the chain shall inherit as much properties from
     *         this common entry as possible - with the exception of its name
     *         and type.
     * @param  options if {@code CREATE_PARENTS} is set, any missing parent
     *         directories will be created and linked into this archive file
     *         system with its last modification time set to the system's
     *         current time.
     * @throws NullPointerException if {@code path} or {@code type} are
     *         {@code null}.
     * @throws FalsePositiveEntryException if the target archive file is a false
     *         positive.
     * @throws IOException for some other I/O related reason, including but
     *         not exclusively upon one or more of the following conditions:
     *         <ul>
     *         <li>The archive file system is read only.</li>
     *         <li>{@code path} contains characters which are not
     *             supported by the archive file.</li>
     *         <li>TODO: type is not {@code FILE} or {@code DIRECTORY}.</li>
     *         <li>The new entry already exists as a directory.</li>
     *         <li>The new entry shall be a directory, but already exists.</li>
     *         <li>A parent entry exists but is not a directory.</li>
     *         <li>A parent entry is missing and {@code createParents} is
     *             {@code false}.</li>
     *         </ul>
     */
    public abstract boolean mknod(  String path, Type type,
                                    CommonEntry template,
                                    BitField<OutputOption> options)
    throws IOException;

    /** Currently supports no options. */
    public abstract void unlink(String path) throws IOException;

    /**
     * Writes all changes to the contents of the target archive file to the
     * underlying file system.
     * As a side effect,
     * all data structures get reset (filesystem, entries, streams etc.)!
     * This method requires synchronization on the write lock!
     *
     * @param  options The non-{@code null} options for processing.
     * @throws NullPointerException if {@code options} or {@code builder} is
     *         {@code null}.
     * @throws ArchiveSyncException if any exceptional condition occurs
     *         throughout the processing of the target archive file.
     * @see    FileSystemControllers#sync(URI, ArchiveSyncExceptionBuilder, BitField)
     */
    // TODO: Hide this!
    public abstract void sync(  ArchiveSyncExceptionBuilder builder,
                                BitField<SyncOption> options)
    throws ArchiveSyncException;
}
