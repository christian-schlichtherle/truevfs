/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access;

import net.java.truecommons.cio.Entry;
import net.java.truecommons.cio.InputSocket;
import net.java.truecommons.cio.IoSockets;
import net.java.truecommons.cio.OutputSocket;
import net.java.truecommons.shed.BitField;
import net.java.truecommons.shed.Paths;
import net.java.truevfs.kernel.spec.FsAccessOption;
import net.java.truevfs.kernel.spec.FsNodePath;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.util.Arrays;
import java.util.Optional;

import static net.java.truevfs.kernel.spec.FsAccessOption.CREATE_PARENTS;

/**
 * Provides fast bulk I/O operations for {@link File}s and {@link TFile}s.
 * <p>
 * Note that in contrast to the {@link TFile} class, the methods in this
 * class accept plain old {@link File} objects.
 * However, full advantage is taken if a parameter is a {@link TFile} object.
 *
 * @author Christian Schlichtherle
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
final class TBIO {

    /**
     * Moves the source file or directory tree to the destination file or
     * directory tree by performing a recursive cp-then-delete operation.
     * Hence, this file system operation works even with archive files or
     * entries within archive files, but is <em>not</em> atomic.
     *
     * @param  src the source directory tree or file.
     *         This file system entity needs to exist.
     * @param  dst the destination directory tree or file.
     *         This file systeme entity may or may not exist.
     *         If it does, its contents are overwritten.
     * @param  detector the object used to detect any archive files in the
     *         source and destination paths.
     * @throws IOException if the source path contains the destination path
     *         or an elementary operation fails for any reason.
     */
    static void
    mv(final File src, final File dst, final TArchiveDetector detector)
    throws IOException {
        checkContains(src, dst);
        if (dst.exists())
            throw new FileAlreadyExistsException(src.getPath(), dst.getPath(), null);
        mv0(src, dst, detector);
    }

    /** Unchecked parameters version. */
    private static void
    mv0(final File src, final File dst, final TArchiveDetector detector)
    throws IOException {
        if (src.isDirectory()) {
            final long srcLastModified = src.lastModified();
            final boolean srcIsArchived = src instanceof TFile
                    && null != ((TFile) src).getInnerArchive();
            final boolean dstIsArchived = dst instanceof TFile
                    && null != ((TFile) dst).getInnerArchive();
            final boolean srcIsGhost = srcIsArchived
                    && 0 >= srcLastModified;
            if (!srcIsGhost || !dstIsArchived || !TConfig.current().isLenient())
                if (!dst.mkdir() && !dst.isDirectory())
                    throw new NotDirectoryException(dst.getPath());
            final String[] members = src.list();
            if (null == members)
                throw new FileSystemException(dst.getPath(), null, "Cannot list directory!");
            if (!srcIsArchived && dstIsArchived) {
                // Create sorted entries if writing a new archive file.
                // This is courtesy only, so natural order is sufficient.
                Arrays.sort(members);
            }
            for (final String member : members)
                mv0(    new TFile(src, member, detector),
                        new TFile(dst, member, detector),
                        detector);
            if (!srcIsGhost)
                if (!dst.setLastModified(srcLastModified))
                    throw new FileSystemException(dst.getPath(), null, "Cannot set last modification time!");
        } else if (src.isFile()) {
            if (dst.exists() && !dst.isFile())
                throw new FileSystemException(dst.getPath(), null, "Not a file!");
            cp0(true, src, dst);
        } else if (src.exists()) {
            throw new FileSystemException(src.getPath(), null, "Cannot move special file!");
        } else {
            throw new NoSuchFileException(src.getPath());
        }
        if (!src.delete())
            throw new FileSystemException(src.getPath(), null, "Cannot delete!");
    }

    /**
     * Recursively copies the source directory tree or file to the destination
     * directory tree or file.
     *
     * @param preserve If {@code true}, then a best effort approach is used to
     *        copy as much properties of any source files to the destination
     *        files as possible.
     *        Which attributes are actually copied is specific to the
     *        destination file system driver implementation, but the minimum
     *        guarantee is to copy the last modification time.
     * @param  src the source directory tree or file.
     *         This file system entity needs to exist.
     * @param  dst the destination directory tree or file.
     *         This file systeme entity may or may not exist.
     *         If it does, its contents are overwritten.
     * @param  srcDetector the object used to detect any archive files in the
     *         source path.
     * @param  dstDetector the object used to detect any archive files in the
     *         destination path.
     * @throws IOException if the source path contains the destination path
     *         or any I/O error.
     */
    static void
    cp_r(   final boolean preserve,
            final File src,
            final File dst,
            final TArchiveDetector srcDetector,
            final TArchiveDetector dstDetector)
    throws IOException {
        checkContains(src, dst);
        cp_r0(preserve, src, dst, srcDetector, dstDetector);
    }

    /** Unchecked parameters version. */
    private static void
    cp_r0(  final boolean preserve,
            final File src,
            final File dst,
            final TArchiveDetector srcDetector,
            final TArchiveDetector dstDetector)
    throws IOException {
        if (src.isDirectory()) {
            final long srcLastModified = src.lastModified();
            final boolean srcArchived = src instanceof TFile
                    && null != ((TFile) src).getInnerArchive();
            final boolean dstArchived = dst instanceof TFile
                    && null != ((TFile) dst).getInnerArchive();
            final boolean srcIsGhost = srcArchived && 0 >= srcLastModified;
            if (!srcIsGhost || !dstArchived || !TConfig.current().isLenient())
                if (!dst.mkdir() && !dst.isDirectory())
                    throw new NotDirectoryException(dst.getPath());
            final String[] members = src.list();
            if (null == members)
                throw new FileSystemException(dst.getPath(), null, "Cannot list directory!");
            if (!srcArchived && dstArchived) {
                // Create sorted entries if copying an ordinary directory to a
                // new archive.
                // This is a courtesy only, so natural order is sufficient.
                Arrays.sort(members);
            }
            for (final String member : members)
                cp_r0(  preserve,
                        new TFile(src, member, srcDetector),
                        new TFile(dst, member, dstDetector),
                        srcDetector, dstDetector);
            if (preserve && !srcIsGhost)
                if (!dst.setLastModified(srcLastModified))
                    throw new FileSystemException(dst.getPath(), null, "Cannot set last modification time!");
        } else if (src.isFile()) {
            if (dst.exists() && !dst.isFile())
                throw new FileSystemException(dst.getPath(), null, "Not a file!");
            cp0(preserve, src, dst);
        } else if (src.exists()) {
            throw new FileSystemException(src.getPath(), null, "Cannot copy special file!");
        } else {
            throw new NoSuchFileException(src.getPath());
        }
    }

    /**
     * Copies a single source file to a destination file.
     *
     * @param  preserve if an elementary cp operation shall cp as much
     *         properties of the source file to the destination file, too.
     *         Currently, only the last modification time is preserved.
     *         Note that this property set may get extended over time.
     * @param  src the source file.
     *         This file system entity needs to exist.
     * @param  dst the destination file.
     *         This file systeme entity may or may not exist.
     *         If it does, its contents are overwritten.
     * @throws IOException if the source path contains the destination path
     *         or an elementary operation fails for any reason.
     */
    static void
    cp(final boolean preserve, final File src, final File dst)
    throws IOException {
        checkContains(src, dst);
        cp0(preserve, src, dst);
    }

    /** Unchecked parameters version. */
    private static void
    cp0(final boolean preserve, final File src, final File dst)
    throws IOException {
        final BitField<FsAccessOption> preferences =
                TConfig.current().getAccessPreferences();
        final InputSocket<?> input = input(preferences, src);
        final OutputSocket<?> output = output(preferences, dst,
                preserve ? Optional.of(input.target()) : Optional.empty());
        IoSockets.copy(input, output);
    }

    /**
     * Recursively deletes the given file or directory tree.
     *
     * @param  file the file or directory tree to delete recursively.
     * @throws IOException if an elementary operation fails for any reason.
     */
    static void rm_r(final File file, final TArchiveDetector detector)
    throws IOException {
        if (file.isDirectory()) {
            final String[] members = file.list();
            if (null == members)
                throw new FileSystemException(file.getPath(), null, "Cannot list directory!");
            for (final String member : members)
                rm_r(new TFile(file, member, detector), detector);
        }
        TFile.rm(file);
    }

    /**
     * Throws an {@code IOException} if and only if the path represented by
     * {@code a} contains the path represented by {@code b}, where a path is
     * said to contain another path if and only if it is equal or an ancestor
     * of the other path.
     * <p>
     * Note that this method uses the absolute path of both files as if by
     * calling {@link File#getAbsolutePath()}.
     *
     * @param a a file.
     * @param b another file.
     */
    private static void checkContains(File a, File b) throws IOException {
        if (Paths.contains( a.getAbsolutePath(),
                            b.getAbsolutePath(),
                            File.separatorChar))
            throw new FileSystemException(a.getPath(), b.getPath(), "First path contains second path!");
    }

    private static TArchiveDetector detector(File file) {
        return file instanceof TFile
                ? ((TFile) file).getArchiveDetector()
                : TConfig.current().getArchiveDetector();
    }

    /**
     * Returns an input socket for the given file.
     * 
     * @param  file the file to read.
     * @param  options the options for accessing the file.
     * @return An input socket for the given file.
     */
    static InputSocket<?> input(
            final BitField<FsAccessOption> options,
            final File file) {
        if (file instanceof TFile) {
            final TFile tfile=  (TFile) file;
            final TFile archive = tfile.getInnerArchive();
            if (null != archive)
                return archive
                        .getController()
                        .input(options, tfile.getNodeName());
        }
        final FsNodePath path = new FsNodePath(file);
        return  TConfig
                .current()
                .getManager()
                .controller(detector(file), path.getMountPoint().get())
                .input(options, path.getNodeName());
    }

    /**
     * Returns an output socket for the given file.
     * 
     * @param  file the file to write.
     * @param  options the options for accessing the file.
     * @param  template a nullable template from which file attributes shall
     *         get copied.
     * @return An output socket for the given file.
     */
    static OutputSocket<?> output(
            final BitField<FsAccessOption> options,
            final File file,
            final Optional<? extends Entry> template) {
        if (file instanceof TFile) {
            final TFile tfile = (TFile) file;
            final TFile archive = tfile.getInnerArchive();
            if (null != archive)
                return archive
                        .getController()
                        .output(    options,
                                    tfile.getNodeName(),
                                    template);
        }
        final FsNodePath path = new FsNodePath(file);
        return TConfig
                .current()
                .getManager()
                .controller(detector(file), path.getMountPoint().get())
                .output(    options.clear(CREATE_PARENTS),
                            path.getNodeName(),
                            template);
    }

    private TBIO() { }
}
