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
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.fs.archive.FsArchiveDetector;
import de.schlichtherle.truezip.fs.FsCompositeDriver;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsEntry;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsInputOption;
import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.FsMountPoint;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.fs.FsOutputOptions;
import de.schlichtherle.truezip.fs.FsSyncException;
import de.schlichtherle.truezip.fs.FsUriModifier;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import static de.schlichtherle.truezip.fs.FsManager.*;
import de.schlichtherle.truezip.fs.FsPath;
import de.schlichtherle.truezip.fs.sl.FsManagerLocator;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.UriBuilder;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.IOException;
import static java.io.File.*;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class TFileSystem extends FileSystem {

    private static final FsManager manager = FsManagerLocator.SINGLETON.get();
    private static final FsCompositeDriver driver = FsArchiveDetector.ALL; // new FsDefaultDriver(FsDriverLocator.SINGLETON);

    private final TFileSystemProvider provider;
    private final FsMountPoint mountPoint;

    TFileSystem(TFileSystemProvider provider, FsMountPoint mountPoint) {
        if (null == provider || null == mountPoint)
            throw new NullPointerException();
        this.provider = provider;
        this.mountPoint = mountPoint;
    }

    FsEntryName resolveParent(FsEntryName name) {
        return mountPoint.getPath().resolve(name).getEntryName();
    }

    @Override
    public TFileSystemProvider provider() {
        return provider;
    }

    public FsMountPoint getMountPoint() {
        return mountPoint;
    }

    @Override
    public void close() throws FsSyncException {
        manager.sync(UMOUNT);
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public String getSeparator() {
        return SEPARATOR;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TPath getPath(String first, String... more) {
        return new TPath(this, FsEntryName.create(
                toUri(first, more),
                FsUriModifier.CANONICALIZE));
    }

    public TPath getPath(FsEntryName name) {
        return new TPath(this, name);
    }

    static URI toUri(final String first, final String... more) {
        final StringBuilder pb = new StringBuilder(first);
        for (final String m : more)
            pb      .append(SEPARATOR_CHAR)
                    .append(m.replace(separatorChar, SEPARATOR_CHAR));
        return new UriBuilder().path(pb.toString()).toUri();
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /*ReadOnlyFile newReadOnlyFile(   TPath path,
                                    BitField<FsInputOption> options)
    throws IOException {
        FsEntryName entryName = path.getEntryName();
        return manager
                .getController(mountPoint, driver)
                .getInputSocket(entryName, options)
                .newReadOnlyFile();
    }*/

    FsEntry getEntry(TPath path) throws IOException {
        FsEntryName name = path.getEntryName();
        return getController().getEntry(name);
    }

    InputSocket<?> getInputSocket(  TPath path,
                                    BitField<FsInputOption> options) {
        FsEntryName name = path.getEntryName();
        return getController()
                .getInputSocket(name, options);
    }

    OutputSocket<?> getOutputSocket(TPath path,
                                    BitField<FsOutputOption> options,
                                    @CheckForNull Entry template) {
        FsEntryName name = path.getEntryName();
        return getController()
                .getOutputSocket(name, options, template);
    }

    DirectoryStream<Path> newDirectoryStream(   final TPath path,
                                                final Filter<? super Path> filter)
    throws IOException {
        final FsEntryName name = path.getEntryName();
        final FsEntry entry = getController().getEntry(name);
        final Set<String> set;
        if (null == entry || null == (set = entry.getMembers()))
            throw new NotDirectoryException(path.toString());

        class Adapter implements Iterator<Path> {
            final UriBuilder uri = new UriBuilder();
            final Iterator<String> i = set.iterator();
            Path next;

            @Override
            public boolean hasNext() {
                while (i.hasNext()) {
                    next = new TPath(TFileSystem.this, new FsEntryName(
                            path.getEntryName(),
                            FsEntryName.create(uri.path(i.next()).toUri())));
                    try {
                        if (filter.accept(next))
                            return true;
                    } catch (IOException ignored) {
                    }
                }
                next = null;
                return false;
            }

            @Override
            public Path next() {
                if (null == next)
                    throw new NoSuchElementException();
                return next;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }

        class Stream implements DirectoryStream<Path> {
            @Override
            public Iterator<Path> iterator() {
                return new Adapter();
            }

            @Override
            public void close() {
            }
        }

        return new Stream();
    }

    void createDirectory(   TPath path,
                            FileAttribute<?>... attrs)
    throws IOException {
        if (0 < attrs.length)
            throw new UnsupportedOperationException();
        FsEntryName name = path.getEntryName();
        getController()
                .mknod(name, Type.DIRECTORY, FsOutputOptions.NO_OUTPUT_OPTION, null);
    }

    void delete(TPath path) throws IOException {
        FsEntryName name = path.getEntryName();
        getController()
                .unlink(name);
    }

    private volatile FsController<?> controller;

    FsController<?> getController() {
        final FsController<?> controller = this.controller;
        return null != controller
                ? controller
                : (this.controller = manager.getController(mountPoint, driver));
    }
}
