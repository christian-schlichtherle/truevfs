/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.archive.filesystem;

import de.schlichtherle.truezip.io.filesystem.DecoratingFileSystemEntry;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.filesystem.FileSystemEntry;
import de.schlichtherle.truezip.io.filesystem.FileSystemEntryName;
import edu.umd.cs.findbugs.annotations.NonNull;

import static de.schlichtherle.truezip.io.entry.Entry.Type.*;

/**
 * Adapts an {@link ArchiveEntry} to a {@link FileSystemEntry}.
 * 
 * @param   <E> The type of the decorated archive entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class ArchiveFileSystemEntry<E extends ArchiveEntry>
extends DecoratingFileSystemEntry<E> {

    /**
     * Constructs a new archive file system entry which decorates the given
     * archive entry.
     */
    @NonNull
    public static <E extends ArchiveEntry>
    ArchiveFileSystemEntry<E> create(   @NonNull final FileSystemEntryName name,
                                        @NonNull final Type type,
                                        @NonNull final E entry) {
        return create(name.getPath(), type, entry);
    }

    @NonNull
    static <E extends ArchiveEntry>
    ArchiveFileSystemEntry<E> create(   @NonNull final String path,
                                        @NonNull final Type type,
                                        @NonNull final E entry) {
        switch (type) {
            case FILE:
                assert FILE == entry.getType();
                return path.equals(entry.getName())
                        ? new      FileEntry<E>(      entry)
                        : new NamedFileEntry<E>(path, entry);

            case DIRECTORY:
                assert DIRECTORY == entry.getType();
                return path.equals(entry.getName())
                        ? new      DirectoryEntry<E>(      entry)
                        : new NamedDirectoryEntry<E>(path, entry);

            case SPECIAL:
                return new NamedSpecialFileEntry<E>(path, entry);

            default:
                throw new UnsupportedOperationException(entry + " (type not supported)");
        }
    }

    /** Constructs a new instance of {@code Entry}. */
    private ArchiveFileSystemEntry(@NonNull E entry) {
        super(entry);
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

    ArchiveFileSystemEntry<E> clone(ArchiveFileSystem<E> fileSystem) {
        return create(getName(), getType(), fileSystem.copy(delegate));
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
    boolean add(@NonNull String member) {
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
    boolean remove(@NonNull String member) {
        throw new UnsupportedOperationException();
    }

    /** A file entry. */
    private static class FileEntry<E extends ArchiveEntry>
    extends ArchiveFileSystemEntry<E> {
        /** Decorates the given archive entry. */
        FileEntry(final E entry) {
            super(entry);
        }

        @Override
        @NonNull
        public String getName() {
            return delegate.getName();
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
    private static class NamedFileEntry<E extends ArchiveEntry>
    extends FileEntry<E> {
        final String path;

        /** Decorates the given archive entry. */
        NamedFileEntry(final String path, final E entry) {
            super(entry);
            assert !path.equals(entry.getName());
            this.path = path;
        }

        @Override
        public String getName() {
            return path;
        }
    } // class NamedFileEntry

    /** A directory entry. */
    private static class DirectoryEntry<E extends ArchiveEntry>
    extends ArchiveFileSystemEntry<E> {
        Set<String> members = new LinkedHashSet<String>();

        /** Decorates the given archive entry. */
        DirectoryEntry(final E entry) {
            super(entry);
        }

        @Override
        ArchiveFileSystemEntry<E> clone(final ArchiveFileSystem<E> fileSystem) {
            final DirectoryEntry<E> clone = (DirectoryEntry<E>) super.clone(fileSystem);
            clone.members = Collections.unmodifiableSet(members);
            return clone;
        }

        @Override
        @NonNull
        public String getName() {
            return delegate.getName();
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
    private static class NamedDirectoryEntry<E extends ArchiveEntry>
    extends DirectoryEntry<E> {
        final String path;

        /** Decorates the given archive entry. */
        NamedDirectoryEntry(final String path, final E entry) {
            super(entry);
            assert !path.equals(entry.getName());
            this.path = path;
        }

        @Override
        public String getName() {
            return path;
        }
    } // class NamedDirectoryEntry

    /** A named special file entry. */
    private static class NamedSpecialFileEntry<E extends ArchiveEntry>
    extends ArchiveFileSystemEntry<E> {
        final String path;

        NamedSpecialFileEntry(final String path, final E entry) {
            super(entry);
            //assert SPECIAL == entry.getType(); // drivers could ignore this type, so we must ignore this!
            final String name = entry.getName();
            this.path = name.equals(path) ? name : path;
        }

        @Override
        public String getName() {
            return path;
        }

        @Override
        public Type getType() {
            return SPECIAL;
        }

        @Override
        public Set<String> getMembers() {
            return null;
        }
    } // class NamedSpecialFileEntry
}
