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

import de.schlichtherle.truezip.io.archive.controller.file.FileOutputSocket;
import de.schlichtherle.truezip.io.archive.controller.file.FileInputSocket;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.driver.TransientIOException;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem.Entry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystems;
import de.schlichtherle.truezip.io.archive.filesystem.VetoableTouchListener;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.socket.IOSocket;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry.Access;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry.Type;
import de.schlichtherle.truezip.io.archive.controller.file.FileEntry;
import de.schlichtherle.truezip.io.socket.input.CommonInputService;
import de.schlichtherle.truezip.io.socket.input.CommonInputShop;
import de.schlichtherle.truezip.io.socket.input.CommonInputSocket;
import de.schlichtherle.truezip.io.socket.input.ConcurrentInputShop;
import de.schlichtherle.truezip.io.socket.input.FilterInputSocket;
import de.schlichtherle.truezip.io.socket.output.CommonOutputService;
import de.schlichtherle.truezip.io.socket.output.CommonOutputShop;
import de.schlichtherle.truezip.io.socket.output.CommonOutputSocket;
import de.schlichtherle.truezip.io.socket.output.ConcurrentOutputShop;
import de.schlichtherle.truezip.io.socket.output.FilterOutputSocket;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Iterator;

import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.OutputOption.CREATE_PARENTS;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.OutputOption.PRESERVE;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.ABORT_CHANGES;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.FORCE_CLOSE_INPUT;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.FORCE_CLOSE_OUTPUT;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.REASSEMBLE;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.UMOUNT;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.WAIT_CLOSE_INPUT;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.WAIT_CLOSE_OUTPUT;
import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.ROOT;
import static de.schlichtherle.truezip.io.socket.entry.CommonEntry.Type.DIRECTORY;
import static de.schlichtherle.truezip.io.socket.entry.CommonEntry.Type.FILE;
import static de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystems.isRoot;
import static de.schlichtherle.truezip.io.Files.isWritableOrCreatable;
import static de.schlichtherle.truezip.io.Files.createTempFile;

