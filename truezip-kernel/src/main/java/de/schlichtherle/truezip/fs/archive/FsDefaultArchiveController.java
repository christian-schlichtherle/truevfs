/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import de.schlichtherle.truezip.io.InputException;
import static de.schlichtherle.truezip.io.Paths.isRoot;
import de.schlichtherle.truezip.socket.*;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.Icon;
import net.jcip.annotations.NotThreadSafe;

/**
 * This archive controller manages I/O to the entry which represents the target
 * archive file in its parent file system and resolves archive entry collisions,
 * for example by performing a full update of the target archive file.
 *
 * @param   <E> The type of the archive entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
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

    private final FsArchiveFileSystemTouchListener<E> touchListener
            = new TouchListener();

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
        return true;
    }

    private @Nullable InputArchive<E> getInputArchive() {
        return inputArchive;
    }

    private void setInputArchive(final @CheckForNull InputArchive<E> inputArchive) {
        this.inputArchive = inputArchive;
        if (null != inputArchive)
            setTouched(true);
    }

    private @Nullable OutputArchive<E> getOutputArchive() {
        return outputArchive;
    }

    private void setOutputArchive(final @CheckForNull OutputArchive<E> outputArchive) {
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
        // HC SUNT DRACONES!
        try {
            // readOnly must be set first because the parent archive controller
            // could be a FileController and on Windows this property changes
            // to TRUE once a file is opened for reading!
            final boolean readOnly = !parent.isWritable(name);
            final InputSocket<?> socket = driver.getInputSocket(
                    parent, name, MOUNT_INPUT_OPTIONS);
            final InputArchive<E> ia = new InputArchive<E>(
                    driver.newInputShop(getModel(), socket));
            setInputArchive(ia);
            setFileSystem(newPopulatedFileSystem(driver,
                    ia.getDriverProduct(),
                    socket.getLocalTarget(),
                    readOnly));
        } catch (FsControllerException ex) {
            assert !(ex instanceof FsFalsePositiveException);
            throw ex;
        } catch (final IOException ex) {
            if (!autoCreate) {
                final FsEntry parentEntry;
                try {
                    parentEntry = parent.getEntry(name);
                } catch (FsControllerException ex2) {
                    assert !(ex2 instanceof FsFalsePositiveException);
                    throw ex2;
                } catch (final IOException ex2) {
                    //ex2.initCause(ex);
                    throw new FsFalsePositiveException(ex2);
                }
                if (null != parentEntry && !parentEntry.isType(SPECIAL))
                    throw new FsCacheableFalsePositiveException(ex);
                throw new FsFalsePositiveException(ex);
            }
            if (null != parent.getEntry(name))
                throw new FsCacheableFalsePositiveException(ex);
            // The entry does NOT exist in the parent archive
            // file, but we may create it automatically.
            // This may fail if the container file is an RAES encrypted ZIP
            // file and the user cancels password prompting.
            makeOutput();
            setFileSystem(newEmptyFileSystem(driver));
        }
        getFileSystem().addFsArchiveFileSystemTouchListener(touchListener);
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
    private OutputArchive<E> makeOutput() throws IOException {
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
        return getInputArchive().getInputSocket(name);
    }

    @Override
    OutputSocket<? extends E> getOutputSocket(final E entry) {
        class Output extends DelegatingOutputSocket<E> {
            @Override
            protected OutputSocket<? extends E> getDelegate()
            throws IOException {
                return makeOutput().getOutputSocket(entry);
            }
        } // Output

        return new Output();
    }

    @Override
    void checkAccess(   final FsEntryName name,
                        final @CheckForNull Access intention)
    throws FsNeedsSyncException {
        // HC SUNT DRACONES!
        final FsArchiveFileSystem<E> f;
        final FsCovariantEntry<E> ce;
        if (null == (f = getFileSystem()) || null == (ce = f.getEntry(name)))
            return;
        Boolean grow = null;
        String aen; // archive entry name
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
                    throw new FsNeedsSyncException();
            }
        } else {
            aen = null;
            oae = null;
        }
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
        if (READ == intention && (null == iae || iae != oae && oae != null))
            throw new FsNeedsSyncException();
    }

    @Override
    public <X extends IOException> void
    sync(   final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        try {
            commenceSync();
        } finally {
            try {
                if (!options.get(ABORT_CHANGES))
                    performSync(handler);
            } finally {
                try {
                    commitSync(handler);
                } finally {
                    assert null == getFileSystem();
                    assert null == getInputArchive();
                    assert null == getOutputArchive();
                    // TODO: Remove a condition and clear a flag in the model
                    // instead.
                    if (options.get(ABORT_CHANGES) || options.get(CLEAR_CACHE))
                        setTouched(false);
                }
            }
        }
    }

    private void commenceSync() {
        try {
            final InputArchive<E> ia = getInputArchive();
            if (null != ia)
                ia.disconnect();
        } finally {
            final OutputArchive<E> oa = getOutputArchive();
            if (null != oa)
                oa.disconnect();
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
    performSync(final ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        class FilterExceptionHandler
        implements ExceptionHandler<IOException, X> {
            IOException warning;

            @Override
            public X fail(final IOException cause) {
                assert false : "should not get used by copy()";
                return handler.fail(new FsSyncException(getModel(), cause));
            }

            @Override
            public void warn(final IOException cause) throws X {
                assert null != cause;
                if (null != warning || !(cause instanceof InputException))
                    throw handler.fail(new FsSyncException(getModel(), cause));
                warning = cause;
                handler.warn(new FsSyncWarningException(getModel(), cause));
            }
        } // FilterExceptionHandler

        final OutputArchive<E> oa = getOutputArchive();
        if (null == oa)
            return;
        final InputArchive<E> ia = getInputArchive();
        copy(   getFileSystem(),
                null == ia ? new DummyInputArchive<E>() : ia.getDriverProduct(),
                oa.getDriverProduct(),
                new FilterExceptionHandler());
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
                        if (!isRoot(ce.getName())) // never write the root directory!
                            if (UNKNOWN != ae.getTime(Access.WRITE)) // never write a ghost directory!
                                output.getOutputSocket(ae).newOutputStream().close();
                    } else if (null != input.getEntry(aen)) {
                        IOSocket.copy(  input.getInputSocket(aen),
                                        output.getOutputSocket(ae));
                    } else {
                        // The file system entry is a newly created non-directory
                        // entry which hasn't received any content yet.
                        // Write an empty file system entry now as a marker in
                        // order to recreate the file system entry when the file
                        // system gets remounted from the target archive file.
                        for (final Size size : ALL_SIZE_SET)
                            ae.setSize(size, UNKNOWN);
                        ae.setSize(DATA, 0);
                        output.getOutputSocket(ae).newOutputStream().close();
                    }
                } catch (IOException ex) {
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
    commitSync(final ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        setFileSystem(null);
        try {
            final InputArchive<E> ia = getInputArchive();
            setInputArchive(null);
            if (null != ia) {
                try {
                    ia.getDriverProduct().close();
                } catch (IOException ex) {
                    handler.warn(new FsSyncWarningException(getModel(), ex));
                }
            }
        } finally {
            final OutputArchive<E> oa = getOutputArchive();
            setOutputArchive(null);
            if (null != oa) {
                try {
                    oa.getDriverProduct().close();
                } catch (IOException ex) {
                    throw handler.fail(new FsSyncException(getModel(), ex));
                }
            }
        }
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

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public Iterator<E> iterator() {
            return (Iterator) Collections.emptyList().iterator();
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
    extends ConcurrentInputShop<E> {
        final InputShop<E> driverProduct;
        final Lock lock;

        InputArchive(final InputShop<E> input) {
            this(input, new ReentrantLock());
        }

        InputArchive(final InputShop<E> input, final Lock lock) {
            super(new DisconnectingInputShop<E>(input), lock);
            this.driverProduct = input;
            this.lock = lock;
        }

        boolean disconnect() {
            lock.lock();
            try {
                return ((DisconnectingInputShop<?>) delegate).disconnect();
            } finally {
                lock.unlock();
            }
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
    extends ConcurrentOutputShop<E> {
        final OutputShop<E> driverProduct;
        final Lock lock;

        OutputArchive(final OutputShop<E> output) {
            this(output, new ReentrantLock());
        }

        OutputArchive(final OutputShop<E> output, final Lock lock) {
            super(new DisconnectingOutputShop<E>(output), lock);
            this.driverProduct = output;
            this.lock = lock;
        }

        boolean disconnect() {
            lock.lock();
            try {
                return ((DisconnectingOutputShop<?>) delegate).disconnect();
            } finally {
                lock.unlock();
            }
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
            makeOutput();
            assert isTouched();
        }

        @Override
        public void afterTouch(FsArchiveFileSystemEvent<? extends E> event) {
            assert event.getSource() == getFileSystem();
            assert isTouched();
        }
    } // TouchListener
}
