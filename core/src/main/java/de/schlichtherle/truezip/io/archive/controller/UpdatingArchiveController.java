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
package de.schlichtherle.truezip.io.archive.controller;

import de.schlichtherle.truezip.io.filesystem.FileSystemController;
import de.schlichtherle.truezip.io.filesystem.FileSystemEntryName;
import de.schlichtherle.truezip.io.InputBusyException;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.OutputBusyException;
import de.schlichtherle.truezip.io.TabuFileException;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystemEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystemEvent;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystemTouchListener;
import de.schlichtherle.truezip.io.archive.model.ArchiveModel;
import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.filesystem.FalsePositiveException;
import de.schlichtherle.truezip.io.filesystem.FileSystemException;
import de.schlichtherle.truezip.io.filesystem.SyncExceptionBuilder;
import de.schlichtherle.truezip.io.filesystem.SyncException;
import de.schlichtherle.truezip.io.filesystem.SyncOption;
import de.schlichtherle.truezip.io.filesystem.SyncWarningException;
import de.schlichtherle.truezip.io.socket.ConcurrentInputShop;
import de.schlichtherle.truezip.io.socket.ConcurrentOutputShop;
import de.schlichtherle.truezip.io.socket.InputOption;
import de.schlichtherle.truezip.io.socket.InputService;
import de.schlichtherle.truezip.io.socket.InputShop;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.IOSocket;
import de.schlichtherle.truezip.io.socket.OutputOption;
import de.schlichtherle.truezip.io.socket.OutputService;
import de.schlichtherle.truezip.io.socket.OutputShop;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import de.schlichtherle.truezip.util.ExceptionHandler;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import javax.swing.Icon;
import net.jcip.annotations.NotThreadSafe;

import static de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem.*;
import static de.schlichtherle.truezip.io.entry.Entry.Access.*;
import static de.schlichtherle.truezip.io.entry.Entry.Type.*;
import static de.schlichtherle.truezip.io.entry.Entry.*;
import static de.schlichtherle.truezip.io.filesystem.FileSystemEntryName.*;
import static de.schlichtherle.truezip.io.filesystem.SyncOption.*;
import static de.schlichtherle.truezip.io.Paths.isRoot;

