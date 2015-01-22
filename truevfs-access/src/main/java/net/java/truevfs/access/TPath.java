/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access;

import java.io.*;
import static java.io.File.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.attribute.*;
import java.util.*;
import javax.annotation.*;
import javax.annotation.concurrent.*;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.java.truecommons.cio.*;
import net.java.truecommons.shed.*;
import static net.java.truecommons.shed.HashMaps.*;
import net.java.truecommons.shed.Paths;
import static net.java.truevfs.access.ExpertFeature.Level.*;
import static net.java.truevfs.access.ExpertFeature.Reason.*;
import static net.java.truevfs.access.TUriHelper.*;
import net.java.truevfs.kernel.spec.*;
import static net.java.truevfs.kernel.spec.FsAccessOption.*;
import static net.java.truevfs.kernel.spec.FsNodeName.*;

/**
 * A {@link Path} implementation for use with NIO.2.
 * Applications should directly instantiate this class to overcome the
 * <a href="package-summary.html#fspsl">restrictions</a> of the file system
 * provider service location in the NIO.2 API for JSE&nbsp;7.
 * Once created, it's safe to use {@code TPath} instances polymorphically as
 * {@code Path} instances.
 * <p>
 * Objects of this class are immutable and inherently volatile because all
 * virtual file system state is managed by the TrueVFS Kernel module.
 * <p>
 * You should never use object identity ('==') to test for equality of objects
 * of this class with another object.
 * Use the method {@link #equals(Object)} instead.
 * <p>
 * Unless otherwise noted, you should <em>not</em> assume that calling the same
 * method on an instance of this class multiple times will return the same
 * object.
 * <p>
 * Unless otherwise noted, when an instance of this class is created, the
 * resulting path name gets scanned for prospective archive files using the
 * current archive detector {@code TConfig.current().getArchiveDetector()}.
 * To change this, wrap the object creation in a code block which
 * {@link TConfig#open() pushes} a temporary configuration on the inheritbale
 * thread local stack of configurations as follows:
 * </p>
 * <pre><code>
 * // Create reference to the current directory.
 * TPath directory = new TPath("");
 * // This is how you would detect a prospective archive file, supposing
 * // the JAR of the module TrueVFS Driver ZIP is present on the run time
 * // class path.
 * TPath archive = directory.resolve("archive.zip");
 * TPath file;
 * try (TConfig config = TConfig.open()) {
 *     config.setArchiveDetector(TArchiveDetector.NULL);
 *     // Ignore prospective archive file here.
 *     file = directory.resolve("archive.zip");
 * }
 * // Once created, the prospective archive file detection does not change
 * // because a TPath is immutable.
 * assert archive.getArchiveDetector() == TArchiveDetector.ALL;
 * assert archive.isArchive();
 * assert file.getArchiveDetector() == TArchiveDetector.NULL;
 * assert !file.isArchive();
 * </code></pre>
 * <p>
 * Mind that you should either use {@code archive} or {@code file} from the
 * previous example to do any subsequent I/O - but not both - so that you don't
 * bypass or corrupt the state which gets implicitly associated with any
 * archive file by the TrueVFS Kernel module!
 *
 * @author Christian Schlichtherle
 */
