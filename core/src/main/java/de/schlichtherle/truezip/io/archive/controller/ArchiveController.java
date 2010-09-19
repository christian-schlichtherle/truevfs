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

import de.schlichtherle.truezip.io.archive.ArchiveDescriptor;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem;
import de.schlichtherle.truezip.io.archive.input.ArchiveInputStreamSocket;
import de.schlichtherle.truezip.io.archive.output.ArchiveOutputStreamSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.Operation;
import de.schlichtherle.truezip.util.concurrent.lock.ReentrantLock;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Set;
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
public interface ArchiveController extends ArchiveDescriptor {

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
    void autoSync(final String path) throws ArchiveSyncException;

    boolean createNewFile(final String path, final boolean createParents) throws FalsePositiveException, IOException;

    boolean delete(final String path) throws FalsePositiveException;

    Icon getClosedIcon(final String path) throws FalsePositiveException;

    /**
     * Returns the controller for the enclosing archive file of this
     * controller's target archive file or {@code null} if it's not enclosed
     * in another archive file.
     */
    ArchiveController getEnclController();

    /**
     * Resolves the given relative {@code path} against the relative path of
     * the target archive file within its enclosing archive file.
     *
     * @throws NullPointerException if the target archive file is not enclosed
     *         within another archive file.
     */
    String getEnclPath(final String path);

    long getLastModified(final String path) throws FalsePositiveException;

    long getLength(final String path) throws FalsePositiveException;

    /**
     * {@inheritDoc}
     * <p>
     * Where the methods of this interface accept a path name string as a
     * parameter, this must be a relative, hierarchical URI which is resolved
     * against this mount point.
     */
    URI getMountPoint();

    Icon getOpenIcon(final String path) throws FalsePositiveException;

    boolean isDirectory(final String path) throws FalsePositiveException;

    boolean isExisting(final String path) throws FalsePositiveException;

    boolean isFile(final String path) throws FalsePositiveException;

    boolean isReadable(final String path) throws FalsePositiveException;

    /**
     * Returns {@code true} if and only if the file system has been touched,
     * i.e. if an operation changed its state.
     */
    boolean isTouched();

    boolean isWritable(final String path) throws FalsePositiveException;

    Set<String> list(final String path) throws FalsePositiveException;

    boolean mkdir(final String path, final boolean createParents) throws FalsePositiveException;

    /**
     * A factory method returning an input stream which is positioned
     * at the beginning of the given entry in the target archive file.
     *
     * @param path An entry in the virtual archive file system
     *        - {@code null} or {@code ""} is not permitted.
     * @return A valid {@code InputStream} object
     *         - {@code null} is never returned.
     */
    InputStream newInputStream(final String path) throws FalsePositiveException, IOException;

    // TODO: Remove this!
    InputStream newInputStream0(final String path) throws FalsePositiveException, IOException;

    /**
     * A factory method returning an {@code OutputStream} allowing to
     * (re)write the given entry in the target archive file.
     *
     * @param path An entry in the virtual archive file system
     *        - {@code null} or {@code ""} is not permitted.
     * @return A valid {@code OutputStream} object
     *         - {@code null} is never returned.
     */
    OutputStream newOutputStream(final String path, final boolean append, final boolean createParents) throws FalsePositiveException, IOException;

    ReentrantLock readLock();

    boolean setLastModified(final String path, final long time) throws FalsePositiveException;

    void setReadOnly(final String path) throws FalsePositiveException, IOException;

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
    void sync(BitField<ArchiveSyncOption> options, ArchiveSyncExceptionBuilder builder) throws ArchiveSyncException;

    @Override
    String toString();

    ReentrantLock writeLock();

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
     * @param operation The {@link Operation} to run while the write lock is
     *        acquired.
     */
    <E extends Exception> void runWriteLocked(Operation<E> operation)
    throws E;

    ArchiveFileSystem autoMount(boolean autoCreate)
    throws FalsePositiveException, IOException;

    /**
     * Tests if the file system entry with the given path name has received or
     * is currently receiving new data via an output stream.
     * As an implication, the entry cannot receive new data from another
     * output stream before the next call to {@link #sync}.
     * Note that for directories this method will always return
     * {@code false}!
     */
    boolean hasNewData(String path);

    /**
     * <b>Important:</b>
     * <ul>
     * <li>This controller's read <em>or</em> write lock must be acquired.
     * <li>{@code entry} must not have received
     *     {@link #hasNewData new data}.
     * <ul>
     */
    ArchiveInputStreamSocket<?> getInputStreamSocket(ArchiveEntry target)
    throws IOException;

    /**
     * <b>Important:</b>
     * <ul>
     * <li>This controller's <em>write</em> lock must be acquired.
     * <li>{@code entry} must not have received
     *     {@link #hasNewData new data}.
     * <ul>
     */
    abstract ArchiveOutputStreamSocket<?> getOutputStreamSocket(ArchiveEntry target)
    throws IOException;
}
