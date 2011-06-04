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

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsInputOptions;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import de.schlichtherle.truezip.fs.FsMountPoint;
import de.schlichtherle.truezip.fs.FsOutputOptions;
import de.schlichtherle.truezip.fs.FsPath;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.io.Paths.Splitter;
import de.schlichtherle.truezip.util.UriBuilder;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderMismatchException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
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

    @Override
    public TFileSystem newFileSystem(   final Path path,
                                        final Map<String, ?> environment) {
        final URI uri = path.normalize().toUri();
        final FsMountPoint inMp = getMountPoint();
        if (!uri.toString().startsWith(inMp.toString()))
            throw new UnsupportedOperationException();
        if (uri.isOpaque())
            throw new UnsupportedOperationException();
        final FsMountPoint outMp = new Scanner(getArchiveDetector(environment))
                .scan(uri)
                .getMountPoint();
        if (inMp.equals(outMp))
            throw new UnsupportedOperationException(); // don't be greedy!
        return new TFileSystem(this, outMp);
    }

    private final class Scanner {
        final TArchiveDetector detector;
        final Splitter splitter = new Splitter(SEPARATOR_CHAR, false);
        final UriBuilder uri = new UriBuilder();
        final String mpusspop = getSspOrPath(getMountPoint().toUri());

        Scanner(TArchiveDetector detector) {
            this.detector = detector;
        }

        FsPath scan(final URI uri) {
            this.uri.path(getSspOrPath(uri)).query(uri.getQuery());
            return scan(uri.getPath());
        }

        FsPath scan(final String input) {
            splitter.split(input);
            final String parent = splitter.getParentPath();
            final FsEntryName member = FsEntryName.create(
                    uri.path(splitter.getMemberName()).toUri());
            if (null == parent || mpusspop.startsWith(parent))
                return new FsPath(getMountPoint(), member);
            FsPath path = scan(parent).resolve(member);
            final FsScheme scheme = detector.getScheme(member.toString());
            if (null != scheme)
                path = new FsPath(FsMountPoint.create(scheme, path), ROOT);
            return path;
        }
    } // class Scanner

    private static String getSspOrPath(URI uri) {
        return uri.isOpaque() ? uri.getSchemeSpecificPart() : uri.getPath();
    }

    private static TArchiveDetector getArchiveDetector(Map<String, ?> env) {
        TArchiveDetector detector = (TArchiveDetector) env.get(
                Parameter.ARCHIVE_DETECTOR);
        return null != detector ? detector : TArchiveDetector.ALL;
    }

    @Override
    public TFileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TFileSystem getFileSystem(URI uri) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TPath getPath(URI uri) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public InputStream newInputStream(Path path, OpenOption... options)
    throws IOException {
        return toPath(path).newInputStream(FsInputOptions.NO_INPUT_OPTION);
    }

    @Override
    public OutputStream newOutputStream(Path path, OpenOption... options)
    throws IOException {
        return toPath(path).newOutputStream(FsOutputOptions.NO_OUTPUT_OPTION, null);
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void delete(Path path) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static TPath toPath(Path path) {
        if (path == null)
            throw new NullPointerException();
        if (!(path instanceof TPath))
            throw new ProviderMismatchException();
        return (TPath) path;
    }

    public interface Parameter {
        String ARCHIVE_DETECTOR = "ARCHIVE_DETECTOR";
    }

    public static final class File extends TFileSystemProvider {
        public File() {
            super(  FsScheme.create("tzp"),
                    FsMountPoint.create(Paths.get("/").toUri()));
        }
    }
}
