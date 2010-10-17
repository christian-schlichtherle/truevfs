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

import de.schlichtherle.truezip.io.TabuFileException;
import de.schlichtherle.truezip.io.entry.FilterCommonEntry;
import de.schlichtherle.truezip.io.filesystem.FileSystemEntry;
import java.io.CharConversionException;
import java.util.Set;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import javax.swing.Icon;
import de.schlichtherle.truezip.io.filesystem.FileSystemController;
import de.schlichtherle.truezip.io.socket.InputOption;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem.Entry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystems;
import de.schlichtherle.truezip.io.archive.filesystem.VetoableTouchListener;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.socket.IOSocket;
import de.schlichtherle.truezip.io.entry.CommonEntry.Access;
import de.schlichtherle.truezip.io.entry.CommonEntry;
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

import static de.schlichtherle.truezip.io.archive.controller.SyncOption.ABORT_CHANGES;
import static de.schlichtherle.truezip.io.archive.controller.SyncOption.FORCE_CLOSE_INPUT;
import static de.schlichtherle.truezip.io.archive.controller.SyncOption.FORCE_CLOSE_OUTPUT;
import static de.schlichtherle.truezip.io.archive.controller.SyncOption.WAIT_CLOSE_INPUT;
import static de.schlichtherle.truezip.io.archive.controller.SyncOption.WAIT_CLOSE_OUTPUT;
import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.ROOT;
import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.SEPARATOR_CHAR;
import static de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystems.isRoot;
import static de.schlichtherle.truezip.io.entry.CommonEntry.Access.READ;
import static de.schlichtherle.truezip.io.entry.CommonEntry.Type.DIRECTORY;
import static de.schlichtherle.truezip.io.entry.CommonEntry.Type.SPECIAL;
import static de.schlichtherle.truezip.io.entry.CommonEntry.UNKNOWN;
import static de.schlichtherle.truezip.io.Paths.cutTrailingSeparators;
import static de.schlichtherle.truezip.io.socket.OutputOption.CREATE_PARENTS;