@Immutable
@SuppressFBWarnings("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
public final class TPath implements Path, TRex {

    private static final TPathComparator COMPARATOR = '\\' == separatorChar
            ? new WindowsTPathComparator()
            : new TPathComparator();

    private final URI name;
    private final TArchiveDetector detector;
    private final FsNodePath nodePath;
    private volatile @CheckForNull TFileSystem fileSystem;
    private volatile @CheckForNull String string;
    private volatile @CheckForNull Integer hashCode;
    private volatile @CheckForNull List<String> elements;

    /**
     * Constructs a new path from the given path strings.
     * <p>
     * The supported path name separators are "{@link File#separator}" and
     * "{@code /}".
     * Any trailing separators in the resulting path name current discarded.
     * <p>
     * This constructor scans the {@linkplain TPath#toString() path name} resulting
     * from the segment parameters to detect prospective archive files using
     * the current archive detector {@code TConfig.current().getArchiveDetector()}.
     *
     * <h3>Examples</h3>
<p>On all platforms:</p>
<dl>
    <dt>Relative path name:</dt>
    <dd><code>Path path = new TPath("app.war/WEB-INF/lib", "lib.jar/META-INF/MANIFEST.MF");</code></dd>
</dl>
<p>On POSIX platforms (Unix, Linux, Mac OS X):</p>
<dl>
    <dt>Absolute path name:</dt>
    <dd><code>Path path = new TPath("/home/christian/archive.zip");</code></dd>
</dl>
<p>On the Windows platform:</p>
<dl>
    <dt>Relative path name:</dt>
    <dd><code>Path path = new TPath("app.war\WEB-INF\lib", "lib.jar\META-INF\MANIFEST.MF");</code></dd>
    <dt>Absolute path name with C: drive letter and wierd case letters:</dt>
    <dd><code>Path path = new TPath("c:\UsErS\cHrIsTiAn\ArChIvE.zIp");</code></dd>
    <dt>Absolute path name with separated UNC host and share name and forward slash separator:</dt>
    <dd><code>Path path = new TPath("//host", "share", "archive.zip");</code></dd>
    <dt>Dito with mixed slash separators:</dt>
    <dd><code>Path path = new TPath("\\host/share\archive.zip");</code></dd>
</dl>
     *
     * @param first the first sub path string.
     * @param more optional sub path strings.
     */
    public TPath(String first, String... more) {
        this(name(first, more), null, null);
    }

    /**
     * Constructs a new path from the given file system and sub path strings.
     * <p>
     * The supported path name separators are "{@link File#separator}" and
     * "{@code /}".
     * Any leading and trailing separators in the resulting path name current
     * discarded.
     * <p>
     * This constructor scans the {@linkplain TPath#toString() path name} resulting
     * from the segment parameters to detect prospective archive files using
     * the current archive detector {@code TConfig.current().getArchiveDetector()}.
     *
     * @param fileSystem the file system to access.
     * @param first the first sub path string.
     * @param more optional sub path strings.
     */
    TPath(final TFileSystem fileSystem, String first, final String... more) {
        final URI name = name(cutLeadingSeparators(first), more);
        this.name = name;
        final TArchiveDetector detector = TConfig.current().getArchiveDetector();
        this.detector = detector;
        this.nodePath = new TUriResolver(detector).resolve(
                new FsNodePath(fileSystem.getMountPoint(), ROOT),
                name);
        this.fileSystem = fileSystem;

        assert invariants();
    }

    /**
     * Constructs a new path from the given hierarchical URI.
     * <p>
     * If the {@linkplain URI#getScheme() scheme component} of the URI is undefined
     * and the {@linkplain URI#getSchemeSpecificPart() scheme specific part} does
     * not start with a "{@code /}", then the URI gets resolved against the
     * "{@code file:}" based URI for the current directory in the platform file
     * system.
     * Otherwise, if the scheme component is undefined, then the URI gets
     * resolved against the URI "{@code file:/}".
     * <p>
     * This constructor scans the {@linkplain URI#getPath() path component} of
     * the URI to detect prospective archive files using the
     * current archive detector {@code TConfig.current().getArchiveDetector()}.
     *
     * <h3>Examples</h3>
<p>On all platforms:</p>
<dl>
    <dt>Relative URI with relative path component:</dt>
    <dd><code>Path path = new TPath(new URI("app.war/WEB-INF/lib/lib.jar/META-INF/MANIFEST.MF"));</code></dd>
    <dt>HTTP URI:</dt>
    <dd><code>Path path = new TPath(new URI("http://acme.com/download/everything.tar.gz/README.TXT"));</code></dd>
</dl>
<p>On POSIX platforms (Unix, Linux, Mac OS X):</p>
<dl>
    <dt>Relative URI with absolute path component:</dt>
    <dd><code>Path path = new TPath(new URI("/home/christian/archive.zip"));</code></dd>
    <dt>Dito with absolute, hierarchical URI:</dt>
    <dd><code>Path path = new TPath(new URI("file:/home/christian/archive.zip"));</code></dd>
</dl>
<p>On the Windows platform:</p>
<dl>
    <dt>Relative URI with relative path component with C: drive letter and following absolute path name:</dt>
    <dd><code>Path path = new TPath(new URI("c%3A/Users/christian/archive.zip"));</code></dd>
    <dt>Dito with absolute, hierarchical URI:</dt>
    <dd><code>Path path = new TPath(new URI("file:/c:/Users/christian/archive.zip"));</code></dd>
    <dt>Relative URI with UNC host and share name:</dt>
    <dd><code>Path path = new TPath(new URI("//host/share/archive.zip"));</code></dd>
    <dt>Dito with absolute, hierarchical URI:</dt>
    <dd><code>Path path = new TPath(new URI("file://host/share/archive.zip"));</code></dd>
</dl>
     *
     * @param  name the path name.
     *         This must be a hierarchical URI with an undefined fragment
     *         component.
     *         Any trailing separators in the path component current discarded.
     * @throws IllegalArgumentException if the preconditions for the parameter
     *         do not hold.
     */
    public TPath(URI name) {
        this(name, null, null);
    }

    /**
     * Constructs a new path from the given file.
     * <p>
     * This constructor is required for interoperability with the {@link TFile}
     * class because it does not support {@link TFile#toPath()}.
     * <p>
     * If {@code file} is an instance of {@link TFile}, its
     * {@linkplain TFile#getArchiveDetector() archive detector} and
     * {@linkplain TFile#getNodePath() file system node path} current shared with
     * this instance.
     * <p>
     * Otherwise, this constructor scans the {@linkplain File#getPath() path name}
     * of the file to detect prospective archive files using the
     * current archive detector {@code TConfig.current().getArchiveDetector()}.
     *
     * <h3>Examples</h3>
<p>On all platforms:</p>
<dl>
    <dt>Relative path name:</dt>
    <dd><code>Path path = new TPath(new File("app.war/WEB-INF/lib", "lib.jar/META-INF/MANIFEST.MF"));</code></dd>
</dl>
<p>On POSIX platforms (Unix, Linux, Mac OS X):</p>
<dl>
    <dt>Absolute path name with plain {@code File}:</dt>
    <dd><code>Path path = new TPath(new File("/home/christian/archive.zip"));</code></dd>
    <dt>Absolute path name with plain {@link TFile}:</dt>
    <dd><code>Path path = new TPath(new TFile("/home/christian/archive.zip"));</code></dd>
</dl>
<p>On the Windows platform:</p>
<dl>
    <dt>Absolute path name with plain {@code File}:</dt>
    <dd><code>Path path = new TPath(new File("c:\home\christian\archive.zip"));</code></dd>
    <dt>Absolute path name with {@link TFile}:</dt>
    <dd><code>Path path = new TPath(new TFile("c:\home\christian\archive.zip"));</code></dd>
</dl>
     *
     * @param file a file.
     *        If this is an instance of {@link TFile}, its
     *        {@linkplain TFile#getArchiveDetector() archive detector} and
     *        {@linkplain TFile#getNodePath() file system node path} current shared
     *        with this instance.
     */
    public TPath(File file) {
        final URI name = name(file.getPath());
        this.name = name;
        if (file instanceof TFile) {
            final TFile tfile = (TFile) file;
            this.detector = tfile.getArchiveDetector();
            this.nodePath = tfile.getNodePath();
        } else {
            final TArchiveDetector detector = TConfig.current().getArchiveDetector();
            this.detector = detector;
            this.nodePath = nodePath(detector, name);
        }

        assert invariants();
    }

    /**
     * Constructs a new path from the given path.
     * <p>
     * This constructor scans the {@link Path#toString() path name} of the
     * given path to detect prospective archive files using the
     * current archive detector {@code TConfig.current().getArchiveDetector()}.
     *
     * <h3>Examples</h3>
<p>On all platforms:</p>
<dl>
    <dt>Relative path name:</dt>
    <dd><code>Path path = new TPath(Paths.current("app.war/WEB-INF/lib", "lib.jar/META-INF/MANIFEST.MF"));</code></dd>
</dl>
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

    @SuppressFBWarnings("ES_COMPARING_STRINGS_WITH_EQ")
    private TPath(
            URI name,
            @CheckForNull TArchiveDetector detector,
            final @CheckForNull FsNodePath nodePath) {
        this.name = name = name(name);
        if (null == detector) detector = TConfig.current().getArchiveDetector();
        this.detector = detector;
        this.nodePath = null != nodePath ? nodePath : nodePath(detector, name);

        assert invariants();
    }

    @SuppressWarnings("AssignmentToForLoopParameter")
    private static URI name(final String first, final String... more) {
        final StringBuilder sb;
        {
            int l = 1 + first.length(); // might prepend SEPARATOR
            for (final String m : more) l += 1 + m.length(); // dito
            sb = new StringBuilder(l);
        }
        int i = -1;
        {
            String s = first.replace(separatorChar, SEPARATOR_CHAR);
            int l = s.length();
            for (int k = 0; k < l; k++) {
                char c = s.charAt(k);
                if (SEPARATOR_CHAR != c
                        || i <= 0
                        || 0 < i && SEPARATOR_CHAR != sb.charAt(i)) {
                    sb.append(c);
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
                final boolean o = 0 <= i && SEPARATOR_CHAR != sb.charAt(i);
                if (n || o) {
                    if (0 == j && n && o) sb.append(SEPARATOR_CHAR);
                    sb.append(c);
                    i++;
                    j++;
                }
            }
        }
        String p = sb.toString();
        final int l = prefixLength(p);
        p = cutTrailingSeparators(p, l);
        try {
            final UriBuilder ub = new UriBuilder();
            if (0 < l) {
                if (SEPARATOR_CHAR != p.charAt(l - 1))
                    throw new QuotedUriSyntaxException(p, "Relative path with non-empty prefix.");
                if (1 < l && SEPARATOR_CHAR == p.charAt(0)) {
                    assert separatorChar == '\\';
                    assert SEPARATOR_CHAR == p.charAt(1);
                    // Windows UNC with forward slashes, e.g. "//host[/share[/path]]"
                    final int j = p.indexOf(SEPARATOR_CHAR, 2);
                    if (-1 != j) {
                        // E.g. "//host/share[/path]"
                        ub.setAuthority(p.substring(2, j)); // "host"
                        p = p.substring(j); // "/share[/path]"
                    }
                }
            }
            return ub.path(p).getUri();
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @SuppressFBWarnings("ES_COMPARING_STRINGS_WITH_EQ")
    private static URI name(URI uri) {
        try {
            uri = fix(check(uri));
            final int pl = pathPrefixLength(uri);
            final String q = uri.getPath();
            final String p = cutTrailingSeparators(q, pl);
            // Mind contract of cutTrailingSeparators(String, int).
            return p == q ? uri : new UriBuilder(uri).path(p).getUri();
        } catch (final URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private static String cutLeadingSeparators(final String p) {
        for (int i = 0, l = p.length(); i < l; i++)
            if (SEPARATOR_CHAR != p.charAt(i))
                return p.substring(i);
        return "";
    }

    static String cutTrailingSeparators(
            final String p,
            final int o) {
        int i = p.length();
        if (o >= i || SEPARATOR_CHAR != p.charAt(--i)) return p;
        while (o <= i && SEPARATOR_CHAR == p.charAt(--i)) {
        }
        return p.substring(0, ++i);
    }

    private static int prefixLength(String p) {
        return Paths.prefixLength(p, SEPARATOR_CHAR, true);
    }

    private static FsNodePath nodePath(TArchiveDetector detector, URI name) {
        TFileSystemProvider provider = TFileSystemProvider.get(name);
        return new TUriResolver(detector).resolve(
                provider.getRoot(),
                provider.relativize(name));
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
        assert null != getNodePath();
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
        final FsNodePath nodePath = getNodePath();
        final boolean root = nodePath.getNodeName().isRoot();
        final FsMountPoint parent = nodePath.getMountPoint().getParent();
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
        final FsNodePath nodePath = getNodePath();
        final boolean root = nodePath.getNodeName().isRoot();
        final FsMountPoint parent = nodePath.getMountPoint().getParent();
        return !root    ? null != parent
                        : null != parent && null != parent.getParent();
    }

    /**
     * Returns the name of this path as a {@code URI}.
     * Multiple invocations of this method will return the same object.
     *
     * @return the name of this path as a {@code URI}.
     */
    URI getName() { return name; }

    @Override
    public TArchiveDetector getArchiveDetector() { return detector; }

    @Override
    public FsNodePath getNodePath() { return nodePath; }

    @Override
    public FsMountPoint getMountPoint() {
        return getNodePath().getMountPoint();
    }

    @Override
    public FsNodeName getNodeName() {
        return getNodePath().getNodeName();
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
        return null != fs ? fs : (this.fileSystem = getFileSystem0());
    }

    private TFileSystem getFileSystem0() {
        return TFileSystemProvider.get(getName()).getFileSystem(this);
    }

    @Override
    public boolean isAbsolute() { return hasAbsolutePath(getName()); }

    /**
     * Returns a path object for the same path name, but does not detect any
     * archive file name patterns in the last path name segment.
     * The parent path object is unaffected by this transformation, so the
     * path name of this path object may address an entry in an archive file.
     * <p>
     * <em>Warning:</em> Doing I/O on the returned path object will yield
     * inconsistent results and may even cause <strong>loss of data</strong> if
     * the last path name segment addresses an archive file which is currently
     * mounted by the TrueVFS Kernel!
     *
     * @return A path object for the same path name, but does not detect any
     *         archive file name patterns in the last path name segment.
     * @see    TFileSystem#close()
     * @see    TVFS#umount()
     */
    @ExpertFeature( level=INTERMEDIATE,
                    value=INJECTING_A_DIFFERENT_DETECTOR_FOR_THE_SAME_PATH_MAY_CORRUPT_DATA)
    public TPath toNonArchivePath() {
        if (!isArchive()) return this;
        try (final TConfig config = TConfig.open()) {
            config.setArchiveDetector(TArchiveDetector.NULL);
            final TPath fileName = getFileName();
            assert null != fileName : "an archive file must not have an empty path name!";
            return resolveSibling(fileName);
        }
    }

    @Override
    public @Nullable TPath getRoot() {
        final URI n = getName();
        final String ssp = n.getSchemeSpecificPart();
        final int l = prefixLength(ssp);
        if (l <= 0 || SEPARATOR_CHAR != ssp.charAt(l - 1)) return null;
        return new TPath(name(ssp.substring(0, l)), getArchiveDetector(), null);
    }

    @Override
    public @Nullable TPath getFileName() {
        final List<String> elements = getElements();
        final int l = elements.size();
        if (l <= 0) return null;
        return new TPath(name(elements.get(l - 1)), getArchiveDetector(), null);
    }

    @Override
    public @Nullable TPath getParent() {
        final URI n = getName();
        {
            final int l = n.getPath().length();
            final int pl = pathPrefixLength(n);
            if (l <= pl) return null;
        }
        final URI p = n.resolve(DOT_URI);
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
    private List<String> getElements() {
        final List<String> elements = this.elements;
        return null != elements ? elements : (this.elements = getElements0());
    }

    private List<String> getElements0() {
        final URI n = getName();
        final String p = n.getPath();
        final String[] ss = p.substring(pathPrefixLength(n)).split(SEPARATOR);
        int i = 0;
        for (String s : ss)
            if (!s.isEmpty())
                ss[i++] = s;
        return //Collections.unmodifiableList(
                Arrays.asList(ss).subList(0, i);
    }

    @Override
    public int getNameCount() { return getElements().size(); }

    @Override
    public TPath getName(int index) {
        return new TPath(getElements().get(index));
    }

    @Override
    public TPath subpath(final int beginIndex, final int endIndex) {
        final List<String> segments = getElements();
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
        return new TPath(getName().normalize(), getArchiveDetector(), getNodePath());
    }

    @Override
    public TPath resolve(Path other) {
        if (other instanceof TPath) {
            final TPath that = (TPath) other;
            if (that.isAbsolute()) return that;
            if (that.toString().isEmpty()) return this;
            return resolve(that.getName());
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

    private TPath resolve(final URI other) {
        URI name;
        final String namePath;
        if (hasAbsolutePath(other) || (name = getName()).toString().isEmpty()) {
            name = other;
        } else if (other.toString().isEmpty()) {
            name = getName();
        } else if ((namePath = name.getPath()).endsWith(SEPARATOR)) {
            name = name.resolve(other);
        } else {
            name = new UriBuilder(name)
                    .path(namePath + SEPARATOR_CHAR)
                    .toUri()
                    .resolve(other);
        }
        final TArchiveDetector detector = TConfig.current().getArchiveDetector();
        final FsNodePath path = new TUriResolver(detector).resolve(
                hasAbsolutePath(other)
                    ? TFileSystemProvider.get(getName()).getRoot()
                    : getNodePath(),
                other);
        return new TPath(name, detector, path);
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
        return new TPath(getUri().relativize(other.toUri()));
    }

    @Override
    public URI getUri() {
        URI n = getName();
        String s = n.getScheme();
        return new UriBuilder(getNodePath().toHierarchicalUri())
                .scheme(null != s ? s : TFileSystemProvider.get(n).getScheme())
                .toUri();
    }

    @Override
    public URI toUri() { return getUri(); }

    @Override
    public TPath toAbsolutePath() {
        return new TPath(getUri(), getArchiveDetector(), getNodePath());
    }

    @Override
    public TPath toRealPath(LinkOption... options) throws IOException {
        // TODO: resolve symlinks!
        return new TPath(getUri(), getArchiveDetector(), getNodePath());
    }

    /**
     * Returns a new {@code TFile} object for this path.
     * If this path was constructed by the
     * {@link #TPath(File) file constructor}, then the returned <em>new</em>
     * {@code TFile} object compares {@link TFile#equals(Object) equal} with
     * that file object, even if it was a plain {@link File} object.
     *
     * @return A {@code TFile} object for this path.
     * @throws UnsupportedOperationException if this path is not file based,
     *         i.e. if the scheme component of the {@link #getUri() URI} of
     *         this path is not {@code file}.
     */
    @Override
    public TFile toFile() {
        try {
            return getName().isAbsolute()
                    ? new TFile(getNodePath(), getArchiveDetector())
                    : new TFile(toString(), getArchiveDetector());
        } catch (IllegalArgumentException ex) {
            throw new UnsupportedOperationException(ex);
        }
    }

    @Override
    public TPath toPath() { return this; }

    /** @throws UnsupportedOperationException always */
    @Override
    public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /** @throws UnsupportedOperationException always */
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
            this.i = path.getElements().iterator();
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
    public int compareTo(Path other) {
        return COMPARATOR.compare(this, (TPath) other);
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
    public boolean equals(final Object other) {
        return this == other
                || other instanceof TPath
                    && COMPARATOR.equals(this, (TPath) other);
    }

    /**
     * Returns a hash code which is consistent with {@link #equals(Object)}.
     *
     * @return A hash code which is consistent with {@link #equals(Object)}.
     */
    @Override
    public int hashCode() { return COMPARATOR.hashCode(this); }

    @Override
    public String toString() {
        final String string = this.string;
        return null != string ? string : (this.string = toString0());
    }

    private String toString0() {
        final URI name = getName();
        // If name is not absolute, we must call name.getSchemeSpecificPart(),
        // not just name.toString() in order to current the *decoded* URI!
        return name.isAbsolute()
                ? name.toString()
                : name.getSchemeSpecificPart().replace(SEPARATOR_CHAR, separatorChar);
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

    FsNode stat() throws IOException {
        return getFileSystem().stat(this);
    }

    InputSocket<?> input(BitField<FsAccessOption> options) {
        return getFileSystem().input(this, options);
    }

    OutputSocket<?> output( BitField<FsAccessOption> options,
                            @CheckForNull Entry template) {
        return getFileSystem().output(this, options, template);
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

    BitField<FsAccessOption> inputOptions(final OpenOption... options) {
        final HashSet<OpenOption> set = new HashSet<>(
                initialCapacity(options.length));
        Collections.addAll(set, options);
        return inputOptions(set);
    }

    BitField<FsAccessOption> inputOptions(final Set<? extends OpenOption> options) {
        final int s = options.size();
        if (0 == s || 1 == s && options.contains(StandardOpenOption.READ))
            return getAccessPreferences();
        throw new IllegalArgumentException(options.toString());
    }

    BitField<FsAccessOption> outputOptions(final OpenOption... options) {
        final HashSet<OpenOption> set = new HashSet<>(
                initialCapacity(options.length));
        Collections.addAll(set, options);
        return outputOptions(set);
    }

    BitField<FsAccessOption> outputOptions(final Set<? extends OpenOption> options) {
        final EnumSet<FsAccessOption> set = EnumSet.noneOf(FsAccessOption.class);
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
                    set.add(APPEND);
                    break;
                case CREATE_NEW:
                    set.add(EXCLUSIVE);
                    break;
                default:
                    throw new UnsupportedOperationException(option.toString());
            }
        }
        final BitField<FsAccessOption> prefs = getAccessPreferences();
        return set.isEmpty() ? prefs : prefs.or(BitField.copyOf(set));
    }

    BitField<FsAccessOption> getAccessPreferences() {
        final BitField<FsAccessOption> preferences =
                TConfig.current().getAccessPreferences();
        return null != getMountPoint().getParent()
                ? preferences
                : preferences.clear(CREATE_PARENTS);
    }

    /**
     * The methods in this class use
     * {@link TPath#getNodePath()}.{@link FsNodePath#getMountPoint()} as an
     * identifier for the file system in order to avoid creating the
     * {@link TFileSystem} object for a {@code TPath}.
     * Creating the file system object usually creates an {@link FsController}
     * too, which is an expensive operation.
     * This may cause some unwanted {@link FsMountPoint} parsing when a
     * {@code TPath} object is just used for resolving another {@code TPath}
     * object.
     */
    @SuppressFBWarnings("SE_COMPARATOR_SHOULD_BE_SERIALIZABLE")
    private static class TPathComparator implements Comparator<TPath> {
        @Override
        public int compare(TPath p1, TPath p2) {
            return p1.toString().compareTo(p2.toString());
        }

        boolean equals(TPath p1, TPath p2) {
            return p1.getNodePath().getMountPoint().equals(p2.getNodePath().getMountPoint())
                    && p1.toString().equals(p2.toString());
        }

        @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
        int hashCode(TPath p) {
            final Integer hashCode = p.hashCode;
            if (null != hashCode) return hashCode;
            int result = 17;
            result = 37 * result + p.getNodePath().getMountPoint().hashCode();
            result = 37 * result + p.toString().hashCode();
            return p.hashCode = result;
        }
    } // TPathComparator

    /**
     * The methods in this class use
     * {@link TPath#getNodePath()}.{@link FsNodePath#getMountPoint()} as an
     * identifier for the file system in order to avoid creating the
     * {@link TFileSystem} object for a {@code TPath}.
     * Creating the file system object usually creates an {@link FsController}
     * too, which is an expensive operation.
     * This may cause some unwanted {@link FsMountPoint} parsing when a
     * {@code TPath} object is just used for resolving another {@code TPath}
     * object.
     */
    @SuppressFBWarnings("SE_COMPARATOR_SHOULD_BE_SERIALIZABLE")
    private static final class WindowsTPathComparator extends TPathComparator {
        @Override
        public int compare(TPath p1, TPath p2) {
            return p1.toString().compareToIgnoreCase(p2.toString());
        }

        @Override
        boolean equals(TPath p1, TPath p2) {
            return p1.getNodePath().getMountPoint().equals(p2.getNodePath().getMountPoint())
                    && p1.toString().equalsIgnoreCase(p2.toString());
        }

        @Override
        @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
        int hashCode(TPath p) {
            final Integer hashCode = p.hashCode;
            if (null != hashCode) return hashCode;
            int result = 17;
            result = 37 * result + p.getNodePath().getMountPoint().hashCode();
            result = 37 * result + p.toString().toLowerCase(Locale.getDefault()).hashCode();
            return p.hashCode = result;
        }
    } // WindowsTPathComparator
}
