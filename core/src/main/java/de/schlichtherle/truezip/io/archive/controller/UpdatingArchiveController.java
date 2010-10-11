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

import de.schlichtherle.truezip.io.socket.InputOption;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem.Entry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystems;
import de.schlichtherle.truezip.io.archive.filesystem.VetoableTouchListener;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.socket.IOSocket;
import de.schlichtherle.truezip.io.socket.CommonEntry.Access;
import de.schlichtherle.truezip.io.socket.CommonEntry;
import de.schlichtherle.truezip.io.socket.InputService;
import de.schlichtherle.truezip.io.socket.InputShop;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.ConcurrentInputShop;
import de.schlichtherle.truezip.io.socket.OutputService;
import de.schlichtherle.truezip.io.socket.OutputShop;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.io.socket.ConcurrentOutputShop;
import de.schlichtherle.truezip.io.socket.OutputOption;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;

import static de.schlichtherle.truezip.io.socket.OutputOption.CREATE_PARENTS;
import static de.schlichtherle.truezip.io.archive.controller.FileSystemController.SyncOption.ABORT_CHANGES;
import static de.schlichtherle.truezip.io.archive.controller.FileSystemController.SyncOption.FORCE_CLOSE_INPUT;
import static de.schlichtherle.truezip.io.archive.controller.FileSystemController.SyncOption.FORCE_CLOSE_OUTPUT;
import static de.schlichtherle.truezip.io.archive.controller.FileSystemController.SyncOption.REASSEMBLE_BUFFERS;
import static de.schlichtherle.truezip.io.archive.controller.FileSystemController.SyncOption.WAIT_CLOSE_INPUT;
import static de.schlichtherle.truezip.io.archive.controller.FileSystemController.SyncOption.WAIT_CLOSE_OUTPUT;
import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.ROOT;
import static de.schlichtherle.truezip.io.socket.CommonEntry.Access.READ;
import static de.schlichtherle.truezip.io.socket.CommonEntry.Type.DIRECTORY;
import static de.schlichtherle.truezip.io.socket.CommonEntry.UNKNOWN;

