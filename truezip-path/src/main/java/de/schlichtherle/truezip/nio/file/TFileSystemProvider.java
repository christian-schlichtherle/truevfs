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

import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.FsSyncException;
import de.schlichtherle.truezip.fs.FsSyncWarningException;
import net.jcip.annotations.ThreadSafe;
import java.util.logging.Logger;
import static de.schlichtherle.truezip.entry.Entry.Type.*;
import de.schlichtherle.truezip.file.TConfig;
import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.fs.FsEntry;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import static de.schlichtherle.truezip.fs.FsInputOptions.*;
import de.schlichtherle.truezip.fs.FsMountPoint;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import de.schlichtherle.truezip.fs.FsPath;
import static de.schlichtherle.truezip.fs.FsManager.*;
import de.schlichtherle.truezip.fs.sl.FsManagerLocator;
import de.schlichtherle.truezip.socket.IOSocket;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import static java.util.logging.Level.*;

/**
 * A {@link FileSystemProvider} implementation
 * based on the TrueZIP Kernel module.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public final class TFileSystemProvider extends FileSystemProvider {

    /** The scheme of the provider for the public no-arg constructor. */
    public  static final String DEFAULT_SCHEME = "tpath";

    /** The root mount point of the provider for the public no-arg constructor. */
    public static final String DEFAULT_ROOT_MOUNT_POINT = "file:/";

    private static final URI DEFAULT_ROOT_MOUNT_POINT_URI
            = URI.create(DEFAULT_ROOT_MOUNT_POINT);
    private static final FsManager manager = FsManagerLocator.SINGLETON.get();
    private static final Map<String, TFileSystemProvider>
            providers = new WeakHashMap<String, TFileSystemProvider>();

    private final String scheme;
    private final FsPath root;

    private Map<FsMountPoint, TFileSystem>
            fileSystems = new WeakHashMap<FsMountPoint, TFileSystem>();

    /**
     * Obtains a file system provider for the given {@link TPath} URI.
     * 
     * @param  name a {@link TPath} URI.
     * @return A file system provider.
     */
    static synchronized TFileSystemProvider get(URI name) {
        if (!TPathScanner.isAbsolute(name))
            return Holder.CURRENT_DIRECTORY_PROVIDER;
        if (!name.isAbsolute())
            name = DEFAULT_ROOT_MOUNT_POINT_URI;
        String scheme = name.getScheme();
        TFileSystemProvider provider = providers.get(scheme);
        if (null != provider)
            return provider;
        provider = new TFileSystemProvider(scheme, name.resolve(SEPARATOR));
        providers.put(scheme, provider);
        return provider;
    }

    /**
     * Constructs a new TrueZIP file system provider which is identified by the
     * URI {@link #getScheme() scheme} {@value #DEFAULT_SCHEME} for accessing
     * any path within the {@link #getRoot() root mount point} URI
     * {@value #DEFAULT_ROOT_MOUNT_POINT}.
     * <p>
     * Do <em>NOT</em> call this constructor directly - it's solely provided in
     * order to use this file system provider class with the service location
     * feature of the NIO.2 API!
     * 
     * @deprecated Do <em>NOT</em> call this constructor directly!
     */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    @SuppressWarnings("LeakingThisInConstructor")
    @Deprecated
    public TFileSystemProvider() {
        this(DEFAULT_SCHEME, DEFAULT_ROOT_MOUNT_POINT_URI);
        synchronized (TFileSystemProvider.class) {
            providers.put(scheme, this);
        }
        Logger  .getLogger(TFileSystemProvider.class.getName())
                .log(CONFIG, "Installed TrueZIP file system provider instance.");
    }

    private TFileSystemProvider(final String scheme, final URI root) {
        assert null != scheme;
        this.scheme = scheme;
        this.root = FsPath.create(root);
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
     */
    public static void umount() throws FsSyncException {
        manager.sync(UMOUNT);
    }

    /**
     * Returns the default scheme of this provider.
     * 
     * @return the default scheme of this provider.
     */
    @Override
    public String getScheme() {
        return scheme;
    }

    /**
     * Returns the root mount point of this provider.
     * 
     * @return The root mount point of this provider.
     */
    public FsPath getRoot() {
        return root;
    }

    private static TConfig push(Map<String, ?> env) {
        final TArchiveDetector detector = (TArchiveDetector) env.get(
                Parameter.ARCHIVE_DETECTOR);
        TConfig session = TConfig.push();
        if (null != detector && detector != session.getArchiveDetector())
            session.setArchiveDetector(detector);
        return session;
    }

    /**
     * Scans the given {@code path} for prospective archive files using the
     * given {@code configuration} and returns the file system for the
     * innermost prospective archive file or throws an
     * {@link UnsupportedOperationException} if no prospective archive file is
     * detected.
     * <p>
     * First, the {@code configuration} {@link Parameter}s get enumerated.
     * If no value is set for a parameter key, the respective value of the
     * {@link TConfig#get() current configuration} gets used.
     * <p>
     * Next, the {@code path} is scanned for prospective archive files using
     * the configuration resulting from the first step.
     * If one or more prospective archive files are found, the file system for
     * the innermost prospective archive file is returned.
     * Otherwise, an {@link UnsupportedOperationException} is thrown.
     * 
     * @param  path the path to scan for prospective archive files.
     * @param  configuration may contain a {@link TArchiveDetector} for the key
     *         {@link Parameter#ARCHIVE_DETECTOR}.
     * @return the file system for the innermost prospective archive file.
     * @throws UnsupportedOperationException if no prospective archive file has
     *         been detected according to the configuration resulting from
     *         merging the given {@code configuration} with the
     *         {@link TConfig#get() current configuration}.
     */
    @Override
    public TFileSystem newFileSystem(Path path, Map<String, ?> configuration) {
        TConfig config = push(configuration);
        try {
            TPath p = new TPath(path);
            if (null == p.getMountPoint().getParent())
                throw new UnsupportedOperationException("No prospective archive file detected."); // don't be greedy!
            return p.getFileSystem();
        } finally {
            config.close();
        }
    }

    /**
     * Returns a file system for the given hierarchical {@link TPath}
     * {@code uri}.
     * <p>
     * First, the {@code configuration} {@link Parameter}s get enumerated.
     * If no value is set for a parameter key, the respective value of the
     * {@link TConfig#get() current configuration} gets used.
     * <p>
     * Next, the {@code uri} is scanned for prospective archive files using
     * the configuration resulting from the first step.
     * Any trailing separators in {@code uri} get discarded.
     * If one or more prospective archive files are found, the file system for
     * the innermost prospective archive file is returned.
     * Otherwise, the file system for the innermost directory is returned.
     * 
     * @param  uri the {@link TPath} uri to return a file system for.
     * @param  configuration may contain a {@link TArchiveDetector} for the key
     *         {@link Parameter#ARCHIVE_DETECTOR}.
     * @return the file system for the innermost prospective archive file or
     *         directory.
     * @throws IllegalArgumentException if the given {@code uri} is opaque.
     */
    @Override
    public TFileSystem newFileSystem(URI uri, Map<String, ?> configuration) {
        TConfig config = push(configuration);
        try {
            return getFileSystem(uri);
        } finally {
            config.close();
        }
    }

    /**
     * Returns a file system for the given hierarchical {@link TPath}
     * {@code uri}.
     * <p>
     * The {@code uri} is scanned for prospective archive files using the
     * {@link TConfig#get() current configuration}.
     * Any trailing separators in {@code uri} get discarded.
     * If one or more prospective archive files are found, the file system for
     * the innermost prospective archive file is returned.
     * Otherwise, the file system for the innermost directory is returned.
     * 
     * @param  uri the {@link TPath} uri to return a file system for.
     * @return the file system for the innermost prospective archive file or
     *         directory.
     * @throws IllegalArgumentException if the given {@code uri} is opaque.
     */
    @Override
    public TFileSystem getFileSystem(URI uri) {
        return getPath(uri).getFileSystem();
    }

    /**
     * Obtains a file system for the given path.
     * If a file system doesn't exist yet, it's created.
     * 
     * @param  path a path.
     * @return A file system.
     */
    synchronized TFileSystem getFileSystem(final TPath path) {
        final FsMountPoint mp = path.getMountPoint();
        TFileSystem fs = fileSystems.get(mp);
        if (null == fs)
            fileSystems.put(mp, fs = new TFileSystem(path));
        return fs;
    }

    /**
     * Returns a {@code TPath} for the given hierarchical {@code name}.
     * <p>
     * The URI path component is scanned for prospective archive files using
     * the {@link TConfig#get() current configuration}.
     * Any trailing separators in {@code name} get discarded.
     * 
     * @param  name the uri to return a {@link TPath} for.
     * @return the {@link TPath}
     * @throws IllegalArgumentException if the given {@code uri} is opaque.
     */
    @Override
    public TPath getPath(URI name) {
        if (!getScheme().equals(name.getScheme()))
            throw new IllegalArgumentException();
        return new TPath(name);
    }

    @Override
    public SeekableByteChannel newByteChannel(
            Path path,
            Set<? extends OpenOption> options,
            FileAttribute<?>... attrs)
    throws IOException {
        return promote(path).newByteChannel(options, attrs);
    }

    @Override
    public InputStream newInputStream(Path path, OpenOption... options)
    throws IOException {
        return promote(path).newInputStream(options);
    }

    @Override
    public OutputStream newOutputStream(Path path, OpenOption... options)
    throws IOException {
        return promote(path).newOutputStream(options);
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
        delete(promote(path));
    }

    private static void delete(TPath path) throws IOException {
        path.delete();
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options)
    throws IOException {
        copy(promote(source), promote(target), options);
    }

    private static void copy(
            final TPath src,
            final TPath dst,
            final CopyOption... options)
    throws IOException {
        if (isSameFile0(src, dst))
            throw new IOException(dst + " (source and destination are the same file)");
        boolean preserve = false;
        BitField<FsOutputOption> o = BitField
                .of(EXCLUSIVE)
                .set(CREATE_PARENTS, dst.shouldCreateParents());
        for (final CopyOption option : options) {
            if (!(option instanceof StandardCopyOption))
                throw new UnsupportedOperationException(option.toString());
            switch ((StandardCopyOption) option) {
                case REPLACE_EXISTING:
                    o = o.clear(EXCLUSIVE);
                    break;
                case COPY_ATTRIBUTES:
                    preserve = true;
                    break;
                case ATOMIC_MOVE:
                    throw new AtomicMoveNotSupportedException(src.toString(), dst.toString(), null);
                default:
                    throw new UnsupportedOperationException(option.toString());
            }
        }
        final FsEntry srcEntry = src.getEntry();
        final FsEntry dstEntry = dst.getEntry();
        if (null == srcEntry)
            throw new NoSuchFileException(src.toString());
        if (!srcEntry.isType(FILE))
            throw new FileNotFoundException(
                    src.toString() + " (expected FILE - is "
                    + BitField.copyOf(srcEntry.getTypes()) + ")");
        if (null != dstEntry) {
            if (o.get(EXCLUSIVE))
                throw new FileAlreadyExistsException(dst.toString());
            if (dstEntry.isType(DIRECTORY)) {
                try {
                    // Try first...
                    dst.delete();
                } catch (IOException ex) {
                    // ... before you check!
                    if (!dstEntry.getMembers().isEmpty())
                        throw (IOException) new DirectoryNotEmptyException(dst.toString())
                                .initCause(ex);
                    throw ex;
                }
            }
        }
        final InputSocket<?> input = src.getInputSocket(NO_INPUT_OPTIONS);
        final OutputSocket<?> output = dst.getOutputSocket(o,
                preserve ? input.getLocalTarget() : null);
        IOSocket.copy(input, output);
    }

    @Override
    public void move(Path source, Path target, CopyOption... options)
    throws IOException {
        move(promote(source), promote(target), options);
    }

    private static void move(TPath source, TPath target, CopyOption... options)
    throws IOException {
        copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
        delete(source);
    }

    @Override
    public boolean isSameFile(Path a, Path b) throws IOException {
        return isSameFile0(a, b);
    }

    private static boolean isSameFile0(final Path a, final Path b)
    throws IOException {
        final String as = a.toRealPath().toString();
        final String bs = b.toRealPath().toString();
        return as.equals(bs);
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        return promote(path).getFileName().toString().startsWith(".");
    }

    /** @throws UnsupportedOperationException always */
    @Override
    public FileStore getFileStore(Path path) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        promote(path).checkAccess(modes);
    }

    @Override
    @Nullable
    public <V extends FileAttributeView> V getFileAttributeView(
            Path path,
            Class<V> type,
            LinkOption... options) {
        return promote(path).getFileAttributeView(type, options);
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(
            Path path,
            Class<A> type,
            LinkOption... options)
    throws IOException {
        return promote(path).readAttributes(type, options);
    }

    /** @throws UnsupportedOperationException always */
    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /** @throws UnsupportedOperationException always */
    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static TPath promote(Path path) {
        try {
            return (TPath) path;
        } catch (ClassCastException ex) {
            throw (ProviderMismatchException) new ProviderMismatchException(
                        ex.toString())
                    .initCause(ex);
        }
    }

    /** Keys for environment maps. */
    public interface Parameter {
        /** The key for the {@code archiveDetector} parameter. */
        String ARCHIVE_DETECTOR = "archiveDetector";
    }

    private static final class Holder {
        static final TFileSystemProvider
            CURRENT_DIRECTORY_PROVIDER = new TFileSystemProvider(
                "file", new File("").toURI());

        private Holder() {
        }
    }
}
