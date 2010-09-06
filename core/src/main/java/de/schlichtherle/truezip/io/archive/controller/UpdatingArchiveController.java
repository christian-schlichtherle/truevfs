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

import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.IOOperation;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.driver.InputArchive;
import de.schlichtherle.truezip.io.archive.driver.OutputArchive;
import de.schlichtherle.truezip.io.archive.driver.TransientIOException;
import de.schlichtherle.truezip.io.archive.filesystem.VetoableTouchListener;
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

import static de.schlichtherle.truezip.io.archive.driver.ArchiveEntry.ROOT;
import static de.schlichtherle.truezip.io.archive.driver.ArchiveEntry.Type.DIRECTORY;
import static de.schlichtherle.truezip.io.archive.driver.ArchiveEntry.Type.FILE;
import static de.schlichtherle.truezip.io.Files.isWritableOrCreatable;
import static de.schlichtherle.truezip.io.Files.createTempFile;

/**
 * This archive controller implements the mounting/unmounting strategy
 * by performing a full update of the target archive file.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class UpdatingArchiveController extends FileSystemArchiveController {

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

    private class TouchListener implements VetoableTouchListener {
        @Override
        public void touch() throws IOException {
            UpdatingArchiveController.this.touch();
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
     * An {@link InputArchive} object used to mount the virtual file system
     * and read the entries from the archive file.
     */
    private InputArchive<?> inArchive;

    /**
     * Plain {@code java.io.File} object used for temporary output.
     * Maybe identical to {@code inFile}.
     */
    private java.io.File outFile;

    /**
     * The (possibly temporary) {@link OutputArchive} we are writing newly
     * created or modified entries to.
     */
    private OutputArchive<?> outArchive;

    /**
     * Whether or not nesting this archive file to its enclosing
     * archive file has been deferred.
     */
    private boolean needsReassembly;

    UpdatingArchiveController(
            java.io.File target,
            ArchiveController enclController,
            String enclEntryName,
            ArchiveDriver driver) {
        super(target, enclController, enclEntryName, driver);
    }

    @Override
    void mount(final boolean autoCreate)
    throws FalsePositiveException, IOException {
        assert writeLock().isLockedByCurrentThread();
        assert inArchive == null;
        assert outFile == null;
        assert outArchive == null;
        assert getFileSystem() == null;

        // Do the logging part and leave the work to mount0.
        logger.log(Level.FINER, "mount.entering", // NOI18N
                new Object[] {
                    getCanonicalPath(),
                    Boolean.valueOf(autoCreate),
        });
        try {
            mount0(autoCreate);
        } catch (IOException ioe) {
            assert writeLock().isLockedByCurrentThread();
            assert inArchive == null;
            assert outFile == null;
            assert outArchive == null;
            assert getFileSystem() == null;

            // Log at FINER level. This is mostly because of false positives.
            logger.log(Level.FINER, "mount.throwing", ioe); // NOI18N
            throw ioe;
        }
        logger.log(Level.FINER, "mount.exiting"); // NOI18N

        assert writeLock().isLockedByCurrentThread();
        assert autoCreate || inArchive != null;
        assert autoCreate || outFile == null;
        assert autoCreate || outArchive == null;
        assert getFileSystem() != null;
    }

    private void mount0(final boolean autoCreate)
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
                    throw new FalsePositiveException(this, ex);
                }
                setFileSystem(new ArchiveFileSystem(
                        getDriver(), vetoableTouchListener, inArchive,
                        time, isReadOnly));
            } else if (!autoCreate) {
                // The archive file does not exist and we may not create it
                // automatically.
                throw new ArchiveFileNotFoundException(this);
            } else {
                // The archive file does NOT exist, but we may create
                // it automatically.
                // Setup output first to implement fail-fast behavior.
                // This may fail e.g. if the target file is a RAES
                // encrypted ZIP file and the user cancels password
                // prompting.
                setFileSystem(new ArchiveFileSystem(
                        getDriver(), vetoableTouchListener));
            }
        } else {
            // The target file of this controller IS (or appears to be)
            // enclosed in another archive file.
            if (inFile == null) {
                unwrap(getEnclController(), getEnclEntryName(), autoCreate);
            } else {
                // The enclosed archive file has already been updated and the
                // file previously used for output has been left over to be
                // reused as our input in order to skip the lengthy process
                // of searching for the right enclosing archive controller
                // to extract the entry which is our target.
                try {
                    initInArchive(inFile);
                } catch (IOException ex) {
                    // This is very unlikely unless someone has tampered with
                    // the temporary file or this controller is managing an
                    // RAES encrypted ZIP file and the client application has
                    // inadvertently called KeyManager.resetKeyProviders() or
                    // similar and the subsequent repetitious prompting for
                    // the key has unfortunately been cancelled by the user.
                    // Now the next problem is that we cannot always generate
                    // a false positive exception with the correct enclosing
                    // controller because we haven't searched for it.
                    // Anyway, this is so unlikely that we simply throw a
                    // false positive exception and cross fingers that the
                    // controller and entry name information will not be used.
                    // When assertions are enabled, we prefer to treat this as
                    // a bug.
                    assert false : "We should never get here! Read the source code comments for full details.";
                    throw new FileArchiveEntryFalsePositiveException(
                            this,
                            getEnclController(), // probably not correct!
                            getEnclEntryName(), // dito
                            ex);
                }
                // Note that the archive file system must be read-write
                // because we are reusing a file which has been previously
                // used to output modifications to it!
                // Similarly, the last modification time of the left over
                // output file has been set to the last modification time of
                // its virtual root directory.
                // Nice trick, isn't it?!
                setFileSystem(new ArchiveFileSystem(
                        getDriver(), vetoableTouchListener, inArchive,
                        inFile.lastModified(), false));
            }
        }
    }

    private void unwrap(
            final ArchiveController controller,
            final String entryName,
            final boolean autoCreate)
    throws FalsePositiveException, IOException {
        assert controller != null;
        //assert !controller.readLock().isLocked();
        //assert !controller.writeLock().isLocked();
        assert entryName != null;
        assert !isRoot(entryName);
        assert inFile == null;

        try {
            // We want to allow as much concurrency as possible, so we will
            // write lock the controller only if we need to update it first
            // or the controller's target shall be automatically created.
            final ReentrantLock lock = autoCreate
                    ? controller.writeLock()
                    : controller.readLock();
            controller.readLock().lock();
            if (controller.hasNewData(entryName) || autoCreate) {
                controller.readLock().unlock();
                class Locker implements IOOperation {
                    public void run() throws IOException {
                        // Update controller if the entry already has new data.
                        // This needs to be done first before we can access the
                        // file system since controller.newInputStream(entryName)
                        // would do the same and controller.update() would
                        // invalidate the file system reference.
                        controller.autoUmount(entryName);

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
                unwrapFromLockedController(controller, entryName, autoCreate);
            } finally {
                lock.unlock();
            }
        } catch (DirectoryArchiveEntryFalsePositiveException ex) {
            // We could as well have catched this exception in the inner
            // try-catch block where we access the controller's file system,
            // but then we would still hold the lock on controller, which
            // is not necessary while accessing the file system of its
            // enclosing controller.
            if (ex.getEnclController() == controller)
                throw ex; // just created - pass on

            unwrap( controller.getEnclController(),
                    controller.enclEntryName(entryName),
                    autoCreate);
        }
    }

    private void unwrapFromLockedController(
            final ArchiveController controller,
            final String path,
            final boolean autoCreate)
    throws FalsePositiveException, IOException {
        assert controller != null;
        assert controller.readLock().isLockedByCurrentThread() || controller.writeLock().isLockedByCurrentThread();
        assert path != null;
        assert !isRoot(path);
        assert inFile == null;

        final ArchiveFileSystem controllerFileSystem;
        controllerFileSystem = controller.autoMount(
                autoCreate && ArchiveControllers.isLenient());
        if (controllerFileSystem.isFile(path)) {
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
                Streams.copy( controller.newInputStream0(path),
                            new java.io.FileOutputStream(tmp));
                // Don't keep tmp if this fails: our caller couldn't reproduce
                // the proper exception on a second try!
                try {
                    initInArchive(tmp);
                } catch (IOException ex) {
                    throw new FileArchiveEntryFalsePositiveException(
                            this, controller, path, ex);
                }
                setFileSystem(new ArchiveFileSystem(
                        getDriver(), vetoableTouchListener, inArchive,
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
        } else if (controllerFileSystem.isDirectory(path)) {
            throw new DirectoryArchiveEntryFalsePositiveException(
                    this, controller, path,
                    new FileNotFoundException("cannot read directories"), this);
        } else if (!autoCreate) {
            // The entry does NOT exist in the enclosing archive
            // file and we may not create it automatically.
            throw new EnclosedArchiveFileNotFoundException(this);
        } else {
            assert autoCreate;
            assert controller.writeLock().isLockedByCurrentThread();

            // The entry does NOT exist in the enclosing archive
            // file, but we may create it automatically.
            // TODO: Document this: Why do we need to pass File.isLenient()
            // instead of just true?
            final IOOperation link = controllerFileSystem.link(
                    path, FILE, ArchiveControllers.isLenient());

            // This may fail if e.g. the target file is an RAES
            // encrypted ZIP file and the user cancels password
            // prompting.
            ensureOutArchive();

            // Now try to create the entry in the enclosing controller.
            try {
                link.run();
            } catch (IOException ex) {
                // The delta on the *enclosing* controller failed.
                // Hence, we need to revert our state changes.
                try {
                    try {
                        outArchive.close();
                    } finally {
                        outArchive = null;
                    }
                } finally {
                    boolean deleted = outFile.delete();
                    assert deleted;
                    outFile = null;
                }

                throw ex;
            }

            setFileSystem(new ArchiveFileSystem(
                    getDriver(), vetoableTouchListener));
        }
    }

    /**
     * Initializes {@code inArchive} with a newly created
     * {@link InputArchive} for reading {@code inFile}.
     *
     * @throws IOException On any I/O related issue with {@code inFile}.
     */
    private void initInArchive(final java.io.File inFile)
    throws IOException {
        assert writeLock().isLockedByCurrentThread();
        assert inArchive == null;

        logger.log(Level.FINEST, "initInArchive.entering", inFile); // NOI18N
        try {
            ReadOnlyFile rof = new SimpleReadOnlyFile(inFile);
            try {
                if (isRfsEntryTarget())
                    rof = new CountingReadOnlyFile(rof);
                inArchive = getDriver().newInputArchive(this, rof);
            } finally {
                // An archive driver could throw a NoClassDefFoundError or
                // similar if the class path is not set up correctly.
                // We are checking success to make sure that we always delete
                // the newly created temp file in case of an error.
                if (inArchive == null)
                    rof.close();
            }
            inArchive.setMetaData(new InputArchiveMetaData(this, inArchive));
        } catch (IOException ex) {
            assert inArchive == null;
            logger.log(Level.FINEST, "initInArchive.throwing", ex); // NOI18N
            throw ex;
        }
        logger.log(Level.FINEST, "initInArchive.exiting", inArchive.size()); // NOI18N

        assert inArchive != null;
    }

    InputStream newInputStream(
            final ArchiveEntry entry,
            final ArchiveEntry dstEntry)
    throws IOException {
        assert entry != null;
        assert readLock().isLockedByCurrentThread() || writeLock().isLockedByCurrentThread();
        assert !hasNewData(entry.getName());
        assert entry.getType() != DIRECTORY;

        final InputStream in
                = inArchive.getMetaData().newInputStream(entry, dstEntry);
        assert in != null : "Bad archive driver returned illegal null value for archive entry \"" + entry.getName() + '"';
        return in;
    }

    OutputStream newOutputStream(
            final ArchiveEntry entry,
            final ArchiveEntry srcEntry)
    throws IOException {
        assert entry != null;
        assert writeLock().isLockedByCurrentThread();
        assert !hasNewData(entry.getName());
        assert entry.getType() != DIRECTORY;

        ensureOutArchive();
        final OutputStream out
                = outArchive.getMetaData().newOutputStream(entry, srcEntry);
        assert out != null : "Bad archive driver returned illegal null value for archive entry: \"" + entry.getName() + '"';
        return out;
    }

    /**
     * Ensures the output archive is set up and sets the touch status to
     * {@code true}.
     * <p>
     * <b>Warning:</b> The write lock of this controller must be acquired
     * while this method is called!
     */
    private void touch() throws IOException {
        assert writeLock().isLockedByCurrentThread();
        ensureOutArchive();
        setTouched(true);
    }

    private void ensureOutArchive()
    throws IOException {
        assert writeLock().isLockedByCurrentThread();

        if (outArchive != null)
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
     * {@link OutputArchive} for writing {@code outFile}.
     * This method will delete {@code outFile} if it has successfully
     * opened it for overwriting, but failed to write the archive file header.
     *
     * @throws IOException On any I/O related issue with {@code outFile}.
     */
    private void initOutArchive(final java.io.File outFile)
    throws IOException {
        assert writeLock().isLockedByCurrentThread();
        assert outArchive == null;

        logger.log(Level.FINEST, "initOutArchive.entering", outFile); // NOI18N
        try {
            OutputStream out = new java.io.FileOutputStream(outFile);
            try {
                // If we are actually writing to the target file,
                // we want to log the byte count.
                if (outFile == getTarget())
                    out = new CountingOutputStream(out);
                try {
                    outArchive = getDriver().newOutputArchive(this, out, inArchive);
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
                if (outArchive == null) {
                    out.close();
                    if (!outFile.delete())
                        throw new IOException(outFile.getPath() + " (couldn't delete corrupted output file)");
                }
            }
            outArchive.setMetaData(new OutputArchiveMetaData(this, outArchive));
        } catch (IOException ex) {
            assert outArchive == null;
            logger.log(Level.FINEST, "initOutArchive.throwing", ex); // NOI18N
            throw ex;
        }
        logger.log(Level.FINEST, "initOutArchive.exiting"); // NOI18N

        assert outArchive != null;
    }

    boolean hasNewData(String entryName) {
        assert readLock().isLockedByCurrentThread() || writeLock().isLockedByCurrentThread();
        return outArchive != null && outArchive.getEntry(entryName) != null;
    }

    public void sync(final SyncConfiguration config)
    throws SyncException {
        assert config.getCloseInputStreams() || !config.getCloseOutputStreams(); // closeOutputStreams => closeInputStreams
        assert !config.getUmount() || config.getReassemble(); // sync => reassemble
        assert writeLock().isLockedByCurrentThread();
        assert inArchive == null || inFile != null; // input archive => input file
        assert !isTouched() || outArchive != null; // file system touched => output archive
        assert outArchive == null || outFile != null; // output archive => output file

        // Do the logging part and leave the work to umount0.
        final Object[] stats = new Object[] {
            getCanonicalPath(),
            Boolean.valueOf(config.getWaitForInputStreams()),
            Boolean.valueOf(config.getCloseInputStreams()),
            Boolean.valueOf(config.getWaitForOutputStreams()),
            Boolean.valueOf(config.getCloseOutputStreams()),
            Boolean.valueOf(config.getUmount()),
            Boolean.valueOf(config.getReassemble()),
        };
        logger.log(Level.FINER, "umount.entering", stats); // NOI18N
        try {
            sync0(config);
        } catch (SyncException ex) {
            logger.log(Level.FINER, "umount.throwing", ex); // NOI18N
            throw ex;
        }
        logger.log(Level.FINER, "umount.exiting", stats); // NOI18N
    }

    private void sync0(final SyncConfiguration config)
    throws SyncException {
        final SyncExceptionBuilder builder
                = config.getSyncExceptionBuilder();

        // Check output streams first, because closeInputStreams may be
        // true and closeOutputStreams may be false in which case we
        // don't even need to check open input streams if there are
        // some open output streams.
        if (outArchive != null) {
            final OutputArchiveMetaData outMetaData = outArchive.getMetaData();
            final int outStreams = outMetaData.waitAllOutputStreamsByOtherThreads(
                    config.getWaitForOutputStreams() ? 0 : 50);
            if (outStreams > 0) {
                if (!config.getCloseOutputStreams())
                    throw builder.fail(new ArchiveOutputBusyException(
                            this, outStreams));
                builder.warn(new ArchiveOutputBusyWarningException(
                        this, outStreams));
            }
        }
        if (inArchive != null) {
            final InputArchiveMetaData inMetaData = inArchive.getMetaData();
            final int inStreams = inMetaData.waitAllInputStreamsByOtherThreads(
                    config.getWaitForInputStreams() ? 0 : 50);
            if (inStreams > 0) {
                if (!config.getCloseInputStreams())
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
                    assert inArchive == null;
                } finally {
                    assert outArchive == null;
                }
                try {
                    if (config.getReassemble()) {
                        reassemble(builder);
                        needsReassembly = false;
                    }
                } finally {
                    shutdownStep3(config.getUmount() && !needsReassembly);
                }
            } else if (config.getReassemble() && needsReassembly) {
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
                    shutdownStep3(config.getUmount() && !needsReassembly);
                }
            } else if (config.getUmount()) {
                assert config.getReassemble();
                assert !needsReassembly;
                shutdownStep2(builder);
                shutdownStep3(true);
            } else {
                // This may happen if File.update() or File.sync() has
                // been called and no modifications have been applied to
                // this ArchiveController since its creation or last update.
                assert outArchive == null;
            }
        } catch (SyncException ex) {
            throw ex;
        } catch (IOException ex) {
            throw builder.fail(new SyncException(this, ex));
        } finally {
            setTouched(needsReassembly);
        }

        builder.check();
    }

    final int waitAllInputStreamsByOtherThreads(long timeout) {
        return inArchive != null
                ? inArchive.getMetaData().waitAllInputStreamsByOtherThreads(timeout)
                : 0;
    }

    final int waitAllOutputStreamsByOtherThreads(long timeout) {
        return outArchive != null
                ? outArchive.getMetaData().waitAllOutputStreamsByOtherThreads(timeout)
                : 0;
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
    private void update(final SyncExceptionHandler handler)
    throws SyncException {
        assert writeLock().isLockedByCurrentThread();
        assert isTouched();
        assert outArchive != null;
        assert checkNoDeletedEntriesWithNewData(handler);

        class FilterExceptionHandler
        implements ExceptionHandler<IOException, SyncException> {

            final SyncExceptionHandler delegate;
            IOException last;

            FilterExceptionHandler(final SyncExceptionHandler delegate) {
                if (delegate == null)
                    throw new NullPointerException();
                this.delegate = delegate;
            }

            public SyncException fail(final IOException cannotHappen) {
                throw new AssertionError(cannotHappen);
            }

            public void warn(final IOException cause) throws SyncException {
                if (cause == null)
                    throw new NullPointerException();
                final IOException old = last;
                last = cause;
                if (!(cause instanceof InputException))
                    throw handler.fail(new SyncException(
                            UpdatingArchiveController.this, cause));
                if (old == null)
                    delegate.warn(new SyncWarningException(
                            UpdatingArchiveController.this, cause));
            }
        } // class FilterExceptionHandler

        final ArchiveFileSystem fileSystem = getFileSystem();
        final long rootTime = fileSystem.get(ROOT).getTime();
        try {
            try {
                shutdownStep1(handler);
                // FIXME: Fix this mess!
                ((ArchiveFileSystem<ArchiveEntry>) fileSystem)
                        .copy(  (InputArchive<ArchiveEntry>) inArchive,
                                (OutputArchive<ArchiveEntry>) outArchive,
                                new FilterExceptionHandler(handler));
            } finally {
                // We MUST do cleanup here because (1) any entries in the
                // filesystem which were successfully written (this is the
                // normal case) have been modified by the OutputArchive
                // and thus cannot getEntry used anymore to access the input;
                // and (2) if there has been any IOException on the
                // output archive there is no way to recover from it.
                shutdownStep2(handler);
            }
        } catch (SyncWarningException ex) {
            throw ex;
        } catch (SyncException ex) {
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
            handler.warn(new SyncWarningException(
                    this, "couldn't preserve last modification time"));
    }

    private boolean checkNoDeletedEntriesWithNewData(
            final SyncExceptionHandler handler)
    throws SyncException {
        assert isTouched();
        assert getFileSystem() != null;

        // Check if we have written out any entries that have been
        // deleted from the master directory meanwhile and prepare
        // to throw a warn exception.
        final ArchiveFileSystem fileSystem = getFileSystem();
        for (final ArchiveEntry entry : outArchive) {
            final String entryName = entry.getName();
            /*final String entryName
                    = Paths.normalize(entry.getName(), ENTRY_SEPARATOR_CHAR);*/
            if (fileSystem.get(entryName) == null) {
                // The entry has been written out already, but also
                // has been deleted from the master directory meanwhile.
                // Create a warn exception, but do not yet throw it.
                handler.warn(new SyncWarningException(
                        this, "couldn't remove archive entry '" + entryName + "'"));
            }
        }
        return true;
    }

    /**
     * Uses the updated temporary output archive file to reassemble the
     * target archive file, which may be an entry in an enclosing
     * archive file.
     * <p>
     * <b>This method is intended to be called by {@code update()} only!</b>
     *
     * @param handler An exception handler - {@code null} is not permitted.
     * @throws SyncException If any exceptional condition occurs
     *         throughout the processing of the target archive file.
     */
    private void reassemble(final SyncExceptionHandler handler)
    throws SyncException {
        assert writeLock().isLockedByCurrentThread();

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
                    throw handler.fail(new SyncException(
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
                    handler.warn(new SyncWarningException(
                            this,
                            "couldn't preserve last modification time"));
                }
            }
        } else {
            // The archive file managed by this archive controller IS
            // enclosed in another archive file.
            try {
                wrap(getEnclController(), getEnclEntryName());
            } catch (IOException cause) {
                throw handler.fail(new SyncException(
                        getEnclController(),
                        "could not update archive entry '" + getEnclEntryName() + "' - all changes are lost",
                        cause));
            }
        }
    }

    private void wrap(
            final ArchiveController controller,
            final String entryName)
    throws IOException {
        assert writeLock().isLockedByCurrentThread();
        assert controller != null;
        //assert !controller.readLock().isLocked();
        //assert !controller.writeLock().isLocked();
        assert entryName != null;
        assert !isRoot(entryName);

        class Wrapper implements IOOperation {
            public void run() throws IOException {
                wrapToWriteLockedController(controller, entryName);
            }
        }
        controller.runWriteLocked(new Wrapper());
    }

    private void wrapToWriteLockedController(
            final ArchiveController controller,
            final String entryName)
    throws IOException {
        assert controller != null;
        assert controller.writeLock().isLockedByCurrentThread();
        assert entryName != null;
        assert !isRoot(entryName);

        // Write the updated output archive file as an entry
        // to its enclosing archive file, preserving the
        // last modification time of the root directory as the last
        // modification time of the entry.
        final InputStream in = new java.io.FileInputStream(outFile);
        try {
            ArchiveControllers.cp(true, outFile, in, controller, entryName);
        } catch (FalsePositiveException cannotHappen) {
            throw new AssertionError(cannotHappen);
        } finally {
            in.close();
        }
    }

    /**
     * Resets the archive controller to its initial state - all changes to the
     * archive file which have not yet been updated getEntry lost!
     * <p>
     * Thereafter, the archive controller will behave as if it has just been
     * created and any subsequent operations on its entries will remount
     * the virtual file system from the archive file again.
     *
     * @param handler An exception handler - {@code null} is not permitted.
     * @throws SyncException If any exceptional condition occurs
     *         throughout the processing of the target archive file.
     */
    @Override
    void reset(final SyncExceptionHandler handler)
    throws SyncException {
        assert writeLock().isLockedByCurrentThread();

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
        try {
            logger.log(Level.FINEST, "finalize.entering", getCanonicalPath()); // NOI18N
            // Note: If fileSystem or inArchive are not null, then the controller
            // has been used to perform read operations.
            // If outArchive is not null, the controller has been used to perform
            // write operations, but however, all file system transactions
            // must have failed.
            // Otherwise, the fileSystem would have been marked as touched and
            // this object should never be made elegible for finalization!
            // Tactical note: Assertions don't work in a finalizer, so we use
            // logging.
            if (isTouched() || readLock().isLockedByCurrentThread() || writeLock().isLockedByCurrentThread())
                logger.log(Level.SEVERE, "finalize.invalidState", getCanonicalPath());
            final SyncExceptionBuilder handler
                    = new DefaultSyncExceptionBuilder();
            shutdownStep1(handler);
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
     * @throws SyncException If any exceptional condition occurs
     *         throughout the processing of the target archive file.
     */
    private void shutdownStep1(final SyncExceptionHandler handler)
    throws SyncException {
        class FilterExceptionHandler
        implements ExceptionHandler<IOException, SyncException> {
            public SyncException fail(IOException cannotHappen) {
                throw new AssertionError(cannotHappen);
            }

            public void warn(IOException cause) throws SyncException {
                if (cause == null)
                    throw new NullPointerException();
                handler.warn(new SyncWarningException(
                        UpdatingArchiveController.this, cause));
            }
        } // class FilterExceptionHandler
        final FilterExceptionHandler decoratedHandler = new FilterExceptionHandler();
        if (outArchive != null)
            outArchive.getMetaData().closeAllOutputStreams(decoratedHandler);
        if (inArchive != null)
            inArchive.getMetaData().closeAllInputStreams(decoratedHandler);
    }

    /**
     * Discards the file system and closes the output and input archive.
     * 
     * @param handler An exception handler - {@code null} is not permitted.
     * @throws SyncException If any exceptional condition occurs
     *         throughout the processing of the target archive file.
     */
    private void shutdownStep2(final SyncExceptionHandler handler)
    throws SyncException {
        super.reset(handler); // discard file system

        // The output archive must be closed BEFORE the input archive is
        // closed. This is because the input archive has been presented
        // to output archive as the "source" when it was created and may
        // be using the input archive when its closing to retrieve some
        // meta data information.
        // E.g. with ZIP archive files, the OutputArchive copies the postamble
        // from the InputArchive when it closes.
        try {
            if (outArchive != null) {
                try {
                    outArchive.close();
                } catch (IOException ioe) {
                    handler.warn(new SyncException(this, ioe));
                } finally {
                    outArchive = null;
                }
            }
        } finally {
            if (inArchive != null) {
                try {
                    inArchive.close();
                } catch (IOException ioe) {
                    handler.warn(new SyncWarningException(this, ioe));
                } finally {
                    inArchive = null;
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
