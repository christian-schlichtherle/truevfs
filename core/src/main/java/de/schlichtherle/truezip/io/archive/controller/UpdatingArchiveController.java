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

import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.io.archive.input.ArchiveInputStreamSocket;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystems;
import de.schlichtherle.truezip.io.archive.input.ConcurrentArchiveInput;
import de.schlichtherle.truezip.io.archive.output.ConcurrentArchiveOutput;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem.Link;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.Type;
import java.net.URI;
import de.schlichtherle.truezip.io.socket.IOOperations;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.IOOperation;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.input.ArchiveInput;
import de.schlichtherle.truezip.io.archive.output.ArchiveOutput;
import de.schlichtherle.truezip.io.archive.driver.TransientIOException;
import de.schlichtherle.truezip.io.archive.filesystem.VetoableTouchListener;
import de.schlichtherle.truezip.io.archive.output.ArchiveOutputStreamSocket;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.rof.SimpleReadOnlyFile;
import de.schlichtherle.truezip.util.ExceptionHandler;
import de.schlichtherle.truezip.util.concurrent.lock.ReentrantLock;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.schlichtherle.truezip.io.archive.controller.ArchiveSyncOption.*;
import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.ROOT;
import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.Type.DIRECTORY;
import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.Type.FILE;
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
final class UpdatingArchiveController<
        AE extends ArchiveEntry,
        AI extends ArchiveInput<AE>,
        AO extends ArchiveOutput<AE>>
