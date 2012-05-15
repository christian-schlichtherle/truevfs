/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel.se;

import de.schlichtherle.truezip.kernel.ClutchInputSocket;
import de.schlichtherle.truezip.kernel.ClutchOutputSocket;
import de.schlichtherle.truezip.kernel.ControlFlowException;
import de.schlichtherle.truezip.kernel.FalsePositiveArchiveException;
import de.schlichtherle.truezip.kernel.LockInputService;
import de.schlichtherle.truezip.kernel.LockOutputService;
import de.schlichtherle.truezip.kernel.NeedsLockRetryException;
import de.schlichtherle.truezip.kernel.NeedsSyncException;
import de.schlichtherle.truezip.kernel.PersistentFalsePositiveArchiveException;
import static de.schlichtherle.truezip.kernel.se.ArchiveFileSystem.newEmptyFileSystem;
import static de.schlichtherle.truezip.kernel.se.ArchiveFileSystem.newPopulatedFileSystem;
import static de.truezip.kernel.FsAccessOption.CACHE;
import static de.truezip.kernel.FsAccessOption.GROW;
import static de.truezip.kernel.FsAccessOptions.ACCESS_PREFERENCES_MASK;
import static de.truezip.kernel.FsSyncOption.ABORT_CHANGES;
import static de.truezip.kernel.FsSyncOption.CLEAR_CACHE;
import de.truezip.kernel.*;
import static de.truezip.kernel.cio.Entry.ALL_SIZES;
import de.truezip.kernel.cio.Entry.Access;
import static de.truezip.kernel.cio.Entry.Access.READ;
import static de.truezip.kernel.cio.Entry.Access.WRITE;
import de.truezip.kernel.cio.Entry.Size;
import static de.truezip.kernel.cio.Entry.Size.DATA;
import static de.truezip.kernel.cio.Entry.Type.DIRECTORY;
import static de.truezip.kernel.cio.Entry.Type.SPECIAL;
import static de.truezip.kernel.cio.Entry.UNKNOWN;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.io.InputClosedException;
import de.truezip.kernel.io.InputException;
import de.truezip.kernel.io.OutputClosedException;
import de.truezip.kernel.util.BitField;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.NoSuchFileException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Manages I/O to the entry which represents the target archive file in its
 * parent file system, detects archive entry collisions and implements a sync
 * of the target archive file.
 * <p>
 * This controller is an emitter for {@link ControlFlowException}s:
 * for example when
 * {@linkplain FalsePositiveArchiveException detecting a false positive archive file}, or
 * {@linkplain NeedsWriteLockException requiring a write lock} or
 * {@linkplain NeedsSyncException requiring a sync}.
 *
 * @param  <E> the type of the archive entries.
 * @see    FalsePositiveArchiveException
 * @see    NeedsWriteLockException
 * @see    NeedsSyncException
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class TargetArchiveController<E extends FsArchiveEntry>
extends FileSystemArchiveController<E>
implements ArchiveFileSystem.TouchListener {

    private static final BitField<FsAccessOption>
            MOUNT_OPTIONS = BitField.of(CACHE);

    private static final BitField<Access> WRITE_ACCESS = BitField.of(WRITE);

    private final FsArchiveDriver<E> driver;
    
    /** The parent file system controller. */
    private final FsController<?> parent;

    /** The entry name of the target archive file in the parent file system. */
    private final FsEntryName name;

    /**
     * The (possibly cached) {@link InputArchive} which is used to mount the
     * (virtual) archive file system and read the entries from the target
     * archive file.
     */
    private @CheckForNull InputArchive<E> inputArchive;

    /**
     * The (possibly cached) {@link OutputArchive} which is used to write the
     * entries to the target archive file.
     */
    private @CheckForNull OutputArchive<E> outputArchive;

    /**
     * Constructs a new target archive controller.
     * 
     * @param model the file system model.
     * @param parent the parent file system
     * @param driver the archive driver.
     */
    TargetArchiveController(
            final FsArchiveDriver<E> driver,
            final LockModel model,
            final FsController<?> parent) {
        super(model);
        this.driver = Objects.requireNonNull(driver);
        if (model.getParent() != (this.parent = parent).getModel())
            throw new IllegalArgumentException("Parent/member mismatch!");
        this.name = getMountPoint().getPath().getEntryName();
        assert invariants();
    }

    private boolean invariants() {
        assert null != driver;
        assert null != parent;
        assert null != name;
        final ArchiveFileSystem<E> fs = getFileSystem();
        final InputArchive<E> ia = inputArchive;
        final OutputArchive<E> oa = outputArchive;
        assert null == ia || null != fs : "null != ia => null != fs";
        assert null == oa || null != fs : "null != oa => null != fs";
        assert null == fs || null != ia || null != oa : "null != fs => null != ia || null != oa";
        // This is effectively the same than the last three assertions, but is
        // harder to trace in the field on failure.
        //assert null != fs == (null != ia || null != oa);
        return true;
    }

    @Override
    public FsController<?> getParent() {
        return parent;
    }

    @Nullable InputArchive<E> getInputArchive() throws NeedsSyncException {
        final InputArchive<E> ia = inputArchive;
        if (null != ia && ia.isClosed())
            throw NeedsSyncException.get();
        return ia;
    }

    private void setInputArchive(final @CheckForNull InputArchive<E> ia) {
        assert null == ia || null == inputArchive;
        if (null != ia)
            setTouched(true);
        inputArchive = ia;
    }

    @Nullable OutputArchive<E> getOutputArchive() throws NeedsSyncException {
        final OutputArchive<E> oa = outputArchive;
        if (null != oa && oa.isClosed())
            throw NeedsSyncException.get();
        return oa;
    }

    private void setOutputArchive(final @CheckForNull OutputArchive<E> oa) {
        assert null == oa || null == outputArchive;
        if (null != oa)
            setTouched(true);
        outputArchive = oa;
    }

    @Override
    public void preTouch(BitField<FsAccessOption> options) throws IOException {
        outputArchive(options);
    }

    @Override
    void mount(BitField<FsAccessOption> options, final boolean autoCreate)
    throws IOException {
        try {
            mount0(options, autoCreate);
        } finally {
            assert invariants();
        }
    }

    private void mount0(
            final BitField<FsAccessOption> options,
            final boolean autoCreate)
    throws IOException {
        // HC SUNT DRACONES!
        
        // Check parent file system entry.
        final FsEntry pe; // parent entry
        try {
            pe = parent.stat(options, name);
        } catch (final FalsePositiveArchiveException ex) {
            throw new AssertionError(ex);
        } catch (final IOException inaccessibleEntry) {
            if (autoCreate)
                throw inaccessibleEntry;
            throw new FalsePositiveArchiveException(inaccessibleEntry);
        }

        // Obtain file system by creating or loading it from the parent entry.
        final ArchiveFileSystem<E> fs;
        if (null == pe) {
            if (autoCreate) {
                // This may fail e.g. if the container file is an RAES
                // encrypted ZIP file and the user cancels password prompting.
                outputArchive(options);
                fs = newEmptyFileSystem(driver);
            } else {
                throw new FalsePositiveArchiveException(
                        new NoSuchFileException(name.toString()));
            }
        } else {
            // ro must be init first because the parent archive
            // controller could be a FileController and on Windows this
            // property changes to TRUE once a file is opened for reading!
            final boolean ro = isReadOnlyTarget();
            final InputService<E> is;
            try {
                is = driver.newInput(getModel(), MOUNT_OPTIONS, parent, name);
            } catch (final FalsePositiveArchiveException ex) {
                throw new AssertionError(ex);
            } catch (final IOException ex) {
                throw pe.isType(SPECIAL)
                        ? new FalsePositiveArchiveException(ex)
                        : new PersistentFalsePositiveArchiveException(ex);
            }
            fs = newPopulatedFileSystem(driver, is, pe, ro);
            setInputArchive(new InputArchive<>(is));
            assert isTouched();
        }

        // Register file system.
        fs.setTouchListener(this);
        setFileSystem(fs);
    }

    private boolean isReadOnlyTarget() {
        try {
            parent.checkAccess(MOUNT_OPTIONS, name, WRITE_ACCESS);
            return false;
        } catch (final FalsePositiveArchiveException ex) {
            throw new AssertionError(ex);
        } catch (final IOException ex) {
            return true;
        }
    }

    /**
     * Ensures that {@link #outputArchive} does not return {@code null}.
     * This method will use
     * <code>{@link #getContext()}.{@link FsOperationContext#getOutputOptions()}</code>
     * to obtain the output options to use for writing the entry in the parent
     * file system.
     * 
     * @return The output archive.
     */
    @CreatesObligation
    OutputArchive<E> outputArchive(BitField<FsAccessOption> options)
    throws IOException {
        OutputArchive<E> oa = getOutputArchive();
        if (null != oa) {
            assert isTouched();
            return oa;
        }
        final InputArchive<E> ia = getInputArchive();
        final InputService<E> is = null == ia ? null : ia.getDriverProduct();
        final FsModel model = getModel();
        options = options.and(ACCESS_PREFERENCES_MASK).set(CACHE);
        final OutputService<E> os;
        try {
            os = driver.newOutput(model, options, parent, name, is);
        } catch (final FalsePositiveArchiveException ex) {
            throw new AssertionError(ex);
        } catch (final ControlFlowException ex) {
            assert ex instanceof NeedsLockRetryException : ex;
            throw ex;
        }
        oa = new OutputArchive<>(os);
        setOutputArchive(oa);
        assert isTouched();
        return oa;
    }

    @Override
    InputSocket<E> input(final String name) {
        class Input extends ClutchInputSocket<E> {
            @Override
            protected InputSocket<E> lazySocket() throws IOException {
                return getInputArchive().input(name);
            }

            @Override
            public E localTarget() throws IOException {
                try {
                    return super.localTarget();
                } catch (InputClosedException discarded) {
                    throw NeedsSyncException.get();
                }
            }

            @Override
            public InputStream stream() throws IOException {
                try {
                    return super.stream();
                } catch (InputClosedException discarded) {
                    throw NeedsSyncException.get();
                }
            }

            @Override
            public SeekableByteChannel channel() throws IOException {
                try {
                    return super.channel();
                } catch (InputClosedException discarded) {
                    throw NeedsSyncException.get();
                }
            }
        } // Input

        return new Input();
    }

    @Override
    OutputSocket<E> output(
            final BitField<FsAccessOption> options,
            final E entry) {
        final class Output extends ClutchOutputSocket<E> {
            @Override
            protected OutputSocket<E> lazySocket() throws IOException {
                return outputArchive(options).output(entry);
            }

            @Override
            public E localTarget() throws IOException {
                return entry;
            }

            @Override
            public OutputStream stream() throws IOException {
                try {
                    return super.stream();
                } catch (OutputClosedException discarded) {
                    throw NeedsSyncException.get();
                }
            }

            @Override
            public SeekableByteChannel channel() throws IOException {
                try {
                    return super.channel();
                } catch (OutputClosedException discarded) {
                    throw NeedsSyncException.get();
                }
            }
        } // Output

        return new Output();
    }

    @Override
    public void sync(final BitField<FsSyncOption> options)
    throws FsSyncWarningException, FsSyncException {
        assert isWriteLockedByCurrentThread();
        try {
            final FsSyncExceptionBuilder builder = new FsSyncExceptionBuilder();
            if (!options.get(ABORT_CHANGES))
                copy(builder);
            close(options, builder);
            builder.check();
        } finally {
            assert invariants();
        }
    }

    /**
     * Synchronizes all entries in the (virtual) archive file system with the
     * (temporary) output archive file.
     *
     * @param  handler the strategy for assembling sync exceptions.
     */
    private void copy(final FsSyncExceptionBuilder handler)
    throws FsSyncException {
        // Skip (In|Out)putArchive for better performance.
        // This is safe because the ResourceController has already shut down
        // all concurrent access by closing the respective resources (streams,
        // channels etc).
        // The Disconnecting(In|Out)putService should not get skipped however:
        // If these would throw an (In|Out)putClosedException, then this would
        // be an artifact of a bug.
        final InputService<E> is;
        {
            final InputArchive<E> ia = inputArchive;
            if (null != ia && ia.isClosed())
                return;
            assert null == ia || !ia.isClosed();
            is = null != ia ? ia.getClutch() : new DummyInputService<E>();
        }

        final OutputService<E> os;
        {
            final OutputArchive<E> oa = outputArchive;
            if (null == oa || oa.isClosed())
                return;
            assert !oa.isClosed();
            os = oa.getClutch();
        }

        IOException warning = null;
        for (final FsCovariantEntry<E> fse : getFileSystem()) {
            for (final E ae : fse.getEntries()) {
                final String aen = ae.getName();
                if (null == os.entry(aen)) {
                    try {
                        if (DIRECTORY == ae.getType()) {
                            if (!fse.isRoot()) // never output the root directory!
                                if (UNKNOWN != ae.getTime(WRITE)) // never output a ghost directory!
                                    os.output(ae).stream().close();
                        } else if (null != is.entry(aen)) {
                            IOSockets.copy(is.input(aen), os.output(ae));
                        } else {
                            // The file system entry is a newly created
                            // non-directory entry which hasn't received any
                            // content yet, e.g. as a result of mknod()
                            // => output an empty file system entry.
                            for (final Size size : ALL_SIZES)
                                ae.setSize(size, UNKNOWN);
                            ae.setSize(DATA, 0);
                            os.output(ae).stream().close();
                        }
                    } catch (final IOException ex) {
                        if (null != warning || !(ex instanceof InputException))
                            throw handler.fail(new FsSyncException(getModel(), ex));
                        warning = ex;
                        handler.warn(new FsSyncWarningException(getModel(), ex));
                    }
                }
            }
        }
    }

    /**
     * Discards the file system, closes the input archive and finally the
     * output archive.
     * Note that this order is critical: The parent file system controller is
     * expected to replace the entry for the target archive file with the
     * output archive when it gets closed, so this must be done last.
     * Using a finally block ensures that this is done even in the unlikely
     * event of an exception when closing the input archive.
     * Note that in this case closing the output archive is likely to fail and
     * override the IOException thrown by this method, too.
     *
     * @param handler the strategy for assembling sync exceptions.
     */
    private void close(
            final BitField<FsSyncOption> options,
            final FsSyncExceptionBuilder handler) {
        // HC SUNT DRACONES!
        final InputArchive<E> ia = inputArchive;
        if (null != ia) {
            try {
                ia.close();
            } catch (final ControlFlowException ex) {
                assert ex instanceof NeedsLockRetryException : ex;
                throw ex;
            } catch (final IOException ex) {
                handler.warn(new FsSyncWarningException(getModel(), ex));
            }
            setInputArchive(null);
        }
        final OutputArchive<E> oa = outputArchive;
        if (null != oa) {
            try {
                oa.close();
            } catch (final ControlFlowException ex) {
                assert ex instanceof NeedsLockRetryException : ex;
                throw ex;
            } catch (final IOException ex) {
                handler.warn(new FsSyncException(getModel(), ex));
            }
            setOutputArchive(null);
        }
        setFileSystem(null);
        // TODO: Remove a condition and clear a flag in the model
        // instead.
        if (options.get(ABORT_CHANGES) || options.get(CLEAR_CACHE))
            setTouched(false);
    }

    /**
     * A dummy input archive to substitute for {@code null} when copying.
     * 
     * @param <E> The type of the entries.
     */
    private static final class DummyInputService<E extends Entry>
    implements InputService<E> {
        @Override
        public int size() {
            return 0;
        }

        @Override
        public Iterator<E> iterator() {
            return Collections.<E>emptyList().iterator();
        }

        @Override
        public @CheckForNull E entry(String name) {
            return null;
        }

        @Override
        public InputSocket<E> input(String name) {
            throw new AssertionError();
        }

        @Override
        @DischargesObligation
        public void close() throws IOException {
            throw new AssertionError();
        }
    } // DummyInputService

    private static final class InputArchive<E extends FsArchiveEntry>
    extends LockInputService<E> {
        final InputService<E> archive;

        InputArchive(final @WillCloseWhenClosed InputService<E> input) {
            super(new DisconnectingInputService<>(input));
            this.archive = input;
        }

        boolean isClosed() {
            return getClutch().isClosed();
        }

        DisconnectingInputService<E> getClutch() {
            return (DisconnectingInputService<E>) container;
        }

        /**
         * Publishes the product of the archive driver this input archive is
         * decorating.
         */
        InputService<E> getDriverProduct() {
            assert !isClosed();
            return archive;
        }
    } // InputArchive

    private static final class OutputArchive<E extends FsArchiveEntry>
    extends LockOutputService<E> {
        OutputArchive(final @WillCloseWhenClosed OutputService<E> output) {
            super(new DisconnectingOutputService<>(output));
        }

        boolean isClosed() {
            return getClutch().isClosed();
        }

        DisconnectingOutputService<E> getClutch() {
            return (DisconnectingOutputService<E>) container;
        }
    } // OutputArchive

    @Override
    void checkSync(
            final BitField<FsAccessOption> options,
            final FsEntryName name,
            final @CheckForNull Access intention)
    throws NeedsSyncException {
        // HC SUNT DRACONES!

        // If no file system exists, then pass the test.
        final ArchiveFileSystem<E> fs = getFileSystem();
        if (null == fs)
            return;

        // If GROWing and the driver supports the respective access method,
        // then pass the test.
        if (options.get(GROW)) {
            if (null == intention) {
                if (driver.getRedundantMetaDataSupport())
                    return;
            } else if (WRITE == intention) {
                if (driver.getRedundantContentSupport()) {
                    getOutputArchive();
                    return;
                }
            }
        }

        // If the file system does not contain an entry with the given name,
        // then pass the test.
        final FsCovariantEntry<E> fse = fs.stat(options, name);
        if (null == fse)
            return;

        // If the entry name addresses the file system root, then pass the test
        // because the root entry cannot get input or output anyway.
        if (name.isRoot())
            return;

        String aen; // archive entry name

        // Check if the entry is already written to the output archive.
        {
            final OutputArchive<E> oa = getOutputArchive();
            if (null != oa) {
                aen = fse.getEntry().getName();
                if (null != oa.entry(aen))
                    throw NeedsSyncException.get();
            } else {
                aen = null;
            }
        }

        // If our intention is not reading the entry then pass the test.
        if (READ != intention)
            return;

        // Check if the entry is present in the input archive.
        final E iae; // input archive entry
        {
            final InputArchive<E> ia = getInputArchive();
            if (null != ia) {
                if (null == aen)
                    aen = fse.getEntry().getName();
                iae = ia.entry(aen);
            } else {
                iae = null;
            }
        }
        if (null == iae)
            throw NeedsSyncException.get();
    }
}
