/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.EntryContainer;
import de.schlichtherle.truezip.entry.EntryFactory;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.fs.FsUriModifier;
import de.schlichtherle.truezip.util.Link;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.CharConversionException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;

import static de.schlichtherle.truezip.entry.Entry.*;
import static de.schlichtherle.truezip.entry.Entry.Access.*;
import static de.schlichtherle.truezip.entry.Entry.Type.*;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import static de.schlichtherle.truezip.io.Paths.*;

/**
 * A read/write virtual file system for archive entries.
 * 
 * @param   <E> The type of the archive entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
class FsArchiveFileSystem<E extends FsArchiveEntry>
implements Iterable<FsArchiveFileSystemEntry<E>> {

    private final Splitter splitter = new Splitter();
    private final EntryFactory<E> factory;
    private final MasterEntryTable<E> master;

    /** Whether or not this file system has been modified (touched). */
    private boolean touched;

    private LinkedHashSet<FsArchiveFileSystemTouchListener<? super E>> touchListeners
            = new LinkedHashSet<FsArchiveFileSystemTouchListener<? super E>>();

    /**
     * Returns a new archive file system and ensures its integrity.
     * The root directory is created with its last modification time set to
     * the system's current time.
     * The file system is modifiable and marked as touched!
     *
     * @param  <E> The type of the archive entries.
     * @param  factory the archive entry factory to use.
     * @return A new archive file system.
     * @throws NullPointerException If {@code factory} is {@code null}.
     */
    static <E extends FsArchiveEntry> FsArchiveFileSystem<E>
    newArchiveFileSystem(EntryFactory<E> factory) {
        return new FsArchiveFileSystem<E>(factory);
    }

    private FsArchiveFileSystem(final EntryFactory<E> factory) {
        this.factory = factory;
        final FsArchiveFileSystemEntry<E>
                root = newEntryUnchecked(ROOT, DIRECTORY, null);
        final E rootEntry = root.getEntry();
        for (Access access : BitField.allOf(Access.class))
            rootEntry.setTime(access, System.currentTimeMillis());
        this.master = newMasterEntryTable(root, 64);
        try {
            touch();
        } catch (FsArchiveFileSystemException ex) {
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
     * @param  <E> The type of the archive entries.
     * @param  factory the archive entry factory to use.
     * @param  archive The archive entry container to read the entries for
     *         the population of the archive file system.
     * @param  rootTemplate The nullable template to use for the root entry of
     *         the returned archive file system.
     * @param  readOnly If and only if {@code true}, any subsequent
     *         modifying operation on the file system will result in a
     *         {@link FsReadOnlyArchiveFileSystemException}.
     * @return A new archive file system.
     * @throws NullPointerException If {@code factory} or {@code archive} are
     *         {@code null}.
     * @throws IllegalArgumentException If {@code rootTemplate} is an instance
     *         of {@link FsArchiveFileSystemEntry}.
     */
    static <E extends FsArchiveEntry> FsArchiveFileSystem<E>
    newArchiveFileSystem(   EntryFactory<E> factory,
                            EntryContainer<E> archive,
                            @CheckForNull Entry rootTemplate,
                            boolean readOnly) {
        return readOnly
            ? new FsReadOnlyArchiveFileSystem<E>(archive, factory, rootTemplate)
            : new FsArchiveFileSystem<E>(factory, archive, rootTemplate);
    }

    FsArchiveFileSystem(final EntryFactory<E> factory,
                        final EntryContainer<E> archive,
                        final @CheckForNull Entry rootTemplate) {
        if (rootTemplate instanceof FsArchiveFileSystemEntry<?>)
            throw new IllegalArgumentException();

        this.factory = factory;
        final FsArchiveFileSystemEntry<E>
                root = newEntryUnchecked(ROOT, DIRECTORY, rootTemplate);
        // Allocate some overhead to create missing parent directories.
        this.master = newMasterEntryTable(root, (int) (archive.getSize() / .7f) + 1);

        // Load entries from input archive.
        final List<FsEntryName>
                names = new ArrayList<FsEntryName>(archive.getSize());
        for (final E entry : archive) {
            try {
                final FsEntryName name = new FsEntryName(
                        new URI(null,
                                null,
                                cutTrailingSeparators(entry.getName().replace('\\', SEPARATOR_CHAR), SEPARATOR_CHAR),
                                //entry.getName().replace('\\', SEPARATOR_CHAR),
                                null),
                        FsUriModifier.CANONICALIZE);
                master.add(FsArchiveFileSystemEntry.create(name, entry.getType(), entry));
                names.add(name);
            } catch (URISyntaxException ex) {
                throw new AssertionError(ex);
            }
        }

        // Setup root file system entry, potentially replacing its previous
        // mapping from the input archive.
        master.add(root);

        // Now perform a file system check to create missing parent directories
        // and populate directories with their members - this needs to be done
        // separately!
        // entries = Collections.enumeration(master.values()); // concurrent modification!
        for (FsEntryName name : names)
            fix(name);
    }

    private static <E extends FsArchiveEntry> MasterEntryTable<E>
    newMasterEntryTable(final FsArchiveFileSystemEntry<E> root, final int initialCapacity) {
        final MasterEntryTable<E>
                master = root.getEntry().getName().endsWith(SEPARATOR)
                    ? new ZipOrTarEntryTable<E>(initialCapacity)
                    : new DefaultEntryTable<E>(initialCapacity);
        master.add(root);
        return master;
    }

    /**
     * Called from a constructor to fix the parent directories of the
     * file system entry identified by {@code name}, ensuring that all
     * parent directories of the file system entry exist and that they
     * contain the respective base.
     * If a parent directory does not exist, it is created using an
     * unkown time as the last modification time - this is defined to be a
     * <i>ghost directory<i>.
     * If a parent directory does exist, the respective base is added
     * (possibly yet again) and the process is continued.
     *
     * @param name the archive file system entry name.
     */
    private void fix(final FsEntryName name) {
        // When recursing into this method, it may be called with the root
        // directory as its parameter, so we may NOT skip the following test.
        if (isRoot(name.getPath()))
            return; // never fix root or empty or absolute pathnames

        splitter.split(name);
        final FsEntryName parentName = splitter.getParentName();
        final String memberName = splitter.getMemberName();
        FsArchiveFileSystemEntry<E> parent = master.get(parentName, DIRECTORY);
        if (null == parent) {
            parent = newEntryUnchecked(parentName, DIRECTORY, null);
            master.add(parent);
        }
        parent.add(memberName);
        fix(parentName);
    }

    /**
     * Returns {@code true} if and only if this archive file system is
     * read-only.
     * <p>
     * The implementation in the class {@link FsArchiveFileSystem} always
     * returns {@code false}.
     * 
     * @return Whether or not the this archive file system is read-only.
     */
    boolean isReadOnly() {
        return false;
    }

    /**
     * Returns {@code true} if and only if this archive file system has been
     * modified since its time of creation.
     * 
     * @return {@code true} if and only if this archive file system has been
     *         modified since its time of creation.
     */
    boolean isTouched() {
        return touched;
    }

    /**
     * Ensures that the controller's data structures required to output
     * entries are properly initialized and marks this (virtual) archive
     * file system as touched.
     *
     * @throws FsReadOnlyArchiveFileSystemException If this (virtual) archive
     *         file system is read only.
     * @throws FsArchiveFileSystemException If the listener vetoed the beforeTouch
     *         operation for any reason.
     */
    private void touch() throws FsArchiveFileSystemException {
        if (touched)
            return;
        // Order is important here because of veto exceptions!
        final FsArchiveFileSystemEvent<E> event
                = new FsArchiveFileSystemEvent<E>(this);
        final Iterable<FsArchiveFileSystemTouchListener<? super E>> listeners
                = getFsArchiveFileSystemTouchListeners();
        try {
            for (FsArchiveFileSystemTouchListener<? super E> listener : listeners)
                listener.beforeTouch(event);
        } catch (IOException ex) {
            throw new FsArchiveFileSystemException(null, "touch vetoed", ex);
        }
        touched = true;
        for (FsArchiveFileSystemTouchListener<? super E> listener : listeners)
            listener.afterTouch(event);
    }

    /**
     * Returns a protective copy of the set of archive file system listeners.
     *
     * @return A clone of the set of archive file system listeners.
     */
    @SuppressWarnings("unchecked")
    Set<FsArchiveFileSystemTouchListener<? super E>>
    getFsArchiveFileSystemTouchListeners() {
        return (Set<FsArchiveFileSystemTouchListener<? super E>>) touchListeners.clone();
    }

    /**
     * Adds the given listener to the set of archive file system listeners.
     *
     * @param  listener the listener for archive file system events.
     */
    final void addFsArchiveFileSystemTouchListener(
            FsArchiveFileSystemTouchListener<? super E> listener) {
        if (null == listener)
            throw new NullPointerException();
        touchListeners.add(listener);
    }

    /**
     * Removes the given listener from the set of archive file system listeners.
     *
     * @param  listener the listener for archive file system events.
     */
    final void removeFsArchiveFileSystemTouchListener(
            @Nullable FsArchiveFileSystemTouchListener<? super E> listener) {
        touchListeners.remove(listener);
    }

    int getSize() {
        return master.getSize();
    }

    @Override
    public Iterator<FsArchiveFileSystemEntry<E>> iterator() {
        return master.iterator();
    }

    @Nullable
    final FsArchiveFileSystemEntry<E> getEntry(FsEntryName name) {
        assert null != name;
        FsArchiveFileSystemEntry<E> entry = master.get(name, null);
        return null == entry ? null : entry.clone(this);
    }

    /**
     * Like {@link #newEntryChecked newEntryChecked(path, type, null)},
     * but wraps any {@link CharConversionException} in an
     * {@link AssertionError}.
     *
     * @param  name the archive file system entry name.
     * @param  type the type of the archive file system entry to create.
     * @param  template the nullable template for the archive file system entry
     *         to create.
     * @return A new file system entry for this (virtual) archive file system.
     */
    private FsArchiveFileSystemEntry<E> newEntryUnchecked(
            final FsEntryName name,
            final Type type,
            @CheckForNull final Entry template) {
        assert null != type;
        assert !isRoot(name.getPath()) || DIRECTORY == type;
        assert !(template instanceof FsArchiveFileSystemEntry<?>);

        try {
            return FsArchiveFileSystemEntry.create(
                    name, type, factory.newEntry(name.getPath(), type, template));
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
     * @param  name the archive file system entry name.
     * @param  type the type of the archive file system entry to create.
     * @param  template the nullable template for the archive file system entry
     *         to create.
     * @return A new file system entry for this (virtual) archive file system.
     * @throws FsArchiveFileSystemException if a {@link CharConversionException}
     *         occurs as its cause.
     */
    private FsArchiveFileSystemEntry<E> newEntryChecked(
            final FsEntryName name,
            final Type type,
            @CheckForNull final Entry template)
    throws FsArchiveFileSystemException {
        assert null != type;
        assert !isRoot(name.getPath()) || DIRECTORY == type;
        assert !(template instanceof FsArchiveFileSystemEntry<?>);

        try {
            return FsArchiveFileSystemEntry.create(
                    name, type, factory.newEntry(name.getPath(), type, template));
        } catch (CharConversionException ex) {
            throw new FsArchiveFileSystemException(name.toString(), ex);
        }
    }

    final E copy(E entry) {
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
     * {@link FsArchiveFileSystemOperation#run} on the returned object, which
     * will mark this archive file system as {@link #isTouched() touched} and
     * set the last modification time of the created and linked archive file
     * system entries to the system's current time at the moment of the call
     * to this method.
     *
     * @param  name the archive file system entry name.
     * @param  type the type of the archive file system entry to create.
     * @param  options if {@code CREATE_PARENTS} is set, any missing parent
     *         directories will be created and linked into this file
     *         system with its last modification time set to the system's
     *         current time.
     * @param  template if not {@code null}, then the archive file system entry
     *         at the end of the chain shall inherit as much properties from
     *         this entry as possible - with the exception of its name and type.
     * @throws NullPointerException if {@code path} or {@code type} are
     *         {@code null}.
     * @throws ArchiveReadOnlyExceptionn If this archive file system is read
     *         only.
     * @throws FsArchiveFileSystemException If one of the following is true:
     *         <ul>
     *         <li>The file system is read only.
     *         <li>{@code name} contains characters which are not
     *             supported by the file system.
     *         <li>TODO: type is not {@code FILE} or {@code DIRECTORY}.
     *         <li>The entry already exists and either the option
     *             {@link FsOutputOption#EXCLUSIVE} is set or the entry is a
     *             directory.
     *         <li>The entry exists as a different type.
     *         <li>A parent entry exists but is not a directory.
     *         <li>A parent entry is missing and {@code createParents} is
     *             {@code false}.
     *         </ul>
     * @return A new archive file system operation on a chain of one or more
     *         archive file system entries for the given path name which will
     *         be linked into this archive file system upon a call to its
     *         {@link FsArchiveFileSystemOperation#run} method.
     */
    public FsArchiveFileSystemOperation<E> mknod(
            final FsEntryName name,
            final Entry.Type type,
            final BitField<FsOutputOption> options,
            @CheckForNull Entry template)
    throws FsArchiveFileSystemException {
        if (null == type)
            throw new NullPointerException();
        if (FILE != type && DIRECTORY != type) // TODO: Add support for other types.
            throw new FsArchiveFileSystemException(name.toString(),
                    "only FILE and DIRECTORY entries are currently supported");
        final FsArchiveFileSystemEntry<E> oldEntry = master.get(name, null);
        if (null != oldEntry) {
            if (options.get(EXCLUSIVE))
                throw new FsArchiveFileSystemException(name.toString(),
                        "entry exists already");
            final Entry.Type oldEntryType = oldEntry.getType();
            if (oldEntryType != FILE && oldEntryType != HYBRID)
                throw new FsArchiveFileSystemException(name.toString(),
                        "only (hybrid) files can get replaced");
            if (oldEntryType != type)
                throw new FsArchiveFileSystemException(name.toString(),
                        "entry exists already as a different type");
        }
        while (template instanceof FsArchiveFileSystemEntry<?>)
            template = ((FsArchiveFileSystemEntry<?>) template).getEntry();
        return new PathLink(name, type, options.get(CREATE_PARENTS), template);
    }

    /**
     * TODO: This implementation yields a potential issue: The state of the
     * file system may be altered between the construction of an instance and
     * the call to the {@link #run} method, which may render the operation
     * illegal and corrupt the file system.
     * As long as only the ArchiveControllers in the package
     * de.schlichtherle.truezip.fs.archive are used, this should not
     * happen, however.
     */
    private final class PathLink implements FsArchiveFileSystemOperation<E> {
        final boolean createParents;
        final SegmentLink<E>[] links;
        long time = -1;

        PathLink(   final FsEntryName name,
                    final Entry.Type type,
                    final boolean createParents,
                    @CheckForNull final Entry template)
        throws FsArchiveFileSystemException {
            this.createParents = createParents;
            links = newSegmentLinks(name, type, template, 1);
        }

        @SuppressWarnings({ "unchecked", "all" })
        private SegmentLink<E>[] newSegmentLinks(
                final FsEntryName entryName,
                final Entry.Type entryType,
                @CheckForNull final Entry template,
                final int level)
        throws FsArchiveFileSystemException {
            splitter.split(entryName);
            final FsEntryName parentName = splitter.getParentName(); // could equal ROOT
            final String memberName = splitter.getMemberName();
            final SegmentLink<E>[] elements;

            // Lookup parent entry, creating it where necessary and allowed.
            final FsArchiveFileSystemEntry<E>
                    parentEntry = master.get(parentName, null);
            final FsArchiveFileSystemEntry<E> newEntry;
            if (parentEntry != null) {
                if (DIRECTORY != parentEntry.getType())
                    throw new FsArchiveFileSystemException(entryName.toString(),
                            "parent entry must be a directory");
                elements = new SegmentLink[level + 1];
                elements[0] = new SegmentLink<E>(null, parentEntry);
                newEntry = newEntryChecked(entryName, entryType, template);
                elements[1] = new SegmentLink<E>(memberName, newEntry);
            } else if (createParents) {
                elements = newSegmentLinks(
                        parentName, DIRECTORY, null, level + 1);
                newEntry = newEntryChecked(entryName, entryType, template);
                elements[elements.length - level]
                        = new SegmentLink<E>(memberName, newEntry);
            } else {
                throw new FsArchiveFileSystemException(entryName.toString(),
                        "missing parent directory entry");
            }
            return elements;
        }

        @Override
        public void run() throws FsArchiveFileSystemException {
            assert 2 <= links.length;

            touch();
            final int l = links.length;
            FsArchiveFileSystemEntry<E> parent = links[0].entry;
            for (int i = 1; i < l ; i++) {
                final SegmentLink<E> link = links[i];
                final FsArchiveFileSystemEntry<E> entry = link.entry;
                final String member = link.base;
                assert DIRECTORY == parent.getType();
                master.add(entry);
                if (parent.add(member) && UNKNOWN != parent.getTime(Access.WRITE)) // never touch ghosts!
                    parent.getEntry().setTime(Access.WRITE, getCurrentTimeMillis());
                parent = entry;
            }
            final E entry = getTarget().getEntry();
            if (UNKNOWN == entry.getTime(WRITE))
                entry.setTime(WRITE, getCurrentTimeMillis());
        }

        private long getCurrentTimeMillis() {
            return 0 <= time ? time : (time = System.currentTimeMillis());
        }

        @Override
        public FsArchiveFileSystemEntry<E> getTarget() {
            return links[links.length - 1].getTarget();
        }
    } // class PathLink

    /**
     * A data class which represents a segment for use by
     * {@link PathLink}.
     * 
     * @param <E> The type of the archive entries.
     */
    private static final class SegmentLink<E extends FsArchiveEntry>
    implements Link<FsArchiveFileSystemEntry<E>> {
        final @CheckForNull String base;
        final FsArchiveFileSystemEntry<E> entry;

        /**
         * Constructs a new {@code SegmentLink}.
         *
         * @param base the nullable base name of the entry name.
         * @param entry the non-{@code null} file system entry for the entry
         *        name.
         */
        SegmentLink(
                final @CheckForNull String base,
                final FsArchiveFileSystemEntry<E> entry) {
            this.entry = entry;
            this.base = base;
        }

        @Override
        public FsArchiveFileSystemEntry<E> getTarget() {
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
     * @param  name the archive file system entry name.
     * @throws FsReadOnlyArchiveFileSystemException If this (virtual) archive
     *         file system is read-only.
     * @throws FsArchiveFileSystemException If the operation fails for some other
     *         reason.
     */
    public void unlink(final FsEntryName name)
    throws FsArchiveFileSystemException {
        if (name.isRoot())
            throw new FsArchiveFileSystemException(name.toString(),
                    "root directory cannot get unlinked");
        final FsArchiveFileSystemEntry<E> entry = master.get(name, null);
        if (entry == null)
            throw new FsArchiveFileSystemException(name.toString(),
                    "archive entry does not exist");
        if (DIRECTORY == entry.getType() && 0 < entry.getMembers().size()) {
            throw new FsArchiveFileSystemException(name.toString(),
                    "directory is not empty");
        }
        touch();
        master.remove(name, entry.getType());
        splitter.split(name);
        final FsEntryName parentName = splitter.getParentName();
        final FsArchiveFileSystemEntry<E> parent = master.get(parentName, DIRECTORY);
        assert parent != null : "The parent directory of \"" + name.toString()
                    + "\" is missing - archive file system is corrupted!";
        final boolean ok = parent.remove(splitter.getMemberName());
        assert ok : "The parent directory of \"" + name.toString()
                    + "\" does not contain this entry - archive file system is corrupted!";
        final E ae = parent.getEntry();
        if (ae.getTime(Access.WRITE) != UNKNOWN) // never touch ghosts!
            ae.setTime(Access.WRITE, System.currentTimeMillis());
    }

    public boolean setTime(
            final FsEntryName name,
            final BitField<Access> types,
            final long value)
    throws FsArchiveFileSystemException {
        if (0 > value)
            throw new IllegalArgumentException(name.toString()
                    + " (negative access time)");
        final FsArchiveFileSystemEntry<E> entry = master.get(name, null);
        if (null == entry)
            throw new FsArchiveFileSystemException(name.toString(),
                    "archive entry not found");
        // Order is important here!
        touch();
        boolean ok = true;
        for (Access type : types)
            ok &= entry.getEntry().setTime(type, value);
        return ok;
    }

    public boolean isWritable(FsEntryName name) {
        return !isReadOnly();
    }

    public void setReadOnly(FsEntryName name)
    throws FsArchiveFileSystemException {
        if (!isReadOnly())
            throw new FsArchiveFileSystemException(name.getPath(),
                "cannot set read-only state");
    }

    private static final class ZipOrTarEntryTable<E extends FsArchiveEntry>
    extends MasterEntryTable<E> {
        ZipOrTarEntryTable(int initialCapacity) {
            super(initialCapacity);
        }

        @Override
        void add(FsArchiveFileSystemEntry<E> entry) {
            String fsName = entry.getName();
            String aeName = entry.getEntry().getName();
            if (aeName.startsWith(fsName))
                table.put(aeName, entry);
            else if (DIRECTORY == entry.getType())
                table.put(fsName + SEPARATOR_CHAR, entry);
            else
                table.put(fsName, entry);
        }

        @Override
        FsArchiveFileSystemEntry<E> get(FsEntryName name, Type type) {
            String path = name.getPath();
            if (null == type) {
                final FsArchiveFileSystemEntry<E> file = table.get(path);
                final FsArchiveFileSystemEntry<E> directory = table.get(path + SEPARATOR_CHAR);
                return null == file
                        ? null == directory
                            ? null
                            : directory
                        : null == directory
                            ? file
                            : new FsArchiveFileSystemEntry.HybridEntry<E>(
                                file.getEntry(), file, directory);
            } else if (DIRECTORY == type) {
                return table.get(path + SEPARATOR_CHAR);
            } else {
                return table.get(path);
            }
        }

        @Override
        void remove(FsEntryName name, Type type) {
            assert null != type;
            String path = name.getPath();
            table.remove(DIRECTORY == type ? path + SEPARATOR_CHAR : path);
        }
    } // class ZipOrTarEntryTable

    private static final class DefaultEntryTable<E extends FsArchiveEntry>
    extends MasterEntryTable<E> {
        DefaultEntryTable(int initialCapacity) {
            super(initialCapacity);
        }

        @Override
        void add(FsArchiveFileSystemEntry<E> entry) {
            table.put(entry.getName(), entry);
        }

        @Override
        FsArchiveFileSystemEntry<E> get(FsEntryName name, Type type) {
            return table.get(name.getPath());
        }

        @Override
        void remove(FsEntryName name, Type type) {
            table.remove(name.getPath());
        }
    } // class DefaultEntryTable

    /**
     * Splits a given path name into its parent path name and base name.
     * 
     * @param <E> The type of the archive entries.
     */
    private static abstract class MasterEntryTable<E extends FsArchiveEntry> {

        /**
         * The map of archive file system entries.
         * <p>
         * Note that the archive entries in this map are shared with the
         * {@link EntryContainer} object provided to the constructor of
         * this class.
         */
        final Map<String, FsArchiveFileSystemEntry<E>> table;

        MasterEntryTable(int initialCapacity) {
            this.table = new LinkedHashMap<String, FsArchiveFileSystemEntry<E>>(
                    initialCapacity);
        }

        final int getSize() {
            return table.size();
        }

        final Iterator<FsArchiveFileSystemEntry<E>> iterator() {
            class ArchiveEntryIterator implements Iterator<FsArchiveFileSystemEntry<E>> {
                final Iterator<FsArchiveFileSystemEntry<E>> it = table.values().iterator();

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public FsArchiveFileSystemEntry<E> next() {
                    return it.next();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            } // class ArchiveEntryIterator

            return new ArchiveEntryIterator();
        }

        abstract void add(FsArchiveFileSystemEntry<E> entry);

        abstract @CheckForNull FsArchiveFileSystemEntry<E>
        get(FsEntryName name, @CheckForNull Type type);

        abstract void remove(FsEntryName name, @CheckForNull Type type);
    } // class MasterEntryTable

    private static final class Splitter {
        private final de.schlichtherle.truezip.io.Paths.Splitter
                splitter = new de.schlichtherle.truezip.io.Paths.Splitter(SEPARATOR_CHAR, false);

        void split(FsEntryName name) {
            splitter.split(name.getPath());
        }

        FsEntryName getParentName() {
            String path = splitter.getParentPath();
            try {
                return null == path
                        ? ROOT
                        : new FsEntryName(  new URI(null, null, path, null),
                                            FsUriModifier.NULL);
            } catch (URISyntaxException ex) {
                throw new AssertionError(ex);
            }
        }

        String getMemberName() {
            return splitter.getMemberName();
        }
    } // class Splitter
}