extends FileSystemArchiveController<AE, AI, AO> {

    private static final String CLASS_NAME
            = UpdatingArchiveController.class.getName();
    private static final Logger logger
            = Logger.getLogger(CLASS_NAME, CLASS_NAME);

    /** Prefix for temporary files created by this class. */
    static final String TEMP_FILE_PREFIX = "tzp-ctrl";

    /**
     * Suffix for temporary files created by this class
     * - should <em>not</em> be {@code null} for enhanced unit tests.
     */
    static final String TEMP_FILE_SUFFIX = ".tmp";

    /**
     * This member class makes an archive controller object strongly reachable
     * from any input stream created by instances of this class.
     * This is required in order to ensure that for any prospective archive
     * file at most one archive controller object exists at any time.
     *
     * @see ArchiveControllers#get(URI, URI, ArchiveDriver)
     */
    private final class Input
    extends ConcurrentArchiveInput<AE, AI> {
        Input(AI target) {
            super(target);
        }

        @Override
        protected AI getTarget() {
            return target;
        }
    }

    /**
     * This member class makes an archive controller object strongly reachable
     * from any output stream created by instances of this class.
     * This is required in order to ensure that for any prospective archive
     * file at most one archive controller object exists at any time.
     *
     * @see ArchiveControllers#get(URI, URI, ArchiveDriver)
     */
    private final class Output
    extends ConcurrentArchiveOutput<AE, AO> {
        Output(AO target) {
            super(target);
        }

        @Override
        protected AO getTarget() {
            return target;
        }
    }

    private final class TouchListener implements VetoableTouchListener {
        @Override
        public void touch() throws IOException {
            ensureOutArchive();
            setTouched(true);
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
     * Whether or not nesting this archive file to its enclosing
     * archive file has been deferred.
     */
    private boolean needsReassembly;

    UpdatingArchiveController(
            URI mountPoint,
            URI enclMountPoint,
            ArchiveDriver driver) {
        super(mountPoint, enclMountPoint, driver);
    }

    /**
     * Returns a new concurrent archive input which decorates (wraps) the
     * given non-{@code null} archive input.
     */
    private Input wrap(AI archive) {
        return new Input(archive);
    }

    /**
     * Returns the wrapped archive input or {@code null} if and only if
     * {@code proxy} is {@code null}.
     */
    private AI unwrap(Input proxy) {
        return proxy != null ? proxy.getTarget() : null;
    }

    /**
     * Returns a new concurrent archive input which decorates (wraps) the
     * given non-{@code null} archive input.
     */
    private Output wrap(AO archive) {
        return new Output(archive);
    }

    /**
     * Returns the wrapped archive input or {@code null} if and only if
     * {@code proxy} is {@code null}.
     */
    private AO unwrap(Output proxy) {
        return proxy != null ? proxy.getTarget() : null;
    }

    private ArchiveFileSystem<AE> newArchiveFileSystem()
    throws IOException {
        return ArchiveFileSystems.newArchiveFileSystem(
                getDriver(), vetoableTouchListener);
    }

    private ArchiveFileSystem<AE> newArchiveFileSystem(
            long rootTime,
            boolean readOnly) {
        return ArchiveFileSystems.newArchiveFileSystem(
                unwrap(input), rootTime, getDriver(),
                vetoableTouchListener, readOnly);
    }

    @Override
    void mount(final boolean autoCreate, final boolean createParents)
    throws FalsePositiveException, IOException {
        assert writeLock().isHeldByCurrentThread();
        assert input == null;
        assert outFile == null;
        assert output == null;
        assert getFileSystem() == null;

        // Do the logging part and leave the work to mount0.
        final Object stats[] = { getMountPoint(), autoCreate, createParents };
        logger.log(Level.FINER, "mount.try", stats); // NOI18N
        try {
            mount0(autoCreate, createParents);

            assert writeLock().isHeldByCurrentThread();
            assert autoCreate || input != null;
            assert autoCreate || outFile == null;
            assert autoCreate || output == null;
            assert getFileSystem() != null;
        } catch (IOException ex) {
            // Log at FINER level. This is mostly because of false positives.
            logger.log(Level.FINER, "mount.catch", ex); // NOI18N

            assert writeLock().isHeldByCurrentThread();
            assert input == null;
            assert outFile == null;
            assert output == null;
            assert getFileSystem() == null;

            throw ex;
        } finally {
            logger.log(Level.FINER, "mount.finally", stats); // NOI18N
        }
    }

    private void mount0(final boolean autoCreate, final boolean createParents)
    throws FalsePositiveException, IOException {
        // We need to mount the virtual file system from the input file.
        // and so far we have not successfully opened the input file.
        if (isRfsEntryTarget()) {
            // The target file of this controller is NOT enclosed
            // in another archive file.
            // Test modification time BEFORE opening the input file!
            if (inFile == null)
                inFile = getTarget();
            final long time = inFile.lastModified();
            if (time != 0) {
                // The archive file isExisting.
                // Thoroughly test read-only status BEFORE opening
                // the device file!
                final boolean isReadOnly = !isWritableOrCreatable(inFile);
                try {
                    initInArchive(inFile);
                } catch (IOException ex) {
                    // Wrap cause so that a matching catch block can assume
                    // that it can access the target in the real file system.
                    throw new FalsePositiveException(this, ROOT, ex);
                }
                setFileSystem(newArchiveFileSystem(time, isReadOnly));
            } else if (autoCreate) {
                // The archive file does NOT exist, but we may create
                // it automatically.
                setFileSystem(newArchiveFileSystem());
            } else {
                assert !autoCreate;

                // The archive file does not exist and we may not create it
                // automatically.
                throw new ArchiveEntryNotFoundException(
                        this, ROOT, "may not create archive file");
            }
        } else {
            // The target file of this controller IS (or appears to be)
            // enclosed in another archive file.
            if (inFile == null) {
                unwrap( getEnclController(), getEnclPath(ROOT),
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
                    throw new FileArchiveEntryFalsePositiveException(
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
                        inFile.lastModified(), false));
            }
        }
    }

    private void unwrap(
            final ArchiveController<?, ?, ?> controller,
            final String path,
            final boolean autoCreate,
            final boolean createParents)
    throws FalsePositiveException, IOException {
        assert controller != null;
        //assert !controller.readLock().isLocked();
        //assert !controller.writeLock().isLocked();
        assert path != null;
        assert !isRoot(path);
        assert inFile == null;

        try {
            // We want to allow as much concurrency as possible, so we will
            // write lock the controller only if we need to update it first
            // or the controller's target shall be automatically created.
            final ReentrantLock lock = autoCreate
                    ? controller.writeLock()
                    : controller.readLock();
            controller.readLock().lock();
            if (controller.hasNewData(path) || autoCreate) {
                controller.readLock().unlock();
                class Locker implements IOOperation {
                    public void run() throws IOException {
                        // Update controller if the entry already has new data.
                        // This needs to be done first before we can access the
                        // file system since controller.newInputStream(entryName)
                        // would do the same and controller.update() would
                        // invalidate the file system reference.
                        controller.autoSync(path);

                        // Keep a lock for the actual unwrapping.
                        // If this is an ordinary mounting procedure where the
                        // file system shall not be created automatically, then
                        // we MUST NOT hold a write lock while unwrapping and
                        // mounting the file system.
                        // This is to prevent dead locks when using RAES
                        // encrypted ZIP files with JFileChooser where the user
                        // may be prompted for a password by the EDT while one
                        // of JFileChooser's background file loading threads is
                        // holding a read lock for the same controller and
                        // waiting for the EDT to be accessible in order to
                        // prompt the user for the same controller's target file,
                        // too.
                        lock.lock(); // keep lock upon return
                    }
                } // class Locker
                controller.runWriteLocked(new Locker());
            }
            try {
                unwrapFromLockedController( controller, path,
                                            autoCreate, createParents);
            } finally {
                lock.unlock();
            }
        } catch (DirectoryArchiveEntryFalsePositiveException ex) {
            // We could as well have catched this exception in the inner
            // try-catch block where we access the controller's file system,
            // but then we would still hold the lock on controller, which
            // is not necessary while accessing the file system of its
            // enclosing controller.
            if (controller.getMountPoint().equals(ex.getMountPoint()))
                throw ex; // just created - pass on
            unwrap( controller.getEnclController(),
                    controller.getEnclPath(path),
                    autoCreate, createParents);
        }
    }

    private void unwrapFromLockedController(
            final ArchiveController<?, ?, ?> controller,
            final String path,
            final boolean autoCreate,
            final boolean createParents)
    throws FalsePositiveException, IOException {
        assert controller != null;
        assert controller.readLock().isHeldByCurrentThread() || controller.writeLock().isHeldByCurrentThread();
        assert path != null;
        assert !isRoot(path);
        assert inFile == null;

        final ArchiveFileSystem controllerFileSystem;
        controllerFileSystem = controller.autoMount(createParents);
        final Type type = controllerFileSystem.getType(path);
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
                Streams.copy(   controller.newInputStream0(path),
                                new java.io.FileOutputStream(tmp));
                // Don't keep tmp if this fails: our caller couldn't reproduce
                // the proper exception on a second try!
                try {
                    initInArchive(tmp);
                } catch (IOException ex) {
                    throw new FileArchiveEntryFalsePositiveException(
                            controller, path, ex);
                }
                setFileSystem(newArchiveFileSystem(
                        controllerFileSystem.getLastModified(path),
                        controllerFileSystem.isReadOnly()));
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
            throw new DirectoryArchiveEntryFalsePositiveException(
                    controller, path,
                    new FileNotFoundException("cannot read directories"));
        } else if (autoCreate) {
            assert controller.writeLock().isHeldByCurrentThread();

            // The entry does NOT exist in the enclosing archive
            // file, but we may create it automatically.
            final Link link = controllerFileSystem.mknod(
                    path, FILE, null, createParents);
            // This may fail if e.g. the target file is an RAES
            // encrypted ZIP file and the user cancels password
            // prompting.
            //ensureOutArchive(); // side effect of the following
            final ArchiveFileSystem fileSystem = newArchiveFileSystem();
            assert outFile != null;
            assert output != null;
            // Now try to create the entry in the enclosing controller.
            try {
                link.run();
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
            throw new ArchiveEntryNotFoundException(
                    controller, path, "may not create archive file");
        }
    }

    /**
     * Initializes {@code inArchive} with a newly created
     * {@link ArchiveInput} for reading {@code inFile}.
     *
     * @throws IOException On any I/O related issue with {@code inFile}.
     */
    private void initInArchive(final java.io.File inFile)
    throws IOException {
        assert writeLock().isHeldByCurrentThread();
        assert input == null;

        logger.log(Level.FINEST, "initInArchive.try", inFile); // NOI18N
        try {
            ReadOnlyFile rof = new SimpleReadOnlyFile(inFile);
            try {
                if (isRfsEntryTarget())
                    rof = new CountingReadOnlyFile(rof);
                input = wrap(getDriver().newInput(this, rof));
            } finally {
                // An archive driver could throw a NoClassDefFoundError or
                // similar if the class path is not set up correctly.
                // We are checking success to make sure that we always delete
                // the newly created temp file in case of an error.
                if (input == null)
                    rof.close();
            }
        } catch (IOException ex) {
            logger.log(Level.FINEST, "initInArchive.catch", ex); // NOI18N

            assert input == null;

            throw ex;
        } finally {
            logger.log(Level.FINEST, "initInArchive.finally",
                    input == null ? 0 : input.size()); // NOI18N
        }

        assert input != null;
    }

    @Override
    ArchiveInputStreamSocket<? extends AE> getInputStreamSocket(final AE target)
    throws IOException {
        assert target != null;
        assert readLock().isHeldByCurrentThread() || writeLock().isHeldByCurrentThread();
        assert !hasNewData(target.getName());
        assert target.getType() != DIRECTORY;

        final ArchiveInputStreamSocket<? extends AE> in = input
                .getInputStreamSocket(target);
        assert in != null : "Bad archive driver returned illegal null value for archive entry \"" + target.getName() + '"';
        return in;
    }

    @Override
    ArchiveOutputStreamSocket<? extends AE> getOutputStreamSocket(final AE target)
    throws IOException {
        assert target != null;
        assert writeLock().isHeldByCurrentThread();
        assert !hasNewData(target.getName());
        assert target.getType() != DIRECTORY;

        ensureOutArchive();
        final ArchiveOutputStreamSocket out = output
                .getOutputStreamSocket(target);
        assert out != null : "Bad archive driver returned illegal null value for archive entry: \"" + target.getName() + '"';
        return out;
    }

    private void ensureOutArchive()
    throws IOException {
        assert writeLock().isHeldByCurrentThread();

        if (output != null)
            return;

        java.io.File tmp = outFile;
        if (tmp == null) {
            if (isRfsEntryTarget() && !getTarget().isFile()) {
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
     * {@link ArchiveOutput} for writing {@code outFile}.
     * This method will delete {@code outFile} if it has successfully
     * opened it for overwriting, but failed to write the archive file header.
     *
     * @throws IOException On any I/O related issue with {@code outFile}.
     */
    private void initOutArchive(final java.io.File outFile)
    throws IOException {
        assert writeLock().isHeldByCurrentThread();
        assert output == null;

        logger.log(Level.FINEST, "initOutArchive.try", outFile); // NOI18N
        try {
            OutputStream out = new java.io.FileOutputStream(outFile);
            try {
                // If we are actually writing to the target file,
                // we want to log the byte count.
                if (outFile == getTarget())
                    out = new CountingOutputStream(out);
                try {
                    output = wrap(getDriver().newOutput(
                                this, out, unwrap(input)));
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
                    out.close();
                    if (!outFile.delete())
                        throw new IOException(outFile.getPath() + " (couldn't delete corrupted output file)");
                }
            }
        } catch (IOException ex) {
            logger.log(Level.FINEST, "initOutArchive.catch", ex); // NOI18N

            assert output == null;

            throw ex;
        } finally {
            logger.log(Level.FINEST, "initOutArchive.finally"); // NOI18N
        }

        assert output != null;
    }

    boolean hasNewData(String path) {
        assert readLock().isHeldByCurrentThread() || writeLock().isHeldByCurrentThread();
        if (output == null)
            return false;
        final ArchiveEntry entry = getFileSystem().getEntry(path);
        return entry != null && output.getEntry(entry.getName()) != null;
    }

    public void sync(
            final BitField<ArchiveSyncOption> options,
            final ArchiveSyncExceptionBuilder builder)
    throws ArchiveSyncException {
        assert options.get(CLOSE_INPUT_STREAMS) || !options.get(CLOSE_OUTPUT_STREAMS); // closeOutputStreams => closeInputStreams
        assert !options.get(UMOUNT) || options.get(REASSEMBLE); // sync => reassemble
        assert writeLock().isHeldByCurrentThread();
        assert input == null || inFile != null; // input archive => input file
        assert !isTouched() || output != null; // file system touched => output archive
        assert output == null || outFile != null; // output archive => output file

        // Do the logging part and leave the work to sync0.
        final Object[] stats = new Object[] {
            getMountPoint(),
            options.get(WAIT_FOR_INPUT_STREAMS),
            options.get(CLOSE_INPUT_STREAMS),
            options.get(WAIT_FOR_OUTPUT_STREAMS),
            options.get(CLOSE_OUTPUT_STREAMS),
            options.get(UMOUNT),
            options.get(REASSEMBLE),
        };
        logger.log(Level.FINER, "sync.try", stats); // NOI18N
        try {
            sync0(options, builder);
        } catch (ArchiveSyncException ex) {
            logger.log(Level.FINER, "sync.catch", ex); // NOI18N
            throw ex;
        } finally {
            logger.log(Level.FINER, "sync.finally", stats); // NOI18N
        }
    }

    private void sync0(
            final BitField<ArchiveSyncOption> options,
            final ArchiveSyncExceptionBuilder builder)
    throws ArchiveSyncException {
        // Check output streams first, because closeInputStreams may be
        // true and closeOutputStreams may be false in which case we
        // don't even need to check open input streams if there are
        // some open output streams.
        if (output != null) {
            final int outStreams = output.waitCloseAllOutputStreams(
                    options.get(WAIT_FOR_OUTPUT_STREAMS) ? 0 : 50);
            if (outStreams > 0) {
                if (!options.get(CLOSE_OUTPUT_STREAMS))
                    throw builder.fail(new ArchiveOutputBusyException(
                            this, outStreams));
                builder.warn(new ArchiveOutputBusyWarningException(
                        this, outStreams));
            }
        }
        if (input != null) {
            final int inStreams = input.waitCloseAllInputStreams(
                    options.get(WAIT_FOR_INPUT_STREAMS) ? 0 : 50);
            if (inStreams > 0) {
                if (!options.get(CLOSE_INPUT_STREAMS))
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

        // Now update the archive.
        try {
            if (isTouched()) {
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
            setTouched(needsReassembly);
        }

        builder.check();
    }

    final int waitAllInputStreamsByOtherThreads(long timeout) {
        return input != null
                ? input.waitCloseAllInputStreams(timeout)
                : 0;
    }

    final int waitAllOutputStreamsByOtherThreads(long timeout) {
        return output != null
                ? output.waitCloseAllOutputStreams(timeout)
                : 0;
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
        assert writeLock().isHeldByCurrentThread();
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

        final long rootTime = getFileSystem().getLastModified(ROOT);
        try {
            try {
                shutdownStep1(handler);
                copy(new FilterExceptionHandler(handler));
            } finally {
                // We MUST do cleanup here because (1) any entries in the
                // filesystem which were successfully written (this is the
                // normal case) have been modified by the ArchiveOutput
                // and thus cannot get used anymore to access the input;
                // and (2) if there has been any IOException on the
                // output archive there is no way to recover from it.
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
        for (final ArchiveEntry entry : output) {
            assert entry.getType() != DIRECTORY;
            // At this point in time we could have written only file archive
            // entries with valid path names, so the following test should be
            // enough:
            final String path = entry.getName();
            //final String path = normalize(entry.getName(), SEPARATOR_CHAR);
            if (fileSystem.getType(path) == null) {
                // The entry has been written out already, but also
                // has been deleted from the master directory meanwhile.
                // Create a warn exception, but do not yet throw it.
                handler.warn(new ArchiveSyncWarningException(
                        this, "couldn't remove archive entry '" + path + "'"));
            }
        }
        return true;
    }

    public <E extends Exception>
    void copy(final ExceptionHandler<IOException, E> h)
    throws E {
        final ArchiveInput<AE> in = unwrap(input);
        final ArchiveOutput<AE> out = unwrap(output);
        final ArchiveFileSystem<AE> fs = getFileSystem();
        final AE root = fs.getEntry(ROOT);
        assert root != null;
        for (final AE e : fs) {
            final String n = e.getName();
            if (out.getEntry(n) != null)
                continue; // we have already written this entry
            try {
                if (e.getType() == DIRECTORY) {
                    if (root == e)
                        continue; // never write the virtual root directory
                    if (e.getTime() < 0)
                        continue; // never write ghost directories
                    out.getOutputStreamSocket(e).newOutputStream(null).close();
                } else if (in != null && in.getEntry(n) != null) {
                    assert e == in.getEntry(n);
                    IOOperations.copy(  in.getInputStreamSocket(e),
                                        out.getOutputStreamSocket(e));
                } else {
                    // The file system entry is an archive file which has been
                    // newly created and not yet been reassembled
                    // into this (potentially new) archive file.
                    // Write an empty file system entry now as a marker in
                    // order to recreate the file system entry when the file
                    // system gets remounted from the archive file.
                    out.getOutputStreamSocket(e).newOutputStream(null).close();
                }
            } catch (IOException ex) {
                h.warn(ex);
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
        assert writeLock().isHeldByCurrentThread();

        if (isRfsEntryTarget()) {
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
            final ArchiveController<?, ?, ?> controller,
            final String path)
    throws IOException {
        assert writeLock().isHeldByCurrentThread();
        assert controller != null;
        //assert !controller.readLock().isLocked();
        //assert !controller.writeLock().isLocked();
        assert path != null;
        assert !isRoot(path);

        class Wrapper implements IOOperation {
            public void run() throws IOException {
                wrapToWriteLockedController(controller, path);
            }
        }
        controller.runWriteLocked(new Wrapper());
    }

    private void wrapToWriteLockedController(
            final ArchiveController<?, ?, ?> controller,
            final String path)
    throws IOException {
        assert controller != null;
        assert controller.writeLock().isHeldByCurrentThread();
        assert path != null;
        assert !isRoot(path);

        // Write the updated output archive file as an entry
        // to its enclosing archive file, preserving the
        // last modification time of the root directory as the last
        // modification time of the entry.
        final InputStream in = new java.io.FileInputStream(outFile);
        try {
            ArchiveControllers.copy(true, false, outFile, in, controller, path);
        } catch (FalsePositiveException cannotHappen) {
            throw new AssertionError(cannotHappen);
        } finally {
            in.close();
        }
    }

    /**
     * Resets the archive controller to its initial state - all changes to the
     * archive file which have not yet been updated get lost!
     * <p>
     * Thereafter, the archive controller will behave as if it has just been
     * created and any subsequent operations on its entries will remount
     * the virtual file system from the archive file again.
     *
     * @param handler An exception handler - {@code null} is not permitted.
     * @throws ArchiveSyncException If any exceptional condition occurs
     *         throughout the processing of the target archive file.
     */
    @Override
    void reset(final ArchiveSyncExceptionHandler handler)
    throws ArchiveSyncException {
        assert writeLock().isHeldByCurrentThread();

        try {
            shutdownStep1(handler);
        } finally {
            shutdownStep2(handler);
        }
        shutdownStep3(true);
        setTouched(false);
    }

    @Override
    @SuppressWarnings("FinalizeDeclaration")
    protected void finalize() throws Throwable {
        logger.log(Level.FINEST, "finalize.try", getMountPoint()); // NOI18N
        try {
            // Note: If fileSystem or inArchive are not null, then the controller
            // has been used to perform read operations.
            // If outArchive is not null, the controller has been used to perform
            // write operations, but however, all file system transactions
            // must have failed.
            // Otherwise, the fileSystem would have been marked as touched and
            // this object should never be made elegible for finalization!
            // Tactical note: Assertions don't work in a finalizer, so we use
            // logging.
            if (    isTouched()
                    || readLock().isHeldByCurrentThread()
                    || writeLock().isHeldByCurrentThread())
                logger.log(Level.SEVERE, "finalize.invalidState", getMountPoint());
            final ArchiveSyncExceptionBuilder handler
                    = new DefaultArchiveSyncExceptionBuilder();
            shutdownStep1(handler);
            shutdownStep2(handler);
            shutdownStep3(true);
        } catch (IOException ex) {
            logger.log(Level.FINEST, "finalize.catch", ex);
        } finally {
            logger.log(Level.FINEST, "finalize.finally", getMountPoint());

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
                if (cause == null)
                    throw new NullPointerException();
                handler.warn(new ArchiveSyncWarningException(
                        UpdatingArchiveController.this, cause));
            }
        } // class FilterExceptionHandler
        final FilterExceptionHandler decoratedHandler = new FilterExceptionHandler();
        if (output != null)
            output.closeAllOutputStreams(decoratedHandler);
        if (input != null)
            input.closeAllInputStreams(decoratedHandler);
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
        super.reset(handler); // discard file system

        // The output archive must be closed BEFORE the input archive is
        // closed. This is because the input archive has been presented
        // to output archive as the "source" when it was created and may
        // be using the input archive when its closing to retrieve some
        // meta data information.
        // E.g. with ZIP archive files, the ArchiveOutput copies the postamble
        // from the ArchiveInput when it closes.
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
            if (inFile != getTarget()) {
                boolean deleted = inFile.delete();
                assert deleted;
            }
            inFile = null;
        }

        if (outFile != null) {
            if (deleteOutFile) {
                if (outFile != getTarget()) {
                    boolean deleted = outFile.delete();
                    assert deleted;
                }
            } else {
                //assert outFile != target; // may have been newly created
                assert outFile.isFile();
                inFile = outFile;
            }
            outFile = null;
        }

        if (deleteOutFile)
            needsReassembly = false;
    }
}