/**
 * This archive controller implements the mounting/unmounting strategy
 * by performing a full update of the target archive file.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class UpdatingArchiveController<AE extends ArchiveEntry>
extends     FileSystemArchiveController<AE> {

    private static final class DummyInputService<CE extends CommonEntry>
    implements InputShop<CE> {

        @Override
        public void close() throws IOException {
        }

        @Override
        public int size() {
            return 0;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
        public Iterator<CE> iterator() {
            return (Iterator) Collections.emptyList().iterator();
        }

        @Override
        public CE getEntry(String name) {
            return null;
        }

        @Override
        public InputSocket<CE> newInputSocket(CE target)
        throws IOException {
            if (target == null)
                throw new NullPointerException();
            throw new FileNotFoundException();
        }
    }

    /**
     * This member class makes this archive controller instance strongly
     * reachable from any created input stream.
     * This is required by the memory management to ensure that for any
     * prospective archive file at most one archive controller object is in
     * use at any time.
     *
     * @see ArchiveControllers#getController(URI, ArchiveDriver, FileSystemController)
     */
    private final class Input extends ConcurrentInputShop<AE> {
        Input(InputShop<AE> input) {
            super(input);
        }

        /** Returns the product of the archive driver this input is wrapping. */
        InputShop<AE> getDriverProduct() {
            return target;
        }
    }

    /**
     * This member class makes this archive controller instance strongly
     * reachable from any created output stream.
     * This is required by the memory management to ensure that for any
     * prospective archive file at most one archive controller object is in
     * use at any time.
     *
     * @see ArchiveControllers#getController(URI, ArchiveDriver, FileSystemController)
     */
    private final class Output extends ConcurrentOutputShop<AE> {
        Output(OutputShop<AE> output) {
            super(output);
        }

        /** Returns the product of the archive driver this output is wrapping. */
        OutputShop<AE> getDriverProduct() {
            return target;
        }

        @Override
        public OutputSocket<AE> newOutputSocket(AE entry)
        throws IOException {
            assert null != entry;
            return super.newOutputSocket(entry);
        }
    }

    private final class TouchListener implements VetoableTouchListener {
        @Override
        public void touch() throws IOException {
            ensureOutput(false);
            getModel().setTouched(true);
        }
    }

    private final VetoableTouchListener vetoableTouchListener
            = new TouchListener();

    /**
     * An {@link Input} object used to mount the virtual file system
     * and read the entries from the archive file.
     */
    private Input input;

    /**
     * The (possibly temporary) {@link Output} we are writing newly
     * created or modified entries to.
     */
    private Output output;

    UpdatingArchiveController(ArchiveModel model, ArchiveDriver<AE> driver) {
        super(model, driver);
    }

    private ArchiveFileSystem<AE> newArchiveFileSystem()
    throws IOException {
        return ArchiveFileSystems.newArchiveFileSystem(
                getDriver(), vetoableTouchListener);
    }

    private ArchiveFileSystem<AE> newArchiveFileSystem(
            CommonEntry rootTemplate,
            boolean readOnly) {
        return ArchiveFileSystems.newArchiveFileSystem(
                input.getDriverProduct(), getDriver(),
                rootTemplate, vetoableTouchListener, readOnly);
    }

    @Override
    void mount(final boolean autoCreate, final boolean createParents) {
        assert input == null;
        assert output == null;
        assert getFileSystem() == null;

        try {
            try {
                final FileSystemController controller = getEnclController();
                final String path = getEnclPath(ROOT);
                final boolean readOnly = !controller.isWritable(path);
                final InputSocket<?> socket = controller.newInputSocket(
                        path, BitField.of(InputOption.BUFFER));
                input = new Input(getDriver().newInputShop(getModel(), socket));
                setFileSystem(newArchiveFileSystem(
                        socket.getLocalTarget(), readOnly));
            } catch (IOException ex) {
                if (!autoCreate)
                    throw ex;
                // The entry does NOT exist in the enclosing archive
                // file, but we may create it automatically.
                // This may fail if e.g. the target file is an RAES
                // encrypted ZIP file and the user cancels password
                // prompting.
                ensureOutput(createParents);
                setFileSystem(newArchiveFileSystem());
            }

            assert autoCreate || input != null;
            assert autoCreate || output == null;
            assert getFileSystem() != null;
        } catch (RuntimeException ex) {
            assert input == null;
            assert output == null;
            assert getFileSystem() == null;

            throw ex;
        } catch (IOException ex) {
            assert input == null;
            assert output == null;
            assert getFileSystem() == null;

            throw new FalsePositiveEntryException(ex);
        }
    }

    private void ensureOutput(final boolean createParents) throws IOException {
        if (null != output)
            return;

        final FileSystemController controller = getEnclController();
        final String path = getEnclPath(ROOT);
        final OutputSocket<?> socket = controller.newOutputSocket(path,
                BitField.of(OutputOption.BUFFER)
                    .set(CREATE_PARENTS, createParents));
        output = new Output(getDriver().newOutputShop(getModel(), socket,
                    null == input ? null : input.getDriverProduct()));
    }

    @Override
    public InputSocket<AE> newInputSocket(final AE entry)
    throws IOException {
        return input.newInputSocket(entry);
    }

    @Override
    public OutputSocket<AE> newOutputSocket(final AE entry)
    throws IOException {
        ensureOutput(false);
        return output.newOutputSocket(entry);
    }

    private boolean isFileSystemTouched() {
        ArchiveFileSystem<AE> fileSystem = getFileSystem();
        return null != fileSystem && fileSystem.isTouched();
    }

    @Override
	boolean autoSync(final String path, final Access intention)
    throws ArchiveSyncException {
        final ArchiveFileSystem<AE> fileSystem;
        final Entry<AE> entry;
        if (null == (fileSystem = getFileSystem())
                || null == (entry = fileSystem.getEntry(path)))
            return false;
        if (null != output && null != output.getEntry(entry.getTarget().getName()))
            return sync();
        if (null != input && null != input.getEntry(entry.getTarget().getName()))
            return false;
        if (READ == intention)
            return sync();
        return false;
    }

    private boolean sync() throws ArchiveSyncException {
        sync(   new DefaultArchiveSyncExceptionBuilder(),
                BitField.of(WAIT_CLOSE_INPUT, WAIT_CLOSE_OUTPUT));
        return true;
    }

    @Override
	public void sync(   final ArchiveSyncExceptionBuilder builder,
                        final BitField<SyncOption> options)
    throws ArchiveSyncException {
        assert !isFileSystemTouched() || output != null; // file system touched => output archive

        ensureWriteLockedByCurrentThread();

        if (options.get(FORCE_CLOSE_OUTPUT) && !options.get(FORCE_CLOSE_INPUT))
            throw new IllegalArgumentException();

        // Check output streams first, because FORCE_CLOSE_INPUT may be
        // set and FORCE_CLOSE_OUTPUT may be unset in which case we
        // don't even need to check open input streams if there are
        // some open output streams.
        if (output != null) {
            final int outStreams = output.waitCloseOthers(
                    options.get(WAIT_CLOSE_OUTPUT) ? 0 : 50);
            if (outStreams > 0) {
                if (!options.get(FORCE_CLOSE_OUTPUT))
                    throw builder.fail(new ArchiveOutputBusyException(
                            getModel(), outStreams));
                builder.warn(new ArchiveOutputBusyWarningException(
                        getModel(), outStreams));
            }
        }
        if (input != null) {
            final int inStreams = input.waitCloseOthers(
                    options.get(WAIT_CLOSE_INPUT) ? 0 : 50);
            if (inStreams > 0) {
                if (!options.get(FORCE_CLOSE_INPUT))
                    throw builder.fail(new ArchiveInputBusyException(
                            getModel(), inStreams));
                builder.warn(new ArchiveInputBusyWarningException(
                        getModel(), inStreams));
            }
        }

        // Now update the target archive file.
        try {
            try {
                reset1(builder);
                if (!options.get(ABORT_CHANGES) && isFileSystemTouched())
                    update(builder);
            } finally {
                reset2(builder);
            }
        } finally {
            assert getFileSystem() == null;
            assert null == input;
            assert null == output;
            getModel().setTouched(false);
        }

        builder.check();
    }

    /**
     * Updates all entries in the virtual file system to the (temporary) output
     * archive file.
     * <p>
     * <b>This method is intended to be called by {@code update()} only!</b>
     *
     * @param handler An exception handler - {@code null} is not permitted.
     * @throws ArchiveSyncException If any exceptional condition occurs
     *         throughout the processing of the target archive file.
     */
    private void update(final ArchiveSyncExceptionHandler handler)
    throws ArchiveSyncException {
        assert isFileSystemTouched();
        assert output != null;
        assert checkNoDeletedEntriesWithNewData();

        class FilterExceptionHandler
        implements ExceptionHandler<IOException, ArchiveSyncException> {

            final ArchiveSyncExceptionHandler delegate;
            IOException last;

            FilterExceptionHandler(final ArchiveSyncExceptionHandler delegate) {
                if (delegate == null)
                    throw new NullPointerException();
                this.delegate = delegate;
            }

            @Override
			public ArchiveSyncException fail(final IOException cannotHappen) {
                throw new AssertionError(cannotHappen);
            }

            @Override
			public void warn(final IOException cause) throws ArchiveSyncException {
                if (cause == null)
                    throw new NullPointerException();
                final IOException old = last;
                last = cause;
                if (!(cause instanceof InputException))
                    throw handler.fail(new ArchiveSyncException(getModel(), cause));
                if (old == null)
                    delegate.warn(new ArchiveSyncWarningException(getModel(), cause));
            }
        } // class FilterExceptionHandler
        update((ExceptionHandler<IOException, ArchiveSyncException>) new FilterExceptionHandler(handler));
    }

    private boolean checkNoDeletedEntriesWithNewData()
    throws ArchiveSyncException {
        assert isFileSystemTouched();

        // Check if we have written out any entries that have been
        // deleted from the archive file system meanwhile and prepare
        // to throw a warning exception.
        final ArchiveFileSystem<AE> fileSystem = getFileSystem();
        for (final AE entry : output) {
            assert DIRECTORY != entry.getType();
            // At this point in time we could have written only file archive
            // entries with valid path names, so the following test should be
            // enough:
            String path = entry.getName();
            //path = de.schlichtherle.truezip.io.Paths.normalize(path, CommonEntry.SEPARATOR_CHAR);
            if (fileSystem.getEntry(path) == null) {
                // The entry has been written out already, but also
                // has been deleted from the master directory meanwhile.
                // Create a warn exception, but do not yet throw it.
                throw new AssertionError(path + " (failed to remove archive entry)");
            }
        }
        return true;
    }

    private <E extends Exception>
    void update(final ExceptionHandler<IOException, E> handler)
    throws E {
        update( getFileSystem(),
                null == input ? new DummyInputService<AE>() : input.getDriverProduct(),
                output.getDriverProduct(),
                handler);
    }

    private static <AE extends ArchiveEntry, E extends Exception>
    void update(  final ArchiveFileSystem<AE> fileSystem,
                final InputService<AE> input,
                final OutputService<AE> output,
                final ExceptionHandler<? super IOException, E> handler)
    throws E {
        final AE root = fileSystem.getEntry(ROOT).getTarget();
        assert root != null;
        // TODO: Consider iterating over input instead, normalizing the input
        // entry name and checking with master map and output.
        // Consider the effect for absolute entry names, too.
        for (final Entry<AE> fse : fileSystem) {
            final AE ae = fse.getTarget();
            final String n = ae.getName();
            if (output.getEntry(n) == ae)
                continue; // we have already written this entry
            try {
                if (DIRECTORY == ae.getType()) {
                    if (root == ae)
                        continue; // never write the virtual root directory
                    if (UNKNOWN == ae.getTime(Access.WRITE))
                        continue; // never write ghost directories
                    output.newOutputSocket(ae).newOutputStream().close();
                } else if (input.getEntry(n) == ae) {
                    IOSocket.copy(  input.newInputSocket(ae),
                                    output.newOutputSocket(ae));
                } else {
                    // The file system entry is a newly created non-directory
                    // entry which hasn't received any content yet.
                    // Write an empty file system entry now as a marker in
                    // order to recreate the file system entry when the file
                    // system gets remounted from the target archive file.
                    output.newOutputSocket(ae).newOutputStream().close();
                }
            } catch (IOException ex) {
                handler.warn(ex);
            }
        }
    }

    /**
     * Closes and disconnects all entry streams of the output and input
     * archive.
     * 
     * @param handler An exception handler - {@code null} is not permitted.
     * @throws ArchiveSyncException If any exceptional condition occurs
     *         throughout the processing of the target archive file.
     */
    private void reset1(final ArchiveSyncExceptionHandler handler)
    throws ArchiveSyncException {
        class FilterExceptionHandler
        implements ExceptionHandler<IOException, ArchiveSyncException> {
            @Override
			public ArchiveSyncException fail(IOException cannotHappen) {
                throw new AssertionError(cannotHappen);
            }

            @Override
			public void warn(IOException cause) throws ArchiveSyncException {
                if (null == cause)
                    throw new NullPointerException();
                handler.warn(new ArchiveSyncWarningException(getModel(), cause));
            }
        } // class FilterExceptionHandler
        final FilterExceptionHandler decoratorHandler = new FilterExceptionHandler();
        if (output != null)
            output.closeAll((ExceptionHandler<IOException, ArchiveSyncException>) decoratorHandler);
        if (input != null)
            input.closeAll((ExceptionHandler<IOException, ArchiveSyncException>) decoratorHandler);
    }

    /**
     * Discards the file system and closes the output and input archive.
     * 
     * @param handler An exception handler - {@code null} is not permitted.
     * @throws ArchiveSyncException If any exceptional condition occurs
     *         throughout the processing of the target archive file.
     */
    private void reset2(final ArchiveSyncExceptionHandler handler)
    throws ArchiveSyncException {
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
                    handler.warn(new ArchiveSyncException(getModel(), ex));
                } finally {
                    output = null;
                }
            }
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ex) {
                    handler.warn(new ArchiveSyncWarningException(getModel(), ex));
                } finally {
                    input = null;
                }
            }
        }
    }
}
