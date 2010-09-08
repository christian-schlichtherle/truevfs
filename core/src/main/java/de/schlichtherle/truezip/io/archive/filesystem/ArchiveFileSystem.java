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
import de.schlichtherle.truezip.io.IOOperation;
import de.schlichtherle.truezip.io.Paths;
import de.schlichtherle.truezip.io.Paths.Normalizer;
import de.schlichtherle.truezip.io.archive.driver.ArchiveEntryFactory;
import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry.Type;
import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.driver.InputArchive;
import de.schlichtherle.truezip.io.archive.driver.OutputArchive;
import de.schlichtherle.truezip.io.socket.IOOperations;
import de.schlichtherle.truezip.io.socket.IOReferences;
import de.schlichtherle.truezip.util.ExceptionHandler;
import java.io.CharConversionException;
import java.io.IOException;
import java.util.Collections;
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
public final class ArchiveFileSystem {

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
    private Map<String, Entry> master;

    /** The file system entry for the virtual root of this file system. */
    private final Entry root;

    /** The number of times this file system has been modified (touched). */
    private long touched;

    private final VetoableTouchListener vetoableTouchListener;

    /**
     * Creates a new archive file system and ensures its integrity.
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
    public ArchiveFileSystem(
            final ArchiveEntryFactory<? extends ArchiveEntry> factory,
            final VetoableTouchListener vetoableTouchListener)
    throws IOException {
        assert factory != null;

        this.factory = factory;
        master = new LinkedHashMap<String, Entry>(64);

        // Setup root.
        root = newEntry(ROOT, DIRECTORY);
        root.setTime(System.currentTimeMillis());
        master.put(ROOT, root);

        readOnly = false;

        this.vetoableTouchListener = vetoableTouchListener;
        touch();
    }

    /**
     * Populates this file system from the given {@code archive} and ensures
     * its integrity.
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
    public ArchiveFileSystem(
            final ArchiveEntryFactory<? extends ArchiveEntry> factory,
            final VetoableTouchListener vetoableTouchListener,
            final InputArchive<? extends ArchiveEntry> archive,
            final long rootTime,
            final boolean readOnly) {
        this.factory = factory;
        master = new LinkedHashMap<String, Entry>(
                (int) (archive.size() / 0.75f) + 1);

        final Normalizer normalizer = new Normalizer(SEPARATOR_CHAR);
        // Load entries from input archive.
        for (final ArchiveEntry entry : archive) {
            final String path = normalizer.normalize(entry.getName());
            master.put(path, Entry.wrap(entry));
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
    private Entry newEntry(final String path, final Type type) {
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
    private Entry newEntry(
            final String path,
            final Type type,
            final ArchiveEntry template)
    throws CharConversionException {
        assert isValidPath(path);
        assert type != null;
        assert !isRoot(path) || type == DIRECTORY;
        assert template == null || type == template.getType();

        return Entry.wrap(factory.newArchiveEntry(
                path, type, Entry.unwrap(template)));
    }

    /**
     * Returns {@code true} iff the given path name refers to the
     * virtual root directory of this file system.
     */
    public static boolean isRoot(String path) {
        assert ROOT.isEmpty();
        return path.isEmpty();
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
            Entry parent = master.get(parentPath);
            if (parent == null) {
                parent = newEntry(parentPath, DIRECTORY);
                master.put(parentPath, parent);
            }
            parent.add(baseName);
            fix(parentPath);
        }
    }

    /**
     * Indicates whether this file system is read only or not.
     * The default is {@code false}.
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Indicates whether this file system has been modified since
     * its time of creation or the last call to {@code resetTouched()}.
     */
    public boolean isTouched() {
        return touched != 0;
    }

    /**
     * Ensures that the controller's data structures required to output
     * entries are properly initialized and marks this virtual archive
     * file system as touched.
     *
     * @throws ArchiveReadOnlyExceptionn If this virtual archive file system
     *         is read only.
     * @throws IOException If setting up the required data structures in the
     *         controller fails for some reason.
     */
    private void touch() throws IOException {
        if (isReadOnly())
            throw new ReadOnlyArchiveFileSystemException();

        // Order is important here because of exceptions!
        if (touched == 0 && vetoableTouchListener != null)
            vetoableTouchListener.touch();
        touched++;
    }

    /**
     * Looks up the file system entry with the given path name and returns it
     * or {@code null} if not existent.
     */
    public Entry get(String path) {
        assert path != null;
        return master.get(path);
    }

    /**
     * This class defines the capabilities of the elements which are actually
     * put into the archive file system.
     * It's implemented as a decorator for {@link ArchiveEntry}s which
     * adds the methods required to implement the concept of a directory.
     */
    public abstract static class Entry
    extends FilterArchiveEntry<ArchiveEntry> {

        /**
         * Constructs a new instance of {@code Entry}
         * which decorates (wraps) the given archive entry.
         *
         * @throws NullPointerException If {@code entry} is {@code null}.
         */
        // TODO: Make this private!
        public static Entry wrap(final ArchiveEntry entry) {
            return entry instanceof Entry
                    ? (Entry) entry
                    : entry.getType() == DIRECTORY
                        ? new Directory(entry)
                        : new      File(entry);
        }

        /**
         * Returns the decorated target archive entry if and only if
         * {@code wrapper} is non-{@code null}.
         * Otherwise, {@code null} is returned.
         */
        // TODO: Make this private!
        public static ArchiveEntry unwrap(final Entry wrapper) {
            return wrapper != null ? wrapper.target : null;
        }

        /**
         * Returns the decorated target archive entry if and only if
         * {@code entry} is an instance of {@code Entry}.
         * Otherwise, {@code entry} is returned.
         */
        // TODO: Make this private!
        public static ArchiveEntry unwrap(final ArchiveEntry entry) {
            return entry instanceof Entry ? ((Entry) entry).target : entry;
        }

        /** Constructs a new instance of {@code Entry}. */
        private Entry(final ArchiveEntry entry) {
            super(entry);
            assert entry != null;
        }

        /** @throws UnsupportedOperationException */
        @Override
        public final void setSize(final long size) {
            throw new UnsupportedOperationException();
        }

        /**
         * Returns the number of members of this file system entry
         * if and only if this file system entry is a directory.
         *
         * @throws UnsupportedOperationException if this file system entry is
         *         not a directory.
         */
        public int size() {
            throw new UnsupportedOperationException();
        }

        /**
         * Visits the members of this directory in arbitrary order
         * if and only if this file system target is a directory.
         * First, {@link ChildVisitor#init} is called in order to initialize
         * the visitor.
         * Then {@link ChildVisitor#visit} is called for every member of this
         * directory.
         *
         * @throws UnsupportedOperationException if this file system target is
         *         not a directory.
         * @throws NullPointerException If {@code visitor} is {@code null}.
         */
        public void list(final ChildVisitor visitor) {
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

        private static final class File extends Entry {
            /** Constructs a new instance of {@code FileEntry}. */
            private File(final ArchiveEntry entry) {
                super(entry);
                assert entry.getType() != DIRECTORY;
            }
        } // class FileEntry

        private static final class Directory extends Entry {
            final Set<String> members = new LinkedHashSet<String>();

            /** Constructs a new instance of {@code DirectoryEntry}. */
            private Directory(final ArchiveEntry entry) {
                super(entry);
                assert entry.getType() == DIRECTORY;
            }

            @Override
            public int size() {
                return members.size();
            }

            @Override
            public void list(final ChildVisitor visitor) {
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
    } // interface Entry

    /**
     * Equivalent to {@link #link(String, ArchiveEntry.Type, boolean, ArchiveEntry)
     * link(path, type, createParents, null)}.
     */
    public LinkOperation link(
            final String path,
            final Type type,
            final boolean createParents)
    throws ArchiveFileSystemException {
        return new LinkOperation(path, type, createParents, null);
    }

    /**
     * Begins a &quot;create and link target&quot; transaction to ensure that
     * either a new target for the given {@code path} will be created or an
     * existing target is replaced within this archive file system.
     * <p>
     * This is the first step of a two-step process to create an archive target
     * and link it into this virtual archive file system.
     * To commit the transaction, call {@link IOOperation#run} on the
     * returned object after you have successfully conducted the operations
     * which compose the transaction.
     * <p>
     * Upon a {@code run} operation, the last modification time of
     * the newly created and linked entries will be set to the system's
     * current time at the moment the transaction has begun and the file
     * system will be marked as touched at the moment the transaction has
     * been committed.
     * <p>
     * Note that there is no rollback operation: After this method returns,
     * nothing in the virtual file system has changed yet and all information
     * required to commit the transaction is contained in the returned object.
     * Hence, if the operations which compose the transaction fails, the
     * returned object may be safely collected by the garbage collector,
     * 
     * @param path The relative path name of the target to create or replace.
     * @param createParents If {@code true}, any non-existing parent
     *        directory will be created in this file system with its last
     *        modification time set to the system's current time.
     * @param template If not {@code null}, then the newly created or
     *        replaced target shall inherit as much properties from this
     *        instance as possible (with the exception of the name).
     *        This is typically used for archive copy operations and requires
     *        some support by the archive driver.
     * @return An I/O operation. You must call its {@link IOOperation#run}
     *         method in order to link the newly created target into this
     *         archive file system.
     * @throws ArchiveReadOnlyExceptionn If this virtual archive file system
     *         is read only.
     * @throws ArchiveFileSystemException If one of the following is true:
     *         <ul>
     *         <li>{@code path} contains characters which are not
     *             supported by the archive file.
     *         <li>The target name indicates a directory (trailing {@code /})
     *             and its target does already exist within this file system.
     *         <li>The target is a file or directory and does already exist as
     *             the respective other type within this file system.
     *         <li>The parent directory does not exist and
     *             {@code createParents} is {@code false}.
     *         <li>One of the target's parents denotes a file.
     *         </ul>
     */
    public LinkOperation link(
            final String path,
            final Type type,
            final boolean createParents,
            final ArchiveEntry template)
    throws ArchiveFileSystemException {
        return new LinkOperation(path, type, createParents, template);
    }

    /**
     * A simple transaction for creating (and hence probably replacing) and
     * linking an target in this archive file system.
     * 
     * @see #link
     */
    public class LinkOperation implements IOOperation, IOReference<Entry> {
        final Splitter splitter = new Splitter();
        final PathNameElement[] elements;

        private LinkOperation(
                final String entryPath,
                final Type entryType,
                final boolean createParents,
                final ArchiveEntry template)
        throws ArchiveFileSystemException {
            if (isReadOnly())
                throw new ReadOnlyArchiveFileSystemException();
            if (isRoot(entryPath))
                throw new ArchiveFileSystemException(entryPath,
                        "cannot replace root directory entry");
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
            final Entry parentEntry = master.get(parentPath);
            final Entry newEntry;
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

        /** Links the entries into this virtual archive file system. */
        @Override
        public void run() throws IOException {
            assert elements.length >= 2;

            touch();

            final long time = System.currentTimeMillis();
            final int l = elements.length;

            Entry parent = elements[0].entry;
            for (int i = 1; i < l ; i++) {
                final PathNameElement element = elements[i];
                final String path = element.path;
                final Entry entry = element.entry;
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
        public Entry get() {
            return elements[elements.length - 1].entry;
        }
    } // class LinkOperation

    /**
     * A data class which represents a path name base for use by
     * {@link LinkOperation}.
     */
    private static class PathNameElement {
        final String path;
        final Entry entry;
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
                final Entry entry,
                final String base) {
            assert path != null;
            assert entry != null;
            this.path = path;
            this.entry = entry;
            this.base = base; // may be null!
        }
    }

    /**
     * If this method returns, the file system entry identified by the given
     * {@code path} has been successfully deleted from this archive file
     * system.
     * If the file system entry is a directory, it must be empty for successful
     * deletion.
     * 
     * @throws ArchiveReadOnlyExceptionn If the virtual archive file system is
     *         read only.
     * @throws ArchiveFileSystemException If the operation fails for
     *         any other reason.
     */
    private void unlink(final String path)
    throws IOException {
        if (isRoot(path))
            throw new ArchiveFileSystemException(path,
                    "virtual root directory cannot get unlinked");
        try {
            final Entry entry = master.remove(path);
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
            final Entry parent = master.get(parentPath);
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

    //
    // File system operations used by the ArchiveController class:
    //

    public boolean isExisting(final String path) {
        return get(path) != null;
    }

    public boolean isFile(final String path) {
        final ArchiveEntry entry = get(path);
        return entry != null && entry.getType() == FILE;
    }
    
    public boolean isDirectory(final String path) {
        final ArchiveEntry entry = get(path);
        return entry != null && entry.getType() == DIRECTORY;
    }
    
    public boolean isWritable(final String path) {
        return !isReadOnly() && isFile(path);
    }

    public boolean setReadOnly(final String path) {
        return isReadOnly() && isFile(path);
    }
    
    public long getLength(final String path) {
        final Entry entry = get(path);
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

    public long getLastModified(final String path) {
        final Entry entry = get(path);
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

    public boolean setLastModified(final String path, final long time)
    throws IOException {
        if (time < 0)
            throw new IllegalArgumentException(path +
                    " (negative entry modification time)");

        if (isReadOnly())
            return false;

        final Entry entry = get(path);
        if (entry == null)
            return false;

        // Order is important here!
        touch();
        entry.setTime(time);

        return true;
    }

    public int getNumChildren(final String path) {
        final Entry entry = get(path);
        return entry != null && entry.getType() == DIRECTORY
                ? entry.size()
                : 0; // does not exist as a directory
    }

    public void list(final String path, final ChildVisitor visitor) {
        final Entry entry = get(path);
        if (entry != null && entry.getType() == DIRECTORY)
            entry.list(visitor);
    }
    
    public void mkdir(String path, boolean createParents)
    throws IOException {
        link(path, DIRECTORY, createParents).run();
    }

    public void delete(final String path)
    throws IOException {
        assert isRoot(path) || path.charAt(0) != SEPARATOR_CHAR;

        if (get(path) != null) {
            unlink(path);
            return;
        }
        throw new ArchiveFileSystemException(path,
                "archive entry does not exist");
    }

    public <E extends Exception>
    void copy(
            final InputArchive<ArchiveEntry> ia,
            final OutputArchive<ArchiveEntry> oa,
            final ExceptionHandler<IOException, E> h)
    throws E {
        final ArchiveEntry root = Entry.unwrap(this.root);
        assert root != null;
        for (final Entry v : master.values()) {
            final ArchiveEntry e = Entry.unwrap(v);
            final String n = e.getName();
            if (oa.getEntry(n) != null)
                continue; // we have already written this target
            try {
                if (e.getType() == DIRECTORY) {
                    if (root == e)
                        continue; // never write the virtual root directory
                    if (e.getTime() < 0)
                        continue; // never write ghost directories
                    oa.getOutputStreamSocket(e)
                            .newOutputStream(IOReferences.ref((ArchiveEntry) null))
                            .close();
                } else if (ia != null && ia.getEntry(n) != null) {
                    assert e == ia.getEntry(n);
                    IOOperations.copy(  ia.getInputStreamSocket(e),
                                        oa.getOutputStreamSocket(e));
                } else {
                    // The file system entry is an archive file which has been
                    // newly created and not yet been reassembled
                    // into this (potentially new) archive file.
                    // Write an empty file system entry now as a marker in
                    // order to recreate the file system entry when the file
                    // system gets remounted from the archive file.
                    oa.getOutputStreamSocket(e)
                            .newOutputStream(IOReferences.ref((ArchiveEntry) null))
                            .close();
                }
            } catch (IOException ex) {
                h.warn(ex);
            }
        }
    }
}
