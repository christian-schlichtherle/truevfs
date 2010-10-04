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

import de.schlichtherle.truezip.io.archive.descriptor.ArchiveDescriptor;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem.Entry;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry.Access;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry.Type;
import de.schlichtherle.truezip.io.socket.input.CommonInputClosedException;
import de.schlichtherle.truezip.io.socket.input.CommonInputSocket;
import de.schlichtherle.truezip.io.socket.output.CommonOutputClosedException;
import de.schlichtherle.truezip.io.socket.output.CommonOutputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import java.net.URI;
import javax.swing.Icon;

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
 * {code ArchiveController}, the path name of the archive file - called
 * <i>mount point</i> - must be canonicalized, so it doesn't matter whether a
 * target archive file is addressed as {@code archive.zip} or
 * {@code /dir/archive.zip} if {@code /dir} is the client application's
 * current directory.
 * <p>
 * Note that in general all of its methods are reentrant on exceptions - so
 * client applications may repeatedly call them.
 * However, depending on the context, some or all of the archive file's data
 * may be lost in this case - see the Javadoc for the respective exception.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class ArchiveController<AE extends ArchiveEntry>
implements ArchiveDescriptor {

    private final ArchiveModel<AE> model;

    ArchiveController(final ArchiveModel<AE> model) {
        assert model != null;
        this.model = model;
    }

    final ArchiveModel<AE> getModel() {
        return model;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Where the methods of this class accept a path name string as a
     * parameter, this must be a relative, hierarchical URI which is resolved
     * against this mount point.
     */
    @Override
    public final URI getMountPoint() {
        return getModel().getMountPoint();
    }

    /** Returns {@code "controller:" + }{@link #getMountPoint()}{@code .}{@link Object#toString()}. */
    @Override
    public final String toString() {
        return "controller:" + getMountPoint().toString();
    }

    ArchiveController<?> getEnclController() {
        final URI enclMountPoint = getModel().getEnclMountPoint();
        return null == enclMountPoint ? null : ArchiveControllers.getController(enclMountPoint);
    }

    final String getEnclPath(String path) {
        return getModel().getEnclPath(path);
    }

    /**
     * Defines the available options for archive file system operations.
     * Not all available options may be applicable for all operations and
     * certain combinations may be useless or even illegal.
     * It's up to the particular operation to define which available options
     * are applicable for it and which combinations are supported.
     */
    public enum IOOption { // FIXME: Make top level class!
        /**
         * Whether or not any missing parent directory entries within an
         * archive file shall get created automatically.
         * If set, client applications do not need to call
         * {@link ArchiveController#mknod} to create the parent directory
         * entries of a file entry within an archive file before they can write
         * to it.
         */
        CREATE_PARENTS,

        /**
         * Whether or not a copy operation shall preserve as much attributes
         * of a file or directory entry within an archive file as possible.
         */
        PRESERVE,

        /**
         * Whether or not a write operation shall append to or replace the
         * contents of a file entry within an archive file.
         */
        APPEND,
    }

    /**
     * Defines the available options for archive synchronization operations, i.e.
     * {@link ArchiveControllers#sync(URI, ArchiveSyncExceptionBuilder, BitField)}
     * and {@link ArchiveController#sync(ArchiveSyncExceptionBuilder, BitField)}.
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
         * {@link CommonInputClosedException} because they have been forced to
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
         * {@link CommonOutputClosedException} instead.
         * <p>
         * If this option is set, then
         * {@link #FORCE_CLOSE_INPUT} must be set, too.
         * Otherwise, an {@code IllegalArgumentException} is thrown.
         */
        FORCE_CLOSE_OUTPUT,

        /**
         * If this option is set, all pending changes are aborted.
         * This option will leave a corrupted target archive file and is only
         * meaningful before the target archive file is deleted.
         */
        ABORT_CHANGES,

        /**
         * If this options is set, the archive controller's target file is
         * completely released in order to enable subsequent read/write
         * access to it for third parties such as other processes
         * <em>before</em> TrueZIP can be used again to read from or write to
         * the target archive file.
         * <p>
         * If this option is <em>not</em> set, some temporary files might be
         * retained for caching in order to enable faster subsequent access to
         * the archive file again.
         * <p>
         * Note that temporary files are always deleted by TrueZIP unless the
         * JVM is terminated unexpectedly. This property solely exists to
         * control cooperation with third parties or enabling faster access.
         */
        UMOUNT,

        /**
         * Suppose an archive controller's target archive file is enclosed in
         * another archive file.
         * Then if this options is set, the updated target archive file is
         * also written to its enclosing archive file.
         * Note that this option <em>must</em> be set if the property
         * {@code umount} is set, too.
         * Otherwise, an {@code IllegalArgumentException} is thrown.
         */
        REASSEMBLE,
    }

    public abstract Icon getOpenIcon()
    throws FalsePositiveEntryException;

    public abstract Icon getClosedIcon()
    throws FalsePositiveEntryException;

    public abstract boolean isReadOnly()
    throws FalsePositiveEntryException;

    public abstract Entry<?> getEntry(String path)
    throws FalsePositiveEntryException;

    public abstract boolean isReadable(String path)
    throws FalsePositiveEntryException;

    public abstract boolean isWritable(String path)
    throws FalsePositiveEntryException;

    public abstract void setReadOnly(String path)
    throws IOException;

    public abstract void setTime(String path, BitField<Access> types, long value)
    throws IOException;

    /**
     * Returns an archive input socket for reading the given entry from the
     * target archive file.
     *
     * @param  path a non-{@code null} relative path name.
     * @throws FalsePositiveEntryException if the target archive file is a false
     *         positive.
     * @throws IOException for some other I/O related reason.
     * @return A non-{@code null} {@code CommonInputSocket}.
     */
    public abstract CommonInputSocket<?> newInputSocket(String path)
    throws IOException;

    /**
     * Returns an archive output socket for writing the given entry to the
     * target archive file.
     *
     * @param  path a non-{@code null} relative path name.
     * @throws FalsePositiveEntryException if the target archive file is a false
     *         positive.
     * @throws IOException for some other I/O related reason.
     * @return A non-{@code null} {@code CommonInputSocket}.
     */
    public abstract CommonOutputSocket<?> newOutputSocket(
            String path, BitField<IOOption> options)
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
    public abstract void mknod( String path, Type type, CommonEntry template,
                                BitField<IOOption> options)
    throws IOException;

    /** Currently supports no options. */
    public abstract void unlink(String path, BitField<IOOption> options)
    throws IOException;

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
     * @see    ArchiveControllers#sync(URI, ArchiveSyncExceptionBuilder, BitField)
     */
    public abstract void sync(  ArchiveSyncExceptionBuilder builder,
                                BitField<SyncOption> options)
    throws ArchiveSyncException;
}
