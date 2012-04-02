/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import static de.schlichtherle.truezip.kernel.FsArchiveFileSystem.newEmptyFileSystem;
import static de.schlichtherle.truezip.kernel.FsArchiveFileSystem.newPopulatedFileSystem;
import de.truezip.kernel.*;
import de.truezip.kernel.addr.FsEntryName;
import static de.truezip.kernel.cio.Entry.ALL_SIZE_SET;
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
import de.truezip.kernel.option.AccessOption;
import static de.truezip.kernel.option.AccessOption.CACHE;
import static de.truezip.kernel.option.AccessOption.GROW;
import static de.truezip.kernel.option.AccessOptions.ACCESS_PREFERENCES_MASK;
import de.truezip.kernel.option.SyncOption;
import static de.truezip.kernel.option.SyncOption.ABORT_CHANGES;
import static de.truezip.kernel.option.SyncOption.CLEAR_CACHE;
import de.truezip.kernel.rof.ReadOnlyFile;
import de.truezip.kernel.util.BitField;
import de.truezip.kernel.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Collections;
import java.util.Iterator;
import java.util.TooManyListenersException;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Manages I/O to the entry which represents the target archive file in its
 * parent file system and resolves archive entry collisions by performing a
 * full update of the target archive file.
 *
 * @param  <E> the type of the archive entries.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class FsTargetArchiveController<E extends FsArchiveEntry>
