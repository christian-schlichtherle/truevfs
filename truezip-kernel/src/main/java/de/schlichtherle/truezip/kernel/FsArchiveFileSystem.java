/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.FsCovariantEntry;
import de.truezip.kernel.cio.Entry;
import static de.truezip.kernel.cio.Entry.Access.WRITE;
import static de.truezip.kernel.cio.Entry.Type.DIRECTORY;
import static de.truezip.kernel.cio.Entry.Type.FILE;
import static de.truezip.kernel.cio.Entry.*;
import de.truezip.kernel.cio.Container;
import de.truezip.kernel.FsArchiveDriver;
import de.truezip.kernel.FsArchiveEntry;
import de.truezip.kernel.FsFileSystemException;
import de.truezip.kernel.addr.FsEntryName;
import static de.truezip.kernel.addr.FsEntryName.*;
import de.truezip.kernel.option.AccessOption;
import static de.truezip.kernel.option.AccessOption.CREATE_PARENTS;
import static de.truezip.kernel.option.AccessOption.EXCLUSIVE;
import de.truezip.kernel.option.AccessOptions;
import de.truezip.kernel.io.Paths.Normalizer;
import static de.truezip.kernel.io.Paths.cutTrailingSeparators;
import static de.truezip.kernel.io.Paths.isRoot;
import de.truezip.kernel.util.BitField;
import de.truezip.kernel.util.Link;
import static de.truezip.kernel.util.Maps.OVERHEAD_SIZE;
import static de.truezip.kernel.util.Maps.initialCapacity;
import java.io.CharConversionException;
import java.io.IOException;
import java.util.*;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A read/write virtual file system for archive entries.
 * Have a look at the online <a href="http://truezip.java.net/faq.html">FAQ</a>
 * to get the concept of how this works.
 * 
 * @param  <E> the type of the archive entries.
 * @see    <a href="http://truezip.java.net/faq.html">Frequently Asked Questions</a>
 * @author Christian Schlichtherle
 */
