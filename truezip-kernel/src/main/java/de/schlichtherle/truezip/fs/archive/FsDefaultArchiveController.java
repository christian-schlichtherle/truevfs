/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.entry.Entry;
import static de.schlichtherle.truezip.entry.Entry.ALL_SIZE_SET;
import de.schlichtherle.truezip.entry.Entry.Access;
import static de.schlichtherle.truezip.entry.Entry.Access.READ;
import static de.schlichtherle.truezip.entry.Entry.Access.WRITE;
import de.schlichtherle.truezip.entry.Entry.Size;
import static de.schlichtherle.truezip.entry.Entry.Size.DATA;
import static de.schlichtherle.truezip.entry.Entry.Type.DIRECTORY;
import static de.schlichtherle.truezip.entry.Entry.Type.SPECIAL;
import static de.schlichtherle.truezip.entry.Entry.UNKNOWN;
import static de.schlichtherle.truezip.fs.FsOutputOption.CACHE;
import static de.schlichtherle.truezip.fs.FsOutputOption.GROW;
import static de.schlichtherle.truezip.fs.FsOutputOptions.OUTPUT_PREFERENCES_MASK;
import static de.schlichtherle.truezip.fs.FsSyncOption.ABORT_CHANGES;
import static de.schlichtherle.truezip.fs.FsSyncOption.CLEAR_CACHE;
import de.schlichtherle.truezip.fs.*;
import static de.schlichtherle.truezip.fs.archive.FsArchiveFileSystem.newEmptyFileSystem;
import static de.schlichtherle.truezip.fs.archive.FsArchiveFileSystem.newPopulatedFileSystem;
import de.schlichtherle.truezip.io.InputClosedException;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.OutputClosedException;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.*;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import javax.swing.Icon;

