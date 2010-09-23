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

import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.IOReferences;
import de.schlichtherle.truezip.io.archive.output.ArchiveOutputSocketProvider;
import de.schlichtherle.truezip.io.archive.input.ArchiveInputSocketProvider;
import de.schlichtherle.truezip.io.archive.output.ArchiveOutputSocket;
import de.schlichtherle.truezip.io.archive.input.ArchiveInputSocket;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem.Link;
import de.schlichtherle.truezip.io.archive.input.ArchiveInput;
import de.schlichtherle.truezip.io.archive.output.ArchiveOutput;
import de.schlichtherle.truezip.io.IOOperation;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.io.socket.IOReference;
import de.schlichtherle.truezip.key.PromptingKeyManager;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.Operation;
import de.schlichtherle.truezip.util.concurrent.lock.ReadWriteLock;
import de.schlichtherle.truezip.util.concurrent.lock.ReentrantLock;
import de.schlichtherle.truezip.util.concurrent.lock.ReentrantReadWriteLock;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.Set;
import javax.swing.Icon;

import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.IOOption.APPEND;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.IOOption.CREATE_PARENTS;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.IOOption.PRESERVE;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.WAIT_FOR_INPUT_STREAMS;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.WAIT_FOR_OUTPUT_STREAMS;
import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.SEPARATOR;
import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.SEPARATOR_CHAR;
import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.Type.DIRECTORY;
import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.Type.FILE;
import static de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystems.isRoot;
import static de.schlichtherle.truezip.io.Paths.cutTrailingSeparators;

