/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl;

import lombok.val;
import global.namespace.truevfs.comp.cio.*;
import global.namespace.truevfs.comp.io.Streams;
import global.namespace.truevfs.comp.logging.LocalizedLogger;
import global.namespace.truevfs.comp.shed.BitField;
import global.namespace.truevfs.kernel.spec.*;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static global.namespace.truevfs.comp.cio.Entry.Access.*;
import static global.namespace.truevfs.comp.cio.Entry.Type.DIRECTORY;
import static global.namespace.truevfs.comp.cio.Entry.Type.FILE;
import static global.namespace.truevfs.kernel.spec.FsAccessOption.APPEND;
import static global.namespace.truevfs.kernel.spec.FsAccessOption.CREATE_PARENTS;
import static global.namespace.truevfs.kernel.spec.FsAccessOptions.NONE;

/**
 * An abstract base class for any archive file system controller which provides all the essential services required for
 * accessing a prospective archive file.
 * This base class encapsulates all the code which is not depending on a particular archive update strategy and the
 * corresponding state of this file system controller.
 * <p>
 * Each instance of this class manages an archive file - the "target archive file" - in order to allow random access to
 * it as if it were a regular directory in its parent file system.
 * <p>
 * In general all of the methods in this class are reentrant on exceptions.
 * This is important because client applications may repeatedly call them.
 * Of course, depending on the calling context, some or all of the archive file's data may be lost in this case.
 *
 * @param <E> the type of the archive entries.
 * @author Christian Schlichtherle
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
abstract class BasicArchiveController<E extends FsArchiveEntry> implements ArchiveController<E> {

    private static final Logger logger = new LocalizedLogger(BasicArchiveController.class);

    private String fullPath(FsNodeName name) {
        return path(name).toString();
    }

    @Override
    public Optional<? extends FsNode> node(BitField<FsAccessOption> options, FsNodeName name) throws IOException {
        return autoMount(options, false).node(options, name);
    }

    @Override
    public void checkAccess(BitField<FsAccessOption> options, FsNodeName name, BitField<Entry.Access> types) throws IOException {
        autoMount(options, false).checkAccess(options, name, types);
    }

    @Override
    public void setReadOnly(BitField<FsAccessOption> options, FsNodeName name) throws IOException {
        autoMount(NONE, false).setReadOnly(options, name);
    }

    @Override
    public boolean setTime(final BitField<FsAccessOption> options, final FsNodeName name, final Map<Entry.Access, Long> times) throws IOException {
        checkSync(options, name, CREATE); // alias for UPDATE
        return autoMount(options, false).setTime(options, name, times);
    }

    @Override
    public boolean setTime(final BitField<FsAccessOption> options, final FsNodeName name, final BitField<Entry.Access> types, final long time) throws IOException {
        checkSync(options, name, CREATE); // alias for UPDATE
        return autoMount(options, false).setTime(options, name, types, time);
    }

    @Override
    public InputSocket<? extends Entry> input(final BitField<FsAccessOption> options, final FsNodeName name) {
        requireNonNull(options);
        requireNonNull(name);

        return new AbstractInputSocket<E>() {

            @Override
            public E target() throws IOException {
                checkSync(options, name, READ);
                val optNode = autoMount(options, false).node(options, name);
                if (optNode.isPresent()) {
                    val node = optNode.get();
                    val ae = node.get(FILE);
                    if (null == ae) {
                        throw new FileSystemException(fullPath(name), null,
                                "Expected a FILE entry, but is a " + node.getTypes() + " entry!");
                    }
                    return ae;
                }
                throw new NoSuchFileException(fullPath(name));
            }

            @Override
            public InputStream stream(Optional<? extends OutputSocket<? extends Entry>> peer) throws IOException {
                return socket(peer).stream(peer);
            }

            @Override
            public SeekableByteChannel channel(Optional<? extends OutputSocket<? extends Entry>> peer) throws IOException {
                return socket(peer).channel(peer);
            }

            InputSocket<E> socket(final Optional<? extends OutputSocket<? extends Entry>> peer) throws IOException {
                if (peer.isPresent()) {
                    peer.get().target(); // may sync() if in same target archive file!
                }
                return input(target().getName());
            }
        };
    }

    abstract InputSocket<E> input(String name);

    @Override
    public OutputSocket<? extends Entry> output(final BitField<FsAccessOption> options, final FsNodeName name, final Optional<? extends Entry> template) {
        return new AbstractOutputSocket<FsArchiveEntry>() {

            @Override
            public FsArchiveEntry target() throws IOException {
                val ae = make().head().getEntry();
                if (options.get(APPEND)) {
                    // A proxy entry must get returned here in order to inhibit
                    // a peer target to recognize the type of this entry and
                    // switch to Raw Data Copy (RDC) mode.
                    // This would not work when APPENDing.
                    return new ProxyEntry(ae);
                } else {
                    return ae;
                }
            }

            @Override
            public OutputStream stream(final Optional<? extends InputSocket<? extends Entry>> peer) throws IOException {
                val tx = make();
                val ae = tx.head().getEntry();
                val in = append();
                Throwable t1 = null;
                try {
                    val os = output(options, ae);
                    val out = os.stream(in.isPresent()
                            ? Optional.empty() // do NOT bind when appending!
                            : peer);
                    try {
                        tx.commit();
                        if (in.isPresent()) {
                            Streams.cat(in.get(), out);
                        }
                    } catch (final Throwable t2) {
                        try {
                            out.close();
                        } catch (final Throwable t3) {
                            t2.addSuppressed(t3);
                        }
                        throw t2;
                    }
                    return out;
                } catch (final Throwable t2) {
                    t1 = t2;
                    throw t2;
                } finally {
                    if (in.isPresent()) {
                        try {
                            in.get().close();
                        } catch (final Throwable t2) {
                            if (null != t1) {
                                t1.addSuppressed(t2);
                            } else {
                                throw t2;
                            }
                        }
                    }
                }
            }

            ArchiveFileSystem<E>.Make make() throws IOException {
                checkSync(options, name, WRITE);
                // Start creating or overwriting the archive entry.
                // This will fail if the entry already exists as a directory.
                return autoMount(options, !name.isRoot() && options.get(CREATE_PARENTS))
                        .make(options, name, FILE, template);
            }

            Optional<InputStream> append() {
                if (options.get(APPEND)) {
                    try {
                        return Optional.of(input(options, name).stream(Optional.empty()));
                    } catch (IOException ignored) {
                        // When appending, there is no need for the entry to be
                        // readable or even exist, so this can get safely ignored.
                    }
                }
                return Optional.empty();
            }
        };
    }

    abstract OutputSocket<E> output(BitField<FsAccessOption> options, E entry);

    @Override
    public void make(final BitField<FsAccessOption> options, final FsNodeName name, final Entry.Type type, final Optional<? extends Entry> template) throws IOException {
        if (name.isRoot()) { // TODO: Is this case differentiation still required?
            try {
                autoMount(options, false); // detect false positives!
            } catch (final FalsePositiveArchiveException e) {
                if (DIRECTORY != type) {
                    throw e;
                }
                autoMount(options, true);
                return;
            }
            throw new FileAlreadyExistsException(fullPath(name), null, "Cannot replace a directory entry!");
        } else {
            checkSync(options, name, CREATE);
            autoMount(options, options.get(CREATE_PARENTS))
                    .make(options, name, type, template)
                    .commit();
        }
    }

    @Override
    public void unlink(final BitField<FsAccessOption> options, final FsNodeName name) throws IOException {
        checkSync(options, name, DELETE);
        val fs = autoMount(options, false);
        fs.unlink(options, name);
        if (name.isRoot()) {
            // Check for any archive entries with absolute entry names.
            val size = fs.size() - 1; // mind the ROOT entry
            if (0 != size) {
                logger.warn("unlink.absolute", getMountPoint(), size);
            }
        }
    }

    /**
     * Checks if the intended access to the named archive entry in the virtual file system is possible without
     * performing a {@link FsController#sync(BitField)} operation in advance.
     *
     * @param options   the options for accessing the file system entry.
     * @param name      the name of the file system entry.
     * @param intention the intended I/O operation on the archive entry.
     * @throws NeedsSyncException If a sync operation is required before the intended access could succeed.
     */
    abstract void checkSync(BitField<FsAccessOption> options, FsNodeName name, Entry.Access intention) throws NeedsSyncException;

    /**
     * Returns the (virtual) archive file system mounted from the target archive file.
     *
     * @param options    the options for accessing the file system entry.
     * @param autoCreate If this is `true` and the archive file does not exist, then a new archive file system with only
     *                   a virtual root directory is created with its last modification time set to the system's current
     *                   time.
     * @return An archive file system.
     * @throws IOException on any I/O error.
     */
    abstract ArchiveFileSystem<E> autoMount(BitField<FsAccessOption> options, boolean autoCreate) throws IOException;

    private static final class ProxyEntry extends DecoratingEntry<FsArchiveEntry> implements FsArchiveEntry {

        ProxyEntry(FsArchiveEntry entry) {
            super(entry);
        }

        @Override
        public Type getType() {
            return entry.getType();
        }

        @Override
        public boolean setSize(Size type, long value) {
            return entry.setSize(type, value);
        }

        @Override
        public boolean setTime(Access type, long value) {
            return entry.setTime(type, value);
        }

        @Override
        public boolean setPermitted(Access type, Entity entity, @Nullable Boolean value) {
            return entry.setPermitted(type, entity, value);
        }
    }
}
