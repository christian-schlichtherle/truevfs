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
import de.truezip.kernel.io.InputException;
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
        return autoMount(options, false);
    }

    /**
     * Returns the (virtual) archive file system mounted from the target
     * archive file.
     *
     * @param  options the options for accessing the file system entry.
     * @param  autoCreate If this is {@code true} and the archive file does not
     *         exist, then a new archive file system with only a virtual root
     *         directory is created with its last modification time set to the
     *         system's current time.
     * @return An archive file system.
     * @throws IOException on any I/O error.
     */
    abstract ArchiveFileSystem<E> autoMount(
            BitField<FsAccessOption> options,
            boolean autoCreate)
    throws IOException;

    @Override
    public final boolean isReadOnly() throws IOException {
        return autoMount(NONE).isReadOnly();
    }

    @Override
    public final FsEntry stat(
            BitField<FsAccessOption> options,
            FsEntryName name)
    throws IOException {
        return autoMount(options).stat(options, name);
    }

    @Override
    public final void checkAccess(
            BitField<FsAccessOption> options,
            FsEntryName name,
            BitField<Access> types)
    throws IOException {
        autoMount(options).checkAccess(options, name, types);
    }

    @Override
    public final void setReadOnly(FsEntryName name) throws IOException {
        autoMount(NONE).setReadOnly(name);
    }

    @Override
    public final boolean setTime(
            BitField<FsAccessOption> options,
            FsEntryName name,
            Map<Access, Long> times)
    throws IOException {
        checkSync(options, name, null);
        return autoMount(options).setTime(options, name, times);
    }

    @Override
    public final boolean setTime(
            BitField<FsAccessOption> options,
            FsEntryName name,
            BitField<Access> types, long value)
    throws IOException {
        checkSync(options, name, null);
        return autoMount(options).setTime(options, name, types, value);
    }

    @Override
    public final InputSocket<?> input(
            final BitField<FsAccessOption> options,
            final FsEntryName name) {
        Objects.requireNonNull(options);
        Objects.requireNonNull(name);

        @NotThreadSafe
        class Input extends DelegatingInputSocket<E> {
            @Override
            public E localTarget() throws IOException {
                peerTarget(); // may sync() if in same target archive file!
                checkSync(options, name, READ);
                final FsCovariantEntry<E> ce = autoMount(options).stat(options, name);
                if (null == ce)
                    throw new NoSuchFileException(name.toString());
                return ce.getEntry();
            }

            @Override
            protected InputSocket<E> socket() throws IOException {
                final FsArchiveEntry ae = localTarget();
                final Type type = ae.getType();
                if (FILE != type)
                    throw new FileSystemException(name.toString(), null,
                            "Expected a FILE entry, but is a " + type + " entry!");
                return input(ae.getName());
            }
        }
        return new Input();
    }

    abstract InputSocket<E> input(String name);

    @Override
    public final OutputSocket<?> output(
            final BitField<FsAccessOption> options,
            final FsEntryName name,
            final @CheckForNull Entry template) {
        Objects.requireNonNull(options);
        Objects.requireNonNull(name);

        @NotThreadSafe
        class Output extends AbstractOutputSocket<FsArchiveEntry> {
            @Override
            public FsArchiveEntry localTarget() throws IOException {
                final E ae = mknod().get().getEntry();
                if (options.get(APPEND)) {
                    // A proxy entry must get returned here in order to inhibit
                    // a peer target to recognize the type of this entry and
                    // switch to Raw Data Copy (RDC) mode.
                    // This would not work when APPENDing.
                    return new ProxyEntry(ae);
                }
                return ae;
            }

            @Override
            @edu.umd.cs.findbugs.annotations.SuppressWarnings("RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE") // false positive
            public OutputStream stream() throws IOException {
                final ArchiveFileSystemOperation<E> op = mknod();
                final E ae = op.get().getEntry();
                final InputStream in = append();
                Throwable ex = null;
                try {
                    final OutputSocket<? extends E> os = output(options, ae);
                    if (null == in) // do NOT bind when appending!
                        os.bind(this);
                    final OutputStream out = os.stream();
                    try {
                        op.commit();
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
                } catch (final Throwable ex2) {
                    ex = ex2;
                    throw ex2;
                } finally {
                    if (null != in) {
                        try {
                            in.close();
                        } catch (final IOException ex2) {
                            final IOException ex3 = new InputException(ex2);
                            if (null == ex)
                                throw ex3;
                            ex.addSuppressed(ex3);
                        } catch (final Throwable ex2) {
                            if (null == ex)
                                throw ex2;
                            ex.addSuppressed(ex2);
                        }
                    }
                }
            }

            ArchiveFileSystemOperation<E> mknod() throws IOException {
                checkSync(options, name, WRITE);
                // Start creating or overwriting the archive entry.
                // This will fail if the entry already exists as a directory.
                return autoMount(options, !name.isRoot() && options.get(CREATE_PARENTS))
                        .mknod(options, name, FILE, template);
            }

            @CheckForNull InputStream append() {
                if (options.get(APPEND)) {
                    try {
                        return input(options, name).stream();
                    } catch (IOException ignored) {
                        // When appending, there is no need for the entry to be
                        // readable or even exist, so this can get safely ignored.
                    }
                }
                return null;
            }
        }
        return new Output();
    }

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

        @Override
        public Boolean isPermitted(Access type, Entity entity) {
            return entry.isPermitted(type, entity);
        }

        @Override
        public boolean setPermitted(Access type, Entity entity, Boolean value) {
            return entry.setPermitted(type, entity, value);
        }
    } // ProxyEntry

    abstract OutputSocket<E> output(BitField<FsAccessOption> options, E entry);

    @Override
    public final void mknod(
            final BitField<FsAccessOption> options,
            final FsEntryName name,
            final Type type,
            final Entry template)
    throws IOException {
        if (name.isRoot()) { // TODO: Is this case differentiation still required?
            try {
                autoMount(options); // detect false positives!
            } catch (final FalsePositiveArchiveException ex) {
                if (DIRECTORY != type)
                    throw ex;
                autoMount(options, true);
                return;
            }
            throw new FileAlreadyExistsException(name.toString(), null,
                    "Cannot replace a directory entry!");
        } else {
            checkSync(options, name, null);
            autoMount(options, options.get(CREATE_PARENTS))
                    .mknod(options, name, type, template)
                    .commit();
        }
    }

    @Override
    public void unlink(
            final BitField<FsAccessOption> options,
            final FsEntryName name)
    throws IOException {
        checkSync(options, name, null);
        final ArchiveFileSystem<E> fs = autoMount(options);
        fs.unlink(options, name);
        if (name.isRoot()) {
            // Check for any archive entries with absolute entry names.
            final int size = fs.size() - 1; // mind the ROOT entry
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
     * @param  options the options for accessing the file system entry.
     * @param  name the name of the file system entry.
     * @param  intention the intended I/O operation on the archive entry.
     *         If {@code null}, then only an update to the archive entry meta
     *         data (i.e. a pure virtual file system operation with no I/O)
     *         is intended.
     * @throws NeedsSyncException If a sync operation is required before the
     *         intended access could succeed.
     */
    abstract void checkSync(
            BitField<FsAccessOption> options,
            FsEntryName name,
            @CheckForNull Access intention)
    throws NeedsSyncException;
}
