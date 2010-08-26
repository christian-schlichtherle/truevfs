/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

import de.schlichtherle.truezip.io.File;
import de.schlichtherle.truezip.io.archive.controller.ArchiveController.ArchiveEntryFalsePositiveException;
import de.schlichtherle.truezip.io.archive.controller.ArchiveFileSystem.Delta;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.driver.RfsEntry;
import de.schlichtherle.truezip.io.util.Files;
import de.schlichtherle.truezip.io.util.InputException;
import de.schlichtherle.truezip.io.util.Streams;
import de.schlichtherle.truezip.key.PromptingKeyManager;
import de.schlichtherle.truezip.util.Action;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides static utility methods for {@link ArchiveController}s.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class ArchiveControllers {

    private static final String CLASS_NAME
            = ArchiveControllers.class.getName();
    private static final Logger logger
            = Logger.getLogger(CLASS_NAME, CLASS_NAME);

    /**
     * The map of all archive controllers.
     * The keys are plain {@link java.io.File} instances and the values
     * are either {@code ArchiveController}s or {@link WeakReference}s
     * to {@code ArchiveController}s.
     * All access to this map must be externally synchronized!
     */
    private static final Map<java.io.File, Object> controllers
            = new WeakHashMap<java.io.File, Object>();

    private static final Comparator<ArchiveController> REVERSE_CONTROLLERS
            = new Comparator<ArchiveController>() {
        public int compare(ArchiveController l, ArchiveController r) {
            return  r.getTarget().compareTo(l.getTarget());
        }
    };

    static int getArchivesTotal() {
        // This is not 100% accurate:
        // Controllers which have been removed from the WeakReference
        // VALUE in the map meanwhile, but not yet removed from the map,
        // are counted as well.
        // But hey, this is only statistics, right?
        return controllers.size();
    }

    /** You cannot instantiate this class. */
    private ArchiveControllers() {
    }

    /**
     * Factory method returning an {@link ArchiveController} object for the
     * given archive file.
     * <p>
     * <b>Note:</b>
     * <ul>
     * <li>Neither {@code file} nor the enclosing archive file(s)
     *     need to actually exist for this to return a valid {@code ArchiveController}.
     *     Just the parent directories of {@code file} need to look like either
     *     an ordinary directory or an archive file, e.g. their lowercase
     *     representation needs to have a .zip or .jar ending.</li>
     * <li>It is an error to call this method on a target file which is
     *     not a valid name for an archive file</li>
     * </ul>
     */
    public static ArchiveController get(final File file) {
        assert file != null;
        assert file.isArchive();

        final java.io.File target = Files.getCanOrAbsFile(file.getDelegate());
        final ArchiveDriver driver = file.getArchiveDetector()
                .getArchiveDriver(target.getPath());
        assert driver != null : "Not an archive file: " + file.getPath();

        ArchiveController controller = null;
        boolean reconfigure = false;
        try {
            synchronized (controllers) {
                final Object value = controllers.get(target);
                if (value instanceof Reference) {
                    controller = (ArchiveController) ((Reference) value).get();
                    // Check that the controller hasn't been garbage collected
                    // meanwhile!
                    if (controller != null) {
                        // If required, reconfiguration of the ArchiveController
                        // must be deferred until we have released the lock on
                        // controllers in order to prevent dead locks.
                        reconfigure = controller.getDriver() != driver;
                        return controller;
                    }
                } else if (value != null) {
                    // Do NOT reconfigure this ArchiveController with another
                    // ArchiveDetector: This controller is touched, i.e. it
                    // most probably has mounted the virtual file system and
                    // using another ArchiveDetector could potentially break
                    // the umount process.
                    // In effect, for an application this means that the
                    // reconfiguration of a previously used ArchiveController
                    // is only guaranteed to happen if
                    // (1) File.umount() or File.umount() has been called and
                    // (2) a new File instance referring to the previously used
                    // archive file as either the file itself or one
                    // of its ancestors is created with a different
                    // ArchiveDetector.
                    return (ArchiveController) value;
                }

                final File enclArchive = file.getEnclArchive();
                final ArchiveController enclController;
                final String enclEntryName;
                if (enclArchive != null) {
                    enclController = enclArchive.getArchiveController();
                    enclEntryName = file.getEnclEntryName();
                } else {
                    enclController = null;
                    enclEntryName = null;
                }

                // TODO: Refactor this to a more flexible design which supports
                // different umount strategies, like update or append.
                controller = new UpdatingArchiveController(
                        target, enclController, enclEntryName, driver);
            }
        } finally {
            if (reconfigure) {
                controller.writeLock().lock();
                try {
                    controller.setDriver(driver);
                } finally {
                    controller.writeLock().unlock();
                }
            }
        }

        return controller;
    }

    /**
     * Associates the given archive controller to the target file.
     *
     * @param target The target file. This must not be {@code null} or
     *        an instance of the {@code File} class in this package!
     * @param controller An {@link ArchiveController} or a
     *        {@link WeakReference} to an {@link ArchiveController}.
     */
    static void set(final java.io.File target, final Object controller) {
        assert target != null;
        assert !(target instanceof File);
        assert controller instanceof ArchiveController
            || ((WeakReference) controller).get() instanceof ArchiveController;

        synchronized (controllers) {
            controllers.put(target, controller);
        }
    }

    /**
     * Updates the real file system with all changes to archive files which's
     * canonical path name start with {@code prefix}.
     * This will reset the state of the respective archive controller and
     * delete all temporary files held for the selected archive files.
     * This method is thread-safe.
     *
     * @param prefix The prefix of the canonical path name of the archive files
     *        which shall get synchronized to the real file system.
     *        This may be {@code null} or empty in order to select all accessed
     *        archive files.
     * @throws ArchiveWarningException If the configuration uses the
     *         {@link DefaultArchiveFileExceptionBuilder} and <em>only</em>
     *         warning conditions occured throughout the course of this method.
     *         This implies that the respective archive file has been updated
     *         with constraints, such as a failure to set the last modification
     *         time of the archive file to the last modification time of its
     *         implicit root directory.
     * @throws ArchiveWarningException If the configuration uses the
     *         {@link DefaultArchiveFileExceptionBuilder} and any error
     *         condition occured throughout the course of this method.
     *         This implies loss of data!
     * @throws NullPointerException If {@code config} is {@code null}.
     * @throws IllegalArgumentException If the configuration property
     *         {@code closeInputStreams} is {@code false} and
     *         {@code closeOutputStreams} is {@code true}.
     */
    public static void umount(final String prefix, UmountConfiguration config)
    throws ArchiveFileException {
        if (!config.getCloseInputStreams() && config.getCloseOutputStreams())
            throw new IllegalArgumentException();
        config = config.setReassemble(true);

        int total = 0, touched = 0;
        logger.log(Level.FINE, "update.entering", // NOI18N
                new Object[] {
            prefix,
            config.getWaitForInputStreams(),
            config.getCloseInputStreams(),
            config.getWaitForOutputStreams(),
            config.getCloseOutputStreams(),
            config.getRelease(),
        });
        try {
            // Reset statistics if it hasn't happened yet.
            CountingReadOnlyFile.init();
            CountingOutputStream.init();
            try {
                final ArchiveFileExceptionBuilder builder
                        = config.getArchiveFileExceptionBuilder();
                // The general algorithm is to sort the targets in descending order
                // of their pathnames (considering the system's default name
                // separator character) and then walk the array in reverse order to
                // call the umount() method on each respective archive controller.
                // This ensures that an archive file will always be updated
                // before its enclosing archive file.
                for (final ArchiveController controller
                        : get(prefix, REVERSE_CONTROLLERS)) {
                    controller.writeLock().lock();
                    try {
                        if (controller.isTouched())
                            touched++;
                        try {
                            // Upon return, some new ArchiveWarningException's may
                            // have been generated. We need to remember them for
                            // later throwing.
                            controller.umount(config);
                        } catch (ArchiveFileException exception) {
                            // Updating the archive file or wrapping it back into
                            // one of it's enclosing archive files resulted in an
                            // exception for some reason.
                            // We are bullheaded and store the exception chain for
                            // later throwing only and continue updating the rest.
                            builder.warn(exception);
                        }
                    } finally {
                        controller.writeLock().unlock();
                    }
                    total++;
                }
                builder.check();
            } finally {
                CountingReadOnlyFile.resetOnInit();
                CountingOutputStream.resetOnInit();
            }
        } catch (ArchiveFileException chain) {
            logger.log(Level.FINE, "update.throwing", chain);// NOI18N
            throw chain;
        }
        logger.log(Level.FINE, "update.exiting", // NOI18N
                new Object[] { total, touched });
    }

    static Iterable<ArchiveController> get() {
        return get(null, null);
    }

    static Iterable<ArchiveController> get(String prefix, final Comparator c) {
        if (prefix == null)
            prefix = "";

        final Set<ArchiveController> snapshot;
        synchronized (controllers) {
            snapshot = c != null
                    ? new TreeSet(c)
                    : new HashSet((int) (controllers.size() / 0.75f));
            for (Object value : controllers.values()) {
                if (value instanceof Reference) {
                    value = ((Reference) value).get(); // dereference
                    if (value == null) {
                        // This may happen if there are no more strong
                        // references to the controller and it has been
                        // removed from the weak reference in the hash
                        // map's value before it's been removed from the
                        // hash map's key (shit happens)!
                        continue;
                    }
                }
                assert value != null;
                assert value instanceof ArchiveController;
                if (((ArchiveController) value).getCanonicalPath().startsWith(prefix))
                    snapshot.add((ArchiveController) value);
            }
        }
        return snapshot;
    }

    /**
     * Returns a proxy instance which encapsulates <em>live</em> statistics
     * about the total set of archive files accessed by this package.
     * Any call to a method of the returned interface instance returns
     * up-to-date data, so there is no need to repeatedly call this method in
     * order to optain updated statistics.
     * <p>
     * Note that this method returns <em>live</em> statistics rather than
     * <em>real time</em> statistics.
     * So there may be a slight delay until the values returned reflect
     * the actual state of this package.
     * This delay increases if the system is under heavy load.
     */
    public static ArchiveStatistics getLiveArchiveStatistics() {
        return LiveArchiveStatistics.SINGLETON;
    }

    /**
     * Adds the given {@code runnable} to the set of runnables to run by a
     * shutdown hook.
     * This is typically used to delete archive files or entries.
     */
    public static void addToShutdownHook(final Runnable runnable) {
        //ShutdownHook.SINGLETON.add(runnable);
        JVMShutdownHook.SINGLETON.add(runnable);
    }

    /**
     * This singleton shutdown hook runnable class runs a set of user-provided
     * runnables which may perform cleanup tasks when it's {@link #run()}
     * method is invoked.
     * This is typically used to delete archive files or entries.
     */
    static final class ShutdownHook implements Runnable {

        /** The singleton instance of this class. */
        public static final ShutdownHook SINGLETON = new ShutdownHook();

        private final Collection<Runnable> runnables = new HashSet<Runnable>();

        /** You cannot instantiate this class. */
        private ShutdownHook() {
            // Force loading the key manager now in order to prevent class
            // loading when running the shutdown hook.
            // This may help if this shutdown hook is run as a JVM shutdown
            // hook in an app server environment where class loading is
            // disabled.
            PromptingKeyManager.getInstance();
        }

        /**
         * Adds the given {@code runnable} to the set of runnables to run by
         * this shutdown hook.
         */
        public synchronized void add(final Runnable runnable) {
            if (runnable != null)
                runnables.add(runnable);
        }

        /**
         * Runs all runnables added to the set.
         * <p>
         * Password prompting will be disabled in order to avoid
         * {@link RuntimeException}s or even {@link Error}s in this shutdown
         * hook.
         * <p>
         * Note that this method is <em>not</em> re-entrant and should not be
         * directly called except for unit testing.
         */
        @SuppressWarnings({"NestedSynchronizedStatement", "CallToThreadDumpStack"})
        @Override
        public void run() {
            synchronized (PromptingKeyManager.class) {
                try {
                    // paranoid, but safe.
                    PromptingKeyManager.setPrompting(false);
                    //ArchiveControllers.logger.setLevel(Level.OFF);
                    synchronized (this) {
                        for (Runnable runnable : runnables)
                            runnable.run();
                    }
                } finally {
                    try {
                        ArchiveControllers.umount("",
                                new UmountConfiguration()
                                .setWaitForInputStreams(false)
                                .setCloseInputStreams(true)
                                .setWaitForOutputStreams(false)
                                .setCloseOutputStreams(true)
                                .setRelease(true));
                    } catch (ArchiveFileException ouch) {
                        ouch.printStackTrace();
                    }
                }
            }
        }
    } // class ShutdownHook

    /**
     * Copies a source file to a destination file, optionally preserving the
     * source's last modification time.
     * We know that the source and destination files both appear to be entries
     * in an archive file.
     *
     * @throws FalsePositiveException If the source or the destination is a
     *         false positive and the exception for the destination
     *         cannot get resolved within this method.
     * @throws InputException If copying the data fails because of an
     *         IOException in the source.
     * @throws IOException If copying the data fails because of an
     *         IOException in the destination.
     */
    public static void cp(
            final boolean preserve,
            final ArchiveController srcController,
            final String srcEntryName,
            final ArchiveController dstController,
            final String dstEntryName)
            throws IOException {
        // Do not assume anything about the lock status of the controller:
        // This method may be called from a subclass while a lock is acquired!
        //assert !srcController.readLock().isLocked();
        //assert !srcController.writeLock().isLocked();
        //assert !dstController.readLock().isLocked();
        //assert !dstController.writeLock().isLocked();

        try {
            class IOStreamCreator implements Action<IOException> {

                InputStream in;
                OutputStream out;

                public void run() throws IOException {
                    // Update controllers.
                    // This may invalidate the file system object, so it must be
                    // done first in case srcController and dstController are the
                    // same!
                    class SrcControllerUpdater implements Action<IOException> {

                        public void run() throws IOException {
                            srcController.autoUmount(srcEntryName);
                            srcController.readLock().lock(); // downgrade to read lock upon return
                        }
                    } // class SrcControllerUpdater

                    final ArchiveEntry srcEntry, dstEntry;
                    final Delta delta;
                    srcController.runWriteLocked(new SrcControllerUpdater());
                    try {
                        dstController.autoUmount(dstEntryName);

                        // Get source archive entry.
                        final ArchiveFileSystem srcFileSystem = srcController.autoMount(false);
                        srcEntry = srcFileSystem.get(srcEntryName);

                        // Get destination archive entry.
                        final boolean lenient = isLenient();
                        final ArchiveFileSystem dstFileSystem = dstController.autoMount(lenient);
                        delta = dstFileSystem.link(dstEntryName,
                                lenient, preserve ? srcEntry : null);
                        dstEntry = delta.getEntry();

                        // Create input stream.
                        in = srcController.createInputStream(srcEntry, dstEntry);
                    } finally {
                        srcController.readLock().unlock();
                    }

                    try {
                        // Create output stream.
                        out = dstController.createOutputStream(dstEntry, srcEntry);

                        try {
                            // Now link the destination entry into the file system.
                            delta.commit();
                        } catch (IOException ex) {
                            out.close();
                            throw ex;
                        }
                    } catch (IOException ex) {
                        try {
                            in.close();
                        } catch (IOException inFailure) {
                            throw new InputException(inFailure);
                        }
                        throw ex;
                    }
                }
            } // class IOStreamCreator

            final IOStreamCreator streams = new IOStreamCreator();
            synchronized (copyLock) {
                dstController.runWriteLocked(streams);
            }

            // Finally copy the entry data.
            Streams.cp(streams.in, streams.out);
        } catch (ArchiveEntryFalsePositiveException ex) {
            // Both the source and/or the destination may be false positives,
            // so we need to use the exception's additional information to
            // find out which controller actually detected the false positive.
            if (dstController != ex.getController()) {
                throw ex; // not my job - pass on!
            }      // Reroute call to the destination's enclosing archive controller.
            cp(preserve, srcController, srcEntryName,
                    dstController.getEnclController(),
                    dstController.enclEntryName(dstEntryName));
        }
    }

    /**
     * Copies a source file to a destination file, optionally preserving the
     * source's last modification time.
     * We already have an input stream to read the source file and the
     * destination appears to be an entry in an archive file.
     * Note that this method <em>never</em> closes the given input stream!
     * <p>
     * Note that this method synchronizes on the class object in order
     * to prevent dead locks by two threads copying archive entries to the
     * other's source archive concurrently!
     *
     * @throws FalsePositiveException If the destination is a
     *         false positive and the exception
     *         cannot get resolved within this method.
     * @throws InputException If copying the data fails because of an
     *         IOException in the source.
     * @throws IOException If copying the data fails because of an
     *         IOException in the destination.
     */
    public static void cp(
            final boolean preserve,
            final java.io.File src,
            final InputStream in,
            final ArchiveController dstController,
            final String dstEntryName)
            throws IOException {
        // Do not assume anything about the lock status of the controller:
        // This method may be called from a subclass while a lock is acquired!
        //assert !dstController.readLock().isLocked();
        //assert !dstController.writeLock().isLocked();

        try {
            class OStreamCreator implements Action<IOException> {

                OutputStream out; // = null;

                public void run() throws IOException {
                    // Update controller.
                    // This may invalidate the file system object, so it must be
                    // done first in case srcController and dstController are the
                    // same!
                    dstController.autoUmount(dstEntryName);

                    final boolean lenient = isLenient();

                    // Get source archive entry.
                    final ArchiveEntry srcEntry = new RfsEntry(src);

                    // Get destination archive entry.
                    final ArchiveFileSystem dstFileSystem = dstController.autoMount(lenient);
                    final Delta delta = dstFileSystem.link(dstEntryName,
                            lenient, preserve ? srcEntry : null);
                    final ArchiveEntry dstEntry = delta.getEntry();

                    // Create output stream.
                    out = dstController.createOutputStream(dstEntry, srcEntry);

                    // Now link the destination entry into the file system.
                    delta.commit();
                }
            }

            // Create the output stream while the destination controller is
            // write locked.
            final OStreamCreator stream = new OStreamCreator();
            dstController.runWriteLocked(stream);
            final OutputStream out = stream.out;

            // Finally copy the entry data.
            try {
                Streams.cat(in, out);
            } finally {
                out.close();
            }
        } catch (ArchiveEntryFalsePositiveException ex) {
            assert dstController == ex.getController();
            // Reroute call to the destination's enclosing ArchiveController.
            cp(preserve, src, in,
                    dstController.getEnclController(),
                    dstController.enclEntryName(dstEntryName));
        }
    }

    //
    // Static member classes and interfaces.
    //
    /**
     * A lock used when copying data from one archive to another.
     * This lock must be acquired before any other locks on the controllers
     * are acquired in order to prevent dead locks.
     */
    private static class CopyLock {
    }
    /**
     * A lock used when copying data from one archive file to another.
     * This lock must be acquired before any other locks on the controllers
     * are acquired in order to prevent dead locks.
     */
    private static final CopyLock copyLock = new CopyLock();

    /**
     * Returns the value of the class property {@code lenient}.
     * By default, this is the inverse of the boolean system property
     * {@code de.schlichtherle.truezip.io.archive.controllers.ArchiveControllers.strict}.
     * In other words, this returns {@code true} unless you set the
     * system property
     * {@code de.schlichtherle.truezip.io.archive.controllers.ArchiveControllers.strict}
     * to {@code true} or call {@link #setLenient(boolean) setLenient(false)}.
     *
     * @see #setLenient(boolean)
     */
    public static boolean isLenient() {
        return lenient;
    }

    /**
     * This class property controls whether (1) archive files and enclosed
     * directories shall be created on the fly if they don't exist and (2)
     * open archive entry streams should automatically be closed if they are
     * only weakly reachable.
     * By default, this class property is {@code true}.
     * <ol>
     * <li>
     * Consider the following path: &quot;a/outer.zip/b/inner.zip/c&quot;.
     * Now let's assume that &quot;a&quot; exists as a directory in the real file
     * system, while all other parts of this path don't, and that TrueZIP's
     * default configuration is used which would recognize &quot;outer.zip&quot; and
     * &quot;inner.zip&quot; as ZIP files.
     * <p>
     * If this class property is set to {@code false}, then
     * the client application would have to call
     * {@code new File(&quot;a/outer.zip/b/inner.zip&quot;).mkdirs()}
     * before it could actually create the innermost &quot;c&quot; entry as a file
     * or directory.
     * <p>
     * More formally, before you can access a node in the virtual file
     * system, all its parent directories must exist, including archive
     * files. This emulates the behaviour of real file systems.
     * <p>
     * If this class property is set to {@code true} however, then
     * any missing parent directories (including archive files) up to the
     * outermost archive file (&quot;outer.zip&quot;) are created on the fly when using
     * operations to create the innermost element of the path (&quot;c&quot;).
     * <p>
     * This allows applications to succeed when doing this:
     * {@code new File(&quot;a/outer.zip/b/inner.zip/c&quot;).createNewFile()},
     * or that:
     * {@code new FileOutputStream(&quot;a/outer.zip/b/inner.zip/c&quot;)}.
     * <p>
     * Note that in any case the parent directory of the outermost archive
     * file (&quot;a&quot;), must exist - TrueZIP does not create regular directories
     * in the real file system on the fly.
     * </li>
     * <li>
     * Many Java applications unfortunately fail to close their streams in all
     * cases, in particular if an {@code IOException} occured while
     * accessing it.
     * However, open streams are a limited resource in any operating system
     * and may interfere with other services of the OS (on Windows, you can't
     * delete an open file).
     * This is called the &quot;unclosed streams issue&quot;.
     * <p>
     * Likewise, in TrueZIP an unclosed archive entry stream may result in an
     * {@code ArchiveFileBusy(Warning)?Exception} to be thrown when
     * {@link #umount} is called.
     * In order to prevent this, TrueZIP's archive entry streams have a
     * {@link Object#finalize()} method which closes an archive entry stream
     * if its garbage collected.
     * <p>
     * Now if this class property is set to {@code false}, then
     * TrueZIP maintains a hard reference to all archive entry streams
     * until {@link #umount} is called, which will deal
     * with them: If they are not closed, an
     * {@code ArchiveFileBusy(Warning)?Exception} is thrown, depending on
     * the boolean parameters to these methods.
     * <p>
     * This setting is useful if you do not want to tolerate the
     * &quot;unclosed streams issue&quot; in a client application.
     * <p>
     * If this class property is set to {@code true} however, then
     * TrueZIP maintains only a weak reference to all archive entry streams.
     * This allows the garbage collector to finalize them before
     * {@link #umount} is called.
     * The finalize() method will then close these archive entry streams,
     * which exempts them, from triggering an
     * {@code ArchiveBusy(Warning)?Exception} on the next call to
     * {@link #umount}.
     * However, closing an archive entry output stream this way may result
     * in loss of buffered data, so it's only a workaround for this issue.
     * <p>
     * Note that for the setting of this class property to take effect, any
     * change must be made before an archive is first accessed.
     * The setting will then persist until the archive is reset by the next
     * call to {@link #umount}.
     * </li>
     * </ol>
     *
     * @see #isLenient()
     */
    public static void setLenient(final boolean lenient) {
        ArchiveControllers.lenient = lenient;
    }

    private static boolean lenient
            = !Boolean.getBoolean(ArchiveControllers.class.getName() + ".strict");
}
