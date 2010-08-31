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

package de.schlichtherle.truezip.io.archive.controller;

import de.schlichtherle.truezip.io.util.Paths.Normalizer;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.Type;
import de.schlichtherle.truezip.io.FileFactory;
import de.schlichtherle.truezip.io.File;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.driver.InputArchive;
import de.schlichtherle.truezip.io.archive.driver.OutputArchive;
import de.schlichtherle.truezip.io.util.InputException;
import de.schlichtherle.truezip.io.util.Paths;
import de.schlichtherle.truezip.io.util.Streams;
import de.schlichtherle.truezip.util.ExceptionHandler;
import java.io.CharConversionException;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.Icon;

import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.ROOT;
import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.SEPARATOR;
import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.SEPARATOR_CHAR;
import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.UNKNOWN;
import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.Type.DIRECTORY;
import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.Type.FILE;
import static de.schlichtherle.truezip.io.util.Paths.normalize;

/**
 * This class implements a virtual file system of archive entries for use
 * by the archive controller provided to the constructor.
 * <p>
 * <b>WARNING:</b>This class is <em>not</em> thread safe!
 * All calls to non-static methods <em>must</em> be synchronized on the
 * respective {@code ArchiveController} object!
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class ArchiveFileSystem {

    /** The controller that this filesystem belongs to. */
    private final UpdatingArchiveController controller;

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
    private Map<String, ArchiveEntry> master;

    /** The archive entry for the virtual root of this file system. */
    private final ArchiveEntry root;

    /** The number of times this file system has been modified (touched). */
    private long touched;

    /**
     * Creates a new archive file system and ensures its integrity.
     * The root directory is created with its last modification time set to
     * the system's current time.
     * The file system is modifiable and marked as touched!
     * 
     * @param controller The controller which will use this file system.
     *        This implementation will solely use the controller as a factory
     *        to create missing archive entries using
     *        {@link FileSystemArchiveController#newArchiveEntry}.
     * @throws NullPointerException If {@code controller} or {@code archive}
     *         is {@code null}.
     */
    // TODO: Replace controller with factory!
    ArchiveFileSystem(final UpdatingArchiveController controller)
    throws IOException {
        assert controller != null;

        this.controller = controller;
        touched = 1;
        master = new LinkedHashMap<String, ArchiveEntry>(64);

        // Setup root.
        root = newArchiveEntry(ROOT, DIRECTORY);
        root.setTime(System.currentTimeMillis());
        master.put(ROOT, root);

        readOnly = false;
    }

    /**
     * Populates this file system from <code>archive</code> and ensures its
     * integrity.
     * First, a root directory with the given last modification time is
     * created - it's never loaded from the archive!
     * Then the entries from the archive are loaded into the file system and
     * its integrity is checked:
     * Any missing parent directories are created using the system's current
     * time as their last modification time - existing directories will never
     * be replaced.
     * <p>
     * Note that the entries in this file system are shared with the given
     * {@code archive}.
     * 
     * @param controller The controller which will use this file system.
     *        This implementation will solely use the controller as a factory
     *        to create missing archive entries using
     *        {@link FileSystemArchiveController#newArchiveEntry}.
     * @param archive The input archive to read the entries for the population
     *        of this file system.
     * @param rootTime The last modification time of the root of the populated
     *        file system in milliseconds since the epoch.
     * @param readOnly If and only if {@code true}, any subsequent
     *        modifying operation on this file system will result in a
     *        {@link ReadOnlyArchiveFileSystemException}.
     * @throws NullPointerException If {@code controller} or {@code archive}
     *         is {@code null}.
     */
    // TODO: Replace controller with factory!
    ArchiveFileSystem(
            final UpdatingArchiveController controller,
            final InputArchive archive,
            final long rootTime,
            final boolean readOnly) {
        this.controller = controller;
        master = new LinkedHashMap<String, ArchiveEntry>(
                (int) (archive.getNumArchiveEntries() / 0.75f) + 1);

        final Normalizer normalizer = new Normalizer(SEPARATOR_CHAR);
        // Load entries from input archive.
        Enumeration<? extends ArchiveEntry> entries
                = archive.getArchiveEntries();
        while (entries.hasMoreElements()) {
            final ArchiveEntry entry = entries.nextElement();
            final String path = normalizer.normalize(entry.getName());
            master.put(path, entry);
            entry.setMetaData(new ArchiveEntryMetaData(entry));
        }

        // Setup root entry, potentially replacing its definition from the
        // input archive.
        root = newArchiveEntry(ROOT, DIRECTORY);
        root.setTime(rootTime); // do NOT yet touch the file system!
        master.put(ROOT, root);

        // Now perform a file system check to create missing parent directories
        // and populate directories with their children - this needs to be done
        // separately!
        // entries = Collections.enumeration(master.values()); // concurrent modification!
        final PopulatePostfix fsck = new PopulatePostfix();
        entries = archive.getArchiveEntries();
        while (entries.hasMoreElements()) {
            final ArchiveEntry entry = entries.nextElement();
            final String path = normalizer.normalize(entry.getName());
            if (isLegalPath(path))
                fsck.fix(path);
        }

        // Make master map unmodifiable if this is a readonly file system
        this.readOnly = readOnly;
        if (readOnly)
            master = Collections.unmodifiableMap(master);

        assert !isTouched();
    }

    /**
     * Like {@link #newArchiveEntry(String, ArchiveEntry.Type, ArchiveEntry)
     * newArchiveEntry(path, type, null)}, but throws an
     * {@link AssertionError} instead of a {@link CharConversionException}.
     *
     * @throws AssertionError If a {@link CharConversionException} occurs.
     *         The original exception is wrapped as its cause.
     */
    private ArchiveEntry newArchiveEntry(
            final String path,
            final Type type) {
        try {
            return newArchiveEntry(path, type, null);
        } catch (CharConversionException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * Constructs a new archive file system entry for this virtual archive
     * file system.
     * The returned entry still needs to be {@link #link}ed into this virtual
     * archive file system.
     * The returned entry has properly initialized meta data, but is
     * otherwise left as created by the archive driver.
     *
     * @param  path The path name of the archive entry to create.
     *         This is always a {@link #isLegalPath(String) legal path}.
     * @param  blueprint If not {@code null}, then the newly created archive
     *         entry shall inherit as much properties from this archive entry
     *         as possible (with the exception of its entry name).
     *         This is typically used for copy operations.
     * @return An {@link ArchiveEntry} created by the archive driver.
     * @throws CharConversionException If {@code path} contains characters
     *         which are not supported by the archive file.
     */
    private ArchiveEntry newArchiveEntry(
            String path,
            final Type type,
            final ArchiveEntry blueprint)
    throws CharConversionException {
        assert !isRoot(path) || type == DIRECTORY;
        assert isLegalPath(path);
        assert type == FILE || type == DIRECTORY : "Only FILE and DIRECTORY entries are currently supported!";

        if (type == DIRECTORY)
            path += SEPARATOR_CHAR;
        final ArchiveEntry entry = controller.newArchiveEntry(path, blueprint);
        entry.setMetaData(new ArchiveEntryMetaData(entry));
        return entry;
    }

    /**
     * Returns {@code true} iff the given entry name refers to the
     * virtual root directory of this file system.
     */
    static boolean isRoot(String path) {
        assert ROOT.isEmpty();
        return path.isEmpty();
    }

    /**
     * Checks whether the given entry entryName is a legal path name.
     * A legal path name is in {@link Paths#normalize(String, char) normal form},
     * is not absolute, does not identify the dot directory ({@code "."}) or
     * the dot-dot directory ({@code ".."}) or any of their descendants.
     */
    private static boolean isLegalPath(final String path) {
        if (isRoot(path))
            return true;

        if (path != normalize(path, SEPARATOR_CHAR)) // mind contract!
            return false;

        final int length = path.length();
        assert length > 0 || isRoot(path) : "Definition of ROOT changed!?";
        switch (path.charAt(0)) {
        case SEPARATOR_CHAR:
            return false; // never fix absolute path names

        case '.':
            if (length >= 2) {
                switch (path.charAt(1)) {
                case '.':
                    if (length >= 3) {
                        if (path.charAt(2) == SEPARATOR_CHAR) {
                            assert path.startsWith(".." + SEPARATOR);
                            return false;
                        }
                        // Fall through.
                    } else {
                        assert "..".equals(path);
                        return false;
                    }
                    break;

                case SEPARATOR_CHAR:
                    assert path.startsWith("." + SEPARATOR);
                    return false;

                default:
                    // Fall through.
                }
            } else {
                assert ".".equals(path);
                return false;
            }
            break;

        default:
            // Fall through.
        }

        return true;
    }

    private static class Splitter
    extends de.schlichtherle.truezip.io.util.Paths.Splitter {
        Splitter() {
            super(SEPARATOR_CHAR);
        }

        /**
         * Splits the given entry name in a parent entry name and a base name.
         * Iff the given entry name does not name a parent directory, then
         * {@link ArchiveEntry#ROOT} is set at index zero of the returned array.
         *
         * @param path The name of the entry which's parent entry name and
         *        base name are to be returned.
         * @throws NullPointerException If {@code entryName} is {@code null}.
         */
        @Override
        public String[] split(String path) {
            final String split[] = super.split(path);
            if (split[0] == null)
                split[0] = ROOT; // postfix
            return split;
        }
    }

    private class PopulatePostfix extends Splitter {
        /**
         * Called from a constructor to fix the parent directories of the entry
         * identified by {@code path}, ensuring that all parent directories of
         * the entry exist and that they contain the respective child.
         * If a parent directory does not exist, it is created using an
         * unkown time as the last modification time - this is defined to be a
         * <i>ghost directory<i>.
         * If a parent directory does exist, the respective child is added
         * (possibly yet again) and the process is continued.
         */
        void fix(final String path) {
            // When recursing into this method, it may be called with the root
            // directory as its parameter, so we may NOT skip the following test.
            if (isRoot(path))
                return; // never fix root or empty or absolute pathnames
            assert isLegalPath(path);

            split(path);
            final String parentPath = getParentPath();
            final String baseName = getBaseName();
            ArchiveEntry parent = master.get(parentPath);
            if (parent == null) {
                parent = newArchiveEntry(parentPath, DIRECTORY);
                master.put(parentPath, parent);
            }
            parent.getMetaData().children.add(baseName);
            fix(parentPath);
        }
    }

    /**
     * Indicates whether this file system is read only or not.
     * The default is {@code false}.
     */
    boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Indicates whether this file system has been modified since
     * its time of creation or the last call to {@code resetTouched()}.
     */
    boolean isTouched() {
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
        if (touched == 0)
            controller.touch();
        touched++;
    }

    /**
     * Looks up the specified entry in the file system and returns it or
     * {@code null} if not existent.
     */
    // TODO: Introduce sockets and make this private!
    public ArchiveEntry get(String path) {
        return master.get(path);
    }

    /**
     * Equivalent to {@link #link(String, ArchiveEntry.Type, boolean, ArchiveEntry)
     * link(path, type, createParents, null)}.
     */
    LinkTransaction link(
            final String path,
            final Type type,
            final boolean createParents)
    throws ArchiveFileSystemException {
        return new LinkTransaction(path, type, createParents, null);
    }

    /**
     * Begins a &quot;create and link entry&quot; transaction to ensure that
     * either a new entry for the given {@code path} will be created or an
     * existing entry is replaced within this archive file system.
     * <p>
     * This is the first step of a two-step process to create an archive entry
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
     * @param path The relative path name of the entry to create or replace.
     * @param createParents If {@code true}, any non-existing parent
     *        directory will be created in this file system with its last
     *        modification time set to the system's current time.
     * @param template If not {@code null}, then the newly created or
     *        replaced entry shall inherit as much properties from this
     *        instance as possible (with the exception of the name).
     *        This is typically used for archive copy operations and requires
     *        some support by the archive driver.
     * @return An I/O operation. You must call its {@link IOOperation#run}
     *         method in order to link the newly created entry into this
     *         archive file system.
     * @throws ArchiveReadOnlyExceptionn If this virtual archive file system
     *         is read only.
     * @throws ArchiveFileSystemException If one of the following is true:
     *         <ul>
     *         <li>{@code entryName} contains characters which are not
     *             supported by the archive file.
     *         <li>The entry name indicates a directory (trailing {@code /})
     *             and its entry does already exist within this file system.
     *         <li>The entry is a file or directory and does already exist as
     *             the respective other type within this file system.
     *         <li>The parent directory does not exist and
     *             {@code createParents} is {@code false}.
     *         <li>One of the entry's parents denotes a file.
     *         </ul>
     */
    public LinkTransaction link(
            final String path,
            final Type type,
            final boolean createParents,
            final ArchiveEntry template)
    throws ArchiveFileSystemException {
        return new LinkTransaction(path, type, createParents, template);
    }

    /**
     * A simple transaction for creating (and hence probably replacing) and
     * linking an entry in this archive file system.
     * 
     * @see #link
     */
    final class LinkTransaction implements IOOperation {
        final Splitter splitter = new Splitter();
        final PathNameElement[] elements;

        private LinkTransaction(
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
            if (!isLegalPath(entryPath))
                throw new ArchiveFileSystemException(entryPath,
                        "is not a legal path name");
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

            // Lookup parent entry, creating it where necessary and allowed.
            final ArchiveEntry parentEntry = master.get(parentPath);
            final ArchiveEntry newEntry;
            if (parentEntry != null) {
                if (parentEntry.getType() != DIRECTORY)
                    throw new ArchiveFileSystemException(entryPath,
                            "parent entry must be a directory");
                final ArchiveEntry oldEntry = master.get(entryPath);
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
                newEntry = newArchiveEntry(entryPath, entryType, template);
                elements[1] = new PathNameElement(entryPath, newEntry, baseName);
            } else if (createParents) {
                elements = newPathNameElements(
                        parentPath, DIRECTORY, createParents, null, level + 1);
                newEntry = newArchiveEntry(entryPath, entryType, template);
                elements[elements.length - level]
                        = new PathNameElement(entryPath, newEntry, baseName);
            } else {
                throw new ArchiveFileSystemException(entryPath,
                        "missing parent directory entry");
            }
            return elements;
        }

        /** Links the entries into this virtual archive file system. */
        public void run() throws IOException {
            assert elements.length >= 2;

            touch();

            final long time = System.currentTimeMillis();
            final int l = elements.length;

            ArchiveEntry parent = elements[0].entry;
            for (int i = 1; i < l ; i++) {
                final PathNameElement element = elements[i];
                final String path = element.path;
                final ArchiveEntry entry = element.entry;
                final String base = element.base;
                if (parent.getMetaData().children.add(base)
                        && parent.getTime() != UNKNOWN) // never touch ghosts!
                    parent.setTime(time);
                master.put(path, entry);
                parent = entry;
            }

            final ArchiveEntry entry = elements[l - 1].entry;
            if (entry.getTime() == UNKNOWN)
                entry.setTime(time);
        }

        public ArchiveEntry getEntry() {
            assert controller.getFileSystem() == ArchiveFileSystem.this;
            return elements[elements.length - 1].entry;
        }
    } // class LinkTransaction

    /**
     * A data class which represents a path name base for use by
     * {@link LinkTransaction}.
     */
    private static class PathNameElement {
        final String path;
        final ArchiveEntry entry;
        final String base;

        /**
         * Constructs a new {@code LinkStep}.
         *
         * @param path The normalized path name of the archive entry.
         *        - {@code null} is not permitted.
         * @param entry The archive entry for the path name
         *        - {@code null} is not permitted.
         * @param base The base name of the path name
         *        - may be {@code null}.
         */
        PathNameElement(
                final String path,
                final ArchiveEntry entry,
                final String base) {
            assert path != null;
            assert entry != null;
            this.path = path;
            this.entry = entry;
            this.base = base; // may be null!
        }
    }

    /**
     * If this method returns, the entry identified by the given
     * {@code entryName} has been successfully deleted from this archive file
     * system.
     * If the entry is a directory, it must be empty for successful deletion.
     * 
     * @throws ArchiveReadOnlyExceptionn If the virtual archive file system is
     *         read only.
     * @throws ArchiveFileSystemException If the operation fails for
     *         any other reason.
     */
    private void unlink(final String entryPath)
    throws IOException {
        if (isRoot(entryPath))
            throw new ArchiveFileSystemException(entryPath,
                    "virtual root directory cannot get unlinked");
        try {
            final ArchiveEntry entry = master.remove(entryPath);
            if (entry == null)
                throw new ArchiveFileSystemException(entryPath,
                        "entry does not exist");
            if (entry == root
                    || entry.getType() == DIRECTORY
                        && !entry.getMetaData().children.isEmpty()) {
                master.put(entryPath, entry); // Restore file system
                throw new ArchiveFileSystemException(entryPath,
                        "directory is not empty");
            }
            final Splitter splitter = new Splitter();
            splitter.split(entryPath);
            final String parentPath = splitter.getParentPath();
            final ArchiveEntry parent = master.get(parentPath);
            assert parent != null : "The parent directory of \"" + entryPath
                        + "\" is missing - archive file system is corrupted!";
            final boolean ok = parent.getMetaData().children.remove(splitter.getBaseName());
            assert ok : "The parent directory of \"" + entryPath
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
    
    boolean exists(final String path) {
        return get(path) != null;
    }

    boolean isFile(final String path) {
        final ArchiveEntry entry = get(path);
        return entry != null && entry.getType() == FILE;
    }
    
    boolean isDirectory(final String path) {
        final ArchiveEntry entry = get(path);
        return entry != null && entry.getType() == DIRECTORY;
    }
    
    boolean canWrite(final String path) {
        return !isReadOnly() && isFile(path);
    }

    boolean setReadOnly(final String path) {
        return isReadOnly() && isFile(path);
    }
    
    long length(final String path) {
        final ArchiveEntry entry = get(path);
        if (entry == null || entry.getType() == DIRECTORY)
            return 0;

        // TODO: Review: Can we avoid this special case?
        // It's probably ZipDriver specific!
        // This entry is a plain file in the file system.
        // If entry.getSize() returns UNKNOWN, the length is yet unknown.
        // This may happen if e.g. a ZIP entry has only been partially
        // written, i.e. not yet closed by another thread, or if this is a
        // ghost directory.
        // As this is not specified in the contract of the File class, return
        // 0 in this case instead.
        final long length = entry.getSize();
        return length != UNKNOWN ? length : 0;
    }

    long lastModified(final String path) {
        final ArchiveEntry entry = get(path);
        if (entry != null) {
            // Depending on the driver type, entry.getTime() could return
            // a negative value. E.g. this is the default value that the
            // ArchiveDriver uses for newly created entries in order to
            // indicate an unknown time.
            // As this is not specified in the contract of the File class,
            // 0 is returned in this case instead.
            final long time = entry.getTime();
            return time >= 0 ? time : 0;
        }
        // This entry does not exist.
        return 0;
    }

    boolean setLastModified(final String path, final long time)
    throws IOException {
        if (time < 0)
            throw new IllegalArgumentException(path +
                    " (negative entry modification time)");

        if (isReadOnly())
            return false;

        final ArchiveEntry entry = get(path);
        if (entry == null)
            return false;

        // Order is important here!
        touch();
        entry.setTime(time);

        return true;
    }
    
    String[] list(final String path) {
        // Lookup the entry as a directory.
        final ArchiveEntry entry = get(path);
        if (entry != null && entry.getType() == DIRECTORY)
            return entry.getMetaData().list();
        return null; // does not exist as a directory
    }
    
    String[] list(
            final String path,
            final FilenameFilter filenameFilter,
            final File dir) {
        // Lookup the entry as a directory.
        final ArchiveEntry entry = get(path);
        if (entry != null && entry.getType() == DIRECTORY)
            if (filenameFilter != null)
                return entry.getMetaData().list(filenameFilter, dir);
            else
                return entry.getMetaData().list(); // most efficient
        return null; // does not exist as directory
    }

    File[] listFiles(
            final String path,
            final FilenameFilter filenameFilter,
            final File dir,
            final FileFactory factory) {
        // Lookup the entry as a directory.
        final ArchiveEntry entry = get(path);
        if (entry != null && entry.getType() == DIRECTORY)
            return entry.getMetaData().listFiles(filenameFilter, dir, factory);
        return null; // does not exist as a directory
    }
    
    File[] listFiles(
            final String path,
            final FileFilter fileFilter,
            final File dir,
            final FileFactory factory) {
        // Lookup the entry as a directory.
        final ArchiveEntry entry = get(path);
        if (entry != null && entry.getType() == DIRECTORY)
            return entry.getMetaData().listFiles(fileFilter, dir, factory);
        return null; // does not exist as a directory
    }

    void mkdir(String path, boolean createParents)
    throws IOException {
        link(path, DIRECTORY, createParents).run();
    }

    void delete(final String path)
    throws IOException {
        assert isRoot(path) || path.charAt(0) != SEPARATOR_CHAR;

        if (get(path) != null) {
            unlink(path);
            return;
        }
        throw new ArchiveFileSystemException(path,
                "archive entry does not exist");
    }

    <T extends Throwable>
    void copy(
            final InputArchive ia,
            final OutputArchive oa,
            final ExceptionHandler<IOException, T> h)
    throws T {
        final Enumeration<ArchiveEntry> en
                = Collections.enumeration(master.values());
        while (en.hasMoreElements()) {
            final ArchiveEntry e = en.nextElement();
            final String n = e.getName();
            if (oa.getArchiveEntry(n) != null)
                continue; // we have already written this entry
            try {
                if (e.getType() == DIRECTORY) {
                    if (root == e)
                        continue; // never write the virtual root directory
                    if (e.getTime() < 0)
                        continue; // never write ghost directories
                    oa.newOutputStream(e, null).close();
                } else if (ia != null && ia.getArchiveEntry(n) != null) {
                    assert e == ia.getArchiveEntry(n);
                    InputStream in;
                    try {
                        in = ia.newInputStream(e, e);
                        assert in != null;
                    } catch (IOException ex) {
                        throw new InputException(ex);
                    }
                    // 'entry' will never be used again, so it is
                    // safe to hand over this entry from the
                    // InputArchive to the OutputArchive.
                    final OutputStream out = oa.newOutputStream(e, e);
                    Streams.cp(in, out);
                } else {
                    // The entry is an archive file which has been
                    // newly created and not yet been reassembled
                    // into this (potentially new) archive file.
                    // Write an empty entry now as a marker in order to
                    // recreate the entry when the file system gets
                    // remounted from the archive file.
                    oa.newOutputStream(e, null).close();
                }
            } catch (IOException ex) {
                h.warn(ex);
            }
        }
    }
}
