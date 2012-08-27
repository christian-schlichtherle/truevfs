/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.FsAccessOption;
import static net.java.truevfs.kernel.spec.FsAccessOption.EXCLUSIVE;
import net.java.truevfs.kernel.spec.FsMountPoint;
import net.java.truevfs.kernel.spec.FsNode;
import static net.java.truevfs.kernel.spec.FsNodeName.SEPARATOR;
import net.java.truevfs.kernel.spec.FsNodePath;
import static net.java.truevfs.kernel.spec.cio.Entry.Type.DIRECTORY;
import static net.java.truevfs.kernel.spec.cio.Entry.Type.FILE;
import net.java.truevfs.kernel.spec.cio.InputSocket;
import net.java.truevfs.kernel.spec.cio.IoSockets;
import net.java.truevfs.kernel.spec.cio.OutputSocket;
import org.slf4j.LoggerFactory;

/**
 * A {@link FileSystemProvider} implementation
 * based on the TrueVFS Kernel module.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class TFileSystemProvider extends FileSystemProvider {

    /** The scheme of the provider for the public no-arg constructor. */
    public  static final String DEFAULT_SCHEME = "tpath";

    /** The root mount point of the provider for the public no-arg constructor. */
    public static final String DEFAULT_ROOT_MOUNT_POINT = "file:/";

    private static final URI DEFAULT_ROOT_MOUNT_POINT_URI
            = URI.create(DEFAULT_ROOT_MOUNT_POINT);
    private static final Map<String, TFileSystemProvider>
            providers = new WeakHashMap<>();

    private final String scheme;
    private final FsNodePath root;

    private Map<FsMountPoint, WeakReference<TFileSystem>>
            fileSystems = new WeakHashMap<>();

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
     * Constructs a new TrueVFS file system provider which is identified by the
     * URI {@link #getScheme() scheme} {@value #DEFAULT_SCHEME} for accessing
     * any path within the {@link #getRoot() root mount point} URI
     * {@value #DEFAULT_ROOT_MOUNT_POINT}.
     * 
     * @deprecated This constructor is solely provided in order to use this
     * file system provider class with the service location feature of the
     * NIO.2 API!
     */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    @SuppressWarnings("LeakingThisInConstructor")
    @Deprecated
    public TFileSystemProvider() {
        this(DEFAULT_SCHEME, DEFAULT_ROOT_MOUNT_POINT_URI);
        synchronized (TFileSystemProvider.class) {
            providers.put(scheme, this);
        }
        LoggerFactory.getLogger(TFileSystemProvider.class).debug("installed");
    }

    private TFileSystemProvider(final String scheme, final URI root) {
        assert null != scheme;
        this.scheme = scheme;
        this.root = FsNodePath.create(root);
    }

    /**
     * Returns the default scheme of this provider.
     * 
     * @return the default scheme of this provider.
     */
    @Override
    public String getScheme() { return scheme; }

    /**
     * Returns the root mount point of this provider.
     * 
     * @return The root mount point of this provider.
     */
    public FsNodePath getRoot() { return root; }

    private static TConfig open(Map<String, ?> env) {
        final TConfig config = TConfig.open();
        final TArchiveDetector detector =
                (TArchiveDetector) env.get(Parameter.ARCHIVE_DETECTOR);
        if (null != detector) config.setArchiveDetector(detector);
        return config;
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
        try (final TConfig config = open(configuration)) {
            final TPath p = new TPath(path);
            if (null == p.getMountPoint().getParent())
                throw new UnsupportedOperationException("No prospective archive file detected."); // don't be greedy!
            return p.getFileSystem();
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
        try (final TConfig config = open(configuration)) {
            return getFileSystem(uri);
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
    TFileSystem getFileSystem(final TPath path) {
        final FsMountPoint mp = path.getMountPoint();
        synchronized (this) {
            TFileSystem fs = deref(fileSystems.get(mp));
            if (null == fs)
                fileSystems.put(mp, new WeakReference<>(fs = new TFileSystem(path)));
            return fs;
        }
    }

    private static <T> T deref(Reference<T> ref) {
        return null != ref ? ref.get() : null;
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
        promote(path).delete();
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options)
    throws IOException {
        copy(promote(source), promote(target), options);
    }

    private static void copy(   final TPath src,
                                final TPath dst,
                                final CopyOption... options)
    throws IOException {
        if (isSameFile0(src, dst))
            throw new FileSystemException(src.toString(), dst.toString(), "Source and destination are the same file!");
        boolean preserve = false;
        BitField<FsAccessOption> o = dst.getAccessPreferences().set(EXCLUSIVE);
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
        final FsNode srcEntry = src.stat();
        final FsNode dstEntry = dst.stat();
        if (null == srcEntry)
            throw new NoSuchFileException(src.toString());
        if (!srcEntry.isType(FILE))
            throw new FileSystemException(src.toString(), null,
                    "Expected type FILE, but is " + srcEntry.getTypes() + "!");
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
        final InputSocket<?> input = src.input(src.getAccessPreferences());
        final OutputSocket<?> output = dst.output(o,
                preserve ? input.target() : null);
        IoSockets.copy(input, output);
    }

    @Override
    public void move(Path source, Path target, CopyOption... options)
    throws IOException {
        move(promote(source), promote(target), options);
    }

    private static void move(TPath source, TPath target, CopyOption... options)
    throws IOException {
        copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
        source.delete();
    }

    @Override
    public boolean isSameFile(Path a, Path b) throws IOException {
        return isSameFile0(a, b);
    }

    private static boolean isSameFile0(final Path a, final Path b)
    throws IOException {
        return a.equals(b) || a.toRealPath().equals(b.toRealPath());
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
    @SuppressWarnings("PublicInnerClass")
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