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

import static de.schlichtherle.truezip.nio.file.TUriScanner.*;
import de.schlichtherle.truezip.file.TConfig;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.fs.FsEntry;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import de.schlichtherle.truezip.fs.FsInputOption;
import de.schlichtherle.truezip.fs.FsMountPoint;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.fs.FsPath;
import de.schlichtherle.truezip.io.Paths;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.QuotedInputUriSyntaxException;
import de.schlichtherle.truezip.util.UriBuilder;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.File;
import static java.io.File.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.jcip.annotations.Immutable;

/**
 * A {@link Path} implementation
 * based on the TrueZIP Kernel module.
 * <p>
 * Note that objects of this class are immutable and inherently volatile
 * because all virtual file system state is managed by the TrueZIP Kernel
 * module.
 * As a consequence, you should never use object identity ('==') to test for
 * equality of objects of this class with another object, but instead use the
 * method {@link #equals(Object)}.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@edu.umd.cs.findbugs.annotations.SuppressWarnings("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
@DefaultAnnotation(NonNull.class)
public final class TPath implements Path {

    private final URI uri;
    private final TArchiveDetector detector;
    private volatile @CheckForNull FsPath address;
    private volatile @CheckForNull TFileSystem fileSystem;
    private volatile @CheckForNull Integer hashCode;
    private volatile @CheckForNull List<String> segments;

    TPath(Path path) {
        this(   uri(path.toString().replace(
                    path.getFileSystem().getSeparator(),
                    separator)),
                null,
                null);
    }

    public TPath(String first, String... more) {
        this(uri(first, more), null, null);
    }

    TPath(URI uri) {
        this(uri, null, null);
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("ES_COMPARING_STRINGS_WITH_EQ")
    private TPath(
            final URI uri,
            final @CheckForNull TArchiveDetector detector,
            final @CheckForNull FsPath address) {
        this.uri = fix(uri);
        this.detector = null != detector ? detector : TConfig.get().getArchiveDetector();
        this.address = address;

        assert invariants();
    }

    TPath(final TFileSystem fileSystem, String first, final String... more) {
        this.uri = uri(cutLeadingSeparators(first), more);
        final TArchiveDetector detector = TConfig.get().getArchiveDetector();
        this.detector = detector;
        this.address = new TUriScanner(detector)
                .resolve(new FsPath(fileSystem.getMountPoint(), ROOT), uri);

        assert invariants();
    }

    private static URI uri(final String first, final String... more) {
        final StringBuilder b;
        {
            int l = 1 + first.length(); // might prepend SEPARATOR
            for (String m : more)
                l += 1 + m.length(); // dito
            b = new StringBuilder(l);
        }
        int i = -1;
        {
            String s = first.replace(File.separatorChar, SEPARATOR_CHAR);
            int l = s.length();
            for (int k = 0; k < l; k++) {
                char c = s.charAt(k);
                if (SEPARATOR_CHAR != c
                        || i <= 0
                        || 0 < i && SEPARATOR_CHAR != b.charAt(i)) {
                    b.append(c);
                    i++;
                }
            }
        }
        for (String s : more) {
            s = s.replace(File.separatorChar, SEPARATOR_CHAR);
            int l = s.length();
            for (int j = 0, k = 0; k < l; k++) {
                char c = s.charAt(k);
                final boolean n = SEPARATOR_CHAR != c;
                final boolean o = 0 <= i && SEPARATOR_CHAR != b.charAt(i);
                if (n || o) {
                    if (0 == j && n && o)
                        b.append(SEPARATOR_CHAR);
                    b.append(c);
                    i++;
                    j++;
                }
            }
        }
        String p = b.toString();
        final int l = prefixLength(p);
        p = cutTrailingSeparators(p, l);
        try {
            if (0 < l) {
                if (SEPARATOR_CHAR != p.charAt(l - 1))
                    throw new QuotedInputUriSyntaxException(p, "Relative path with non-empty prefix.");
                if (SEPARATOR_CHAR == p.charAt(0))
                    return new URI(p); // may parse authority
            }
            return new UriBuilder().path(p).getUri();
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("ES_COMPARING_STRINGS_WITH_EQ")
    private static URI fix(URI uri) {
        if (uri.isOpaque())
            throw new IllegalArgumentException(
                    new QuotedInputUriSyntaxException(uri, "Opaque URI"));
        uri = TUriScanner.fix(uri);
        final int pl = pathPrefixLength(uri);
        String p = uri.getPath();
        final String q = p;
        p = cutTrailingSeparators(p, pl);
        if (p != q) // mind contract of cutTrailingSeparators(String, int)
            return new UriBuilder(uri).path(p).toUri();
        return uri;
    }

    private static String cutLeadingSeparators(final String p) {
        int i = 0;
        while (SEPARATOR_CHAR == p.charAt(i))
            i++;
        return 0 == i ? p : p.substring(i);
    }

    static String cutTrailingSeparators(
            final String p,
            final int o) {
        int i = p.length();
        if (o >= i || SEPARATOR_CHAR != p.charAt(--i))
            return p;
        while (o <= i && SEPARATOR_CHAR == p.charAt(--i)) {
        }
        return p.substring(0, ++i);
    }

    private static int prefixLength(String p) {
        return Paths.prefixLength(p, SEPARATOR_CHAR, true);
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
     * <p>
     * If assertions are enabled however, a call to this method will initialize
     * all volatile fields as a side effect.
     *
     * @throws AssertionError If assertions are enabled and any invariant is
     *         violated.
     * @return {@code true}
     */
    private boolean invariants() {
        assert null != getArchiveDetector();
        assert null != getUri();
        assert null != getAddress();
        assert getAddress().toUri().isAbsolute();
        assert null != getFileSystem();
        return true;
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

    TArchiveDetector getArchiveDetector() {
        return detector;
    }

    URI getUri() {
        return this.uri;
    }

    /**
     * Returns an {@link FsPath} for this path with an absolute URI.
     * 
     * @return An {@link FsPath} for this path with an absolute URI.
     */
    FsPath getAddress() {
        final FsPath addr = this.address;
        return null != addr ? addr : (this.address = newAddress());
    }

    private FsPath newAddress() {
        final URI uri = getUri();
        return new TUriScanner(getArchiveDetector()).resolve(
                isAbsolute(uri)
                    ? TFileSystemProvider.get(this).getRoot()
                    : TFileSystemProvider.get(this).getCurrent(),
                uri);
    }

    @Override
    public TFileSystem getFileSystem() {
        final TFileSystem fs = this.fileSystem;
        return null != fs ? fs : (this.fileSystem
                = TFileSystemProvider.get(this).getFileSystem(this));
    }

    @Override
    public boolean isAbsolute() {
        return isAbsolute(getUri());
    }

    private static boolean isAbsolute(URI uri) {
        return uri.isAbsolute()
                || Paths.isAbsolute(uri.getSchemeSpecificPart(), SEPARATOR_CHAR);
    }

    @Override
    public @Nullable TPath getRoot() {
        final String s = getUri().getSchemeSpecificPart();
        final int l = prefixLength(s);
        if (0 >= l || SEPARATOR_CHAR != s.charAt(l - 1))
            return null;
        return new TPath(
                uri(s.substring(0, l)),
                getArchiveDetector(),
                null);
    }

    @Override
    public TPath getFileName() {
        final URI uri = getUri();
        final URI parent = uri.resolve(".");
        final URI member = parent.relativize(uri);
        return new TPath(member, getArchiveDetector(), null);
    }

    @Override
    public TPath getParent() {
        final URI parent = getUri().resolve(".");
        if (parent.getRawPath().isEmpty())
            return null;
        try {
            return new TPath(
                    parent,
                    getArchiveDetector(),
                    TUriScanner.parent(getAddress()));
        } catch (URISyntaxException ex) {
            throw new AssertionError(ex);
        }
    }

    private List<String> getSegments() {
        final List<String> segments = this.segments;
        if (null != segments)
            return segments;
        final String ssp = getUri().getSchemeSpecificPart();
        final String[] a = ssp.substring(prefixLength(ssp)).split(SEPARATOR);
        int i = 0;
        for (String s : a)
            if (!s.isEmpty())
                a[i++] = s;
        return this.segments = //Collections.unmodifiableList(
                Arrays.asList(a).subList(0, i);
    }

    @Override
    public int getNameCount() {
        return getSegments().size();
    }

    @Override
    public TPath getName(int index) {
        return new TPath(getSegments().get(index));
    }

    @Override
    public TPath subpath(final int beginIndex, final int endIndex) {
        final List<String> segments = getSegments();
        final String first = segments.get(beginIndex);
        final String[] more = new String[endIndex - beginIndex - 1];
        return new TPath(
                first,
                segments.subList(beginIndex + 1, endIndex).toArray(more));
    }

    @Override
    public boolean startsWith(Path that) {
        if (!this.getFileSystem().equals(that.getFileSystem()))
            return false;
        return startsWith(that.toString());
    }

    @Override
    public boolean startsWith(String other) {
        final String name = toString();
        final int ol = other.length();
        return name.startsWith(other)
                && (name.length() == ol
                    || separatorChar == name.charAt(ol));
    }

    @Override
    public boolean endsWith(Path that) {
        if (!this.getFileSystem().equals(that.getFileSystem()))
            return false;
        return endsWith(that.toString());
    }

    @Override
    public boolean endsWith(String other) {
        final String name = toString();
        final int ol = other.length(), tl;
        return name.endsWith(other)
                && ((tl = name.length()) == ol
                    || separatorChar == name.charAt(tl - ol));
    }

    @Override
    public TPath normalize() {
        return new TPath(getUri().normalize(), getArchiveDetector(), address); // don't use getAddress()!
    }

    @Override
    public TPath resolve(Path member) {
        return member instanceof TPath
                ? resolve(((TPath) member).getUri())
                : resolve(member.toString().replace(
                    member.getFileSystem().getSeparator(),
                    separator));
    }

    @Override
    public TPath resolve(String member) {
        return resolve(uri(member));
    }

    private TPath resolve(final URI member) {
        URI u = getUri();
        final String up = u.getPath();
        try {
            u = up.isEmpty()
                    ? member
                    : up.endsWith(SEPARATOR)
                        ? u.resolve(member)
                        : member.getRawPath().isEmpty()
                                && null == member.getRawQuery()
                            ? u
                            : new UriBuilder()
                                .path(up + SEPARATOR_CHAR)
                                .getUri()
                                .resolve(member);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
        final TArchiveDetector d = TConfig.get().getArchiveDetector();
        return new TPath(u, d, new TUriScanner(d).resolve(
                isAbsolute(member)
                    ? TFileSystemProvider.get(this).getRoot()
                    : getAddress(),
                member));
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
        return new TPath(toUri().relativize(other.toUri()));
    }

    @Override
    public TFile toFile() {
        final URI uri = getUri();
        return uri.isAbsolute()
                ? new TFile(    getAddress())
                : new TFile(    uri.getSchemeSpecificPart(),
                                getArchiveDetector());
    }

    @Override
    public URI toUri() {
        return new UriBuilder(getAddress().toHierarchicalUri())
                .scheme(TFileSystemProvider.get(this).getScheme())
                .toUri();
    }

    @Override
    public TPath toAbsolutePath() {
        return new TPath(toUri(), getArchiveDetector(), address); // don't use getAddress()!
    }

    @Override
    public TPath toRealPath(LinkOption... options) throws IOException {
        // FIXME: resolve symlinks!
        return new TPath(toUri(), getArchiveDetector(), address); // don't use getAddress()!
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
        return new SegmentIterator(this);
    }

    private static final class SegmentIterator implements Iterator<Path> {
        final Iterator<String> i;
        
        SegmentIterator(TPath path) {
            this.i = path.getSegments().iterator();
        }

        @Override
        public boolean hasNext() {
            return i.hasNext();
        }

        @Override
        public Path next() {
            return new TPath(i.next());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public int compareTo(Path that) {
        return this.toString().compareTo(that.toString());
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other)
            return true;
        if (!(other instanceof Path))
            return false;
        final Path that = (Path) other;
        return this.getFileSystem().equals(that.getFileSystem())
                && this.toString().equals(that.toString());
    }

    @Override
    public int hashCode() {
        final Integer hashCode = this.hashCode;
        if (null != hashCode)
            return hashCode;
        int result = 17;
        result = 37 * result + getFileSystem().hashCode();
        result = 37 * result + toString().hashCode();
        return this.hashCode = result;
    }

    @Override
    public String toString() {
        return getUri()
                .getSchemeSpecificPart()
                .replace(SEPARATOR_CHAR, separatorChar);
    }

    SeekableByteChannel newByteChannel(
            Set<? extends OpenOption> options,
            FileAttribute<?>... attrs)
    throws IOException {
        return getFileSystem().newByteChannel(this, options, attrs);
    }

    InputStream newInputStream(OpenOption... options)
    throws IOException {
        return getFileSystem().newInputStream(this, options);
    }

    OutputStream newOutputStream(OpenOption... options)
    throws IOException {
        return getFileSystem().newOutputStream(this, options);
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

    FsEntry getEntry() throws IOException {
        return getFileSystem().getEntry(this);
    }

    InputSocket<?> getInputSocket(BitField<FsInputOption> options) {
        return getFileSystem().getInputSocket(this, options);
    }

    OutputSocket<?> getOutputSocket(BitField<FsOutputOption> options,
                                    @CheckForNull Entry template) {
        return getFileSystem().getOutputSocket(this, options, template);
    }

    void checkAccess(AccessMode... modes) throws IOException {
        getFileSystem().checkAccess(this, modes);
    }

    @Nullable
    <V extends FileAttributeView> V getFileAttributeView(
            Class<V> type,
            LinkOption... options) {
        return getFileSystem().getFileAttributeView(this, type, options);
    }

    <A extends BasicFileAttributes> A readAttributes(
            Class<A> type,
            LinkOption... options)
    throws IOException {
        return getFileSystem().readAttributes(this, type, options);
    }
}
