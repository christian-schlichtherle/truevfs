/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.nio.file;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Access;
import static de.schlichtherle.truezip.entry.Entry.Access.*;
import static de.schlichtherle.truezip.entry.Entry.Size.DATA;
import static de.schlichtherle.truezip.entry.Entry.Type.*;
import static de.schlichtherle.truezip.entry.Entry.UNKNOWN;
import static de.schlichtherle.truezip.entry.EntryName.SEPARATOR;
import de.schlichtherle.truezip.file.TConfig;
import static de.schlichtherle.truezip.fs.FsOutputOption.EXCLUSIVE;
import static de.schlichtherle.truezip.fs.FsSyncOptions.UMOUNT;
import de.schlichtherle.truezip.fs.*;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.FilteringIterator;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A {@link FileSystem} implementation based on the TrueZIP Kernel module.
 * 
 * @since  TrueZIP 7.2
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class TFileSystem extends FileSystem {

    private final FsController<?> controller;
    private final TFileSystemProvider provider;


    @SuppressWarnings("deprecation")
    TFileSystem(final TPath path) {
        assert null != path;
        this.controller = TConfig.get().getFsManager().getController(
                path.getMountPoint(),
                path.getArchiveDetector());
        this.provider = TFileSystemProvider.get(path.getName());

        assert invariants();
    }

    private boolean invariants() {
        assert null != getController();
        assert null != provider();
        return true;
    }

    /**
     * Equivalent to
     * {@link TConfig#isLenient TConfig.get().isLenient()}.
     */
    public static boolean isLenient() {
        return TConfig.get().isLenient();
    }

    /**
     * Equivalent to
     * {@link TConfig#setLenient TConfig.get().setLenient(lenient)}.
     */
    public static void setLenient(boolean lenient) {
        TConfig.get().setLenient(lenient);
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
     *         {@code FsSyncOption.FORCE_CLOSE_OUTPUT} is consumed or if the
     *         synchronization option {@code FsSyncOption.ABORT_CHANGES} is consumed.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         occur.
     *         This implies that the respective parent file system has been
     *         updated with constraints, such as a failure to consumed the last
     *         modification time of the entry for the federated file system
     *         (i.e. archive file) in its parent file system.
     * @throws FsSyncException if any error conditions occur.
     *         This implies loss of data!
     * @see    #sync(BitField)
     */
    @SuppressWarnings("deprecation")
    public void sync(BitField<FsSyncOption> options) throws FsSyncException {
        new FsFilteringManager(TConfig.get().getFsManager(), getMountPoint())
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
     * Note that the file system stays open (!) after this call and can get
     * used subsequently.
     *
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         occur.
     *         This implies that the respective parent file system has been
     *         updated with constraints, such as a failure to consumed the last
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

    /** 
     * Returns {@link File#separator}.
     * 
     * @return {@link File#separator}.
     */
    @Override
    public String getSeparator() {
        return File.separator;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.singleton((Path)
                new TPath(getMountPoint().toHierarchicalUri().resolve(SEPARATOR)));
    }

    /** @throws UnsupportedOperationException always */
    @Override
    public Iterable<FileStore> getFileStores() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return Collections.singleton("basic");
    }

    /**
     * Constructs a new path from the given sub path strings.
     * <p>
     * This method scans the {@link TPath#toString() path name} resulting
     * from the segment parameters to detect prospective archive files using
     * the {@link TPath#getDefaultArchiveDetector() default archive detector}.
     * <p>
     * The supported path name separators are "{@link File#separator}" and
     * "{@code /}".
     * Any leading and trailing separators in the resulting path name get
     * discarded.
     * 
     * @param first the first sub path string.
     * @param more optional sub path strings.
     */
    @Override
    public TPath getPath(String first, String... more) {
        return new TPath(this, first, more);
    }

    /** @throws UnsupportedOperationException always */
    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /** @throws UnsupportedOperationException always */
    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /** @throws UnsupportedOperationException always */
    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    SeekableByteChannel newByteChannel(
            final TPath path,
            final Set<? extends OpenOption> options,
            final FileAttribute<?>... attrs)
    throws IOException {
        final FsEntryName name = path.getEntryName();
        final FsController<?> controller = getController();
        if (options.isEmpty() || options.contains(StandardOpenOption.READ)) {
            final BitField<FsInputOption>
                    o = path.mapInput(options).set(FsInputOption.CACHE);
            return controller
                    .getInputSocket(name, o)
                    .newSeekableByteChannel();
        } else {
            final BitField<FsOutputOption>
                    o = path.mapOutput(options).set(FsOutputOption.CACHE);
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
                    path.getEntryName(),
                    path.mapInput(options))
                .newInputStream();
    }

    OutputStream newOutputStream(TPath path, OpenOption... options)
    throws IOException {
        return getController()
                .getOutputSocket(
                    path.getEntryName(),
                    path.mapOutput(options),
                    null)
                .newOutputStream();
    }

    DirectoryStream<Path> newDirectoryStream(
            final TPath path,
            final Filter<? super Path> filter)
    throws IOException {
        final FsEntry entry = getEntry(path);
        final Set<String> set;
        if (null == entry || null == (set = entry.getMembers()))
            throw new NotDirectoryException(path.toString());

        @NotThreadSafe
        class Adapter implements Iterator<Path> {
            final Iterator<String> it = set.iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Path next() {
                return path.resolve(it.next());
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        } // Adapter

        @NotThreadSafe
        class FilterIterator extends FilteringIterator<Path> {
            FilterIterator() { super(new Adapter()); }

            @Override
            protected boolean accept(Path element) {
                try {
                    return filter.accept(element);
                } catch (IOException ex) {
                    throw new DirectoryIteratorException(ex);
                }
            }
        } // FilterIterator

        return  new Stream(new FilterIterator());
    }

    @NotThreadSafe
    private static final class Stream implements DirectoryStream<Path> {
        final Iterator<Path> it;
        boolean consumed;

        Stream(final Iterator<Path> it) {
            this.it = it;
        }
        
        @Override
        public Iterator<Path> iterator() {
            if (consumed)
                throw new IllegalStateException();
            consumed = true;
            return it;
        }

        @Override
        public void close() {
            consumed = true;
        }
    } // Stream

    void createDirectory(final TPath path, final FileAttribute<?>... attrs)
    throws IOException {
        if (0 < attrs.length)
            throw new UnsupportedOperationException();
        final FsController<?> controller = getController();
        final FsEntryName name = path.getEntryName();
        try {
            controller.mknod(
                    name,
                    DIRECTORY,
                    path.getOutputPreferences(),
                    null);
        } catch (IOException ex) {
            if (null != controller.getEntry(name))
                throw (IOException) new FileAlreadyExistsException(path.toString())
                        .initCause(ex);
            throw ex;
        }
    }

    void delete(TPath path) throws IOException {
        getController().unlink(path.getEntryName(), path.getOutputPreferences());
    }

    FsEntry getEntry(TPath path) throws IOException {
        return getController().getEntry(path.getEntryName());
    }

    InputSocket<?> getInputSocket(  TPath path,
                                    BitField<FsInputOption> options) {
        return getController()
                .getInputSocket(path.getEntryName(), options);
    }

    OutputSocket<?> getOutputSocket(TPath path,
                                    BitField<FsOutputOption> options,
                                    @CheckForNull Entry template) {
        return getController()
                .getOutputSocket(path.getEntryName(), options, template);
    }

    void checkAccess(final TPath path, final AccessMode... modes)
    throws IOException {
        final FsEntryName name = path.getEntryName();
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

    @Nullable
    <V extends FileAttributeView> V getFileAttributeView(
            TPath path,
            Class<V> type,
            LinkOption... options) {
        if (type.isAssignableFrom(BasicFileAttributeView.class))
            return type.cast(new FsEntryAttributeView(path));
        return null;
    }

    <A extends BasicFileAttributes> A readAttributes(
            TPath path,
            Class<A> type,
            LinkOption... options)
    throws IOException {
        if (type.isAssignableFrom(BasicFileAttributes.class))
            return type.cast(new FsEntryAttributes(path));
        throw new UnsupportedOperationException();
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
            final Map<Access, Long> times = new EnumMap<Access, Long>(
                    Access.class);
            if (null != lastModifiedTime)
                times.put(WRITE, lastModifiedTime.toMillis());
            if (null != lastAccessTime)
                times.put(READ, lastAccessTime.toMillis());
            if (null != createTime)
                times.put(CREATE, createTime.toMillis());
            controller.setTime(
                    path.getEntryName(),
                    times,
                    path.getOutputPreferences());
        }
    } // FsEntryAttributeView

    private final class FsEntryAttributes
    implements BasicFileAttributes {
        private final FsEntry entry;

        FsEntryAttributes(final TPath path) throws IOException {
            if (null == (entry = getController().getEntry(path.getEntryName())))
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

        /** @throws UnsupportedOperationException always */
        @Override
        public Object fileKey() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    } // FsEntryAttributes
}
