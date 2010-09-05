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

import de.schlichtherle.truezip.io.util.IOOperation;
import de.schlichtherle.truezip.io.FileFactory;
import de.schlichtherle.truezip.io.File;
import de.schlichtherle.truezip.io.archive.Archive;
import de.schlichtherle.truezip.io.archive.controller.ArchiveFileSystem.LinkTransaction;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry;
import de.schlichtherle.truezip.io.util.Streams;
import de.schlichtherle.truezip.key.PromptingKeyManager;
import de.schlichtherle.truezip.util.Operation;
import de.schlichtherle.truezip.util.concurrent.lock.ReadWriteLock;
import de.schlichtherle.truezip.util.concurrent.lock.ReentrantLock;
import de.schlichtherle.truezip.util.concurrent.lock.ReentrantReadWriteLock;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import javax.swing.Icon;

import static de.schlichtherle.truezip.io.archive.driver.ArchiveEntry.SEPARATOR;
import static de.schlichtherle.truezip.io.archive.driver.ArchiveEntry.Type.DIRECTORY;
import static de.schlichtherle.truezip.io.archive.driver.ArchiveEntry.Type.FILE;

/**
 * This is the base class for any archive controller, providing all the
 * essential services required by the {@link File} class to implement its
 * behaviour.
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
 * <i>target</i>) is canonicalized, so it doesn't matter whether the
 * {@link File} class addresses an archive file as {@code "archive.zip"}
 * or {@code "/dir/archive.zip"} if {@code "/dir"} is the client
 * application's current directory.
 * <p>
 * Note that in general all of its methods are reentrant on exceptions.
 * This is important because the {@link File} class may repeatedly call them,
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
public abstract class ArchiveController implements Archive {

    /**
     * A weak reference to this archive controller.
     * This field is for exclusive use by {@link #setTouched(boolean)}.
     */
    private final WeakReference weakThis = new WeakReference(this);

    /**
     * The canonicalized or at least normalized absolute path name
     * representation of the target archive file.
     */
    private final java.io.File target;

    /**
     * The archive controller of the enclosing archive, if any.
     */
    private final ArchiveController enclController;

    /**
     * The name of the entry for this archive in the enclosing archive, if any.
     */
    private final String enclEntryName;

    /**
     * The {@link ArchiveDriver} to use for this controller's target file.
     */
    private /*volatile*/ ArchiveDriver driver;

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
     * {@link ArchiveControllers#sync(String, SyncConfiguration)}
     * even if the client application holds no more references to it.
     * Otherwise, all changes may get lost!
     * 
     * @see #setTouched(boolean)
     */
    ArchiveController(
            final java.io.File target,
            final ArchiveController enclController,
            final String enclEntryName,
            final ArchiveDriver driver) {
        assert target != null;
        assert target.isAbsolute();
        assert (enclController != null) == (enclEntryName != null);
        assert driver != null;

        this.target = target;
        this.enclController = enclController;
        this.enclEntryName = enclEntryName;
        this.driver = driver;

        ReadWriteLock rwl = new ReentrantReadWriteLock();
        this.readLock  = rwl.readLock();
        this.writeLock = rwl.writeLock();

        setTouched(false);
    }

    //
    // Methods.
    //

    public final ReentrantLock readLock() {
        return readLock;
    }

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
     * Hence, the runnable should recheck the state of the controller
     * before it proceeds with any write operations.
     *
     * @param runnable The {@link Operation} to run while the write lock is
     *        acquired.
     *        No read lock is acquired while it's running.
     */
    final <T extends Throwable> void runWriteLocked(Operation<T> runnable)
    throws T {
        // A read lock cannot get upgraded to a write lock.
        // Hence the following mess is required.
        // Note that this is not just a limitation of the current
        // implementation in JSE 5: If automatic upgrading were implemented,
        // two threads holding a read lock try to upgrade concurrently,
        // they would dead lock each other!
        final int lockCount = readLock().getLockCount();
        for (int c = lockCount; c > 0; c--)
            readLock().unlock();

        // The current thread may get blocked here!
        writeLock().lock();
        try {
            try {
                runnable.run();
            } finally {
                // Restore lock count - effectively downgrading the lock
                for (int c = lockCount; c > 0; c--)
                    readLock().lock();
            }
        } finally {
            writeLock().unlock();
        }
    }

    /**
     * Returns the canonical or at least normalized absolute
     * {@code java.io.File} object for the target archive file.
     */
    final java.io.File getTarget() {
        return target;
    }

    public final String getCanonicalPath() {
        return target.getPath();
    }

    public final Archive getEnclArchive() {
        return enclController;
    }

    /**
     * Returns {@code true} iff the given entry name refers to the
     * virtual root directory within this controller.
     */
    static boolean isRoot(String entryName) {
        return ArchiveFileSystem.isRoot(entryName);
    }

    boolean isLenient() {
        return ArchiveControllers.isLenient();
    }

    /**
     * Returns the {@link ArchiveController} of the enclosing archive file,
     * if any.
     */
    public final ArchiveController getEnclController() {
        return enclController;
    }

    /**
     * Returns the entry name of this controller within the enclosing archive
     * file, if any.
     */
    public final String getEnclEntryName() {
        return enclEntryName;
    }

    public final String enclEntryName(final String entryName) {
        return isRoot(entryName)
                ? enclEntryName
                : enclEntryName + SEPARATOR + entryName;
    }

    /**
     * Returns the driver instance which is used for the target archive.
     * All access to this method must be externally synchronized on this
     * controller's read lock!
     * 
     * @return A valid reference to an {@link ArchiveDriver} object
     *         - never {@code null}.
     */
    public final ArchiveDriver getDriver() {
        return driver;
    }

    /**
     * Sets the driver instance which is used for the target archive.
     * All access to this method must be externally synchronized on this
     * controller's write lock!
     * 
     * @param driver A valid reference to an {@link ArchiveDriver} object
     *        - never {@code null}.
     */
    final void setDriver(ArchiveDriver driver) {
        assert writeLock().isLockedByCurrentThread();

        // This affects all subsequent creations of the driver's products
        // (In/OutputArchive and ArchiveEntry) and hence ArchiveFileSystem.
        // Normally, these are initialized together in mountFileSystem(...)
        // which is externally synchronized on this controller's write lock,
        // so we don't need to be afraid of this.
        this.driver = driver;
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
        return enclController == null
                || enclController.getTarget().isDirectory();
    }

    /**
     * Returns {@code true} if and only if the file system has been touched,
     * i.e. if an operation changed its state.
     */
    abstract boolean isTouched();

    /**
     * Sets the <i>touch status</i> of the virtual file system and
     * (re)schedules this archive controller for the synchronization of its
     * archive contents to the target archive file in the real file system
     * upon the next call to
     * {@link ArchiveControllers#sync(String, SyncConfiguration)}
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
        ArchiveControllers.set( getTarget(), touched ? this : weakThis);
    }

    /**
     * Tests if the archive entry with the given name has received or is
     * currently receiving new data via an output stream.
     * As an implication, the entry cannot receive new data from another
     * output stream before the next call to {@link #sync}.
     * Note that for directories this method will always return
     * {@code false}!
     */
    abstract boolean hasNewData(String entryName);

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
     * @param create If the archive file does not exist and this is
     *        {@code true}, a new file system with only a virtual root
     *        directory is created with its last modification time set to the
     *        system's current time.
     * @return A valid archive file system - {@code null} is never returned.
     * @throws FalsePositiveException
     * @throws IOException On any other I/O related issue with the target file
     *         or the target file of any enclosing archive file's controller.
     */
    public abstract ArchiveFileSystem autoMount(boolean create)
    throws FalsePositiveException, IOException;

    /**
     * Unmounts the archive file only if the archive file has already new
     * data for {@code entryName}.
     * <p>
     * <b>Warning:</b> As a side effect, all data structures returned by this
     * controller get reset (filesystem, entries, streams, etc.)!
     * As an implication, this method requires external synchronization on
     * this controller's write lock!
     * <p>
     * <b>TODO:</b> Consider adding configuration switch to allow overwriting
     * an archive entry to the same output archive multiple times, whereby
     * only the last written entry would be added to the central directory
     * of the archive (unless the archive type doesn't support this).
     * 
     * @see #sync(SyncConfiguration)
     * @throws SyncException If any exceptional condition occurs
     *         throughout the processing of the target archive file.
     */
    public final void autoUmount(final String entryName)
    throws SyncException {
        assert writeLock().isLockedByCurrentThread();
        if (hasNewData(entryName)) {
            sync(new SyncConfiguration()
                    .setWaitForInputStreams(true)
                    .setCloseInputStreams(false)
                    .setWaitForOutputStreams(true)
                    .setCloseOutputStreams(false)
                    .setUmount(false)
                    .setReassemble(false));
        }
    }

    /**
     * Writes all changes to the contents of the target archive file to the
     * underlying file system.
     * As a side effect, all data structures returned by this controller get
     * reset (filesystem, entries, streams etc.)!
     * This method requires external synchronization on this controller's write
     * lock!
     *
     * @param config The parameters for processing - {@code null} is not
     *        permitted.
     * @throws NullPointerException if {@code config} is {@code null}.
     * @throws SyncException if any exceptional condition occurs
     *         throughout the processing of the target archive file.
     * @see ArchiveControllers#sync(String, SyncConfiguration)
     */
    public abstract void sync(SyncConfiguration config)
    throws SyncException;

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
    final void reset() throws SyncException {
        final SyncExceptionBuilder builder
                = new DefaultSyncExceptionBuilder();
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
    abstract void reset(final SyncExceptionHandler handler)
    throws SyncException;

    @Override
    public String toString() {
        return getClass().getName() + "@" + System.identityHashCode(this) + "(" + getCanonicalPath() + ")";
    }

    //
    // File system operations used by the File* classes.
    // Stream operations:
    //

    /**
     * A factory method returning an input stream which is positioned
     * at the beginning of the given entry in the target archive file.
     * 
     * @param path An entry in the virtual archive file system
     *        - {@code null} or {@code ""} is not permitted.
     * @return A valid {@code InputStream} object
     *         - {@code null} is never returned.
     */
    public final InputStream newInputStream(final String path)
    throws FalsePositiveException, IOException {
        assert path != null;

        try {
            return newInputStream0(path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.newInputStream(enclEntryName(path));
        }
    }

    // TODO: Make this private!
    public InputStream newInputStream0(final String path)
    throws FalsePositiveException, IOException {
        assert path != null;

        readLock().lock();
        try {
            if (isRoot(path)) {
                try {
                    final boolean directory = isDirectory0(path); // detects false positives
                    assert directory : "The root entry must be a directory!";
                } catch (EnclosedArchiveFileNotFoundException ex) {
                    return enclController.newInputStream0(enclEntryName(path));
                } catch (ArchiveFileNotFoundException ex) {
                    throw new FalsePositiveException(this, ex);
                }
                throw new ArchiveEntryNotFoundException(this, path,
                        "cannot read from (virtual root) directory entry");
            } else {
                if (hasNewData(path)) {
                    class AutoUmount4CreateInputStream
                    implements IOOperation {
                        public void run() throws IOException {
                            autoUmount(path);
                        }
                    }
                    runWriteLocked(new AutoUmount4CreateInputStream());
                }
                final ArchiveEntry entry = autoMount(false).get(path);
                if (entry == null)
                    throw new ArchiveEntryNotFoundException(this, path,
                            "no such file entry");
                if (entry.getType() == DIRECTORY)
                    throw new ArchiveEntryNotFoundException(this, path,
                            "cannot read from directory entry");
                return newInputStream(entry, null);
            }
        } finally {
            readLock().unlock();
        }
    }

    /**
     * <b>Important:</b>
     * <ul>
     * <li>This controller's read <em>or</em> write lock must be acquired.
     * <li>{@code entry} must not have received
     *     {@link #hasNewData new data}.
     * <ul>
     */
    abstract InputStream newInputStream(
            ArchiveEntry entry,
            ArchiveEntry dstEntry)
    throws IOException;

    /**
     * A factory method returning an {@code OutputStream} allowing to
     * (re)write the given entry in the target archive file.
     * 
     * @param path An entry in the virtual archive file system
     *        - {@code null} or {@code ""} is not permitted.
     * @return A valid {@code OutputStream} object
     *         - {@code null} is never returned.
     */
    public final OutputStream newOutputStream(
            final String path,
            final boolean append)
    throws FalsePositiveException, IOException {
        assert path != null;

        try {
            return newOutputStream0(path, append);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.newOutputStream(enclEntryName(path),
                    append);
        }
    }

    private OutputStream newOutputStream0(
            final String path,
            final boolean append)
    throws FalsePositiveException, IOException {
        assert path != null;

        final InputStream in;
        final OutputStream out;
        writeLock().lock();
        try {
            if (isRoot(path)) {
                try {
                    final boolean directory = isDirectory0(path); // detects false positives
                    assert directory : "The root entry must be a directory!";
                } catch (EnclosedArchiveFileNotFoundException ex) {
                    return enclController.newOutputStream0(enclEntryName(path), append);
                } catch (ArchiveFileNotFoundException ex) {
                    throw new FalsePositiveException(this, ex);
                }
                throw new ArchiveEntryNotFoundException(this, path,
                        "cannot write to (virtual root) directory entry");
            } else {
                autoUmount(path);
                final boolean lenient = isLenient();
                final ArchiveFileSystem fileSystem = autoMount(lenient);
                in = append && fileSystem.isFile(path)
                        ? newInputStream0(path)
                        : null;
                // Start creating or overwriting the archive entry.
                // Note that this will fail if the entry already exists as a
                // directory.
                final LinkTransaction link = fileSystem.link(path, FILE, lenient);
                // Create output stream.
                out = newOutputStream(link.getEntry(), null);
                // Now link the entry into the file system.
                link.run();
            }
        } finally {
            writeLock().unlock();
        }
        if (in != null) {
            try {
                Streams.cat(in, out);
            } finally {
                in.close();
            }
        }
        return out;
    }

    /**
     * <b>Important:</b>
     * <ul>
     * <li>This controller's <em>write</em> lock must be acquired.
     * <li>{@code entry} must not have received
     *     {@link #hasNewData new data}.
     * <ul>
     */
    abstract OutputStream newOutputStream(
            ArchiveEntry entry,
            ArchiveEntry srcEntry)
    throws IOException;

    //
    // File system operations used by the File class.
    // Read only operations:
    //

    public final boolean exists(final String path)
    throws FalsePositiveException {
        try {
            return exists0(path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.exists(enclEntryName(path));
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    private boolean exists0(final String path)
    throws FalsePositiveException, IOException {
        readLock().lock();
        try {
            final ArchiveFileSystem fileSystem = autoMount(false);
            return fileSystem.exists(path);
        } finally {
            readLock().unlock();
        }
    }

    public final boolean isFile(final String path)
    throws FalsePositiveException {
        try {
            return isFile0(path);
        } catch (FileArchiveEntryFalsePositiveException ex) {
            // TODO: Document this!
            if (isRoot(path)
            && ex.getCause() instanceof FileNotFoundException)
                return false;
            return enclController.isFile(enclEntryName(path));
        } catch (DirectoryArchiveEntryFalsePositiveException ex) {
            return enclController.isFile(enclEntryName(path));
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
            final ArchiveFileSystem fileSystem = autoMount(false);
            return fileSystem.isFile(path);
        } finally {
            readLock().unlock();
        }
    }

    public final boolean isDirectory(final String path)
    throws FalsePositiveException {
        try {
            return isDirectory0(path);
        } catch (FileArchiveEntryFalsePositiveException ex) {
            return false;
        } catch (DirectoryArchiveEntryFalsePositiveException ex) {
            return enclController.isDirectory(enclEntryName(path));
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
            final ArchiveFileSystem fileSystem = autoMount(false);
            return fileSystem.isDirectory(path);
        } finally {
            readLock().unlock();
        }
    }

    public final Icon getOpenIcon(final String path)
    throws FalsePositiveException {
        try {
            return getOpenIcon0(path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.getOpenIcon(enclEntryName(path));
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
            autoMount(false); // detect false positives!
            return isRoot(path)
                    ? getDriver().getOpenIcon(this)
                    : null;
        } finally {
            readLock().unlock();
        }
    }

    public final Icon getClosedIcon(final String path)
    throws FalsePositiveException {
        try {
            return getClosedIcon0(path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.getClosedIcon(enclEntryName(path));
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
            autoMount(false); // detect false positives!
            return isRoot(path)
                    ? getDriver().getClosedIcon(this)
                    : null;
        } finally {
            readLock().unlock();
        }
    }

    public final boolean canRead(final String path)
    throws FalsePositiveException {
        try {
            return canRead0(path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.canRead(enclEntryName(path));
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    private boolean canRead0(final String path)
    throws FalsePositiveException, IOException {
        readLock().lock();
        try {
            final ArchiveFileSystem fileSystem = autoMount(false);
            return fileSystem.exists(path);
        } finally {
            readLock().unlock();
        }
    }

    public final boolean canWrite(final String path)
    throws FalsePositiveException {
        try {
            return canWrite0(path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.canWrite(enclEntryName(path));
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    private boolean canWrite0(final String path)
    throws FalsePositiveException, IOException {
        readLock().lock();
        try {
            final ArchiveFileSystem fileSystem = autoMount(false);
            return fileSystem.canWrite(path);
        } finally {
            readLock().unlock();
        }
    }

    public final long length(final String path)
    throws FalsePositiveException {
        try {
            return length0(path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.length(enclEntryName(path));
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return 0;
        }
    }

    private long length0(final String path)
    throws FalsePositiveException, IOException {
        readLock().lock();
        try {
            final ArchiveFileSystem fileSystem = autoMount(false);
            return fileSystem.length(path);
        } finally {
            readLock().unlock();
        }
    }

    public final long lastModified(final String path)
    throws FalsePositiveException {
        try {
            return lastModified0(path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.lastModified(enclEntryName(path));
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return 0;
        }
    }

    private long lastModified0(final String path)
    throws FalsePositiveException, IOException {
        readLock().lock();
        try {
            final ArchiveFileSystem fileSystem = autoMount(false);
            return fileSystem.lastModified(path);
        } finally {
            readLock().unlock();
        }
    }

    public final String[] list(final String path)
    throws FalsePositiveException {
        try {
            return list0(path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.list(enclEntryName(path));
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return null;
        }
    }

    private String[] list0(final String path)
    throws FalsePositiveException, IOException {
        readLock().lock();
        try {
            final ArchiveFileSystem fileSystem = autoMount(false);
            return fileSystem.list(path);
        } finally {
            readLock().unlock();
        }
    }

    public final String[] list(
            final String path,
            final FilenameFilter filenameFilter,
            final File dir)
    throws FalsePositiveException {
        try {
            return list0(path, filenameFilter, dir);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.list(enclEntryName(path),
                    filenameFilter, dir);
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return null;
        }
    }

    private String[] list0(
            final String path,
            final FilenameFilter filenameFilter,
            final File dir)
    throws FalsePositiveException, IOException {
        readLock().lock();
        try {
            final ArchiveFileSystem fileSystem = autoMount(false);
            return fileSystem.list(path, filenameFilter, dir);
        } finally {
            readLock().unlock();
        }
    }

    public final File[] listFiles(
            final String path,
            final FilenameFilter filenameFilter,
            final File dir,
            final FileFactory factory)
    throws FalsePositiveException {
        try {
            return listFiles0(path, filenameFilter, dir, factory);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.listFiles(enclEntryName(path),
                    filenameFilter, dir, factory);
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return null;
        }
    }

    private File[] listFiles0(
            final String path,
            final FilenameFilter filenameFilter,
            final File dir,
            final FileFactory factory)
    throws FalsePositiveException, IOException {
        readLock().lock();
        try {
            final ArchiveFileSystem fileSystem = autoMount(false);
            return fileSystem.listFiles(path, filenameFilter, dir, factory);
        } finally {
            readLock().unlock();
        }
    }

    public final File[] listFiles(
            final String path,
            final FileFilter fileFilter,
            final File dir,
            final FileFactory factory)
    throws FalsePositiveException {
        try {
            return listFiles0(path, fileFilter, dir, factory);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.listFiles(enclEntryName(path),
                    fileFilter, dir, factory);
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return null;
        }
    }

    private File[] listFiles0(
            final String path,
            final FileFilter fileFilter,
            final File dir,
            final FileFactory factory)
    throws FalsePositiveException, IOException {
        readLock().lock();
        try {
            final ArchiveFileSystem fileSystem = autoMount(false);
            return fileSystem.listFiles(path, fileFilter, dir, factory);
        } finally {
            readLock().unlock();
        }
    }

    //
    // File system operations used by the File class.
    // Write operations:
    //

    public final boolean setReadOnly(final String path)
    throws FalsePositiveException {
        try {
            return setReadOnly0(path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.setReadOnly(enclEntryName(path));
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    private boolean setReadOnly0(final String path)
    throws FalsePositiveException, IOException {
        writeLock().lock();
        try {
            final ArchiveFileSystem fileSystem = autoMount(false);
            return fileSystem.setReadOnly(path);
        } finally {
            writeLock().unlock();
        }
    }

    public final boolean setLastModified(
            final String path,
            final long time)
    throws FalsePositiveException {
        try {
            return setLastModified0(path, time);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.setLastModified(enclEntryName(path),
                    time);
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
            autoUmount(path);
            final ArchiveFileSystem fileSystem = autoMount(false);
            return fileSystem.setLastModified(path, time);
        } finally {
            writeLock().unlock();
        }
    }

    public final boolean createNewFile(
            final String path,
            final boolean autoCreate)
    throws FalsePositiveException, IOException {
        try {
            return createNewFile0(path, autoCreate);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.createNewFile(enclEntryName(path),
                    autoCreate);
        }
    }

    private boolean createNewFile0(
            final String path,
            final boolean autoCreate)
    throws FalsePositiveException, IOException {
        assert !isRoot(path);

        writeLock().lock();
        try {
            final ArchiveFileSystem fileSystem = autoMount(autoCreate);
            if (fileSystem.exists(path))
                return false;

            // If we got until here without an exception,
            // write an empty file now.
            newOutputStream0(path, false).close();

            return true;
        } finally {
            writeLock().unlock();
        }
    }

    public final boolean mkdir(
            final String path,
            final boolean autoCreate)
    throws FalsePositiveException {
        try {
            mkdir0(path, autoCreate);
            return true;
        } catch (ArchiveEntryFalsePositiveException ex) {
            return enclController.mkdir(enclEntryName(path), autoCreate);
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    private void mkdir0(final String path, final boolean autoCreate)
    throws FalsePositiveException, IOException {
        writeLock().lock();
        try {
            if (isRoot(path)) {
                // This is the virtual root of an archive file system, so we
                // are actually working on the controller's target file.
                if (isRfsEntryTarget()) {
                    if (target.exists())
                        throw new IOException("target file exists already!");
                } else {
                    if (enclController.exists(enclEntryName))
                        throw new IOException("target file exists already!");
                }
                // Ensure file system existence.
                autoMount(true);
            } else { // !isRoot(entryName)
                // This file is a regular archive entry.
                final ArchiveFileSystem fileSystem = autoMount(autoCreate);
                fileSystem.mkdir(path, autoCreate);
            }
        } finally {
            writeLock().unlock();
        }
    }

    public final boolean delete(final String path)
    throws FalsePositiveException {
        try {
            delete0(path);
            return true;
        } catch (DirectoryArchiveEntryFalsePositiveException ex) {
            return enclController.delete(enclEntryName(path));
        } catch (FileArchiveEntryFalsePositiveException ex) {
            // TODO: Document this!
            if (isRoot(path)
            && !enclController.isDirectory(enclEntryName(path))
            && ex.getCause() instanceof FileNotFoundException)
                return false;
            return enclController.delete(enclEntryName(path));
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
            autoUmount(path);

            if (isRoot(path)) {
                // Get the file system or die trying!
                final ArchiveFileSystem fileSystem;
                try {
                    fileSystem = autoMount(false);
                } catch (FalsePositiveException ex) {
                    // The File instance is going to delete the target file
                    // anyway, so we need to reset now.
                    try {
                        reset();
                    } catch (SyncException cannotHappen) {
                        throw new AssertionError(cannotHappen);
                    }
                    throw ex;
                }

                // We are actually working on the controller's target file.
                // Do not use the number of entries in the file system
                // for the following test - it's size would count absolute
                // pathnames as well!
                final String[] members = fileSystem.list(path);
                if (members != null && members.length != 0)
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
                // way to model this.
                PromptingKeyManager.resetKeyProvider(getCanonicalPath());
                // Delete the target file or the entry in the enclosing
                // archive file, too.
                if (isRfsEntryTarget()) {
                    // The target file of the controller is NOT enclosed
                    // in another archive file.
                    if (!target.delete())
                        throw new IOException("couldn't delete archive file!");
                } else {
                    // The target file of the controller IS enclosed in
                    // another archive file.
                    enclController.delete0(enclEntryName(path));
                }
            } else { // !isRoot(entryName)
                final ArchiveFileSystem fileSystem = autoMount(false);
                fileSystem.delete(path);
            }
        } finally {
            writeLock().unlock();
        }
    }
}