/**
 * This archive controller implements the mounting/unmounting strategy
 * by performing a full update of the target archive file.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class UpdatingArchiveController  <AE extends ArchiveEntry>
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
        public InputSocket<? extends CE> getInputSocket(String name)
        throws IOException {
            if (name == null)
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
     * @see Controllers#getController(URI, ArchiveDriver, FileSystemController)
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
     * @see Controllers#getController(URI, ArchiveDriver, FileSystemController)
     */
    private final class Output extends ConcurrentOutputShop<AE> {
        Output(OutputShop<AE> output) {
            super(output);
        }

        /** Returns the product of the archive driver this output is wrapping. */
        OutputShop<AE> getDriverProduct() {
            return target;
        }
    }

    private final class TouchListener implements VetoableTouchListener {
        @Override
        public void touch() throws IOException {
            ensureOutput(false);
            getModel().setTouched(true);
        }
    }

    private final FileSystemController<?> enclController;
    private final String enclPath;
    private final ArchiveDriver<AE> driver;

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

    private final VetoableTouchListener vetoableTouchListener
            = new TouchListener();

    UpdatingArchiveController(  final FileSystemController<?> enclController,
                                final ArchiveModel model,
                                final ArchiveDriver<AE> driver) {
        super(model);
        assert null != driver;
        this.driver = driver;
        this.enclController = enclController;
        this.enclPath = enclController
                .getModel()
                .getMountPoint()
                .relativize(model.getMountPoint())
                .getPath();
    }

    /**
     * Returns the driver instance which is used for the target archive.
     * All access to this method must be externally synchronized on this
     * controller's read lock!
     *
     * @return A valid reference to an {@link ArchiveDriver} object
     *         - never {@code null}.
     */
    private ArchiveDriver<AE> getDriver() {
        return driver;
    }

    /** Returns the file system controller for the enclosing file system. */
    private FileSystemController<?> getEnclController() {
        return enclController;
    }

    /**
     * Resolves the given relative {@code path} against the relative path of
     * this controller's archive file within its enclosing file system.
     */
    private String getEnclPath(String path) {
        return isRoot(path)
                ? cutTrailingSeparators(enclPath, SEPARATOR_CHAR)
                : enclPath + path;
    }

    @Override
    public Icon getOpenIcon()
    throws ArchiveControllerException {
        try {
            autoMount(); // detect false positives!
        } catch (ArchiveControllerException ex) {
            throw ex;
        } catch (IOException ex) {
            return null;
        }
        return getDriver().getOpenIcon(getModel());
    }

    @Override
    public Icon getClosedIcon()
    throws ArchiveControllerException {
        try {
            autoMount(); // detect false positives!
        } catch (ArchiveControllerException ex) {
            throw ex;
        } catch (IOException ex) {
            return null;
        }
        return getDriver().getClosedIcon(getModel());
    }

    @Override
    public final Entry<AE> getEntry(final String path)
    throws ArchiveControllerException {
        try {
            return autoMount().getEntry(path);
        } catch (ArchiveControllerException ex) {
            throw ex;
        } catch (IOException ex) {
            if (!isRoot(path))
                return null;
            final FileSystemEntry<?> entry = getEnclController()
                    .getEntry(getEnclPath(path));
            if (null == entry)
                return null;
            try {
                return new SpecialFileEntry<AE>(
                        getDriver().newEntry(ROOT, SPECIAL, entry.getTarget()));
            } catch (CharConversionException cannotHappen) {
                throw new AssertionError(cannotHappen);
            }
        }
    }

    private static final class SpecialFileEntry<AE extends ArchiveEntry>
    extends FilterCommonEntry<AE>
    implements Entry<AE> {
        SpecialFileEntry(AE entry) {
            super(entry);
        }

        @Override
        public CommonEntry.Type getType() {
            return SPECIAL; // drivers could ignore this type, so we must ignore them!
        }

        @Override
        public Set<String> getMembers() {
            return null;
        }

        @Override
        public AE getTarget() {
            return entry;
        }
    }

    @Override
    void mount(final boolean autoCreate, final boolean createParents)
    throws IOException {
        assert input == null;
        assert output == null;
        assert getFileSystem() == null;

        try {
            try {
                final FileSystemController<?> controller = getEnclController();
                final String path = getEnclPath(ROOT);
                // readOnly must be set first because the enclosing controller
                // could be a HostFileSystemController and on stinky Windows
                // this property turns to TRUE once a file is opened for
                // reading!
                final boolean readOnly = !controller.isWritable(path);
                final InputSocket<?> socket = controller.getInputSocket(
                        path, BitField.of(InputOption.CACHE));
                try {
                    input = new Input(getDriver().newInputShop(getModel(), socket));
                } catch (FileNotFoundException ex) {
                    throw ex;
                } catch (IOException ex) {
                    throw new FalsePositiveException(ex);
                }
                setFileSystem(ArchiveFileSystems.newArchiveFileSystem(
                        input.getDriverProduct(), getDriver(),
                        socket.getLocalTarget(), vetoableTouchListener,
                        readOnly));
            } catch (ArchiveControllerException ex) {
                throw ex;
            } catch (TabuFileException ex) {
                throw ex;
            } catch (FileNotFoundException ex) {
                if (!autoCreate)
                    throw new FalsePositiveException(ex);
                // The entry does NOT exist in the enclosing archive
                // file, but we may create it automatically.
                // This may fail if e.g. the target file is an RAES
                // encrypted ZIP file and the user cancels password
                // prompting.
                ensureOutput(createParents);
                setFileSystem(ArchiveFileSystems.newArchiveFileSystem(
                        getDriver(), vetoableTouchListener));
            }

            assert autoCreate || input != null;
            assert autoCreate || output == null;
            assert getFileSystem() != null;
        } catch (IOException ex) {
            assert null == input;
            assert null == output;
            assert getFileSystem() == null;

            throw ex;
        }
    }

    private void ensureOutput(final boolean createParents)
    throws IOException {
        if (null != output)
            return;

        final FileSystemController<?> controller = getEnclController();
        final String path = getEnclPath(ROOT);
        final OutputSocket<?> socket = controller.getOutputSocket(path,
                BitField.of(OutputOption.CACHE)
                    .set(CREATE_PARENTS, createParents),
                null);
        try {
            output = new Output(getDriver().newOutputShop(getModel(), socket,
                        null == input ? null : input.getDriverProduct()));
        } catch (FileNotFoundException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new FalsePositiveException(ex);
        }
    }

    @Override
    public InputSocket<? extends AE> getInputSocket(final String name)
    throws IOException {
        return input.getInputSocket(name);
    }

    @Override
    public OutputSocket<? extends AE> getOutputSocket(final AE entry)
    throws IOException {
        ensureOutput(false);
        return output.getOutputSocket(entry);
    }

    @Override
	boolean autoSync(final String path, final Access intention)
    throws SyncException, ArchiveControllerException {
        final ArchiveFileSystem<AE> fileSystem;
        final Entry<AE> entry;
        if (null == (fileSystem = getFileSystem())
                || null == (entry = fileSystem.getEntry(path)))
            return false;
        String n = null;
        if (null != output && null != output.getEntry(n = entry.getTarget().getName()))
            return sync();
        if (null != input && null != input.getEntry(null != n ? n : (n = entry.getTarget().getName())))
            return false;
        if (READ == intention)
            return sync();
        return false;
    }

    private boolean sync() throws SyncException, ArchiveControllerException {
        sync(   new DefaultSyncExceptionBuilder(),
                BitField.of(WAIT_CLOSE_INPUT, WAIT_CLOSE_OUTPUT));
        return true;
    }

    @Override
	public <E extends IOException>
    void sync(  final ExceptionBuilder<? super SyncException, E> builder,
                final BitField<SyncOption> options)
    throws E, ArchiveControllerException {
        assert !isTouched() || output != null; // file system touched => output archive

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
                    throw builder.fail(new SyncException(this, new ArchiveOutputBusyException(outStreams)));
                builder.warn(new SyncWarningException(this, new ArchiveOutputBusyException(outStreams)));
            }
        }
        if (input != null) {
            final int inStreams = input.waitCloseOthers(
                    options.get(WAIT_CLOSE_INPUT) ? 0 : 50);
            if (inStreams > 0) {
                if (!options.get(FORCE_CLOSE_INPUT))
                    throw builder.fail(new SyncException(this, new ArchiveInputBusyException(inStreams)));
                builder.warn(new SyncWarningException(this, new ArchiveInputBusyException(inStreams)));
            }
        }

        // Now update the target archive file.
        try {
            try {
                reset1(builder);
                if (!options.get(ABORT_CHANGES) && isTouched())
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
     * @throws SyncException If any exceptional condition occurs
     *         throughout the processing of the target archive file.
     */
    private <E extends IOException>
    void update(final ExceptionHandler<? super SyncException, E> handler)
    throws E {
        assert isTouched();
        assert output != null;
        assert checkNoDeletedEntriesWithNewData();

        class FilterExceptionHandler
        implements ExceptionHandler<IOException, E> {
            IOException last;

            @Override
			public E fail(final IOException cause) {
                last = cause;
                return handler.fail(new SyncException(UpdatingArchiveController.this, cause));
            }

            @Override
			public void warn(final IOException cause) throws E {
                assert null != cause;
                final IOException old = last;
                last = cause;
                if (null != old || !(cause instanceof InputException))
                    throw handler.fail(new SyncException(UpdatingArchiveController.this, cause));
                handler.warn(new SyncWarningException(UpdatingArchiveController.this, cause));
            }
        } // class FilterExceptionHandler

        update( getFileSystem(),
                null == input ? new DummyInputService<AE>() : input.getDriverProduct(),
                output.getDriverProduct(),
                (ExceptionHandler<IOException, E>) new FilterExceptionHandler());
    }

    private boolean checkNoDeletedEntriesWithNewData() {
        assert isTouched();

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
            if (null == fileSystem.getEntry(path)) {
                // The entry has been written out already, but also
                // has been deleted from the master directory meanwhile.
                // Create a warn exception, but do not yet throw it.
                throw new AssertionError(path + " (failed to remove archive entry)");
            }
        }
        return true;
    }

    private static <AE extends ArchiveEntry, E extends IOException>
    void update(final ArchiveFileSystem<AE> fileSystem,
                final InputService<AE> input,
                final OutputService<AE> output,
                final ExceptionHandler<IOException, E> handler)
    throws E {
        for (final Entry<AE> fse : fileSystem) {
            final AE ae = fse.getTarget();
            final String n = ae.getName();
            if (null != output.getEntry(n))
                continue; // we have already written this entry
            try {
                if (DIRECTORY == fse.getType()) {
                    if (isRoot(fse.getName()))
                        continue; // never write the virtual root directory
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
                    // system gets remounted from the target archive file.
                    output.getOutputSocket(ae).newOutputStream().close();
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
     * @throws SyncException If any exceptional condition occurs
     *         throughout the processing of the target archive file.
     */
    private <E extends IOException>
    void reset1(final ExceptionHandler<? super SyncException, E> handler)
    throws E {
        class FilterExceptionHandler
        implements ExceptionHandler<IOException, E> {
            @Override
			public E fail(IOException cannotHappen) {
                throw new AssertionError(cannotHappen);
            }

            @Override
			public void warn(IOException cause) throws E {
                if (null == cause)
                    throw new NullPointerException();
                handler.warn(new SyncWarningException(UpdatingArchiveController.this, cause));
            }
        } // class FilterExceptionHandler
        final FilterExceptionHandler decoratorHandler = new FilterExceptionHandler();
        if (output != null)
            output.closeAll((ExceptionHandler<IOException, E>) decoratorHandler);
        if (input != null)
            input.closeAll((ExceptionHandler<IOException, E>) decoratorHandler);
    }

    /**
     * Discards the file system and closes the output and input archive.
     * 
     * @param handler An exception handler - {@code null} is not permitted.
     * @throws SyncException If any exceptional condition occurs
     *         throughout the processing of the target archive file.
     */
    private <E extends IOException>
    void reset2(final ExceptionHandler<? super SyncException, E> handler)
    throws E {
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
                    throw handler.fail(new SyncException(this, ex));
                } finally {
                    output = null;
                }
            }
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ex) {
                    handler.warn(new SyncWarningException(this, ex));
                } finally {
                    input = null;
                }
            }
        }
    }
}
