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

import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.ArchiveDescriptor;
import de.schlichtherle.truezip.io.socket.common.input.CommonInputSocket;
import de.schlichtherle.truezip.io.socket.common.output.CommonOutputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
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

    public abstract Icon getOpenIcon()
    throws FalsePositiveException;

    public abstract Icon getClosedIcon()
    throws FalsePositiveException;

    public abstract boolean isReadOnly()
    throws FalsePositiveException;

    public abstract boolean isReadable(String path)
    throws FalsePositiveException;

    public abstract boolean isWritable(String path)
    throws FalsePositiveException;

    public abstract ArchiveEntry getEntry(String path)
    throws FalsePositiveException;

    public abstract Set<String> list(String path)
    throws FalsePositiveException;

    /**
     * Returns an archive input socket for reading the given entry from the
     * target archive file.
     *
     * @param  path a non-{@code null} entry in the virtual archive file
     *         system.
     * @return A non-{@code null} {@code CommonInputSocket}.
     */
    // TODO: Consider variant without options in order to implement InputSocketProvider<String> interface
    public abstract CommonInputSocket
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
    // TODO: Consider variant without options in order to implement InputSocketProvider<String> interface
    public abstract CommonOutputSocket
    getOutputSocket(BitField<ArchiveIOOption> options, String path)
    throws IOException;

    public abstract boolean createNewFile(String path, boolean createParents)
    throws IOException;

    public abstract boolean mkdir(String path, boolean createParents)
    throws FalsePositiveException;

    public abstract boolean delete(String path)
    throws FalsePositiveException;

    public abstract boolean setLastModified(String path, long time)
    throws FalsePositiveException;

    public abstract boolean setReadOnly(String path)
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
    public abstract void sync(BitField<ArchiveSyncOption> options, ArchiveSyncExceptionBuilder builder)
    throws ArchiveSyncException;
}
