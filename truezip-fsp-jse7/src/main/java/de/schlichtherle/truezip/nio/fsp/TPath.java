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
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileAttribute;
import java.util.Iterator;
import net.jcip.annotations.Immutable;

/**
 * A path implementation for the TrueZIP FileSystemProvider.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
@edu.umd.cs.findbugs.annotations.SuppressWarnings("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
public final class TPath implements Path {

    private static final FsMountPoint
            CURRENT_DIRECTORY = FsMountPoint.create(Paths.get("").toUri());

    private final TArchiveDetector detector;
    private final URI uri;
    private volatile FsPath path;
    private volatile TFileSystem fileSystem;
    private volatile Integer hashCode;

    public TPath(String first, String... more) {
        this((TArchiveDetector) null, first, more);
    }

    public TPath(final @CheckForNull TArchiveDetector detector, final String first, final String... more) {
        this.detector = null != detector ? detector : getDefaultArchiveDetector();
        this.uri = uri(first, more);

        assert invariants();
    }

    public TPath(TPath parent, String first, String... more) {
        this(parent, null, first, more);
    }

    public TPath(TPath parent, @CheckForNull TArchiveDetector detector, String first, String... more) {
        this(parent.getPath(), null != detector ? detector : parent.getArchiveDetector(), first, more);
    }

    TPath(final FsPath parent, final @CheckForNull TArchiveDetector detector, final String first, final String... more) {
        this.detector = null != detector ? detector : getDefaultArchiveDetector();
        final URI uri = uri(first, more);
        this.uri = parent.toUri().resolve(uri);
        this.path = new Scanner(parent, detector).toPath(uri);

        assert invariants();
    }

    public TPath(URI uri) {
        this(null, uri);
    }

    public TPath(final @CheckForNull TArchiveDetector detector, final URI uri) {
        if (null == uri)
            throw new NullPointerException();
        this.detector = null != detector ? detector : getDefaultArchiveDetector();
        this.uri = uri;

        assert invariants();
    }

    private TPath(final TArchiveDetector detector, final URI uri, final FsPath path) {
        this.detector = detector;
        this.uri = uri;
        this.path = path;

        assert invariants();
    }

    TPath(Path path) {
        this(null, path);
    }

    TPath(final @CheckForNull TArchiveDetector detector, final Path path) {
        this.detector = null != detector ? detector : getDefaultArchiveDetector();
        this.uri = new UriBuilder().path(path.toString().replace(path.getFileSystem().getSeparator(), SEPARATOR)).toUri();
    }

    private static URI uri(final String first, final String... more) {
        final StringBuilder pb = new StringBuilder(first);
        for (final String m : more)
            pb      .append(SEPARATOR_CHAR)
                    .append(m.replace(File.separatorChar, SEPARATOR_CHAR));
        return new UriBuilder().path(pb.toString()).toUri();
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
        assert null != getUri();
        assert null != getArchiveDetector();
        assert null != getPath();
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

    URI getUri() {
        return this.uri;
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

    FsPath getPath() {
        final FsPath p = this.path;
        return null != p ? p : (this.path = toPath(uri));
    }

    private FsPath toPath(URI uri) {
        return new Scanner(
                uri.isAbsolute()
                    ? TFileSystemProvider.get(this).getRoot()
                    : CURRENT_DIRECTORY,
                detector).toPath(uri);
    }

    @Override
    public TFileSystem getFileSystem() {
        final TFileSystem fs = this.fileSystem;
        return null != fs ? fs : (this.fileSystem = TFileSystem.get(this));
    }

    @Override
    public boolean isAbsolute() {
        return true;
    }

    @Override
    public @Nullable TPath getRoot() {
        return new TPath("/");
    }

    @Override
    public TPath getFileName() {
        final URI uri = getUri();
        final URI parent = uri.resolve(".");
        final URI member = parent.relativize(uri);
        return new TPath(getArchiveDetector(), member);
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
    public boolean startsWith(Path that) {
        if (this.getFileSystem() != that.getFileSystem())
            return false;
        return startsWith(((TPath) that).getPath().getEntryName().toString());
    }

    @Override
    public boolean startsWith(String other) {
        final String name = this.getPath().getEntryName().toString();
        final int ol = other.length();
        return name.startsWith(other)
                && (name.length() == ol
                    || SEPARATOR_CHAR == name.charAt(ol));
    }

    @Override
    public boolean endsWith(Path that) {
        if (this.getFileSystem() != that.getFileSystem())
            return false;
        return endsWith(((TPath) that).getPath().getEntryName().toString());
    }

    @Override
    public boolean endsWith(String other) {
        final String name = this.getPath().getEntryName().toString();
        final int ol = other.length(), tl;
        return name.endsWith(other)
                && ((tl = name.length()) == ol
                    || SEPARATOR_CHAR == name.charAt(tl - ol));
    }

    @Override
    public TPath normalize() {
        return this;
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

    @Override
    public TFile toFile() {
        final URI uri = getUri();
        return uri.isAbsolute() ? new TFile(getPath()) : new TFile(uri);
    }

    @Override
    public URI toUri() {
        return getPath().toHierarchicalUri();
    }

    @Override
    public TPath toAbsolutePath() {
        return new TPath(getArchiveDetector(), toUri(), getPath());
    }

    @Override
    public TPath toRealPath(LinkOption... options) throws IOException {
        // FIXME: resolve symlinks!
        return new TPath(getArchiveDetector(), toUri(), getPath());
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
    public boolean equals(final Object other) {
        if (this == other)
            return true;
        if (!(other instanceof Path))
            return false;
        final Path that = (Path) other;
        return this.toUri().equals(that.toUri());
    }

    @Override
    public int compareTo(Path that) {
        return this.toUri().compareTo(that.toUri());
    }
    
    @Override
    public int hashCode() {
        final Integer hashCode = this.hashCode;
        return null != hashCode ? hashCode : (this.hashCode = toUri().hashCode());
    }

    @Override
    public String toString() {
        return getUri().toString();
    }

    FsController<?> getController() throws IOException {
        return getFileSystem().getController(getArchiveDetector());
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