extends FsFileSystemArchiveController<E> {

    private static final BitField<AccessOption>
            MOUNT_INPUT_OPTIONS = BitField.of(AccessOption.CACHE);

    private final FsArchiveDriver<E> driver;
    
    /** The parent file system controller. */
    private final FsController<?> parent;

    /** The entry name of the target archive file in the parent file system. */
    private final FsEntryName name;

    /**
     * An {@link InputArchive} object used to mount the (virtual) archive file system
     * and read the entries from the archive file.
     */
    private @CheckForNull InputArchive<E> inputArchive;

    /**
     * The (possibly temporary) {@link OutputArchive} we are writing newly
     * created or modified entries to.
     */
    private @CheckForNull OutputArchive<E> outputArchive;

    private final FsArchiveFileSystemTouchListener<E>
            touchListener = new TouchListener();

    /**
     * Constructs a new default archive file system controller.
     * 
     * @param model the file system model.
     * @param parent the parent file system
     * @param driver the archive driver.
     */
    FsTargetArchiveController(
            final FsLockModel model,
            final FsController<?> parent,
            final FsArchiveDriver<E> driver) {
        super(model);
        if (null == driver)
            throw new NullPointerException();
        if (model.getParent() != parent.getModel())
            throw new IllegalArgumentException("Parent/member mismatch!");
        this.driver = driver;
        this.parent = parent;
        this.name = getMountPoint().getPath().getEntryName();
        assert invariants();
    }

    private boolean invariants() {
        assert null != driver;
        assert null != parent;
        assert null != name;
        final FsArchiveFileSystem<E> fs = getFileSystem();
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

    @Nullable InputArchive<E> getCheckedInputArchive()
    throws FsNeedsSyncException {
        final InputArchive<E> ia = inputArchive;
        if (null != ia && ia.isClosed())
            throw FsNeedsSyncException.get(parent.getModel(), name, READ);
        return ia;
    }

    private void setInputArchive(final @CheckForNull InputArchive<E> ia) {
        assert null == ia || null == this.inputArchive;
        this.inputArchive = ia;
        if (null != ia)
            setTouched(true);
    }

    @Nullable OutputArchive<E> getCheckedOutputArchive()
    throws FsNeedsSyncException {
        final OutputArchive<E> oa = outputArchive;
        if (null != oa && oa.isClosed())
            throw FsNeedsSyncException.get(parent.getModel(), name, WRITE);
        return oa;
    }

    private void setOutputArchive(final @CheckForNull OutputArchive<E> oa) {
        assert null == oa || null == this.outputArchive;
        this.outputArchive = oa;
        if (null != oa)
            setTouched(true);
    }

    @Override
    public FsController<?> getParent() {
        return parent;
    }

    @Override
    void mount(final boolean autoCreate) throws IOException {
        try {
            mount0(autoCreate);
        } finally {
            assert invariants();
        }
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    private void mount0(final boolean autoCreate) throws IOException {
        // HC SUNT DRACONES!
        
        // Check parent file system entry.
        final FsEntry pe; // parent entry
        try {
            pe = parent.getEntry(name);
        } catch (final FsControllerException ex) {
            assert ex instanceof FsNeedsLockRetryException;
            throw ex;
        } catch (final IOException inaccessibleEntry) {
            throw autoCreate
                    ? inaccessibleEntry
                    : new FsFalsePositiveException(getModel(),
                        inaccessibleEntry);
        }

        // Obtain file system by creating or loading it from the parent entry.
        final FsArchiveFileSystem<E> fs;
        if (null == pe) {
            if (autoCreate) {
                // This may fail e.g. if the container file is an RAES
                // encrypted ZIP file and the user cancels password prompting.
                makeOutputArchive();
                fs = newEmptyFileSystem(driver);
            } else {
                throw new FsFalsePositiveException(getModel(),
                        new FsEntryNotFoundException(parent.getModel(),
                            name, "no such entry"));
            }
        } else {
            try {
                // readOnly must be set first because the parent archive controller
                // could be a FileController and on Windows this property changes
                // to TRUE once a file is opened for reading!
                final boolean ro = !parent.isWritable(name);
                final InputSocket<?> is = driver.getInputSocket(
                        parent, name, MOUNT_INPUT_OPTIONS);
                final InputArchive<E> ia = new InputArchive<>(
                        driver.newInputService(getModel(), is));
                fs = newPopulatedFileSystem(driver, ia.getArchive(), pe, ro);
                setInputArchive(ia);
            } catch (final FsControllerException ex) {
                assert ex instanceof FsNeedsLockRetryException;
                throw ex;
            } catch (final IOException ex) {
                throw pe.isType(SPECIAL)
                        ? new FsFalsePositiveException(getModel(), ex)
                        : new FsPersistentFalsePositiveException(getModel(), ex);
            }
        }

        // Register file system.
        try {
            fs.addFsArchiveFileSystemTouchListener(touchListener);
        } catch (final TooManyListenersException ex) {
            throw new AssertionError(ex);
        }
        setFileSystem(fs);
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
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION") // false positive
    OutputArchive<E> makeOutputArchive() throws IOException {
        OutputArchive<E> oa = getCheckedOutputArchive();
        if (null != oa)
            return oa;
        final BitField<AccessOption> options = getContext()
                .getOutputOptions()
                .and(ACCESS_PREFERENCES_MASK)
                .set(CACHE);
        final OutputSocket<?> os = driver.getOutputSocket(
                parent, name, options, null);
        final InputArchive<E> ia = getCheckedInputArchive();
        try {
            oa = new OutputArchive<>(driver.newOutputService(
                    getModel(), os, null == ia ? null : ia.getArchive()));
        } catch (final FsControllerException ex) {
            assert ex instanceof FsNeedsLockRetryException;
            throw ex;
        }
        setOutputArchive(oa);
        return oa;
    }

    @Override
    InputSocket<? extends E> getInputSocket(final String name) {
        class Input extends ClutchInputSocket<E> {
            @Override
            protected InputSocket<? extends E> getLazyDelegate()
            throws IOException {
                return getCheckedInputArchive().getInputSocket(name);
            }

            @Override
            public E getLocalTarget() throws IOException {
                try {
                    return super.getLocalTarget();
                } catch (InputClosedException ex) {
                    throw map(ex);
                }
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                try {
                    return super.newReadOnlyFile();
                } catch (InputClosedException ex) {
                    throw map(ex);
                }
            }

            @Override
            public SeekableByteChannel newChannel() throws IOException {
                try {
                    return super.newChannel();
                } catch (InputClosedException ex) {
                    throw map(ex);
                }
            }

            @Override
            public InputStream newStream() throws IOException {
                try {
                    return super.newStream();
                } catch (InputClosedException ex) {
                    throw map(ex);
                }
            }

            IOException map(IOException ex) {
                // DON'T try to sync() locally - this could make the state of
                // clients inconsistent if they have cached other artifacts of
                // this controller, e.g. the archive file system.
                return FsNeedsSyncException.get(getModel(), name, ex);
            }
        } // Input

        return new Input();
    }

    @Override
    OutputSocket<? extends E> getOutputSocket(final E entry) {
        class Output extends ClutchOutputSocket<E> {
            @Override
            protected OutputSocket<? extends E> getLazyDelegate()
            throws IOException {
                return makeOutputArchive().getOutputSocket(entry);
            }

            @Override
            public E getLocalTarget() throws IOException {
                try {
                    return super.getLocalTarget();
                } catch (OutputClosedException ex) {
                    throw map(ex);
                }
            }

            @Override
            public SeekableByteChannel newChannel() throws IOException {
                try {
                    return super.newChannel();
                } catch (OutputClosedException ex) {
                    throw map(ex);
                }
            }

            @Override
            public OutputStream newStream() throws IOException {
                try {
                    return super.newStream();
                } catch (OutputClosedException ex) {
                    throw map(ex);
                }
            }

            IOException map(IOException ex) {
                // DON'T try to sync() locally - this could make the state of
                // clients inconsistent if they have cached other artifacts of
                // this controller, e.g. the archive file system.
                return FsNeedsSyncException.get(getModel(), entry.getName(), ex);
            }
        } // Output

        return new Output();
    }

    @Override
    void checkSync(   final FsEntryName name,
                        final @CheckForNull Access intention)
    throws FsNeedsSyncException {
        // HC SUNT DRACONES!

        // If GROWing and the driver supports the respective access method,
        // then pass the test.
        if (getContext().get(GROW)) {
            if (null == intention) {
                if (driver.getRedundantMetaDataSupport())
                    return;
            } else if (WRITE == intention) {
                if (driver.getRedundantContentSupport()) {
                    getCheckedOutputArchive();
                    return;
                }
            }
        }

        // If no file system exists or does not contain an entry with the given
        // name, then pass the test.
        final FsCovariantEntry<E> fse; // file system entry
        {
            final FsArchiveFileSystem<E> fs;
            if (null == (fs = getFileSystem()) || null == (fse = fs.getEntry(name)))
                return;
        }

        // If the entry name addresses the file system root, then pass the test
        // because the root entry is not present in the input or output archive
        // anyway.
        if (name.isRoot())
            return;

        String aen; // archive entry name

        // Check if the entry is already written to the output archive.
        {
            final OutputArchive<E> oa = getCheckedOutputArchive();
            if (null != oa) {
                aen = fse.getEntry().getName();
                if (null != oa.getEntry(aen))
                    throw FsNeedsSyncException.get(getModel(), name, intention);
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
            final InputArchive<E> ia = getCheckedInputArchive();
            if (null != ia) {
                if (null == aen)
                    aen = fse.getEntry().getName();
                iae = ia.getEntry(aen);
            } else {
                iae = null;
            }
        }
        if (null == iae)
            throw FsNeedsSyncException.get(getModel(), name, READ);
    }

    @Override
    public <X extends IOException> void
    sync(   final BitField<SyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws FsControllerException, X {
        assert isWriteLockedByCurrentThread();
        try {
            sync0(options, handler);
        } finally {
            assert invariants();
        }
    }

    private <X extends IOException> void
    sync0(  final BitField<SyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws FsControllerException, X {
        if (!options.get(ABORT_CHANGES))
            copy(handler);
        close(handler);
        // TODO: Remove a condition and clear a flag in the model
        // instead.
        if (options.get(ABORT_CHANGES) || options.get(CLEAR_CACHE))
            setTouched(false);
    }

    /**
     * Synchronizes all entries in the (virtual) archive file system with the
     * (temporary) output archive file.
     *
     * @param  handler the exception handling strategy for consuming input
     *         {@code FsSyncException}s and/or assembling output
     *         {@code IOException}s.
     * @param  <X> The type of the {@code IOException} to throw at the
     *         discretion of the exception {@code handler}.
     * @throws IOException at the discretion of the exception {@code handler}
     *         upon the occurence of an {@link FsSyncException}.
     */
    private <X extends IOException> void
    copy(final ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        class Filter implements ExceptionHandler<IOException, X> {
            IOException warning;

            @Override
            public X fail(final IOException cause) {
                assert false : "should not get used by copy()";
                assert null != cause;
                assert !(cause instanceof FsControllerException);
                return handler.fail(new FsSyncException(getModel(), cause));
            }

            @Override
            public void warn(final IOException cause) throws X {
                assert null != cause;
                assert !(cause instanceof FsControllerException);
                if (null != warning || !(cause instanceof InputException))
                    throw handler.fail(new FsSyncException(getModel(), cause));
                warning = cause;
                handler.warn(new FsSyncWarningException(getModel(), cause));
            }
        } // Filter

        // Skip (In|Out)putArchive for better performance.
        // This is safe because the FsResourceController has already shut down
        // all concurrent access by closing the respective resources (streams,
        // channels etc).
        // The Disconnecting(In|Out)putService should not get skipped however:
        // If these would throw an (In|Out)putClosedException, then this would
        // be an artifact of a bug.
        final OutputService<E> os;
        {
            final OutputArchive<E> oa = outputArchive;
            if (null == oa || oa.isClosed())
                return;
            assert !oa.isClosed();
            os = oa.getClutch();
        }

        final InputService<E> is;
        {
            final InputArchive<E> ia = inputArchive;
            if (null != ia && ia.isClosed())
                return;
            assert null == ia || !ia.isClosed();
            is = null != ia  ? ia.getClutch() : new DummyInputService<E>();
        }

        copy(getFileSystem(), is, os, new Filter());
    }

    private static <E extends FsArchiveEntry, X extends IOException> void
    copy(   final FsArchiveFileSystem<E> fs,
            final InputService<E> is,
            final OutputService<E> os,
            final ExceptionHandler<? super IOException, X> handler)
    throws X {
        for (final FsCovariantEntry<E> fse : fs) {
            for (final E ae : fse.getEntries()) {
                final String aen = ae.getName();
                if (null != os.getEntry(aen))
                    continue; // entry has already been output
                try {
                    if (DIRECTORY == ae.getType()) {
                        if (!fse.isRoot()) // never output the root directory!
                            if (UNKNOWN != ae.getTime(Access.WRITE)) // never write a ghost directory!
                                os.getOutputSocket(ae).newStream().close();
                    } else if (null != is.getEntry(aen)) {
                        IOSocket.copy(  is.getInputSocket(aen),
                                        os.getOutputSocket(ae));
                    } else {
                        // The file system entry is a newly created
                        // non-directory entry which hasn't received any
                        // content yet, e.g. as a result of mknod()
                        // => output an empty file system entry.
                        for (final Size size : ALL_SIZE_SET)
                            ae.setSize(size, UNKNOWN);
                        ae.setSize(DATA, 0);
                        os.getOutputSocket(ae).newStream().close();
                    }
                } catch (final IOException ex) {
                    handler.warn(ex);
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
     * @param  handler the exception handling strategy for consuming input
     *         {@code FsSyncException}s and/or assembling output
     *         {@code IOException}s.
     * @param  <X> The type of the {@code IOException} to throw at the
     *         discretion of the exception {@code handler}.
     * @throws IOException at the discretion of the exception {@code handler}
     *         upon the occurence of an {@link FsSyncException}.
     */
    private <X extends IOException> void
    close(final ExceptionHandler<? super FsSyncException, X> handler)
    throws FsControllerException, X {
        // HC SUNT DRACONES!
        final InputArchive<E> ia = inputArchive;
        if (null != ia) {
            try {
                ia.close();
            } catch (final FsControllerException nonLocalFlowControl) {
                assert nonLocalFlowControl instanceof FsNeedsLockRetryException;
                throw nonLocalFlowControl;
            } catch (final IOException ex) {
                handler.warn(new FsSyncWarningException(getModel(), ex));
            }
            setInputArchive(null);
        }
        final OutputArchive<E> oa = outputArchive;
        if (null != oa) {
            try {
                oa.close();
            } catch (final FsControllerException nonLocalFlowControl) {
                assert nonLocalFlowControl instanceof FsNeedsLockRetryException;
                throw nonLocalFlowControl;
            } catch (final IOException ex) {
                throw handler.fail(new FsSyncException(getModel(), ex));
            }
            setOutputArchive(null);
        }
        setFileSystem(null);
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
        public @CheckForNull E getEntry(String name) {
            return null;
        }

        @Override
        public InputSocket<E> getInputSocket(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() throws IOException {
            throw new UnsupportedOperationException();
        }
    } // DummyInputService

    private static final class InputArchive<E extends FsArchiveEntry>
    extends LockInputService<E> {
        final InputService<E> archive;

        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
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
        InputService<E> getArchive() {
            assert !isClosed();
            return archive;
        }
    } // InputArchive

    private static final class OutputArchive<E extends FsArchiveEntry>
    extends LockOutputService<E> {
        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
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

    /** An archive file system listener which makes the output archive. */
    private final class TouchListener
    implements FsArchiveFileSystemTouchListener<E> {
        @Override
        public void beforeTouch(FsArchiveFileSystemEvent<? extends E> event)
        throws IOException {
            assert event.getSource() == getFileSystem();
            makeOutputArchive();
            assert isTouched();
        }

        @Override
        public void afterTouch(FsArchiveFileSystemEvent<? extends E> event) {
            assert event.getSource() == getFileSystem();
            assert isTouched();
        }
    } // TouchListener
}