/**
 * This is the base class for any archive controller, providing all the
 * essential services required for accessing archive files.
 * Each instance of this class manages a globally unique archive file
 * (the <i>target file</i>) in order to allow random access to it as if it
 * were a regular directory in the real file system.
 * <p>
 * In terms of software patterns, an {@code ArchiveController} is
 * similar to a Director in a Builder pattern, with the {@link ArchiveDriver}
 * interface as its Builder or Abstract Factory.
 * However, an archive controller does not necessarily build a new archive.
 * It may also simply be used to access an existing archive for read-only
 * operations, such as listing its top level directory, or reading entry data.
 * Whatever type of operation it's used for, an archive controller provides
 * and controls <em>all</em> access to any particular archive file by the
 * client application and deals with the rather complex details of its
 * states and transitions.
 * <p>
 * Each instance of this class maintains a virtual file system, provides input
 * and output streams for the entries of the archive file and methods
 * to update the contents of the virtual file system to the target file
 * in the real file system.
 * In cooperation with the calling methods, it also knows how to deal with
 * nested archive files (such as {@code "outer.zip/inner.tar.gz"}
 * and <i>false positives</i>, i.e. plain files or directories or file or
 * directory entries in an enclosing archive file which have been incorrectly
 * recognized to be <i>prospective archive files</i>.
 * <p>
 * To ensure that for each archive file there is at most one
 * {code ArchiveController}, the path name of the archive file (called
 * <i>target</i>) must be canonicalized, so it doesn't matter whether a target
 * archive file is addressed as {@code "archive.zip"} or
 * {@code "/dir/archive.zip"} if {@code "/dir"} is the client application's
 * current directory.
 * <p>
 * Note that in general all of its methods are reentrant on exceptions.
 * This is important because client applications may repeatedly call them,
 * triggered by the client application. Of course, depending on the context,
 * some or all of the archive file's data may be lost in this case.
 * <p>
 * This class is actually the abstract base class for any archive controller.
 * It encapsulates all the code which is not depending on a particular entry
 * updating strategy and the corresponding state of the controller.
 * Though currently unused, this is intended to be helpful for future
 * extensions of TrueZIP, where different synchronization strategies may be
 * implemented.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
abstract class BasicArchiveController<
        AE extends ArchiveEntry,
        AI extends ArchiveInput<AE>,
        AO extends ArchiveOutput<AE>>
extends     ArchiveController
implements  ArchiveInputSocketProvider<AE>,
            ArchiveOutputSocketProvider<AE> {

    /**
     * A weak reference to this archive controller.
     * This field is for exclusive use by {@link #setTouched(boolean)}.
     */
    private final WeakReference weakThis = new WeakReference(this);

    private final URI mountPoint;

    /**
     * The archive controller of the enclosing archive, if any.
     */
    private final BasicArchiveController enclController;

    /**
     * The relative path name of the entry for the target archive in its
     * enclosing archive, if any.
     */
    private final URI enclPath;

    /**
     * The {@link ArchiveDriver} to use for this controller's target file.
     */
    private final ArchiveDriver<AE, AI, AO> driver;

    /**
     * The canonicalized or at least normalized absolute path name
     * representation of the target archive file.
     */
    private final File target;

    private final ReentrantLock  readLock;
    private final ReentrantLock writeLock;

    //
    // Constructors.
    //

    /**
     * This constructor schedules this controller to be thrown away if the
     * client application holds no more references to it.
     * The subclass must update this schedule according to the controller's
     * state.
     * For example, if the controller has started to update some entry data,
     * it must call {@link #setTouched(boolean)} in order to force the
     * controller to be updated on the next call to
     * {@link ArchiveControllers#sync(URI, BitField, ArchiveSyncExceptionBuilder)}
     * even if the client application holds no more references to it.
     * Otherwise, all changes may get lost!
     * 
     * @see #setTouched(boolean)
     */
    BasicArchiveController(
            final URI mountPoint,
            final URI enclMountPoint,
            final ArchiveDriver<AE, AI, AO> driver) {
        assert "file".equals(mountPoint.getScheme());
        assert !mountPoint.isOpaque();
        assert mountPoint.getPath().endsWith(SEPARATOR);
        assert mountPoint.equals(mountPoint.normalize());
        assert enclMountPoint == null || "file".equals(enclMountPoint.getScheme());
        assert enclMountPoint == null || mountPoint.getPath().startsWith(enclMountPoint.getPath());
        //assert enclMountPoint == null || enclMountPoint.getPath().endsWith(SEPARATOR);
        assert driver != null;

        this.mountPoint = mountPoint;
        this.target = new File(mountPoint);
        if (enclMountPoint != null) {
            this.enclController = (BasicArchiveController) ArchiveControllers.get(enclMountPoint); // FIXME: This is cheating!
            assert this.enclController != null;
            this.enclPath = enclMountPoint.relativize(mountPoint);
        } else {
            this.enclController = null;
            this.enclPath = null;
        }
        this.driver = driver;

        final ReadWriteLock rwl = new ReentrantReadWriteLock();
        this.readLock  = rwl.readLock();
        this.writeLock = rwl.writeLock();

        setTouched(false);

        assert this.enclPath == null || this.enclPath.getPath().endsWith(SEPARATOR);
    }

    //
    // Methods.
    //

    @Override
    public final ReentrantLock readLock() {
        return readLock;
    }

    @Override
    public final ReentrantLock writeLock() {
        return writeLock;
    }

    /**
     * Runs the given {@link Operation} while this controller has
     * acquired its write lock regardless of the state of its read lock.
     * You must use this method if this controller may have acquired a
     * read lock in order to prevent a dead lock.
     * <p>
     * <b>Warning:</b> This method temporarily releases the read lock
     * before the write lock is acquired and the runnable is run!
     * Hence, the runnable must retest the state of the controller
     * before it proceeds with any write operations.
     *
     * @param  operation the operation to run while the write lock is acquired.
     * @return {@code operation}
     */
    @Override
    public final <O extends IOOperation> O runWriteLocked(O operation)
    throws IOException {
        assert operation != null;

        // A read lock cannot get upgraded to a write lock.
        // Hence the following mess is required.
        // Note that this is not just a limitation of the current
        // implementation in JSE 5: If automatic upgrading were implemented,
        // two threads holding a read lock try to upgrade concurrently,
        // they would dead lock each other!
        final int holdCount = readLock().getHoldCount();
        for (int c = holdCount; c-- > 0; )
                readLock().unlock();
        // The current thread may get blocked here!
        writeLock().lock();
        try {
            // First restore lock count to protect agains exceptions.
            for (int c = holdCount; c-- > 0; )
                readLock().lock();
            operation.run();
        } finally {
            writeLock().unlock(); // downgrade the lock
        }
        return operation;
    }

    @Override
    public final URI getMountPoint() {
        return mountPoint;
    }

    @Override
    public final BasicArchiveController getEnclController() {
        return enclController;
    }

    @Override
    public final String getEnclPath(final String path) {
        final String result = isRoot(path)
                ? cutTrailingSeparators(enclPath.toString(), SEPARATOR_CHAR)
                : enclPath.resolve(path).toString();
        assert super.getEnclPath(path).equals(result);
        return result;
    }

    /**
     * Returns the driver instance which is used for the target archive.
     * All access to this method must be externally synchronized on this
     * controller's read lock!
     *
     * @return A valid reference to an {@link ArchiveDriver} object
     *         - never {@code null}.
     */
    final ArchiveDriver<AE, AI, AO> getDriver() {
        return driver;
    }

    /**
     * Returns the canonical or at least normalized absolute file for the
     * target archive file.
     */
    final File getTarget() {
        return target;
    }

    /**
     * Returns {@code true} if and only if the target file of this
     * controller should be considered to be a file or directory in the real
     * file system (RFS).
     * Note that the target doesn't need to exist for this method to return
     * {@code true}.
     */
    final boolean isRfsEntryTarget() {
        // May be called from FileOutputStream while unlocked!
        //assert readLock().isLocked() || writeLock().isLocked();

        // True iff not enclosed or the enclosing archive file is actually
        // a plain directory.
        final BasicArchiveController enclController = getEnclController();
        return enclController == null
                || enclController.getTarget().isDirectory();
    }

    /**
     * Sets the <i>touch status</i> of the virtual file system and
     * (re)schedules this archive controller for the synchronization of its
     * archive contents to the target archive file in the real file system
     * upon the next call to
     * {@link ArchiveControllers#sync(URI, BitField, ArchiveSyncExceptionBuilder)}
     * according to the given touch status:
     * <p>
     * If set to {@code true}, the archive contents of this controller are
     * guaranteed to get synced to the target archive file in the real file
     * system even if there are no other objects referring to it.
     * <p>
     * If set to {@code false}, this controller is only conditionally
     * scheduled to get synced, i.e. it gets automatically removed from the
     * controllers weak hash map and discarded once the last file object
     * directly or indirectly referring to it has been discarded unless
     * {@code setTouched(true)} has been called again meanwhile.
     * <p>
     * Call this method if the archive controller has been newly created or
     * successfully updated.
     *
     * @param touched The touch status of the virtual file system.
     * @see #isTouched
     */
    final void setTouched(final boolean touched) {
        assert weakThis.get() != null || !touched; // (garbage collected => no scheduling) == (scheduling => not garbage collected)
        ArchiveControllers.map(getMountPoint(), touched ? this : weakThis);
    }

    /**
     * Returns the virtual archive file system mounted from the target file.
     * This method is reentrant with respect to any exceptions it may throw.
     * <p>
     * <b>Warning:</b> Either the read or the write lock of this controller
     * must be acquired while this method is called!
     * If only a read lock is acquired, but a write lock is required, this
     * method will temporarily release all locks, so any preconditions must be
     * checked again upon return to protect against concurrent modifications!
     * 
     * @param autoCreate If the archive file does not exist and this is
     *        {@code true}, a new file system with only a virtual root
     *        directory is created with its last modification time set to the
     *        system's current time.
     * @return A valid archive file system - {@code null} is never returned.
     * @throws FalsePositiveException
     * @throws IOException On any other I/O related issue with the target file
     *         or the target file of any enclosing archive file's controller.
     */
    abstract ArchiveFileSystem<AE> autoMount(boolean autoCreate, boolean createParents)
    throws IOException;

    @Override
    public final ArchiveFileSystem<AE> autoMount(boolean autoCreate)
    throws IOException {
        return autoMount(autoCreate, autoCreate);
    }

    final ArchiveFileSystem<AE> autoMount()
    throws IOException {
        return autoMount(false, false);
    }

    @Override
    public final void autoSync(final String path)
    throws ArchiveSyncException {
        assert writeLock().isHeldByCurrentThread();
        if (hasNewData(path)) {
            sync(   BitField.of(WAIT_FOR_INPUT_STREAMS, WAIT_FOR_OUTPUT_STREAMS),
                    new DefaultArchiveSyncExceptionBuilder());
        }
    }

    // TODO: Document this!
    abstract int waitAllInputStreamsByOtherThreads(long timeout);

    // TODO: Document this!
    abstract int waitAllOutputStreamsByOtherThreads(long timeout);

    /**
     * Resets the archive controller to its initial state - all changes to the
     * archive file which have not yet been updated get lost!
     * Thereafter, the archive controller will behave as if it has just been
     * created and any subsequent operations on its entries will remount
     * the virtual file system from the archive file again.
     */
    final void reset() throws ArchiveSyncException {
        final ArchiveSyncExceptionBuilder builder
                = new DefaultArchiveSyncExceptionBuilder();
        reset(builder);
        builder.check();
    }

    /**
     * Resets the archive controller to its initial state - all changes to the
     * archive file which have not yet been updated get lost!
     * Thereafter, the archive controller will behave as if it has just been
     * created and any subsequent operations on its entries will remount
     * the virtual file system from the archive file again.
     * <p>
     * This method should be overridden by subclasses, but must still be
     * called when doing so.
     */
    abstract void reset(final ArchiveSyncExceptionHandler handler)
    throws ArchiveSyncException;

    @Override
    public ArchiveInputSocket<?> getInputSocket(
            final BitField<IOOption> options, // currently unused
            final String path)
    throws IOException {
        assert path != null;

        try {
            return getInputSocket0(options, path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return getEnclController().getInputSocket(options, getEnclPath(path));
        }
    }

    private ArchiveInputSocket<?> getInputSocket0(
            final BitField<IOOption> options, // currently unused
            final String path)
    throws IOException {
        assert options != null;
        assert path != null;

        readLock().lock();
        try {
            if (isRoot(path)) {
                try {
                    autoMount(); // detect false positives!
                } catch (ArchiveEntryNotFoundException ex) {
                    if (isRoot(ex.getPath()))
                        throw new FalsePositiveException(this, path, ex);
                    // TODO: throw new ArchiveEntryFalsePositiveException(ex); ?!?! archive entry not found is not really an archive entry false positive ?!?!
                    return getEnclController().getInputSocket0(
                            options, getEnclPath(path));
                }
                throw new ArchiveEntryNotFoundException(this, path,
                        "cannot read directories");
            } else {
                autoMount(); // detect false positives!
                class InputSocketProxy extends ArchiveInputSocket<AE> {
                    private IOReference<AE> local = this;

                    @Override
                    public InputSocket<AE, ArchiveEntry> connect(
                            final OutputSocket<? extends ArchiveEntry, ? super AE> newPeer) {
                        super.connect(newPeer);
                        getPeerTarget();
                        return this;
                    }

                    @Override
                    protected void beforeConnectComplete() {
                        local = this; // reset local target reference
                    }

                    private AE target() throws IOException {
                        if (hasNewData(path)) {
                            local = this;
                            class AutoSync implements IOOperation {
                                @Override
                                public void run() throws IOException {
                                    autoSync(path);
                                }
                            }
                            runWriteLocked(new AutoSync());
                        }
                        if (this == local) {
                            try {
                                local = IOReferences.ref(autoMount().getEntry(path));
                            } catch (FalsePositiveException alreadyDetected) {
                                throw new AssertionError(alreadyDetected);
                            }
                        }
                        return IOReferences.deref(local);
                    }

                    @Override
                    public AE getTarget() {
                        readLock().lock();
                        try {
                            try {
                                return target();
                            } catch (IOException resolveToNull) {
                                return null; // FIXME: interface contract violation
                            }
                        } finally {
                            readLock().unlock();
                        }
                    }

                    @Override
                    public InputStream newInputStream()
                    throws IOException {
                        readLock().lock();
                        try {
                            final AE target = target();
                            if (null != target && DIRECTORY == target.getType())
                                throw new ArchiveEntryNotFoundException(
                                        BasicArchiveController.this, path,
                                        "cannot read directories");
                            final ArchiveInputSocket<? extends AE> input;
                            if (null == target ||
                                    null == (input = getInputSocket(target)))
                                throw new ArchiveEntryNotFoundException(
                                        BasicArchiveController.this, path,
                                        "no such file or directory");
                            return input.chain(this).newInputStream();
                        } finally {
                            readLock().unlock();
                        }
                    }
                }
                return new InputSocketProxy();
            }
        } finally {
            readLock().unlock();
        }
    }

    @Override
    public ArchiveOutputSocket<?> getOutputSocket(
            final BitField<IOOption> options,
            final String path)
    throws IOException {
        assert path != null;

        try {
            return getOutputSocket0(options, path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return getEnclController().getOutputSocket(
                    options, getEnclPath(path));
        }
    }

    private ArchiveOutputSocket<?> getOutputSocket0(
            final BitField<IOOption> options,
            final String path)
    throws IOException {
        assert path != null;

        writeLock().lock();
        try {
            if (isRoot(path)) {
                try {
                    autoMount(); // detect false positives!
                } catch (ArchiveEntryNotFoundException ex) {
                    if (isRoot(ex.getPath()))
                        throw new FalsePositiveException(this, path, ex);
                    // TODO: throw new ArchiveEntryFalsePositiveException(ex); ??? not found is not really a false positive ???
                    return getEnclController().getOutputSocket0(
                            options, getEnclPath(path));
                }
                throw new ArchiveEntryNotFoundException(this, path,
                        "cannot write directories");
            } else {
                autoMount(options.get(CREATE_PARENTS)); // detect false positives!
                class OutputSocketProxy extends ArchiveOutputSocket<AE> {
                    private Link<AE> local;

                    @Override
                    public OutputSocket<AE, ArchiveEntry> connect(
                            final InputSocket<? extends ArchiveEntry, ? super AE> newPeer) {
                        super.connect(newPeer);
                        getPeerTarget();
                        return this;
                    }

                    @Override
                    protected void beforeConnectComplete() {
                        local = null; // reset local target reference
                    }

                    private AE target() throws IOException {
                        if (hasNewData(path)) {
                            local = null;
                            autoSync(path);
                        }
                        if (null == local) {
                            try {
                                // Start creating or overwriting the archive entry.
                                // This will fail if the entry already exists as a directory.
                                local = autoMount(options.get(CREATE_PARENTS))
                                        .mknod(path, FILE,
                                            options.get(PRESERVE)
                                                ? getPeerTarget()
                                                : null,
                                            options.get(CREATE_PARENTS));
                            } catch (FalsePositiveException alreadyDetected) {
                                throw new AssertionError(alreadyDetected);
                            }
                        }
                        return local.getTarget();
                    }

                    @Override
                    public AE getTarget() {
                        class GetTarget implements IOOperation {
                            AE target;

                            @Override
                            public void run() throws IOException {
                                target = target();
                            }
                        }
                        try {
                            return runWriteLocked(new GetTarget()).target;
                        } catch (IOException ex) {
                            return null; // FIXME: interface contract violation
                        }
                    }

                    @Override
                    public OutputStream newOutputStream()
                    throws IOException {
                        class NewOutputStream implements IOOperation {
                            OutputStream out;

                            @Override
                            public void run() throws IOException {
                                final AE target = target();
                                final ArchiveOutputSocket<? extends AE> output
                                        = getOutputSocket(target);
                                final boolean append = options.get(APPEND);
                                final InputStream in = append
                                        ? getInputSocket(target)
                                            .connect(null)
                                            .newInputStream()
                                        : null;
                                try {
                                    out = output
                                            .chain(append
                                                ? null
                                                : OutputSocketProxy.this)
                                            .newOutputStream();
                                    try {
                                        local.run();
                                        if (in != null)
                                            Streams.cat(in, out);
                                    } catch (IOException ex) {
                                        out.close(); // may throw another exception!
                                        throw ex;
                                    }
                                } finally {
                                    if (in != null) {
                                        try {
                                            in.close();
                                        } catch (IOException ex) {
                                            throw new InputException(ex);
                                        }
                                    }
                                }
                            }

                        }
                        return runWriteLocked(new NewOutputStream()).out;
                    }
                }
                return new OutputSocketProxy();
            }
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public final boolean isExisting(final String path)
    throws FalsePositiveException {
        try {
            return isExisting0(path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return getEnclController().isExisting(getEnclPath(path));
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    private boolean isExisting0(final String path)
    throws FalsePositiveException, IOException {
        readLock().lock();
        try {
            try {
                return autoMount().getType(path) != null;
            } catch (ArchiveEntryNotFoundException ex) {
                return false;
            }
        } finally {
            readLock().unlock();
        }
    }

    @Override
    public final boolean isFile(final String path)
    throws FalsePositiveException {
        try {
            return isFile0(path);
        } catch (FileArchiveEntryFalsePositiveException ex) {
            // See ArchiveDriver#newArchiveInput!
            if (isRoot(path) && ex.getCause() instanceof FileNotFoundException)
                return false;
            return getEnclController().isFile(getEnclPath(path));
        } catch (DirectoryArchiveEntryFalsePositiveException ex) {
            return getEnclController().isFile(getEnclPath(path)); // the directory could be one of the target's ancestors!
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    private boolean isFile0(final String path)
    throws FalsePositiveException, IOException {
        readLock().lock();
        try {
            try {
                return autoMount().getType(path) == FILE;
            } catch (ArchiveEntryNotFoundException ex) {
                return false;
            }
        } finally {
            readLock().unlock();
        }
    }

    @Override
    public final boolean isDirectory(final String path)
    throws FalsePositiveException {
        try {
            return isDirectory0(path);
        } catch (FileArchiveEntryFalsePositiveException ex) {
            return false;
        } catch (DirectoryArchiveEntryFalsePositiveException ex) {
            return getEnclController().isDirectory(getEnclPath(path));
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    private boolean isDirectory0(final String path)
    throws FalsePositiveException, IOException {
        readLock().lock();
        try {
            try {
                return autoMount().getType(path) == DIRECTORY;
            } catch (ArchiveEntryNotFoundException ex) {
                return false;
            }
        } finally {
            readLock().unlock();
        }
    }

    @Override
    public final Icon getOpenIcon(final String path)
    throws FalsePositiveException {
        try {
            return getOpenIcon0(path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return getEnclController().getOpenIcon(getEnclPath(path));
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return null;
        }
    }

    private Icon getOpenIcon0(final String path)
    throws FalsePositiveException, IOException {
        readLock().lock();
        try {
            autoMount(); // detect false positives!
            return isRoot(path)
                    ? getDriver().getOpenIcon(this)
                    : null;
        } finally {
            readLock().unlock();
        }
    }

    @Override
    public final Icon getClosedIcon(final String path)
    throws FalsePositiveException {
        try {
            return getClosedIcon0(path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return getEnclController().getClosedIcon(getEnclPath(path));
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return null;
        }
    }

    private Icon getClosedIcon0(final String path)
    throws FalsePositiveException, IOException {
        readLock().lock();
        try {
            autoMount(); // detect false positives!
            return isRoot(path)
                    ? getDriver().getClosedIcon(this)
                    : null;
        } finally {
            readLock().unlock();
        }
    }

    @Override
    public final boolean isReadable(final String path)
    throws FalsePositiveException {
        try {
            return isReadable0(path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return getEnclController().isReadable(getEnclPath(path));
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    private boolean isReadable0(final String path)
    throws FalsePositiveException, IOException {
        readLock().lock();
        try {
            return autoMount().getType(path) != null;
        } finally {
            readLock().unlock();
        }
    }

    @Override
    public final boolean isWritable(final String path)
    throws FalsePositiveException {
        try {
            return isWritable0(path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return getEnclController().isWritable(getEnclPath(path));
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    private boolean isWritable0(final String path)
    throws FalsePositiveException, IOException {
        readLock().lock();
        try {
            return autoMount().isWritable(path);
        } finally {
            readLock().unlock();
        }
    }

    @Override
    public final long getLength(final String path)
    throws FalsePositiveException {
        try {
            return getLength0(path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return getEnclController().getLength(getEnclPath(path));
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return 0;
        }
    }

    private long getLength0(final String path)
    throws FalsePositiveException, IOException {
        readLock().lock();
        try {
            return autoMount().getLength(path);
        } finally {
            readLock().unlock();
        }
    }

    @Override
    public final long getLastModified(final String path)
    throws FalsePositiveException {
        try {
            return getLastModified0(path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return getEnclController().getLastModified(getEnclPath(path));
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return 0;
        }
    }

    private long getLastModified0(final String path)
    throws FalsePositiveException, IOException {
        readLock().lock();
        try {
            return autoMount().getLastModified(path);
        } finally {
            readLock().unlock();
        }
    }

    @Override
    public final Set<String> list(final String path)
    throws FalsePositiveException {
        try {
            return list0(path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return getEnclController().list(getEnclPath(path));
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return null;
        }
    }

    private Set<String> list0(final String path)
    throws FalsePositiveException, IOException {
        readLock().lock();
        try {
            return autoMount().list(path);
        } finally {
            readLock().unlock();
        }
    }

    @Override
    public final void setReadOnly(final String path)
    throws FalsePositiveException, IOException {
        try {
            setReadOnly0(path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            getEnclController().setReadOnly(getEnclPath(path));
        }
    }

    private void setReadOnly0(final String path)
    throws FalsePositiveException, IOException {
        writeLock().lock();
        try {
            autoMount().setReadOnly(path);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public final boolean setLastModified(
            final String path,
            final long time)
    throws FalsePositiveException {
        try {
            return setLastModified0(path, time);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return getEnclController().setLastModified(getEnclPath(path), time);
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    private boolean setLastModified0(
            final String path,
            final long time)
    throws FalsePositiveException, IOException {
        writeLock().lock();
        try {
            autoSync(path);
            return autoMount().setLastModified(path, time);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public final boolean createNewFile(
            final String path,
            final boolean createParents)
    throws FalsePositiveException, IOException {
        try {
            return createNewFile0(path, createParents);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return getEnclController().createNewFile(
                    getEnclPath(path), createParents);
        }
    }

    private boolean createNewFile0(
            final String path,
            final boolean createParents)
    throws FalsePositiveException, IOException {
        assert !isRoot(path);

        writeLock().lock();
        try {
            if (autoMount(createParents).getType(path) != null)
                return false;
            // If we got here without an exception, write an empty file now.
            getOutputSocket0(
                        BitField
                            .noneOf(IOOption.class)
                            .set(CREATE_PARENTS, createParents),
                        path)
                    .connect(null)
                    .newOutputStream()
                    .close();
            return true;
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public final boolean mkdir(
            final String path,
            final boolean createParents)
    throws FalsePositiveException {
        try {
            mkdir0(path, createParents);
            return true;
        } catch (ArchiveEntryFalsePositiveException ex) {
            return getEnclController().mkdir(getEnclPath(path), createParents);
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    private void mkdir0(final String path, final boolean createParents)
    throws FalsePositiveException, IOException {
        writeLock().lock();
        try {
            if (isRoot(path)) {
                try {
                    autoMount(); // detect false positives!
                } catch (ArchiveEntryNotFoundException ex) {
                    autoMount(true, createParents);
                    return;
                }
                throw new ArchiveEntryNotFoundException(this, path,
                        "directory exists already");
            } else { // !isRoot(entryName)
                // This is going to be a regular directory archive entry.
                autoMount(createParents)
                        .mknod(path, DIRECTORY, null, createParents)
                        .run();
            }
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public final boolean delete(final String path)
    throws FalsePositiveException {
        try {
            delete0(path);
            return true;
        } catch (DirectoryArchiveEntryFalsePositiveException ex) {
            return getEnclController().delete(getEnclPath(path));
        } catch (FileArchiveEntryFalsePositiveException ex) {
            // See ArchiveDriver#newArchiveInput!
            if (isRoot(path)
            && !getEnclController().isDirectory(getEnclPath(path)) // isFile() would fail!
            && ex.getCause() instanceof FileNotFoundException)
                return false;
            return getEnclController().delete(getEnclPath(path));
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    private void delete0(final String path)
    throws FalsePositiveException, IOException {
        writeLock().lock();
        try {
            autoSync(path);
            if (isRoot(path)) {
                // Get the file system or die trying!
                final ArchiveFileSystem fileSystem;
                try {
                    fileSystem = autoMount();
                } catch (FalsePositiveException ex) {
                    // The File instance is going to delete the target file
                    // anyway, so we need to reset now.
                    try {
                        reset();
                    } catch (ArchiveSyncException cannotHappen) {
                        throw new AssertionError(cannotHappen);
                    }
                    throw ex;
                }
                // We are actually working on the controller's target file.
                if (!fileSystem.list(path).isEmpty())
                    throw new IOException("archive file system not empty!");
                final int outputStreams = waitAllOutputStreamsByOtherThreads(50);
                // TODO: Review: This policy may be changed - see method start.
                assert outputStreams <= 0
                        : "Entries for open output streams should not be deletable!";
                // Note: Entry for open input streams ARE deletable!
                final int inputStreams = waitAllInputStreamsByOtherThreads(50);
                if (inputStreams > 0 || outputStreams > 0)
                    throw new IOException("archive file has open streams!");
                reset();
                // Just in case our target is an RAES encrypted ZIP file,
                // forget it's password as well.
                // TODO: Review: This is an archive driver dependency!
                // Calling it doesn't harm, but please consider a more opaque
                // way to model this, e.g. by calling a listener interface.
                PromptingKeyManager.resetKeyProvider(getMountPoint());
                // Delete the target file or the entry in the enclosing
                // archive file, too.
                if (isRfsEntryTarget()) {
                    // The target file of the controller is NOT enclosed
                    // in another archive file.
                    if (!getTarget().delete())
                        throw new IOException("couldn't delete archive file!");
                } else {
                    // The target file of the controller IS enclosed in
                    // another archive file.
                    getEnclController().delete0(getEnclPath(path));
                }
            } else { // !isRoot(entryName)
                autoMount().unlink(path);
            }
        } finally {
            writeLock().unlock();
        }
    }
}
