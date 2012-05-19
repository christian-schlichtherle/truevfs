/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se;

import static net.truevfs.kernel.FsAccessOption.CREATE_PARENTS;
import static net.truevfs.kernel.FsAccessOption.EXCLUSIVE;
import static net.truevfs.kernel.FsAccessOptions.NONE;
import static net.truevfs.kernel.FsEntryName.*;
import net.truevfs.kernel.*;
import net.truevfs.kernel.cio.Container;
import net.truevfs.kernel.cio.Entry;
import static net.truevfs.kernel.cio.Entry.Access.WRITE;
import static net.truevfs.kernel.cio.Entry.Type.DIRECTORY;
import static net.truevfs.kernel.cio.Entry.Type.FILE;
import static net.truevfs.kernel.cio.Entry.*;
import net.truevfs.kernel.util.BitField;
import static net.truevfs.kernel.util.HashMaps.OVERHEAD_SIZE;
import static net.truevfs.kernel.util.HashMaps.initialCapacity;
import net.truevfs.kernel.util.PathNormalizer;
import static net.truevfs.kernel.util.Paths.cutTrailingSeparators;
import static net.truevfs.kernel.util.Paths.isRoot;
import java.io.CharConversionException;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A read/write virtual file system for archive entries.
 * Have a look at the online <a href="http://truevfs.java.net/faq.html">FAQ</a>
 * to get the concept of how this works.
 * 
 * @param  <E> the type of the archive entries.
 * @see    <a href="http://truevfs.java.net/faq.html">Frequently Asked Questions</a>
 * @author Christian Schlichtherle
 */
