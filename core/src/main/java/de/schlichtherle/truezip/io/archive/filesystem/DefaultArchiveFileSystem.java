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

import de.schlichtherle.truezip.io.archive.driver.spi.FilterArchiveEntry;
import de.schlichtherle.truezip.io.socket.IOReference;
import de.schlichtherle.truezip.io.Paths;
import de.schlichtherle.truezip.io.Paths.Normalizer;
import de.schlichtherle.truezip.io.archive.driver.ArchiveEntryFactory;
import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry.Type;
import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.driver.InputArchive;
import java.io.CharConversionException;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static de.schlichtherle.truezip.io.archive.driver.ArchiveEntry.ROOT;
import static de.schlichtherle.truezip.io.archive.driver.ArchiveEntry.SEPARATOR;
import static de.schlichtherle.truezip.io.archive.driver.ArchiveEntry.SEPARATOR_CHAR;
import static de.schlichtherle.truezip.io.archive.driver.ArchiveEntry.UNKNOWN;
import static de.schlichtherle.truezip.io.archive.driver.ArchiveEntry.Type.DIRECTORY;
import static de.schlichtherle.truezip.io.archive.driver.ArchiveEntry.Type.FILE;
import static de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystems.isRoot;
import static de.schlichtherle.truezip.io.Paths.normalize;

