/*
 * Copyright (C) 2007-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.file;

import java.io.File;
import de.schlichtherle.truezip.io.Paths;
import de.schlichtherle.truezip.fs.FsPath;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.FsInputOption;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.IOSocket;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.Arrays;
import net.jcip.annotations.Immutable;

import static de.schlichtherle.truezip.fs.FsOutputOption.*;

/**
 * Provides static utility methods for {@link TFile}s.
 * <p>
 * Note that in contrast to the {@link TFile} class, the methods in this
 * class accept and return plain {@code java.io.TFile} instances.
 * Full advantage is taken if a parameter is actually an instance of the
 * {@code TFile} class in this package, however.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
@Immutable
class TFiles {

    private TFiles() {
    }

    /**
     * Moves the source to the destination by recursively copying and deleting
     * its files and directories.
     * Hence, this file system operation works even with archive files or
     * entries within archive files, but is <em>not</em> atomic.
     *
     * @param  src the source file or directory.
     *             This must exist.
     * @param  dst the destination file or directory.
     *             This may or may not exist.
     *             If it does, its contents are overwritten.
     * @param  detector the object used to detect any archive
     *         files in the path and configure their parameters.
     * @return Whether the operation succeeded or not.
     *         If it fails, the source and destination may contain only a
     *         subset of the source before this operation.
     *         However, each file has either been completely moved or not.
     * @see    TFile#renameTo(File, TArchiveDetector)
     */
    static boolean
    move(   final File src,
            final File dst,
            final TArchiveDetector detector) {
        return !contains(src, dst) && move0(src, dst, detector);
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    private static boolean
    move0(  final File src,
            final File dst,
            final TArchiveDetector detector) {
        boolean ok = true;
        if (src.isDirectory()) {
            final long srcLastModified = src.lastModified();
            final boolean srcIsArchived = src instanceof TFile
                    && ((TFile) src).getInnerArchive() != null;
            final boolean dstIsArchived = dst instanceof TFile
                    && ((TFile) dst).getInnerArchive() != null;
            final boolean srcIsGhost = srcIsArchived
                    && srcLastModified <= 0;
            if (!srcIsGhost || !dstIsArchived || !TFile.isLenient())
                dst.mkdir();
            final String[] members = src.list();
            if (!srcIsArchived && dstIsArchived) {
                // Create sorted entries if writing a new archive file.
                // This is courtesy only, so natural order is sufficient.
                Arrays.sort(members);
            }
            for (int i = 0, l = members.length; i < l; i++) {
                final String member = members[i];
                ok &= move0(new TFile(src, member, detector),
                            new TFile(dst, member, detector),
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
     * @see TFile#copyAllTo(File, TArchiveDetector, TArchiveDetector)
     * @see TFile#archiveCopyAllTo(File, TArchiveDetector, TArchiveDetector)
     */
    static void
    copyAll(final boolean preserve,
            final File src,
            final File dst,
            final TArchiveDetector srcDetector,
            final TArchiveDetector dstDetector)
    throws IOException {
        if (contains(src, dst))
            throw new TContainsFileException(src, dst);
        copyAll0(preserve, src, dst, srcDetector, dstDetector);
    }

    /** Unchecked parameters version. */
    private static void
    copyAll0(   final boolean preserve,
                final File src,
                final File dst,
                final TArchiveDetector srcDetector,
                final TArchiveDetector dstDetector)
    throws IOException {
        if (src.isDirectory()) {
            final long srcLastModified = src.lastModified();
            final boolean srcIsArchived = src instanceof TFile
                    && ((TFile) src).getInnerArchive() != null;
            final boolean dstIsArchived = dst instanceof TFile
                    && ((TFile) dst).getInnerArchive() != null;
            final boolean srcIsGhost = srcIsArchived
                    && srcLastModified <= 0;
            if (!srcIsGhost || !dstIsArchived || !TFile.isLenient())
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
                copyAll0(   preserve,
                            new TFile(src, member, srcDetector),
                            new TFile(dst, member, dstDetector),
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
     * @see TFile#cp(File, File)
     * @see TFile#cp_p(File, File)
     */
    static void
    copy(   final boolean preserve,
            final File src,
            final File dst)
    throws IOException {
        if (contains(src, dst))
            throw new TContainsFileException(src, dst); // TODO: Required anymore?
        copy0(preserve, src, dst);
    }

    /** Unchecked parameters version. */
    private static void
    copy0(  final boolean preserve,
            final File src,
            final File dst)
    throws IOException {
        final InputSocket<?> input = getInputSocket(src,
                BitField.noneOf(FsInputOption.class));
        final OutputSocket<?> output = getOutputSocket(dst,
                BitField.noneOf(FsOutputOption.class)
                    .set(CREATE_PARENTS, TFile.isLenient()),
                preserve ? input.getLocalTarget() : null);
        IOSocket.copy(input, output);
    }

    static InputSocket<?>
    getInputSocket( final File src,
                    final BitField<FsInputOption> options) {
        if (src instanceof TFile) {
            // TODO: Consider removing this block and using the more general pattern below it!
            // FIXME: Removing this block yields a concurrency issue in the unit tests!
            final TFile file = (TFile) src;
            final TFile archive = file.getInnerArchive();
            if (null != archive)
                return archive.getController()
                        .getInputSocket(file.getInnerEntryName0(), options);
        }
        final FsPath path = new FsPath(src);
        return TFile.manager
                .getController(path.getMountPoint(), TFile.getDefaultArchiveDetector())
                .getInputSocket(path.getEntryName(), options);
    }

    static OutputSocket<?>
    getOutputSocket(final File dst,
                    final BitField<FsOutputOption> options,
                    final @CheckForNull Entry template) {
        if (dst instanceof TFile) {
            // TODO: Consider removing this block and using the more general pattern below it!
            // FIXME: Removing this block yields a concurrency issue in the unit tests!
            final TFile file = (TFile) dst;
            final TFile archive = file.getInnerArchive();
            if (null != archive)
                return archive.getController()
                        .getOutputSocket(file.getInnerEntryName0(), options, template);
        }
        final FsPath path = new FsPath(dst);
        return TFile.manager
                .getController(path.getMountPoint(), TFile.getDefaultArchiveDetector())
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
    static boolean deleteAll(final File file) {
        boolean ok = true;
        if (file.isDirectory()) {
            // If the directory is an archive file, one may be tempted to delete it
            // directly (using e.g. java.io.TFile.delete()).
            // However, this would bypass the ArchiveController's state and cause
            // subsequent mayhem.
            // So we play it safe despite the fact that this procedure is comparably
            // much slower.
            File[] members = file.listFiles();
            for (int i = members.length; --i >= 0;)
                ok &= deleteAll(members[i]);
        }
        return ok && file.delete();
    }

    /**
     * Returns {@code true} if and only if the path represented
     * by {@code a} contains the path represented by {@code b},
     * where a path is said to contain another path if and only
     * if it is equal or a parent of the other path.
     * <p>
     * <b>Note:</b>
     * <ul>
     * <li>This method uses the canonical path name of the given files or,
     *     if failing to canonicalize the path names, the normalized absolute
     *     path names in order to compute reliable results.
     * <li>This method does <em>not</em> access the file system.
     *     It just tests the path names.
     * </ul>
     *
     * @param a a file.
     * @param b another file.
     */
    static boolean contains(File a, File b) {
        return Paths.contains(  a.getAbsolutePath(),
                                b.getAbsolutePath(),
                                TFile.separatorChar);
    }
}