@NotThreadSafe
class ArchiveFileSystem<E extends FsArchiveEntry>
implements Iterable<FsCovariantEntry<E>> {

    private static final String ROOT_PATH = ROOT.getPath();

    private final PathSplitter splitter = new PathSplitter();
    private final FsArchiveDriver<E> driver;
    private final EntryTable<E> master;

    /** Whether or not this file system has been modified. */
    private boolean touched;

    private @CheckForNull TouchListener touchListener;

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
        this(driver, new EntryTable<E>(OVERHEAD_SIZE));
        final E root = newEntry(ROOT_PATH, DIRECTORY, null);
        final long time = System.currentTimeMillis();
        for (final Access access : ALL_ACCESS)
            root.setTime(access, time);
        master.add(ROOT_PATH, root);
        touched = true;
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
            ? new ReadOnlyArchiveFileSystem<>(driver, archive, rootTemplate)
            : new ArchiveFileSystem<>(driver, archive, rootTemplate);
    }

    ArchiveFileSystem(
            final FsArchiveDriver<E> driver,
            final @WillNotClose Container<E> archive,
            final @CheckForNull Entry rootTemplate) {
        // Allocate some extra capacity to create missing parent directories.
        this(driver, new EntryTable<E>(archive.size() + OVERHEAD_SIZE));
        // Load entries from source archive.
        final List<String> paths = new ArrayList<>(archive.size());
        final PathNormalizer normalizer = new PathNormalizer(SEPARATOR_CHAR);
        for (final E ae : archive) {
            final String path = cutTrailingSeparators(
                normalizer.normalize(
                    ae.getName().replace('\\', SEPARATOR_CHAR)), // fix illegal Windoze file name separators
                SEPARATOR_CHAR);
            master.add(path, ae);
            if (!path.startsWith(SEPARATOR)
                    && !(".." + SEPARATOR).startsWith(path.substring(0, Math.min(3, path.length()))))
                paths.add(path);
        }
        // Setup root file system entry, potentially replacing its previous
        // mapping from the source archive.
        master.add(ROOT_PATH, newEntry(ROOT_PATH, DIRECTORY, rootTemplate));
        // Now perform a file system check to create missing parent directories
        // and populate directories with their members - this must be done
        // separately!
        for (final String path : paths) fix(path);
    }

    private ArchiveFileSystem(
            final FsArchiveDriver<E> driver,
            final EntryTable<E> master) {
        this.driver = driver;
        this.master = master;
    }

    /**
     * Called from a constructor in order to fix the parent directories of the
     * file system entry identified by {@code name}, ensuring that all parent
     * directories of the file system entry exist and that they contain the
     * respective member entry.
     * If a parent directory does not exist, it is created using an unkown time
     * as the last modification time - this is defined to be a
     * <i>ghost directory<i>.
     * If a parent directory does exist, the respective member entry is added
     * (possibly yet again) and the process is continued.
     *
     * @param name the entry name.
     */
    private void fix(final String name) {
        // When recursing into this method, it may be called with the root
        // directory as its parameter, so we may NOT skip the following test.
        if (!isRoot(name)) {
            splitter.split(name);
            final String pp = splitter.getParentPath();
            final String mn = splitter.getMemberName();
            FsCovariantEntry<E> pce = master.get(pp);
            if (null == pce || !pce.isType(DIRECTORY))
                pce = master.add(pp, newEntry(pp, DIRECTORY, null));
            pce.add(mn);
            fix(pp);
        }
    }

    int size() {
        return master.size();
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
    final FsCovariantEntry<E> stat(
            final BitField<FsAccessOption> options,
            final FsEntryName name) {
        final FsCovariantEntry<E> ce = master.get(name.getPath());
        return null == ce ? null : ce.clone(driver);
    }

    void checkAccess(
            final BitField<FsAccessOption> options,
            final FsEntryName name,
            final BitField<Access> types)
    throws IOException {
        if (null == master.get(name.getPath()))
            throw new NoSuchFileException(name.toString());
    }

    void setReadOnly(FsEntryName name) throws IOException {
        throw new FileSystemException(name.toString(), null,
            "Cannot set read-only state!");
    }

    boolean setTime(
            final BitField<FsAccessOption> options,
            final FsEntryName name,
            final Map<Access, Long> times)
    throws IOException {
        final FsCovariantEntry<E> ce = master.get(name.getPath());
        if (null == ce)
            throw new NoSuchFileException(name.toString());
        // HC SUNT DRACONES!
        touch(options);
        final E ae = ce.getEntry();
        boolean ok = true;
        for (final Map.Entry<Access, Long> e : times.entrySet()) {
            final long value = e.getValue();
            ok &= 0 <= value && ae.setTime(e.getKey(), value);
        }
        return ok;
    }

    boolean setTime(
            final BitField<FsAccessOption> options,
            final FsEntryName name,
            final BitField<Access> types,
            final long value)
    throws IOException {
        if (0 > value)
            throw new IllegalArgumentException(name.toString()
                    + " (negative access time)");
        final FsCovariantEntry<E> ce = master.get(name.getPath());
        if (null == ce)
            throw new NoSuchFileException(name.toString());
        // HC SUNT DRACONES!
        touch(options);
        final E ae = ce.getEntry();
        boolean ok = true;
        for (final Access type : types)
            ok &= ae.setTime(type, value);
        return ok;
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
    Mknod mknod(
            final BitField<FsAccessOption> options,
            final FsEntryName name,
            final Entry.Type type,
            final @CheckForNull Entry template)
    throws IOException {
        Objects.requireNonNull(type);
        if (FILE != type && DIRECTORY != type) // TODO: Add support for other types.
            throw new FileSystemException(name.toString(), null,
                    "Can only create file or directory entries, but not a " + typeName(type) + " entry!");
        final String path = name.getPath();
        final FsCovariantEntry<E> ce = master.get(path);
        if (null != ce) {
            if (!ce.isType(FILE))
                throw new FileAlreadyExistsException(name.toString(), null,
                        "Cannot replace a " + typeName(ce) + " entry!");
            if (FILE != type)
                throw new FileAlreadyExistsException(name.toString(), null,
                        "Can only replace a file entry with a file entry, but not a " + typeName(type) + " entry!");
            if (options.get(EXCLUSIVE))
                throw new FileAlreadyExistsException(name.toString());
        }
        final Entry e = template instanceof FsCovariantEntry<?>
                ? ((FsCovariantEntry<?>) template).getEntry(type)
                : template;
        return new Mknod(options, path, type, e);
    }

    private static String typeName(final FsCovariantEntry<?> entry) {
        final BitField<Type> types = entry.getTypes();
        return 1 == types.cardinality()
                ? typeName(types.iterator().next())
                : types.toString().toLowerCase(Locale.ENGLISH);
    }

    private static String typeName(final Type type) {
        return type.toString().toLowerCase(Locale.ENGLISH);
    }

    /**
     * Represents an {@linkplain #mknod} transaction.
     * The transaction get committed by calling {@link #commit}.
     * The state of the archive file system will not change until this method
     * gets called.
     * The head of the chain of covariant file system entries to commit can get
     * obtained by calling {@link #head}.
     * <p>
     * TODO: The current implementation yields a potential issue: The state of
     * the file system may get altered between the construction of this
     * transaction and the call to its {@link #commit} method.
     * However, the change may render this operation illegal and so the file
     * system may get corrupted upon a call to {@link #commit}.
     * To avoid this, the caller must not allow concurrent changes to this
     * archive file system.
     */
    final class Mknod {
        final BitField<FsAccessOption> options;
        long time = UNKNOWN;
        final Segment<E>[] segments;

        private Mknod(
                final BitField<FsAccessOption> options,
                final String path,
                final Entry.Type type,
                final @CheckForNull Entry template)
        throws IOException {
            this.options = options;
            segments = newSegments(1, path, type, template);
        }

        @SuppressWarnings("unchecked")
        private Segment<E>[] newSegments(
                final int level,
                final String path,
                final Entry.Type type,
                final @CheckForNull Entry template)
        throws IOException {
            splitter.split(path);
            final String parentPath = splitter.getParentPath(); // may equal ROOT_PATH
            final String memberName = splitter.getMemberName();

            // Lookup parent entry, creating it if necessary and allowed.
            final Segment<E>[] segments;
            final FsCovariantEntry<E> pce = master.get(parentPath);
            final FsCovariantEntry<E> mce;
            if (null != pce) {
                if (!pce.isType(DIRECTORY))
                    throw new NotDirectoryException(path);
                segments = new Segment[level + 1];
                segments[0] = new Segment<>(null, pce);
                mce = new FsCovariantEntry<>(path);
                mce.putEntry(type, newEntry(options, path, type, template));
                segments[1] = new Segment<>(memberName, mce);
            } else if (options.get(CREATE_PARENTS)) {
                segments = newSegments(level + 1, parentPath, DIRECTORY, null);
                mce = new FsCovariantEntry<>(path);
                mce.putEntry(type, newEntry(options, path, type, template));
                segments[segments.length - level]
                        = new Segment<>(memberName, mce);
            } else {
                throw new NoSuchFileException(path, null,
                        "Missing parent directory entry!");
            }
            return segments;
        }

        /** Executes this archive file system operation. */
        void commit() throws IOException {
            assert 2 <= segments.length;

            touch(options);
            final int size = segments.length;
            FsCovariantEntry<E> pce = segments[0].entry;
            E pae = pce.getEntry(DIRECTORY);
            for (int i = 1; i < size ; i++) {
                final Segment<E> segment = segments[i];
                final FsCovariantEntry<E> mce = segment.entry;
                final E mae = mce.getEntry();
                master.add(mce.getName(), mae);
                if (master.get(pce.getName()).add(segment.name)
                        && UNKNOWN != pae.getTime(WRITE)) // never touch ghost directories!
                    pae.setTime(WRITE, getTimeMillis());
                pce = mce;
                pae = mae;
            }
            if (UNKNOWN == pae.getTime(WRITE))
                pae.setTime(WRITE, getTimeMillis());
        }

        private long getTimeMillis() {
            return UNKNOWN != time ? time : (time = System.currentTimeMillis());
        }

        FsCovariantEntry<E> head() {
            return segments[segments.length - 1].entry;
        }
    } // Mknod

    /**
     * A case class which represents a segment for use by {@link Mknod}.
     * 
     * @param <E> The type of the archive entries.
     */
    private static final class Segment<E extends FsArchiveEntry> {
        final @Nullable String name;
        final FsCovariantEntry<E> entry;

        /**
         * Constructs a new {@code SegmentLink}.
         *
         * @param name the nullable member name for the covariant file system
         *        entry.
         * @param entry the covariant file system entry for the nullable member
         *        name.
         */
        Segment(final @CheckForNull String name,
                final FsCovariantEntry<E> entry) {
            this.name = name;
            this.entry = entry;
        }
    } // Segment

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
    void unlink(BitField<FsAccessOption> options, final FsEntryName name)
    throws IOException {
        // Test.
        final String path = name.getPath();
        final FsCovariantEntry<E> mce = master.get(path);
        if (null == mce)
            throw new NoSuchFileException(name.toString());
        if (mce.isType(DIRECTORY)) {
            final int size = mce.getMembers().size();
            if (0 != size)
                throw new DirectoryNotEmptyException(name.toString());
        }
        if (name.isRoot()) {
            // Removing the root entry MUST get silently ignored in order to
            // make the controller logic work.
            return;
        }

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
            final E mae = mce.getEntry();
            for (final Size type : ALL_SIZES)
                mae.setSize(type, UNKNOWN);
            for (final Access type : ALL_ACCESS)
                mae.setTime(type, UNKNOWN);
        }
        splitter.split(path);
        final String pp = splitter.getParentPath();
        final FsCovariantEntry<E> pce = master.get(pp);
        assert null != pce : "The parent directory of \"" + name.toString()
                    + "\" is missing - archive file system is corrupted!";
        final boolean ok = pce.remove(splitter.getMemberName());
        assert ok : "The parent directory of \"" + name.toString()
                    + "\" does not contain this entry - archive file system is corrupted!";
        final E pae = pce.getEntry(DIRECTORY);
        if (UNKNOWN != pae.getTime(WRITE)) // never touch ghost directories!
            pae.setTime(WRITE, System.currentTimeMillis());
    }

    /**
     * Returns a new archive entry.
     * This is just a factory method and the returned file system entry is not
     * (yet) linked into this (virtual) archive file system.
     *
     * @param  name the entry name.
     * @param  options a bit field of access options.
     * @param  type the entry type.
     * @param  template if not {@code null}, then the new entry shall inherit
     *         as much properties from this entry as possible - with the
     *         exception of its name and type.
     * @return A new entry for the given name.
     */
    private E newEntry(final String name, final Type type, @CheckForNull
    final Entry template) {
        assert null != type;
        assert !isRoot(name) || DIRECTORY == type;
        return driver.newEntry(NONE, name, type, template);
    }

    /**
     * Like {@link #entry entry(name, type, options, template)},
     * but checks that the given entry name can get encoded by the driver's
     * character set.
     *
     * @param  name the entry name.
     * @param  options a bit field of access options.
     * @param  type the entry type.
     * @param  template if not {@code null}, then the new entry shall inherit
     *         as much properties from this entry as possible - with the
     *         exception of its name and type.
     * @return A new entry for the given name.
     * @throws CharConversionException If the entry name contains characters
     *         which cannot get encoded.
     * @see    #mknod
     */
    private E newEntry(
            final BitField<FsAccessOption> options,
            final String name,
            final Type type,
            final @CheckForNull Entry template)
    throws CharConversionException {
        assert null != type;
        assert !isRoot(name);
        driver.checkEncodable(name);
        return driver.newEntry(options, name, type, template);
    }

    /**
     * Marks this (virtual) archive file system as touched and notifies the
     * listener if and only if the touch status is changing.
     *
     * @throws IOException If the listener's preTouch implementation vetoed
     *         the operation for any reason.
     */
    private void touch(final BitField<FsAccessOption> options)
    throws IOException {
        if (!touched) {
            final TouchListener tl = touchListener;
            if (null != tl) tl.preTouch(options);
            touched = true;
        }
    }

    /** Gets the archive file system touch listener. */
    final TouchListener getTouchListener() {
        return touchListener;
    }

    /**
     * Sets the archive file system touch listener.
     *
     * @param  listener the listener for archive file system events.
     * @throws IllegalStateException if {@code listener} is not null and the
     *         touch listener has already been set.
     */
    final void setTouchListener(final TouchListener listener) {
        if (null != listener && null != touchListener)
            throw new IllegalStateException("The touch listener has already been set!");
        touchListener = listener;
    }

    /** Used to notify implementations of an event in this file system. */
    @SuppressWarnings("PackageVisibleInnerClass")
    interface TouchListener extends EventListener {
        /**
         * Called immediately before the source archive file system is going to
         * get modified (touched) for the first time.
         * If this method throws an {@code IOException}), then the modification
         * is effectively vetoed.
         *
         * @throws IOException at the discretion of the implementation.
         */
        void preTouch(BitField<FsAccessOption> options) throws IOException;
    } // TouchListener

    /** Splits a given path name into its parent path name and base name. */
    private static final class PathSplitter
    extends net.truevfs.kernel.util.PathSplitter {
        PathSplitter() {
            super(SEPARATOR_CHAR, false);
        }

        @Override
        public String getParentPath() {
            final String path = super.getParentPath();
            return null != path ? path : ROOT_PATH;
        }
    } // Splitter

    /**
     * The master archive entry table.
     * 
     * @param <E> The type of the archive entries.
     */
    private static final class EntryTable<E extends FsArchiveEntry>
    implements Iterable<FsCovariantEntry<E>> {

        /**
         * The map of covariant file system entries.
         * <p>
         * Note that the archive entries in the covariant file system entries
         * in this map are shared with the {@link Container} object
         * provided to the constructor of this class.
         */
        final Map<String, FsCovariantEntry<E>> map;

        EntryTable(int initialSize) {
            this.map = new LinkedHashMap<>(initialCapacity(initialSize));
        }

        int size() {
            return map.size();
        }

        @Override
        public Iterator<FsCovariantEntry<E>> iterator() {
            return Collections.unmodifiableCollection(map.values()).iterator();
        }

        FsCovariantEntry<E> add(final String name, final E ae) {
            FsCovariantEntry<E> ce = map.get(name);
            if (null == ce)
                map.put(name, ce = new FsCovariantEntry<>(name));
            ce.putEntry(ae.getType(), ae);
            return ce;
        }

        @Nullable FsCovariantEntry<E> get(String name) {
            return map.get(name);
        }

        @Nullable FsCovariantEntry<E> remove(String name) {
            return map.remove(name);
        }
    } // EntryTable
}
