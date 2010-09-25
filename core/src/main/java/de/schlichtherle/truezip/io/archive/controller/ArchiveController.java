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

import de.schlichtherle.truezip.io.socket.common.entry.CommonEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystemEntry;
import de.schlichtherle.truezip.io.archive.ArchiveDescriptor;
import de.schlichtherle.truezip.io.socket.common.input.CommonInputSocket;
import de.schlichtherle.truezip.io.socket.common.output.CommonOutputSocket;
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
public interface ArchiveController extends ArchiveDescriptor {

    /**
     * {@inheritDoc}
     * <p>
     * Where the methods of this class accept a path name string as a
     * parameter, this must be a relative, hierarchical URI which is resolved
     * against this mount point.
     */
    @Override
    URI getMountPoint();

    ArchiveController getEnclArchive();

    /**
     * Resolves the given relative {@code path} against the relative path of
     * the target archive file within its enclosing archive file.
     *
     * @throws NullPointerException if the target archive file is not enclosed
     *         within another archive file.
     */
    String getEnclPath(final String path);

    Icon getOpenIcon() throws FalsePositiveException;

    Icon getClosedIcon() throws FalsePositiveException;

    boolean isReadOnly() throws FalsePositiveException;

    boolean isReadable(String path) throws FalsePositiveException;

    boolean isWritable(String path) throws FalsePositiveException;

    ArchiveFileSystemEntry getEntry(String path) throws FalsePositiveException;

    /**
     * Returns an archive input socket for reading the given entry from the
     * target archive file.
     *
     * @param  path a non-{@code null} entry in the virtual archive file
     *         system.
     * @return A non-{@code null} {@code CommonInputSocket}.
     */
    CommonInputSocket<? extends CommonEntry>
    getInputSocket(BitField<ArchiveIOOption> options, String path)
    throws IOException;

    /**
     * Returns an archive output socket for writing the given entry to the
     * target archive file.
     *
     * @param  path a non-{@code null} entry in the virtual archive file
     *         system.
     * @return A non-{@code null} {@code CommonInputSocket}.
     */
    CommonOutputSocket<? extends CommonEntry>
    getOutputSocket(BitField<ArchiveIOOption> options, String path)
    throws IOException;

    boolean createNewFile(String path, boolean createParents)
    throws IOException;

    boolean mkdir(String path, boolean createParents)
    throws FalsePositiveException;

    boolean delete(String path)
    throws FalsePositiveException;

    boolean setLastModified(String path, long time)
    throws FalsePositiveException;

    boolean setReadOnly(String path)
    throws FalsePositiveException;

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
    void sync(BitField<ArchiveSyncOption> options, ArchiveSyncExceptionBuilder builder)
    throws ArchiveSyncException;
}
