/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import static de.truezip.kernel.FsAccessOption.APPEND;
import static de.truezip.kernel.FsAccessOption.CREATE_PARENTS;
import static de.truezip.kernel.FsAccessOptions.NONE;
import de.truezip.kernel.*;
import de.truezip.kernel.cio.Entry.Access;
import static de.truezip.kernel.cio.Entry.Access.READ;
import static de.truezip.kernel.cio.Entry.Access.WRITE;
import de.truezip.kernel.cio.Entry.Type;
import static de.truezip.kernel.cio.Entry.Type.DIRECTORY;
import static de.truezip.kernel.cio.Entry.Type.FILE;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.io.Streams;
import de.truezip.kernel.util.BitField;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An abstract base class for any archive file system controller which
 * provide all the essential services required for accessing a prospective
 * archive file.
 * This base class encapsulates all the code which is not depending on a
 * particular archive update strategy and the corresponding state of this
 * file system controller.
 * <p>
 * Each instance of this class manages an archive file - the <i>target file</i>
 * - in order to allow random access to it as if it were a regular directory in
 * its parent file system.
 * <p>
 * Note that in general all of the methods in this class are reentrant on
 * exceptions.
 * This is important because client applications may repeatedly call them.
 * Of course, depending on the calling context, some or all of the archive
 * file's data may be lost in this case.
 * 
 * @param  <E> the type of the archive entries.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
