/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import static de.truezip.kernel.FsAccessOption.CREATE_PARENTS;
import static de.truezip.kernel.FsAccessOption.EXCLUSIVE;
import static de.truezip.kernel.FsEntryName.*;
import de.truezip.kernel.*;
import de.truezip.kernel.cio.Container;
import de.truezip.kernel.cio.Entry;
import static de.truezip.kernel.cio.Entry.Access.WRITE;
import static de.truezip.kernel.cio.Entry.Type.DIRECTORY;
import static de.truezip.kernel.cio.Entry.Type.FILE;
import static de.truezip.kernel.cio.Entry.*;
import de.truezip.kernel.io.PathNormalizer;
import static de.truezip.kernel.io.Paths.cutTrailingSeparators;
import static de.truezip.kernel.io.Paths.isRoot;
import de.truezip.kernel.util.BitField;
import de.truezip.kernel.util.Link;
import static de.truezip.kernel.util.Maps.OVERHEAD_SIZE;
import static de.truezip.kernel.util.Maps.initialCapacity;
import java.io.CharConversionException;
import java.io.IOException;
import java.nio.charset.CharsetEncoder;
import java.nio.file.*;
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
class ArchiveFileSystem<E extends FsArchiveEntry>
implements Iterable<FsCovariantEntry<E>> {

    private static final String ROOT_PATH = ROOT.getPath();

    private final PathSplitter splitter = new PathSplitter();
    private final FsArchiveDriver<E> driver;
    private final EntryTable<E> master;

    private final ThreadLocalCharsetEncoder
            encoder = new ThreadLocalCharsetEncoder();

    /** Whether or not this file system has been modified (touched). */
    private boolean touched;

    private @CheckForNull ArchiveFileSystemTouchListener<? super E>
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
    static <E extends FsArchiveEntry> ArchiveFileSystem<E>
    newEmptyFileSystem(FsArchiveDriver<E> driver) {
        return new ArchiveFileSystem<>(driver);
    }

    private ArchiveFileSystem(final FsArchiveDriver<E> driver) {
        this.driver = driver;
        final E root = newEntry(ROOT_PATH, DIRECTORY, FsAccessOptions.NONE, null);
        final long time = System.currentTimeMillis();
        for (final Access access : ALL_ACCESS_SET)
            root.setTime(access, time);
        final EntryTable<E> master = new EntryTable<>(
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
    static <E extends FsArchiveEntry> ArchiveFileSystem<E>
    newPopulatedFileSystem( FsArchiveDriver<E> driver,
                            @WillNotClose Container<E> archive,
                            @CheckForNull Entry rootTemplate,
                            boolean readOnly) {
        return readOnly
            ? new ReadOnlyArchiveFileSystem<>(archive, driver, rootTemplate)
            : new ArchiveFileSystem<>(driver, archive, rootTemplate);
    }

    ArchiveFileSystem(final FsArchiveDriver<E> driver,
                        final @WillNotClose Container<E> archive,
                        final @CheckForNull Entry rootTemplate) {
        this.driver = driver;
        // Allocate some extra capacity to create missing parent directories.
        final EntryTable<E> master = new EntryTable<>(
                initialCapacity(archive.size() + OVERHEAD_SIZE));
        // Load entries from input archive.
        final List<String> paths = new ArrayList<>(archive.size());
        final PathNormalizer normalizer = new PathNormalizer(SEPARATOR_CHAR);
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
                ROOT_PATH, DIRECTORY, FsAccessOptions.NONE, rootTemplate));
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
                    parentPath, DIRECTORY, FsAccessOptions.NONE, null));
        parent.add(memberName);
        fix(parentPath);
    }

    /**
     * Returns {@code true} if and only if this archive file system is
     * read-only.
     * <p>
     * The implementation in the class {@link ArchiveFileSystem} always
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
     * @throws IOException If the listener's beforeTouch implementation vetoed
     *         the operation for any reason.
     */
    private void touch(final BitField<FsAccessOption> options) throws IOException {
        if (touched)
            return;
        // Order is important here because of veto exceptions!
        final ArchiveFileSystemEvent<E>
                e = new ArchiveFileSystemEvent<>(this);
        final ArchiveFileSystemTouchListener<? super E> tl = touchListener;
        if (null != tl)
            tl.beforeTouch(e, options);
        touched = true;
        if (null != tl)
            tl.afterTouch(e, options);
    }

    /**
     * Returns a protective copy of the set of archive file system listeners.
     *
     * @return A clone of the set of archive file system listeners.
     */
    @SuppressWarnings("unchecked")
    final ArchiveFileSystemTouchListener<? super E>[]
    getFsArchiveFileSystemTouchListeners() {
        return null == touchListener
                ? new ArchiveFileSystemTouchListener[0]
                : new ArchiveFileSystemTouchListener[] { touchListener };
    }

    /**
     * Adds the given listener to the set of archive file system listeners.
     *
     * @param  listener the listener for archive file system events.
     */
    final void addFsArchiveFileSystemTouchListener(
            final ArchiveFileSystemTouchListener<? super E> listener)
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
            final ArchiveFileSystemTouchListener<? super E> listener) {
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
        return null == entry ? null : entry.clone(driver);
    }

    /**
     * Returns a new archive entry.
     * This is just a factory method and the returned file system entry is not
     * (yet) linked into this (virtual) archive file system.
     *
     * @param  name the archive entry name.
     * @param  type the type of the archive entry to create.
     * @param  template the nullable template for the archive entry to create.
     * @return A new archive entry.
     */
    private E newEntry(
            final String name,
            final Type type,
            final BitField<FsAccessOption> mknod,
            final @CheckForNull Entry template) {
        assert null != type;
        assert !isRoot(name) || DIRECTORY == type;
        return driver.newEntry(name, type, template, mknod);
    }

    /**
     * Like {@link #newEntry newEntry(name, type, mknod, template)},
     * but ensures that the given entry name can get encoded by the driver's
     * character set.
     *
     * @see    #mknod
     * @param  name the archive entry name.
     * @param  type the type of the archive entry to create.
     * @param  template the nullable template for the archive entry to create.
     * @return A new archive entry.
     * @throws CharConversionException If the entry name contains characters
     *         which cannot get encoded.
     */
    private E newCheckedEntry(
            final String name,
            final Type type,
            final BitField<FsAccessOption> mknod,
            final @CheckForNull Entry template)
    throws CharConversionException {
        assert null != type;
        assert !isRoot(name) || DIRECTORY == type;
        if (!encoder.canEncode(name))
            throw new CharConversionException(name +
                    " (not encodable with " + driver.getCharset() + ")");
        return driver.newEntry(name, type, template, mknod);
    }

    /**
     * Begins a <i>transaction</i> to create or replace and finally link a
     * chain of one or more archive entries for the given {@code path} into
     * this archive file system.
     * <p>
     * To commit the transaction, you need to call
     * {@link ArchiveFileSystemOperation#commit} on the returned object, which
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
     * @throws IOException on any I/O error.
     * @return A new archive file system operation on a chain of one or more
     *         archive file system entries for the given path name which will
     *         be linked into this archive file system upon a call to its
     *         {@link ArchiveFileSystemOperation#commit} method.
     */
    ArchiveFileSystemOperation<E> mknod(
            final FsEntryName name,
            final Entry.Type type,
            final BitField<FsAccessOption> options,
            @CheckForNull Entry template)
    throws IOException {
        if (null == type)
            throw new NullPointerException();
        if (FILE != type && DIRECTORY != type) // TODO: Add support for other types.
            throw new FileSystemException(name.toString(), null,
                    "Can only create file or directory entries, but not a " + typeName(type) + " entry!");
        final String path = name.getPath();
        final FsCovariantEntry<E> oldEntry = master.get(path);
        if (null != oldEntry) {
            if (!oldEntry.isType(FILE))
                throw new FileAlreadyExistsException(name.toString(), null,
                        "Cannot replace a " + typeName(oldEntry) + " entry!");
            if (FILE != type)
                throw new FileAlreadyExistsException(name.toString(), null,
                        "Can only replace a file entry with a file entry, but not a " + typeName(type) + " entry!");
            if (options.get(EXCLUSIVE))
                throw new FileAlreadyExistsException(name.toString());
        }
        while (template instanceof FsCovariantEntry<?>)
            template = ((FsCovariantEntry<?>) template).getEntry(type);
        return new PathLink(path, type, options, template);
    }

    private static String typeName(final FsCovariantEntry<?> entry) {
        return entry.getTypes().toString().toLowerCase(Locale.ENGLISH);
    }

    private static String typeName(final Type type) {
        return type.toString().toLowerCase(Locale.ENGLISH);
    }

    /**
     * TODO: This implementation yields a potential issue: The state of the
     * file system may be altered between the construction of an instance and
     * the call to the {@link #commit} method, which may render the operation
     * illegal and corrupt the file system.
     * As long as only the ArchiveControllers in this package are used, this
     * should not happen, however.
     */
    private final class PathLink implements ArchiveFileSystemOperation<E> {
        final BitField<FsAccessOption> options;
        final SegmentLink<E>[] links;
        long time = UNKNOWN;

        PathLink(   final String path,
                    final Entry.Type type,
                    final BitField<FsAccessOption> options,
                    @CheckForNull final Entry template)
        throws IOException {
            this.options = options;
            links = newSegmentLinks(1, path, type, template);
        }

        @SuppressWarnings("unchecked")
        private SegmentLink<E>[] newSegmentLinks(
                final int level,
                final String entryName,
                final Entry.Type entryType,
                @CheckForNull final Entry template)
        throws IOException {
            splitter.split(entryName);
            final String parentPath = splitter.getParentPath(); // could equal ROOT_PATH
            final String memberName = splitter.getMemberName();
            final SegmentLink<E>[] elements;

            // Lookup parent entry, creating it where necessary and allowed.
            final FsCovariantEntry<E> parentEntry = master.get(parentPath);
            final FsCovariantEntry<E> newEntry;
            if (null != parentEntry) {
                if (!parentEntry.isType(DIRECTORY))
                    throw new NotDirectoryException(entryName);
                elements = new SegmentLink[level + 1];
                elements[0] = new SegmentLink<>(null, parentEntry);
                newEntry = new FsCovariantEntry<>(entryName);
                newEntry.putEntry(entryType,
                        newCheckedEntry(entryName, entryType, options, template));
                elements[1] = new SegmentLink<>(memberName, newEntry);
            } else if (options.get(CREATE_PARENTS)) {
                elements = newSegmentLinks(
                        level + 1, parentPath, DIRECTORY, null);
                newEntry = new FsCovariantEntry<>(entryName);
                newEntry.putEntry(entryType,
                        newCheckedEntry(entryName, entryType, options, template));
                elements[elements.length - level]
                        = new SegmentLink<>(memberName, newEntry);
            } else {
                throw new NoSuchFileException(entryName, null,
                        "Missing parent directory entry!");
            }
            return elements;
        }

        @Override
        public void commit() throws IOException {
            assert 2 <= links.length;

            touch(options);
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
     * @throws IOException on any I/O error.
     */
    void unlink(final FsEntryName name, BitField<FsAccessOption> options)
    throws IOException {
        // Test.
        final String path = name.getPath();
        final FsCovariantEntry<E> ce = master.get(path);
        if (null == ce)
            throw new NoSuchFileException(name.toString());
        if (ce.isType(DIRECTORY)) {
            final int size = ce.getMembers().size();
            if (0 != size)
                throw new DirectoryNotEmptyException(name.toString());
        }
        if (name.isRoot())
            return;

        // Notify listener and modify.
        touch(options);
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
            final long value,
            final BitField<FsAccessOption> options)
    throws IOException {
        if (0 > value)
            throw new IllegalArgumentException(name.toString()
                    + " (negative access time)");
        final FsCovariantEntry<E> ce = master.get(name.getPath());
        if (null == ce)
            throw new NoSuchFileException(name.toString());
        // Order is important here!
        touch(options);
        final E ae = ce.getEntry();
        boolean ok = true;
        for (final Access type : types)
            ok &= ae.setTime(type, value);
        return ok;
    }

    boolean setTime(
            final FsEntryName name,
            final Map<Access, Long> times,
            BitField<FsAccessOption> options)
    throws IOException {
        final FsCovariantEntry<E> ce = master.get(name.getPath());
        if (null == ce)
            throw new NoSuchFileException(name.toString());
        // Order is important here!
        touch(options);
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

    boolean isExecutable(FsEntryName name) {
        return false;
    }

    void setReadOnly(FsEntryName name)
    throws IOException {
        if (!isReadOnly())
            throw new FileSystemException(name.toString(), null,
                "Cannot set read-only state!");
    }

    /**
     * A thread local encoder for fast and convenient checking that a given
     * entry name is encodable with the driver's character set.
     */
    private final class ThreadLocalCharsetEncoder
    extends ThreadLocal<CharsetEncoder> {
        @Override
        protected CharsetEncoder initialValue() {
            return driver.getCharset().newEncoder();
        }

        boolean canEncode(CharSequence cs) {
            return get().canEncode(cs);
        }
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
            this.map = new LinkedHashMap<>(initialCapacity);
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
                map.put(path, ce = new FsCovariantEntry<>(path));
            ce.putEntry(ae.getType(), ae);
            return ce;
        }

        @Nullable FsCovariantEntry<E> get(String path) {
            return map.get(path);
        }

        @Nullable FsCovariantEntry<E> remove(String path) {
            return map.remove(path);
        }
    } // EntryTable

    /** Splits a given path name into its parent path name and base name. */
    private static final class PathSplitter
    extends de.truezip.kernel.io.PathSplitter {
        PathSplitter() {
            super(SEPARATOR_CHAR, false);
        }

        @Override
        public String getParentPath() {
            final String path = super.getParentPath();
            return null != path ? path : ROOT_PATH;
        }
    } // Splitter
}
