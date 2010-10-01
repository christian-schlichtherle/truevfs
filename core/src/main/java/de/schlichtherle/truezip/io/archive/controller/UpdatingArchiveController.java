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

import de.schlichtherle.truezip.io.socket.input.FilterInputSocket;
import java.util.Collections;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry.Access;
import de.schlichtherle.truezip.io.socket.file.FileEntry;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry;
import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem.Entry;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.io.socket.input.CommonInputSocket;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystems;
import de.schlichtherle.truezip.io.socket.input.ConcurrentInputShop;
import de.schlichtherle.truezip.io.socket.output.ConcurrentOutputShop;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry.Type;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.io.socket.input.CommonInputShop;
import de.schlichtherle.truezip.io.socket.output.CommonOutputShop;
import de.schlichtherle.truezip.io.archive.driver.TransientIOException;
import de.schlichtherle.truezip.io.archive.filesystem.VetoableTouchListener;
import de.schlichtherle.truezip.io.socket.output.CommonOutputSocket;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.socket.file.FileIOProvider;
import de.schlichtherle.truezip.util.ExceptionHandler;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.IOOption.CREATE_PARENTS;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.ABORT_CHANGES;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.CLOSE_INPUT;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.CLOSE_OUTPUT;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.REASSEMBLE;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.UMOUNT;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.WAIT_CLOSE_INPUT;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.WAIT_CLOSE_OUTPUT;
import static de.schlichtherle.truezip.io.archive.driver.ArchiveEntry.ROOT;
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
extends FileSystemArchiveController<AE> {

    /** Prefix for temporary files created by this class. */
    static final String TEMP_FILE_PREFIX = "tzp-ctrl";

    /**
     * Suffix for temporary files created by this class
     * - should <em>not</em> be {@code null} for enhanced unit tests.
     */
    static final String TEMP_FILE_SUFFIX = ".tmp";

    private static class DummyInputService<CE extends CommonEntry>
    implements CommonInputShop<CE> {

        @Override
        public void close() throws IOException {
        }

        @Override
        public int size() {
            return 0;
        }

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
     * This member class makes an archive controller object strongly reachable
     * from any input stream created by instances of this class.
     * This is required in order to ensure that for any prospective archive
     * file at most one archive controller object exists at any time.
     *
     * @see ArchiveControllers#getController(URI, ArchiveDriver, ArchiveController)
     */
    private final class Input extends ConcurrentInputShop<AE> {
        Input(CommonInputShop<AE> target) {
            super(target);
            setSticky(true); // FIXME: This is a hack: It will prevent an archive controller from being garbage collected even if only input was done!
        }

        CommonInputShop<AE> getTarget() {
            return (CommonInputShop<AE>) target;
        }
    }

    /**
     * This member class makes an archive controller object strongly reachable
     * from any output stream created by instances of this class.
     * This is required in order to ensure that for any prospective archive
     * file at most one archive controller object exists at any time.
     *
     * @see ArchiveControllers#getController(URI, ArchiveDriver, ArchiveController)
     */
    private final class Output extends ConcurrentOutputShop<AE> {
        Output(CommonOutputShop<AE> target) {
            super(target);
            setSticky(true);
        }

        CommonOutputShop<AE> getTarget() {
            return (CommonOutputShop<AE>) target;
        }
    }

    private final class TouchListener implements VetoableTouchListener {
        @Override
        public void touch() throws IOException {
            ensureOutArchive();
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
    private java.io.File inFile;

    /**
     * An {@link Input} object used to mount the virtual file system
     * and read the entries from the archive file.
     */
    private Input input;

    /**
     * Plain {@code java.io.File} object used for temporary output.
     * Maybe identical to {@code inFile}.
     */
    private java.io.File outFile;

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

    UpdatingArchiveController(ArchiveModel<AE> model) {
        super(model);
    }

    /**
     * Returns the wrapped archive input or {@code null} if and only if
     * {@code proxy} is {@code null}.
     */
    private CommonInputShop<AE> getNullableInputTarget() {
        return null == input ? null : input.getTarget();
    }

    private CommonInputShop<AE> getNonNullInputTarget() {
        return null == input ? new DummyInputService<AE>() : input.getTarget();
    }

    private ArchiveFileSystem newArchiveFileSystem()
    throws IOException {
        return ArchiveFileSystems.newArchiveFileSystem(
                getDriver(), vetoableTouchListener);
    }

    private ArchiveFileSystem newArchiveFileSystem(
            CommonEntry rootTemplate,
            boolean readOnly) {
        return ArchiveFileSystems.newArchiveFileSystem(
                input.getTarget(), getDriver(),
                rootTemplate, vetoableTouchListener, readOnly);
    }

    // FIXME: Remove this hack!
    private static ArchiveController getEnclController(ArchiveController controller) {
        return ArchiveControllers.getController(controller.getModel().getEnclMountPoint());
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
        if (isHostFileSystemEntryTarget()) {
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
                    initInArchive(inFile);
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
                unwrap( getEnclController(this), getEnclPath(ROOT),
                        autoCreate, createParents);
            } else {
                // The enclosed archive file has already been updated and the
                // file previously used for output has been left over to be
                // reused as our input in order to skip the lengthy process
                // of searching for the right enclosing archive controller
                // to extract the entry which is our target archive file.
                try {
                    initInArchive(inFile);
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
                            getEnclController(this), getEnclPath(ROOT), ex);
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
            final java.io.File tmp = createTempFile(
                    TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
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
                    initInArchive(tmp);
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
            final ArchiveFileSystem fileSystem = newArchiveFileSystem();
            assert outFile != null;
            assert output != null;
            // Now try to create the entry in the enclosing controller.
            try {
                controller.mknod(path, FILE, null,
                        BitField.noneOf(IOOption.class).set(CREATE_PARENTS, createParents));
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

    /**
     * Initializes {@code inArchive} with a newly created
     * {@link CommonInputShop} for reading {@code inFile}.
     *
     * @throws IOException On any I/O related issue with {@code inFile}.
     */
    private void initInArchive(final java.io.File inFile)
    throws IOException {
        assert input == null;

        try {
            class InputSocket extends FilterInputSocket<FileEntry> {
                InputSocket() throws IOException {
                    super(FileIOProvider.get().newInputSocket(new FileEntry(inFile)));
                }

                @Override
                public ReadOnlyFile newReadOnlyFile() throws IOException {
                    final ReadOnlyFile rof = super.newReadOnlyFile();
                    return isHostFileSystemEntryTarget()
                            ? new CountingReadOnlyFile(rof)
                            : rof;
                }
            }
            input = new Input(getDriver().newInputShop(this, new InputSocket()));
        } catch (IOException ex) {
            assert input == null;

            throw ex;
        }
        assert input != null;
    }

    @Override
    public CommonInputSocket<AE> newInputSocket(final AE target)
    throws IOException {
        return null == input ? null : input.newInputSocket(target);
    }

    @Override
    public CommonOutputSocket<AE> newOutputSocket(final AE target)
    throws IOException {
        ensureOutArchive();
        return output.newOutputSocket(target);
    }

    private void ensureOutArchive()
    throws IOException {
        if (null != output)
            return;

        java.io.File tmp = outFile;
        if (tmp == null) {
            if (isHostFileSystemEntryTarget() && !getTarget().isFile()) {
                tmp = getTarget();
            } else {
                // Use a new temporary file as the output archive file.
                tmp = createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
                // We do properly delete our temps, so this is not required.
                // In addition, this would be dangerous as the deletion
                // could happen before our shutdown hook has a chance to
                // process this controller!!!
                //tmp.deleteOnExit();
            }
        }

        initOutArchive(tmp);
        outFile = tmp; // init outFile on success only!
    }

    /**
     * Initializes {@code outArchive} with a newly created
     * {@link CommonOutputShop} for writing {@code outFile}.
     * This method will delete {@code outFile} if it has successfully
     * opened it for overwriting, but failed to write the archive file header.
     *
     * @throws IOException On any I/O related issue with {@code outFile}.
     */
    private void initOutArchive(final java.io.File outFile)
    throws IOException {
        assert output == null;

        try {
            final FileEntry entry = new FileEntry(outFile);
            final CommonOutputSocket<FileEntry> fileInput
                    = FileIOProvider.get().newOutputSocket(entry);
            class OutputSocket extends CommonOutputSocket<FileEntry> {
                @Override
                public FileEntry getTarget() {
                    return entry;
                }

                @Override
                public OutputStream newOutputStream() throws IOException {
                    final OutputStream out = fileInput.newOutputStream();
                    return outFile == UpdatingArchiveController.this.getTarget()
                            ? new CountingOutputStream(out)
                            : out;
                }
            }
            try {
                try {
                    output = new Output(getDriver().newOutputShop(
                                this, new OutputSocket(), getNullableInputTarget()));
                } catch (TransientIOException ex) {
                    // Currently we do not have any use for this wrapper exception
                    // when creating output archives, so we unwrap the transient
                    // cause here.
                    throw ex.getCause();
                }
            } finally {
                // An archive driver could throw a NoClassDefFoundError or
                // similar if the class path is not set up correctly.
                // We are checking success to make sure that we always delete
                // the newly created temp file in case of an error.
                if (output == null) {
                    if (!outFile.delete())
                        throw new IOException(outFile.getPath() + " (couldn't delete corrupted output file)");
                }
            }
        } catch (IOException ex) {
            assert output == null;

            throw ex;
        }

        assert output != null;
    }

    @Override
    public boolean hasNewData(String path) {
        if (output == null)
            return false;
        final Entry entry = getFileSystem().getEntry(path);
        return entry != null && output.getEntry(entry.getName()) != null;
    }

    final int waitCloseOtherInputs(long timeout) {
        return null == input ? 0 : input.waitCloseOthers(timeout);
    }

    final int waitCloseOtherOutputs(long timeout) {
        return null == output ? 0 : output.waitCloseOthers(timeout);
    }

    public void sync(   final ArchiveSyncExceptionBuilder builder,
                        final BitField<SyncOption> options)
    throws ArchiveSyncException {
        assert input == null || inFile != null; // input archive => input file
        assert !isTouched() || output != null; // file system touched => output archive
        assert output == null || outFile != null; // output archive => output file

        if (options.get(CLOSE_OUTPUT) && !options.get(CLOSE_INPUT))
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
                if (!options.get(CLOSE_OUTPUT))
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
                if (!options.get(CLOSE_INPUT))
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
                    shutdownStep1(builder);
                } finally {
                    shutdownStep2(builder);
                }
                shutdownStep3(true); // TODO: Check: Why not in another finally-block?
            } else if (isTouched()) {
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
                    shutdownStep3(options.get(UMOUNT) && !needsReassembly);
                }
            } else if (options.get(REASSEMBLE) && needsReassembly) {
                // Nesting this archive file to its enclosing archive file
                // has been deferred until now.
                assert outFile == null; // isTouched() otherwise!
                assert inFile != null; // !needsReassembly otherwise!
                // Beware: inArchive or fileSystem may be initialized!
                shutdownStep2(builder);
                outFile = inFile;
                inFile = null;
                try {
                    reassemble(builder);
                    needsReassembly = false;
                } finally {
                    shutdownStep3(options.get(UMOUNT) && !needsReassembly);
                }
            } else if (options.get(UMOUNT)) {
                assert options.get(REASSEMBLE);
                assert !needsReassembly;
                shutdownStep2(builder);
                shutdownStep3(true);
            } else {
                // This may happen if File.update() or File.sync() has
                // been called and no modifications have been applied to
                // this ArchiveController since its creation or last update.
                assert output == null;
            }
        } catch (ArchiveSyncException ex) {
            throw ex;
        } catch (IOException ex) {
            throw builder.fail(new ArchiveSyncException(this, ex));
        } finally {
            setSticky(needsReassembly);
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
        assert isTouched();
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

            public ArchiveSyncException fail(final IOException cannotHappen) {
                throw new AssertionError(cannotHappen);
            }

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
                shutdownStep1(handler);
                copy((ExceptionHandler<IOException, ArchiveSyncException>) new FilterExceptionHandler(handler));
            } finally {
                // We MUST do cleanup here because (1) any entries in the
                // filesystem which were successfully written (this is the
                // normal case) have been modified by the CommonOutputShop
                // and thus cannot get used anymore to access the input;
                // and (2) if there has been any IOException on the
                // output archive there is no way to recover.
                shutdownStep2(handler);
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
        assert isTouched();
        assert getFileSystem() != null;

        // Check if we have written out any entries that have been
        // deleted from the archive file system meanwhile and prepare
        // to throw a warning exception.
        final ArchiveFileSystem fileSystem = getFileSystem();
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
        getFileSystem().copy(getNonNullInputTarget(), output.getTarget(), handler);
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
        if (isHostFileSystemEntryTarget()) {
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
                wrap(getEnclController(this), getEnclPath(ROOT));
            } catch (IOException ex) {
                throw handler.fail(new ArchiveSyncException(
                        getEnclController(this),
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

        ensureWriteLockedByCurrentThread();
        // Write the updated output archive file as an entry
        // to its enclosing archive file, preserving the
        // last modification time of the root directory as the last
        // modification time of the entry.
        final InputStream in = new java.io.FileInputStream(outFile);
        try {
            ArchiveControllers.copy(true, false, outFile, in, controller, path);
        } catch (FalsePositiveEntryException cannotHappen) {
            throw new AssertionError(cannotHappen);
        } finally {
            in.close();
        }
    }

    @Override
    @SuppressWarnings("FinalizeDeclaration")
    protected void finalize() throws Throwable {
        try {
            final ArchiveSyncExceptionBuilder handler
                    = new DefaultArchiveSyncExceptionBuilder();
            //shutdownStep1(handler);
            shutdownStep2(handler);
            shutdownStep3(true);
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
    private void shutdownStep1(final ArchiveSyncExceptionHandler handler)
    throws ArchiveSyncException {
        class FilterExceptionHandler
        implements ExceptionHandler<IOException, ArchiveSyncException> {
            public ArchiveSyncException fail(IOException cannotHappen) {
                throw new AssertionError(cannotHappen);
            }

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
    private void shutdownStep2(final ArchiveSyncExceptionHandler handler)
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
    private void shutdownStep3(final boolean deleteOutFile) {
        if (inFile != null) {
            final java.io.File file = inFile;
            inFile = null;
            if (file != getTarget()) {
                boolean deleted = file.delete();
                assert deleted;
            }
        }

        if (outFile != null) {
            final java.io.File file = outFile;
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
