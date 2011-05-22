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

import java.util.EnumMap;
import de.schlichtherle.truezip.entry.EntryFactory;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import de.schlichtherle.truezip.fs.FsEntry;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.CharConversionException;
import net.jcip.annotations.NotThreadSafe;

import static de.schlichtherle.truezip.entry.Entry.Type.*;

/**
 * An abstract archive file system entry which contains one or more
 * {@link FsArchiveEntry archive entries} of different
 * {@link FsArchiveEntry#isType(Type) types} in order to implement a
 * {@link FsEntry file system entry}.
 * 
 * @param   <E> The type of the decorated archive entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public abstract class FsArchiveFileSystemEntry<E extends FsArchiveEntry>
extends FsEntry implements Cloneable {

    private final String name;
    private Type type;
    private final EnumMap<Type, FsEntry> entries = new EnumMap<Type, FsEntry>(Type.class);

    public FsArchiveFileSystemEntry(final String name) {
        if (null == name)
            throw new NullPointerException();
        this.name = name;
    }

    @Override
    @SuppressWarnings("unchecked")
    public FsArchiveFileSystemEntry<E> clone() {
        try {
            return (FsArchiveFileSystemEntry<E>) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * Returns a <em>deep</em> clone of this archive file system entry.
     * 
     * @param  factory the archive entry factory to use for cloning the
     *         contained archive entries.
     * @return A deep clone of this archive file system entry.
     */
    FsArchiveFileSystemEntry<E> clone(EntryFactory<E> factory) {
        try {
            return create(getName(), getType(),
                    factory.newEntry(   delegate.getName(),
                                        delegate.getType(),
                                        delegate));
        } catch (CharConversionException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    void setType(Type type) {
        this.type = type;
    }

    @Override
    public Set<String> getMembers() {
        return entries.get(type).getMembers();
    }

    @Override
    public boolean isType(Type type) {
        return entries.containsKey(type);
    }

    @Override
    public long getSize(Size type) {
        return entries.get(this.type).getSize(type);
    }

    @Override
    public long getTime(Access type) {
        return entries.get(this.type).getTime(type);
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
    private static final class SpecialFileEntry<E extends FsArchiveEntry>
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
    private static final class NamedSpecialFileEntry<E extends FsArchiveEntry>
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
        FsArchiveFileSystemEntry<E> clone(EntryFactory<E> factory) {
            DirectoryEntry<E> clone = (DirectoryEntry<E>) super.clone(factory);
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
    private static final class SpecialDirectoryEntry<E extends FsArchiveEntry>
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
    private static final class NamedSpecialDirectoryEntry<E extends FsArchiveEntry>
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
    static final class HybridEntry<E extends FsArchiveEntry>
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
        FsArchiveFileSystemEntry<E> clone(EntryFactory<E> factory) {
            try {
                return new HybridEntry<E>(
                        factory.newEntry(   delegate.getName(),
                                            delegate.getType(),
                                            delegate),
                        file,
                        directory);
            } catch (CharConversionException ex) {
                throw new AssertionError(ex);
            }
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
