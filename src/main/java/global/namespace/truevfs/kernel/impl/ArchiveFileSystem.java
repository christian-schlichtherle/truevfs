/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl;

import global.namespace.truevfs.comp.cio.Container;
import global.namespace.truevfs.comp.cio.Entry;
import global.namespace.truevfs.comp.shed.BitField;
import global.namespace.truevfs.comp.shed.PathNormalizer;
import global.namespace.truevfs.comp.shed.PathSplitter;
import global.namespace.truevfs.kernel.api.FsAccessOption;
import global.namespace.truevfs.kernel.api.FsArchiveEntry;
import global.namespace.truevfs.kernel.api.FsCovariantNode;
import global.namespace.truevfs.kernel.api.FsNodeName;
import lombok.Value;
import lombok.val;

import javax.annotation.Nonnull;
import java.io.CharConversionException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;

import static global.namespace.truevfs.comp.cio.Entry.*;
import static global.namespace.truevfs.comp.cio.Entry.Access.WRITE;
import static global.namespace.truevfs.comp.cio.Entry.Type.DIRECTORY;
import static global.namespace.truevfs.comp.cio.Entry.Type.FILE;
import static global.namespace.truevfs.comp.shed.HashMaps.OVERHEAD_SIZE;
import static global.namespace.truevfs.comp.shed.HashMaps.initialCapacity;
import static global.namespace.truevfs.comp.shed.Paths.*;
import static global.namespace.truevfs.kernel.api.FsAccessOption.CREATE_PARENTS;
import static global.namespace.truevfs.kernel.api.FsAccessOption.EXCLUSIVE;
import static global.namespace.truevfs.kernel.api.FsAccessOptions.NONE;
import static global.namespace.truevfs.kernel.api.FsNodeName.*;
import static java.util.Objects.requireNonNull;

