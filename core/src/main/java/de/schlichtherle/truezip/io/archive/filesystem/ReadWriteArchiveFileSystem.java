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

import de.schlichtherle.truezip.io.archive.entry.CommonEntry;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.entry.CommonEntry.Type;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntryContainer;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntryFactory;
import de.schlichtherle.truezip.io.Paths;
import de.schlichtherle.truezip.io.Paths.Normalizer;
import de.schlichtherle.truezip.io.archive.entry.UnmodifiableArchiveEntry;
import de.schlichtherle.truezip.io.socket.IOReference;
import java.io.CharConversionException;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.ROOT;
import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.SEPARATOR;
import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.SEPARATOR_CHAR;
import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.UNKNOWN;
import static de.schlichtherle.truezip.io.archive.entry.CommonEntry.Type.DIRECTORY;
import static de.schlichtherle.truezip.io.archive.entry.CommonEntry.Type.FILE;
import static de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystems.isRoot;
import static de.schlichtherle.truezip.io.Paths.normalize;

/**
 * A read/write archive file system.
 * <p>
 * This class is <em>not</em> thread-safe!
 * Multithreading needs to be addressed by client classes.
 * 
 * @param   <AE> The type of the archive entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
class ReadWriteArchiveFileSystem<AE extends ArchiveEntry>
implements ArchiveFileSystem<AE> {

    /** The controller that this filesystem belongs to. */
    private final ArchiveEntryFactory<? extends AE> factory;

    /**
     * The map of archive entries in this file system.
     * If this is a read-only file system, this is actually an unmodifiable
     * map.
     * This field should be considered final!
     * <p>
     * Note that the archive entries in this map are shared with the
     * {@link ArchiveEntryContainer} object provided to the constructor of
     * this class.
     */
    private Map<String, BaseEntry<AE>> master;

    /** The file system entry for the virtual root of this file system. */
    private final BaseEntry<AE> root;

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
    ReadWriteArchiveFileSystem(
            final ArchiveEntryFactory<AE> factory,
            final VetoableTouchListener vetoableTouchListener)
    throws ArchiveFileSystemException {
        assert factory != null;

        this.factory = factory;
        master = new LinkedHashMap<String, BaseEntry<AE>>(64);

        // Setup root.
        root = newEntry(ROOT, DIRECTORY);
        root.getTarget().setTime(System.currentTimeMillis());
        master.put(ROOT, root);

        this.vetoableTouchListener = vetoableTouchListener;
        touch();
    }

    /**
     * @see ArchiveFileSystems#newArchiveFileSystem(ArchiveEntryContainer, long, ArchiveEntryFactory, VetoableTouchListener, boolean)
     */
    ReadWriteArchiveFileSystem(
            final ArchiveEntryContainer<AE> container,
            final long rootTime,
            final ArchiveEntryFactory<AE> factory,
            final VetoableTouchListener vetoableTouchListener) {
        this.factory = factory;
        master = new LinkedHashMap<String, BaseEntry<AE>>(
                (int) (container.size() / 0.75f) + 1);

        final Normalizer normalizer = new Normalizer(SEPARATOR_CHAR);
        // Load entries from input archive.
        for (final AE entry : container) {
            final String path = normalizer.normalize(entry.getName());
            master.put(path, wrap(entry));
        }

        // Setup root file system entry, potentially replacing its previous
        // mapping from the input archive.
        root = newEntry(ROOT, DIRECTORY);
        root.getTarget().setTime(rootTime); // do NOT yet touch the file system!
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
    private BaseEntry<AE> newEntry(final String path, final Type type) {
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
     * @see    #mknod
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
    private BaseEntry<AE> newEntry(
            final String path,
            final Type type,
            final CommonEntry template)
    throws CharConversionException {
        assert isValidPath(path);
        assert type != null;
        assert !isRoot(path) || type == DIRECTORY;
        assert template == null || type == template.getType();
        assert !(template instanceof BaseEntry);

        return wrap(factory.newEntry(path, type, template));
    }

    /**
     * Checks whether the given path name is a <i>valid path name</i>.
     * A valid path name is in
     * {@link Paths#normalize(String, char) normal form},
     * is relative, does not identify the dot directory ({@code "."}) or
     * the dot-dot directory ({@code ".."}) or any of their descendants.
     *
     * @see    ArchiveEntryFactory#newEntry Common Requirements For Path Names
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

    /** Splits a path name into a parent path name and a base name. */
    private static class Splitter
    extends de.schlichtherle.truezip.io.Paths.Splitter {
        Splitter() {
            super(SEPARATOR_CHAR);
        }

        /**
         * Splits the given path name into a parent path name and a base name.
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
            BaseEntry<AE> parent = master.get(parentPath);
            if (parent == null) {
                parent = newEntry(parentPath, DIRECTORY);
                master.put(parentPath, parent);
            }
            parent.add(baseName);
            fix(parentPath);
        }
    }

    /** The implementation in this class returns {@code false}. */
    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
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
     * @throws ArchiveFileSystemException If setting up the required data structures in the
     *         controller fails for some reason.
     */
    private void touch() throws ArchiveFileSystemException {
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
    public int size() {
        return master.size();
    }

    @Override
    public Iterator<AE> iterator() {
        class ArchiveEntryIterator implements Iterator<AE> {
            final Iterator<BaseEntry<AE>> it = master.values().iterator();

            public boolean hasNext() {
                return it.hasNext();
            }

            public AE next() {
                return it.next().getTarget(); // must return target!
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        }
        return new ArchiveEntryIterator();
    }

    @Override
    public AE getEntry(String path) {
        if (path == null)
            throw new NullPointerException();
        final BaseEntry<AE> entry = master.get(path);
        return entry == null ? null : entry.getTarget();
    }

    /**
     * Constructs a new instance of {@code CommonEntry}
     * which decorates (wraps) the given archive entry.
     *
     * @throws NullPointerException If {@code entry} is {@code null}.
     */
    private static <AE extends ArchiveEntry>
    BaseEntry<AE> wrap(final AE entry) {
        return entry.getType() == DIRECTORY
                ? new DirectoryEntry(entry)
                : new      FileEntry(entry);
    }

    /**
     * Defines the common features of all entries in this archive file system.
     * It decorates an {@link ArchiveEntry} in order to add the methods
     * required to implement the concept of a directory.
     */
    private static abstract class BaseEntry<AE extends ArchiveEntry>
    extends UnmodifiableArchiveEntry<AE>
    implements IOReference<AE> {

        /** Constructs a new instance of {@code CommonEntry}. */
        BaseEntry(final AE entry) {
            super(entry);
            assert entry != null;
        }

        /**
         * If this is not a directory entry, {@code null} is returned.
         * Otherwise, an unmodifiable set of strings is returned which
         * represent the base names of the members of this directory entry.
         */
        Set<String> list() {
            return null;
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

        /** Returns the decorated archive entry. */
        @Override
        public final AE getTarget() {
            return target;
        }
    } // class CommonEntry

    /** A file entry. */
    private static final class FileEntry<AE extends ArchiveEntry>
    extends BaseEntry<AE> {
        /** Decorates the given archive entry. */
        FileEntry(final AE entry) {
            super(entry);
            assert entry.getType() != DIRECTORY;
        }
    } // class FileEntry

    /** A directory entry. */
    private static final class DirectoryEntry<AE extends ArchiveEntry>
    extends BaseEntry<AE> {
        Set<String> members = new LinkedHashSet<String>();

        /** Decorates the given archive entry. */
        DirectoryEntry(final AE entry) {
            super(entry);
            assert entry.getType() == DIRECTORY;
        }

        @Override
        Set<String> list() {
            if (!(members instanceof CopyOnWriteArraySet))
                members = new CopyOnWriteArraySet<String>(members);
            return Collections.unmodifiableSet(members);
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
    public Link<AE> mknod(
            final String path,
            final Type type,
            final CommonEntry template,
            final boolean createParents)
    throws ArchiveFileSystemException {
        return new PathLink(path, type, template, createParents);
    }

    private final class PathLink implements Link<AE> {
        final Splitter splitter = new Splitter();
        final boolean createParents;
        final SegmentLink<AE>[] links;

        PathLink(
                final String entryPath,
                final Type entryType,
                final CommonEntry template,
                final boolean createParents)
        throws ArchiveFileSystemException {
            if (isRoot(entryPath))
                throw new ArchiveFileSystemException(entryPath,
                        "cannot replace virtual root directory entry");
            if (!isValidPath(entryPath))
                throw new ArchiveFileSystemException(entryPath,
                        "is not a valid path name");
            if (entryType != FILE && entryType != DIRECTORY)
                throw new ArchiveFileSystemException(entryPath,
                        "only FILE and DIRECTORY entries are currently supported");
            this.createParents = createParents;
            try {
                links = newSegmentLinks(entryPath, entryType, template, 1);
            } catch (CharConversionException ex) {
                throw new ArchiveFileSystemException(entryPath, ex);
            }
        }

        private SegmentLink<AE>[] newSegmentLinks(
                final String entryPath,
                final Type entryType,
                final CommonEntry template,
                final int level)
        throws ArchiveFileSystemException, CharConversionException {
            final String split[] = splitter.split(entryPath);
            final String parentPath = split[0]; // could equal ROOT
            final String baseName = split[1];
            final SegmentLink<AE>[] elements;

            // Lookup parent target, creating it where necessary and allowed.
            final BaseEntry<AE> parentEntry = master.get(parentPath);
            final BaseEntry<AE> newEntry;
            if (parentEntry != null) {
                if (parentEntry.getType() != DIRECTORY)
                    throw new ArchiveFileSystemException(entryPath,
                            "parent entry must be a directory");
                final BaseEntry<AE> oldEntry = master.get(entryPath);
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
                elements = new SegmentLink[level + 1];
                elements[0] = new SegmentLink<AE>(parentPath, parentEntry, null);
                newEntry = newEntry(entryPath, entryType, template);
                elements[1] = new SegmentLink<AE>(entryPath, newEntry, baseName);
            } else if (createParents) {
                elements = newSegmentLinks(
                        parentPath, DIRECTORY, null, level + 1);
                newEntry = newEntry(entryPath, entryType, template);
                elements[elements.length - level]
                        = new SegmentLink<AE>(entryPath, newEntry, baseName);
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
                final String path = link.path;
                final BaseEntry<AE> entry = link.entry;
                final String base = link.base;
                assert parent.getType() == DIRECTORY;
                master.put(path, entry);
                if (parent.add(base) && parent.getTime() != UNKNOWN) // never touch ghosts!
                    parent.getTarget().setTime(time);
                parent = entry;
            }
            final AE entry = getTarget();
            if (entry.getTime() == UNKNOWN)
                entry.setTime(time);
        }

        @Override
        public AE getTarget() {
            return links[links.length - 1].getTarget();
        }
    } // class PathLink

    /**
     * A data class which represents a path name segment for use by
     * {@link PathLink}.
     */
    private static final class SegmentLink<AE extends ArchiveEntry>
    implements IOReference<AE> {
        final String path;
        final BaseEntry<AE> entry;
        final String base;

        /**
         * Constructs a new {@code SegmentLink}.
         *
         * @param path The non-{@code null} normalized path name of the file
         *        system entry.
         * @param entry The non-{@code null} file system entry for the path
         *        name.
         * @param base The nullable base (segment) name of the path name.
         */
        SegmentLink(
                final String path,
                final BaseEntry<AE> entry,
                final String base) {
            assert path != null;
            assert entry != null;
            this.path = path;
            this.entry = entry;
            this.base = base; // may be null!
        }

        @Override
        public AE getTarget() {
            return entry.getTarget();
        }
    } // class SegmentLink

    @Override
    public void unlink(final String path) throws ArchiveFileSystemException {
        assert isRoot(path) || path.charAt(0) != SEPARATOR_CHAR;

        if (isRoot(path))
            throw new ArchiveFileSystemException(path,
                    "virtual root directory cannot get unlinked");
        final BaseEntry<AE> entry = master.remove(path);
        if (entry == null)
            throw new ArchiveFileSystemException(path,
                    "entry does not exist");
        assert entry != root;
        if (entry.getType() == DIRECTORY && entry.list().size() > 0) {
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
        final boolean ok = parent.remove(splitter.getBaseName());
        assert ok : "The parent directory of \"" + path
                    + "\" does not contain this entry - archive file system is corrupted!";
        touch();
        if (parent.getTime() != UNKNOWN) // never touch ghosts!
            parent.getTarget().setTime(System.currentTimeMillis());
    }

    private Type getType(final String path) {
        final BaseEntry<AE> entry = master.get(path);
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
    public boolean setLastModified(final String path, final long time)
    throws ArchiveFileSystemException {
        if (time < 0)
            throw new IllegalArgumentException(path +
                    " (negative entry modification time)");
        final BaseEntry<AE> entry = master.get(path);
        if (entry == null)
            return false;
        // Order is important here!
        touch();
        entry.getTarget().setTime(time);
        return true;
    }

    @Override
    public Set<String> list(final String path) {
        final BaseEntry<AE> entry = master.get(path);
        return entry == null ? null : entry.list();
    }
}
