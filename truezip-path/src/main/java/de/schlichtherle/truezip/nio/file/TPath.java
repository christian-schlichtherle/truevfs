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

import static de.schlichtherle.truezip.nio.file.TPathScanner.*;
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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.jcip.annotations.Immutable;

/**
 * A {@link Path} implementation
 * based on the TrueZIP Kernel module.
 * <p>
 * Unless explicitly specified, you should <em>not</em> assume that calling
 * the same method on an instance of this class multiple times returns
 * identical objects.
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

    private static final TPathComparator COMPARATOR = '\\' == separatorChar
            ? new WindowsTPathComparator()
            : new TPathComparator();

    private final URI name;
    private final TArchiveDetector detector;
    private final FsPath address;
    private volatile @CheckForNull TFileSystem fileSystem;
    private volatile @CheckForNull String string;
    private volatile @CheckForNull Integer hashCode;
    private volatile @CheckForNull List<String> segments;

    /**
     * Constructs a new path from the given path.
     * 
     * @param path a path.
     */
    public TPath(Path path) {
        this(   path instanceof TPath
                    ? ((TPath) path).getName()
                    : name(path.toString().replace(
                        path.getFileSystem().getSeparator(),
                        separator)),
                null,
                null);
    }

    /**
     * Constructs a new path from the given file.
     * 
     * @param file a file.
     *        If this is an instance of {@link TFile}, its
     *        {@link TFile#getArchiveDetector() archive detector} and
     *        {@link TFile#toFsPath() address} get shared with this instance.
     */
    public TPath(File file) {
        final URI name = name(file.getPath());
        this.name = name;
        if (file instanceof TFile) {
            final TFile f = (TFile) file;
            this.detector = f.getArchiveDetector();
            this.address = f.toFsPath();
        } else {
            final TArchiveDetector detector = getDefaultArchiveDetector();
            this.detector = detector;
            this.address = address(name, detector);
        }

        assert invariants();
    }

    /**
     * Constructs a new path from the given sub path strings.
     * The supported separators are {@link File#separator} and {@code "/"}.
     * 
     * @param first the first sub path string.
     * @param more optional sub path strings.
     */
    public TPath(String first, String... more) {
        this(name(first, more), null, null);
    }

    TPath(URI uri) {
        this(uri, null, null);
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("ES_COMPARING_STRINGS_WITH_EQ")
    private TPath(
            URI uri,
            @CheckForNull TArchiveDetector detector,
            final @CheckForNull FsPath address) {
        this.name = uri = name(uri);
        if (null == detector)
            detector = getDefaultArchiveDetector();
        this.detector = detector;
        this.address = null != address ? address : address(uri, detector);

        assert invariants();
    }

    TPath(final TFileSystem fileSystem, String first, final String... more) {
        this.name = name(cutLeadingSeparators(first), more);
        final TArchiveDetector detector = getDefaultArchiveDetector();
        this.detector = detector;
        this.address = new TPathScanner(detector)
                .scan(new FsPath(fileSystem.getMountPoint(), ROOT), name);

        assert invariants();
    }

    private static URI name(final String first, final String... more) {
        final StringBuilder b;
        {
            int l = 1 + first.length(); // might prepend SEPARATOR
            for (String m : more)
                l += 1 + m.length(); // dito
            b = new StringBuilder(l);
        }
        int i = -1;
        {
            String s = first.replace(separatorChar, SEPARATOR_CHAR);
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
            s = s.replace(separatorChar, SEPARATOR_CHAR);
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
    private static URI name(URI uri) {
        try {
            uri = checkFix(uri);
            final int pl = pathPrefixLength(uri);
            final String q = uri.getPath();
            final String p = cutTrailingSeparators(q, pl);
            if (p != q) // mind contract of cutTrailingSeparators(String, int)
                return new UriBuilder(uri).path(p).getUri();
            return uri;
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
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

    private static FsPath address(URI name, TArchiveDetector detector) {
        return new TPathScanner(detector).scan(
                TFileSystemProvider.get(name).getRoot(),
                name);
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
        assert null != getName();
        assert null != getArchiveDetector();
        assert null != getAddress();
        return true;
    }

    /**
     * Equivalent to
     * {@link TConfig#getArchiveDetector TConfig.get().getArchiveDetector()}.
     */
    public static TArchiveDetector getDefaultArchiveDetector() {
        return TConfig.get().getArchiveDetector();
    }

    /**
     * Equivalent to
     * {@link TConfig#setArchiveDetector TConfig.get().setArchiveDetector(detector)}.
     */
    public static void setDefaultArchiveDetector(TArchiveDetector detector) {
        TConfig.get().setArchiveDetector(detector);
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
     * Returns the name of this path as a {@code URI}.
     * Multiple invocations of this method will return the same object.
     * 
     * @return the name of this path as a {@code URI}.
     */
    URI getName() {
        return this.name;
    }

    /**
     * Returns the {@code TArchiveDetector} used for scanning this path's
     * {@link #toString() name} for prospective archive files at construction
     * time.
     * Multiple invocations of this method will return the same object.
     * 
     * @return The {@code TArchiveDetector} used for scanning this path's
     *         {@link #toString() name} for prospective archive files at
     *         construction time.
     */
    TArchiveDetector getArchiveDetector() {
        return detector;
    }

    /**
     * Returns an {@link FsPath} for this path with an absolute URI.
     * 
     * @return An {@link FsPath} for this path with an absolute URI.
     */
    FsPath getAddress() {
        return this.address;
    }

    /**
     * Returns the {@code TFileSystem} for this path.
     * Multiple invocations of this method will return the same object.
     * 
     * @return the {@code TFileSystem} for this path.
     */
    @Override
    public TFileSystem getFileSystem() {
        final TFileSystem fs = this.fileSystem;
        return null != fs ? fs : (this.fileSystem = newFileSystem());
    }

    private TFileSystem newFileSystem() {
        return TFileSystemProvider.get(getName()).getFileSystem(this);
    }

    @Override
    public boolean isAbsolute() {
        return isAbsolute(getName());
    }

    private static boolean isAbsolute(URI uri) {
        return uri.isAbsolute()
                || Paths.isAbsolute(uri.getSchemeSpecificPart(), SEPARATOR_CHAR);
    }

    @Override
    public @Nullable TPath getRoot() {
        final String s = getName().getSchemeSpecificPart();
        final int l = prefixLength(s);
        if (0 >= l || SEPARATOR_CHAR != s.charAt(l - 1))
            return null;
        return new TPath(
                name(s.substring(0, l)),
                getArchiveDetector(),
                null);
    }

    @Override
    public TPath getFileName() {
        final URI uri = getName();
        final URI parent = uri.resolve(DOT);
        final URI member = parent.relativize(uri);
        return new TPath(member, getArchiveDetector(), null);
    }

    @Override
    public TPath getParent() {
        final URI n = getName();
        final int npl = n.getPath().length();
        final int nppl = pathPrefixLength(n);
        if (npl <= nppl)
            return null;
        final URI p = n.resolve(DOT);
        return p.getPath().isEmpty()
                ? null
                : new TPath(p, getArchiveDetector(), null);
    }

    /**
     * Returns the segments of this path's {@link #toString() name}.
     * Multiple invocations of this method will return the same object.
     * 
     * @return The segments of this path's {@link #toString() name}.
     */
    private List<String> getSegments() {
        final List<String> segments = this.segments;
        return null != segments ? segments : (this.segments = newSegments());
    }

    private List<String> newSegments() {
        final String ssp = getName().getSchemeSpecificPart();
        final String[] a = ssp.substring(prefixLength(ssp)).split(SEPARATOR);
        int i = 0;
        for (String s : a)
            if (!s.isEmpty())
                a[i++] = s;
        return //Collections.unmodifiableList(
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
        return new TPath(getName().normalize(), getArchiveDetector(), getAddress());
    }

    @Override
    public TPath resolve(Path other) {
        if (other instanceof TPath) {
            final TPath o = (TPath) other;
            if (o.isAbsolute())
                return o;
            if (o.toString().isEmpty())
                return this;
            return resolve(o.getName());
        } else {
            return resolve(other.toString().replace(
                    other.getFileSystem().getSeparator(),
                    separator));
        }
    }

    @Override
    public TPath resolve(String other) {
        return resolve(name(other));
    }

    private TPath resolve(final URI m) {
        URI n;
        final String np;
        if (isAbsolute(m) || (n = getName()).toString().isEmpty()) {
            n = m;
        } else if (m.toString().isEmpty()) {
            n = getName();
        } else if ((np = n.getPath()).endsWith(SEPARATOR)) {
            n = n.resolve(m);
        } else {
            try {
                n = new UriBuilder(n).path(np + SEPARATOR_CHAR).getUri().resolve(m);
            } catch (URISyntaxException ex) {
                throw new AssertionError(ex);
            }
        }
        final TArchiveDetector d = getDefaultArchiveDetector();
        final FsPath a = new TPathScanner(d).scan(
                isAbsolute(m)
                    ? TFileSystemProvider.get(getName()).getRoot()
                    : getAddress(),
                m);
        return new TPath(n, d, a);
    }

    @Override
    public TPath resolveSibling(final Path other) {
        if (!(other instanceof TPath))
            return resolveSibling(new TPath(other));
        final TPath o = (TPath) other;
        if (o.isAbsolute())
            return o;
        final TPath p = getParent();
        if (null == p)
            return o;
        if (o.toString().isEmpty())
            return p;
        return p.resolve(o);
    }

    @Override
    public TPath resolveSibling(String other) {
        return resolveSibling(new TPath(other));
    }

    @Override
    public TPath relativize(Path other) {
        return new TPath(toUri().relativize(other.toUri()));
    }

    @Override
    public URI toUri() {
        return new UriBuilder(getAddress().toHierarchicalUri())
                .scheme(TFileSystemProvider.get(getName()).getScheme())
                .toUri();
    }

    @Override
    public TPath toAbsolutePath() {
        return new TPath(toUri(), getArchiveDetector(), getAddress());
    }

    @Override
    public TPath toRealPath(LinkOption... options) throws IOException {
        // FIXME: scan symlinks!
        return new TPath(toUri(), getArchiveDetector(), getAddress());
    }

    /**
     * Returns a {@code TFile} object for this path.
     * If this path was constructed by the
     * {@link #TPath(File) file constructor}, then the returned {@code TFile}
     * object compares {@link TFile#equals(Object) equal} with this file
     * object, even if it was a plain {@link File} object.
     * However, the returned {@code TFile} object is <em>not</em> identical to
     * this object, even if it was a {@link TFile} object.
     * 
     * @return A {@code TFile} object for this path.
     * @throws UnsupportedOperationException if this path is not file based,
     *         i.e. if the scheme component of the {@link #toUri() URI} of
     *         this path is not {@code file}.
     */
    @Override
    public TFile toFile() {
        try {
            return getName().isAbsolute()
                    ? new TFile(getAddress())
                    : new TFile(toString(), getArchiveDetector());
        } catch (IllegalArgumentException ex) {
            throw new UnsupportedOperationException(ex);
        }
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

    /**
     * The natural ordering imposed by this implementation is identical to the
     * natural ordering of path's {@link #toString() name}.
     * On Windows, case is ignored when comparing the path names.
     */
    @Override
    public int compareTo(Path that) {
        return COMPARATOR.compare(this, (TPath) that);
    }

    /**
     * This path is considered equal to the given {@code other} object
     * if and only if the other object is identical to this object or if the
     * other object is a {@link TPath} object with a
     * {@link #getFileSystem() file system} which is considered
     * {@link TFileSystem#equals(Object) equal} to this path's file system and
     * a {@link #toString() name} which is considered
     * {@link String#equals(Object) equal} to this path's name.
     * On Windows, case is ignored when comparing the path names.
     */
    @Override
    public boolean equals(final Object that) {
        return this == that
                || that instanceof TPath
                    && COMPARATOR.equals(this, (TPath) that);
    }

    /**
     * Returns a hash code which is consistent with {@link #equals(Object)}.
     * 
     * @return A hash code which is consistent with {@link #equals(Object)}.
     */
    @Override
    public int hashCode() {
        return COMPARATOR.hashCode(this);
    }

    @Override
    public String toString() {
        final String string = this.string;
        return null != string ? string : (this.string = newString());
    }

    private String newString() {
        return getName()
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

    private static class TPathComparator implements Comparator<TPath> {
        @Override
        public int compare(TPath p1, TPath p2) {
            return p1.toString().compareTo(p2.toString());
        }

        boolean equals(TPath p1, TPath p2) {
            return p1.getFileSystem().equals(p2.getFileSystem())
                    && p1.toString().equals(p2.toString());
        }
        
        int hashCode(TPath p) {
            final Integer hashCode = p.hashCode;
            if (null != hashCode)
                return hashCode;
            int result = 17;
            result = 37 * result + p.getFileSystem().hashCode();
            result = 37 * result + p.toString().hashCode();
            return p.hashCode = result;
        }
    }

    private static final class WindowsTPathComparator extends TPathComparator {
        @Override
        public int compare(TPath p1, TPath p2) {
            return p1.toString().compareToIgnoreCase(p2.toString());
        }

        @Override
        boolean equals(TPath p1, TPath p2) {
            return p1.getFileSystem().equals(p2.getFileSystem())
                    && p1.toString().equalsIgnoreCase(p2.toString());
        }

        @Override
        int hashCode(TPath p) {
            final Integer hashCode = p.hashCode;
            if (null != hashCode)
                return hashCode;
            int result = 17;
            result = 37 * result + p.getFileSystem().hashCode();
            result = 37 * result + p.toString().toLowerCase().hashCode();
            return p.hashCode = result;
        }
    }
}