abstract class BasicArchiveController<E extends FsArchiveEntry>
extends LockModelController {

    private static final Logger logger = Logger.getLogger(
            BasicArchiveController.class.getName(),
            BasicArchiveController.class.getName());

    /**
     * Constructs a new basic archive controller.
     *
     * @param model the non-{@code null} archive model.
     */
    BasicArchiveController(final LockModel model) {
        super(model);
        if (null == model.getParent())
            throw new IllegalArgumentException();
    }

    /** Equivalent to {@link #autoMount(boolean) autoMount(false)}. */
    final ArchiveFileSystem<E> autoMount(BitField<FsAccessOption> options)
    throws IOException {
        return autoMount(false, options);
    }

    /**
     * Returns the (virtual) archive file system mounted from the target
     * archive file. This method is reentrant with respect to any exceptions
     * it may throw.
     *
     * @param  autoCreate If this is {@code true} and the archive file does not
     *         exist, then a new archive file system with only a virtual root
     *         directory is created with its last modification time set to the
     *         system's current time.
     * @return An archive file system.
     */
    abstract ArchiveFileSystem<E> autoMount(
            boolean autoCreate,
            BitField<FsAccessOption> options)
    throws IOException;

    @Override
    public final boolean isReadOnly() throws IOException {
        return autoMount(NONE).isReadOnly();
    }

    @Override
    public final FsEntry entry(FsEntryName name)
    throws IOException {
        return autoMount(NONE).entry(name);
    }

    @Override
    public final boolean isReadable(FsEntryName name) throws IOException {
        return autoMount(NONE).entry(name) != null;
    }

    @Override
    public final boolean isWritable(FsEntryName name) throws IOException {
        return autoMount(NONE).isWritable(name);
    }

    @Override
    public final boolean isExecutable(FsEntryName name) throws IOException {
        return autoMount(NONE).isExecutable(name);
    }

    @Override
    public final void setReadOnly(FsEntryName name) throws IOException {
        autoMount(NONE).setReadOnly(name);
    }

    @Override
    public final boolean setTime(   FsEntryName name,
                                    Map<Access, Long> times,
                                    BitField<FsAccessOption> options)
    throws IOException {
        checkSync(name, null, options);
        return autoMount(options).setTime(name, times, options);
    }

    @Override
    public final boolean setTime(   FsEntryName name,
                                    BitField<Access> types,
                                    long value,
                                    BitField<FsAccessOption> options)
    throws IOException {
        checkSync(name, null, options);
        return autoMount(options).setTime(name, types, value, options);
    }

    @Override
    public final InputSocket<?> input(
            FsEntryName name,
            BitField<FsAccessOption> options) {
        return new Input(name, options);
    }

    @NotThreadSafe
    private final class Input extends DelegatingInputSocket<FsArchiveEntry> {
        final FsEntryName name;
        final BitField<FsAccessOption> options;

        Input(final FsEntryName name, final BitField<FsAccessOption> options) {
            this.name = Objects.requireNonNull(name);
            this.options = Objects.requireNonNull(options);
        }

        @Override
        public FsArchiveEntry localTarget() throws IOException {
            peerTarget(); // may sync() if in same target archive file!
            checkSync(name, READ, options);
            final FsCovariantEntry<E> fse = autoMount(options).entry(name);
            if (null == fse)
                throw new NoSuchFileException(name.toString());
            return fse.getEntry();
        }

        @Override
        protected InputSocket<E> getSocket()
        throws IOException {
            final FsArchiveEntry ae = localTarget();
            final Type type = ae.getType();
            if (FILE != type)
                throw new FileSystemException(name.toString(), null,
                        "Expected a FILE entry, but is a " + type + " entry!");
            return input(ae.getName());
        }
    } // Input

    abstract InputSocket<E> input(String name);

    @Override
    public final OutputSocket<?> output(
            FsEntryName name,
            BitField<FsAccessOption> options,
            @CheckForNull Entry template) {
        return new Output(name, options, template);
    }

    @NotThreadSafe
    private final class Output extends OutputSocket<FsArchiveEntry> {
        final FsEntryName name;
        final BitField<FsAccessOption> options;
        final @CheckForNull Entry template;

        Output( final FsEntryName name,
                final BitField<FsAccessOption> options,
                final @CheckForNull Entry template) {
            this.name = Objects.requireNonNull(name);
            this.options = Objects.requireNonNull(options);
            this.template = template;
        }

        ArchiveFileSystemOperation<E> mknod() throws IOException {
            checkSync(name, WRITE, options);
            // Start creating or overwriting the archive entry.
            // This will fail if the entry already exists as a directory.
            return autoMount(   !name.isRoot() && options.get(CREATE_PARENTS),
                                options)
                    .mknod(name, FILE, options, template);
        }

        @Override
        public FsArchiveEntry localTarget() throws IOException {
            final E ae = mknod().getTarget().getEntry();
            if (options.get(APPEND)) {
                // A proxy entry must get returned here in order to inhibit
                // a peer target to recognize the type of this entry and
                // change the contents of the transferred data accordingly.
                // This would not work when APPENDing.
                return new ProxyEntry(ae);
            }
            return ae;
        }

        @Override
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE") // false positive
        public OutputStream stream() throws IOException {
            final ArchiveFileSystemOperation<E> mknod = mknod();
            final E ae = mknod.getTarget().getEntry();
            try (final InputStream in = append()) {
                final OutputSocket<? extends E> os = output(ae, options);
                if (null == in) // do NOT bind when appending!
                    os.bind(this);
                final OutputStream out = os.stream();
                try {
                    mknod.commit();
                    if (null != in)
                        Streams.cat(in, out);
                } catch (final Throwable ex2) {
                    try {
                        out.close();
                    } catch (final Throwable ex3) {
                        ex2.addSuppressed(ex3);
                    }
                    throw ex2;
                }
                return out;
            }
        }

        @CheckForNull InputStream append() {
            if (options.get(APPEND)) {
                try {
                    return new Input(name, options).stream();
                } catch (IOException ignored) {
                    // When appending, there is no need for the entry to be
                    // readable, so we can safely ignore this - fall through!
                }
            }
            return null;
        }
    } // Output

    private static final class ProxyEntry
    extends DecoratingEntry<FsArchiveEntry>
    implements FsArchiveEntry {
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
    } // ProxyEntry

    abstract OutputSocket<E> output(E entry, BitField<FsAccessOption> options);

    @Override
    public final void mknod(
            final FsEntryName name,
            final Type type,
            final BitField<FsAccessOption> options,
            final Entry template)
    throws IOException {
        if (name.isRoot()) { // TODO: Is this case differentiation still required?
            try {
                autoMount(options); // detect false positives!
            } catch (final FalsePositiveArchiveException ex) {
                if (DIRECTORY != type)
                    throw ex;
                autoMount(true, options);
                return;
            }
            throw new FileAlreadyExistsException(name.toString(), null,
                    "Cannot replace a directory entry!");
        } else {
            checkSync(name, null, options);
            autoMount(options.get(CREATE_PARENTS), options)
                    .mknod(name, type, options, template)
                    .commit();
        }
    }

    @Override
    public void unlink( final FsEntryName name,
                        final BitField<FsAccessOption> options)
    throws IOException {
        checkSync(name, null, options);
        final ArchiveFileSystem<E> fs = autoMount(options);
        fs.unlink(name, options);
        if (name.isRoot()) {
            // Check for any archive entries with absolute entry names.
            final int size = fs.getSize() - 1; // mind the ROOT entry
            if (0 != size)
                logger.log(Level.WARNING, "unlink.absolute",
                        new Object[] { getMountPoint(), size });
        }
    }

    /**
     * Checks if the intended access to the named archive entry in the virtual
     * file system is possible without performing a
     * {@link FsController#sync(BitField, ExceptionHandler) sync} operation in
     * advance.
     *
     * @param  name the file system entry name.
     * @param  intention the intended I/O operation on the archive entry.
     *         If {@code null}, then only an update to the archive entry meta
     *         data (i.e. a pure virtual file system operation with no I/O)
     *         is intended.
     * @throws NeedsSyncException If a sync operation is required before the
     *         intended access could succeed.
     */
    abstract void checkSync(    FsEntryName name,
                                @CheckForNull Access intention,
                                BitField<FsAccessOption> options)
    throws NeedsSyncException;
}
