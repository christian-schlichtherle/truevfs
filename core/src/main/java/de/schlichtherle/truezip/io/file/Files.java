/*
 * Copyright (C) 2007-2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.file;

import de.schlichtherle.truezip.io.filesystem.FileSystemManagers;
import de.schlichtherle.truezip.io.filesystem.Path;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystemException;
import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.filesystem.file.FileDriver;
import de.schlichtherle.truezip.io.filesystem.SyncException;
import de.schlichtherle.truezip.io.InputBusyException;
import de.schlichtherle.truezip.io.OutputBusyException;
import de.schlichtherle.truezip.io.filesystem.InputOption;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.IOSocket;
import de.schlichtherle.truezip.io.filesystem.OutputOption;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import static de.schlichtherle.truezip.io.filesystem.OutputOption.CREATE_PARENTS;
import static de.schlichtherle.truezip.io.Files.contains;

/**
 * Provides static utility methods for {@link File}s.
 * This class cannot get instantiated outside its package.
 * <p>
 * Note that in contrast to the {@link File} class, the methods in this
 * class accept and return plain {@code java.io.File} instances.
 * Full advantage is taken if a parameter is actually an instance of the
 * {@code File} class in this package, however.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
class Files {

    Files() {
    }

    /**
     * Moves the source to the destination by recursively copying and deleting
     * its files and directories.
     * Hence, this file system operation works even with archive files or
     * entries within archive files, but is <em>not</em> atomic.
     *
     * @param src The source file or directory.
     *            This must exist.
     * @param dst The destination file or directory.
     *            This may or may not exist.
     *            If it does, its contents are overwritten.
     * @param detector The object used to detect any archive
     *        files in the path and configure their parameters.
     * @return Whether the operation succeeded or not.
     *         If it fails, the source and destination may contain only a
     *         subset of the source before this operation.
     *         However, each file has either been completely moved or not.
     * @see File#renameTo(java.io.File, ArchiveDetector)
     * @see <a href="package-summary.html#third_parties">Third Party
     *      Access using different Archive Detectors</a>
     */
    static boolean move(
            final java.io.File src,
            final java.io.File dst,
            final ArchiveDetector detector) {
        return !contains(src, dst) && move0(src, dst, detector);
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    private static boolean move0(
            final java.io.File src,
            final java.io.File dst,
            final ArchiveDetector detector) {
        boolean ok = true;
        if (src.isDirectory()) {
            final long srcLastModified = src.lastModified();
            final boolean srcIsArchived = src instanceof File
                    && ((File) src).getInnerArchive() != null;
            final boolean dstIsArchived = dst instanceof File
                    && ((File) dst).getInnerArchive() != null;
            final boolean srcIsGhost = srcIsArchived
                    && srcLastModified <= 0;
            if (!srcIsGhost || !dstIsArchived || !File.isLenient())
                dst.mkdir();
            final String[] members = src.list();
            if (!srcIsArchived && dstIsArchived) {
                // Create sorted entries if writing a new archive file.
                // This is courtesy only, so natural order is sufficient.
                Arrays.sort(members);
            }
            for (int i = 0, l = members.length; i < l; i++) {
                final String member = members[i];
                ok &= move0(  detector.newFile(src, member),
                            detector.newFile(dst,  member),
                            detector);
            }
            if (!srcIsGhost)
                ok &= dst.setLastModified(srcLastModified);
        } else if (src.isFile()) { // !isDirectory()
            try {
                copy(true, src, dst);
            } catch (IOException ex) {
                ok = false;
            }
        } else {
            ok = false; // don't move special files!
        }
        return ok && src.delete(); // only unlink if ok!
    }

    /**
     * Performs a recursive copy operation.
     *
     * @see File#copyAllTo(java.io.File, ArchiveDetector, ArchiveDetector)
     * @see File#archiveCopyAllTo(java.io.File, ArchiveDetector, ArchiveDetector)
     * @see <a href="package-summary.html#third_parties">Third Party
     *      Access using different Archive Detectors</a>
     */
    static void copyAll(
            final boolean preserve,
            final java.io.File src,
            final java.io.File dst,
            final ArchiveDetector srcDetector,
            final ArchiveDetector dstDetector)
    throws IOException {
        if (contains(src, dst))
            throw new ContainsFileException(src, dst);
        copyAll0(preserve, src, dst, srcDetector, dstDetector);
    }

    /**
     * Unchecked parameters version.
     */
    private static void copyAll0(
            final boolean preserve,
            final java.io.File src,
            final java.io.File dst,
            final ArchiveDetector srcDetector,
            final ArchiveDetector dstDetector)
    throws IOException {
        if (src.isDirectory()) {
            final long srcLastModified = src.lastModified();
            final boolean srcIsArchived = src instanceof File
                    && ((File) src).getInnerArchive() != null;
            final boolean dstIsArchived = dst instanceof File
                    && ((File) dst).getInnerArchive() != null;
            final boolean srcIsGhost = srcIsArchived
                    && srcLastModified <= 0;
            if (!srcIsGhost || !dstIsArchived || !File.isLenient())
                if (!dst.mkdir() && !dst.isDirectory())
                    throw new IOException("destination is not a directory");
            final String[] members = src.list();
            if (!srcIsArchived && dstIsArchived) {
                // Create sorted entries if writing a new archive.
                // This is a courtesy only, so natural order is sufficient.
                Arrays.sort(members);
            }
            for (int i = 0, l = members.length; i < l; i++) {
                final String member = members[i];
                copyAll0(  preserve,
                        srcDetector.newFile(src, member),
                        dstDetector.newFile(dst, member),
                        srcDetector, dstDetector);
            }
            if (preserve && !srcIsGhost)
                if (!dst.setLastModified(srcLastModified))
                    throw new IOException("cannot set last modification time");
        } else if (src.isFile() && (!dst.exists() || dst.isFile())) {
            copy0(preserve, src, dst);
        } else {
            throw new IOException("cannot copy non-existent or special files");
        }
    }

    /**
     * The name of this method is inspired by the Unix command line utility
     * {@code copy}.
     *
     * @see File#cp(java.io.File, java.io.File)
     * @see File#cp_p(java.io.File, java.io.File)
     * @see <a href="package-summary.html#third_parties">Third Party
     *      Access using different Archive Detectors</a>
     */
    static void copy(
            final boolean preserve,
            final java.io.File src,
            final java.io.File dst)
    throws IOException {
        if (contains(src, dst))
            throw new ContainsFileException(src, dst); // TODO: Required anymore?
        copy0(preserve, src, dst);
    }

    /** Unchecked parameters version. */
    private static void copy0(
            final boolean preserve,
            final java.io.File src,
            final java.io.File dst)
    throws IOException {
        final InputSocket<?> input = getInputSocket(src,
                BitField.noneOf(InputOption.class));
        final OutputSocket<?> output = getOutputSocket(dst,
                BitField.noneOf(OutputOption.class)
                    .set(CREATE_PARENTS, File.isLenient()),
                preserve ? input.getLocalTarget() : null);
        try {
            IOSocket.copy(input, output);
        } catch (FileNotFoundException ex) {
            throw ex;
        } catch (SyncException ex) {
            if (ex.getCause() instanceof InputBusyException)
                throw (InputBusyException) ex.getCause();
            else if (ex.getCause() instanceof OutputBusyException)
                throw (OutputBusyException) ex.getCause();
            else
                throw (FileNotFoundException) new FileNotFoundException(ex.toString())
                        .initCause(ex);
        } catch (ArchiveFileSystemException ex) {
            throw (FileNotFoundException) new FileNotFoundException(ex.toString())
                    .initCause(ex);
        }
    }

    static InputSocket<?> getInputSocket(
            final java.io.File src,
            final BitField<InputOption> options) {
        if (src instanceof File) {
            // TODO: Get rid of this and use the more general pattern below!
            final File file = (File) src;
            final File archive = file.getInnerArchive();
            if (null != archive)
                return archive.getController()
                        .getInputSocket(file.getInnerEntryName0(), options);
        }
        // TODO: Replace new FileDriver() with an adapter for an ArchiveDetector!
        final Path path = Path.create(src.toURI(), true);
        return FileSystemManagers
                .getInstance()
                .getController( path.getMountPoint(), new FileDriver())
                .getInputSocket(path.getEntryName(), options);
    }

    static OutputSocket<?> getOutputSocket(
            final java.io.File dst,
            final BitField<OutputOption> options,
            final Entry template) {
        if (dst instanceof File) {
            // TODO: Get rid of this and use the more general pattern below!
            final File file = (File) dst;
            final File archive = file.getInnerArchive();
            if (null != archive)
                return archive.getController()
                        .getOutputSocket(file.getInnerEntryName0(), options, template);
        }
        // TODO: Replace new FileDriver() with an adapter for an ArchiveDetector!
        final Path path = Path.create(dst.toURI(), true);
        return FileSystemManagers
                .getInstance()
                .getController(  path.getMountPoint(), new FileDriver())
                .getOutputSocket(path.getEntryName(), options, template);
    }

    /**
     * Deletes the entire directory tree represented by the parameter,
     * regardless whether it's a file or directory, whether the directory
     * is empty.
     * <p>
     * This file system operation is <em>not</em> atomic.
     *
     * @return Whether or not the entire directory tree was successfully
     *         removed.
     */
    static boolean deleteAll(final java.io.File file) {
        boolean ok = true;
        if (file.isDirectory()) {
            // If the directory is an archive file, one may be tempted to delete it
            // directly (using e.g. java.io.File.delete()).
            // However, this would bypass the ArchiveController's state and cause
            // subsequent mayhem.
            // So we play it safe despite the fact that this procedure is comparably
            // much slower.
            java.io.File[] members = file.listFiles();
            for (int i = members.length; --i >= 0;)
                ok &= deleteAll(members[i]);
        }
        return ok && file.delete();
    }
}
