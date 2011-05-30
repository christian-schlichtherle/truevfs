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
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.fs.FsEntryName;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import de.schlichtherle.truezip.fs.FsInputOption;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.fs.FsPath;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
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
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
@edu.umd.cs.findbugs.annotations.SuppressWarnings("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
public final class TPath implements Path {

    private final TFileSystem fileSystem;
    private final FsEntryName entryName;

    TPath(final TFileSystem fileSystem, final FsEntryName entryName) {
        if (null == fileSystem || null == entryName)
            throw new NullPointerException();
        this.fileSystem = fileSystem;
        this.entryName = entryName;

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
        assert null != getFileSystem();
        assert null != toUri();
        return true;
    }

    @Override
    public TFileSystem getFileSystem() {
        return fileSystem;
    }

    public FsEntryName getEntryName() {
        return entryName;
    }

    @Override
    public boolean isAbsolute() {
        return true;
    }

    @Override
    public @Nullable TPath getRoot() {
        return new TPath(fileSystem, ROOT);
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
    public boolean startsWith(Path that) {
        if (this.getFileSystem() != that.getFileSystem())
            return false;
        return startsWith(((TPath) that).getEntryName().toString());
    }

    @Override
    public boolean startsWith(String other) {
        final String entryName = this.getEntryName().toString();
        final int ol = other.length();
        return entryName.startsWith(other)
                && (entryName.length() == ol
                    || entryName.charAt(ol) == SEPARATOR_CHAR);
    }

    @Override
    public boolean endsWith(Path that) {
        if (this.getFileSystem() != that.getFileSystem())
            return false;
        return endsWith(((TPath) that).getEntryName().toString());
    }

    @Override
    public boolean endsWith(String other) {
        final String entryName = this.getEntryName().toString();
        final int ol = other.length(), tl;
        return entryName.endsWith(other)
                && ((tl = entryName.length()) == ol
                    || entryName.charAt(tl - ol) == SEPARATOR_CHAR);
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
        return new TFile(toFsPath());
    }

    public FsPath toFsPath() {
        return new FsPath(getFileSystem().getMountPoint(), getEntryName());
    }

    @Override
    public URI toUri() {
        return getEntryName().toUri();
    }

    @Override
    public TPath toAbsolutePath() {
        return this;
    }

    @Override
    public TPath toRealPath(LinkOption... options) throws IOException {
        return this;
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
        return this.getFileSystem() == that.getFileSystem()
                && this.toUri().equals(that.toUri());
    }

    @Override
    public int compareTo(Path that) {
        if (this.getFileSystem() != that.getFileSystem())
            throw new ClassCastException("contract is contract - even if it's nonsense!"); // TODO
        return this.toUri().compareTo(that.toUri());
    }

    private volatile Integer hashCode;
    
    @Override
    public int hashCode() {
        if (null != hashCode)
            return hashCode;
        int hc = 17;
        hc = 37 * hc + getFileSystem().hashCode();
        hc = 37 * hc + toUri().hashCode();
        return hashCode = hc;
    }

    @Override
    public String toString() {
        return getEntryName().toString();
    }

    InputStream newInputStream(BitField<FsInputOption> options)
    throws IOException {
        return getFileSystem().newInputStream(this, options);
    }

    OutputStream newOutputStream(BitField<FsOutputOption> options, Entry template)
    throws IOException {
        return getFileSystem().newOutputStream(this, options, template);
    }
}
