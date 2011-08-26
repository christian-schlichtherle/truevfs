/*
 * Copyright (C) 2004-2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.entry.Entry;
import static de.schlichtherle.truezip.entry.Entry.*;
import static de.schlichtherle.truezip.entry.Entry.Access.*;
import static de.schlichtherle.truezip.entry.Entry.Type.*;
import de.schlichtherle.truezip.fs.FsConcurrentModel;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsEntry;
import de.schlichtherle.truezip.fs.FsEntryName;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import de.schlichtherle.truezip.fs.FsException;
import de.schlichtherle.truezip.fs.FsFalsePositiveException;
import de.schlichtherle.truezip.fs.FsInputOption;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import static de.schlichtherle.truezip.fs.FsOutputOptions.*;
import de.schlichtherle.truezip.fs.FsSyncException;
import de.schlichtherle.truezip.fs.FsSyncOption;
import static de.schlichtherle.truezip.fs.FsSyncOption.*;
import de.schlichtherle.truezip.fs.FsSyncWarningException;
import static de.schlichtherle.truezip.fs.archive.FsArchiveFileSystem.*;
import de.schlichtherle.truezip.io.DecoratingInputStream;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.OutputBusyException;
import static de.schlichtherle.truezip.io.Paths.isRoot;
import de.schlichtherle.truezip.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.SynchronizedInputShop;
import de.schlichtherle.truezip.socket.SynchronizedOutputShop;
import de.schlichtherle.truezip.socket.DecoratingInputShop;
import de.schlichtherle.truezip.socket.DecoratingInputSocket;
import de.schlichtherle.truezip.socket.DecoratingOutputShop;
import de.schlichtherle.truezip.socket.DecoratingOutputSocket;
import de.schlichtherle.truezip.socket.DelegatingOutputSocket;
import de.schlichtherle.truezip.socket.DisconnectingInputShop;
import de.schlichtherle.truezip.socket.DisconnectingOutputShop;
import de.schlichtherle.truezip.socket.IOSocket;
import de.schlichtherle.truezip.socket.InputService;
import de.schlichtherle.truezip.socket.InputShop;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputService;
import de.schlichtherle.truezip.socket.OutputShop;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import static java.lang.Boolean.*;
import java.util.Collections;
import java.util.Iterator;
import net.jcip.annotations.NotThreadSafe;
import javax.swing.Icon;

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
    private static final BitField<FsSyncOption>
            AUTO_SYNC_OPTIONS = BitField.of(WAIT_CLOSE_INPUT,
                                            WAIT_CLOSE_OUTPUT);

    private final FsArchiveDriver<E> driver;
    private final FsController<?> parent;
    private final FsEntryName parentName;

    private @CheckForNull FsResourceAccountant accountant;

    /**
     * An {@link InputArchive} object used to mount the (virtual) archive file system
     * and read the entries from the archive file.
     */
    private @CheckForNull InputArchive inputArchive;

    /**
     * The (possibly temporary) {@link OutputArchive} we are writing newly
     * created or modified entries to.
     */
    private @CheckForNull OutputArchive outputArchive;

    private final FsArchiveFileSystemTouchListener<E> touchListener
            = new TouchListener();

    /**
     * Constructs a new archive file system controller.
     * 
     * @param model the file system model.
     * @param parent the parent file system
     * @param driver the archive driver.
     */
    FsDefaultArchiveController(
            final FsConcurrentModel model,
            final FsController<?> parent,
            final FsArchiveDriver<E> driver) {
        super(model);
        if (null == driver)
            throw new NullPointerException();
        if (model.getParent() != parent.getModel())
            throw new IllegalArgumentException("Parent/member mismatch!");
        this.driver = driver;
        this.parent = parent;
        this.parentName = getModel().getMountPoint().getPath().resolve(ROOT)
                .getEntryName();
        assert invariants();
    }

    private boolean invariants() {
        assert null != driver;
        assert null != parent;
        assert null != parentName;
        return true;
    }

    private FsResourceAccountant getAccountant() {
        final FsResourceAccountant accountant = this.accountant;
        return null != accountant
                ? accountant
                : (this.accountant = new FsResourceAccountant(getModel().writeLock()));
    }

    private @CheckForNull InputArchive getInputArchive() {
        return inputArchive;
    }

    private void setInputArchive(final @CheckForNull InputArchive inputArchive) {
        this.inputArchive = inputArchive;
        if (null != inputArchive)
            getModel().setTouched(true);
    }

    private @CheckForNull OutputArchive getOutputArchive() {
        return outputArchive;
    }

    private void setOutputArchive(final @CheckForNull OutputArchive outputArchive) {
        this.outputArchive = outputArchive;
        if (null != outputArchive)
            getModel().setTouched(true);
    }

    @Override
    public FsController<?> getParent() {
        return parent;
    }

    @Override
    public Icon getOpenIcon() throws IOException {
        autoMount(); // detect false positives!
        return driver.getOpenIcon(getModel());
    }

    @Override
    public Icon getClosedIcon() throws IOException {
        autoMount(); // detect false positives!
        return driver.getClosedIcon(getModel());
    }

    @Override
    void mount(final boolean autoCreate) throws IOException {
        try {
            // readOnly must be set first because the parent archive controller
            // could be a FileController and on Windows this property changes
            // to TRUE once a file is opened for reading!
            final boolean readOnly = !parent.isWritable(parentName);
            final InputSocket<?> socket = driver.getInputSocket(
                    parent, parentName, MOUNT_INPUT_OPTIONS);
            final InputArchive ia = new InputArchive(
                    driver.newInputShop(getModel(), socket));
            setInputArchive(ia);
            setFileSystem(newPopulatedFileSystem(driver,
                    ia.getDriverProduct(),
                    socket.getLocalTarget(),
                    readOnly));
        } catch (FsException ex) {
            throw ex;
        } catch (IOException ex) {
            if (!autoCreate) {
                final FsEntry parentEntry;
                try {
                    parentEntry = parent.getEntry(parentName);
                } catch (FsException ex2) {
                    assert false;
                    throw ex2;
                } catch (IOException ex2) {
                    //ex2.initCause(ex);
                    throw new FsFalsePositiveException(getModel(), ex2);
                }
                if (null != parentEntry && !parentEntry.isType(SPECIAL))
                    throw new FsCacheableFalsePositiveException(getModel(), ex);
                throw new FsFalsePositiveException(getModel(), ex);
            }
            if (null != parent.getEntry(parentName))
                throw new FsCacheableFalsePositiveException(getModel(), ex);
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
     * @throws IOException on any I/O error.
     * @return The output.
     */
    private OutputArchive makeOutput() throws IOException {
        OutputArchive oa = getOutputArchive();
        if (null != oa)
            return oa;
        final BitField<FsOutputOption> options = getContext()
                .getOutputOptions()
                .and(OUTPUT_PREFERENCES_MASK)
                .set(CACHE);
        final OutputSocket<?> socket = driver.getOutputSocket(
                parent, parentName, options, null);
        final InputArchive ia = getInputArchive();
        oa = new OutputArchive(driver.newOutputShop(
                getModel(),
                socket,
                null == ia ? null : ia.getDriverProduct()));
        setOutputArchive(oa);
        return oa;
    }

    @Override
    InputSocket<?> getInputSocket(final String name) {
        final InputArchive ia = getInputArchive();
        assert null != ia; // make FindBugs happy
        return ia.getInputSocket(name);
    }

    @Override
    OutputSocket<?> getOutputSocket(final E entry) {
        class Output extends DelegatingOutputSocket<Entry> {
            @Override
            protected OutputSocket<? extends Entry> getDelegate()
            throws IOException {
                return makeOutput().getOutputSocket(entry);
            }
        } // Output

        return new Output();
    }

    @Override
    public void unlink(FsEntryName name, BitField<FsOutputOption> options)
    throws IOException {
        super.unlink(name, options);
        if (name.isRoot())
            parent.unlink(parentName, options);
    }

    @Override
    boolean autoSync(   final FsEntryName name,
                        final @CheckForNull Access intention)
    throws FsSyncException, FsException {
        final FsArchiveFileSystem<E> f;
        final FsCovariantEntry<E> ce;
        if (null == (f = getFileSystem()) || null == (ce = f.getEntry(name)))
            return false;
        // HC SUNT DRACONES!
        Boolean grow = null;
        String aen; // archive entry name
        final OutputArchive oa = getOutputArchive(); // output archive
        final E oae; // output archive entry
        if (null != oa) {
            aen = ce.getEntry().getName();
            oae = oa.getEntry(aen);
            if (null != oae)
                if (!(grow = getContext().get(GROW))
                        || null == intention && !driver.getRedundantMetaDataSupport()
                        || WRITE == intention && !driver.getRedundantContentSupport())
                    return autoSync();
        } else {
            aen = null;
            oae = null;
        }
        final InputArchive ia = getInputArchive(); // input archive
        final E iae; // input archive entry
        if (null != ia) {
            if (null == aen)
                aen = ce.getEntry().getName();
            iae = ia.getEntry(aen);
            if (null != iae)
                if (FALSE.equals(grow)
                        || null == grow && !getContext().get(GROW))
                    return false;
        } else {
            iae = null;
        }
        if (READ == intention && (null == iae || iae != oae && oae != null))
            return autoSync();
        return false;
    }

    private boolean autoSync() throws FsSyncException, FsException {
        getModel().assertWriteLockedByCurrentThread();
        sync(AUTO_SYNC_OPTIONS);
        return true;
    }

    @Override
    public <X extends IOException> void sync(
            final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        assert !isTouched() || null != getOutputArchive(); // file system touched => output archive
        assert getModel().isWriteLockedByCurrentThread();
        if (options.get(FORCE_CLOSE_OUTPUT) && !options.get(FORCE_CLOSE_INPUT))
            throw new IllegalArgumentException();
        awaitSync(options, handler);
        beginSync(handler);
        try {
            if (!options.get(ABORT_CHANGES) && isTouched())
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
                    getModel().setTouched(false);
            }
        }
    }

    /**
     * Waits for all entry input and entry output streams to close or forces
     * them to close, dependending on the {@code options}.
     *
     * @param  options a bit field of synchronization options.
     * @param  handler the exception handling strategy for consuming input
     *         {@code FsSyncException}s and/or assembling output
     *         {@code IOException}s.
     * @param  <X> The type of the {@code IOException} to throw at the
     *         discretion of the exception {@code handler}.
     * @throws IOException at the discretion of the exception {@code handler}
     *         upon the occurence of an {@link FsSyncException}.
     */
    private <X extends IOException> void awaitSync(
            final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        final FsResourceAccountant accountant = this.accountant;
        if (null == accountant)
            return;
        final boolean wait = options.get(WAIT_CLOSE_INPUT)
                || options.get(WAIT_CLOSE_OUTPUT);
        final int resources = accountant.waitStopAccounting(wait ? 0 : 50);
        if (0 >= resources)
            return;
        final IOException cause = new OutputBusyException("Number of open entry resources: " + resources);
        final boolean force = options.get(FORCE_CLOSE_INPUT)
                || options.get(FORCE_CLOSE_OUTPUT);
        if (!force)
            throw handler.fail(new FsSyncException(getModel(), cause));
        handler.warn(new FsSyncWarningException(getModel(), cause));
    }

    /**
     * Closes and disconnects all entry streams of the output and input
     * archive.
     *
     * @param  handler the exception handling strategy for consuming input
     *         {@code FsSyncException}s and/or assembling output
     *         {@code IOException}s.
     * @param  <X> The type of the {@code IOException} to throw at the
     *         discretion of the exception {@code handler}.
     * @throws IOException at the discretion of the exception {@code handler}
     *         upon the occurence of an {@link FsSyncException}.
     */
    private <X extends IOException> void beginSync(
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        class FilterExceptionHandler
        implements ExceptionHandler<IOException, X> {
            @Override
            public X fail(IOException shouldNotHappen) {
                throw new AssertionError(shouldNotHappen);
            }

            @Override
            public void warn(IOException cause) throws X {
                handler.warn(new FsSyncWarningException(getModel(), cause));
            }
        } // FilterExceptionHandler

        final FsResourceAccountant accountant = this.accountant;
        if (null != accountant)
            accountant.closeAll(new FilterExceptionHandler());
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
    private <X extends IOException> void performSync(
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        class FilterExceptionHandler
        implements ExceptionHandler<IOException, X> {
            IOException last;

            @Override
            public X fail(final IOException cause) {
                last = cause;
                return handler.fail(new FsSyncException(getModel(), cause));
            }

            @Override
            public void warn(final IOException cause) throws X {
                assert null != cause;
                final IOException old = last;
                last = cause;
                if (null != old || !(cause instanceof InputException))
                    throw handler.fail(new FsSyncException(getModel(), cause));
                handler.warn(new FsSyncWarningException(getModel(), cause));
            }
        } // FilterExceptionHandler

        assert isTouched();
        final OutputArchive oa = getOutputArchive();
        assert null != oa;
        final InputArchive ia = getInputArchive();
        copy(   getFileSystem(),
                null == ia ? new DummyInputService<E>() : ia.getDriverProduct(),
                oa.getDriverProduct(),
                (ExceptionHandler<IOException, X>) new FilterExceptionHandler());
    }

    private static <E extends FsArchiveEntry, X extends IOException> void copy(
            final FsArchiveFileSystem<E> fileSystem,
            final InputService<E> input,
            final OutputService<E> output,
            final ExceptionHandler<IOException, X> handler)
    throws X {
        for (final FsCovariantEntry<E> ce : fileSystem) {
            for (final E ae : ce.getEntries()) {
                final String aen = ae.getName();
                if (null != output.getEntry(aen))
                    continue; // we have already written this entry
                try {
                    if (DIRECTORY == ae.getType()) {
                        if (isRoot(ce.getName()))
                            continue; // never write the root directory, but preserve covariant root files
                        if (UNKNOWN == ae.getTime(Access.WRITE))
                            continue; // never write ghost directories
                        output.getOutputSocket(ae).newOutputStream().close();
                    } else if (null != input.getEntry(aen)) {
                        IOSocket.copy(  input.getInputSocket(aen),
                                        output.getOutputSocket(ae));
                    } else {
                        // The file system entry is a newly created non-directory
                        // entry which hasn't received any content yet.
                        // Write an empty file system entry now as a marker in
                        // order to recreate the file system entry when the file
                        // system gets remounted from the container archive file.
                        output.getOutputSocket(ae).newOutputStream().close();
                    }
                } catch (IOException ex) {
                    handler.warn(ex);
                }
            }
        }
    }

    /**
     * Discards the file system and closes the output and input archive.
     *
     * @param  handler the exception handling strategy for consuming input
     *         {@code FsSyncException}s and/or assembling output
     *         {@code IOException}s.
     * @param  <X> The type of the {@code IOException} to throw at the
     *         discretion of the exception {@code handler}.
     * @throws IOException at the discretion of the exception {@code handler}
     *         upon the occurence of an {@link FsSyncException}.
     */
    private <X extends IOException> void commitSync(
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        setFileSystem(null);
        try {
            final InputArchive ia = getInputArchive();
            setInputArchive(null);
            if (ia != null) {
                try {
                    ia.close();
                } catch (IOException ex) {
                    handler.warn(new FsSyncWarningException(getModel(), ex));
                }
            }
        } finally {
            final OutputArchive oa = getOutputArchive();
            setOutputArchive(null);
            if (oa != null) {
                try {
                    oa.close();
                } catch (IOException ex) {
                    throw handler.fail(new FsSyncException(getModel(), ex));
                }
            }
        }
    }

    private boolean isTouched() {
        final FsArchiveFileSystem<E> fileSystem = getFileSystem();
        return null != fileSystem && fileSystem.isTouched();
    }

    /**
     * A dummy input service to substitute for {@code null}.
     * 
     * @param <E> The type of the entries.
     */
    private static final class DummyInputService<E extends Entry>
    implements InputShop<E> {
        @Override
        public void close() throws IOException {
        }

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
        public E getEntry(String name) {
            return null;
        }

        @Override
        public InputSocket<? extends E> getInputSocket(String name) {
            if (null == name)
                throw new NullPointerException();
            throw new UnsupportedOperationException();
        }
    } // DummyInputService

    private final class InputArchive
    extends DecoratingInputShop<E, InputShop<E>> {
        final InputShop<E> driverProduct;

        InputArchive(final InputShop<E> driverProduct) {
            super(new DisconnectingInputShop<E>(
                    new SynchronizedInputShop<E>(driverProduct)));
            this.driverProduct = driverProduct;
        }

        /**
         * Exposes the product of the archive driver this input archive is
         * decorating.
         */
        InputShop<E> getDriverProduct() {
            return driverProduct;
        }

        @Override
        public InputSocket<? extends E> getInputSocket(final String name) {
            if (null == name)
                throw new NullPointerException();

            class Input extends DecoratingInputSocket<E> {
                Input() {
                    super(InputArchive.super.getInputSocket(name));
                }

                @Override
                public ReadOnlyFile newReadOnlyFile() throws IOException {
                    assert getModel().isWriteLockedByCurrentThread();

                    return new AccountedReadOnlyFile(
                            getBoundSocket().newReadOnlyFile());
                }

                @Override
                public InputStream newInputStream() throws IOException {
                    assert getModel().isWriteLockedByCurrentThread();

                    return new AccountedInputStream(
                            getBoundSocket().newInputStream());
                }
            } // Input

            return new Input();
        }
    } // InputArchive

    private final class OutputArchive
    extends DecoratingOutputShop<E, OutputShop<E>> {
        final OutputShop<E> driverProduct;

        OutputArchive(final OutputShop<E> driverProduct) {
            super(new DisconnectingOutputShop<E>(
                    new SynchronizedOutputShop<E>(driverProduct)));
            this.driverProduct = driverProduct;
        }

        /**
         * Exposes the product of the archive driver this output archive is
         * decorating.
         */
        OutputShop<E> getDriverProduct() {
            return driverProduct;
        }

        @Override
        public final OutputSocket<? extends E> getOutputSocket(final E entry) {
            if (null == entry)
                throw new NullPointerException();

            class Output extends DecoratingOutputSocket<E> {
                Output() {
                    super(OutputArchive.super.getOutputSocket(entry));
                }

                @Override
                public OutputStream newOutputStream() throws IOException {
                    assert getModel().isWriteLockedByCurrentThread();

                    return new AccountedOutputStream(
                            getBoundSocket().newOutputStream());
                }
            } // Output

            return new Output();
        }
    } // OutputArchive

    private final class AccountedReadOnlyFile extends DecoratingReadOnlyFile {
        @SuppressWarnings("LeakingThisInConstructor")
        AccountedReadOnlyFile(ReadOnlyFile rof) {
            super(rof);
            getAccountant().startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            assert getModel().isWriteLockedByCurrentThread();
            getAccountant().stopAccountingFor(this);
            delegate.close();
        }

        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            try {
                //getAccountant().stopAccountingFor(this); // superfluous - done by GC!
                delegate.close();
            } finally {
                super.finalize();
            }
        }
    } // AccountedReadOnlyFile

    private final class AccountedInputStream extends DecoratingInputStream {
        @SuppressWarnings("LeakingThisInConstructor")
        AccountedInputStream(InputStream in) {
            super(in);
            getAccountant().startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            assert getModel().isWriteLockedByCurrentThread();
            getAccountant().stopAccountingFor(this);
            delegate.close();
        }

        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            try {
                //getAccountant().stopAccountingFor(this); // superfluous - done by GC!
                delegate.close();
            } finally {
                super.finalize();
            }
        }
    } // AccountedInputStream

    private final class AccountedOutputStream extends DecoratingOutputStream {
        @SuppressWarnings("LeakingThisInConstructor")
        AccountedOutputStream(OutputStream out) {
            super(out);
            getAccountant().startAccountingFor(this);
        }

        @Override
        public void close() throws IOException {
            assert getModel().isWriteLockedByCurrentThread();
            getAccountant().stopAccountingFor(this);
            delegate.close();
        }

        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            try {
                //getAccountant().stopAccountingFor(this); // superfluous - done by GC!
                delegate.close();
            } finally {
                super.finalize();
            }
        }
    } // AccountedOutputStream

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
            assert getModel().isTouched();
        }

        @Override
        public void afterTouch(FsArchiveFileSystemEvent<? extends E> event) {
            assert event.getSource() == getFileSystem();
            //getModel().setTouched(true);
        }
    } // TouchListener
}