/**
 * This archive controller implements the mounting/unmounting strategy
 * by performing a full update of the target archive file.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class UpdatingArchiveController<AE extends ArchiveEntry>
extends     FileSystemArchiveController<AE> {

    /** Prefix for temporary files created by this class. */
    static final String TEMP_FILE_PREFIX = "tzp-ctrl";

    /**
     * Suffix for temporary files created by this class
     * - should <em>not</em> be {@code null} for enhanced unit tests.
     */
    static final String TEMP_FILE_SUFFIX = ".tmp";

    private static final class DummyInputService<CE extends CommonEntry>
    implements CommonInputShop<CE> {

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
        public CommonInputSocket<CE> newInputSocket(CE target)
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
     * @see ArchiveControllers#getController(URI, ArchiveDriver, ArchiveController)
     */
    private final class Input extends ConcurrentInputShop<AE> {
        Input(CommonInputShop<AE> input) {
            super(input);
        }

        /** Returns the product of the archive driver this input is wrapping. */
        CommonInputShop<AE> getDriverProduct() {
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
     * @see ArchiveControllers#getController(URI, ArchiveDriver, ArchiveController)
     */
    private final class Output extends ConcurrentOutputShop<AE> {
        Output(CommonOutputShop<AE> output) {
            super(output);
        }

        /** Returns the product of the archive driver this output is wrapping. */
        CommonOutputShop<AE> getDriverProduct() {
            return target;
        }
    }

    private final class TouchListener implements VetoableTouchListener {
        @Override
        public void touch() throws IOException {
            ensureOutArchive();
            getModel().setTouched(true);
        }
    }

    private final VetoableTouchListener vetoableTouchListener
            = new TouchListener();

    /**
     * The actual archive file as a plain {@code java.io.File} object
     * which serves as the input file for the virtual file system managed
     * by this {@link ArchiveController} object.
     * Note that this will be set to a tempory file if the archive file is
     * enclosed within another archive file.
     */
    private FileEntry inFile;

    /**
     * An {@link Input} object used to mount the virtual file system
     * and read the entries from the archive file.
     */
    private Input input;

    /**
     * Plain {@code java.io.File} object used for temporary output.
     * Maybe identical to {@code inFile}.
     */
    private FileEntry outFile;

    /**
     * The (possibly temporary) {@link Output} we are writing newly
     * created or modified entries to.
     */
    private Output output;

    /**
     * Whether or not updating the archive entry in the enclosing archive file
     * after the target archive file has been successfully updated is postponed.
     */
    private boolean needsReassembly;

    UpdatingArchiveController(ArchiveModel model, ArchiveDriver<AE> driver) {
        super(model, driver);
    }

    @Deprecated
    private FileEntry getTarget() {
        return getModel().getTarget();
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
    void mount(final boolean autoCreate, final boolean createParents)
    throws IOException {
        assert input == null;
        assert outFile == null;
        assert output == null;
        assert getFileSystem() == null;

        try {
            mount0(autoCreate, createParents);

            assert autoCreate || input != null;
            assert autoCreate || outFile == null;
            assert autoCreate || output == null;
            assert getFileSystem() != null;
        } catch (IOException ex) {
            assert input == null;
            assert outFile == null;
            assert output == null;
            assert getFileSystem() == null;

            throw ex;
        }
    }

    private void mount0(final boolean autoCreate, final boolean createParents)
    throws IOException {
        // We need to mount the virtual file system from the input file.
        // and so far we have not successfully opened the input file.
        if (isHostedDirectoryEntryTarget()) {
            // The target file of this controller is NOT enclosed
            // in another archive file.
            // Test modification time BEFORE opening the input file!
            if (inFile == null)
                inFile = getTarget();
            if (inFile.exists()) {
                // The archive file isExisting.
                // Thoroughly test read-only status BEFORE opening
                // the device file!
                final boolean isReadOnly = !isWritableOrCreatable(inFile);
                try {
                    input = newInput(inFile);
                } catch (IOException ex) {
                    // Wrap cause so that a matching catch block can assume
                    // that it can access the target in the real file system.
                    throw new FalsePositiveEntryException(this, ROOT, ex);
                }
                setFileSystem(newArchiveFileSystem(
                        new FileEntry(inFile), isReadOnly));
            } else if (autoCreate) {
                // The archive file does NOT exist, but we may create
                // it automatically.
                setFileSystem(newArchiveFileSystem());
            } else {
                assert !autoCreate;

                // The archive file does not exist and we may not create it
                // automatically.
                throw new EntryNotFoundException(
                        this, ROOT, "may not create archive file");
            }
        } else {
            // The target file of this controller IS (or appears to be)
            // enclosed in another archive file.
            if (null == inFile) {
                unwrap( getEnclController(), getEnclPath(ROOT),
                        autoCreate, createParents);
            } else {
                // The enclosed archive file has already been updated and the
                // file previously used for output has been left over to be
                // reused as our input in order to skip the lengthy process
                // of searching for the right enclosing archive controller
                // to extract the entry which is our target archive file.
                try {
                    input = newInput(inFile);
                } catch (IOException ex) {
                    // This is very unlikely unless someone has tampered with
                    // the temporary file or this controller is managing an
                    // RAES encrypted ZIP file and the client application has
                    // inadvertently called KeyManager.resetKeyProviders() or
                    // similar and the subsequent repetitious prompting for
                    // the key has been cancelled by the user.
                    // Now the next problem is that we cannot always generate
                    // a false positive exception with the correct enclosing
                    // controller because we haven't searched for it.
                    // Anyway, this is so unlikely that we simply throw a
                    // false positive exception and cross fingers that the
                    // controller and entry name information will not be used.
                    // When assertions are enabled, we prefer to treat this as
                    // a bug.
                    assert false : "We should never get here! Please read the source code comments for full details.";
                    throw new FalsePositiveEnclosedFileException(
                            getEnclController(), getEnclPath(ROOT), ex);
                }
                // Note that the archive file system must be read-write
                // because we are reusing a file which has been previously
                // used to output modifications to it!
                // Similarly, the last modification time of the left over
                // output file has been set to the last modification time of
                // its virtual root directory.
                // Nice trick, isn't it?!
                setFileSystem(newArchiveFileSystem(
                        new FileEntry(inFile), false));
            }
        }
    }

    private void unwrap(
            final ArchiveController controller,
            final String path,
            final boolean autoCreate,
            final boolean createParents)
    throws IOException {
        assert controller != null;
        assert path != null;
        assert !isRoot(path);
        assert inFile == null;

        CommonInputSocket<?> input;
        try {
            input = controller.newInputSocket(path);
        } catch (EntryNotFoundException ex) {
            input = null;
        }
        final CommonEntry entry = null == input ? null : input.getTarget();
        final Type type = null == entry ? null : entry.getType();
        if (type == FILE) {
            // This archive file DOES exist in the enclosing archive.
            // The input file is only temporarily used for the
            // archive file entry.
            final FileEntry tmp = new FileEntry(createTempFile(
                    TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX));
            // We do properly delete our temps, so this is not required.
            // In addition, this would be dangerous as the deletion
            // could happen before our shutdown hook has a chance to
            // process this archive controller!!!
            //tmp.deleteOnExit();
            try {
                // Now extract the entry to the temporary file.
                // TODO: Try InputSocket.newReadOnlyFile()!
                Streams.copy(   input.newInputStream(),
                                new java.io.FileOutputStream(tmp));
                // Don't keep tmp if this fails: our caller couldn't reproduce
                // the proper exception on a second try!
                try {
                    this.input = newInput(tmp);
                } catch (IOException ex) {
                    throw new FalsePositiveEnclosedFileException(
                            controller, path, ex);
                }
                setFileSystem(newArchiveFileSystem(entry, controller.isReadOnly()));
                inFile = tmp; // init on success only!
            } finally {
                // An archive driver could throw a NoClassDefFoundError or
                // similar if the class path is not set up correctly.
                // We are checking success to make sure that we always delete
                // the newly created temp file in case of an error.
                if (inFile == null && !tmp.delete())
                    throw new IOException(tmp.getPath() + " (couldn't delete corrupted input file)");
            }
        } else if (type != null) {
            assert type == DIRECTORY : "Only file or directory entries are supported!";
            throw new FalsePositiveEnclosedDirectoryException(
                    controller, path,
                    new FileNotFoundException("cannot read directories"));
        } else if (autoCreate) {
            // The entry does NOT exist in the enclosing archive
            // file, but we may create it automatically.
            // This may fail if e.g. the target file is an RAES
            // encrypted ZIP file and the user cancels password
            // prompting.
            final ArchiveFileSystem<AE> fileSystem = newArchiveFileSystem();
            assert outFile != null;
            assert output != null;
            // Now try to create the entry in the enclosing controller.
            try {
                controller.mknod(path, FILE, null,
                        BitField.noneOf(OutputOption.class).set(CREATE_PARENTS, createParents));
            } catch (IOException ex) {
                // The delta on the *enclosing* controller failed.
                // Hence, we need to revert our state changes.
                try {
                    try {
                        output.close();
                    } finally {
                        output = null;
                    }
                } finally {
                    boolean deleted = outFile.delete();
                    assert deleted;
                    outFile = null;
                }
                throw ex;
            }
            setFileSystem(fileSystem);
        } else {
            assert !autoCreate;

            // The entry does NOT exist in the enclosing archive
            // file and we may not create it automatically.
            throw new EntryNotFoundException(
                    controller, path, "may not create archive file");
        }
    }

    private Input newInput(final FileEntry file) throws IOException {
        class InputSocket extends FilterInputSocket<FileEntry> {
            InputSocket() throws IOException {
                super(newInputSocket(new FileEntry(file)));
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                final ReadOnlyFile rof = super.newReadOnlyFile();
                return isHostedDirectoryEntryTarget()
                        ? new CountingReadOnlyFile(rof)
                        : rof;
            }
        }
        return new Input(getDriver().newInputShop(this, new InputSocket()));
    }

    @Override
    public CommonInputSocket<AE> newInputSocket(final AE entry)
    throws IOException {
        assert input.getEntry(entry.getName()) == entry : "interface contract violation";
        return input.newInputSocket(entry);
    }

    public CommonInputSocket<FileEntry> newInputSocket(FileEntry entry)
    throws IOException {
        return new FileInputSocket(entry);
    }

    private void ensureOutArchive()
    throws IOException {
        if (null != output)
            return;

        FileEntry tmp = outFile;
        if (tmp == null) {
            if (isHostedDirectoryEntryTarget() && !getTarget().isFile()) {
                tmp = getTarget();
            } else {
                // Use a new temporary file as the output archive file.
                tmp = new FileEntry(createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX));
                // We do properly delete our temps, so this is not required.
                // In addition, this would be dangerous as the deletion
                // could happen before our shutdown hook has a chance to
                // process this controller!!!
                //tmp.deleteOnExit();
            }
        }

        output = newOutput(tmp);
        outFile = tmp; // init outFile on success only!
    }

    private Output newOutput(final FileEntry file) throws IOException {
        class OutputSocket extends FilterOutputSocket<FileEntry> {
            OutputSocket() throws IOException {
                super(newOutputSocket(new FileEntry(file)));
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                final OutputStream out = super.newOutputStream();
                return file == UpdatingArchiveController.this.getTarget()
                        ? new CountingOutputStream(out)
                        : out;
            }
        } // class OutputSocket
        Output output = null;
        try {
            return output = new Output(getDriver().newOutputShop(
                        this, new OutputSocket(),
                        null == input ? null : input.getDriverProduct()));
        } catch (TransientIOException ex) {
            // Currently we do not have any use for this wrapper exception
            // when creating output archives, so we unwrap the transient
            // cause here.
            throw ex.getCause();
        } finally {
            // An archive driver could throw a NoClassDefFoundError or
            // similar if the class path is not set up correctly.
            // We are checking success to make sure that we delete the newly
            // created temp file in case of ANY throwable.
            if (output == null) {
                if (!file.delete())
                    throw new IOException(file.getPath() + " (couldn't delete corrupted output file)");
            }
        }
    }

    @Override
    public CommonOutputSocket<AE> newOutputSocket(final AE entry)
    throws IOException {
        assert null != entry;
        ensureOutArchive();
        return output.newOutputSocket(entry);
    }

    public CommonOutputSocket<FileEntry> newOutputSocket(FileEntry entry)
    throws IOException {
        return new FileOutputSocket(entry);
    }

    private boolean isFileSystemTouched() {
        ArchiveFileSystem<AE> fileSystem = getFileSystem();
        return null != fileSystem && fileSystem.isTouched();
    }

    @Override
	boolean autoSync(String path) throws ArchiveSyncException {
        if (null == output)
            return false;
        final Entry<AE> entry = getFileSystem().getEntry(path);
        if (null == entry || null == output.getEntry(entry.getName()))
            return false;
        ensureWriteLockedByCurrentThread();
        sync(   new DefaultArchiveSyncExceptionBuilder(),
                BitField.of(WAIT_CLOSE_INPUT, WAIT_CLOSE_OUTPUT));
        return true;
    }

    @Override
	public void sync(   final ArchiveSyncExceptionBuilder builder,
                        final BitField<SyncOption> options)
    throws ArchiveSyncException {
        assert input == null || inFile != null; // input archive => input file
        assert !isFileSystemTouched() || output != null; // file system touched => output archive
        assert output == null || outFile != null; // output archive => output file

        if (options.get(FORCE_CLOSE_OUTPUT) && !options.get(FORCE_CLOSE_INPUT))
            throw new IllegalArgumentException();
        if (options.get(UMOUNT) && !options.get(REASSEMBLE))
            throw new IllegalArgumentException();

        // Check output streams first, because closeInputStreams may be
        // true and closeOutputStreams may be false in which case we
        // don't even need to check open input streams if there are
        // some open output streams.
        if (output != null) {
            final int outStreams = output.waitCloseOthers(
                    options.get(WAIT_CLOSE_OUTPUT) ? 0 : 50);
            if (outStreams > 0) {
                if (!options.get(FORCE_CLOSE_OUTPUT))
                    throw builder.fail(new ArchiveOutputBusyException(
                            this, outStreams));
                builder.warn(new ArchiveOutputBusyWarningException(
                        this, outStreams));
            }
        }
        if (input != null) {
            final int inStreams = input.waitCloseOthers(
                    options.get(WAIT_CLOSE_INPUT) ? 0 : 50);
            if (inStreams > 0) {
                if (!options.get(FORCE_CLOSE_INPUT))
                    throw builder.fail(new ArchiveInputBusyException(
                            this, inStreams));
                builder.warn(new ArchiveInputBusyWarningException(
                        this, inStreams));
            }
        }

        // Delete all entries which have been marked for deletion.
        /*super.sync(newExceptionChain,
                     waitInputStreams, closeInputStreams,
                     waitOutputStreams, closeOutputStreams,
                     sync, reassemble);*/

        // Now update the target archive file.
        try {
            if (options.get(ABORT_CHANGES)) {
                try {
                    reset1(builder);
                } finally {
                    reset2(builder);
                }
                reset3(true); // TODO: Check: Why not in another finally-block?
            } else if (isFileSystemTouched()) {
                needsReassembly = true;
                try {
                    update(builder);
                    assert getFileSystem() == null;
                    assert input == null;
                } finally {
                    assert output == null;
                }
                try {
                    if (options.get(REASSEMBLE)) {
                        reassemble(builder);
                        needsReassembly = false;
                    }
                } finally {
                    reset3(options.get(UMOUNT) && !needsReassembly);
                }
            } else if (options.get(REASSEMBLE) && needsReassembly) {
                // Nesting this archive file to its enclosing archive file
                // has been deferred until now.
                assert outFile == null; // isTouched() otherwise!
                assert inFile != null; // !needsReassembly otherwise!
                // Beware: inArchive or fileSystem may be initialized!
                try {
                    reset1(builder);
                } finally {
                    reset2(builder);
                }
                outFile = inFile;
                inFile = null;
                try {
                    reassemble(builder);
                    needsReassembly = false;
                } finally {
                    reset3(options.get(UMOUNT) && !needsReassembly);
                }
            } else if (options.get(UMOUNT)) {
                assert options.get(REASSEMBLE);
                assert !needsReassembly;
                try {
                    reset1(builder);
                } finally {
                    reset2(builder);
                }
                reset3(true);
            } else {
                // This may happen if File.update() or File.sync() has
                // been called and no modifications have been applied to
                // this ArchiveController since its creation or last update.
                assert output == null;
            }
        /*} catch (ArchiveSyncException ex) {
            throw ex;
        } catch (IOException ex) {
            throw builder.fail(new ArchiveSyncException(this, ex));*/
        } finally {
            getModel().setTouched(needsReassembly);
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
        assert checkNoDeletedEntriesWithNewData(handler);

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
                    throw handler.fail(new ArchiveSyncException(
                            UpdatingArchiveController.this, cause));
                if (old == null)
                    delegate.warn(new ArchiveSyncWarningException(
                            UpdatingArchiveController.this, cause));
            }
        } // class FilterExceptionHandler

        final long rootTime = getFileSystem().getEntry(ROOT).getTime(Access.WRITE);
        try {
            try {
                reset1(handler);
                copy((ExceptionHandler<IOException, ArchiveSyncException>) new FilterExceptionHandler(handler));
            } finally {
                // We MUST do cleanup here because (1) any entries in the
                // filesystem which were successfully written (this is the
                // normal case) have been modified by the CommonOutputShop
                // and thus cannot get used anymore to access the input;
                // and (2) if there has been any IOException on the
                // output archive there is no way to recover.
                reset2(handler);
            }
        } catch (ArchiveSyncWarningException ex) {
            throw ex;
        } catch (ArchiveSyncException ex) {
            // The output file is corrupted! We must remove it now to
            // prevent it from being reused as the input file.
            // We do this even if the output file is the target file, i.e.
            // the archive file has just been created, because it
            // doesn't make any sense to keep a corrupted archive file:
            // There is no way to recover it and it could spoil any
            // attempts to redo the file operations, because TrueZIP would
            // normaly correctly identify it as a false positive archive
            // file and would not allow to treat it like a directory again.
            boolean deleted = outFile.delete();
            outFile = null;
            assert deleted;
            throw ex;
        }

        // Set the last modification time of the output archive file
        // to the last modification time of the virtual root directory,
        // hence preserving it.
        if (!outFile.setLastModified(rootTime))
            handler.warn(new ArchiveSyncWarningException(
                    this, "couldn't preserve last modification time"));
    }

    private boolean checkNoDeletedEntriesWithNewData(
            final ArchiveSyncExceptionHandler handler)
    throws ArchiveSyncException {
        assert isFileSystemTouched();
        assert getFileSystem() != null;

        // Check if we have written out any entries that have been
        // deleted from the archive file system meanwhile and prepare
        // to throw a warning exception.
        final ArchiveFileSystem<AE> fileSystem = getFileSystem();
        for (final AE entry : output) {
            assert entry.getType() != DIRECTORY;
            // At this point in time we could have written only file archive
            // entries with valid path names, so the following test should be
            // enough:
            final String path = entry.getName();
            //final String path = normalize(entry.getName(), SEPARATOR_CHAR);
            if (fileSystem.getEntry(path) == null) {
                // The entry has been written out already, but also
                // has been deleted from the master directory meanwhile.
                // Create a warn exception, but do not yet throw it.
                handler.warn(new ArchiveSyncWarningException(
                        this, "couldn't remove archive entry '" + path + "'"));
            }
        }
        return true;
    }

    private <E extends Exception>
    void copy(final ExceptionHandler<IOException, E> handler)
    throws E {
        copy(   getFileSystem(),
                null == input ? new DummyInputService<AE>() : input.getDriverProduct(),
                output.getDriverProduct(),
                handler);
    }

    private static <AE extends ArchiveEntry, E extends Exception>
    void copy(  final ArchiveFileSystem<AE> fileSystem,
                final CommonInputService<AE> input,
                final CommonOutputService<AE> output,
                final ExceptionHandler<? super IOException, E> handler)
    throws E {
        final AE root = fileSystem.getEntry(ROOT).getTarget();
        assert root != null;
        // TODO: Consider iterating over input instead, normalizing the input
        // entry name and checking with master map and output.
        // Consider the effect for absolute entry names, too.
        for (final Entry<AE> fse : fileSystem) {
            final AE e = fse.getTarget();
            final String n = e.getName();
            if (output.getEntry(n) != null)
                continue; // we have already written this entry
            try {
                if (e.getType() == DIRECTORY) {
                    if (root == e)
                        continue; // never write the virtual root directory
                    if (e.getTime(Access.WRITE) < 0)
                        continue; // never write ghost directories
                    output.newOutputSocket(e).newOutputStream().close();
                } else if (input.getEntry(n) != null) {
                    assert e == input.getEntry(n);
                    IOSocket.copy(  input.newInputSocket(e),
                                    output.newOutputSocket(e));
                } else {
                    // The file system entry is an archive file which has been
                    // newly created and not yet been reassembled
                    // into this (potentially new) archive file.
                    // Write an empty file system entry now as a marker in
                    // order to recreate the file system entry when the file
                    // system gets remounted from the archive file.
                    output.newOutputSocket(e).newOutputStream().close();
                }
            } catch (IOException ex) {
                handler.warn(ex);
            }
        }
    }

    /**
     * Uses the updated temporary output archive file to reassemble the
     * target archive file, which may be an entry in an enclosing
     * archive file.
     * <p>
     * <b>This method is intended to be called by {@code update()} only!</b>
     *
     * @param handler An exception handler - {@code null} is not permitted.
     * @throws ArchiveSyncException If any exceptional condition occurs
     *         throughout the processing of the target archive file.
     */
    private void reassemble(final ArchiveSyncExceptionHandler handler)
    throws ArchiveSyncException {
        if (isHostedDirectoryEntryTarget()) {
            // The archive file managed by this object is NOT enclosed in
            // another archive file.
            if (outFile != getTarget()) {
                // The archive file existed before and we have written
                // to a temporary output file.
                // Now copy the temporary output file to the target file,
                // preserving the last modification time and counting the
                // output.
                try {
                    final OutputStream out = new CountingOutputStream(
                            new java.io.FileOutputStream(getTarget()));
                    final InputStream in;
                    try {
                        in = new java.io.FileInputStream(outFile);
                    } catch (IOException ex) {
                        out.close();
                        throw ex;
                    }
                    Streams.copy(in , out); // always closes in and out
                } catch (IOException cause) {
                    throw handler.fail(new ArchiveSyncException(
                            this,
                            "could not reassemble archive file - all changes are lost",
                            cause));
                }

                // Set the last modification time of the target archive file
                // to the last modification time of the output archive file,
                // which has been set to the last modification time of the root
                // directory during update(...).
                final long time = outFile.lastModified();
                if (time != 0 && !getTarget().setLastModified(time)) {
                    handler.warn(new ArchiveSyncWarningException(
                            this,
                            "couldn't preserve last modification time"));
                }
            }
        } else {
            // The archive file managed by this archive controller IS
            // enclosed in another archive file.
            try {
                wrap(getEnclController(), getEnclPath(ROOT));
            } catch (IOException ex) {
                throw handler.fail(new ArchiveSyncException(
                        getEnclController(),
                        "could not update archive entry '" + getEnclPath(ROOT) + "' - all changes are lost",
                        ex));
            }
        }
    }

    private void wrap(
            final ArchiveController controller,
            final String path)
    throws IOException {
        assert controller != null;
        assert path != null;
        assert !isRoot(path);

        // Write the updated output archive file as an entry
        // to its enclosing archive file, preserving the
        // last modification time of the root directory as the last
        // modification time of the entry.
        try {
            IOSocket.copy(
                    newInputSocket(outFile),
                    controller.newOutputSocket(path, BitField.of(PRESERVE)));
        } catch (FalsePositiveEntryException cannotHappen) {
            throw new AssertionError(cannotHappen);
        }
    }

    @Override
    @SuppressWarnings("FinalizeDeclaration")
    protected void finalize() throws Throwable {
        try {
            final ArchiveSyncExceptionBuilder handler
                    = new DefaultArchiveSyncExceptionBuilder();
            //shutdownStep1(handler);
            reset2(handler);
            reset3(true);
        } finally {
            super.finalize();
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
                handler.warn(new ArchiveSyncWarningException(
                        UpdatingArchiveController.this, cause));
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
        // closed. This is because the input archive has been presented
        // to output archive as the "source" when it was created and may
        // be using the input archive when its closing to retrieve some
        // meta data information.
        // E.g. with ZIP archive files, the CommonOutputShop copies the postamble
        // from the CommonInputShop when it closes.
        try {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ioe) {
                    handler.warn(new ArchiveSyncException(this, ioe));
                } finally {
                    output = null;
                }
            }
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ioe) {
                    handler.warn(new ArchiveSyncWarningException(this, ioe));
                } finally {
                    input = null;
                }
            }
        }
    }

    /**
     * Cleans up temporary files.
     * 
     * @param deleteOutFile If this parameter is {@code true},
     *        this method also deletes the temporary output file unless it's
     *        the target archive file (i.e. unless the archive file has been
     *        newly created).
     */
    private void reset3(final boolean deleteOutFile) {
        if (inFile != null) {
            final java.io.File file = inFile;
            inFile = null;
            if (file != getTarget()) {
                boolean deleted = file.delete();
                assert deleted;
            }
        }

        if (outFile != null) {
            final FileEntry file = outFile;
            outFile = null;
            if (deleteOutFile) {
                if (file != getTarget()) {
                    boolean deleted = file.delete();
                    assert deleted;
                }
            } else {
                //assert file != target; // may have been newly created
                inFile = file;
                assert file.isFile();
            }
        }

        if (deleteOutFile)
            needsReassembly = false;
    }
}
