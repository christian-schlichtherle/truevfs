/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.nio.fsp;

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.entry.Entry;
import static de.schlichtherle.truezip.entry.Entry.Type.*;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsEntry;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsInputOption;
import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.FsMountPoint;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import static de.schlichtherle.truezip.fs.FsOutputOptions.*;
import de.schlichtherle.truezip.fs.FsSyncException;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import de.schlichtherle.truezip.fs.FsFilteringManager;
import static de.schlichtherle.truezip.fs.FsManager.*;
import de.schlichtherle.truezip.fs.FsSyncOption;
import de.schlichtherle.truezip.fs.FsSyncWarningException;
import de.schlichtherle.truezip.fs.sl.FsManagerLocator;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A {@link FileSystem} implementation based on the TrueZIP Kernel module.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class TFileSystem extends FileSystem {

    /** The file system manager to use within this package. */
    private static final FsManager manager = FsManagerLocator.SINGLETON.get();

    private final FsMountPoint mountPoint;
    private final FsController<?> controller;
    private final TFileSystemProvider provider;

    static TFileSystem get(final TPath path) {
        return new TFileSystem(path);
    }

    private TFileSystem(final TPath path) {
        assert null != path;
        this.mountPoint = path.getAddress().getMountPoint();
        this.controller = manager.getController(
                path.getAddress().getMountPoint(),
                path.getArchiveDetector());
        this.provider = TFileSystemProvider.get(path);

        assert invariants();
    }

    private boolean invariants() {
        assert null != getController();
        assert null != provider();
        return true;
    }

    /**
     * Returns the value of the class property {@code lenient}.
     * By default, the value of this class property is {@code true}.
     *
     * @see #setLenient(boolean)
     * @return The value of the class property {@code lenient}.
     */
    public static boolean isLenient() {
        return TFile.isLenient();
    }

    /**
     * Sets the value of the class property {@code lenient}.
     * This class property controls whether archive files and their member
     * directories get automatically created whenever required.
     * By default, the value of this class property is {@code true}!
     * <p>
     * Consider the following path: {@code a/outer.zip/b/inner.zip/c}.
     * Now let's assume that {@code a} exists as a plain directory in the
     * platform file system, while all other segments of this path don't, and
     * that the module TrueZIP Driver ZIP is present on the run-time class path
     * in order to detect {@code outer.zip} and {@code inner.zip} as ZIP files
     * according to the initial setup.
     * <p>
     * Now, if this class property is set to {@code false}, then an application
     * needs to call {@code new TFile("a/outer.zip/b/inner.zip").mkdirs()}
     * before it can actually create the innermost {@code c} entry as a file
     * or directory.
     * <p>
     * More formally, before an application can access an entry in a federated
     * file system, all its parent directories need to exist, including archive
     * files.
     * This emulates the behaviour of the platform file system.
     * <p>
     * If this class property is set to {@code true} however, then any missing
     * parent directories (including archive files) up to the outermost archive
     * file {@code outer.zip} get automatically created when using operations
     * to create the innermost element of the path {@code c}.
     * <p>
     * This allows applications to succeed with doing this:
     * {@code new TFile("a/outer.zip/b/inner.zip/c").createNewFile()},
     * or that:
     * {@code new TFileOutputStream("a/outer.zip/b/inner.zip/c")}.
     * <p>
     * Note that in either case the parent directory of the outermost archive
     * file {@code a} must exist - TrueZIP does not automatically create
     * directories in the platform file system!
     *
     * @param lenient the value of the class property {@code lenient}.
     * @see   #isLenient()
     */
    public static void setLenient(boolean lenient) {
        TFile.setLenient(lenient);
    }

    FsController<?> getController() {
        return controller;
    }

    FsMountPoint getMountPoint() {
        return getController().getModel().getMountPoint();
    }

    @Override
    public TFileSystemProvider provider() {
        return provider;
    }

    /**
     * Commits all unsynchronized changes to the contents of this federated
     * file system (i.e. prospective archive file)
     * and all its member federated file systems to their respective parent
     * file system, releases the associated resources (i.e. target archive
     * files) for access by third parties (e.g. other processes), cleans up any
     * temporary allocated resources (e.g. temporary files) and purges any
     * cached data.
     * Note that temporary files may get used even if the archive files where
     * accessed read-only.
     * <p>
     * If a client application needs to sync an individual archive file,
     * the following idiom could be used:
     * <pre>{@code
     * if (file.isArchive() && file.getEnclArchive() == null) // filter top level federated file system
     *   if (file.isDirectory()) // ignore false positives
     *     TFile.sync(file); // sync federated file system and all its members
     * }</pre>
     * Again, this will also sync all federated file systems which are
     * located within the file system referred to by {@code file}.
     *
     * @param  options a bit field of synchronization options.
     * @throws IllegalArgumentException if {@code archive} is not a top level
     *         federated file system or the combination of synchronization
     *         options is illegal, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_INPUT} is cleared and
     *         {@code FsSyncOption.FORCE_CLOSE_OUTPUT} is set or if the
     *         synchronization option {@code FsSyncOption.ABORT_CHANGES} is set.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         occur.
     *         This implies that the respective parent file system has been
     *         updated with constraints, such as a failure to set the last
     *         modification time of the entry for the federated file system
     *         (i.e. archive file) in its parent file system.
     * @throws FsSyncException if any error conditions occur.
     *         This implies loss of data!
     * @see    #sync(BitField)
     */
    public void sync(BitField<FsSyncOption> options) throws FsSyncException {
        new FsFilteringManager( manager,
                                getController().getModel().getMountPoint())
                .sync(options);
    }

    /**
     * Commits all unsynchronized changes to the contents of this federated
     * file system (i.e. prospective archive files)
     * and all its member federated file systems to their respective parent
     * system, releases the associated resources (i.e. target archive files)
     * for access by third parties (e.g. other processes), cleans up any
     * temporary allocated resources (e.g. temporary files) and purges any
     * cached data.
     * Note that temporary files may get used even if the archive files where
     * accessed read-only.
     * <p>
     * This method is equivalent to calling
     * {@link #sync(BitField) sync(FsSyncOptions.UMOUNT)}.
     * <p>
     * The file system stays open after this call and can be subsequently used.
     *
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         occur.
     *         This implies that the respective parent file system has been
     *         updated with constraints, such as a failure to set the last
     *         modification time of the entry for the federated file system
     *         (i.e. prospective archive file) in its parent file system.
     * @throws FsSyncException if any error conditions occur.
     *         This implies loss of data!
     * @see    #sync(BitField)
     */
    @Override
    public void close() throws FsSyncException {
        sync(UMOUNT);
    }

    /**
     * Returns {@code true}.
     * 
     * @return {@code true}.
     */
    @Override
    public boolean isOpen() {
        return true;
    }

    /**
     * Returns {@code false}.
     * 
     * @return {@code false}.
     */
    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public String getSeparator() {
        return SEPARATOR;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.singleton((Path)
                new TPath(mountPoint.toHierarchicalUri().resolve(SEPARATOR)));
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return Collections.singleton("basic");
    }

    @Override
    public TPath getPath(String first, String... more) {
        return new TPath(getMountPoint(), first, more);
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    FsEntry getEntry(TPath path) throws IOException {
        FsEntryName name = path.getAddress().getEntryName();
        return getController().getEntry(name);
    }

    InputSocket<?> getInputSocket(  TPath path,
                                    BitField<FsInputOption> options) {
        FsEntryName name = path.getAddress().getEntryName();
        return getController().getInputSocket(name, options);
    }

    OutputSocket<?> getOutputSocket(TPath path,
                                    BitField<FsOutputOption> options,
                                    @CheckForNull Entry template) {
        FsEntryName name = path.getAddress().getEntryName();
        return getController().getOutputSocket(name, options, template);
    }

    DirectoryStream<Path> newDirectoryStream(   final TPath path,
                                                final Filter<? super Path> filter)
    throws IOException {
        final FsEntryName name = path.getAddress().getEntryName();
        final FsEntry entry = getController().getEntry(name);
        final Set<String> set;
        if (null == entry || null == (set = entry.getMembers()))
            throw new NotDirectoryException(path.toString());

        class Adapter implements Iterator<Path> {
            final Iterator<String> i = set.iterator();
            Path next;

            @Override
            public boolean hasNext() {
                while (i.hasNext()) {
                    next = path.resolve(i.next());
                    try {
                        if (filter.accept(next))
                            return true;
                    } catch (IOException ex) {
                        throw new DirectoryIteratorException(ex);
                    }
                }
                next = null;
                return false;
            }

            @Override
            public Path next() {
                if (null == next)
                    throw new NoSuchElementException();
                return next;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }

        class Stream implements DirectoryStream<Path> {
            @Override
            public Iterator<Path> iterator() {
                return new Adapter();
            }

            @Override
            public void close() {
            }
        }

        return new Stream();
    }

    void createDirectory(TPath path, FileAttribute<?>... attrs)
    throws IOException {
        if (0 < attrs.length)
            throw new UnsupportedOperationException();
        FsEntryName name = path.getAddress().getEntryName();
        getController().mknod(
                name,
                DIRECTORY,
                NO_OUTPUT_OPTION.set(CREATE_PARENTS, isLenient()),
                null);
    }

    void delete(TPath path) throws IOException {
        FsEntryName name = path.getAddress().getEntryName();
        getController().unlink(name);
    }
}
