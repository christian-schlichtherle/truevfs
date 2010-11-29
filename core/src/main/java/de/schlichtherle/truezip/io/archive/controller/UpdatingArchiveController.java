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

import de.schlichtherle.truezip.io.filesystem.FileFileSystemFactory;
import de.schlichtherle.truezip.io.archive.model.ArchiveModel;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystemEvent;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystemEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystemListener;
import de.schlichtherle.truezip.io.entry.Entry.Access;
import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.entry.FilterEntry;
import de.schlichtherle.truezip.io.filesystem.ComponentFileSystemController;
import de.schlichtherle.truezip.io.filesystem.FalsePositiveException;
import de.schlichtherle.truezip.io.filesystem.FileSystemEntry;
import de.schlichtherle.truezip.io.filesystem.FileSystemException;
import de.schlichtherle.truezip.io.filesystem.SyncExceptionBuilder;
import de.schlichtherle.truezip.io.filesystem.SyncException;
import de.schlichtherle.truezip.io.filesystem.SyncOption;
import de.schlichtherle.truezip.io.filesystem.SyncWarningException;
import de.schlichtherle.truezip.io.InputBusyException;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.OutputBusyException;
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
import de.schlichtherle.truezip.io.TabuFileException;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import de.schlichtherle.truezip.util.ExceptionHandler;
import java.io.CharConversionException;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import javax.swing.Icon;

import static de.schlichtherle.truezip.io.filesystem.SyncOption.ABORT_CHANGES;
import static de.schlichtherle.truezip.io.filesystem.SyncOption.FORCE_CLOSE_INPUT;
import static de.schlichtherle.truezip.io.filesystem.SyncOption.FORCE_CLOSE_OUTPUT;
import static de.schlichtherle.truezip.io.filesystem.SyncOption.WAIT_CLOSE_INPUT;
import static de.schlichtherle.truezip.io.filesystem.SyncOption.WAIT_CLOSE_OUTPUT;
import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.ROOT;
import static de.schlichtherle.truezip.io.entry.Entry.Access.READ;
import static de.schlichtherle.truezip.io.entry.Entry.Type.DIRECTORY;
import static de.schlichtherle.truezip.io.entry.Entry.Type.SPECIAL;
import static de.schlichtherle.truezip.io.entry.Entry.UNKNOWN;
import static de.schlichtherle.truezip.io.Paths.isRoot;

