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

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsEntry;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import de.schlichtherle.truezip.fs.FsInputOption;
import de.schlichtherle.truezip.fs.FsMountPoint;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.fs.FsPath;
import static de.schlichtherle.truezip.io.Paths.*;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.UriBuilder;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileAttribute;
import java.util.Iterator;
import net.jcip.annotations.Immutable;

/**
 * A {@link Path} implementation based on the TrueZIP Kernel module.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
@edu.umd.cs.findbugs.annotations.SuppressWarnings("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
public final class TPath implements Path {

    private final TArchiveDetector detector;
    private final URI uri;
    private volatile @CheckForNull FsPath address;
    private volatile @CheckForNull TFileSystem fileSystem;
    private volatile @CheckForNull Integer hashCode;

    public TPath(String first, String... more) {
        this((TArchiveDetector) null, first, more);
    }

    public TPath(final @CheckForNull TArchiveDetector detector, final String first, final String... more) {
        this.detector = null != detector ? detector : getDefaultArchiveDetector();
        this.uri = toUri(first, more);

        assert invariants();
    }

    TPath(final FsMountPoint mountPoint, final String first, final String... more) {
        this.detector = getDefaultArchiveDetector();
        this.uri = toUri(first, more);
        this.address = new TScanner(detector).toPath(mountPoint, uri);

        assert invariants();
    }

    TPath(URI uri) {
        this(null, uri);
    }

    TPath(final @CheckForNull TArchiveDetector detector, final URI uri) {
        this.detector = null != detector ? detector : getDefaultArchiveDetector();
        String p = uri.getRawPath(), q = p;
        p = cutTrailingSeparators(p, SEPARATOR_CHAR);
        this.uri = p == q
                ? uri
                : new UriBuilder(uri, true).path(p).toUri();

        assert invariants();
    }

    private TPath(
            final TArchiveDetector detector,
            final URI uri,
            final @CheckForNull FsPath address) {
        this.detector = detector;
        this.uri = uri;
        this.address = address;

        assert invariants();
    }

    TPath(Path path) {
        this(null, path);
    }

    TPath(final @CheckForNull TArchiveDetector detector, final Path path) {
        this.detector = null != detector ? detector : getDefaultArchiveDetector();
        this.uri = new UriBuilder().path(path.toString().replace(path.getFileSystem().getSeparator(), SEPARATOR)).toUri();
    }

    private static URI toUri(final String first, final String... more) {
        final StringBuilder path;
        {
            int l = first.length();
            for (String m : more)
                l += 1 + m.length();
            path = new StringBuilder(l);
        }
        int i = -1;
        {
            String s = cutTrailingSeparators(
                    first.replace(File.separatorChar, SEPARATOR_CHAR),
                    SEPARATOR_CHAR);
            int l = s.length();
            for (int k = 0; k < l; k++) {
                char c = s.charAt(k);
                if (SEPARATOR_CHAR != c
                        || i <= 0
                        || 0 < i && SEPARATOR_CHAR != s.charAt(i)) {
                    path.append(c);
                    i++;
                }
            }
        }
        for (String s : more) {
            s = cutTrailingSeparators(
                    s.replace(File.separatorChar, SEPARATOR_CHAR),
                    SEPARATOR_CHAR);
            int l = s.length();
            for (int j = 0, k = 0; k < l; k++) {
                char c = s.charAt(k);
                if (SEPARATOR_CHAR != c
                        || 0 < j && 0 <= i && SEPARATOR_CHAR != s.charAt(i)) {
                    if (0 == j)
                        path.append(SEPARATOR);
                    path.append(c);
                    i++;
                    j++;
                }
            }
        }
        return 0 == path.length() || SEPARATOR_CHAR == path.charAt(0)
                ? URI.create(path.toString())
                : new UriBuilder().path(path.toString()).toUri();
    }

    /**
     * Checks the invariants of this class and throws an AssertionError if
     * any is violated even if assertion checking is disabled.
     * <p>
     * The constructors call this method like this:
     * <pre>{@code assert invariants();}</pre>
     * This calls the method if and only if assertions are enabled in order
     * to assert that the instance invariants are properly obeyed.
     * If assertions are disabled, the call to this method is thrown away by
     * the HotSpot compiler, so there is no performance penalty.
     *
     * @throws AssertionError If assertions are enabled and any invariant is
     *         violated.
     * @return {@code true}
     */
    private boolean invariants() {
        assert null != getArchiveDetector();
        assert null != getUri();
        assert null != getAddress();
        assert null != getFileSystem();
        return true;
    }

    /**
     * Returns the {@link TArchiveDetector} that was used to detect any archive
     * files in the path of this {@code TPath} object at construction time.
     * 
     * @return The {@link TArchiveDetector} that was used to detect any archive
     *         files in the path of this file object at construction time.
     */
    public TArchiveDetector getArchiveDetector() {
        return detector;
    }

    /**
     * Returns {@code true} if and only if this {@code TPath} addresses an
     * archive file.
     * Whether or not this is true solely depends on the
     * {@link TArchiveDetector} which was used to construct this {@code TPath}
     * - no file system tests are performed by this method!
     *
     * @return {@code true} if and only if this {@code TPath} addresses an
     *         archive file.
     * @see    #isEntry
     */
    public boolean isArchive() {
        final FsPath address = getAddress();
        final boolean root = address.getEntryName().isRoot();
        final FsMountPoint parent = address.getMountPoint().getParent();
        return root && null != parent;
    }

    /**
     * Returns {@code true} if and only if this {@code TPath} addresses an
     * entry located within an archive file.
     * Whether or not this is true solely depends on the
     * {@link TArchiveDetector} which was used to construct this {@code TPath}
     * - no file system tests are performed by this method!
     *
     * @return {@code true} if and only if this {@code TPath} addresses an
     *         entry located within an archive file.
     * @see #isArchive
     */
    public boolean isEntry() {
        final FsPath address = getAddress();
        final boolean root = address.getEntryName().isRoot();
        final FsMountPoint parent = address.getMountPoint().getParent();
        return !root    ? null != parent
                        : null != parent && null != parent.getParent();
    }

    /**
     * Returns the {@link TArchiveDetector} to use if no archive detector is
     * explicitly passed to the constructor of a {@code TPath} instance.
     * <p>
     * This class property is initially set to
     * {@link TArchiveDetector#ALL}
     *
     * @return The {@link TArchiveDetector} to use if no archive detector is
     *         explicitly passed to the constructor of a {@code TPath} instance.
     * @see #setDefaultArchiveDetector
     */
    public static TArchiveDetector getDefaultArchiveDetector() {
        return TFile.getDefaultArchiveDetector();
    }

    /**
     * Sets the {@link TArchiveDetector} to use if no archive detector is
     * explicitly passed to the constructor of a {@code TPath} instance.
     * When a new {@code TPath} instance is constructed, but no archive
     * detector is provided, then the value of this class property is used.
     * So changing the value of this class property affects only subsequently
     * constructed {@code TPath} instances - not any existing ones.
     *
     * @param detector the {@link TArchiveDetector} to use for subsequently
     *        constructed {@code TPath} instances if no archive detector is
     *        explicitly provided to the constructor
     * @see   #getDefaultArchiveDetector()
     */
    public static void setDefaultArchiveDetector(TArchiveDetector detector) {
        TFile.setDefaultArchiveDetector(detector);
    }

    URI getUri() {
        return this.uri;
    }

    FsPath getAddress() {
        final FsPath address = this.address;
        return null != address
                ? address
                : (this.address = toFsPath(getUri()));
    }

    private FsPath toFsPath(URI uri) {
        return new TScanner(detector).toPath(
                uri.isAbsolute() || null != uri.getAuthority() || uri.getPath().startsWith(SEPARATOR)
                    ? TFileSystemProvider.get(this).getRoot()
                    : TFileSystemProvider.get(this).getCurrent(),
                uri);
    }

    @Override
    public TFileSystem getFileSystem() {
        final TFileSystem fileSystem = this.fileSystem;
        return null != fileSystem
                ? fileSystem
                : (this.fileSystem = TFileSystem.get(this));
    }

    @Override
    public boolean isAbsolute() {
        final URI uri = getUri();
        return uri.isAbsolute()
                || uri.getSchemeSpecificPart().startsWith(SEPARATOR);
    }

    @Override
    public @Nullable TPath getRoot() {
        return new TPath(getArchiveDetector(), toUri().resolve(SEPARATOR), null); // don't use getAddress()!
    }

    @Override
    public TPath getFileName() {
        final URI uri = getUri();
        final URI parent = uri.resolve(".");
        final URI member = parent.relativize(uri);
        return new TPath(getArchiveDetector(), member, null); // don't use getAddress()!
    }

    @Override
    public TPath getParent() {
        return new TPath(getArchiveDetector(), getUri().resolve("."));
    }

    @Override
    public int getNameCount() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TPath getName(int index) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TPath subpath(int beginIndex, int endIndex) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public boolean startsWith(Path that) {
        if (this.getFileSystem() != that.getFileSystem())
            return false;
        return startsWith(((TPath) that).getAddress().getEntryName().toString());
    }

    @Override
    public boolean startsWith(String other) {
        final String name = this.getAddress().getEntryName().toString();
        final int ol = other.length();
        return name.startsWith(other)
                && (name.length() == ol
                    || SEPARATOR_CHAR == name.charAt(ol));
    }

    @Override
    public boolean endsWith(Path that) {
        if (this.getFileSystem() != that.getFileSystem())
            return false;
        return endsWith(((TPath) that).getAddress().getEntryName().toString());
    }

    @Override
    public boolean endsWith(String other) {
        final String name = this.getAddress().getEntryName().toString();
        final int ol = other.length(), tl;
        return name.endsWith(other)
                && ((tl = name.length()) == ol
                    || SEPARATOR_CHAR == name.charAt(tl - ol));
    }

    @Override
    public TPath normalize() {
        return new TPath(getArchiveDetector(), getUri().normalize(), address); // don't use getAddress()!
    }

    @Override
    public TPath resolve(final Path member) {
        return resolve(
                member instanceof TPath
                    ? ((TPath) member).getArchiveDetector()
                    : getArchiveDetector(),
                member  .toString()
                        .replace(   member.getFileSystem().getSeparator(),
                                    SEPARATOR));
    }

    @Override
    public TPath resolve(final String member) {
        return resolve(getArchiveDetector(), member);
    }

    private TPath resolve(final TArchiveDetector detector, final String member) {
        URI u = getUri();
        final String up = u.getPath();
        try {
            u = up.isEmpty()
                    ? new UriBuilder().path(member).getUri()
                    : up.endsWith(SEPARATOR)
                        ? u.resolve(member)
                        : member.isEmpty()
                            ? u
                            : new UriBuilder()
                                .path(up + SEPARATOR_CHAR)
                                .getUri()
                                .resolve(member);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
        return new TPath(detector, u);
    }

    @Override
    public TPath resolveSibling(Path other) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TPath resolveSibling(String other) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TPath relativize(Path other) {
        return new TPath(getArchiveDetector(), toUri().relativize(other.toUri()));
    }

    @Override
    public TFile toFile() {
        final URI uri = getUri();
        return uri.isAbsolute()
                ? new TFile(getAddress())
                : new TFile(uri.getSchemeSpecificPart(), getArchiveDetector());
    }

    @Override
    public URI toUri() {
        return getAddress().toHierarchicalUri();
    }

    @Override
    public TPath toAbsolutePath() {
        return new TPath(getArchiveDetector(), toUri(), address); // don't use getAddress()!
    }

    @Override
    public TPath toRealPath(LinkOption... options) throws IOException {
        // FIXME: resolve symlinks!
        return new TPath(getArchiveDetector(), toUri(), address); // don't use getAddress()!
    }

    @Override
    public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public WatchKey register(WatchService watcher, Kind<?>... events) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Iterator<Path> iterator() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int compareTo(Path that) {
        return this.toUri().compareTo(that.toUri());
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other)
            return true;
        if (!(other instanceof Path))
            return false;
        final Path that = (Path) other;
        return this.getFileSystem().equals(that.getFileSystem())
                && this.toUri().equals(that.toUri());
    }

    @Override
    public int hashCode() {
        final Integer hashCode = this.hashCode;
        if (null != hashCode)
            return hashCode;
        int result = 17;
        result = 37 * result + getFileSystem().hashCode();
        result = 37 * result + toUri().hashCode();
        return this.hashCode = result;
    }

    @Override
    public String toString() {
        return getUri().toString();
    }

    FsController<?> getController() throws IOException {
        return getFileSystem().getController();
    }

    FsEntry getEntry() throws IOException {
        return getFileSystem().getEntry(this);
    }

    public InputSocket<?> getInputSocket(BitField<FsInputOption> options) {
        return getFileSystem().getInputSocket(this, options);
    }

    public OutputSocket<?> getOutputSocket(BitField<FsOutputOption> options,
                                    @CheckForNull Entry template) {
        return getFileSystem().getOutputSocket(this, options, template);
    }

    DirectoryStream<Path> newDirectoryStream(Filter<? super Path> filter)
    throws IOException {
        return getFileSystem().newDirectoryStream(this, filter);
    }

    void createDirectory(FileAttribute<?>... attrs) throws IOException {
        getFileSystem().createDirectory(this, attrs);
    }

    void delete() throws IOException {
        getFileSystem().delete(this);
    }
}
