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
import de.schlichtherle.truezip.util.Operation;
import de.schlichtherle.truezip.io.archive.ArchiveDescriptor;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem;
import de.schlichtherle.truezip.io.archive.input.ArchiveInputSocket;
import de.schlichtherle.truezip.io.archive.output.ArchiveOutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.concurrent.lock.ReentrantLock;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Set;
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
 * nested archive files (such as {@code "outer.zip/inner.tar.gz"}
 * and <i>false positives</i>, i.e. plain files or directories or file or
 * directory entries in an enclosing archive file which have been incorrectly
 * recognized to be <i>prospective archive files</i>.
 * <p>
 * To ensure that for each archive file there is at most one
 * {code ArchiveController}, the path name of the archive file (called
 * <i>mount point</i>) must be canonicalized, so it doesn't matter whether a
 * target archive file is addressed as {@code "archive.zip"} or
 * {@code "/dir/archive.zip"} if {@code "/dir"} is the client application's
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
public abstract class ArchiveController extends ArchiveDescriptor {

    /** This class cannot get instantiated outside its package. */
    ArchiveController() {}

    /**
     * {@inheritDoc}
     * <p>
     * Where the methods of this class accept a path name string as a
     * parameter, this must be a relative, hierarchical URI which is resolved
     * against this mount point.
     */
    @Override
    public abstract URI getMountPoint();

    /**
     * Returns the controller for the enclosing archive file of this
     * controller's target archive file or {@code null} if it's not enclosed
     * in another archive file.
     */
    public abstract ArchiveController getEnclController();

    /**
     * Resolves the given relative {@code path} against the relative path of
     * the target archive file within its enclosing archive file.
     *
     * @throws NullPointerException if the target archive file is not enclosed
     *         within another archive file.
     */
    public String getEnclPath(final String path) {
        final URI enclPath = getEnclController()
                .getMountPoint()
                .relativize(getMountPoint());
        final String result = isRoot(path)
                ? cutTrailingSeparators(enclPath.toString(), SEPARATOR_CHAR)
                : enclPath.resolve(path).toString();
        assert result.endsWith(path);
        assert !result.endsWith(SEPARATOR);
        return result;
    }

    /**
     * Runs the given {@link Operation} while this controller has
     * acquired its write lock regardless of the state of its read lock.
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
    public abstract <O extends IOOperation> O runWriteLocked(O operation)
    throws IOException;

    /**
     * Synchronizes the archive file only if the archive file has already new
     * data for the file system entry with the given path name.
     * <p>
     * <b>Warning:</b> As a side effect, all data structures returned by this
     * controller get reset (filesystem, entries, streams, etc.)!
     * As an implication, this method requires external synchronization on
     * this controller's write lock!
     * <p>
     * <b>TODO:</b> Consider adding configuration switch to allow overwriting
     * an archive entry to the same output archive multiple times, whereby
     * only the last written entry would be added to the central directory
     * of the archive (unless the archive type doesn't support this).
     *
     * @see    #sync(BitField, ArchiveSyncExceptionBuilder)
     * @throws ArchiveSyncException If any exceptional condition occurs
     *         throughout the processing of the target archive file.
     */
    public abstract void autoSync(final String path) throws ArchiveSyncException;

    public abstract boolean createNewFile(final String path, final boolean createParents) throws FalsePositiveException, IOException;

    public abstract boolean delete(final String path) throws FalsePositiveException;

    public abstract Icon getClosedIcon(final String path) throws FalsePositiveException;

    public abstract long getLastModified(final String path) throws FalsePositiveException;

    public abstract long getLength(final String path) throws FalsePositiveException;

    public abstract Icon getOpenIcon(final String path) throws FalsePositiveException;

    public abstract boolean isDirectory(final String path) throws FalsePositiveException;

    public abstract boolean isExisting(final String path) throws FalsePositiveException;

    public abstract boolean isFile(final String path) throws FalsePositiveException;

    public abstract boolean isReadable(final String path) throws FalsePositiveException;

    /**
     * Returns {@code true} if and only if the file system has been touched,
     * i.e. if an operation changed its state.
     */
    public abstract boolean isTouched();

    public abstract boolean isWritable(final String path) throws FalsePositiveException;

    public abstract Set<String> list(final String path) throws FalsePositiveException;

    public abstract boolean mkdir(final String path, final boolean createParents) throws FalsePositiveException;

    public abstract ReentrantLock readLock();

    public abstract boolean setLastModified(final String path, final long time) throws FalsePositiveException;

    public abstract void setReadOnly(final String path) throws FalsePositiveException, IOException;

    /**
     * Defines the available options for archive synchronization operations, i.e.
     * {@link ArchiveControllers#sync(URI, BitField, ArchiveSyncExceptionBuilder)}
     * and {@link ArchiveController#sync(BitField, ArchiveSyncExceptionBuilder)}.
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
         * an archive controller's target file because the client application has
         * forgot to {@link InputStream#close()} all {@code InputStream} objects
         * or another thread is still busy doing I/O on the target archive file.
         * Then if this property is {@code true}, the respective archive
         * controller will proceed to update the target archive file anyway and
         * finally throw an {@link ArchiveBusyWarningException} to indicate
         * that any subsequent operations on these streams will fail with an
         * {@link ArchiveEntryStreamClosedException} because they have been
         * forced to close.
         * <p>
         * If this property is {@code false}, the target archive file is
         * <em>not</em> updated and an {@link ArchiveBusyException} is thrown to
         * indicate that the application must close all entry input streams
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
         * If this property is {@code true}, the archive controller's target file
         * is completely released in order to enable subsequent read/write access
         * to it for third parties such as other processes <em>before</em> TrueZIP
         * can be used again to read from or write to the target archive file.
         * <p>
         * If this property is {@code true}, some temporary files might be retained
         * for caching in order to enable faster subsequent access to the archive
         * file again.
         * <p>
         * Note that temporary files are always deleted by TrueZIP unless the JVM
         * is terminated unexpectedly. This property solely exists to control
         * cooperation with third parties or enabling faster access.
         */
        UMOUNT,
        /**
         * Let's assume an archive controller's target file is enclosed in another
         * archive file.
         * Then if this property is {@code true}, the updated target archive file
         * is also written to its enclosing archive file.
         * Note that this property <em>must</em> be set to {@code true} if the
         * property {@code umount} is set to {@code true} as well.
         * Failing to comply to this requirement may throw an
         * {@link AssertionError} and will incur loss of data!
         */
        REASSEMBLE,
    }

    /**
     * Writes all changes to the contents of the target archive file to the
     * underlying file system.
     * As a side effect, all data structures returned by this controller get
     * reset (filesystem, entries, streams etc.)!
     * This method requires external synchronization on this controller's write
     * lock!
     *
     * @param options The non-{@code null} options for processing.
     * @throws NullPointerException if {@code options} or {@code builder} is
     * {@code null}.
     * @throws ArchiveSyncException if any exceptional condition occurs
     * throughout the processing of the target archive file.
     * @see ArchiveControllers#sync(URI, BitField, ArchiveSyncExceptionBuilder)
     */
    public abstract void sync(BitField<SyncOption> options, ArchiveSyncExceptionBuilder builder)
    throws ArchiveSyncException;

    public abstract ReentrantLock writeLock();

    public abstract ArchiveFileSystem autoMount(boolean autoCreate)
    throws FalsePositiveException, IOException;

    /**
     * Tests if the file system entry with the given path name has received or
     * is currently receiving new data via an output stream.
     * As an implication, the entry cannot receive new data from another
     * output stream before the next call to {@link #sync}.
     * Note that for directories this method will always return
     * {@code false}!
     */
    public abstract boolean hasNewData(String path);

    /**
     * Defines the available options for archive file system operations.
     * Not all available options may be applicable for all operations and
     * certain combinations may be useless or even illegal.
     * It's up to the particular operation to define which available options
     * are applicable for it and which combinations are supported.
     */
    public enum IOOption {

        /**
         * Whether or not any missing parent directory entries within an archive
         * file shall get created automatically.
         * If set, client applications do not need to call {@link #mkdir}
         * to create the parent directory entries of a file entry within an
         * archive file before they can write to it.
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
         * Whether or not a write operation shall append to or replace the contents
         * of a file entry within an archive file.
         */
        APPEND,
    }

    /**
     * Returns an input socket for reading the given entry from the
     * target archive file.
     *
     * @param  path a non-{@code null} entry in the virtual archive file
     *         system.
     * @return A non-{@code null} {@code ArchiveInputSocket}.
     */
    public abstract ArchiveInputSocket<?>
    getInputSocket(BitField<IOOption> options, String path)
    throws FalsePositiveException, IOException;

    /**
     * Returns an output socket for writing the given entry to the
     * target archive file.
     *
     * @param  path a non-{@code null} entry in the virtual archive file
     *         system.
     * @param  input a nullable archive input socket.
     * @return A non-{@code null} {@code ArchiveInputSocket}.
     */
    public abstract ArchiveOutputSocket<?>
    getOutputSocket(BitField<IOOption> options, String path)
    throws FalsePositiveException, IOException;
}
