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
import de.schlichtherle.truezip.file.TFile;
import static de.schlichtherle.truezip.file.TFile.*;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsMountPoint;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import de.schlichtherle.truezip.fs.FsPath;
import de.schlichtherle.truezip.fs.FsScheme;
import static de.schlichtherle.truezip.io.Paths.*;
import de.schlichtherle.truezip.io.Paths.Splitter;
import static de.schlichtherle.truezip.fs.FsUriModifier.*;
import de.schlichtherle.truezip.util.UriBuilder;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;
import net.jcip.annotations.Immutable;

/**
 * A path implementation for the TrueZIP FileSystemProvider.
 * 
 * <a name="constructors><h3>Constructors</h3>
 * <p>
 * There are two types of constructors for this class:
 * <ol>
 * <li>The {@link #TPath(URI) URI constructor} parses the given URI for the
 *     pattern
 * </ol>
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
@edu.umd.cs.findbugs.annotations.SuppressWarnings("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
public final class TPath implements Path {

    // Order is important here!
    private static TArchiveDetector
            defaultDetector = TArchiveDetector.ALL;
    private static TPath
            ROOT_DIRECTORY = new TPath(new File(File.separator).toURI());
    private static TPath CURRENT_DIRECTORY = new TPath(new File("").toURI());

    private transient TArchiveDetector detector;
    private URI uri;
    private FsPath path;
    private transient @Nullable TPath innerArchive;
    private transient @Nullable TPath enclArchive;
    private transient @Nullable FsEntryName enclEntryName;

    public TPath(String first, String... more) {
        this(null, null, first, more);
    }

    public TPath(TArchiveDetector detector, String first, String... more) {
        this(null, detector, first, more);
    }

    public TPath(TPath parent, String first, String... more) {
        this(parent, null, first, more);
    }

    public TPath(   final TPath parent,
                    final TArchiveDetector detector,
                    String first,
                    final String... more) {
        this.detector = null != detector
                ? detector
                : null != parent
                    ? parent.detector
                    : defaultDetector;

        // Compute the URI.
        final UriBuilder ub = new UriBuilder();
        final StringBuilder pb = new StringBuilder();
        if (null != parent) {
            final URI parentUri = parent.toUri();
            ub.setUri(parent.toUri());
            pb.append(parentUri.getPath()).append(SEPARATOR);
        }
        pb.append(first);
        for (final String m : more)
            pb      .append(SEPARATOR_CHAR)
                    .append(m.replace(separatorChar, SEPARATOR_CHAR));
        uri = ub.path(pb.toString()).toUri().normalize();

        // Compute $path and $uri;
        if (null != parent)
            path = parent.toFsPath();
        else if (uri.getPath().startsWith(SEPARATOR))
            path = ROOT_DIRECTORY.toFsPath();
        else
            path = CURRENT_DIRECTORY.toFsPath();
        for (String p; "../".startsWith((p = uri.getPath()).substring(0, 3)); ) {
            if (1 >= p.length())
                break; // path component is "" or ".".
            // Path component is ".." or starts with "../".
            uri = path.getEntryName().toUri().resolve(uri);
            path = path.getMountPoint().getPath();
        }

        scan();
        parse();
    }

    private void scan() {
        class Scanner {
            final Splitter splitter = split(uri.getPath(), SEPARATOR_CHAR, false);

            void scan() {
                final String parent = splitter.getParentPath();
                final String member = splitter.getMemberName();
                if (null != parent) {
                    splitter.split(parent);
                    scan();
                } else if (member.isEmpty())
                    return;
                path = path.resolve(FsEntryName.create(member));
                final FsScheme scheme = detector.getScheme(member);
                if (null != scheme)
                    path = new FsPath(FsMountPoint.create(scheme, path), ROOT);
            }
        }
        new Scanner().scan();
    }

    /**
     * Constructs a new {@code TPath} instance from the given {@code uri}.
     * If the URI matches the pattern {@code scheme:file:path!/entry},
     * then the constructed file object treats the URI like an entry in the
     * federated file system of the type named {@code scheme}.
     * This may be recursively applied to access archive entries within other
     * archive files.
     * <p>
     * Note that the constructed {@code TPath} instance uses the
     * {@link #getDefaultArchiveDetector() default archive detector} to look
     * up archive drivers for the named scheme(s).
     *
     * @param  uri an absolute URI with a scheme supported by the
     *         {@link #getDefaultArchiveDetector() default archive detector}.
     * @throws IllegalArgumentException if the given URI does not conform to
     *         the syntax constraints for {@link FsPath}s.
     */
    public TPath(final URI uri) {
        this.detector = defaultDetector;
        if (null == uri)
            throw new NullPointerException();
        this.uri = uri;
        parse();
    }

    private void parse() {
        URI uri = this.uri;
        if (!uri.isAbsolute())
            if (uri.toString().startsWith(SEPARATOR))
                uri = new UriBuilder(uri)
                        .scheme(ROOT_DIRECTORY.toUri().getScheme())
                        .toUri();
            else
                uri = CURRENT_DIRECTORY.toUri().resolve(uri);
        if (null == path)
            path = FsPath.create(uri, CANONICALIZE);
        final FsMountPoint mp = path.getMountPoint();
        final FsPath mpp = mp == null ? null : mp.getPath();
        final FsEntryName en;

        if (null == mpp) {
            assert !path.toUri().isOpaque();
            assert null != mp == path.toUri().isAbsolute();
            this.enclArchive = null;
            this.enclEntryName = null;
            this.innerArchive = null;
        } else if ((en = path.getEntryName()).isRoot()) {
            assert path.toUri().isOpaque();
            if (mpp.toUri().isOpaque()) {
                this.enclArchive
                        = new TPath(mpp.getMountPoint(), detector);
                this.enclEntryName = mpp.getEntryName();
            } else {
                this.enclArchive = null;
                this.enclEntryName = null;
            }
            this.innerArchive = this;
        } else {
            assert path.toUri().isOpaque();
            this.enclArchive = new TPath(mp, detector);
            this.enclEntryName = en;
            this.innerArchive = this.enclArchive;
        }

        assert invariants();
    }

    @SuppressWarnings("LeakingThisInConstructor")
    private TPath(  final FsMountPoint mountPoint,
                    final TArchiveDetector detector) {
        this.uri = mountPoint.toUri();
        this.path = new FsPath(mountPoint, FsEntryName.ROOT);
        this.detector = detector;

        final FsPath mountPointPath = mountPoint.getPath();

        if (null == mountPointPath) {
            assert !mountPoint.toUri().isOpaque();
            this.enclArchive = null;
            this.enclEntryName = null;
            this.innerArchive = null;
        } else {
            assert mountPoint.toUri().isOpaque();
            if (mountPointPath.toUri().isOpaque()) {
                this.enclArchive
                        = new TPath(mountPointPath.getMountPoint(), detector);
                this.enclEntryName = mountPointPath.getEntryName();
            } else {
                this.enclArchive = null;
                this.enclEntryName = null;
            }
            this.innerArchive = this;
        }

        assert invariants();
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
     * @throws AssertionError If any invariant is violated even if assertions
     *         are disabled.
     * @return {@code true}
     */
    private boolean invariants() {
        assert null != toUri();
        assert null != toFsPath();
        assert null != toFsPath().getMountPoint();
        assert null != toFsPath().getEntryName();
        assert null != getArchiveDetector();
        assert (null != getInnerArchive()) == (getInnerEntryName() != null);
        assert (null != getEnclArchive()) == (getEnclEntryName() != null);
        assert this != getEnclArchive();
        assert (this == getInnerArchive()) ^ (getInnerArchive() == getEnclArchive());
        assert null == getEnclArchive() || !getEnclEntryName().toString().isEmpty();
        return true;
    }

    /**
     * Returns {@code true} if and only if this path object addresses an
     * archive file.
     * Whether or not this is true solely depends on the
     * {@link TArchiveDetector} which was used to construct this path object
     * - no file system tests are performed by this method!
     *
     * Returns {@code true} if and only if this path object addresses an
     *         archive file.
     * @see    #isEntry
     */
    public boolean isArchive() {
        return this == innerArchive;
    }

    /**
     * Returns {@code true} if and only if this file object addresses an
     * entry located within an archive file.
     * Whether or not this is true solely depends on the
     * {@link TArchiveDetector} which was used to construct this path object
     * - no file system tests are performed by this method!
     *
     * @return {@code true} if and only if this file object addresses an
     *         entry located within an archive file.
     * @see #isArchive
     */
    public boolean isEntry() {
        return enclEntryName != null;
    }

    /**
     * Returns the innermost archive path object for this path object.
     * That is, if this object addresses an archive file, then this method
     * returns {@code this}.
     * If this object addresses an entry located within an archive file, then
     * this methods returns the path object representing the enclosing archive
     * file, or {@code null} otherwise.
     * <p>
     * This method always returns a normalized path, i.e. all occurences of
     * {@code "."} and {@code ".."} in the path name are removed according to
     * their meaning wherever possible.
     * <p>
     * In order to support unlimited nesting levels, this method returns
     * a {@code TPath} instance which may recursively address an entry within
     * another archive file.
     * 
     * @return The innermost archive path object for this path object.
     */
    public @CheckForNull TPath getInnerArchive() {
        return innerArchive;
    }

    /**
     * Returns the entry name relative to the innermost archive file.
     * That is, if this object addresses an archive file, then this method
     * returns the empty string {@code ""}.
     * If this object addresses an entry located within an archive file,
     * then this method returns the relative path of the entry in the
     * enclosing archive file separated by the entry separator character
     * {@code '/'}, or {@code null} otherwise.
     * <p>
     * This method always returns an undotified path, i.e. all redundant
     * occurences of {@code "."} and {@code ".."} in the path are removed
     * wherever possible.
     * 
     * @return The entry name relative to the innermost archive file.
     */
    public @Nullable String getInnerEntryName() {
        return this == innerArchive
                ? ROOT.getPath()
                : null == enclEntryName
                    ? null
                    : enclEntryName.getPath();
    }

    @Nullable FsEntryName getInnerEntryName0() {
        return this == innerArchive ? ROOT : enclEntryName;
    }

    /**
     * Returns the enclosing archive path object for this path object.
     * That is, if this object addresses an entry located within an archive
     * file, then this method returns the path object representing the
     * enclosing archive file, or {@code null} otherwise.
     * <p>
     * This method always returns a normalized path, i.e. all occurences of
     * {@code "."} and {@code ".."} in the path name are removed according to
     * their meaning wherever possible.
     * <p>
     * In order to support unlimited nesting levels, this method returns
     * a {@code TFile} instance which again could be an entry within
     * another archive file.
     * 
     * @return The enclosing archive file in this path.
     */
    public @CheckForNull TPath getEnclArchive() {
        return enclArchive;
    }

    /**
     * Returns the entry name relative to the enclosing archive file.
     * That is, if this object addresses an entry located within an archive
     * file, then this method returns the relative path of the entry in the
     * enclosing archive file separated by the entry separator character
     * {@code '/'}, or {@code null} otherwise.
     * <p>
     * This method always returns an undotified path, i.e. all redundant
     * occurences of {@code "."} and {@code ".."} in the path are removed
     * wherever possible.
     * 
     * @return The entry name relative to the enclosing archive file.
     */
    public @Nullable String getEnclEntryName() {
        return null == enclEntryName ? null : enclEntryName.getPath();
    }

    @Nullable FsEntryName getEnclEntryName0() {
        return enclEntryName;
    }

    /**
     * Returns the {@link TArchiveDetector} that was used to detect any archive
     * files in the path of this object at construction time.
     * 
     * @return The {@link TArchiveDetector} that was used to detect any archive
     *         files in the path of this object at construction time.
     */
    public TArchiveDetector getArchiveDetector() {
        return detector;
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
        return defaultDetector;
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
        if (null == detector)
            throw new NullPointerException();
        TPath.defaultDetector = detector;
    }

    @Override
    public FileSystem getFileSystem() {
        return TFileSystem.SINGLETON;
    }

    @Override
    public boolean isAbsolute() {
        return toFile().isAbsolute();
    }

    @Override
    public TPath getRoot() {
        return null;
    }

    @Override
    public TPath getFileName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TPath getParent() {
        throw new UnsupportedOperationException("Not supported yet.");
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
    public boolean startsWith(Path other) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean startsWith(String other) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean endsWith(Path other) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean endsWith(String other) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TPath normalize() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Path resolve(Path other) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TPath resolve(String other) {
        throw new UnsupportedOperationException("Not supported yet.");
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
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Returns a file system path for this path.
     * The returned file system path has a {@link FsPath#toUri() uri} which is
     * absolute and thus can be decomposed into a
     * {@link FsPath#getMountPoint() mount point} and an
     * {@link FsPath#getEntryName() entry name}.
     * 
     * @return a file system path for this path.
     */
    FsPath toFsPath() {
        return path;
    }

    @Override
    public URI toUri() {
        return uri;
    }

    @Override
    public TPath toAbsolutePath() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TPath toRealPath(LinkOption... options) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TFile toFile() {
        throw new UnsupportedOperationException("Not supported yet.");
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
    public boolean equals(final Object that) {
        return this == that
                || that instanceof TPath
                    && this.uri.equals(((TPath) that).uri);
    }

    @Override
    public int compareTo(Path that) {
        return this.uri.compareTo(((TPath) that).uri);
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }

    @Override
    public String toString() {
        return uri.toString();
    }
}
