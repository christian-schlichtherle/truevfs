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

import de.schlichtherle.truezip.io.IOOperation;
import de.schlichtherle.truezip.io.archive.ArchiveDescriptor;
import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem.Entry;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry.Access;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry.Type;
import de.schlichtherle.truezip.io.socket.entry.CommonEntryStreamClosedException;
import de.schlichtherle.truezip.io.socket.input.CommonInputSocket;
import de.schlichtherle.truezip.io.socket.output.CommonOutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.concurrent.lock.ReentrantLock;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

    /** The archive controller of the enclosing archive, if any. */
    private final ArchiveController<?> enclController;

    ArchiveController(final ArchiveModel<AE> model) {
        assert model != null;

        this.model = model;
        final URI enclMountPoint = model.getEnclMountPoint();
        enclController = null == enclMountPoint
                ? null
                : ArchiveControllers.getController(enclMountPoint);
        assert (null == enclMountPoint) == (null == enclController);
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

    final ArchiveController<?> getEnclController() {
        return enclController;
    }

    final String getEnclPath(String path) {
        return getModel().getEnclPath(path);
    }

    final File getTarget() {
        return getModel().getTarget();
    }

    /**
     * Returns {@code true} if and only if the target file of this
     * controller should be considered to be a file or directory in the host
     * file system.
     * Note that the target doesn't need to exist for this method to return
     * {@code true}.
     */
    // TODO: Move to UpdatingArchiveController and declare private.
    final boolean isHostFileSystemEntryTarget() {
        // True iff not enclosed or the enclosing archive file is actually
        // a plain directory.
        final ArchiveController enclController = getEnclController();
        return null == enclController
                || enclController.getTarget().isDirectory();
    }

    final ReentrantLock readLock() {
        return getModel().readLock();
    }

    final ReentrantLock writeLock() {
        return getModel().writeLock();
    }

    /**
     * Runs the given I/O operation while this controller has acquired its
     * write lock regardless of the state of its read lock.
     * You must use this method if this controller may have acquired a
     * read lock in order to prevent a dead lock.
     * <p>
     * <b>Warning:</b> This method temporarily releases the read lock
     * before the write lock is acquired and the runnable is run!
     * Hence, the runnable must retest the state of the controller
     * before it proceeds with any write operations.
     *
     * @param  operation the operation to run while the write lock is acquired.
     * @return {@code operation}
     */
    final <O extends IOOperation> O runWriteLocked(final O operation)
    throws IOException {
        assert operation != null;

        // A read lock cannot get upgraded to a write lock.
        // Hence the following mess is required.
        // Note that this is not just a limitation of the current
        // implementation in JSE 5: If automatic upgrading were implemented,
        // two threads holding a read lock try to upgrade concurrently,
        // they would dead lock each other!
        final int holdCount = readLock().getHoldCount();
        for (int c = holdCount; c-- > 0; )
                readLock().unlock();
        // The current thread may get blocked here!
        writeLock().lock();
        try {
            // First restore lock count to protect agains exceptions.
            for (int c = holdCount; c-- > 0; )
                readLock().lock();
            operation.run();
        } finally {
            writeLock().unlock(); // downgrade the lock
        }
        return operation;
    }

    abstract ArchiveFileSystem<?> autoMount(boolean autoCreate) throws IOException;

    abstract boolean hasNewData(String path);

    abstract void autoSync(final String path) throws ArchiveSyncException;

    /**
     * Defines the available options for archive file system operations.
     * Not all available options may be applicable for all operations and
     * certain combinations may be useless or even illegal.
     * It's up to the particular operation to define which available options
     * are applicable for it and which combinations are supported.
     */
    public enum IOOption {
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
         * Whether or not an operation is recursive.
         * This option affects only files and directories <em>below</em> the
         * operated node in the file system tree.
         */
        //RECURSIVE,
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
         * Suppose any other thread has still one or more archive entry input
         * streams open to an archive controller's target file.
         * Then if and only if this property is {@code true}, the respective
         * archive controller will wait until all other threads have closed
         * their archive entry input streams before proceeding with the update
         * of the target archive file.
         * Archive entry input streams opened (and not yet closed) by the
         * current thread are always ignored.
         * If the current thread gets interrupted while waiting, it will
         * stop waiting and proceed normally as if this property is
         * {@code false}.
         * <p>
         * Beware: If a stream has not been closed because the client
         * application does not always properly close its streams, even on an
         * {@link IOException} (which is a typical bug in many Java
         * applications), then the respective archive controller will not
         * return from the update until the current thread gets interrupted!
         */
        WAIT_FOR_INPUT_STREAMS,
        /**
         * Suppose there are any open input streams for any archive entries of
         * an archive controller's target file because the client application
         * has forgot to {@link InputStream#close()} all {@code InputStream}
         * objects or another thread is still busy doing I/O on the target
         * archive file.
         * Then if this property is {@code true}, the respective archive
         * controller will proceed to update the target archive file anyway and
         * finally throw an {@link ArchiveBusyWarningException} to indicate
         * that any subsequent operations on these streams will fail with an
         * {@link CommonEntryStreamClosedException} because they have been
         * forced to close.
         * <p>
         * If this property is {@code false}, the target archive file is
         * <em>not</em> updated and an {@link ArchiveBusyException} is thrown
         * to indicate that the application must close all entry input streams
         * first.
         */
        CLOSE_INPUT_STREAMS,
        /**
         * Similar to {@code waitInputStreams},
         * but applies to archive entry output streams instead.
         */
        WAIT_FOR_OUTPUT_STREAMS,
        /**
         * Similar to {@code closeInputStreams},
         * but applies to archive entry output streams instead.
         * <p>
         * If this parameter is {@code true}, then
         * {@code closeInputStreams} must be {@code true}, too.
         * Otherwise, an {@code IllegalArgumentException} is thrown.
         */
        CLOSE_OUTPUT_STREAMS,
        /**
         * If this property is {@code true}, the archive controller's target
         * file is completely released in order to enable subsequent read/write
         * access to it for third parties such as other processes
         * <em>before</em> TrueZIP can be used again to read from or write to
         * the target archive file.
         * <p>
         * If this property is {@code true}, some temporary files might be
         * retained for caching in order to enable faster subsequent access to
         * the archive file again.
         * <p>
         * Note that temporary files are always deleted by TrueZIP unless the
         * JVM is terminated unexpectedly. This property solely exists to
         * control cooperation with third parties or enabling faster access.
         */
        UMOUNT,
        /**
         * Let's assume an archive controller's target file is enclosed in
         * another archive file.
         * Then if this property is {@code true}, the updated target archive
         * file is also written to its enclosing archive file.
         * Note that this property <em>must</em> be set to {@code true} if the
         * property {@code umount} is set to {@code true} as well.
         * Failing to comply to this requirement may throw an
         * {@link AssertionError} and will incur loss of data!
         */
        REASSEMBLE,
    }

    public abstract Icon getOpenIcon() throws FalsePositiveException;

    public abstract Icon getClosedIcon() throws FalsePositiveException;

    public abstract boolean isReadOnly() throws FalsePositiveException;

    public abstract void setReadOnly(String path) throws IOException;

    public abstract boolean isReadable(String path) throws FalsePositiveException;

    public abstract boolean isWritable(String path) throws FalsePositiveException;

    public abstract Entry<?> getEntry(String path) throws FalsePositiveException;

    public abstract void setTime(String path, BitField<Access> types, long value)
    throws IOException;

    /**
     * Returns an archive input socket for reading the given entry from the
     * target archive file.
     *
     * @param  path a non-{@code null} relative path name.
     * @throws FalsePositiveException if the target archive file is a false
     *         positive.
     * @throws IOException for some other I/O related reason.
     * @return A non-{@code null} {@code CommonInputSocket}.
     */
    public abstract CommonInputSocket<? extends CommonEntry>
    getInputSocket(String path)
    throws IOException;

    /**
     * Returns an archive output socket for writing the given entry to the
     * target archive file.
     *
     * @param  path a non-{@code null} relative path name.
     * @throws FalsePositiveException if the target archive file is a false
     *         positive.
     * @throws IOException for some other I/O related reason.
     * @return A non-{@code null} {@code CommonInputSocket}.
     */
    public abstract CommonOutputSocket<? extends CommonEntry>
    getOutputSocket(String path, BitField<IOOption> options)
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
     * @throws FalsePositiveException if the target archive file is a false
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
    public abstract void mknod(String path, Type type, CommonEntry template, BitField<IOOption> options)
    throws IOException;

    /** Currently supports no options. */
    public abstract void unlink(String path, BitField<IOOption> options)
    throws IOException;

    /**
     * Writes all changes to the contents of the target archive file to the
     * underlying file system.
     * As a side effect, all data structures returned by this controller get
     * reset (filesystem, entries, streams etc.)!
     * This method requires external synchronization on this controller's write
     * lock!
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
