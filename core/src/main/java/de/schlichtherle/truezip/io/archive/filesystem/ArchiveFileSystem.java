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
import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.entry.EntryContainer;
import de.schlichtherle.truezip.io.entry.EntryFactory;
import de.schlichtherle.truezip.io.entry.EntryName;
import de.schlichtherle.truezip.io.filesystem.FileSystemEntryName;
import de.schlichtherle.truezip.util.Link;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.CharConversionException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static de.schlichtherle.truezip.io.entry.Entry.*;
import static de.schlichtherle.truezip.io.entry.Entry.Access.*;
import static de.schlichtherle.truezip.io.entry.Entry.Type.*;
import static de.schlichtherle.truezip.io.filesystem.FileSystemEntryName.*;
import static de.schlichtherle.truezip.io.Paths.*;

/**
 * A base class for a virtual file system for archive entries.
 * <p>
 * This class is <em>not</em> thread-safe!
 * Multithreading needs to be addressed by client classes.
 * 
 * @param   <E> The type of the archive entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class ArchiveFileSystem<E extends ArchiveEntry>
implements EntryContainer<ArchiveFileSystemEntry<E>> {

    /** The controller that this filesystem belongs to. */
    private final EntryFactory<E> factory;

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
    private Map<String, ArchiveFileSystemEntry<E>> master;

    /** The file system entry for the (virtual) root of this file system. */
    private final ArchiveFileSystemEntry<E> root;

    /** Whether or not this file system has been modified (touched). */
    private boolean touched;

    private LinkedHashSet<ArchiveFileSystemTouchListener<? super E>> touchListeners
            = new LinkedHashSet<ArchiveFileSystemTouchListener<? super E>>();

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
    ArchiveFileSystem<AE> newArchiveFileSystem(@NonNull EntryFactory<AE> factory) {
        return new ArchiveFileSystem<AE>(factory);
    }

    private ArchiveFileSystem(@NonNull final EntryFactory<E> factory) {
        this.factory = factory;
        master = new LinkedHashMap<String, ArchiveFileSystemEntry<E>>(64);

        // Setup root.
        root = newEntryUnchecked(ROOT, DIRECTORY, null);
        for (Access access : BitField.allOf(Access.class))
            root.getEntry().setTime(access, System.currentTimeMillis());
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
    public static <E extends ArchiveEntry>
    ArchiveFileSystem<E> newArchiveFileSystem(
            @NonNull EntryContainer<E> container,
            @NonNull EntryFactory<E> factory,
            @CheckForNull Entry rootTemplate,
            boolean readOnly) {
        return readOnly
            ? new ReadOnlyArchiveFileSystem<E>(container, factory, rootTemplate)
            : new ArchiveFileSystem<E>(container, factory, rootTemplate);
    }

    ArchiveFileSystem(
            @NonNull final EntryContainer<E> container,
            @NonNull final EntryFactory<E> factory,
            @CheckForNull final Entry rootTemplate) {
        if (null == rootTemplate)
            throw new NullPointerException();
        if (rootTemplate instanceof ArchiveFileSystemEntry<?>)
            throw new IllegalArgumentException();

        this.factory = factory;
        master = new LinkedHashMap<String, ArchiveFileSystemEntry<E>>(
                (int) (container.getSize() / .75f) + 1);

        // Load entries from input archive.
        for (final E entry : container) {
            String name = cutTrailingSeparators(entry.getName(), SEPARATOR_CHAR);
            name = EntryName.create(name, null, true).getPath();
            master.put(name, ArchiveFileSystemEntry.create(name, entry.getType(), entry));
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
        for (final E entry : container) {
            String name = cutTrailingSeparators(entry.getName(), SEPARATOR_CHAR);
            try {
                fsck.fix(new FileSystemEntryName(
                        new URI(null, null, name, null, null),
                        true).getPath());
            } catch (URISyntaxException ignore) {
            }
        }
    }

    /** Splits a path name into a parent path name and a base path. */
    private static class Splitter
    extends de.schlichtherle.truezip.io.Paths.Splitter {
        Splitter() {
            super(SEPARATOR_CHAR);
        }

        /**
         * Like its super class implementation, but substitutes {@code ROOT}
         * for {@code null}.
         */
        @Override
        @NonNull
        public String getParentPath() {
            final String parentPath = super.getParentPath();
            return null != parentPath ? parentPath : ROOT;
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
        void fix(@NonNull final String path) {
            // When recursing into this method, it may be called with the root
            // directory as its parameter, so we may NOT skip the following test.
            if (isRoot(path))
                return; // never fix root or empty or absolute pathnames

            split(path);
            final String parentPath = getParentPath();
            final String memberName = getMemberName();
            ArchiveFileSystemEntry<E> parent = master.get(parentPath);
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
        final ArchiveFileSystemEvent<E> event
                = new ArchiveFileSystemEvent<E>(this);
        final Iterable<ArchiveFileSystemTouchListener<? super E>> listeners
                = getArchiveFileSystemTouchListeners();
        try {
            for (ArchiveFileSystemTouchListener<? super E> listener : listeners)
                listener.beforeTouch(event);
        } catch (IOException ex) {
            throw new ArchiveFileSystemException(null, "touch vetoed", ex);
        }
        touched = true;
        for (ArchiveFileSystemTouchListener<? super E> listener : listeners)
            listener.afterTouch(event);
    }

    /**
     * Returns a protective copy of the set of archive file system listeners.
     *
     * @return A clone of the set of archive file system listeners.
     */
    @SuppressWarnings("unchecked")
    Set<ArchiveFileSystemTouchListener<? super E>>
    getArchiveFileSystemTouchListeners() {
        return (Set<ArchiveFileSystemTouchListener<? super E>>) touchListeners.clone();
    }

    /**
     * Adds the given listener to the set of archive file system listeners.
     *
     * @param  listener the non-{@code null} listener for archive file system
     *         events.
     * @throws NullPointerException if {@code listener} is {@code null}.
     */
    public final void addArchiveFileSystemTouchListener(
            @NonNull final ArchiveFileSystemTouchListener<? super E> listener) {
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
            @NonNull final ArchiveFileSystemTouchListener<? super E> listener) {
        if (null == listener)
            throw new NullPointerException();
        touchListeners.remove(listener);
    }

    @Override
    public int getSize() {
        return master.size();
    }

    @Override
    @NonNull
    public Iterator<ArchiveFileSystemEntry<E>> iterator() {
        class ArchiveEntryIterator implements Iterator<ArchiveFileSystemEntry<E>> {
            final Iterator<ArchiveFileSystemEntry<E>> it = master.values().iterator();

            @Override
			public boolean hasNext() {
                return it.hasNext();
            }

            @Override
			public ArchiveFileSystemEntry<E> next() {
                return it.next();
            }

            @Override
			public void remove() {
                throw new UnsupportedOperationException();
            }
        }
        return new ArchiveEntryIterator();
    }

    @Nullable
    public final ArchiveFileSystemEntry<E> getEntry(
            @NonNull FileSystemEntryName name) {
        return getEntry(name.getPath());
    }

    @Override
    @Nullable
    public ArchiveFileSystemEntry<E> getEntry(@NonNull String path) {
        if (path == null)
            throw new NullPointerException();
        final ArchiveFileSystemEntry<E> entry = master.get(path);
        return null == entry ? null : entry.clone(this);
    }

    /**
     * Like {@link #newEntryChecked newEntryChecked(path, type, null)},
     * but wraps any {@link CharConversionException} in an
     * {@link AssertionError}.
     *
     * @throws AssertionError if a {@link CharConversionException}
     *         occurs. The original exception is wrapped as its cause.
     */
    @NonNull
    private ArchiveFileSystemEntry<E> newEntryUnchecked(
            @NonNull final String path,
            @NonNull final Type type,
            @CheckForNull final Entry template) {
        assert type != null;
        assert !isRoot(path) || type == DIRECTORY;
        assert !(template instanceof ArchiveFileSystemEntry<?>);

        try {
            return ArchiveFileSystemEntry.create(
                    path, type, factory.newEntry(path, type, template));
        } catch (CharConversionException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * Returns a new file system entry for this (virtual) archive file system.
     * This is only a factory method, i.e. the returned file system entry is
     * not yet linked into this (virtual) archive file system.
     *
     * @see    #mknod
     * @param  path the path name of the archive file system entry.
     */
    @NonNull
    private ArchiveFileSystemEntry<E> newEntryChecked(
            @NonNull final String path,
            @NonNull final Type type,
            @CheckForNull final Entry template)
    throws ArchiveFileSystemException {
        assert type != null;
        assert !isRoot(path) || type == DIRECTORY;
        assert !(template instanceof ArchiveFileSystemEntry<?>);

        try {
            return ArchiveFileSystemEntry.create(
                    path, type, factory.newEntry(path, type, template));
        } catch (CharConversionException ex) {
            throw new ArchiveFileSystemException(path, ex);
        }
    }

    E copy(final E entry) {
        try {
            return factory.newEntry(entry.getName(), entry.getType(), entry);
        } catch (CharConversionException ex) {
            throw new AssertionError(ex);
        }
    }

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
     * @param  name an entry name.
     * @param  type an entry type.
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
    @NonNull
    public ArchiveFileSystemOperation<E> mknod(
            @NonNull final FileSystemEntryName name,
            @NonNull final Type type,
            final boolean createParents,
            @CheckForNull Entry template)
    throws ArchiveFileSystemException {
        final String path = name.getPath();
        if (isRoot(path))
            throw new ArchiveFileSystemException(path,
                    "cannot replace (virtual) root directory entry");
        if (null == type)
            throw new NullPointerException();
        if (FILE != type && DIRECTORY != type)
            throw new ArchiveFileSystemException(path,
                    "only FILE and DIRECTORY entries are currently supported");
        while (template instanceof ArchiveFileSystemEntry<?>)
            template = ((ArchiveFileSystemEntry<?>) template).getEntry();
        return new PathLink(path, type, createParents, template);
    }

    private final class PathLink implements ArchiveFileSystemOperation<E> {
        final Splitter splitter = new Splitter();
        final boolean createParents;
        final SegmentLink<E>[] links;

        PathLink(   @NonNull final String entryPath,
                    @NonNull final Entry.Type entryType,
                    final boolean createParents,
                    @CheckForNull final Entry template)
        throws ArchiveFileSystemException {
            this.createParents = createParents;
            links = newSegmentLinks(entryPath, entryType, template, 1);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private SegmentLink<E>[] newSegmentLinks(
                @NonNull final String entryPath,
                @NonNull final Entry.Type entryType,
                @CheckForNull final Entry template,
                final int level)
        throws ArchiveFileSystemException {
            splitter.split(entryPath);
            final String parentPath = splitter.getParentPath(); // could equal ROOT
            final String memberName = splitter.getMemberName();
            final SegmentLink<E>[] elements;

            // Lookup parent entry, creating it where necessary and allowed.
            final ArchiveFileSystemEntry<E> parentEntry = master.get(parentPath);
            final ArchiveFileSystemEntry<E> newEntry;
            if (parentEntry != null) {
                if (DIRECTORY != parentEntry.getType())
                    throw new ArchiveFileSystemException(entryPath,
                            "parent entry must be a directory");
                final ArchiveFileSystemEntry<E> oldEntry = master.get(entryPath);
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
                elements[0] = new SegmentLink<E>(parentEntry, null);
                newEntry = newEntryChecked(entryPath, entryType, template);
                elements[1] = new SegmentLink<E>(newEntry, memberName);
            } else if (createParents) {
                elements = newSegmentLinks(
                        parentPath, DIRECTORY, null, level + 1);
                newEntry = newEntryChecked(entryPath, entryType, template);
                elements[elements.length - level]
                        = new SegmentLink<E>(newEntry, memberName);
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
            ArchiveFileSystemEntry<E> parent = links[0].entry;
            for (int i = 1; i < l ; i++) {
                final SegmentLink<E> link = links[i];
                final ArchiveFileSystemEntry<E> entry = link.entry;
                final String base = link.base;
                assert DIRECTORY == parent.getType();
                master.put(entry.getName(), entry);
                if (parent.add(base) && UNKNOWN != parent.getTime(Access.WRITE)) // never beforeTouch ghosts!
                    parent.getEntry().setTime(Access.WRITE, time);
                parent = entry;
            }
            final E entry = getTarget().getEntry();
            if (UNKNOWN == entry.getTime(WRITE))
                entry.setTime(WRITE, time);
        }

        @Override
        @NonNull
        public ArchiveFileSystemEntry<E> getTarget() {
            return links[links.length - 1].getTarget();
        }
    } // class PathLink

    /**
     * A data class which represents a segment for use by
     * {@link PathLink}.
     */
    private static final class SegmentLink<E extends ArchiveEntry>
    implements Link<ArchiveFileSystemEntry<E>> {
        final ArchiveFileSystemEntry<E> entry;
        final String base;

        /**
         * Constructs a new {@code SegmentLink}.
         *
         * @param entry The non-{@code null} file system entry for the path
         *        path.
         * @param base The nullable base (segment) path of the path name.
         */
        SegmentLink(
                @NonNull final ArchiveFileSystemEntry<E> entry,
                @Nullable final String base) {
            assert null != entry;
            this.entry = entry;
            this.base = base; // may be null!
        }

        @Override
        @NonNull
        public ArchiveFileSystemEntry<E> getTarget() {
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
    public void unlink(@NonNull final FileSystemEntryName name) throws ArchiveFileSystemException {
        final String path = name.getPath();
        if (isRoot(path))
            throw new ArchiveFileSystemException(path,
                    "(virtual) root directory cannot get unlinked");
        final ArchiveFileSystemEntry<E> entry = master.remove(path);
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
        final ArchiveFileSystemEntry<E> parent = master.get(parentPath);
        assert parent != null : "The parent directory of \"" + path
                    + "\" is missing - archive file system is corrupted!";
        final boolean ok = parent.remove(splitter.getMemberName());
        assert ok : "The parent directory of \"" + path
                    + "\" does not contain this entry - archive file system is corrupted!";
        touch();
        if (parent.getTime(Access.WRITE) != UNKNOWN) // never beforeTouch ghosts!
            parent.getEntry().setTime(Access.WRITE, System.currentTimeMillis());
    }

    public boolean setTime(
            @NonNull final FileSystemEntryName name,
            @NonNull final BitField<Access> types,
            final long value)
    throws ArchiveFileSystemException {
        final String path = name.getPath();
        if (0 > value)
            throw new IllegalArgumentException(path +
                    " (negative access time)");
        final ArchiveFileSystemEntry<E> entry = master.get(path);
        if (entry == null)
            throw new ArchiveFileSystemException(path,
                    "archive entry not found");
        // Order is important here!
        touch();
        boolean ok = true;
        for (Access type : types)
            ok &= entry.getEntry().setTime(type, value);
        return ok;
    }

    public boolean isWritable(@NonNull FileSystemEntryName name) {
        return !isReadOnly();
    }

    public void setReadOnly(@NonNull FileSystemEntryName name)
    throws ArchiveFileSystemException {
        if (!isReadOnly())
            throw new ArchiveFileSystemException(name.getPath(),
                "cannot set read-only state");
    }
}
