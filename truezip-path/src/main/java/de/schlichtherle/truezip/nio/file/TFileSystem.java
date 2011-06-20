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
package de.schlichtherle.truezip.nio.file;

import de.schlichtherle.truezip.file.TConfig;
import de.schlichtherle.truezip.entry.Entry;
import static de.schlichtherle.truezip.entry.Entry.*;
import de.schlichtherle.truezip.entry.Entry.Access;
import static de.schlichtherle.truezip.entry.Entry.Access.*;
import static de.schlichtherle.truezip.entry.Entry.Size.*;
import static de.schlichtherle.truezip.entry.Entry.Type.*;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsEntry;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsInputOption;
import static de.schlichtherle.truezip.fs.FsInputOptions.*;
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
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import net.jcip.annotations.Immutable;

/**
 * A {@link FileSystem} implementation based on the TrueZIP Kernel module.
 * <p>
 * Note that {@code TFileSystem} objects are immutable and volatile because
 * all virtual file system state is managed by the TrueZIP Kernel module.
 * As a consequence, you should never use object identity ('==') to test for
 * equality of a {@code TFileSystem} with another object, but instead use the
 * method {@link #equals(Object)}.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public final class TFileSystem extends FileSystem {

    /** The file system manager to use within this package. */
    private static final FsManager manager = FsManagerLocator.SINGLETON.get();

    private final FsController<?> controller;
    private final TFileSystemProvider provider;

    static TFileSystem get(final TPath path) {
        return new TFileSystem(path);
    }

    private TFileSystem(final TPath path) {
        assert null != path;
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

    private FsController<?> getController() {
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
        new FsFilteringManager(manager, getMountPoint()).sync(options);
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
     * Commits all unsynchronized changes to the contents of all federated file
     * systems (i.e. prospective archive files) to their respective parent file
     * system, releases the associated resources (i.e. target archive files)
     * for access by third parties (e.g. other processes), cleans up any
     * temporary allocated resources (e.g. temporary files) and purges any
     * cached data.
     * Note that temporary files may get used even if the archive files where
     * accessed read-only.
     *
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
    public static void umount() throws FsSyncException {
        manager.sync(UMOUNT);
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
        return File.separator;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.singleton((Path)
                new TPath(getMountPoint().toHierarchicalUri().resolve(SEPARATOR)));
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
        return new TPath(this, first, more);
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

    @Override
    public boolean equals(Object that) {
        return this == that
                || that instanceof TFileSystem
                    && this.getMountPoint().equals(
                        ((TFileSystem) that).getMountPoint());
    }

    @Override
    public int hashCode() {
        return getMountPoint().hashCode();
    }

    private static BitField<FsInputOption> mapInput(
            final OpenOption... options) {
        final HashSet<OpenOption> set = new HashSet<>(options.length * 4 / 3 + 1);
        Collections.addAll(set, options);
        return mapInput(set);
    }

    private static BitField<FsInputOption> mapInput(
            final Set<? extends OpenOption> options) {
        final int s = options.size();
        if (0 == s || 1 == s && options.contains(StandardOpenOption.READ))
            return NO_INPUT_OPTION;
        throw new IllegalArgumentException(options.toString());
    }

    private static BitField<FsOutputOption> mapOutput(
            final OpenOption... options) {
        final HashSet<OpenOption> set = new HashSet<>(options.length * 4 / 3 + 1);
        Collections.addAll(set, options);
        return mapOutput(set);
    }

    private static BitField<FsOutputOption> mapOutput(
            final Set<? extends OpenOption> options) {
        final EnumSet<FsOutputOption> set = EnumSet.noneOf(FsOutputOption.class);
        if (TConfig.get().isLenient())
            set.add(CREATE_PARENTS);
        for (final OpenOption option : options) {
            if (!(option instanceof StandardOpenOption))
                throw new UnsupportedOperationException(option.toString());
            switch ((StandardOpenOption) option) {
                case READ:
                    throw new IllegalArgumentException(option.toString());
                case WRITE:
                case TRUNCATE_EXISTING:
                case CREATE:
                    break;
                case APPEND:
                    set.add(APPEND);
                    break;
                case CREATE_NEW:
                    set.add(EXCLUSIVE);
                    break;
                default:
                    throw new UnsupportedOperationException(option.toString());
            }
        }
        return set.isEmpty()
                ? NO_OUTPUT_OPTION
                : BitField.copyOf(set);
    }

    SeekableByteChannel newByteChannel(
            final TPath path,
            final Set<? extends OpenOption> options,
            final FileAttribute<?>... attrs)
    throws IOException {
        final FsEntryName name = path.getAddress().getEntryName();
        final FsController<?> controller = getController();
        if (options.isEmpty() || options.contains(StandardOpenOption.READ)) {
            final BitField<FsInputOption>
                    o = mapInput(options).set(FsInputOption.CACHE);
            return controller
                    .getInputSocket(name, o)
                    .newSeekableByteChannel();
        } else {
            final BitField<FsOutputOption>
                    o = mapOutput(options).set(FsOutputOption.CACHE);
            try {
                return controller
                        .getOutputSocket(name, o, null)
                        .newSeekableByteChannel();
            } catch (IOException ex) {
                if (o.get(EXCLUSIVE) && null != controller.getEntry(name))
                    throw (IOException) new FileAlreadyExistsException(path.toString())
                            .initCause(ex);
                throw ex;
            }
        }
    }

    InputStream newInputStream(TPath path, OpenOption... options)
    throws IOException {
        return getController()
                .getInputSocket(
                    path.getAddress().getEntryName(),
                    mapInput(options))
                .newInputStream();
    }

    OutputStream newOutputStream(TPath path, OpenOption... options)
    throws IOException {
        return getController()
                .getOutputSocket(
                    path.getAddress().getEntryName(),
                    mapOutput(options),
                    null)
                .newOutputStream();
    }

    DirectoryStream<Path> newDirectoryStream(
            final TPath path,
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

    void createDirectory(final TPath path, final FileAttribute<?>... attrs)
    throws IOException {
        if (0 < attrs.length)
            throw new UnsupportedOperationException();
        final FsController<?> controller = getController();
        final FsEntryName name = path.getAddress().getEntryName();
        try {
            controller.mknod(
                    name,
                    DIRECTORY,
                    NO_OUTPUT_OPTION.set(CREATE_PARENTS, TConfig.get().isLenient()),
                    null);
        } catch (IOException ex) {
            if (null != controller.getEntry(name))
                throw (IOException) new FileAlreadyExistsException(path.toString())
                        .initCause(ex);
            throw ex;
        }
    }

    void delete(TPath path) throws IOException {
        getController().unlink(path.getAddress().getEntryName());
    }

    FsEntry getEntry(TPath path) throws IOException {
        return getController().getEntry(path.getAddress().getEntryName());
    }

    InputSocket<?> getInputSocket(  TPath path,
                                    BitField<FsInputOption> options) {
        return getController()
                .getInputSocket(path.getAddress().getEntryName(), options);
    }

    OutputSocket<?> getOutputSocket(TPath path,
                                    BitField<FsOutputOption> options,
                                    @CheckForNull Entry template) {
        return getController()
                .getOutputSocket(path.getAddress().getEntryName(), options, template);
    }

    void checkAccess(final TPath path, final AccessMode... modes)
    throws IOException {
        final FsEntryName name = path.getAddress().getEntryName();
        final FsController<?> controller = getController();
        if (null == controller.getEntry(name))
            throw new NoSuchFileException(path.toString());
        for (final AccessMode m : modes) {
            switch (m) {
                case READ:
                    if (!controller.isReadable(name))
                        throw new AccessDeniedException(path.toString());
                    break;
                case WRITE:
                    if (!controller.isWritable(name))
                        throw new AccessDeniedException(path.toString());
                    break;
                case EXECUTE:
                    if (!controller.isExecutable(name))
                        throw new AccessDeniedException(path.toString());
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    <V extends FileAttributeView> V getFileAttributeView(
            TPath path,
            Class<V> type,
            LinkOption... options) {
        if (!type.isAssignableFrom(BasicFileAttributeView.class))
            return null;
        return (V) new FsEntryAttributeView(path);
    }

    @SuppressWarnings("unchecked")
    <A extends BasicFileAttributes> A readAttributes(
            TPath path,
            Class<A> type,
            LinkOption... options)
    throws IOException {
        if (!type.isAssignableFrom(BasicFileAttributes.class))
            throw new UnsupportedOperationException();
        return (A) new FsEntryAttributes(path);
    }

    private final class FsEntryAttributeView
    implements BasicFileAttributeView {
        private final TPath path;

        FsEntryAttributeView(final TPath path) {
            this.path = path;
        }

        @Override
        public String name() {
            return "basic";
        }

        @Override
        public BasicFileAttributes readAttributes() throws IOException {
            return new FsEntryAttributes(path);
        }

        @Override
        public void setTimes(   final FileTime lastModifiedTime,
                                final FileTime lastAccessTime,
                                final FileTime createTime)
        throws IOException {
            final FsController<?> controller = getController();
            final Map<Access, Long> times = new EnumMap<>(Access.class);
            if (null != lastModifiedTime)
                times.put(WRITE, lastModifiedTime.toMillis());
            if (null != lastAccessTime)
                times.put(READ, lastAccessTime.toMillis());
            if (null != createTime)
                times.put(CREATE, createTime.toMillis());
            controller.setTime(path.getAddress().getEntryName(), times);
        }
    } // class FsEntryAttributeView

    private final class FsEntryAttributes
    implements BasicFileAttributes {
        private final FsEntry entry;

        FsEntryAttributes(final TPath path) throws IOException {
            if (null == (entry = getController().getEntry(
                    path.getAddress().getEntryName())))
                throw new NoSuchFileException(path.toString());
        }

        @Override
        public FileTime lastModifiedTime() {
            return FileTime.fromMillis(entry.getTime(WRITE));
        }

        @Override
        public FileTime lastAccessTime() {
            return FileTime.fromMillis(entry.getTime(READ));
        }

        @Override
        public FileTime creationTime() {
            return FileTime.fromMillis(entry.getTime(CREATE));
        }

        @Override
        public boolean isRegularFile() {
            return entry.isType(FILE);
        }

        @Override
        public boolean isDirectory() {
            return entry.isType(DIRECTORY);
        }

        @Override
        public boolean isSymbolicLink() {
            return entry.isType(SYMLINK);
        }

        @Override
        public boolean isOther() {
            return entry.isType(SPECIAL);
        }

        @Override
        public long size() {
            final long size = entry.getSize(DATA);
            return UNKNOWN == size ? 0 : size;
        }

        @Override
        public Object fileKey() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    } // class FsEntryAttributes
}
