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
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsEntry;
import de.schlichtherle.truezip.fs.FsEntryName;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import de.schlichtherle.truezip.fs.FsException;
import de.schlichtherle.truezip.fs.FsFalsePositiveException;
import de.schlichtherle.truezip.fs.FsInputOption;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import de.schlichtherle.truezip.fs.FsSyncException;
import de.schlichtherle.truezip.fs.FsSyncOption;
import static de.schlichtherle.truezip.fs.FsSyncOption.*;
import de.schlichtherle.truezip.fs.FsSyncWarningException;
import static de.schlichtherle.truezip.fs.archive.FsArchiveFileSystem.*;
import de.schlichtherle.truezip.io.InputBusyException;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.OutputBusyException;
import static de.schlichtherle.truezip.io.Paths.isRoot;
import de.schlichtherle.truezip.socket.ConcurrentInputShop;
import de.schlichtherle.truezip.socket.ConcurrentOutputShop;
import de.schlichtherle.truezip.socket.DelegatingOutputSocket;
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
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
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
    private static final BitField<FsOutputOption>
            MAKE_OUTPUT_MASK = BitField.of(CACHE, CREATE_PARENTS, GROW);
    private static final BitField<FsSyncOption>
            AUTO_SYNC_OPTIONS = BitField.of(WAIT_CLOSE_INPUT,
                                            WAIT_CLOSE_OUTPUT);

    private final FsArchiveDriver<E> driver;
    private final FsController<?> parent;
    private final FsEntryName parentName;

    /**
     * An {@link Input} object used to mount the (virtual) archive file system
     * and read the entries from the archive file.
     */
    private @Nullable Input input;

    /**
     * The (possibly temporary) {@link Output} we are writing newly
     * created or modified entries to.
     */
    private @Nullable Output output;

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
            final FsContextModel model,
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

    private Input getInput() {
        return input;
    }

    private void setInput(final @CheckForNull Input input) {
        this.input = input;
        if (null != input)
            getModel().setTouched(true);
    }

    private Output getOutput() {
        return output;
    }

    private void setOutput(final @CheckForNull Output output) {
        this.output = output;
        if (null != output)
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
            setInput(new Input(driver.newInputShop(getModel(), socket)));
            setFileSystem(newPopulatedFileSystem(driver,
                    getInput().getDelegate(),
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
                    throw new FsPermanentFalsePositiveException(getModel(), ex);
                throw new FsFalsePositiveException(getModel(), ex);
            }
            if (null != parent.getEntry(parentName))
                throw new FsPermanentFalsePositiveException(getModel(), ex);
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
     * Ensures that {@link #output} is not {@code null}.
     * This method will use
     * <code>{@link #getContext()}.{@link FsOperationContext#getOutputOptions()}</code>
     * to obtain the output options to use for writing the entry in the parent
     * file system.
     * 
     * @throws IOException on any I/O error.
     * @return The output.
     */
    private Output makeOutput() throws IOException {
        Output output = getOutput();
        if (null != output)
            return output;
        final BitField<FsOutputOption> options = getContext()
                .getOutputOptions()
                .and(MAKE_OUTPUT_MASK)
                .set(CACHE);
        final OutputSocket<?> socket = driver.getOutputSocket(
                parent, parentName, options, null);
        final Input input = getInput();
        setOutput(output = new Output(driver.newOutputShop(getModel(), socket,
                     null != input ? input.getDelegate() : null)));
        return output;
    }

    @Override
    InputSocket<?> getInputSocket(final String name) {
        return getInput().getInputSocket(name);
    }

    @Override
    OutputSocket<?> getOutputSocket(final E entry) {
        class Output extends DelegatingOutputSocket<Entry> {
            OutputSocket<? extends Entry> delegate;

            @Override
            protected OutputSocket<? extends Entry> getDelegate()
            throws IOException {
                final OutputSocket<? extends Entry> delegate = this.delegate;
                return null != delegate
                        ? delegate
                        : (this.delegate = makeOutput().getOutputSocket(entry));
            }
        } // Output

        return new Output();
    }

    @Override
    public void unlink(FsEntryName name) throws IOException {
        super.unlink(name);
        if (name.isRoot())
            parent.unlink(parentName);
    }

    @Override
    boolean autoSync(   final FsEntryName name,
                        final @CheckForNull Access intention)
    throws FsSyncException, FsException {
        final FsArchiveFileSystem<E> f;
        final FsCovariantEntry<E> ce;
        if (null == (f = getFileSystem()) || null == (ce = f.getEntry(name)))
            return false;
        String aen = null;
        final Output o = getOutput();
        E oe = null;
        Boolean grow = null;
        if (null != o && null != (oe = o.getEntry(aen = ce.getEntry().getName())))
            if (!(grow = getContext().get(GROW)))
                return autoSync();
        final Input i = getInput();
        E ie = null;
        if (null != i && null != (ie = i.getEntry(null != aen ? aen : (aen = ce.getEntry().getName()))))
            if (FALSE.equals(grow) || null == grow && !getContext().get(GROW))
                return false;
        if (READ == intention && (null == ie || ie != oe && oe != null))
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
        assert !isTouched() || null != getOutput(); // file system touched => output archive
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
                assert null == getInput();
                assert null == getOutput();
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
        // Check output streams first, because FORCE_CLOSE_INPUT may be
        // set and FORCE_CLOSE_OUTPUT may be unset in which case we
        // don't even need to check open input streams if there are
        // some open output streams.
        if (getOutput() != null) {
            final int outStreams = getOutput().waitCloseOthers(
                    options.get(WAIT_CLOSE_OUTPUT) ? 0 : 50);
            if (outStreams > 0) {
                final String message =  "Number of open output streams: "
                                        + outStreams;
                if (!options.get(FORCE_CLOSE_OUTPUT))
                    throw handler.fail( new FsSyncException(getModel(),
                                            new OutputBusyException(message)));
                handler.warn(   new FsSyncWarningException(getModel(),
                                    new OutputBusyException(message)));
            }
        }
        if (getInput() != null) {
            final int inStreams = getInput().waitCloseOthers(
                    options.get(WAIT_CLOSE_INPUT) ? 0 : 50);
            if (inStreams > 0) {
                final String message =  "Number of open input streams: "
                                        + inStreams;
                if (!options.get(FORCE_CLOSE_INPUT))
                    throw handler.fail( new FsSyncException(getModel(),
                                            new InputBusyException(message)));
                handler.warn(   new FsSyncWarningException(getModel(),
                                    new InputBusyException(message)));
            }
        }
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
            public X fail(IOException cannotHappen) {
                throw new AssertionError(cannotHappen);
            }

            @Override
            public void warn(IOException cause) throws X {
                if (null == cause)
                    throw new NullPointerException();
                handler.warn(new FsSyncWarningException(getModel(), cause));
            }
        } // class FilterExceptionHandler

        final FilterExceptionHandler decoratorHandler = new FilterExceptionHandler();
        final Output output = getOutput();
        if (null != output)
            output.closeAll((ExceptionHandler<IOException, X>) decoratorHandler);
        final Input input = getInput();
        if (null != input)
            input.closeAll((ExceptionHandler<IOException, X>) decoratorHandler);
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
        assert isTouched();
        assert null != getOutput();

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
        } // class FilterExceptionHandler

        final Input input = getInput();
        copy(   getFileSystem(),
                null != input ? input.getDelegate() : new DummyInputService<E>(),
                getOutput().getDelegate(),
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
            final Input input = getInput();
            setInput(null);
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ex) {
                    handler.warn(new FsSyncWarningException(getModel(), ex));
                }
            }
        } finally {
            final Output output = getOutput();
            setOutput(null);
            if (output != null) {
                try {
                    output.close();
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

    /**
     * This inner class makes this archive controller instance strongly
     * reachable from any created input stream.
     * This is required by the memory management to ensure that for any
     * prospective archive file at most one archive controller object is in
     * use at any time.
     */
    private final class Input extends ConcurrentInputShop<E> {
        Input(InputShop<E> input) {
            super(input);
        }

        /** Exposes the product of the archive driver this input is wrapping. */
        InputShop<E> getDelegate() {
            return delegate;
        }
    } // class Input

    /**
     * This inner class makes this archive controller instance strongly
     * reachable from any created output stream.
     * This is required by the memory management to ensure that for any
     * prospective archive file at most one archive controller object is in
     * use at any time.
     */
    private final class Output extends ConcurrentOutputShop<E> {
        Output(OutputShop<E> output) {
            super(output);
        }

        /** Exposes the product of the archive driver this output is wrapping. */
        OutputShop<E> getDelegate() {
            return delegate;
        }
    } // class Output

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