@NotThreadSafe
class FsArchiveFileSystem<E extends FsArchiveEntry>
implements Iterable<FsCovariantEntry<E>> {

    private static final String ROOT_PATH = ROOT.getPath();

    private final Splitter splitter = new Splitter();
    private final FsArchiveDriver<E> factory;
    private final EntryTable<E> master;

    /** Whether or not this file system has been modified (touched). */
    private boolean touched;

    private @CheckForNull FsArchiveFileSystemTouchListener<? super E>
            touchListener;

    /**
     * Returns a new empty archive file system and ensures its integrity.
     * Only the root directory is created with its last modification time set
     * to the system's current time.
     * The file system is modifiable and marked as touched!
     *
     * @param  <E> The type of the archive entries.
     * @param  driver the archive driver to use.
     * @return A new archive file system.
     * @throws NullPointerException If {@code factory} is {@code null}.
     */
    static <E extends FsArchiveEntry> FsArchiveFileSystem<E>
    newEmptyFileSystem(FsArchiveDriver<E> driver) {
        return new FsArchiveFileSystem<E>(driver);
    }

    private FsArchiveFileSystem(final FsArchiveDriver<E> driver) {
        this.factory = driver;
        final E root = newEntry(ROOT_PATH, DIRECTORY, AccessOptions.NONE, null);
        final long time = System.currentTimeMillis();
        for (final Access access : ALL_ACCESS_SET)
            root.setTime(access, time);
        final EntryTable<E> master = new EntryTable<E>(
                initialCapacity(OVERHEAD_SIZE));
        master.add(ROOT_PATH, root);
        this.master = master;
        this.touched = true;
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
     * @param  driver the archive driver to use.
     * @param  archive The archive entry container to read the entries for
     *         the population of the archive file system.
     * @param  rootTemplate The nullable template to use for the root entry of
     *         the returned archive file system.
     * @param  readOnly If and only if {@code true}, any subsequent
     *         modifying operation on the file system will result in a
     *         {@link FsReadOnlyFileSystemException}.
     * @return A new archive file system.
     * @throws NullPointerException If {@code factory} or {@code archive} are
     *         {@code null}.
     */
    static <E extends FsArchiveEntry> FsArchiveFileSystem<E>
    newPopulatedFileSystem( FsArchiveDriver<E> driver,
                            @WillNotClose Container<E> archive,
                            @CheckForNull Entry rootTemplate,
                            boolean readOnly) {
        return readOnly
            ? new FsReadOnlyArchiveFileSystem<E>(archive, driver, rootTemplate)
            : new FsArchiveFileSystem<E>(driver, archive, rootTemplate);
    }

    FsArchiveFileSystem(final FsArchiveDriver<E> driver,
                        final @WillNotClose Container<E> archive,
                        final @CheckForNull Entry rootTemplate) {
        this.factory = driver;
        // Allocate some extra capacity to create missing parent directories.
        final EntryTable<E> master = new EntryTable<E>(
                initialCapacity(archive.size() + OVERHEAD_SIZE));
        // Load entries from input archive.
        final List<String> paths = new ArrayList<String>(archive.size());
        final Normalizer normalizer = new Normalizer(SEPARATOR_CHAR);
        for (final E entry : archive) {
            final String path = cutTrailingSeparators(
                normalizer.normalize(
                    // Fix illegal Windoze file name separators.
                    entry.getName().replace('\\', SEPARATOR_CHAR)),
                SEPARATOR_CHAR);
            master.add(path, entry);
            if (!path.startsWith(SEPARATOR)
                    && !(".." + SEPARATOR).startsWith(path.substring(0, Math.min(3, path.length()))))
                paths.add(path);
        }
        // Setup root file system entry, potentially replacing its previous
        // mapping from the input archive.
        master.add(ROOT_PATH, newEntry(
                ROOT_PATH, DIRECTORY, AccessOptions.NONE, rootTemplate));
        this.master = master;
        // Now perform a file system check to create missing parent directories
        // and populate directories with their members - this must be done
        // separately!
        for (final String path : paths)
            fix(path);
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
    private void fix(final String name) {
        // When recursing into this method, it may be called with the root
        // directory as its parameter, so we may NOT skip the following test.
        if (isRoot(name))
            return; // never fix root or empty or absolute pathnames

        splitter.split(name);
        final String parentPath = splitter.getParentPath();
        final String memberName = splitter.getMemberName();
        FsCovariantEntry<E> parent = master.get(parentPath);
        if (null == parent || !parent.isType(DIRECTORY))
            parent = master.add(parentPath, newEntry(
                    parentPath, DIRECTORY, AccessOptions.NONE, null));
        parent.add(memberName);
        fix(parentPath);
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
     * Marks this (virtual) archive file system as touched and notifies the
     * listener if and only if the touch status is changing.
     *
     * @throws FsReadOnlyArchiveFileSystemException If this (virtual) archive
     *         file system is read only.
     * @throws FsFileSystemException If the listener vetoed the beforeTouch
     *         operation for any reason.
     */
    private void touch() throws IOException {
        if (touched)
            return;
        // Order is important here because of veto exceptions!
        final FsArchiveFileSystemEvent<E>
                e = new FsArchiveFileSystemEvent<E>(this);
        final FsArchiveFileSystemTouchListener<? super E> tl = touchListener;
        if (null != tl)
            tl.beforeTouch(e);
        touched = true;
        if (null != tl)
            tl.afterTouch(e);
    }

    /**
     * Returns a protective copy of the set of archive file system listeners.
     *
     * @return A clone of the set of archive file system listeners.
     */
    @SuppressWarnings("unchecked")
    final FsArchiveFileSystemTouchListener<? super E>[]
    getFsArchiveFileSystemTouchListeners() {
        return null == touchListener
                ? new FsArchiveFileSystemTouchListener[0]
                : new FsArchiveFileSystemTouchListener[] { touchListener };
    }

    /**
     * Adds the given listener to the set of archive file system listeners.
     *
     * @param  listener the listener for archive file system events.
     */
    final void addFsArchiveFileSystemTouchListener(
            final FsArchiveFileSystemTouchListener<? super E> listener)
    throws TooManyListenersException {
        if (null == listener)
            throw new NullPointerException();
        if (null != touchListener)
            throw new TooManyListenersException();
        touchListener = listener;
    }

    /**
     * Removes the given listener from the set of archive file system listeners.
     *
     * @param  listener the listener for archive file system events.
     */
    final void removeFsArchiveFileSystemTouchListener(
            final FsArchiveFileSystemTouchListener<? super E> listener) {
        if (null == listener)
            throw new NullPointerException();
        if (touchListener == listener)
            touchListener = null;
    }

    // TODO: Consider renaming to size().
    int getSize() {
        return master.getSize();
    }

    @Override
    public Iterator<FsCovariantEntry<E>> iterator() {
        return master.iterator();
    }

    /**
     * Returns a covariant file system entry or {@code null} if no file system
     * entry exists for the given name.
     * Modifying the returned object graph is either not supported (i.e. throws
     * an {@link UnsupportedOperationException}) or does not show any effect on
     * this file system.
     * 
     * @param  name the name of the file system entry to look up.
     * @return A covariant file system entry or {@code null} if no file system
     *         entry exists for the given name.
     */
    @Nullable
    final FsCovariantEntry<E> getEntry(final FsEntryName name) {
        final FsCovariantEntry<E> entry = master.get(name.getPath());
        return null == entry ? null : entry.clone(factory);
    }

    /**
     * Like {@link #newCheckedEntry newEntryChecked(path, type, null)},
     * but wraps any {@link CharConversionException} in an
     * {@link AssertionError}.
     *
     * @param  name the archive entry name.
     * @param  type the type of the archive entry to create.
     * @param  template the nullable template for the archive entry to create.
     * @return A new archive entry.
     */
    private E newEntry(
            final String name,
            final Type type,
            final BitField<AccessOption> mknod,
            final @CheckForNull Entry template) {
        assert null != type;
        assert !isRoot(name) || DIRECTORY == type;

        try {
            return factory.newEntry(name, type, template, mknod);
        } catch (CharConversionException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * Returns a new archive entry.
     * This is just a factory method and the returned file system entry is not
     * (yet) linked into this (virtual) archive file system.
     *
     * @see    #mknod
     * @param  name the archive entry name.
     * @param  type the type of the archive entry to create.
     * @param  template the nullable template for the archive entry to create.
     * @return A new archive entry.
     * @throws FsFileSystemException if a {@link CharConversionException}
     *         occurs as its cause.
     */
    private E newCheckedEntry(
            final String name,
            final Type type,
            final BitField<AccessOption> mknod,
            final @CheckForNull Entry template)
    throws FsFileSystemException {
        assert null != type;
        assert !isRoot(name) || DIRECTORY == type;

        try {
            return factory.newEntry(name, type, template, mknod);
        } catch (CharConversionException ex) {
            throw new FsFileSystemException(name, ex);
        }
    }

    /**
     * Begins a <i>transaction</i> to create or replace and finally link a
     * chain of one or more archive entries for the given {@code path} into
     * this archive file system.
     * <p>
     * To commit the transaction, you need to call
     * {@link FsArchiveFileSystemOperation#commit} on the returned object, which
     * will mark this archive file system as touched and set the last
     * modification time of the created and linked archive file system entries
     * to the system's current time at the moment of the call to this method.
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
     * @throws FsFileSystemException If one of the following is true:
     *         <ul>
     *         <li>The file system is read only.
     *         <li>{@code name} contains characters which are not
     *             supported by the file system.
     *         <li>TODO: type is not {@code FILE} or {@code DIRECTORY}.
     *         <li>The entry already exists and either the option
     *             {@link AccessOption#EXCLUSIVE} is set or the entry is a
     *             directory.
     *         <li>The entry exists as a different type.
     *         <li>A parent entry exists but is not a directory.
     *         <li>A parent entry is missing and {@code createParents} is
     *             {@code false}.
     *         </ul>
     * @return A new archive file system operation on a chain of one or more
     *         archive file system entries for the given path name which will
     *         be linked into this archive file system upon a call to its
     *         {@link FsArchiveFileSystemOperation#commit} method.
     */
    FsArchiveFileSystemOperation<E> mknod(
            final FsEntryName name,
            final Entry.Type type,
            final BitField<AccessOption> options,
            @CheckForNull Entry template)
    throws FsFileSystemException {
        if (null == type)
            throw new NullPointerException();
        if (FILE != type && DIRECTORY != type) // TODO: Add support for other types.
            throw new FsFileSystemException(name,
                    "only FILE and DIRECTORY entries are supported");
        final String path = name.getPath();
        final FsCovariantEntry<E> oldEntry = master.get(path);
        if (null != oldEntry) {
            if (!oldEntry.isType(FILE))
                throw new FsFileSystemException(name,
                        "only files can get replaced");
            if (FILE != type)
                throw new FsFileSystemException(name,
                        "entry exists as a different type");
            if (options.get(EXCLUSIVE))
                throw new FsFileSystemException(name,
                        "entry exists already");
        }
        while (template instanceof FsCovariantEntry<?>)
            template = ((FsCovariantEntry<?>) template).getEntry(type);
        return new PathLink(path, type, options, template);
    }

    /**
     * TODO: This implementation yields a potential issue: The state of the
     * file system may be altered between the construction of an instance and
     * the call to the {@link #commit} method, which may render the operation
     * illegal and corrupt the file system.
     * As long as only the ArchiveControllers in this package are used, this
     * should not happen, however.
     */
    private final class PathLink implements FsArchiveFileSystemOperation<E> {
        final boolean createParents;
        final BitField<AccessOption> options;
        final SegmentLink<E>[] links;
        long time = UNKNOWN;

        PathLink(   final String path,
                    final Entry.Type type,
                    final BitField<AccessOption> options,
                    @CheckForNull final Entry template)
        throws FsFileSystemException {
            // Consume AccessOption.CREATE_PARENTS.
            this.createParents = options.get(CREATE_PARENTS);
            this.options = options.clear(CREATE_PARENTS);
            links = newSegmentLinks(1, path, type, template);
        }

        @SuppressWarnings("unchecked")
        private SegmentLink<E>[] newSegmentLinks(
                final int level,
                final String entryName,
                final Entry.Type entryType,
                @CheckForNull final Entry template)
        throws FsFileSystemException {
            splitter.split(entryName);
            final String parentPath = splitter.getParentPath(); // could equal ROOT_PATH
            final String memberName = splitter.getMemberName();
            final SegmentLink<E>[] elements;

            // Lookup parent entry, creating it where necessary and allowed.
            final FsCovariantEntry<E> parentEntry = master.get(parentPath);
            final FsCovariantEntry<E> newEntry;
            if (null != parentEntry) {
                if (!parentEntry.isType(DIRECTORY))
                    throw new FsFileSystemException(entryName,
                            "parent entry must be a directory");
                elements = new SegmentLink[level + 1];
                elements[0] = new SegmentLink<E>(null, parentEntry);
                newEntry = new FsCovariantEntry<E>(entryName);
                newEntry.putEntry(entryType,
                        newCheckedEntry(entryName, entryType, options, template));
                elements[1] = new SegmentLink<E>(memberName, newEntry);
            } else if (createParents) {
                elements = newSegmentLinks(
                        level + 1, parentPath, DIRECTORY, null);
                newEntry = new FsCovariantEntry<E>(entryName);
                newEntry.putEntry(entryType,
                        newCheckedEntry(entryName, entryType, options, template));
                elements[elements.length - level]
                        = new SegmentLink<E>(memberName, newEntry);
            } else {
                throw new FsFileSystemException(entryName,
                        "missing parent directory entry");
            }
            return elements;
        }

        @Override
        public void commit() throws IOException {
            assert 2 <= links.length;

            touch();
            final int l = links.length;
            FsCovariantEntry<E> parentCE = links[0].entry;
            E parentAE = parentCE.getEntry(DIRECTORY);
            for (int i = 1; i < l ; i++) {
                final SegmentLink<E> link = links[i];
                final FsCovariantEntry<E> entryCE = link.entry;
                final E entryAE = entryCE.getEntry();
                final String member = link.base;
                master.add(entryCE.getName(), entryAE);
                if (master.get(parentCE.getName()).add(member)
                        && UNKNOWN != parentAE.getTime(WRITE)) // never touch ghosts!
                    parentAE.setTime(WRITE, getCurrentTimeMillis());
                parentCE = entryCE;
                parentAE = entryAE;
            }
            if (UNKNOWN == parentAE.getTime(WRITE))
                parentAE.setTime(WRITE, getCurrentTimeMillis());
        }

        private long getCurrentTimeMillis() {
            return UNKNOWN != time ? time : (time = System.currentTimeMillis());
        }

        @Override
        public FsCovariantEntry<E> getTarget() {
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
    implements Link<FsCovariantEntry<E>> {
        final @Nullable String base;
        final FsCovariantEntry<E> entry;

        /**
         * Constructs a new {@code SegmentLink}.
         *
         * @param base the nullable base name of the entry name.
         * @param entry the non-{@code null} file system entry for the entry
         *        name.
         */
        SegmentLink(final @CheckForNull String base,
                    final FsCovariantEntry<E> entry) {
            this.entry = entry;
            this.base = base;
        }

        @Override
        public FsCovariantEntry<E> getTarget() {
            return entry;
        }
    } // class SegmentLink

    /**
     * Tests the named file system entry and then - unless its the file system
     * root - notifies the listener and deletes the entry.
     * For the file system root, only the tests are performed but the listener
     * does not get notified and the entry does not get deleted.
     * For the tests to succeed, the named file system entry must exist and
     * directory entries (including the file system root) must be empty.
     *
     * @param  name the archive file system entry name.
     * @throws FsReadOnlyArchiveFileSystemException If this (virtual) archive
     *         file system is read-only.
     * @throws FsFileSystemException If the operation fails for some other
     *         reason.
     */
    void unlink(final FsEntryName name)
    throws IOException {
        // Test.
        final String path = name.getPath();
        final FsCovariantEntry<E> ce = master.get(path);
        if (null == ce)
            throw new FsFileSystemException(name,
                    "archive entry does not exist");
        if (ce.isType(DIRECTORY)) {
            final int size = ce.getMembers().size();
            if (0 != size)
                throw new FsFileSystemException(name, String.format(
                        "directory not empty - contains %d member(s)",
                        size));
        }
        if (name.isRoot())
            return;

        // Notify listener and modify.
        touch();
        master.remove(path);
        {
            // See http://java.net/jira/browse/TRUEZIP-144 :
            // This is used to signal to the driver that the entry should not
            // be included in the central directory even if the entry is
            // already physically present in the archive file (ZIP).
            // This signal will be ignored by drivers which do no support a
            // central directory (TAR).
            final E ae = ce.getEntry();
            for (final Size type : ALL_SIZE_SET)
                ae.setSize(type, UNKNOWN);
            for (final Access type : ALL_ACCESS_SET)
                ae.setTime(type, UNKNOWN);
        }
        splitter.split(path);
        final String parentPath = splitter.getParentPath();
        final FsCovariantEntry<E> pce = master.get(parentPath);
        assert null != pce : "The parent directory of \"" + name.toString()
                    + "\" is missing - archive file system is corrupted!";
        final boolean ok = pce.remove(splitter.getMemberName());
        assert ok : "The parent directory of \"" + name.toString()
                    + "\" does not contain this entry - archive file system is corrupted!";
        final E pae = pce.getEntry(DIRECTORY);
        if (UNKNOWN != pae.getTime(WRITE)) // never touch ghosts!
            pae.setTime(WRITE, System.currentTimeMillis());
    }

    boolean setTime(
            final FsEntryName name,
            final BitField<Access> types,
            final long value)
    throws IOException {
        if (0 > value)
            throw new IllegalArgumentException(name.toString()
                    + " (negative access time)");
        final FsCovariantEntry<E> ce = master.get(name.getPath());
        if (null == ce)
            throw new FsFileSystemException(name,
                    "archive entry not found");
        // Order is important here!
        touch();
        final E ae = ce.getEntry();
        boolean ok = true;
        for (final Access type : types)
            ok &= ae.setTime(type, value);
        return ok;
    }

    boolean setTime(
            final FsEntryName name,
            final Map<Access, Long> times)
    throws IOException {
        final FsCovariantEntry<E> ce = master.get(name.getPath());
        if (null == ce)
            throw new FsFileSystemException(name,
                    "archive entry not found");
        // Order is important here!
        touch();
        final E ae = ce.getEntry();
        boolean ok = true;
        for (final Map.Entry<Access, Long> time : times.entrySet()) {
            final long value = time.getValue();
            ok &= 0 <= value && ae.setTime(time.getKey(), value);
        }
        return ok;
    }

    boolean isWritable(FsEntryName name) {
        return !isReadOnly();
    }

    void setReadOnly(FsEntryName name)
    throws FsFileSystemException {
        if (!isReadOnly())
            throw new FsFileSystemException(name,
                "cannot set read-only state");
    }

    /**
     * @param <E> The type of the archive entries.
     */
    private static final class EntryTable<E extends FsArchiveEntry> {

        /**
         * The map of covariant file system entries.
         * <p>
         * Note that the archive entries in the covariant file system entries
         * in this map are shared with the {@link Container} object
         * provided to the constructor of this class.
         */
        final Map<String, FsCovariantEntry<E>> map;

        EntryTable(int initialCapacity) {
            this.map = new LinkedHashMap<String, FsCovariantEntry<E>>(
                    initialCapacity);
        }

        int getSize() {
            return map.size();
        }

        Iterator<FsCovariantEntry<E>> iterator() {
            return map.values().iterator();
        }

        FsCovariantEntry<E> add(final String path, final E ae) {
            FsCovariantEntry<E> ce = map.get(path);
            if (null == ce)
                map.put(path, ce = new FsCovariantEntry<E>(path));
            ce.putEntry(ae.getType(), ae);
            return ce;
        }

        @Nullable FsCovariantEntry<E> get(String path) {
            return map.get(path);
        }

        @Nullable FsCovariantEntry<E> remove(String path) {
            return map.remove(path);
        }
    } // class EntryTable

    /** Splits a given path name into its parent path name and base name. */
    private static final class Splitter
    extends de.truezip.kernel.io.Paths.Splitter {
        Splitter() {
            super(SEPARATOR_CHAR, false);
        }

        @Override
        public String getParentPath() {
            final String path = super.getParentPath();
            return null != path ? path : ROOT_PATH;
        }
    } // class Splitter
}