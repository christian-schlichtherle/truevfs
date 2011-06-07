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
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import de.schlichtherle.truezip.fs.FsInputOption;
import static de.schlichtherle.truezip.fs.FsInputOptions.*;
import de.schlichtherle.truezip.fs.FsMountPoint;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOptions.*;
import de.schlichtherle.truezip.fs.FsPath;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.io.Paths.Splitter;
import de.schlichtherle.truezip.socket.IOSocket;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.UriBuilder;
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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.spi.FileSystemProvider;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public class TFileSystemProvider extends FileSystemProvider {

    private final FsScheme scheme;
    private final FsMountPoint mountPoint;
    //private volatile IOPool<?> pool;

    public TFileSystemProvider() {
        this(   FsScheme.create("tzp"),
                FsMountPoint.create(fix(Paths.get("/").toUri())));
    }

    TFileSystemProvider(final FsScheme scheme,
                        final FsMountPoint mountPoint) {
        if (null == scheme || null == mountPoint)
            throw new NullPointerException();
        this.scheme = scheme;
        this.mountPoint = mountPoint;
    }

    /**
     * Returns the URI scheme that identifies this provider.
     *
     * @return The URI scheme that identifies this provider.
     */
    @Override
    public String getScheme() {
        return scheme.toString();
    }

    public FsMountPoint getMountPoint() {
        return mountPoint;
    }

    /**
     * Rewrites URIs of the form {@code [scheme:]///path[?query][#fragment]} to
     * {@code [scheme:]/path[?query][#fragment]} in order to fix the broken
     * identity provision for hierarchical URIs with an empty authority in the
     * URI class.
     * All other URIs are returned unchanged.
     * 
     * @return A URI with no empty authority in its scheme specific part.
     */
    static URI fix(URI uri) {
        return !uri.isOpaque()
                && null == uri.getAuthority()
                && uri.getSchemeSpecificPart().startsWith(SEPARATOR + SEPARATOR + SEPARATOR)
                    ? new UriBuilder(uri).toUri()
                    : uri;
    }

    @Override
    public TFileSystem newFileSystem(   final Path path,
                                        final Map<String, ?> env) {
        final URI pnu = fix(path.normalize().toUri());
        final FsMountPoint fspmp = getMountPoint();
        final URI enu = fspmp.toUri().relativize(pnu);
        if (enu == pnu)
            throw new UnsupportedOperationException("path not relative to mount point");
        final FsMountPoint fsmp = new Scanner(getArchiveDetector(env))
                .scan(enu)
                .getMountPoint();
        if (fspmp.equals(fsmp))
            throw new UnsupportedOperationException("no prospective archive file detected"); // don't be greedy!
        return new TFileSystem(this, fsmp);
    }

    private FsPath toFsPath(URI uri, @CheckForNull Map<String, ?> env) {
        return new Scanner(getArchiveDetector(env)).scan(uri.normalize());
    }

    /**
     * {@inheritDoc}
     * 
     * @param env If {@code null} or does not contain a {@link TArchiveDetector}
     *        for the key {@link Parameter#ARCHIVE_DETECTOR}, then
     *        {@link TArchiveDetector#ALL} is used to detect prospective
     *        archive files.
     */
    @Override
    public TFileSystem newFileSystem(URI uri, @CheckForNull final Map<String, ?> env) {
        return new TFileSystem(this, toFsPath(uri, env).getMountPoint());
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

    private static TArchiveDetector getArchiveDetector(@CheckForNull Map<String, ?> env) {
        if (null == env)
            return TArchiveDetector.ALL;
        TArchiveDetector detector = (TArchiveDetector) env.get(
                Parameter.ARCHIVE_DETECTOR);
        return null != detector ? detector : TArchiveDetector.ALL;
    }

    private final class Scanner {
        final TArchiveDetector detector;
        final Splitter splitter = new Splitter(SEPARATOR_CHAR, false);
        final UriBuilder uri = new UriBuilder();

        Scanner(TArchiveDetector detector) {
            this.detector = detector;
        }

        FsPath scan(final URI uri) {
            assert !uri.isOpaque();
            final String p = uri.getPath();
            this.uri.path(p).query(uri.getQuery());
            return scan(p);
        }

        FsPath scan(final String input) {
            splitter.split(input);
            final String parent = splitter.getParentPath();
            final FsEntryName member = FsEntryName.create(
                    uri.path(splitter.getMemberName()).toUri());
            if (null == parent)
                return new FsPath(getMountPoint(), member);
            FsPath path = scan(parent).resolve(member);
            final FsScheme scheme = detector.getScheme(member.toString());
            if (null != scheme)
                path = new FsPath(FsMountPoint.create(scheme, path), ROOT);
            return path;
        }
    } // class Scanner

    @Override
    public TPath getPath(final URI uri) {
        final FsPath path = toFsPath(uri, null);
        return new TFileSystem(this, path.getMountPoint())
                .getPath(path.getEntryName());
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /*private IOPool<?> getPool() {
        final IOPool<?> pool = this.pool;
        return null != pool ? pool : (this.pool = IOPoolLocator.SINGLETON.get());
    }*/

    @Override
    public InputStream newInputStream(Path path, OpenOption... options)
    throws IOException {
        final BitField<FsInputOption> inputOptions;
        if (0 < options.length) {
            final EnumSet<FsInputOption> set = EnumSet.noneOf(FsInputOption.class);
            for (final OpenOption option : options) {
                if (!(option instanceof StandardOpenOption))
                    throw new UnsupportedOperationException(option.toString());
                switch ((StandardOpenOption) option) {
                    case READ:
                        break;
                    default:
                        throw new IllegalArgumentException(option.toString());
                }
            }
            inputOptions = BitField.copyOf(set);
        } else {
            inputOptions = NO_INPUT_OPTION;
        }
        return toTPath(path)
                .getInputSocket(inputOptions)
                .newInputStream();
    }

    @Override
    public OutputStream newOutputStream(Path path, OpenOption... options)
    throws IOException {
        final BitField<FsOutputOption> outputOptions;
        if (0 < options.length) {
            final EnumSet<FsOutputOption> set = EnumSet.noneOf(FsOutputOption.class);
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
                        set.add(FsOutputOption.APPEND);
                        break;
                    case CREATE_NEW:
                        // Note that EXCLUSIVE is usually NOT atomic.
                        set.add(FsOutputOption.EXCLUSIVE);
                        break;
                    default:
                        throw new UnsupportedOperationException(option.toString());
                }
            }
            outputOptions = BitField.copyOf(set);
        } else {
            outputOptions = NO_OUTPUT_OPTION;
        }
        return toTPath(path)
                .getOutputSocket(outputOptions, null)
                .newOutputStream();
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter)
    throws IOException {
        return toTPath(dir).newDirectoryStream(filter);
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        toTPath(dir).createDirectory(attrs);
    }

    @Override
    public void delete(Path path) throws IOException {
        delete0(toTPath(path));
    }

    private void delete0(TPath path) throws IOException {
        toTPath(path).delete();
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options)
    throws IOException {
        copy0(toTPath(source), toTPath(target), options);
    }

    private void copy0( final TPath source,
                        final TPath target,
                        final CopyOption... options)
    throws IOException {
        checkContains(source, target);
        boolean preserve = false;
        if (0 < options.length) {
            for (final CopyOption option : options) {
                if (!(option instanceof StandardCopyOption))
                    throw new UnsupportedOperationException(option.toString());
                switch ((StandardCopyOption) option) {
                    case REPLACE_EXISTING:
                        break;
                    case COPY_ATTRIBUTES:
                        preserve = true;
                        break;
                    default:
                        throw new UnsupportedOperationException(option.toString());
                }
            }
        }
        final InputSocket<?> input = source.getInputSocket(NO_INPUT_OPTION);
        final OutputSocket<?> output = target.getOutputSocket(NO_OUTPUT_OPTION,
                preserve ? input.getLocalTarget() : null);
        IOSocket.copy(input, output);
    }

    @Override
    public void move(Path source, Path target, CopyOption... options)
    throws IOException {
        TPath s = toTPath(source);
        TPath t = toTPath(target);
        if (null != t.getEntry())
            throw new FileAlreadyExistsException(target.toString());
        copy0(s, t, StandardCopyOption.COPY_ATTRIBUTES);
        delete0(s);
    }

    private static void checkContains(TPath a, TPath b) throws IOException {
        // toAbsolutePath() is redundant, but kept for comprehensibility.
        URI ua = a.toAbsolutePath().toPath().toHierarchicalUri();
        URI ub = b.toAbsolutePath().toPath().toHierarchicalUri();
        if (ua.resolve(ub) != ub)
            throw new IOException(b + " (contained in " + a + ")");
    }

    @Override
    public boolean isSameFile(Path a, Path b) throws IOException {
        // toAbsolutePath() is redundant, but kept for comprehensibility.
        URI ua = toTPath(a).toAbsolutePath().toPath().toHierarchicalUri();
        URI ub = toTPath(b).toAbsolutePath().toPath().toHierarchicalUri();
        return ua.equals(ub);
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        return toTPath(path).getFileName().startsWith(".");
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        final TPath p = toTPath(path);
        final FsEntryName n = p.toPath().getEntryName();
        final FsController<?> c = p.getFileSystem().getController();
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
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        throw new UnsupportedOperationException("Not supported yet.");
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
        return (A) readAttributes0(toTPath(path));
    }

    private BasicFileAttributes readAttributes0(final TPath path)
    throws IOException {
        final FsEntry e = path.getEntry();

        class Attributes implements BasicFileAttributes {
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
        }

        return new Attributes();
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static TPath toTPath(Path path) {
        /*if (path == null)
            throw new NullPointerException();
        if (!(path instanceof TPath))
            throw new ProviderMismatchException();*/
        return (TPath) path;
    }

    /** Keys for environment maps. */
    public interface Parameter {
        /** The key for the {@link TArchiveDetector} parameter. */
        String ARCHIVE_DETECTOR = "ARCHIVE_DETECTOR";
    }
}
