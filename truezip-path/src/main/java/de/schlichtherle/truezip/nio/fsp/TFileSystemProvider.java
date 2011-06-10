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

import static de.schlichtherle.truezip.entry.Entry.*;
import static de.schlichtherle.truezip.entry.Entry.Access.*;
import static de.schlichtherle.truezip.entry.Entry.Size.*;
import static de.schlichtherle.truezip.entry.Entry.Type.*;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsEntry;
import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsInputOption;
import static de.schlichtherle.truezip.fs.FsInputOptions.*;
import de.schlichtherle.truezip.fs.FsMountPoint;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import static de.schlichtherle.truezip.fs.FsOutputOptions.*;
import de.schlichtherle.truezip.fs.FsScheme;
import static de.schlichtherle.truezip.nio.fsp.TPath.*;
import de.schlichtherle.truezip.socket.IOSocket;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public class TFileSystemProvider extends FileSystemProvider {

    private static volatile TFileSystemProvider
            DEFAULT = new TFileSystemProvider();

    private final String scheme;
    private final FsMountPoint root;

    /**
     * Obtains a file system provider for the given path.
     * 
     * @param  path a path.
     * @return A file system provider.
     */
    public static TFileSystemProvider get(final TPath path) {
        final TFileSystemProvider provider = DEFAULT;
        return null != provider ? provider : new TFileSystemProvider();
    }

    /**
     * @deprecated This constructor is solely provided in order to use this
     *             file system provider class with the service loading feature
     *             of NIO.2.
     * @see        #get(TPath) 
     */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    @SuppressWarnings("LeakingThisInConstructor")
    @Deprecated
    public TFileSystemProvider() {
        this("truezip", FsMountPoint.create(URI.create("file:/")));
        DEFAULT = this;
    }

    private TFileSystemProvider(final String scheme, final FsMountPoint root) {
        this.scheme = FsScheme.create(scheme).toString(); // check syntax
        if (null == root)
            throw new NullPointerException();
        this.root = root;
    }

    /**
     * Returns the URI scheme that identifies this provider.
     *
     * @return The URI scheme that identifies this provider.
     */
    @Override
    public String getScheme() {
        return scheme;
    }

    public FsMountPoint getRoot() {
        return root;
    }

    private static TArchiveDetector getArchiveDetector(@CheckForNull Map<String, ?> env) {
        if (null == env)
            return TPath.getDefaultArchiveDetector();
        TArchiveDetector detector = (TArchiveDetector) env.get(
                Parameter.ARCHIVE_DETECTOR);
        return null != detector ? detector : TPath.getDefaultArchiveDetector();
    }

    /**
     * {@inheritDoc}
     * 
     * @param env If {@code null} or does not contain a {@link TArchiveDetector}
     *        for the key {@link Parameter#ARCHIVE_DETECTOR}, then
     *        {@link TPath#getDefaultArchiveDetector()} is used to detect prospective
     *        archive files.
     */
    @Override
    public TFileSystem newFileSystem(Path path, Map<String, ?> env) {
        TPath p = new TPath(getArchiveDetector(env), path);
        if (null == p.getAddress().getMountPoint().getParent())
            throw new UnsupportedOperationException("no prospective archive file detected"); // don't be greedy!
        return p.getFileSystem();
    }

    /**
     * {@inheritDoc}
     * 
     * @param env If {@code null} or does not contain a {@link TArchiveDetector}
     *        for the key {@link Parameter#ARCHIVE_DETECTOR}, then
     *        {@link TPath#getDefaultArchiveDetector()} is used to detect prospective
     *        archive files.
     */
    @Override
    public TFileSystem newFileSystem(URI uri, @CheckForNull Map<String, ?> env) {
        return new TPath(getArchiveDetector(env), uri).getFileSystem();
    }

    /**
     * Equivalent to {@link #newFileSystem(URI, Map) newFileSystem(uri, null)}.
     * Note that TFileSystem objects are transient - they solely exist to
     * please the NIO.2 API.
     * 
     */
    @Override
    public TFileSystem getFileSystem(URI uri) {
        return newFileSystem(uri, null);
    }

    @Override
    public TPath getPath(URI uri) {
        return new TPath(uri);
    }

    private static BitField<FsInputOption> mapInput(OpenOption... options) {
        HashSet<OpenOption> set = new HashSet<>(options.length * 4 / 3 + 1);
        Collections.addAll(set, options);
        return mapInput(set);
    }

    private static BitField<FsInputOption> mapInput(Set<? extends OpenOption> options) {
        int s = options.size();
        if (0 == s || 1 == s && options.contains(StandardOpenOption.READ))
            return NO_INPUT_OPTION;
        throw new IllegalArgumentException(options.toString());
    }

    private static BitField<FsOutputOption> mapOutput(OpenOption... options) {
        HashSet<OpenOption> set = new HashSet<>(options.length * 4 / 3 + 1);
        Collections.addAll(set, options);
        return mapOutput(set);
    }

    private static BitField<FsOutputOption> mapOutput(Set<? extends OpenOption> options) {
        if (options.isEmpty())
            return NO_OUTPUT_OPTION;
        EnumSet<FsOutputOption> set = EnumSet.noneOf(FsOutputOption.class);
        for (OpenOption option : options) {
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
                    set.add(FsOutputOption.APPEND);
                    break;
                case CREATE_NEW:
                    set.add(FsOutputOption.EXCLUSIVE);
                    break;
                default:
                    throw new UnsupportedOperationException(option.toString());
            }
        }
        return set.isEmpty()
                ? NO_OUTPUT_OPTION
                : BitField.copyOf(set);
    }

    @Override
    public SeekableByteChannel newByteChannel(
            final Path path,
            final Set<? extends OpenOption> options,
            final FileAttribute<?>... attrs)
    throws IOException {
        final TPath p = promote(path);
        if (options.isEmpty() || options.contains(StandardOpenOption.READ))
            return p.getInputSocket(
                        mapInput(options).set(FsInputOption.CACHE))
                    .newSeekableByteChannel();
        else
            return p.getOutputSocket(
                        mapOutput(options)
                            .set(FsOutputOption.CACHE)
                            .set(CREATE_PARENTS, TFileSystem.isLenient()),
                        null)
                    .newSeekableByteChannel();
    }

    @Override
    public InputStream newInputStream(Path path, OpenOption... options)
    throws IOException {
        return promote(path)
                .getInputSocket(mapInput(options))
                .newInputStream();
    }

    @Override
    public OutputStream newOutputStream(Path path, OpenOption... options)
    throws IOException {
        return promote(path)
                .getOutputSocket(
                    mapOutput(options)
                        .set(CREATE_PARENTS, TFileSystem.isLenient()),
                    null)
                .newOutputStream();
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter)
    throws IOException {
        return promote(dir).newDirectoryStream(filter);
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        promote(dir).createDirectory(attrs);
    }

    @Override
    public void delete(Path path) throws IOException {
        delete0(promote(path));
    }

    private void delete0(TPath path) throws IOException {
        promote(path).delete();
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options)
    throws IOException {
        copy0(promote(source), promote(target), options);
    }

    private void copy0( final TPath source,
                        final TPath target,
                        final CopyOption... options)
    throws IOException {
        checkContains(source, target);
        boolean preserve = false;
        BitField<FsOutputOption> outputOptions = BitField
                .of(EXCLUSIVE)
                .set(CREATE_PARENTS, TFileSystem.isLenient());
        if (0 < options.length) {
            for (final CopyOption option : options) {
                if (!(option instanceof StandardCopyOption))
                    throw new UnsupportedOperationException(option.toString());
                switch ((StandardCopyOption) option) {
                    case REPLACE_EXISTING:
                        outputOptions = outputOptions.clear(EXCLUSIVE);
                        break;
                    case COPY_ATTRIBUTES:
                        preserve = true;
                        break;
                    default:
                        throw new UnsupportedOperationException(option.toString());
                }
            }
        }
        final InputSocket<?> input = source.getInputSocket(
                NO_INPUT_OPTION);
        final OutputSocket<?> output = target.getOutputSocket(
                outputOptions,
                preserve ? input.getLocalTarget() : null);
        IOSocket.copy(input, output);
    }

    @Override
    public void move(Path source, Path target, CopyOption... options)
    throws IOException {
        TPath s = promote(source);
        TPath t = promote(target);
        if (null != t.getEntry())
            throw new FileAlreadyExistsException(target.toString());
        copy0(s, t, StandardCopyOption.COPY_ATTRIBUTES);
        delete0(s);
    }

    private static void checkContains(TPath a, TPath b) throws IOException {
        URI ua = a.toRealPath().getAddress().toHierarchicalUri();
        URI ub = b.toRealPath().getAddress().toHierarchicalUri();
        if (ua.resolve(ub) != ub)
            throw new IOException(b + " (contained in " + a + ")");
    }

    @Override
    public boolean isSameFile(Path a, Path b) throws IOException {
        URI ua = promote(a).toRealPath().getAddress().toHierarchicalUri();
        URI ub = promote(b).toRealPath().getAddress().toHierarchicalUri();
        return ua.equals(ub);
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        return promote(path).getFileName().startsWith(".");
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        final TPath p = promote(path);
        final FsEntryName n = p.getAddress().getEntryName();
        final FsController<?> c = p.getController();
        if (null == c.getEntry(n))
            throw new NoSuchFileException(path.toString());
        for (final AccessMode m : modes) {
            switch (m) {
                case READ:
                    if (!c.isReadable(n))
                        throw new AccessDeniedException(path.toString());
                    break;
                case WRITE:
                    if (!c.isWritable(n))
                        throw new AccessDeniedException(path.toString());
                    break;
                case EXECUTE:
                    if (!c.isExecutable(n))
                        throw new AccessDeniedException(path.toString());
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V extends FileAttributeView> V getFileAttributeView(
            Path path,
            Class<V> type,
            LinkOption... options) {
        if (!type.isAssignableFrom(BasicFileAttributeView.class))
            throw new UnsupportedOperationException();
        return (V) new FsEntryAttributeView(promote(path));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A extends BasicFileAttributes> A readAttributes(
            Path path,
            Class<A> type,
            LinkOption... options)
    throws IOException {
        if (!type.isAssignableFrom(BasicFileAttributes.class))
            throw new UnsupportedOperationException();
        return (A) new FsEntryAttributes(promote(path));
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /** Keys for environment maps. */
    public interface Parameter {
        /** The key for the {@link TArchiveDetector} parameter. */
        String ARCHIVE_DETECTOR = "ARCHIVE_DETECTOR";
    }

    private static final class FsEntryAttributeView
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
            final FsController<?> c = path.getController();
            final Map<Access, Long> t = new EnumMap<>(Access.class);
            t.put(WRITE, toMillis(lastModifiedTime));
            t.put(READ, toMillis(lastAccessTime));
            t.put(CREATE, toMillis(createTime));
            c.setTime(path.getAddress().getEntryName(), t);
        }

        private static long toMillis(FileTime time) {
            return time == null ? null : time.toMillis();
        }
    } // class FsEntryAttributeView

    private static final class FsEntryAttributes
    implements BasicFileAttributes {
        private final FsEntry e;

        FsEntryAttributes(TPath path) throws IOException {
            e = path.getEntry();
        }

        @Override
        public FileTime lastModifiedTime() {
            return e == null ? null : FileTime.fromMillis(e.getTime(WRITE));
        }

        @Override
        public FileTime lastAccessTime() {
            return e == null ? null : FileTime.fromMillis(e.getTime(READ));
        }

        @Override
        public FileTime creationTime() {
            return e == null ? null : FileTime.fromMillis(e.getTime(CREATE));
        }

        @Override
        public boolean isRegularFile() {
            return e == null ? false : e.isType(FILE);
        }

        @Override
        public boolean isDirectory() {
            return e == null ? false : e.isType(DIRECTORY);
        }

        @Override
        public boolean isSymbolicLink() {
            return e == null ? false : e.isType(SYMLINK);
        }

        @Override
        public boolean isOther() {
            return e == null ? false : e.isType(SPECIAL);
        }

        @Override
        public long size() {
            return e == null ? UNKNOWN : e.getSize(DATA);
        }

        @Override
        public Object fileKey() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    } // class FsEntryAttributes
}