/**
 * A read/write virtual file system for archive entries.
 *
 * @param <E> the type of the archive entries.
 * @author Christian Schlichtherle
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
class ArchiveFileSystem<E extends FsArchiveEntry>
        extends AbstractCollection<FsCovariantNode<E>>
        implements ArchiveModelAspect<E> {

    private static final String RootPath = ROOT.getPath();

    private final ArchiveModel<E> model;
    private final EntryTable<E> master;

    private final Splitter splitter = new Splitter();

    /**
     * Returns a new empty archive file system and ensures its integrity.
     * Only the root directory is created with its last modification time set to the system's current time.
     * The file system is set to be modifiable.
     *
     * @param model the archive model to use.
     */
    static <E extends FsArchiveEntry> ArchiveFileSystem<E> create(ArchiveModel<E> model) {
        return new ArchiveFileSystem<>(model);
    }

    private ArchiveFileSystem(final ArchiveModel<E> model) {
        this(model, new EntryTable<>(OVERHEAD_SIZE));
        val root = newEntry(RootPath, DIRECTORY, Optional.empty());
        val time = System.currentTimeMillis();
        ALL_ACCESS.forEach(access -> root.setTime(access, time));
        master.add(RootPath, root);
    }

    /**
     * Returns a new archive file system which populates its entries from the given {@code archive} and ensures its
     * integrity.
     * <p>
     * First, the entries from the archive are loaded into the file system.
     * <p>
     * Second, a root directory with the given last modification time is created and linked into the filesystem (so it's
     * never loaded from the archive).
     * <p>
     * Finally, the file system integrity is checked and fixed:
     * Any missing parent directories are created using the system's current time as their last modification time -
     * existing directories are not replaced.
     * <p>
     * Note that the entries in the file system are shared with the given {@code archive}.
     *
     * @param model        the archive model to use.
     * @param archive      the archive entry container to read the entries for
     *                     the population of the archive file system.
     * @param rootTemplate the optional template to use for the root entry of
     *                     the returned archive file system.
     * @param readOnly     if not empty, any subsequent
     *                     modifying operation on the file system will result in a
     *                     {@link global.namespace.truevfs.kernel.api.FsReadOnlyFileSystemException} with the contained
     *                     {@link java.lang.Throwable} as its cause.
     * @param <E>          the type of the archive entries.
     * @return A new archive file system.
     */
    static <E extends FsArchiveEntry> ArchiveFileSystem<E> create(
            ArchiveModel<E> model,
            Container<E> archive,
            Entry rootTemplate,
            Optional<? extends Supplier<? extends Throwable>> readOnly
    ) throws IOException {
        return readOnly.isPresent()
                ? new ReadOnlyArchiveFileSystem<>(model, archive, rootTemplate, readOnly.get())
                : new ArchiveFileSystem<>(model, archive, rootTemplate);
    }

    ArchiveFileSystem(
            final ArchiveModel<E> model,
            final Container<E> archive,
            final Entry rootTemplate
    ) throws IOException {
        // Allocate some extra capacity for creating missing parent directories.
        this(model, new EntryTable<>(archive.entries().size() + OVERHEAD_SIZE));
        // Load entries from source archive:
        val paths = new LinkedList<String>();
        val normalizer = new PathNormalizer(SEPARATOR_CHAR);
        archive.entries().forEach(ae -> {
            val path = cutTrailingSeparators(
                    // Fix invalid Windoze file name separators:
                    normalizer.normalize(ae.getName().replace('\\', SEPARATOR_CHAR)),
                    SEPARATOR_CHAR
            );
            master.add(path, ae);
            if (isValidEntryName(path)) {
                paths.add(path);
            }
        });
        // Setup root file system entry, potentially replacing its previous mapping from the source archive:
        master.add(RootPath, newEntry(RootPath, DIRECTORY, Optional.of(rootTemplate)));
        // Now perform a file system check to create missing parent directories and populate directories with their
        // members - this must be done separately!
        paths.forEach(this::fix);
    }

    /**
     * Called from a constructor in order to fix the parent directories of the file system entry identified by `name`,
     * ensuring that all parent directories of the file system entry exist and that they contain the respective member
     * entry.
     * If a parent directory does not exist, it is created using an unknown time as the last modification time - this is
     * defined to be a <em>ghost directory</em>.
     * If a parent directory does exist, the respective member entry is added.
     *
     * @param name the entry name.
     */
    private void fix(String name) {
        while (!isRoot(name)) {
            splitter.split(name);
            val pp = splitter.getParentPath().get();
            val mn = splitter.getMemberName();
            val pcn = master
                    .get(pp)
                    .filter(x -> x.isType(DIRECTORY))
                    .orElseGet(() -> master.add(pp, newEntry(pp, DIRECTORY, Optional.empty())));
            pcn.add(mn);
            name = pp;
        }
    }

    private ArchiveFileSystem(final ArchiveModel<E> model, final EntryTable<E> master) {
        this.model = model;
        this.master = master;
    }

    private static String typeName(final FsCovariantNode<?> entry) {
        val types = entry.getTypes();
        if (1 == types.cardinality()) {
            return typeName(types.iterator().next());
        } else {
            return types.toString().toLowerCase(Locale.ROOT);
        }
    }

    private static String typeName(Entry.Type type) {
        return type.toString().toLowerCase(Locale.ROOT);
    }

    private static boolean isValidEntryName(String path) {
        return !isAbsolute(path, SEPARATOR_CHAR) &&
                !(".." + SEPARATOR).startsWith(path.substring(0, Math.min(3, path.length())));
    }

    @Override
    public ArchiveModel<E> getModel() {
        return model;
    }

    @Override
    public int size() {
        return master.size();
    }

    @Override
    public Iterator<FsCovariantNode<E>> iterator() {
        return master.iterator();
    }

    private String fullPath(FsNodeName name) {
        return path(name).toString();
    }

    /**
     * Possibly returns the covariant file system node for the given name.
     * Modifying the returned object graph is either not supported (i.e. throws an
     * {@link java.lang.UnsupportedOperationException} or does not have any visible side effect on this file system.
     *
     * @param name the name of the file system entry to look up.
     * @return A covariant file system node or {@link Optional#empty()} if no file system node exists for the given
     * name.
     */
    Optional<FsCovariantNode<E>> node(final BitField<FsAccessOption> options, final FsNodeName name) {
        return master.get(name.getPath()).map(e -> e.clone(getDriver()));
    }

    void checkAccess(
            final BitField<FsAccessOption> options,
            final FsNodeName name,
            final BitField<Entry.Access> types
    ) throws IOException {
        if (!master.get(name.getPath()).isPresent()) {
            throw new NoSuchFileException(fullPath(name));
        }
    }

    void setReadOnly(final BitField<FsAccessOption> options, final FsNodeName name) throws IOException {
        throw new FileSystemException(fullPath(name), null, "Cannot set read-only state!");
    }

    boolean setTime(
            final BitField<FsAccessOption> options,
            final FsNodeName name,
            final Map<Entry.Access, Long> times
    ) throws IOException {
        val cn = master.get(name.getPath()).orElseThrow(() -> new NoSuchFileException(fullPath(name)));
        // HC SVNT DRACONES!
        touch(options);
        val ae = cn.getEntry();
        boolean ok = true;
        for (val time : times.entrySet()) {
            val access = time.getKey();
            val value = time.getValue();
            ok &= 0 <= value && ae.setTime(access, value);
        }
        return ok;
    }

    boolean setTime(
            final BitField<FsAccessOption> options,
            final FsNodeName name,
            final BitField<Entry.Access> types,
            final long value
    ) throws IOException {
        if (0 > value) {
            throw new IllegalArgumentException(fullPath(name) + " (negative access time)");
        }
        val cn = master.get(name.getPath()).orElseThrow(() -> new NoSuchFileException(fullPath(name)));
        // HC SVNT DRACONES!
        touch(options);
        val ae = cn.getEntry();
        boolean ok = true;
        for (val type : types) {
            ok &= ae.setTime(type, value);
        }
        return ok;
    }

    /**
     * Begins a <em>transaction</em> to create or replace and finally link a chain of one or more archive entries for
     * the given {@code name} into this archive file system.
     * <p>
     * To commit the transaction, you need to call {@link Make#commit()} on the returned object, which will mark this
     * archive file system as touched and set the last modification time of the created and linked archive file system
     * entries to the system's current time at the moment of the call to this method.
     *
     * @param name     the archive file system entry name.
     * @param type     the type of the archive file system entry to create.
     * @param options  if `CREATE_PARENTS` is set, any missing parent
     *                 directories will be created and linked into this file
     *                 system with its last modification time set to the system's
     *                 current time.
     * @param template if not `None`, then the archive file system entry
     *                 at the end of the chain shall inherit as much properties from
     *                 this entry as possible - with the exception of its name and type.
     * @return A new archive file system operation on a chain of one or more archive file system entries for the given
     * path name which will be linked into this archive file system upon a call to the {@link Make#commit()} method of
     * the returned object.
     * @throws IOException on any I/O error.
     */
    Make make(
            final BitField<FsAccessOption> options,
            final FsNodeName name,
            final Entry.Type type,
            final Optional<? extends Entry> template
    ) throws IOException {
        requireNonNull(type);
        // TODO: Add support for other entry types:
        if (FILE != type && DIRECTORY != type) {
            throw new FileSystemException(fullPath(name), null,
                    "Can only create file or directory entries, but not a " + typeName(type) + " entry!");
        }
        val np = name.getPath();
        val ocn = master.get(np);
        if (ocn.isPresent()) {
            val cn = ocn.get();
            if (!cn.isType(FILE)) {
                throw new FileAlreadyExistsException(fullPath(name), null,
                        "Cannot replace a " + typeName(cn) + " entry!");
            }
            if (FILE != type) {
                throw new FileAlreadyExistsException(fullPath(name), null,
                        "Can only replace a file entry with a file entry, but not a " + typeName(type) + " entry!");
            }
            if (options.get(EXCLUSIVE)) {
                throw new FileAlreadyExistsException(fullPath(name));
            }
        }
        val t = template.map(e -> e instanceof FsCovariantNode ? ((FsCovariantNode<?>) e).get(type) : e);
        return new Make(options, np, type, t);
    }

    /**
     * Represents a `make` transaction.
     * The transaction gets committed by calling `commit`.
     * The state of the archive file system will not change until this method gets called.
     * The head of the chain of covariant file system entries to commit can get obtained by calling `head`.
     * <p>
     * TODO:
     * The current implementation yields a potential issue:
     * The state of the file system may get altered between the construction of this transaction and the call to its
     * `commit` method.
     * However, the change may render this operation illegal and so the file system may get corrupted upon a call to
     * `commit`.
     * To avoid this, the caller must not allow concurrent changes to this archive file system.
     */
    final class Make {

        private final BitField<FsAccessOption> options;

        private final LinkedList<Segment<E>> segments;
        private long time = UNKNOWN;

        Make(
                final BitField<FsAccessOption> options,
                final String path,
                final Entry.Type type,
                final Optional<Entry> template
        ) throws IOException {
            this.options = options;
            this.segments = newSegments(path, type, template);
        }

        private String fullPath(String path) {
            return ArchiveFileSystem.this.fullPath(FsNodeName.create(URI.create(path)));
        }

        private LinkedList<Segment<E>> newSegments(
                final String path,
                final Entry.Type type,
                final Optional<Entry> template
        ) throws IOException {
            splitter.split(path);
            val pp = splitter.getParentPath().get(); // may equal ROOT_PATH
            val mn = splitter.getMemberName();
            // Lookup parent entry, creating it if necessary and allowed:
            val opcn = master.get(pp);
            if (opcn.isPresent()) {
                val pcn = opcn.get();
                if (!pcn.isType(DIRECTORY)) {
                    throw new NotDirectoryException(fullPath(path));
                }
                val segments = new LinkedList<Segment<E>>();
                segments.push(new Segment<>(Optional.empty(), pcn));
                val mcn = new FsCovariantNode<E>(path);
                mcn.put(type, newEntry(options, path, type, template));
                segments.push(new Segment<>(Optional.of(mn), mcn));
                return segments;
            } else {
                if (options.get(CREATE_PARENTS)) {
                    LinkedList<Segment<E>> segments = newSegments(pp, DIRECTORY, Optional.empty());
                    val mcn = new FsCovariantNode<E>(path);
                    mcn.put(type, newEntry(options, path, type, template));
                    segments.push(new Segment<>(Optional.of(mn), mcn));
                    return segments;
                } else {
                    throw new NoSuchFileException(fullPath(path), null, "Missing parent directory entry!");
                }
            }
        }

        void commit() throws IOException {
            touch(options);
            val size = commit(segments);
            assert 2 <= size;
            val mae = segments.getFirst().entry.getEntry();
            if (UNKNOWN == mae.getTime(WRITE)) {
                mae.setTime(WRITE, getTimeMillis());
            }
        }

        private int commit(final List<Segment<E>> segments) {
            if (0 < segments.size()) {
                val segment = segments.get(0);
                val mn = segment.getName();
                val mcn = segment.getEntry();
                val parentSegments = segments.subList(1, segments.size());
                val parentSize = commit(parentSegments);
                if (0 < parentSize) {
                    val pcn = parentSegments.get(0).entry;
                    val pae = pcn.get(DIRECTORY);
                    val mae = mcn.getEntry();
                    master.add(mcn.getName(), mae);
                    // Never touch ghost directories:
                    if (master.get(pcn.getName()).get().add(mn.get()) && UNKNOWN != pae.getTime(WRITE)) {
                        pae.setTime(WRITE, getTimeMillis());
                    }
                }
                return 1 + parentSize;
            } else {
                return 0;
            }
        }

        private long getTimeMillis() {
            if (UNKNOWN == time) {
                time = System.currentTimeMillis();
            }
            return time;
        }

        FsCovariantNode<E> head() {
            return segments.getFirst().entry;
        }
    }

    /**
     * Tests the named file system entry and then - unless its the file system root - notifies the listener and deletes
     * the entry.
     * For the file system root, only the tests are performed but the listener does not get notified and the entry does
     * not get deleted.
     * For the tests to succeed, the named file system entry must exist and directory entries (including the file system
     * root) must be empty.
     *
     * @param name the archive file system entry name.
     * @throws IOException on any I/O error.
     */
    void unlink(final BitField<FsAccessOption> options, final FsNodeName name) throws IOException {
        // Test:
        val np = name.getPath();
        val mcn = master.get(np).orElseThrow(() -> new NoSuchFileException(fullPath(name)));
        if (mcn.isType(DIRECTORY)) {
            if (0 != mcn.getMembers().size()) {
                throw new DirectoryNotEmptyException(fullPath(name));
            }
        }
        if (name.isRoot()) {
            // Removing the root entry MUST get silently ignored in order to make the controller logic work.
            return;
        }

        // Notify listener and modify:
        touch(options);
        master.remove(np);
        {
            // See http://java.net/jira/browse/TRUEZIP-144 :
            // This is used to signal to the driver that the entry should not be included in the central directory even
            // if the entry is already physically present in the archive file (ZIP).
            // This signal will be ignored by drivers which do no support a central directory, e.g. for the TAR file
            // format.
            val mae = mcn.getEntry();
            for (val type : ALL_SIZES) {
                mae.setSize(type, UNKNOWN);
            }
            for (val type : ALL_ACCESS) {
                mae.setTime(type, UNKNOWN);
            }
        }
        splitter.split(np);
        val pp = splitter.getParentPath().get();
        val pcn = master.get(pp).get();
        val ok = pcn.remove(splitter.getMemberName());
        assert ok : "The parent directory of \"" + fullPath(name) + "\" does not contain this entry - archive file system is corrupted!";
        val pae = pcn.get(DIRECTORY);
        if (UNKNOWN != pae.getTime(WRITE)) { // never touch ghost directories!
            pae.setTime(WRITE, System.currentTimeMillis());
        }
    }

    /**
     * Returns a new archive entry.
     * Note that this is just a factory method and the returned file system entry is not (yet) linked into this
     * (virtual) archive file system.
     *
     * @param name     the entry name.
     * @param type     the entry type.
     * @param template if present, then the new entry shall inherit as many properties from this entry as possible,
     *                 with the exception of its name and type.
     * @return A new entry for the given name.
     */
    private E newEntry(final String name, final Entry.Type type, final Optional<Entry> template) {
        assert !isRoot(name) || DIRECTORY == type;
        return getDriver().newEntry(NONE, name, type, template.orElse(null));
    }

    /**
     * Returns a new archive entry.
     * Note that this is just a factory method and the returned file system entry is not (yet) linked into this
     * (virtual) archive file system.
     * <p>
     * This version checks that the given entry name can get encoded by the driver's character set.
     *
     * @param name     the entry name.
     * @param options  a bit field of access options.
     * @param type     the entry type.
     * @param template if not `None`, then the new entry shall inherit
     *                 as much properties from this entry as possible - with the
     *                 exception of its name and type.
     * @return A new entry for the given name.
     * @throws CharConversionException If the entry name contains characters
     *                                 which cannot get encoded.
     * @see #make
     */
    private E newEntry(
            final BitField<FsAccessOption> options,
            final String name,
            final Entry.Type type,
            final Optional<Entry> template
    ) throws CharConversionException {
        assert !isRoot(name);
        val driver = this.getDriver();
        driver.checkEncodable(name);
        return driver.newEntry(options, name, type, template.orElse(null));
    }

    /**
     * The master archive entry table.
     *
     * @param <E> The type of the archive entries.
     */
    private static final class EntryTable<E extends FsArchiveEntry> extends AbstractCollection<FsCovariantNode<E>> {

        /**
         * The map of covariant file system entries.
         * <p>
         * Note that the archive entries in the covariant file system entries in this map are shared with the
         * constructor parameter {@code archive} of the archive file system object.
         */
        private final Map<String, FsCovariantNode<E>> map;

        EntryTable(final int initialSize) {
            this.map = new LinkedHashMap<>(initialCapacity(initialSize));
        }

        @Override
        public Iterator<FsCovariantNode<E>> iterator() {
            return map.values().iterator();
        }

        @Override
        public int size() {
            return map.size();
        }

        FsCovariantNode<E> add(String name, E ae) {
            val cn = map.computeIfAbsent(name, FsCovariantNode::new);
            cn.put(ae.getType(), ae);
            return cn;
        }

        Optional<FsCovariantNode<E>> get(String name) {
            return Optional.ofNullable(map.get(name));
        }

        Optional<FsCovariantNode<E>> remove(String name) {
            return Optional.ofNullable(map.remove(name));
        }
    }

    private static final class Splitter extends PathSplitter {

        Splitter() {
            super(SEPARATOR_CHAR, false);
        }

        @Nonnull
        @Override
        public Optional<String> getParentPath() {
            val path = super.getParentPath();
            return path.isPresent() ? path : Optional.of(RootPath);
        }
    }

    /**
     * A case class which represents a path segment for use by {@link ArchiveFileSystem.Make}.
     */
    @Value
    private static class Segment<E extends FsArchiveEntry> {

        /**
         * The optional member name for the covariant file system entry.
         */
        Optional<String> name;

        /**
         * The covariant file system entry for the nullable member name.
         */
        FsCovariantNode<E> entry;
    }
}