/**
 * This class implements a virtual file system of archive entries.
 * <p>
 * This class is <em>not</em> thread-safe!
 * Multithreading needs to be addressed by client classes.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
class DefaultArchiveFileSystem implements ArchiveFileSystem {

    /** The controller that this filesystem belongs to. */
    private final ArchiveEntryFactory<? extends ArchiveEntry> factory;

    /** The read only status of this file system. */
    private final boolean readOnly;

    /**
     * The map of archive entries in this file system.
     * If this is a read-only file system, this is actually an unmodifiable
     * map.
     * This field should be considered final!
     * <p>
     * Note that the archive entries in this map are shared with the
     * {@link InputArchive} object provided to this class' constructor.
     */
    private Map<String, CommonEntry> master;

    /** The file system entry for the virtual root of this file system. */
    private final CommonEntry root;

    /** The number of times this file system has been modified (touched). */
    private long touched;

    private final VetoableTouchListener vetoableTouchListener;

    /**
     * Constructs a new archive file system and ensures its integrity.
     * The root directory is created with its last modification time set to
     * the system's current time.
     * The file system is modifiable and marked as touched!
     * 
     * @param factory the archive entry factory to use.
     * @param vetoableTouchListener the nullable listener for touch events.
     *        If not {@code null}, its {@link VetoableTouchListener#touch()}
     *        method will be called at the end of this constructor and whenever
     *        a client class changes the state of this archive file system.
     * @throws NullPointerException If {@code factory} is {@code null}.
     */
    DefaultArchiveFileSystem(
            final ArchiveEntryFactory<? extends ArchiveEntry> factory,
            final VetoableTouchListener vetoableTouchListener)
    throws ArchiveFileSystemException {
        assert factory != null;

        this.factory = factory;
        master = new LinkedHashMap<String, CommonEntry>(64);

        // Setup root.
        root = newEntry(ROOT, DIRECTORY);
        root.setTime(System.currentTimeMillis());
        master.put(ROOT, root);

        readOnly = false;

        this.vetoableTouchListener = vetoableTouchListener;
        touch();
    }

    /**
     * Constructs a new archive file system which populates its entries from
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
     * Note that the entries in this file system are shared with the given
     * {@code archive}.
     * 
     * @param factory the archive entry factory to use.
     * @param vetoableTouchListener the nullable listener for touch events.
     *        If not {@code null}, its {@link VetoableTouchListener#touch()}
     *        method will be called whenever a client class changes the state
     *        of this archive file system.
     * @param archive The input archive to read the entries for the population
     *        of this file system.
     * @param rootTime The last modification time of the root of the populated
     *        file system in milliseconds since the epoch.
     * @param readOnly If and only if {@code true}, any subsequent
     *        modifying operation on this file system will result in a
     *        {@link ReadOnlyArchiveFileSystemException}.
     * @throws NullPointerException If {@code factory} or {@code archive}
     *         is {@code null}.
     */
    DefaultArchiveFileSystem(
            final ArchiveEntryFactory<? extends ArchiveEntry> factory,
            final VetoableTouchListener vetoableTouchListener,
            final InputArchive<? extends ArchiveEntry> archive,
            final long rootTime,
            final boolean readOnly) {
        this.factory = factory;
        master = new LinkedHashMap<String, CommonEntry>(
                (int) (archive.size() / 0.75f) + 1);

        final Normalizer normalizer = new Normalizer(SEPARATOR_CHAR);
        // Load entries from input archive.
        for (final ArchiveEntry entry : archive) {
            final String path = normalizer.normalize(entry.getName());
            master.put(path, wrap(entry));
        }

        // Setup root file system entry, potentially replacing its previous
        // mapping from the input archive.
        root = newEntry(ROOT, DIRECTORY);
        root.setTime(rootTime); // do NOT yet touch the file system!
        master.put(ROOT, root);

        // Now perform a file system check to create missing parent directories
        // and populate directories with their children - this needs to be done
        // separately!
        // entries = Collections.enumeration(master.values()); // concurrent modification!
        final Check fsck = new Check();
        for (final ArchiveEntry entry : archive) {
            final String path = normalizer.normalize(entry.getName());
            if (isValidPath(path))
                fsck.fix(path);
        }

        // Make master map unmodifiable if this is a readonly file system
        this.readOnly = readOnly;
        if (readOnly)
            master = Collections.unmodifiableMap(master);

        assert !isTouched();
        this.vetoableTouchListener = vetoableTouchListener;
    }

    /**
     * Like {@link #newEntry(String, ArchiveEntry.Type, ArchiveEntry)
     * newEntry(path, type, null)}, but throws an
     * {@link AssertionError} instead of a {@link CharConversionException}.
     *
     * @throws AssertionError if a {@link CharConversionException} occurs.
     *         The original exception is wrapped as its cause.
     */
    private CommonEntry newEntry(final String path, final Type type) {
        try {
            return newEntry(path, type, null);
        } catch (CharConversionException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * Returns a new file system entry for this virtual archive file system.
     * The returned file system entry is not yet linked into this virtual
     * archive file system.
     *
     * @see    #link
     * @param  path the non-{@code null} path name of the new file system entry.
     *         This is always a {@link #isValidPath(String) valid path name}.
     * @param  type the non-{@code null} type of the new file system entry.
     * @param  template if not {@code null}, then the new file system entry
     *         shall inherit as much properties from this archive entry
     *         as possible (with the exception of its entry name).
     *         This is typically used for copy operations.
     * @return A non-{@code null} file system entry.
     * @throws CharConversionException if {@code path} contains characters
     *         which are not supported by the archive file.
     */
    private CommonEntry newEntry(
            final String path,
            final Type type,
            final ArchiveEntry template)
    throws CharConversionException {
        assert isValidPath(path);
        assert type != null;
        assert !isRoot(path) || type == DIRECTORY;
        assert template == null || type == template.getType();

        return wrap(factory.newArchiveEntry(path, type, unwrap(template)));
    }

    /**
     * Checks whether the given path name is a <i>valid path name</i>.
     * A valid path name is in
     * {@link Paths#normalize(String, char) normal form},
     * is relative, does not identify the dot directory ({@code "."}) or
     * the dot-dot directory ({@code ".."}) or any of their descendants.
     *
     * @see    ArchiveEntryFactory#newArchiveEntry Common Requirements For Path Names
     * @param  name a non-{@code null} path name.
     */
    private static boolean isValidPath(final String name) {
        if (isRoot(name))
            return true;

        if (name != normalize(name, SEPARATOR_CHAR)) // mind contract!
            return false;

        final int length = name.length();
        assert length > 0 || isRoot(name) : "Definition of ROOT changed!?";
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

    private static class Splitter
    extends de.schlichtherle.truezip.io.Paths.Splitter {
        Splitter() {
            super(SEPARATOR_CHAR);
        }

        /**
         * Splits the given path name in a parent path name and a base name.
         * Iff the given path name does not name a parent directory, then
         * {@link ArchiveEntry#ROOT} is set at index zero of the returned array.
         *
         * @param  path The path name which's parent path name and base name
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
            final String baseName = getBaseName();
            CommonEntry parent = master.get(parentPath);
            if (parent == null) {
                parent = newEntry(parentPath, DIRECTORY);
                master.put(parentPath, parent);
            }
            parent.add(baseName);
            fix(parentPath);
        }
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public final boolean isTouched() {
        return touched != 0;
    }

    /**
     * Ensures that the controller's data structures required to output
     * entries are properly initialized and marks this virtual archive
     * file system as touched.
     *
     * @throws ArchiveReadOnlyExceptionn If this virtual archive file system
     *         is read only.
     * @throws ArchiveFileSystemException If setting up the required data structures in the
     *         controller fails for some reason.
     */
    private void touch() throws ArchiveFileSystemException {
        if (isReadOnly())
            throw new ReadOnlyArchiveFileSystemException();

        // Order is important here because of exceptions!
        if (touched == 0 && vetoableTouchListener != null) {
            try {
                vetoableTouchListener.touch();
            } catch (IOException ex) {
                throw new ArchiveFileSystemException(null, "touch vetoed", ex);
            }
        }
        touched++;
    }

    @Override
    public Iterator<IOReference<? extends ArchiveEntry>> iterator() {
        return (Iterator) master.values().iterator(); // FIXME: Make this typesafe!
    }

    @Override
    public Entry getReference(String path) {
        if (path == null)
            throw new NullPointerException();
        return master.get(path);
    }

    /**
     * Defines the features of the file system entries in this archive file
     * system.
     */
    private interface Entry
    extends ArchiveEntry, IOReference<ArchiveEntry> {

        /** @throws UnsupportedOperationException */
        @Override
        void setSize(long size);

        /**
         * Returns the number of members of this file system entry
         * if and only if this file system entry is a directory.
         *
         * @throws UnsupportedOperationException if this file system entry is
         *         not a directory.
         */
        int size();

        /**
         * Visits the members of this directory in arbitrary order
         * if and only if this file system target is a directory.
         * First, {@link MemberVisitor#init} is called in order to initialize
         * the visitor.
         * Then {@link MemberVisitor#visit} is called for every member of this
         * directory.
         *
         * @throws UnsupportedOperationException if this file system target is
         *         not a directory.
         * @throws NullPointerException If {@code visitor} is {@code null}.
         */
        void list(MemberVisitor visitor);
    } // interface Entry

    /**
     * Constructs a new instance of {@code Entry}
     * which decorates (wraps) the given archive entry.
     *
     * @throws NullPointerException If {@code entry} is {@code null}.
     */
    private static CommonEntry wrap(final ArchiveEntry entry) {
        return entry instanceof CommonEntry
                ? (CommonEntry) entry
                : entry.getType() == DIRECTORY
                    ? new DirectoryEntry(entry)
                    : new      FileEntry(entry);
    }

    /**
     * Returns the decorated target archive entry if and only if
     * {@code wrapper} is non-{@code null}.
     * Otherwise, {@code null} is returned.
     */
    private static ArchiveEntry unwrap(final Entry wrapper) {
        return wrapper != null ? wrapper.get() : null;
    }

    /**
     * Returns the decorated target archive entry if and only if
     * {@code entry} is an instance of {@code Entry}.
     * Otherwise, {@code entry} is returned.
     */
    private static ArchiveEntry unwrap(final ArchiveEntry entry) {
        return entry instanceof Entry ? ((Entry) entry).get() : entry;
    }

    /**
     * This class defines the capabilities of the elements which are actually
     * put into the archive file system.
     * It's implemented as a decorator for {@link ArchiveEntry}s which
     * adds the methods required to implement the concept of a directory.
     */
    private abstract static class CommonEntry
    extends FilterArchiveEntry<ArchiveEntry>
    implements Entry {

        /** Constructs a new instance of {@code Entry}. */
        CommonEntry(final ArchiveEntry entry) {
            super(entry);
            assert entry != null;
        }

        @Override
        public final void setSize(final long size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void list(final MemberVisitor visitor) {
            throw new UnsupportedOperationException();
        }

        /**
         * Adds the given base name to the set of members of this directory
         * if and only if this file system target is a directory.
         *
         * @param  member The non-{@code null} base name of the member to add.
         * @return Whether the member has been added or an equal member was
         *         already present in the directory.
         * @throws UnsupportedOperationException if this file system target is
         *         not a directory.
         */
        boolean add(final String member) {
            throw new UnsupportedOperationException();
        }

        /**
         * Removes the given base name from the set of members of this
         * directory
         * if and only if this file system target is a directory.
         *
         * @param  member The non-{@code null} base name of the member to
         *         remove.
         * @return Whether the member has been removed or no equal member was
         *         present in the directory.
         * @throws UnsupportedOperationException if this file system target is
         *         not a directory.
         */
        boolean remove(final String member) {
            throw new UnsupportedOperationException();
        }

        @Override
        public final ArchiveEntry get() {
            return target;
        }
    } // class CommonEntry

    static final class FileEntry extends CommonEntry {
        /** Constructs a new instance of {@code FileEntry}. */
        FileEntry(final ArchiveEntry entry) {
            super(entry);
            assert entry.getType() != DIRECTORY;
        }
    } // class FileEntry

    static final class DirectoryEntry extends CommonEntry {
        final Set<String> members = new LinkedHashSet<String>();

        /** Constructs a new instance of {@code DirectoryEntry}. */
        DirectoryEntry(final ArchiveEntry entry) {
            super(entry);
            assert entry.getType() == DIRECTORY;
        }

        @Override
        public int size() {
            return members.size();
        }

        @Override
        public void list(final MemberVisitor visitor) {
            visitor.init(members.size());
            for (final String member : members)
                visitor.visit(member);
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

    @Override
    public Link mknod(
            final String path,
            final Type type,
            final ArchiveEntry template, final boolean createParents)
    throws ArchiveFileSystemException {
        return new Operation(path, type, createParents, template);
    }

    private class Operation implements Link {
        final Splitter splitter = new Splitter();
        final PathNameElement[] elements;

        Operation(
                final String entryPath,
                final Type entryType,
                final boolean createParents,
                final ArchiveEntry template)
        throws ArchiveFileSystemException {
            if (isReadOnly())
                throw new ReadOnlyArchiveFileSystemException();
            if (isRoot(entryPath))
                throw new ArchiveFileSystemException(entryPath,
                        "cannot replace virtual root directory entry");
            if (!isValidPath(entryPath))
                throw new ArchiveFileSystemException(entryPath,
                        "is not a valid path name");
            if (entryType != FILE && entryType != DIRECTORY)
                throw new ArchiveFileSystemException(entryPath,
                        "only FILE and DIRECTORY entries are currently supported");
            try {
                elements = newPathNameElements(
                        entryPath, entryType, createParents, template, 1);
            } catch (CharConversionException ex) {
                throw new ArchiveFileSystemException(entryPath, ex);
            }
        }

        private PathNameElement[] newPathNameElements(
                final String entryPath,
                final Type entryType,
                final boolean createParents,
                final ArchiveEntry template,
                final int level)
        throws ArchiveFileSystemException, CharConversionException {
            final String split[] = splitter.split(entryPath);
            final String parentPath = split[0]; // could equal ROOT
            final String baseName = split[1];
            final PathNameElement[] elements;

            // Lookup parent target, creating it where necessary and allowed.
            final CommonEntry parentEntry = master.get(parentPath);
            final CommonEntry newEntry;
            if (parentEntry != null) {
                if (parentEntry.getType() != DIRECTORY)
                    throw new ArchiveFileSystemException(entryPath,
                            "parent entry must be a directory");
                final Entry oldEntry = master.get(entryPath);
                if (entryType == DIRECTORY) {
                    if (oldEntry != null) {
                        throw new ArchiveFileSystemException(entryPath,
                                "directory entries cannot replace existing entries");
                    }
                } else {
                    assert entryType == FILE;
                    if (oldEntry != null && oldEntry.getType() == DIRECTORY)
                        throw new ArchiveFileSystemException(entryPath,
                                "directory entries cannot get replaced");
                }
                elements = new PathNameElement[level + 1];
                elements[0] = new PathNameElement(parentPath, parentEntry, null);
                newEntry = newEntry(entryPath, entryType, template);
                elements[1] = new PathNameElement(entryPath, newEntry, baseName);
            } else if (createParents) {
                elements = newPathNameElements(
                        parentPath, DIRECTORY, createParents, null, level + 1);
                newEntry = newEntry(entryPath, entryType, template);
                elements[elements.length - level]
                        = new PathNameElement(entryPath, newEntry, baseName);
            } else {
                throw new ArchiveFileSystemException(entryPath,
                        "missing parent directory entry");
            }
            return elements;
        }

        @Override
        public void run() throws ArchiveFileSystemException {
            assert elements.length >= 2;

            touch();
            final int l = elements.length;
            final long time = System.currentTimeMillis();
            CommonEntry parent = elements[0].entry;
            for (int i = 1; i < l ; i++) {
                final PathNameElement element = elements[i];
                final String path = element.path;
                final CommonEntry entry = element.entry;
                final String base = element.base;
                assert parent.getType() == DIRECTORY;
                if (parent.add(base) && parent.getTime() != UNKNOWN) // never touch ghosts!
                    parent.setTime(time);
                master.put(path, entry);
                parent = entry;
            }
            final Entry entry = elements[l - 1].entry;
            if (entry.getTime() == UNKNOWN)
                entry.setTime(time);
        }

        @Override
        public ArchiveEntry get() {
            return elements[elements.length - 1].entry.get();
        }
    } // class Operation

    /**
     * A data class which represents a path name element for use by
     * {@link Operation}.
     */
    private static class PathNameElement {
        final String path;
        final CommonEntry entry;
        final String base;

        /**
         * Constructs a new {@code LinkStep}.
         *
         * @param path The non-{@code null} normalized path name of the file
         *        system entry.
         * @param entry The non-{@code null} file system entry for the path
         *        name.
         * @param base The nullable base name of the path name.
         */
        PathNameElement(
                final String path,
                final CommonEntry entry,
                final String base) {
            assert path != null;
            assert entry != null;
            this.path = path;
            this.entry = entry;
            this.base = base; // may be null!
        }
    }

    @Override
    public void unlink(final String path) throws ArchiveFileSystemException {
        assert isRoot(path) || path.charAt(0) != SEPARATOR_CHAR;

        if (isRoot(path))
            throw new ArchiveFileSystemException(path,
                    "virtual root directory cannot get unlinked");
        try {
            final CommonEntry entry = master.remove(path);
            if (entry == null)
                throw new ArchiveFileSystemException(path,
                        "entry does not exist");
            assert entry != root;
            if (entry.getType() == DIRECTORY && entry.size() != 0) {
                master.put(path, entry); // Restore file system
                throw new ArchiveFileSystemException(path,
                        "directory is not empty");
            }
            final Splitter splitter = new Splitter();
            splitter.split(path);
            final String parentPath = splitter.getParentPath();
            final CommonEntry parent = master.get(parentPath);
            assert parent != null : "The parent directory of \"" + path
                        + "\" is missing - archive file system is corrupted!";
            final boolean ok = parent.remove(splitter.getBaseName());
            assert ok : "The parent directory of \"" + path
                        + "\" does not contain this entry - archive file system is corrupted!";
            touch();
            if (parent.getTime() != UNKNOWN) // never touch ghosts!
                parent.setTime(System.currentTimeMillis());
        } catch (UnsupportedOperationException unmodifiableMap) {
            throw new ReadOnlyArchiveFileSystemException();
        }
    }

    @Override
    public Type getType(final String path) {
        final ArchiveEntry entry = getReference(path);
        return entry != null ? entry.getType() : null;
    }

    @Override
    public boolean isWritable(final String path) {
        return !isReadOnly() && getType(path) == FILE;
    }

    @Override
    public void setReadOnly(final String path)
    throws ArchiveFileSystemException {
        if (!isReadOnly() || getType(path) != FILE)
            throw new ArchiveFileSystemException(path,
                "cannot set read-only state");
    }

    @Override
    public long getLength(final String path) {
        final Entry entry = getReference(path);
        if (entry == null || entry.getType() == DIRECTORY)
            return 0;

        // TODO: Review: Can we avoid this special case?
        // It's probably ZipDriver specific!
        // This target is a plain file in the file system.
        // If target.getSize() returns UNKNOWN, the getLength is yet unknown.
        // This may happen if e.g. a ZIP target has only been partially
        // written, i.e. not yet closed by another thread, or if this is a
        // ghost directory.
        // As this is not specified in the contract of this class,
        // return 0 in this case instead.
        final long length = entry.getSize();
        return length >= 0 ? length : 0;
    }

    @Override
    public long getLastModified(final String path) {
        final Entry entry = getReference(path);
        if (entry != null) {
            // Depending on the driver type, target.getTime() could return
            // a negative value. E.g. this is the default value that the
            // ArchiveDriver uses for newly created entries in order to
            // indicate an unknown time.
            // As this is not specified in the contract of this class,
            // 0 is returned in this case instead.
            final long time = entry.getTime();
            return time >= 0 ? time : 0;
        }
        // This target does not exist.
        return 0;
    }

    @Override
    public boolean setLastModified(final String path, final long time)
    throws ArchiveFileSystemException {
        if (time < 0)
            throw new IllegalArgumentException(path +
                    " (negative entry modification time)");

        if (isReadOnly())
            return false;

        final Entry entry = getReference(path);
        if (entry == null)
            return false;

        // Order is important here!
        touch();
        entry.setTime(time);

        return true;
    }

    @Override
    public int getNumMembers(final String path) {
        final Entry entry = getReference(path);
        return entry != null && entry.getType() == DIRECTORY ? entry.size() : 0;
    }

    @Override
    public void list(final String path, final MemberVisitor visitor) {
        final Entry entry = getReference(path);
        if (entry != null && entry.getType() == DIRECTORY)
            entry.list(visitor);
    }
}