/**
 * This archive controller implements the mounting/unmounting strategy
 * by performing a full update of the container archive file.
 *
 * @param   <E> The type of the archive entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public final class UpdatingArchiveController<E extends ArchiveEntry>
extends FileSystemArchiveController<E> {

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
        InputShop<E> getDriverProduct() {
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
        OutputShop<E> getDriverProduct() {
            return delegate;
        }
    }

    private final class TouchListener
    implements ArchiveFileSystemTouchListener<ArchiveEntry> {
        @Override
        public void beforeTouch(ArchiveFileSystemEvent<?> event)
        throws IOException {
            assert null == event || event.getSource() == getFileSystem();
            makeOutput(BitField.noneOf(OutputOption.class));
        }

        @Override
        public void afterTouch(ArchiveFileSystemEvent<?> event) {
            assert null == event || event.getSource() == getFileSystem();
            getModel().setTouched(true);
        }
    }

    private final ArchiveDriver<E> driver;
    private final FileSystemController<?> parent;

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

    private final ArchiveFileSystemTouchListener<ArchiveEntry> touchListener
            = new TouchListener();

    public UpdatingArchiveController(
            final ArchiveModel model,
            final ArchiveDriver<E> driver,
            final FileSystemController<?> parent) {
        super(model);
        if (null == driver)
            throw new NullPointerException();
        if (parent.getModel() != model.getParent())
            throw new IllegalArgumentException("Parent/member mismatch!");
        this.driver = driver;
        this.parent = parent;
    }

    @Override
    public FileSystemController<?> getParent() {
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
    void mount(final boolean autoCreate, final BitField<OutputOption> options)
    throws IOException {
        try {
            final FileSystemController<?> parent = getParent();
            final FileSystemEntryName parentName = getModel()
                    .resolveParent(ROOT_ENTRY_NAME);
            // readOnly must be set first because the parent archive controller
            // could be a FileFileSystemController and on stinky Windows
            // this property turns to TRUE once a file is opened for
            // reading!
            final boolean readOnly = !parent.isWritable(parentName);
            final InputSocket<?> socket = parent.getInputSocket(
                    parentName, BitField.of(InputOption.CACHE));
            input = new Input(driver.newInputShop(getModel(), socket));
            setFileSystem(newArchiveFileSystem(
                    input.getDriverProduct(), driver,
                    socket.getLocalTarget(), readOnly));
        } catch (FileSystemException ex) {
            throw ex;
        } catch (TabuFileException ex) {
            throw ex;
        } catch (IOException ex) {
            if (!autoCreate)
                throw new FalsePositiveException(getModel(), ex);
            // The entry does NOT exist in the parent archive
            // file, but we may create it automatically.
            // This may fail if e.g. the container file is an RAES
            // encrypted ZIP file and the user cancels password
            // prompting.
            try {
                makeOutput(options);
            } catch (FileSystemException ex2) {
                throw ex2;
            } catch (TabuFileException ex2) {
                throw ex2;
            } catch (IOException ex2) {
                throw new FalsePositiveException(getModel(), ex2);
            }
            touchListener.beforeTouch(null);
            setFileSystem(newArchiveFileSystem(driver));
            touchListener.afterTouch(null);
        }
        getFileSystem().addArchiveFileSystemTouchListener(touchListener);
    }

    private void makeOutput(final BitField<OutputOption> options)
    throws IOException {
        if (null != output)
            return;
        final FileSystemController<?> parent = getParent();
        final FileSystemEntryName parentName = getModel()
                .resolveParent(ROOT_ENTRY_NAME);
        final OutputSocket<?> socket = parent.getOutputSocket(
                parentName, options.set(OutputOption.CACHE), null);
        output = new Output(driver.newOutputShop(getModel(), socket,
                    null == input ? null : input.getDriverProduct()));
    }

    @Override
    InputSocket<?> getInputSocket(final String name)
    throws IOException {
        return input.getInputSocket(name);
    }

    @Override
    OutputSocket<?> getOutputSocket(final E entry)
    throws IOException {
        makeOutput(BitField.noneOf(OutputOption.class));
        return output.getOutputSocket(entry);
    }

    @Override
    boolean autoSync(final FileSystemEntryName name, final Access intention)
    throws SyncException, FileSystemException {
        final ArchiveFileSystem<E> fileSystem;
        final ArchiveFileSystemEntry<E> entry;
        if (null == (fileSystem = getFileSystem())
                || null == (entry = fileSystem.getEntry(name)))
            return false;
        String n = null;
        if (null != output && null != output.getEntry(
                n = entry.getArchiveEntry().getName()))
            return sync();
        if (null != input && null != input.getEntry(
                null != n ? n : (n = entry.getArchiveEntry().getName())))
            return false;
        if (READ == intention)
            return sync();
        return false;
    }

    private boolean sync() throws SyncException, FileSystemException {
        getModel().assertWriteLockedByCurrentThread();
        sync(   BitField.of(WAIT_CLOSE_INPUT, WAIT_CLOSE_OUTPUT), new SyncExceptionBuilder());
        return true;
    }

    @Override
	public <X extends IOException>
    void sync(  final BitField<SyncOption> options, final ExceptionBuilder<? super SyncException, X> builder)
    throws X {
        assert !isTouched() || null != output; // file system touched => output archive
        assert getModel().writeLock().isHeldByCurrentThread();

        if (options.get(FORCE_CLOSE_OUTPUT) && !options.get(FORCE_CLOSE_INPUT))
            throw new IllegalArgumentException();
        awaitSync(builder, options);
        commenceSync(builder);
        try {
            if (!options.get(ABORT_CHANGES) && isTouched())
                performSync(builder);
        } finally {
            try {
                commitSync(builder);
            } finally {
                assert null == getFileSystem();
                assert null == input;
                assert null == output;
                getModel().setTouched(false);
            }
        }
        builder.check();
    }

    private <X extends IOException>
    void awaitSync(
            final ExceptionBuilder<? super SyncException, X> builder,
            final BitField<SyncOption> options)
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
                    throw builder.fail( new SyncException(getModel(),
                                            new OutputBusyException(message)));
                builder.warn(   new SyncWarningException(getModel(),
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
                    throw builder.fail( new SyncException(getModel(),
                                            new InputBusyException(message)));
                builder.warn(   new SyncWarningException(getModel(),
                                    new InputBusyException(message)));
            }
        }
    }

    /**
     * Closes and disconnects all entry streams of the output and input
     * archive.
     *
     * @param handler An exception handler - {@code null} is not permitted.
     * @throws SyncException If any exceptional condition occurs
     *         throughout the processing of the container archive file.
     */
    private <X extends IOException>
    void commenceSync(final ExceptionHandler<? super SyncException, X> handler)
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
                handler.warn(new SyncWarningException(getModel(), cause));
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
     * @param  handler An exception handler - {@code null} is not permitted.
     * @throws IOException If any exceptional condition occurs throughout the
     *         processing of the container archive file.
     */
    private <X extends IOException>
    void performSync(final ExceptionHandler<? super SyncException, X> handler)
    throws X {
        assert isTouched();
        assert null != output;

        class FilterExceptionHandler
        implements ExceptionHandler<IOException, X> {
            IOException last;

            @Override
			public X fail(final IOException cause) {
                last = cause;
                return handler.fail(new SyncException(getModel(), cause));
            }

            @Override
			public void warn(final IOException cause) throws X {
                assert null != cause;
                final IOException old = last;
                last = cause;
                if (null != old || !(cause instanceof InputException))
                    throw handler.fail(new SyncException(getModel(), cause));
                handler.warn(new SyncWarningException(getModel(), cause));
            }
        } // class FilterExceptionHandler

        copy(   getFileSystem(),
                null == input
                    ? new DummyInputService<E>()
                    : input.getDriverProduct(),
                output.getDriverProduct(),
                (ExceptionHandler<IOException, X>) new FilterExceptionHandler());
    }

    private static <E extends ArchiveEntry, X extends IOException>
    void copy(  final ArchiveFileSystem<E> fileSystem,
                final InputService<E> input,
                final OutputService<E> output,
                final ExceptionHandler<IOException, X> handler)
    throws X {
        for (final ArchiveFileSystemEntry<E> fse : fileSystem) {
            final E ae = fse.getArchiveEntry();
            final String n = ae.getName();
            if (null != output.getEntry(n))
                continue; // we have already written this entry
            try {
                if (DIRECTORY == fse.getType()) {
                    if (isRoot(fse.getName()))
                        continue; // never write the (virtual) root directory
                    if (UNKNOWN == fse.getTime(Access.WRITE))
                        continue; // never write ghost directories
                    output.getOutputSocket(ae).newOutputStream().close();
                } else if (null != input.getEntry(n)) {
                    IOSocket.copy(  input.getInputSocket(n),
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

    /**
     * Discards the file system and closes the output and input archive.
     * 
     * @param handler An exception handler - {@code null} is not permitted.
     * @throws SyncException If any exceptional condition occurs
     *         throughout the processing of the container archive file.
     */
    private <X extends IOException>
    void commitSync(final ExceptionHandler<? super SyncException, X> handler)
    throws X {
        setFileSystem(null);

        // The output archive must be closed BEFORE the input archive is
        // closed. This is because the input archive has been presented to the
        // output archive as the "source" when it was created and may be using
        // the input archive when its closing to retrieve some meta data
        // information.
        // E.g. for ZIP archive files, the OutputShop copies the postamble
        // from the InputShop when it closes.
        try {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ex) {
                    throw handler.fail(new SyncException(getModel(), ex));
                } finally {
                    output = null;
                }
            }
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ex) {
                    handler.warn(new SyncWarningException(getModel(), ex));
                } finally {
                    input = null;
                }
            }
        }
    }

    private boolean isTouched() {
        final ArchiveFileSystem<E> fileSystem = getFileSystem();
        return null != fileSystem && fileSystem.isTouched();
    }

    @Override
    public void unlink(FileSystemEntryName name) throws IOException {
        super.unlink(name);
        if (isRoot(name.getPath()))
            getParent().unlink(getModel().resolveParent(name));
    }
}