/**
 * Manages I/O to the entry which represents the target archive file in its
 * parent file system and resolves archive entry collisions by performing a
 * full update of the target archive file.
 *
 * @param  <E> the type of the archive entries.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class FsDefaultArchiveController<E extends FsArchiveEntry>
extends FsFileSystemArchiveController<E> {

    private static final BitField<FsInputOption>
            MOUNT_INPUT_OPTIONS = BitField.of(FsInputOption.CACHE);

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
    FsDefaultArchiveController(
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
        final InputArchive<E> ia = getInputArchive();
        final OutputArchive<E> oa = getOutputArchive();
        assert null == ia || null != fs : "null != ia => null != fs";
        assert null == oa || null != fs : "null != oa => null != fs";
        assert null == fs || null != ia || null != oa : "null != fs => null != ia || null != oa";
        // This is effectively the same than the last three assertions, but is
        // harder to trace in the field on failure.
        //assert null != fs == (null != ia || null != oa);
        return true;
    }

    private @Nullable InputArchive<E> getInputArchive() {
        return inputArchive;
    }

    private void setInputArchive(final @CheckForNull InputArchive<E> inputArchive) {
        assert null == inputArchive || null == this.inputArchive;
        this.inputArchive = inputArchive;
        if (null != inputArchive)
            setTouched(true);
    }

    private @Nullable OutputArchive<E> getOutputArchive() {
        return outputArchive;
    }

    private void setOutputArchive(final @CheckForNull OutputArchive<E> outputArchive) {
        assert null == outputArchive || null == this.outputArchive;
        this.outputArchive = outputArchive;
        if (null != outputArchive)
            setTouched(true);
    }

    @Override
    public FsController<?> getParent() {
        return parent;
    }

    @Override
    @Deprecated
    public Icon getOpenIcon() throws IOException {
        autoMount(); // detect false positives!
        return driver.getOpenIcon(getModel());
    }

    @Override
    @Deprecated
    public Icon getClosedIcon() throws IOException {
        autoMount(); // detect false positives!
        return driver.getClosedIcon(getModel());
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
        FsArchiveFileSystem<E> fileSystem;
        try {
            // readOnly must be set first because the parent archive controller
            // could be a FileController and on Windows this property changes
            // to TRUE once a file is opened for reading!
            final boolean readOnly = !parent.isWritable(name);
            final InputSocket<?> socket = driver.getInputSocket(
                    parent, name, MOUNT_INPUT_OPTIONS);
            final Entry rootTemplate = socket.getLocalTarget();
            final InputArchive<E> archive = new InputArchive<E>(
                    driver.newInputShop(getModel(), socket));
            setInputArchive(archive);
            fileSystem = newPopulatedFileSystem(
                    driver, archive.getDriverProduct(), rootTemplate, readOnly);
        } catch (FsControllerException nonLocalFlowControl) {
            assert !(nonLocalFlowControl instanceof FsFalsePositiveException);
            throw nonLocalFlowControl;
        } catch (final IOException ex) {
            final FsEntry parentEntry;
            try {
                parentEntry = parent.getEntry(name);
            } catch (FsControllerException nonLocalFlowControl) {
                assert !(nonLocalFlowControl instanceof FsFalsePositiveException);
                throw nonLocalFlowControl;
            } catch (final IOException inaccessibleEntry) {
                throw autoCreate
                        ? inaccessibleEntry
                        : new FsFalsePositiveException(inaccessibleEntry);
            }
            if (null == parentEntry && autoCreate) {
                // This may fail if the container file is an RAES encrypted ZIP
                // file and the user cancels password prompting.
                makeOutputArchive();
                fileSystem = newEmptyFileSystem(driver);
            } else {
                throw null != parentEntry && !parentEntry.isType(SPECIAL)
                        ? new FsPersistentFalsePositiveException(ex)
                        : new FsFalsePositiveException(ex);
            }
        }
        fileSystem.addFsArchiveFileSystemTouchListener(touchListener);
        setFileSystem(fileSystem);
    }

    /**
     * Ensures that {@link #getOutputArchive} does not return {@code null}.
     * This method will use
     * <code>{@link #getContext()}.{@link FsOperationContext#getOutputOptions()}</code>
     * to obtain the output options to use for writing the entry in the parent
     * file system.
     * 
     * @return The output archive.
     */
    private OutputArchive<E> makeOutputArchive() throws IOException {
        OutputArchive<E> oa = getOutputArchive();
        if (null != oa)
            return oa;
        final BitField<FsOutputOption> options = getContext()
                .getOutputOptions()
                .and(OUTPUT_PREFERENCES_MASK)
                .set(CACHE);
        final OutputSocket<?> socket = driver.getOutputSocket(
                parent, name, options, null);
        final InputArchive<E> ia = getInputArchive();
        oa = new OutputArchive<E>(driver.newOutputShop(
                getModel(), socket, null == ia ? null : ia.getDriverProduct()));
        setOutputArchive(oa);
        return oa;
    }

    @Override
    InputSocket<? extends E> getInputSocket(final String name) {
        class Input extends ProxyInputSocket<E> {
            @Override
            protected InputSocket<? extends E> getProxiedDelegate()
            throws IOException {
                return getInputArchive().getInputSocket(name);
            }

            @Override
            public E getLocalTarget() throws IOException {
                try {
                    return super.getLocalTarget();
                } catch (InputClosedException discard) {
                    throw map(discard);
                }
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                try {
                    return super.newReadOnlyFile();
                } catch (InputClosedException discard) {
                    throw map(discard);
                }
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel() throws IOException {
                try {
                    return super.newSeekableByteChannel();
                } catch (InputClosedException discard) {
                    throw map(discard);
                }
            }

            @Override
            public InputStream newInputStream() throws IOException {
                try {
                    return super.newInputStream();
                } catch (InputClosedException discard) {
                    throw map(discard);
                }
            }

            private IOException map(IOException discard) {
                // DON'T try to sync() locally - this could make the state of
                // clients inconsistent if they have cached other artifacts of
                // this controller, e.g. the archive file system.
                return FsNeedsSyncException.get();
            }
        } // Input

        return new Input();
    }

    @Override
    OutputSocket<? extends E> getOutputSocket(final E entry) {
        class Output extends ProxyOutputSocket<E> {
            @Override
            protected OutputSocket<? extends E> getProxiedDelegate()
            throws IOException {
                return makeOutputArchive().getOutputSocket(entry);
            }

            @Override
            public E getLocalTarget() throws IOException {
                try {
                    return super.getLocalTarget();
                } catch (OutputClosedException discard) {
                    throw map(discard);
                }
            }

            @Override
            public SeekableByteChannel newSeekableByteChannel() throws IOException {
                try {
                    return super.newSeekableByteChannel();
                } catch (OutputClosedException discard) {
                    throw map(discard);
                }
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                try {
                    return super.newOutputStream();
                } catch (OutputClosedException discard) {
                    throw map(discard);
                }
            }

            private IOException map(IOException discard) {
                // DON'T try to sync() locally - this could make the state of
                // clients inconsistent if they have cached other artifacts of
                // this controller, e.g. the archive file system.
                return FsNeedsSyncException.get();
            }
        } // Output

        return new Output();
    }

    @Override
    void checkAccess(   final FsEntryName name,
                        final @CheckForNull Access intention)
    throws FsNeedsSyncException {
        try {
            checkAccess0(name, intention);
        } finally {
            assert invariants();
        }
    }

    void checkAccess0(  final FsEntryName name,
                        final @CheckForNull Access intention)
    throws FsNeedsSyncException {
        // HC SUNT DRACONES!

        // If the named entry is a file system root, then always pass this test
        // because a root entry is not accessible to the client application
        // anyway so file system synchronization would be redundant at best.
        if (name.isRoot())
            return;

        // Check if there exists a file system with the named entry.
        final FsArchiveFileSystem<E> f;
        final FsCovariantEntry<E> ce;
        if (null == (f = getFileSystem()) || null == (ce = f.getEntry(name)))
            return;

        Boolean grow = null;
        String aen; // archive entry name

        // Check if the entry is already written to the output archive.
        final OutputArchive<E> oa = getOutputArchive();
        final E oae; // output archive entry
        if (null != oa) {
            aen = ce.getEntry().getName();
            oae = oa.getEntry(aen);
            if (null != oae) {
                grow = getContext().get(GROW);
                if (!grow
                        || null == intention && !driver.getRedundantMetaDataSupport()
                        || WRITE == intention && !driver.getRedundantContentSupport())
                    throw FsNeedsSyncException.get();
            }
        } else {
            aen = null;
            oae = null;
        }

        // Check if the entry is present in the input archive.
        final InputArchive<E> ia = getInputArchive();
        final E iae; // input archive entry
        if (null != ia) {
            if (null == aen)
                aen = ce.getEntry().getName();
            iae = ia.getEntry(aen);
            if (null != iae) {
                if (null == grow)
                    grow = getContext().get(GROW);
                if (!grow) {
                    assert null == oae;
                    return;
                }
            }
        } else {
            iae = null;
        }

        // Check for reading an entry which either doesn't yet exist in the
        // input archive or has been obsoleted by a new version in the output
        // archive.
        if (READ == intention && (null == iae || null != oae && iae != oae))
            throw FsNeedsSyncException.get();
    }

    @Override
    public <X extends IOException> void
    sync(   final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws IOException {
        try {
            sync0(options, handler);
        } finally {
            assert invariants();
        }
    }

    private <X extends IOException> void
    sync0(  final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws IOException {
        try {
            copy(options, handler);
        } finally {
            commit(options, handler);
        }
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
    copy(   final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
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
        } // FilterExceptionHandler

        if (options.get(ABORT_CHANGES))
            return;
        final OutputArchive<E> oa = getOutputArchive();
        if (null == oa)
            return;
        final InputArchive<E> ia = getInputArchive();
        copy(   getFileSystem(),
                null != ia ? ia.getDriverProduct() : new DummyInputArchive<E>(),
                oa.getDriverProduct(),
                new Filter());
    }

    private static <E extends FsArchiveEntry, X extends IOException> void
    copy(   final FsArchiveFileSystem<E> fileSystem,
            final InputService<E> input,
            final OutputService<E> output,
            final ExceptionHandler<? super IOException, X> handler)
    throws X {
        for (final FsCovariantEntry<E> ce : fileSystem) {
            for (final E ae : ce.getEntries()) {
                final String aen = ae.getName();
                if (null != output.getEntry(aen))
                    continue; // we have already written this entry
                try {
                    if (DIRECTORY == ae.getType()) {
                        if (!ce.isRoot()) // never write the root directory!
                            if (UNKNOWN != ae.getTime(Access.WRITE)) // never write a ghost directory!
                                output.getOutputSocket(ae).newOutputStream().close();
                    } else if (null != input.getEntry(aen)) {
                        IOSocket.copy(  input.getInputSocket(aen),
                                        output.getOutputSocket(ae));
                    } else {
                        // The file system entry is a newly created
                        // non-directory entry which hasn't received any
                        // content yet, e.g. as a result of mknod()
                        // => write an empty file system entry.
                        for (final Size size : ALL_SIZE_SET)
                            ae.setSize(size, UNKNOWN);
                        ae.setSize(DATA, 0);
                        output.getOutputSocket(ae).newOutputStream().close();
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
    commit( final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws FsControllerException, X {
        final InputArchive<E> ia = getInputArchive();
        if (null != ia) {
            try {
                ia.close();
                setInputArchive(null);
            } catch (final FsControllerException ex) {
                throw ex;
            } catch (final IOException ex) {
                handler.warn(new FsSyncWarningException(getModel(), ex));
            }
        }
        final OutputArchive<E> oa = getOutputArchive();
        if (null != oa) {
            try {
                oa.close();
                setOutputArchive(null);
            } catch (final FsControllerException ex) {
                throw ex;
            } catch (final IOException ex) {
                throw handler.fail(new FsSyncException(getModel(), ex));
            }
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
    private static final class DummyInputArchive<E extends Entry>
    implements InputService<E> {
        @Override
        public int getSize() {
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
        public InputSocket<? extends E> getInputSocket(String name) {
            throw new UnsupportedOperationException();
        }
    } // DummyInputArchive

    private static final class InputArchive<E extends FsArchiveEntry>
    extends LockInputShop<E> {
        final InputShop<E> driverProduct;

        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        InputArchive(final @WillCloseWhenClosed InputShop<E> input) {
            super(new DisconnectingInputShop<E>(input), new ReentrantLock());
            this.driverProduct = input;
        }

        /**
         * Publishes the product of the archive driver this input archive is
         * decorating.
         */
        InputShop<E> getDriverProduct() {
            return driverProduct;
        }
    } // InputArchive

    private static final class OutputArchive<E extends FsArchiveEntry>
    extends LockOutputShop<E> {
        final OutputShop<E> driverProduct;

        @CreatesObligation
        @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
        OutputArchive(final @WillCloseWhenClosed OutputShop<E> output) {
            super(new DisconnectingOutputShop<E>(output), new ReentrantLock());
            this.driverProduct = output;
        }

        /**
         * Publishes the product of the archive driver this output archive is
         * decorating.
         */
        OutputShop<E> getDriverProduct() {
            return driverProduct;
        }
    } // OutputArchive

    /**
     * An archive file system listener which makes the output before it
     * touches the file system model.
     */
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
