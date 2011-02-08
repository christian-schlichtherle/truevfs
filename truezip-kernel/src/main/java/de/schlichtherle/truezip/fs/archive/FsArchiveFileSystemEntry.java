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
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.entry.Entry.Access;
import de.schlichtherle.truezip.entry.Entry.Size;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsDecoratingEntry;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import de.schlichtherle.truezip.fs.FsEntry;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.NotThreadSafe;

import static de.schlichtherle.truezip.entry.Entry.Type.*;

/**
 * An abstract archive file system entry which adapts an
 * {@link FsArchiveEntry archive entry} to a {@link FsEntry file system entry}.
 * 
 * @param   <E> The type of the decorated archive entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public abstract class FsArchiveFileSystemEntry<E extends FsArchiveEntry>
extends FsDecoratingEntry<E> {

    /**
     * Constructs a new archive file system entry which decorates the given
     * archive entry.
     */
    public static <E extends FsArchiveEntry>
    FsArchiveFileSystemEntry<E> create( final FsEntryName name,
                                        final Type        type,
                                        final E           entry) {
        return create(name.getPath(), type, entry);
    }

    private static <E extends FsArchiveEntry>
    FsArchiveFileSystemEntry<E> create( final String path,
                                        final Type   type,
                                        final E      entry) {
        switch (type) {
            case FILE:
                assert FILE == entry.getType();
                return path.equals(entry.getName())
                        ? new      FileEntry<E>(entry)
                        : new NamedFileEntry<E>(entry, path);

            case DIRECTORY:
                assert DIRECTORY == entry.getType();
                return path.equals(entry.getName())
                        ? new      DirectoryEntry<E>(entry)
                        : new NamedDirectoryEntry<E>(entry, path);

            case SPECIAL:
                return path.equals(entry.getName())
                        ? DIRECTORY == entry.getType()
                            ? new      SpecialDirectoryEntry<E>(entry)
                            : new           SpecialFileEntry<E>(entry)
                        : DIRECTORY == entry.getType()
                            ? new NamedSpecialDirectoryEntry<E>(entry, path)
                            : new      NamedSpecialFileEntry<E>(entry, path);

            default:
                throw new UnsupportedOperationException(entry + " (type not supported)");
        }
    }

    /** Constructs a new instance of {@code Entry}. */
    private FsArchiveFileSystemEntry(E entry) {
        super(entry);
    }

    FsArchiveFileSystemEntry<E> clone(FsArchiveFileSystem<E> fileSystem) {
        return create(getName(), getType(), fileSystem.copy(delegate));
    }

    /**
     * Returns the archive entry which is adapted by this archive file system
     * entry.
     *
     * @return The archive entry which is adapted by this archive file system
     *         entry.
     */
    public final E getEntry() {
        return delegate;
    }

    /**
     * Adds the given base path to the set of members of this directory
     * if and only if this file system entry is a directory.
     *
     * @param  member The non-{@code null} base path of the member to add.
     * @return Whether the member has been added or an equal member was
     *         already present in the directory.
     * @throws UnsupportedOperationException if this file system entry is
     *         not a directory.
     */
    boolean add(String member) {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes the given base path from the set of members of this
     * directory
     * if and only if this file system entry is a directory.
     *
     * @param  member The non-{@code null} base path of the member to
     *         remove.
     * @return Whether the member has been removed or no equal member was
     *         present in the directory.
     * @throws UnsupportedOperationException if this file system entry is
     *         not a directory.
     */
    boolean remove(String member) {
        throw new UnsupportedOperationException();
    }

    /** A file entry. */
    private static class FileEntry<E extends FsArchiveEntry>
    extends FsArchiveFileSystemEntry<E> {
        /** Decorates the given archive entry. */
        FileEntry(final E entry) {
            super(entry);
        }

        @Override
        public Type getType() {
            return FILE;
        }

        @Override
        public Set<String> getMembers() {
            return null;
        }
    } // class FileEntry

    /** A named file entry. */
    private static class NamedFileEntry<E extends FsArchiveEntry>
    extends FileEntry<E> {
        final String path;

        /** Decorates the given archive entry. */
        NamedFileEntry(final E entry, final String path) {
            super(entry);
            assert !path.equals(entry.getName());
            this.path = path;
        }

        @Override
        public final String getName() {
            return path;
        }
    } // class NamedFileEntry

    /** A named special file entry. */
    private static class SpecialFileEntry<E extends FsArchiveEntry>
    extends FileEntry<E> {
        SpecialFileEntry(E entry) {
            super(entry);
        }

        @Override
        public Type getType() {
            return SPECIAL;
        }
    } // class SpecialFileEntry

    /** A named special file entry. */
    private static class NamedSpecialFileEntry<E extends FsArchiveEntry>
    extends NamedFileEntry<E> {
        NamedSpecialFileEntry(E entry, String path) {
            super(entry, path);
        }

        @Override
        public Type getType() {
            return SPECIAL;
        }
    } // class NamedSpecialFileEntry

    /** A directory entry. */
    private static class DirectoryEntry<E extends FsArchiveEntry>
    extends FsArchiveFileSystemEntry<E> {
        Set<String> members = new LinkedHashSet<String>();

        /** Decorates the given archive entry. */
        DirectoryEntry(final E entry) {
            super(entry);
        }

        @Override
        FsArchiveFileSystemEntry<E> clone(FsArchiveFileSystem<E> fileSystem) {
            DirectoryEntry<E> clone = (DirectoryEntry<E>) super.clone(fileSystem);
            clone.members = Collections.unmodifiableSet(members);
            return clone;
        }

        @Override
        public Type getType() {
            return DIRECTORY;
        }

        @Override
        public Set<String> getMembers() {
            return members;
        }

        @Override
        boolean add(String member) {
            return members.add(member);
        }

        @Override
        boolean remove(String member) {
            return members.remove(member);
        }
    } // class DirectoryEntry

    /** A named directory entry. */
    private static class NamedDirectoryEntry<E extends FsArchiveEntry>
    extends DirectoryEntry<E> {
        final String path;

        /** Decorates the given archive entry. */
        NamedDirectoryEntry(final E entry, final String path) {
            super(entry);
            assert !path.equals(entry.getName());
            this.path = path;
        }

        @Override
        public final String getName() {
            return path;
        }
    } // class NamedDirectoryEntry

    /** A named special directory entry. */
    private static class SpecialDirectoryEntry<E extends FsArchiveEntry>
    extends DirectoryEntry<E> {
        SpecialDirectoryEntry(E entry) {
            super(entry);
        }

        @Override
        public Type getType() {
            return SPECIAL;
        }
    } // class SpecialDirectoryEntry

    /** A named special file entry. */
    private static class NamedSpecialDirectoryEntry<E extends FsArchiveEntry>
    extends NamedDirectoryEntry<E> {
        NamedSpecialDirectoryEntry(E entry, String path) {
            super(entry, path);
        }

        @Override
        public Type getType() {
            return SPECIAL;
        }
    } // class NamedSpecialDirectoryEntry

    /** A hybrid file entry. */
    static class HybridEntry<E extends FsArchiveEntry>
    extends FsArchiveFileSystemEntry<E> {
        final FsArchiveFileSystemEntry<E> file, directory;

        HybridEntry(    final E delegate,
                        final FsArchiveFileSystemEntry<E> file,
                        final FsArchiveFileSystemEntry<E> directory) {
            super(delegate);
            this.file = file;
            this.directory = directory;
        }

        @Override
        FsArchiveFileSystemEntry<E> clone(FsArchiveFileSystem<E> fileSystem) {
            return new HybridEntry<E>(fileSystem.copy(delegate), file, directory);
        }

        @Override
        public String getName() {
            return file.getName();
        }

        @Override
        public Type getType() {
            return HYBRID;
        }

        @Override
        public long getSize(Size type) {
            return file.getSize(type);
        }

        @Override
        public long getTime(Access type) {
            return Math.max(file.getTime(type), directory.getTime(type));
        }

        @Override
        public Set<String> getMembers() {
            return directory.getMembers();
        }
    }
}
