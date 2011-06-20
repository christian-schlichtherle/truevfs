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
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.socket.IOSocket;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CheckForNull;
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
import net.jcip.annotations.Immutable;

/**
 * A {@link FileSystemProvider} implementation based on the TrueZIP Kernel
 * module.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public final class TFileSystemProvider extends FileSystemProvider {

    private static volatile TFileSystemProvider
            DEFAULT = new TFileSystemProvider();

    private final String scheme;
    private final FsPath root;
    private final FsPath current;

    /**
     * Obtains a file system provider for the given path.
     * 
     * @param  path a path.
     * @return A file system provider.
     */
    static TFileSystemProvider get(final TPath path) {
        return DEFAULT;
    }

    /**
     * Do <em>NOT</em> call this constructor!
     * 
     * @deprecated This constructor is solely provided in order to use this
     *             file system provider class with the service loading feature
     *             of NIO.2.
     */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    @SuppressWarnings("LeakingThisInConstructor")
    @Deprecated
    public TFileSystemProvider() {
        this(   FsScheme.create("tpath"),
                FsMountPoint.create(URI.create("file:/")),
                FsMountPoint.create(new File("").toURI()));
        DEFAULT = this;
    }

    private TFileSystemProvider(
            final FsScheme scheme,
            final FsMountPoint root,
            final FsMountPoint current) {
        this.scheme = scheme.toString();
        this.root = new FsPath(root, ROOT);
        this.current = new FsPath(current, ROOT);

        assert invariants();
    }

    private boolean invariants() {
        assert null != getScheme();
        assert !getRoot().toUri().isOpaque();
        assert !getCurrent().toUri().isOpaque();
        return true;
    }

    @Override
    public String getScheme() {
        return scheme;
    }

    FsPath getRoot() {
        return root;
    }

    FsPath getCurrent() {
        return current;
    }

    private static void pushConfiguration(final @CheckForNull Map<String, ?> env) {
        if (null == env)
            return;
        final TArchiveDetector detector = (TArchiveDetector) env.get(
                Parameter.ARCHIVE_DETECTOR);
        final Boolean lenient = (Boolean) env.get(Parameter.LENIENT);
        if (null == detector && null == lenient)
            return;
        TConfig session = TConfig.get();
        if ((null == detector || detector == session.getArchiveDetector())
                && (null == lenient || lenient == session.isLenient()))
            return;
        session = TConfig.push();
        if (null != detector)
            session.setArchiveDetector(detector);
        if (null != lenient)
            session.setLenient(lenient);
    }

    /**
     * {@inheritDoc}
     * 
     * @param env May contain a {@link TArchiveDetector} for the key
     *        {@link Parameter#ARCHIVE_DETECTOR} or a {@link Boolean} for the
     *        key {@link Parameter#LENIENT} for subsequent use.
     *        If any key is present and the respective value differs from the
     *        {@link TConfig#get() current configuration}, then a new
     *        configuration is {@link TConfig#push() pushed} on the thread
     *        local configuration stack.
     */
    @Override
    public TFileSystem newFileSystem(Path path, @CheckForNull Map<String, ?> env) {
        pushConfiguration(env);
        TPath p = new TPath(path);
        if (null == p.getAddress().getMountPoint().getParent())
            throw new UnsupportedOperationException("no prospective archive file detected"); // don't be greedy!
        return p.getFileSystem();
    }

    /**
     * {@inheritDoc}
     * 
     * @param env May contain a {@link TArchiveDetector} for the key
     *        {@link Parameter#ARCHIVE_DETECTOR} or a {@link Boolean} for the
     *        key {@link Parameter#LENIENT} for subsequent use.
     *        If any key is present and the respective value differs from the
     *        {@link TConfig#get() current configuration}, then a new
     *        configuration is {@link TConfig#push() pushed} on the thread
     *        local configuration stack.
     */
    @Override
    public TFileSystem newFileSystem(URI uri, @CheckForNull Map<String, ?> env) {
        pushConfiguration(env);
        return new TPath(uri).getFileSystem();
    }

    /**
     * Equivalent to {@link #newFileSystem(URI, Map) newFileSystem(uri, null)}.
     */
    @Override
    public TFileSystem getFileSystem(URI uri) {
        return newFileSystem(uri, null);
    }

    @Override
    public TPath getPath(URI uri) {
        return new TPath(uri);
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
                .set(CREATE_PARENTS, TConfig.get().isLenient());
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
        final InputSocket<?> input = src.getInputSocket(NO_INPUT_OPTION);
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
        return promote(path).getFileName().startsWith(".");
    }

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
    @SuppressWarnings("unchecked")
    public <V extends FileAttributeView> V getFileAttributeView(
            Path path,
            Class<V> type,
            LinkOption... options) {
        return promote(path).getFileAttributeView(type, options);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A extends BasicFileAttributes> A readAttributes(
            Path path,
            Class<A> type,
            LinkOption... options)
    throws IOException {
        return promote(path).readAttributes(type, options);
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean equals(Object that) {
        return this == that
                || that instanceof TFileSystemProvider
                    && this.getScheme().equalsIgnoreCase(
                        ((TFileSystemProvider) that).getScheme());
    }

    public int hashCode() {
        return getScheme().hashCode();
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
        /** The key for the {@code lenient} parameter. */
        String LENIENT = "lenient";
    }
}
