/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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

import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.io.entry.Entry.Access;
import de.schlichtherle.truezip.io.entry.FilterEntry;
import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.entry.Entry.Type;
import de.schlichtherle.truezip.io.entry.EntryContainer;
import de.schlichtherle.truezip.io.entry.EntryFactory;
import de.schlichtherle.truezip.io.Paths;
import de.schlichtherle.truezip.util.Link;
import java.io.CharConversionException;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static de.schlichtherle.truezip.io.entry.Entry.Access.WRITE;
import static de.schlichtherle.truezip.io.entry.Entry.ROOT;
import static de.schlichtherle.truezip.io.entry.Entry.SEPARATOR;
import static de.schlichtherle.truezip.io.entry.Entry.SEPARATOR_CHAR;
import static de.schlichtherle.truezip.io.entry.Entry.UNKNOWN;
import static de.schlichtherle.truezip.io.entry.Entry.Type.DIRECTORY;
import static de.schlichtherle.truezip.io.entry.Entry.Type.FILE;
import static de.schlichtherle.truezip.io.Paths.cutTrailingSeparators;
import static de.schlichtherle.truezip.io.Paths.isRoot;

/**
 * A base class for a virtual file system for archive entries.
 * <p>
 * This class is <em>not</em> thread-safe!
 * Multithreading needs to be addressed by client classes.
 * 
 * @param   <AE> The type of the archive entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class ArchiveFileSystem<AE extends ArchiveEntry>
implements EntryContainer<ArchiveFileSystemEntry<AE>> {

    /** The controller that this filesystem belongs to. */
    private final EntryFactory<AE> factory;

    /**
     * The map of archive entries in this file system.
     * If this is a read-only file system, this is actually an unmodifiable
     * map.
     * This field should be considered final!
     * <p>
     * Note that the archive entries in this map are shared with the
     * {@link EntryContainer} object provided to the constructor of
     * this class.
     */
    private Map<String, BaseEntry<AE>> master;

    /** The file system entry for the (virtual) root of this file system. */
    private final BaseEntry<AE> root;

    /** Whether or not this file system has been modified (touched). */
    private boolean touched;

    private LinkedHashSet<ArchiveFileSystemTouchListener<? super AE>> touchListeners
            = new LinkedHashSet<ArchiveFileSystemTouchListener<? super AE>>();

    /**
     * Returns a new archive file system and ensures its integrity.
     * The root directory is created with its last modification time set to
     * the system's current time.
     * The file system is modifiable and marked as touched!
     *
     * @param  factory the archive entry factory to use.
     * @throws NullPointerException If {@code factory} is {@code null}.
     */
    public static <AE extends ArchiveEntry>
    ArchiveFileSystem<AE> newArchiveFileSystem(EntryFactory<AE> factory) {
        return new ArchiveFileSystem<AE>(factory);
    }

    private ArchiveFileSystem(final EntryFactory<AE> factory) {
        assert factory != null;

        this.factory = factory;
        master = new LinkedHashMap<String, BaseEntry<AE>>(64);

        // Setup root.
        root = newEntryUnchecked(ROOT, DIRECTORY, null);
        for (Access access : BitField.allOf(Access.class))
            root.getTarget().setTime(access, System.currentTimeMillis());
        master.put(ROOT, root);
        try {
            touch();
        } catch (ArchiveFileSystemException ex) {
            throw new AssertionError("veto without a listener!?");
        }
    }

    /**
     * Returns a new archive file system which populates its entries from
     * the given {@code archive} and ensures its integrity.
     * <p>
     * First, the entries from the archive are loaded into the file system.
     * <p>
     * Second, a root directory with the given last modification time is
     * created and linked into the filesystem (so it's never loaded from the
     * archive).
     * <p>
     * Finally, the file system integrity is checked and fixed: Any missing
     * parent directories are created using the system's current time as their
     * last modification time - existing directories will never be replaced.
     * <p>
     * Note that the entries in the file system are shared with the given
     * archive entry {@code container}.
     *
     * @param  container The archive entry container to read the entries for
     *         the population of the file system.
     * @param  factory the archive entry factory to use.
     * @param  rootTemplate The last modification time of the root of the populated
     *         file system in milliseconds since the epoch.
     * @param  readOnly If and only if {@code true}, any subsequent
     *         modifying operation on the file system will result in a
     *         {@link ReadOnlyArchiveFileSystemException}.
     * @throws NullPointerException If {@code container}, {@code factory} or
     *         {@code rootTemplate} are {@code null}.
     * @throws IllegalArgumentException If {@code rootTemplate} is an instance
     *         of {@link ArchiveFileSystemEntry}.
     */
    public static <AE extends ArchiveEntry>
    ArchiveFileSystem<AE> newArchiveFileSystem(
            EntryContainer<AE> container,
            EntryFactory<AE> factory,
            Entry rootTemplate,
            boolean readOnly) {
        return readOnly
            ? new ReadOnlyArchiveFileSystem<AE>(container, factory, rootTemplate)
            : new ArchiveFileSystem<AE>(container, factory, rootTemplate);
    }

    ArchiveFileSystem(
            final EntryContainer<AE> container,
            final EntryFactory<AE> factory,
            final Entry rootTemplate) {
        if (null == rootTemplate)
            throw new NullPointerException();
        if (rootTemplate instanceof ArchiveFileSystemEntry<?>)
            throw new IllegalArgumentException();

        this.factory = factory;
        master = new LinkedHashMap<String, BaseEntry<AE>>(
                (int) (container.size() / .75f) + 1);

        // Load entries from input archive.
        final Normalizer normalizer = new Normalizer();
        for (final AE entry : container) {
            final String path = normalizer.normalize(entry.getName());
            master.put(path, newEntry(path, entry));
        }

        // Setup root file system entry, potentially replacing its previous
        // mapping from the input archive.
        root = newEntryUnchecked(ROOT, DIRECTORY, rootTemplate);
        master.put(ROOT, root);

        // Now perform a file system check to create missing parent directories
        // and populate directories with their children - this needs to be done
        // separately!
        // entries = Collections.enumeration(master.values()); // concurrent modification!
        final Check fsck = new Check();
        for (final AE entry : container) {
            final String path = normalizer.normalize(entry.getName());
            if (isValidPath(path))
                fsck.fix(path);
        }
    }

    private static class Normalizer
    extends de.schlichtherle.truezip.io.Paths.Normalizer {
        Normalizer() {
            super(SEPARATOR_CHAR);
        }

        @Override
        public String normalize(String path) {
            return cutTrailingSeparators(super.normalize(path), SEPARATOR_CHAR);
        }
    }

    /**
     * Checks whether the given path name is a <i>valid path name</i>.
     * A valid path name is in
     * {@link Paths#normalize(String, char) normal form},
     * is relative, does not identify the dot directory ({@code "."}) or
     * the dot-dot directory ({@code ".."}) or any of their descendants.
     *
     * @see    EntryFactory#newEntry Common Requirements For Operation Names
     * @param  name a non-{@code null} path name.
     */
    private static boolean isValidPath(final String name) {
        if (isRoot(name))
            return true;

        if (name != new Normalizer().normalize(name)) // mind contract!
            return false;

        final int length = name.length();
        assert length > 0 || isRoot(name);
        switch (name.charAt(0)) {
        case SEPARATOR_CHAR:
            return false; // not a relative path name

        case '.':
            if (length >= 2) {
                switch (name.charAt(1)) {
                case '.':
                    if (length >= 3) {
                        if (name.charAt(2) == SEPARATOR_CHAR) {
                            assert name.startsWith(".." + SEPARATOR);
                            return false;
                        }
                        // Fall through.
                    } else {
                        assert "..".equals(name);
                        return false;
                    }
                    break;

                case SEPARATOR_CHAR:
                    assert name.startsWith("." + SEPARATOR);
                    return false;

                default:
                    // Fall through.
                }
            } else {
                assert ".".equals(name);
                return false;
            }
            break;

        default:
            // Fall through.
        }

        return true;
    }

    /** Splits a path name into a parent path name and a base path. */
    private static class Splitter
    extends de.schlichtherle.truezip.io.Paths.Splitter {
        Splitter() {
            super(SEPARATOR_CHAR);
        }

        /**
         * Splits the given path name into a parent path name and a base path.
         * Iff the given path name does not path a parent directory, then
         * {@link ArchiveEntry#ROOT} is set at index zero of the returned array.
         *
         * @param  path The path name which's parent path name and base path
         *         are to be returned.
         * @throws NullPointerException If {@code path} is {@code null}.
         */
        @Override
        public String[] split(String path) {
            final String split[] = super.split(path);
            if (split[0] == null)
                split[0] = ROOT; // postfix
            return split;
        }
    }

    private class Check extends Splitter {
        /**
         * Called from a constructor to fix the parent directories of the
         * file system entry identified by {@code path}, ensuring that all
         * parent directories of the file system entry exist and that they
         * contain the respective member.
         * If a parent directory does not exist, it is created using an
         * unkown time as the last modification time - this is defined to be a
         * <i>ghost directory<i>.
         * If a parent directory does exist, the respective member is added
         * (possibly yet again) and the process is continued.
         */
        void fix(final String path) {
            // When recursing into this method, it may be called with the root
            // directory as its parameter, so we may NOT skip the following test.
            if (isRoot(path))
                return; // never fix root or empty or absolute pathnames
            assert isValidPath(path);

            split(path);
            final String parentPath = getParentPath();
            final String memberName = getMemberName();
            BaseEntry<AE> parent = master.get(parentPath);
            if (parent == null) {
                parent = newEntryUnchecked(parentPath, DIRECTORY, null);
                master.put(parentPath, parent);
            }
            parent.add(memberName);
            fix(parentPath);
        }
    }

    /**
     * Returns {@code true} if and only if this archive file system is
     * read-only.
     * This method is provided for inheritance - the implementation in this
     * class always returns {@code false}.
     */
    public boolean isReadOnly() {
        return false;
    }

    /**
     * Returns {@code true} if and only if this archive file system has been
     * modified since its time of creation.
     */
    public boolean isTouched() {
        return touched;
    }

    /**
     * Ensures that the controller's data structures required to output
     * entries are properly initialized and marks this (virtual) archive
     * file system as touched.
     *
     * @throws ArchiveReadOnlyExceptionn If this (virtual) archive file system
     *         is read only.
     * @throws ArchiveFileSystemException If the listener vetoed the beforeTouch
     *         operation for any reason.
     */
    private void touch() throws ArchiveFileSystemException {
        if (touched)
            return;
        // Order is important here because of veto exceptions!
        final ArchiveFileSystemEvent<AE> event
                = new ArchiveFileSystemEvent<AE>(this);
        final Iterable<ArchiveFileSystemTouchListener<? super AE>> listeners
                = getArchiveFileSystemTouchListeners();
        try {
            for (ArchiveFileSystemTouchListener<? super AE> listener : listeners)
                listener.beforeTouch(event);
        } catch (IOException ex) {
            throw new ArchiveFileSystemException(null, "touch vetoed", ex);
        }
        touched = true;
        for (ArchiveFileSystemTouchListener<? super AE> listener : listeners)
            listener.afterTouch(event);
    }

    /**
     * Returns a protective copy of the set of archive file system listeners.
     *
     * @return A clone of the set of archive file system listeners.
     */
    @SuppressWarnings("unchecked")
    final Set<ArchiveFileSystemTouchListener<? super AE>>
    getArchiveFileSystemTouchListeners() {
        return (Set<ArchiveFileSystemTouchListener<? super AE>>) touchListeners.clone();
    }

    /**
     * Adds the given listener to the set of archive file system listeners.
     *
     * @param  listener the non-{@code null} listener for archive file system
     *         events.
     * @throws NullPointerException if {@code listener} is {@code null}.
     */
    public final void addArchiveFileSystemTouchListener(
            final ArchiveFileSystemTouchListener<? super AE> listener) {
        if (null == listener)
            throw new NullPointerException();
        touchListeners.add(listener);
    }

    /**
     * Removes the given listener from the set of archive file system listeners.
     *
     * @param  listener the non-{@code null} listener for archive file system
     *         events.
     * @throws NullPointerException if {@code listener} is {@code null}.
     */
    public final void removeArchiveFileSystemTouchListener(
            final ArchiveFileSystemTouchListener<? super AE> listener) {
        if (null == listener)
            throw new NullPointerException();
        touchListeners.remove(listener);
    }

    @Override
    public int size() {
        return master.size();
    }

    @Override
    public Iterator<ArchiveFileSystemEntry<AE>> iterator() {
        class ArchiveEntryIterator implements Iterator<ArchiveFileSystemEntry<AE>> {
            final Iterator<BaseEntry<AE>> it = master.values().iterator();

            @Override
			public boolean hasNext() {
                return it.hasNext();
            }

            @Override
			public ArchiveFileSystemEntry<AE> next() {
                return it.next();
            }

            @Override
			public void remove() {
                throw new UnsupportedOperationException();
            }
        }
        return new ArchiveEntryIterator();
    }

    @Override
    public ArchiveFileSystemEntry<AE> getEntry(String path) {
        if (path == null)
            throw new NullPointerException();
        final BaseEntry<AE> entry = master.get(path);
        return null == entry ? null : entry.clone(factory);
    }

    /**
     * Like {@link #newEntryChecked(String, Entry.Type, Entry)
     * newEntry(path, type, null)}, but throws an
     * {@link AssertionError} instead of a {@link CharConversionException}.
     *
     * @throws IllegalArgumentException if a {@link CharConversionException}
     *         occurs. The original exception is wrapped as its cause.
     */
    private BaseEntry<AE> newEntryUnchecked(
            final String path,
            final Type type,
            final Entry template) {
        assert isValidPath(path);
        assert type != null;
        assert !isRoot(path) || type == DIRECTORY;
        assert !(template instanceof ArchiveFileSystemEntry<?>);

        try {
            return newEntry(path, factory.newEntry(path, type, template));
        } catch (CharConversionException ex) {
            throw new IllegalArgumentException(path, ex);
        }
    }

    /**
     * Returns a new file system entry for this (virtual) archive file system.
     * This is only a factory method, i.e. the returned file system entry is
     * not yet linked into this (virtual) archive file system.
     *
     * @see    #mknod
     * @param  path the non-{@code null} path name of the archive file system entry.
     *         This is always a {@link #isValidPath(String) valid path name}.
     */
    private BaseEntry<AE> newEntryChecked(
            final String path,
            final Type type,
            final Entry template)
    throws ArchiveFileSystemException {
        assert isValidPath(path);
        assert type != null;
        assert !isRoot(path) || type == DIRECTORY;
        assert !(template instanceof ArchiveFileSystemEntry<?>);

        try {
            return newEntry(path, factory.newEntry(path, type, template));
        } catch (CharConversionException ex) {
            throw new ArchiveFileSystemException(path, ex);
        }
    }

    /**
     * Constructs a new instance of {@code Entry}
     * which decorates (wraps) the given archive entry.
     *
     * @throws NullPointerException If {@code entry} is {@code null}.
     */
    private static <AE extends ArchiveEntry>
    BaseEntry<AE> newEntry(final String path, final AE entry) {
        return DIRECTORY == entry.getType()
                ? path.equals(entry.getName())
                    ? new      DirectoryEntry<AE>(      entry)
                    : new NamedDirectoryEntry<AE>(path, entry)
                : path.equals(entry.getName())
                    ? new           FileEntry<AE>(      entry)
                    : new      NamedFileEntry<AE>(path, entry);
    }

    /**
     * Defines the common features of all entries in this archive file system.
     * It decorates an {@link ArchiveEntry} in order to add the methods
     * required to implement the concept of a directory.
     */
    private static abstract class BaseEntry<AE extends ArchiveEntry>
    extends FilterEntry<AE>
    implements ArchiveFileSystemEntry<AE>, Cloneable {
        /** Constructs a new instance of {@code Entry}. */
        BaseEntry(final AE entry) {
            super(entry);
            assert entry != null;
        }

        @SuppressWarnings("unchecked")
        BaseEntry<AE> clone(final EntryFactory<AE> factory) {
            final BaseEntry<AE> clone;
            try {
                clone = (BaseEntry<AE>) clone();
            } catch (CloneNotSupportedException cannotHappen) {
                throw new AssertionError(cannotHappen);
            }
            final AE entry = clone.entry;
            try {
                clone.entry = factory.newEntry(entry.getName(), entry.getType(), entry);
            } catch (CharConversionException cannotHappen) {
                throw new AssertionError(cannotHappen);
            }
            return clone;
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
        boolean add(final String member) {
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
        boolean remove(final String member) {
            throw new UnsupportedOperationException();
        }

        /** Returns the decorated archive entry. */
        @Override
        public final AE getTarget() {
            return entry;
        }
    } // class Entry

    /** A file entry. */
    private static class FileEntry<AE extends ArchiveEntry>
    extends BaseEntry<AE> {
        /** Decorates the given archive entry. */
        FileEntry(final AE entry) {
            super(entry);
            assert entry.getType() != DIRECTORY;
        }

        @Override
        public Set<String> getMembers() {
            return null;
        }
    } // class FileEntry

    /** A named file entry. */
    private static class NamedFileEntry<AE extends ArchiveEntry>
    extends FileEntry<AE> {
        final String path;

        /** Decorates the given archive entry. */
        NamedFileEntry(final String path, final AE entry) {
            super(entry);
            assert entry.getType() != DIRECTORY;
            this.path = path;
        }

        @Override
        public String getName() {
            return path;
        }
    } // class NamedFileEntry

    /** A directory entry. */
    private static class DirectoryEntry<AE extends ArchiveEntry>
    extends BaseEntry<AE> {
        Set<String> members = new LinkedHashSet<String>();

        /** Decorates the given archive entry. */
        DirectoryEntry(final AE entry) {
            super(entry);
            assert DIRECTORY == entry.getType();
        }

        @Override
        BaseEntry<AE> clone(final EntryFactory<AE> factory) {
            final DirectoryEntry<AE> clone = (DirectoryEntry<AE>) super.clone(factory);
            clone.members = Collections.unmodifiableSet(clone.members);
            return clone;
        }

        @Override
        public Set<String> getMembers() {
            return members;
        }

        @Override
        boolean add(final String member) {
            return members.add(member);
        }

        @Override
        boolean remove(final String member) {
            return members.remove(member);
        }
    } // class DirectoryEntry

    /** A named file entry. */
    private static class NamedDirectoryEntry<AE extends ArchiveEntry>
    extends DirectoryEntry<AE> {
        final String path;

        /** Decorates the given archive entry. */
        NamedDirectoryEntry(final String path, final AE entry) {
            super(entry);
            assert DIRECTORY == entry.getType();
            this.path = path;
        }

        @Override
        public String getName() {
            return path;
        }
    } // class NamedDirectoryEntry

    /**
     * Begins a <i>transaction</i> to create or replace and finally link a
     * chain of one or more archive entries for the given {@code path} into
     * this archive file system.
     * <p>
     * To commit the transaction, you need to call
     * {@link ArchiveFileSystemOperation#run} on the returned object, which
     * will mark this archive file system as {@link #isTouched() touched} and
     * set the last modification time of the created and linked archive file
     * system entries to the system's current time at the moment of the call
     * to this method.
     *
     * @param  path a non-{@code null} relative path name.
     * @param  type a non-{@code null} entry type.
     * @param  createParents if {@code true}, any missing parent directories
     *         will be created and linked into this archive file system with
     *         its last modification time set to the system's current time.
     * @param  template if not {@code null}, then the archive file system entry
     *         at the end of the chain shall inherit as much properties from
     *         this entry as possible - with the exception of its name and type.
     * @throws NullPointerException if {@code path} or {@code type} are
     *         {@code null}.
     * @throws ArchiveReadOnlyExceptionn If this archive file system is read
     *         only.
     * @throws ArchiveFileSystemException If one of the following is true:
     *         <ul>
     *         <li>{@code path} contains characters which are not
     *             supported by the archive file.</li>
     *         <li>TODO: type is not {@code FILE} or {@code DIRECTORY}.</li>
     *         <li>The new entry already exists as a directory.</li>
     *         <li>The new entry shall be a directory, but already exists.</li>
     *         <li>A parent entry exists but is not a directory.</li>
     *         <li>A parent entry is missing and {@code createParents} is
     *             {@code false}.</li>
     *         </ul>
     * @return A new archive file system operation on a chain of one or more
     *         archive file system entries for the given path name which will
     *         be linked into this archive file system upon a call to its
     *         {@link ArchiveFileSystemOperation#run} method.
     */
    public ArchiveFileSystemOperation<AE> mknod(
            final String path,
            final Type type,
            final boolean createParents,
            Entry template)
    throws ArchiveFileSystemException {
        if (isRoot(path))
            throw new ArchiveFileSystemException(path,
                    "cannot replace (virtual) root directory entry");
        if (!isValidPath(path))
            throw new ArchiveFileSystemException(path,
                    "is not a valid path name");
        if (null == type)
            throw new NullPointerException();
        if (FILE != type && DIRECTORY != type)
            throw new ArchiveFileSystemException(path,
                    "only FILE and DIRECTORY entries are currently supported");
        while (template instanceof ArchiveFileSystemEntry<?>)
            template = ((ArchiveFileSystemEntry<?>) template).getTarget();
        return new PathLink(path, type, template, createParents);
    }

    private final class PathLink implements ArchiveFileSystemOperation<AE> {
        final Splitter splitter = new Splitter();
        final boolean createParents;
        final SegmentLink<AE>[] links;

        PathLink(
                final String entryPath,
                final Entry.Type entryType,
                final Entry template,
                final boolean createParents)
        throws ArchiveFileSystemException {
            this.createParents = createParents;
            links = newSegmentLinks(entryPath, entryType, template, 1);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
		private SegmentLink<AE>[] newSegmentLinks(
                final String entryPath,
                final Entry.Type entryType,
                final Entry template,
                final int level)
        throws ArchiveFileSystemException {
            final String split[] = splitter.split(entryPath);
            final String parentPath = split[0]; // could equal ROOT
            final String baseName = split[1];
            final SegmentLink<AE>[] elements;

            // Lookup parent entry, creating it where necessary and allowed.
            final BaseEntry<AE> parentEntry = master.get(parentPath);
            final BaseEntry<AE> newEntry;
            if (parentEntry != null) {
                if (DIRECTORY != parentEntry.getType())
                    throw new ArchiveFileSystemException(entryPath,
                            "parent entry must be a directory");
                final BaseEntry<AE> oldEntry = master.get(entryPath);
                if (DIRECTORY == entryType) {
                    if (oldEntry != null) {
                        throw new ArchiveFileSystemException(entryPath,
                                "directory entries cannot replace existing entries");
                    }
                } else {
                    if (oldEntry != null && DIRECTORY == oldEntry.getType())
                        throw new ArchiveFileSystemException(entryPath,
                                "directory entries cannot get replaced");
                }
                elements = new SegmentLink[level + 1];
                elements[0] = new SegmentLink<AE>(parentEntry, null);
                newEntry = newEntryChecked(entryPath, entryType, template);
                elements[1] = new SegmentLink<AE>(newEntry, baseName);
            } else if (createParents) {
                elements = newSegmentLinks(
                        parentPath, DIRECTORY, null, level + 1);
                newEntry = newEntryChecked(entryPath, entryType, template);
                elements[elements.length - level]
                        = new SegmentLink<AE>(newEntry, baseName);
            } else {
                throw new ArchiveFileSystemException(entryPath,
                        "missing parent directory entry");
            }
            return elements;
        }

        @Override
        public void run() throws ArchiveFileSystemException {
            assert links.length >= 2;

            touch();
            final int l = links.length;
            final long time = System.currentTimeMillis();
            BaseEntry<AE> parent = links[0].entry;
            for (int i = 1; i < l ; i++) {
                final SegmentLink<AE> link = links[i];
                final BaseEntry<AE> entry = link.entry;
                final String base = link.base;
                assert DIRECTORY == parent.getType();
                master.put(entry.getName(), entry);
                if (parent.add(base) && UNKNOWN != parent.getTime(Access.WRITE)) // never beforeTouch ghosts!
                    parent.getTarget().setTime(Access.WRITE, time);
                parent = entry;
            }
            final AE entry = getTarget().getTarget();
            if (UNKNOWN == entry.getTime(WRITE))
                entry.setTime(WRITE, time);
        }

        @Override
        public ArchiveFileSystemEntry<AE> getTarget() {
            return links[links.length - 1].getTarget();
        }
    } // class PathLink

    /**
     * A data class which represents a segment for use by
     * {@link PathLink}.
     */
    private static final class SegmentLink<AE extends ArchiveEntry>
    implements Link<ArchiveFileSystemEntry<AE>> {
        final BaseEntry<AE> entry;
        final String base;

        /**
         * Constructs a new {@code SegmentLink}.
         *
         * @param entry The non-{@code null} file system entry for the path
         *        path.
         * @param base The nullable base (segment) path of the path name.
         */
        SegmentLink(
                final BaseEntry<AE> entry,
                final String base) {
            assert entry != null;
            this.entry = entry;
            this.base = base; // may be null!
        }

        @Override
        public ArchiveFileSystemEntry<AE> getTarget() {
            return entry;
        }
    } // class SegmentLink

    /**
     * If this method returns, the file system entry identified by the given
     * {@code path} has been successfully deleted from this archive file
     * system.
     * If the file system entry is a directory, it must be empty for successful
     * deletion.
     *
     * @throws ArchiveReadOnlyExceptionn If this (virtual) archive file system
     *         is read-only.
     * @throws ArchiveFileSystemException If the operation fails for some other
     *         reason.
     */
    public void unlink(final String path) throws ArchiveFileSystemException {
        if (isRoot(path))
            throw new ArchiveFileSystemException(path,
                    "(virtual) root directory cannot get unlinked");
        final BaseEntry<AE> entry = master.remove(path);
        if (entry == null)
            throw new ArchiveFileSystemException(path,
                    "archive entry does not exist");
        assert entry != root;
        if (entry.getType() == DIRECTORY && entry.getMembers().size() > 0) {
            master.put(path, entry); // Restore file system
            throw new ArchiveFileSystemException(path,
                    "directory is not empty");
        }
        final Splitter splitter = new Splitter();
        splitter.split(path);
        final String parentPath = splitter.getParentPath();
        final BaseEntry<AE> parent = master.get(parentPath);
        assert parent != null : "The parent directory of \"" + path
                    + "\" is missing - archive file system is corrupted!";
        final boolean ok = parent.remove(splitter.getMemberName());
        assert ok : "The parent directory of \"" + path
                    + "\" does not contain this entry - archive file system is corrupted!";
        touch();
        if (parent.getTime(Access.WRITE) != UNKNOWN) // never beforeTouch ghosts!
            parent.getTarget().setTime(Access.WRITE, System.currentTimeMillis());
    }

    public boolean setTime(
            final String path,
            final BitField<Access> types,
            final long value)
    throws ArchiveFileSystemException {
        if (0 > value)
            throw new IllegalArgumentException(path +
                    " (negative access time)");
        final BaseEntry<AE> entry = master.get(path);
        if (entry == null)
            throw new ArchiveFileSystemException(path,
                    "archive entry not found");
        // Order is important here!
        touch();
        boolean ok = true;
        for (Access type : types)
            ok &= entry.getTarget().setTime(type, value);
        return ok;
    }

    private Type getType(final String path) {
        final BaseEntry<AE> entry = master.get(path);
        return entry != null ? entry.getType() : null;
    }

    public boolean isWritable(final String path) {
        return !isReadOnly() && getType(path) == FILE;
    }

    public void setReadOnly(final String path)
    throws ArchiveFileSystemException {
        if (!isReadOnly() || getType(path) != FILE)
            throw new ArchiveFileSystemException(path,
                "cannot set read-only state");
    }
}
