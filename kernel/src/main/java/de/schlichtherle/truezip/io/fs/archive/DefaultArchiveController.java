/*
 * Copyright (C) 2004-2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.fs.archive;

import de.schlichtherle.truezip.io.fs.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.fs.FsController;
import de.schlichtherle.truezip.io.fs.FSEntryName;
import de.schlichtherle.truezip.io.InputBusyException;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.OutputBusyException;
import de.schlichtherle.truezip.io.TabuFileException;
import de.schlichtherle.truezip.io.fs.concurrency.FSConcurrencyModel;
import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.fs.FSFalsePositiveException;
import de.schlichtherle.truezip.io.fs.FSException;
import de.schlichtherle.truezip.io.fs.FSSyncExceptionBuilder;
import de.schlichtherle.truezip.io.fs.FSSyncException;
import de.schlichtherle.truezip.io.fs.FSSyncOption;
import de.schlichtherle.truezip.io.fs.FSSyncWarningException;
import de.schlichtherle.truezip.io.socket.ConcurrentInputShop;
import de.schlichtherle.truezip.io.socket.ConcurrentOutputShop;
import de.schlichtherle.truezip.io.fs.FSInputOption;
import de.schlichtherle.truezip.io.socket.InputService;
import de.schlichtherle.truezip.io.socket.InputShop;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.IOSocket;
import de.schlichtherle.truezip.io.fs.FSOutputOption;
import de.schlichtherle.truezip.io.socket.OutputService;
import de.schlichtherle.truezip.io.socket.OutputShop;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import javax.swing.Icon;
import net.jcip.annotations.NotThreadSafe;

import static de.schlichtherle.truezip.io.fs.archive.ArchiveFileSystem.*;
import static de.schlichtherle.truezip.io.entry.Entry.Access.*;
import static de.schlichtherle.truezip.io.entry.Entry.Type.*;
import static de.schlichtherle.truezip.io.entry.Entry.*;
import static de.schlichtherle.truezip.io.fs.FSEntryName.*;
import static de.schlichtherle.truezip.io.fs.FSOutputOption.*;
import static de.schlichtherle.truezip.io.fs.FSSyncOption.*;
import static de.schlichtherle.truezip.io.Paths.isRoot;

/**
 * This archive controller implements the mounting/unmounting strategy
 * for the container archive file.
 *
 * @param   <E> The type of the archive entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public final class DefaultArchiveController<E extends ArchiveEntry>
extends FileSystemArchiveController<E> {

    private static final BitField<FSOutputOption> MOUNT_MASK
            = BitField.of(CREATE_PARENTS);

    private static final BitField<FSInputOption> MOUNT_INPUT_OPTIONS
            = BitField.of(FSInputOption.CACHE);

    private static final BitField<FSOutputOption> MAKE_OUTPUT_OPTIONS
            = BitField.noneOf(FSOutputOption.class);

    private static final BitField<FSSyncOption> SYNC_OPTIONS
            = BitField.of(WAIT_CLOSE_INPUT, WAIT_CLOSE_OUTPUT, CLEAR_CACHE);

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
    }

    /**
     * This member class makes this archive controller instance strongly
     * reachable from any created input stream.
     * This is required by the memory management to ensure that for any
     * prospective archive file at most one archive controller object is in
     * use at any time.
     */
    private final class Input extends ConcurrentInputShop<E> {
        Input(InputShop<E> input) {
            super(input);
        }

        /** Returns the product of the archive driver this input is wrapping. */
        InputShop<E> getDelegate() {
            return delegate;
        }
    }

    /**
     * This member class makes this archive controller instance strongly
     * reachable from any created output stream.
     * This is required by the memory management to ensure that for any
     * prospective archive file at most one archive controller object is in
     * use at any time.
     */
    private final class Output extends ConcurrentOutputShop<E> {
        Output(OutputShop<E> output) {
            super(output);
        }

        /** Returns the product of the archive driver this output is wrapping. */
        OutputShop<E> getDelegate() {
            return delegate;
        }
    }

    private final class TouchListener
    implements ArchiveFileSystemTouchListener<E> {
        @Override
        public void beforeTouch(ArchiveFileSystemEvent<? extends E> event)
        throws IOException {
            assert event.getSource() == getFileSystem();
            makeOutput(MAKE_OUTPUT_OPTIONS, getFileSystem().getEntry(ROOT));
        }

        @Override
        public void afterTouch(ArchiveFileSystemEvent<? extends E> event) {
            assert event.getSource() == getFileSystem();
            getModel().setTouched(true);
        }
    }

    private final ArchiveDriver<E> driver;
    private final FsController<?> parent;
    private final boolean useRootTemplate;

    /**
     * An {@link Input} object used to mount the (virtual) archive file system
     * and read the entries from the archive file.
     */
    private Input input;

    /**
     * The (possibly temporary) {@link Output} we are writing newly
     * created or modified entries to.
     */
    private Output output;

    private final ArchiveFileSystemTouchListener<E> touchListener
            = new TouchListener();

    public DefaultArchiveController(
            final FSConcurrencyModel model,
            final ArchiveDriver<E> driver,
            final FsController<?> parent,
            final boolean useRootTemplate) {
        super(model);
        if (null == driver)
            throw new NullPointerException();
        if (parent.getModel() != model.getParent())
            throw new IllegalArgumentException("Parent/member mismatch!");
        this.driver = driver;
        this.parent = parent;
        this.useRootTemplate = useRootTemplate;
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
    void mount(final boolean autoCreate, BitField<FSOutputOption> options)
    throws IOException {
        options = options.and(MOUNT_MASK);
        try {
            final FsController<?> parent = getParent();
            final FSEntryName parentName = getModel()
                    .getMountPoint()
                    .getPath()
                    .resolve(ROOT)
                    .getEntryName();
            // readOnly must be set first because the parent archive controller
            // could be a FileController and on Windows this property turns to
            // TRUE once a file is opened for reading!
            final boolean readOnly = !parent.isWritable(parentName);
            final InputSocket<?> socket = parent.getInputSocket(
                    parentName, MOUNT_INPUT_OPTIONS);
            input = new Input(driver.newInputShop(getModel(), socket));
            setFileSystem(newArchiveFileSystem(driver,
                    input.getDelegate(), socket.getLocalTarget(), readOnly));
        } catch (FSException ex) {
            throw ex;
        } catch (TabuFileException ex) {
            throw ex;
        } catch (IOException ex) {
            if (!autoCreate)
                throw new FSFalsePositiveException(getModel(), ex);
            // The entry does NOT exist in the parent archive
            // file, but we may create it automatically.
            final ArchiveFileSystem<E> fileSystem
                    = newArchiveFileSystem(driver);
            final ArchiveFileSystemEntry<E> root
                    = fileSystem.getEntry(ROOT);
            // This may fail if e.g. the container file is an RAES
            // encrypted ZIP file and the user cancels password
            // prompting.
            try {
                makeOutput(options, root);
            } catch (FSException ex2) {
                throw ex2;
            } catch (TabuFileException ex2) {
                throw ex2;
            } catch (IOException ex2) {
                throw new FSFalsePositiveException(getModel(), ex2);
            }
            setFileSystem(fileSystem);
            getModel().setTouched(true);
        }
        getFileSystem().addArchiveFileSystemTouchListener(touchListener);
    }

    void makeOutput(@NonNull final BitField<FSOutputOption> options,
                    @NonNull final Entry rootTemplate)
    throws IOException {
        if (null != output)
            return;
        final FSEntryName parentName = getModel()
                .getMountPoint()
                .getPath()
                .resolve(ROOT)
                .getEntryName();
        final OutputSocket<?> socket = getParent().getOutputSocket(
                parentName, options.set(FSOutputOption.CACHE),
                useRootTemplate ? rootTemplate : null);
        output = new Output(driver.newOutputShop(getModel(), socket,
                    null == input ? null : input.getDelegate()));
    }

    @Override
    InputSocket<?> getInputSocket(final String name)
    throws IOException {
        return input.getInputSocket(name);
    }

    @Override
    OutputSocket<?> getOutputSocket(final E entry)
    throws IOException {
        if (null == output)
            makeOutput(MAKE_OUTPUT_OPTIONS, getFileSystem().getEntry(ROOT));
        return output.getOutputSocket(entry);
    }

    @Override
    boolean autoSync(   @NonNull final FSEntryName name,
                        @CheckForNull final Access intention)
    throws FSSyncException, FSException {
        final ArchiveFileSystem<E> fileSystem;
        final ArchiveFileSystemEntry<E> entry;
        if (null == (fileSystem = getFileSystem())
                || null == (entry = fileSystem.getEntry(name)))
            return false;
        String n = null;
        if (null != output && null != output.getEntry(
                n = entry.getEntry().getName()))
            //if (READ == intention || !output.canAppend(entry.getEntry()))
                return sync();
        if (null != input && null != input.getEntry(
                null != n ? n : (n = entry.getEntry().getName())))
            return false;
        if (READ == intention)
            return sync();
        return false;
    }

    private boolean sync() throws FSSyncException, FSException {
        getModel().assertWriteLockedByCurrentThread();
        final ExceptionBuilder<IOException, FSSyncException> builder
                = new FSSyncExceptionBuilder();
        sync(SYNC_OPTIONS, builder);
        builder.check();
        return true;
    }

    @Override
    public <X extends IOException> void sync(
            @NonNull final BitField<FSSyncOption> options,
            @NonNull final ExceptionHandler<? super FSSyncException, X> handler)
    throws X, FSException {
        assert !isTouched() || null != output; // file system touched => output archive
        assert getModel().writeLock().isHeldByCurrentThread();

        if (options.get(FORCE_CLOSE_OUTPUT) && !options.get(FORCE_CLOSE_INPUT))
            throw new IllegalArgumentException();

        awaitSync(options, handler);
        commenceSync(handler);
        try {
            if (!options.get(ABORT_CHANGES) && isTouched())
                performSync(handler);
        } finally {
            try {
                commitSync(handler);
            } finally {
                assert null == getFileSystem();
                assert null == input;
                assert null == output;
                getModel().setTouched(false);
            }
        }
    }

    /**
     * Waits for all entry input and entry output streams to close or forces
     * them to close, dependending on the {@code options}.
     *
     * @param  options the output options.
     * @param  handler the exception handler.
     * @throws FSSyncException If any exceptional condition occurs
     *         throughout the processing of the container archive file.
     */
    private <X extends IOException> void awaitSync(
            @NonNull final BitField<FSSyncOption> options,
            @NonNull final ExceptionHandler<? super FSSyncException, X> handler)
    throws X {
        // Check output streams first, because FORCE_CLOSE_INPUT may be
        // set and FORCE_CLOSE_OUTPUT may be unset in which case we
        // don't even need to check open input streams if there are
        // some open output streams.
        if (output != null) {
            final int outStreams = output.waitCloseOthers(
                    options.get(WAIT_CLOSE_OUTPUT) ? 0 : 50);
            if (outStreams > 0) {
                final String message =  "Number of open output streams: "
                                        + outStreams;
                if (!options.get(FORCE_CLOSE_OUTPUT))
                    throw handler.fail( new FSSyncException(getModel(),
                                            new OutputBusyException(message)));
                handler.warn(   new FSSyncWarningException(getModel(),
                                    new OutputBusyException(message)));
            }
        }
        if (input != null) {
            final int inStreams = input.waitCloseOthers(
                    options.get(WAIT_CLOSE_INPUT) ? 0 : 50);
            if (inStreams > 0) {
                final String message =  "Number of open input streams: "
                                        + inStreams;
                if (!options.get(FORCE_CLOSE_INPUT))
                    throw handler.fail( new FSSyncException(getModel(),
                                            new InputBusyException(message)));
                handler.warn(   new FSSyncWarningException(getModel(),
                                    new InputBusyException(message)));
            }
        }
    }

    /**
     * Closes and disconnects all entry streams of the output and input
     * archive.
     *
     * @param  handler the exception handler.
     * @throws FSSyncException If any exceptional condition occurs
     *         throughout the processing of the container archive file.
     */
    private <X extends IOException> void commenceSync(
            @NonNull final ExceptionHandler<? super FSSyncException, X> handler)
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
                handler.warn(new FSSyncWarningException(getModel(), cause));
            }
        } // class FilterExceptionHandler

        final FilterExceptionHandler decoratorHandler = new FilterExceptionHandler();
        if (output != null)
            output.closeAll((ExceptionHandler<IOException, X>) decoratorHandler);
        if (input != null)
            input.closeAll((ExceptionHandler<IOException, X>) decoratorHandler);
    }

    /**
     * Synchronizes all entries in the (virtual) archive file system with the
     * (temporary) output archive file.
     *
     * @param  handler the exception handler.
     * @throws IOException If any exceptional condition occurs throughout the
     *         processing of the container archive file.
     */
    private <X extends IOException> void performSync(
            @NonNull final ExceptionHandler<? super FSSyncException, X> handler)
    throws X {
        assert isTouched();
        assert null != output;

        class FilterExceptionHandler
        implements ExceptionHandler<IOException, X> {
            IOException last;

            @Override
            public X fail(final IOException cause) {
                last = cause;
                return handler.fail(new FSSyncException(getModel(), cause));
            }

            @Override
            public void warn(final IOException cause) throws X {
                assert null != cause;
                final IOException old = last;
                last = cause;
                if (null != old || !(cause instanceof InputException))
                    throw handler.fail(new FSSyncException(getModel(), cause));
                handler.warn(new FSSyncWarningException(getModel(), cause));
            }
        } // class FilterExceptionHandler

        copy(   getFileSystem(),
                null != input ? input.getDelegate() : new DummyInputService<E>(),
                output.getDelegate(),
                (ExceptionHandler<IOException, X>) new FilterExceptionHandler());
    }

    private static <E extends ArchiveEntry, X extends IOException> void copy(
            @NonNull final ArchiveFileSystem<E> fileSystem,
            @NonNull final InputService<E> input,
            @NonNull final OutputService<E> output,
            @NonNull final ExceptionHandler<IOException, X> handler)
    throws X {
        for (final ArchiveFileSystemEntry<E> fse : fileSystem) {
            final E e = fse.getEntry();
            final String n = e.getName();
            if (null != output.getEntry(n))
                continue; // we have already written this entry
            try {
                if (DIRECTORY == fse.getType()) {
                    if (isRoot(fse.getName()))
                        continue; // never write the root directory
                    if (UNKNOWN == fse.getTime(Access.WRITE))
                        continue; // never write ghost directories
                    output.getOutputSocket(e).newOutputStream().close();
                } else if (null != input.getEntry(n)) {
                    IOSocket.copy(  input.getInputSocket(n),
                                    output.getOutputSocket(e));
                } else {
                    // The file system entry is a newly created non-directory
                    // entry which hasn't received any content yet.
                    // Write an empty file system entry now as a marker in
                    // order to recreate the file system entry when the file
                    // system gets remounted from the container archive file.
                    output.getOutputSocket(e).newOutputStream().close();
                }
            } catch (IOException ex) {
                handler.warn(ex);
            }
        }
    }

    /**
     * Discards the file system and closes the output and input archive.
     *
     * @param  handler the exception handler.
     * @throws FSSyncException If any exceptional condition occurs
     *         throughout the processing of the container archive file.
     */
    private <X extends IOException> void commitSync(
            @NonNull final ExceptionHandler<? super FSSyncException, X> handler)
    throws X {
        setFileSystem(null);

        try {
            final Input input = this.input;
            this.input = null;
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ex) {
                    handler.warn(new FSSyncWarningException(getModel(), ex));
                }
            }
        } finally {
            final Output output = this.output;
            this.output = null;
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ex) {
                    throw handler.fail(new FSSyncException(getModel(), ex));
                }
            }
        }
    }

    private boolean isTouched() {
        final ArchiveFileSystem<E> fileSystem = getFileSystem();
        return null != fileSystem && fileSystem.isTouched();
    }

    @Override
    public void unlink(FSEntryName name) throws IOException {
        super.unlink(name);
        if (name.isRoot())
            getParent().unlink(
                    getModel()
                        .getMountPoint()
                        .getPath()
                        .resolve(name)
                        .getEntryName());
    }
}