/**
 * This archive controller implements the mounting/unmounting strategy
 * by performing a full update of the target archive file.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class UpdatingArchiveController<AE extends ArchiveEntry>
extends FileSystemArchiveController<AE> {

    private static final class DummyInputService<E extends Entry>
    implements InputShop<E> {

        @Override
        public void close() throws IOException {
        }

        @Override
        public int size() {
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

    private final class Listener
    implements ArchiveFileSystemListener<ArchiveEntry> {
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

    private final ArchiveDriver<AE> driver;
    private final ComponentFileSystemController<?> parent;

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

    private final ArchiveFileSystemListener<ArchiveEntry> listener
            = new Listener();

    public UpdatingArchiveController(
            final ArchiveModel model,
            final ComponentFileSystemController<?> parent,
            final ArchiveDriver<AE> driver) {
        super(model);
        assert null != driver;
        this.driver = driver;
        if (null != parent) {
            if (model.getParent() != parent.getModel())
                throw new IllegalArgumentException("parent/member mismatch!");
            this.parent = parent;
        } else {
            this.parent = FileFileSystemFactory.INSTANCE.newController(
                    model.getParent());
        }
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

    @Override
    public ComponentFileSystemController<?> getParent() {
        return parent;
    }

    private String parentPath(String path) {
        return getModel().parentPath(path);
    }

    @Override
    public Icon getOpenIcon()
    throws FileSystemException {
        try {
            autoMount(); // detect false positives!
        } catch (FileSystemException ex) {
            throw ex;
        } catch (IOException ex) {
            return null;
        }
        return getDriver().getOpenIcon(getModel());
    }

    @Override
    public Icon getClosedIcon()
    throws FileSystemException {
        try {
            autoMount(); // detect false positives!
        } catch (FileSystemException ex) {
            throw ex;
        } catch (IOException ex) {
            return null;
        }
        return getDriver().getClosedIcon(getModel());
    }

    @Override
    public final ArchiveFileSystemEntry<AE> getEntry(final String path)
    throws FileSystemException {
        try {
            return autoMount().getEntry(path);
        } catch (FileSystemException ex) {
            throw ex;
        } catch (IOException ex) {
            if (!isRoot(path))
                return null;
            final FileSystemEntry<?> entry = getParent()
                    .getEntry(parentPath(path));
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
    extends FilterEntry<AE>
    implements ArchiveFileSystemEntry<AE> {
        SpecialFileEntry(AE entry) {
            super(entry);
        }

        @Override
        public Entry.Type getType() {
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
    void mount(final boolean autoCreate, final BitField<OutputOption> options)
    throws IOException {
        try {
            final ComponentFileSystemController<?> parent = getParent();
            final String parentPath = parentPath(ROOT);
            // readOnly must be set first because the parent archive controller
            // could be a FileFileSystemController and on stinky Windows
            // this property turns to TRUE once a file is opened for
            // reading!
            final boolean readOnly = !parent.isWritable(parentPath);
            final InputSocket<?> socket = parent.getInputSocket(
                    parentPath, BitField.of(InputOption.CACHE));
            input = new Input(getDriver().newInputShop(getModel(), socket));
            setFileSystem(ArchiveFileSystem.newArchiveFileSystem(
                    input.getDriverProduct(), getDriver(),
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
            // This may fail if e.g. the target file is an RAES
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
            listener.beforeTouch(null);
            setFileSystem(ArchiveFileSystem.newArchiveFileSystem(getDriver()));
            listener.afterTouch(null);
        }
        getFileSystem().addArchiveFileSystemListener(listener);
    }

    private void makeOutput(final BitField<OutputOption> options)
    throws IOException {
        if (null != output)
            return;
        final ComponentFileSystemController<?> parent = getParent();
        final String parentPath = parentPath(ROOT);
        final OutputSocket<?> socket = parent.getOutputSocket(
                parentPath, options.set(OutputOption.CACHE), null);
        output = new Output(getDriver().newOutputShop(getModel(), socket,
                    null == input ? null : input.getDriverProduct()));
    }

    @Override
    InputSocket<? extends AE> getInputSocket(final String name)
    throws IOException {
        return input.getInputSocket(name);
    }

    @Override
    OutputSocket<? extends AE> getOutputSocket(final AE entry)
    throws IOException {
        makeOutput(BitField.noneOf(OutputOption.class));
        return output.getOutputSocket(entry);
    }

    @Override
	boolean autoSync(final String path, final Access intention)
    throws SyncException, FileSystemException {
        final ArchiveFileSystem<AE> fileSystem;
        final ArchiveFileSystemEntry<AE> entry;
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

    private boolean sync() throws SyncException, FileSystemException {
        sync(   new SyncExceptionBuilder(),
                BitField.of(WAIT_CLOSE_INPUT, WAIT_CLOSE_OUTPUT));
        return true;
    }

    @Override
	public <E extends IOException>
    void sync(  final ExceptionBuilder<? super SyncException, E> builder,
                final BitField<SyncOption> options)
    throws E, FileSystemException {
        assert !isTouched() || null != output; // file system touched => output archive
        getModel().assertWriteLockedByCurrentThread();
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

    private <E extends IOException>
    void awaitSync(
            final ExceptionBuilder<? super SyncException, E> builder,
            final BitField<SyncOption> options)
    throws E {
        // Check output streams first, because FORCE_CLOSE_INPUT may be
        // set and FORCE_CLOSE_OUTPUT may be unset in which case we
        // don't even need to check open input streams if there are
        // some open output streams.
        if (output != null) {
            final int outStreams = output.waitCloseOthers(options.get(WAIT_CLOSE_OUTPUT) ? 0 : 50);
            if (outStreams > 0) {
                final String message = "Number of open output streams: " + outStreams;
                if (!options.get(FORCE_CLOSE_OUTPUT))
                    throw builder.fail(new SyncException(getModel(), new OutputBusyException(message)));
                builder.warn(new SyncWarningException(getModel(), new OutputBusyException(message)));
            }
        }
        if (input != null) {
            final int inStreams = input.waitCloseOthers(options.get(WAIT_CLOSE_INPUT) ? 0 : 50);
            if (inStreams > 0) {
                final String message = "Number of open input streams: " + inStreams;
                if (!options.get(FORCE_CLOSE_INPUT))
                    throw builder.fail(new SyncException(getModel(), new InputBusyException(message)));
                builder.warn(new SyncWarningException(getModel(), new InputBusyException(message)));
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
    void commenceSync(final ExceptionHandler<? super SyncException, E> handler)
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
                handler.warn(new SyncWarningException(getModel(), cause));
            }
        } // class FilterExceptionHandler

        final FilterExceptionHandler decoratorHandler = new FilterExceptionHandler();
        if (output != null)
            output.closeAll((ExceptionHandler<IOException, E>) decoratorHandler);
        if (input != null)
            input.closeAll((ExceptionHandler<IOException, E>) decoratorHandler);
    }

    /**
     * Synchronizes all entries in the (virtual) archive file system with the
     * (temporary) output archive file.
     *
     * @param  handler An exception handler - {@code null} is not permitted.
     * @throws IOException If any exceptional condition occurs throughout the
     *         processing of the target archive file.
     */
    private <E extends IOException>
    void performSync(final ExceptionHandler<? super SyncException, E> handler)
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
                return handler.fail(new SyncException(getModel(), cause));
            }

            @Override
			public void warn(final IOException cause) throws E {
                assert null != cause;
                final IOException old = last;
                last = cause;
                if (null != old || !(cause instanceof InputException))
                    throw handler.fail(new SyncException(getModel(), cause));
                handler.warn(new SyncWarningException(getModel(), cause));
            }
        } // class FilterExceptionHandler

        copy(   getFileSystem(),
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
            //path = de.schlichtherle.truezip.io.Paths.normalize(path, Entry.SEPARATOR_CHAR);
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
    void copy(  final ArchiveFileSystem<AE> fileSystem,
                final InputService<AE> input,
                final OutputService<AE> output,
                final ExceptionHandler<IOException, E> handler)
    throws E {
        for (final ArchiveFileSystemEntry<AE> fse : fileSystem) {
            final AE ae = fse.getTarget();
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
                    // system gets remounted from the target archive file.
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
     *         throughout the processing of the target archive file.
     */
    private <E extends IOException>
    void commitSync(final ExceptionHandler<? super SyncException, E> handler)
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
        final ArchiveFileSystem<AE> fileSystem = getFileSystem();
        return null != fileSystem && fileSystem.isTouched();
    }

    @Override
    public void unlink(String path) throws IOException {
        super.unlink(path);
        if (isRoot(path))
            getParent().unlink(parentPath(path));
    }
}
