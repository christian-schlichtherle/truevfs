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

import de.schlichtherle.truezip.io.archive.controller.ArchiveController.IOOption;
import de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption;
import de.schlichtherle.truezip.io.archive.ArchiveDescriptor;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry;
import de.schlichtherle.truezip.io.socket.file.FileEntry;
import de.schlichtherle.truezip.io.socket.input.CommonInputSocket;
import de.schlichtherle.truezip.io.socket.output.CommonOutputSocket;
import de.schlichtherle.truezip.io.socket.file.FileIOProvider;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.socket.IOSocket;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.key.PromptingKeyManager;
import de.schlichtherle.truezip.util.BitField;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.IOOption.CREATE_PARENTS;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.IOOption.PRESERVE;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.CLOSE_INPUT_STREAMS;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.CLOSE_OUTPUT_STREAMS;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.REASSEMBLE;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.UMOUNT;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.WAIT_FOR_INPUT_STREAMS;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.WAIT_FOR_OUTPUT_STREAMS;
import static de.schlichtherle.truezip.io.archive.driver.ArchiveEntry.SEPARATOR;
import static de.schlichtherle.truezip.io.archive.driver.ArchiveEntry.SEPARATOR_CHAR;

/**
 * Provides static utility methods for {@link ArchiveController}s.
 * This class cannot get instantiated outside its package.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ArchiveControllers {

    private static final String CLASS_NAME
            = ArchiveControllers.class.getName();
    private static final Logger logger
            = Logger.getLogger(CLASS_NAME, CLASS_NAME);

    private static final Comparator<ArchiveController> REVERSE_CONTROLLERS
            = new Comparator<ArchiveController>() {
        public int compare(ArchiveController l, ArchiveController r) {
            return  r.getMountPoint().compareTo(l.getMountPoint());
        }
    };

    /**
     * The map of all archive controllers.
     * The keys are plain {@link URI} instances and the values are either
     * {@code ArchiveController}s or {@link WeakReference}s to
     * {@code ArchiveController}s.
     * All access to this map must be externally synchronized!
     */
    private static final Map<URI, Object> controllers = new WeakHashMap<URI, Object>();

    private ArchiveControllers() {
    }

    static int getArchivesTotal() {
        // This is not 100% accurate:
        // Controllers which have been removed from the WeakReference
        // VALUE in the map meanwhile, but not yet removed from the map,
        // are counted as well.
        // But hey, this is only statistics, right?
        return controllers.size();
    }

    public static ArchiveController getController(URI mountPoint) {
        return getController(mountPoint, null, null);
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
    public static <AE extends ArchiveEntry> ArchiveController getController(
            URI mountPoint,
            final ArchiveController<?> enclController,
            final ArchiveDriver<AE> driver) {
        if (!mountPoint.isAbsolute()) throw new IllegalArgumentException();
        if (mountPoint.isOpaque()) throw new IllegalArgumentException();
        //if (!mountPoint.equals(mountPoint.normalize())) throw new IllegalArgumentException();
        mountPoint = URI.create(mountPoint.toString() + SEPARATOR_CHAR).normalize();
        assert mountPoint.getPath().endsWith(SEPARATOR);
        synchronized (controllers) {
            final Object value = controllers.get(mountPoint);
            if (value instanceof Reference) {
                final ArchiveController controller
                        = (ArchiveController) ((Reference) value).get();
                // Check that the controller hasn't been garbage collected
                // meanwhile!
                if (controller != null) {
                    // If required, reconfiguration of the ArchiveController
                    // must be deferred until we have released the lock on
                    // controllers in order to prevent dead locks.
                    //reconfigure = driver != null && driver != controller.getDriver();
                    return controller;
                }
                // Fall through!
            } else if (value != null) {
                // Do NOT reconfigure this ArchiveController with another
                // ArchiveDetector: This controller is touched, i.e. it
                // most probably has mounted the virtual file system and
                // using another ArchiveDetector could potentially break
                // the sync process.
                // Effectively, this means that the reconfiguration of a
                // previously created ArchiveController is only guaranteed
                // to happen if
                // (1) sync(*) has been called and
                // (2) a new File object referring to the previously used
                //     archive file as either the file itself or one of its
                //     ancestors is created with a different
                //     ArchiveDetector.
                return (ArchiveController) value;
            }
            if (driver == null) // pure lookup operation?
                return null;
            // TODO: Refactor this to a more flexible design which supports
            // different sync strategies, like update or append.
            final ArchiveModel model = new ArchiveModel<AE>(mountPoint,
                    null == enclController ? null : enclController.getMountPoint());
            return new UpdatingArchiveController<AE>(model , driver);
        }
    }

    /**
     * Associates the given archive controller to its mount point.
     *
     * @param mountPoint the non-{@code null} URI for the mount point of the
     *        target archive file.
     * @param controller An {@link ArchiveController} or a
     *        {@link WeakReference} to an {@link ArchiveController}.
     * @see   ArchiveDescriptor#getMountPoint()
     */
    static void map(URI mountPoint, final Object controller) {
        assert mountPoint.isAbsolute();
        assert !mountPoint.isOpaque();
        assert mountPoint.equals(URI.create(mountPoint.toString() + SEPARATOR_CHAR).normalize());
        assert controller instanceof ArchiveController
            || ((WeakReference) controller).get() instanceof ArchiveController;

        synchronized (controllers) {
            controllers.put(mountPoint, controller);
        }
    }

    /**
     * Writes all changes to the contents of the target archive files who's
     * canonical path name starts with the given {@code prefix} to the
     * underlying file system.
     * This will reset the state of the respective archive controllers.
     * This method is thread-safe.
     *
     * @param prefix The prefix of the canonical path name of the archive files
     *        which shall get synchronized to the real file system.
     *        This may be {@code null} or empty in order to select all accessed
     *        archive files.
     * @throws ArchiveWarningException if the configuration uses the
     *         {@link DefaultArchiveSyncExceptionBuilder} and <em>only</em>
     *         warning conditions occured throughout the course of this method.
     *         This implies that the respective archive file has been updated
     *         with constraints, such as a failure to set the last modification
     *         time of the archive file to the last modification time of its
     *         implicit root directory.
     * @throws ArchiveWarningException if the configuration uses the
     *         {@link DefaultArchiveSyncExceptionBuilder} and any error
     *         condition occured throughout the course of this method.
     *         This implies loss of data!
     * @throws NullPointerException if {@code config} is {@code null}.
     * @throws IllegalArgumentException if the configuration property
     *         {@code closeInputStreams} is {@code false} and
     *         {@code closeOutputStreams} is {@code true}.
     * @see ArchiveController#sync(ArchiveSyncExceptionBuilder, BitField)
     */
    public static void sync(
            final URI prefix,
            final ArchiveSyncExceptionBuilder builder,
            BitField<SyncOption> options)
    throws ArchiveSyncException {
        if (!options.get(CLOSE_INPUT_STREAMS) && options.get(CLOSE_OUTPUT_STREAMS))
            throw new IllegalArgumentException();
        options = options.set(REASSEMBLE);

        int total = 0, touched = 0;
        logger.log(Level.FINE, "sync.try", new Object[] { // NOI18N
            prefix,
            options.get(WAIT_FOR_INPUT_STREAMS),
            options.get(CLOSE_INPUT_STREAMS),
            options.get(WAIT_FOR_OUTPUT_STREAMS),
            options.get(CLOSE_OUTPUT_STREAMS),
            options.get(UMOUNT),
        });
        try {
            // Reset statistics if it hasn't happened yet.
            CountingReadOnlyFile.init();
            CountingOutputStream.init();
            try {
                // The general algorithm is to sort the targets in descending order
                // of their pathnames (considering the system's default name
                // separator character) and then walk the array in reverse order to
                // call the sync() method on each respective archive controller.
                // This ensures that an archive file will always be updated
                // before its enclosing archive file.
                for (final ArchiveController controller
                        : getControllers(prefix, REVERSE_CONTROLLERS)) {
                    final ArchiveModel model = controller.getModel();
                        try {
                            model.writeLock().lock();
                            try {
                                if (model.isTouched())
                                    touched++;
                                // Upon return, some new ArchiveWarningException's may
                                // have been generated. We need to remember them for
                                // later throwing.
                                controller.sync(builder, options);
                            } finally {
                                model.writeLock().unlock();
                            }
                        } catch (ArchiveSyncException exception) {
                            // Updating the archive file or wrapping it back into
                            // one of it's enclosing archive files resulted in an
                            // exception for some reason.
                            // We are bullheaded and store the exception chain for
                            // later throwing only and continue updating the rest.
                            builder.warn(exception);
                        }
                    total++;
                }
                builder.check();
            } finally {
                CountingReadOnlyFile.resetOnInit();
                CountingOutputStream.resetOnInit();
            }
        } catch (ArchiveSyncException ex) {
            logger.log(Level.FINE, "sync.catch", ex);// NOI18N
            throw ex;
        }
        logger.log(Level.FINE, "sync.return", // NOI18N
                new Object[] { total, touched });
    }

    static Iterable<ArchiveController> getControllers() {
        return getControllers(null, null);
    }

    static Iterable<ArchiveController> getControllers(
            URI prefix,
            final Comparator<ArchiveController> comparator) {
        if (prefix == null)
            prefix = URI.create(""); // catch all
        final Set<ArchiveController> snapshot;
        synchronized (controllers) {
            snapshot = null != comparator
                    ? new TreeSet(comparator)
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
                final ArchiveController controller = (ArchiveController) value;
                if (controller.getMountPoint().toString().startsWith(prefix.toString()))
                    snapshot.add(controller);
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
    public static ArchiveStatistics getLiveStatistics() {
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
                        ArchiveControllers.sync(
                                null,
                                new DefaultArchiveSyncExceptionBuilder(), BitField.of(CLOSE_INPUT_STREAMS, CLOSE_OUTPUT_STREAMS, UMOUNT));
                    } catch (ArchiveSyncException ouch) {
                        ouch.printStackTrace();
                    }
                }
            }
        }
    } // class ShutdownHook

    /**
     * Copies a source file to a destination file, optionally preserving the
     * source's last modification time.
     * We know that the source and destination both appear to be entries in an
     * archive file.
     *
     * @throws FalsePositiveException If the source or the destination is a
     *         false positive and the exception for the destination
     *         cannot get resolved within this method.
     * @throws InputException If copying the data fails because of an
     *         IOException in the source.
     * @throws IOException If copying the data fails because of an
     *         IOException in the destination.
     */
    public static <SE extends ArchiveEntry, DE extends ArchiveEntry>
    void copy(
            final boolean preserve,
            final boolean createParents,
            final ArchiveController srcController,
            final String srcPath,
            final ArchiveController dstController,
            final String dstPath)
    throws FalsePositiveException, IOException {
        // Do not assume anything about the lock status of the controller:
        // This method may be called from a subclass while a lock is acquired!
        //assert !srcController.readLock().isLocked();
        //assert !srcController.writeLock().isLocked();
        //assert !dstController.readLock().isLocked();
        //assert !dstController.writeLock().isLocked();

        try {
            final BitField<IOOption> options = BitField.noneOf(IOOption.class)
                    .set(PRESERVE, preserve)
                    .set(CREATE_PARENTS, createParents);
            final CommonInputSocket<?> input
                    = srcController.getInputSocket(srcPath);
            final CommonOutputSocket<?> output
                    = dstController.getOutputSocket(dstPath, options);
            IOSocket.copy(input, output);
        } catch (ArchiveEntryFalsePositiveException ex) {
            // Both the source and/or the destination may be false positives,
            // so we need to use the exception's additional information to
            // find out which controller actually detected the false positive.
            final URI enclMountPoint = ex.getMountPoint();
            if (!dstController.getMountPoint().toString().startsWith(ex.getCanonicalPath()))
                throw ex; // not my job - pass on!
            final ArchiveController enclController = getController(enclMountPoint);
            final String enclPath = enclMountPoint.relativize(
                    enclMountPoint
                    .resolve(ex.getPath() + SEPARATOR_CHAR)
                    .resolve(dstPath)).toString();
            // Reroute call to the destination's enclosing archive controller.
            copy(   preserve, createParents,
                    srcController, srcPath,
                    enclController, enclPath);
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
    public static void copy(
            final boolean preserve,
            final boolean createParents,
            final File src,
            final InputStream in,
            final ArchiveController dstController,
            final String dstPath)
    throws FalsePositiveException, IOException {
        // Do not assume anything about the lock status of the controller:
        // This method may be called from a subclass while a lock is acquired!
        //assert !dstController.readLock().isLocked();
        //assert !dstController.writeLock().isLocked();

        try {
            final CommonInputSocket<?> input = FileIOProvider
                    .get()
                    .newInputSocket(new FileEntry(src));
            final OutputStream out = dstController
                    .getOutputSocket(
                        dstPath, BitField.noneOf(IOOption.class).set(PRESERVE, preserve).set(CREATE_PARENTS, createParents))
                    .connect(input)
                    .newOutputStream();
            try {
                Streams.cat(in, out);
            } finally {
                out.close();
            }
        } catch (ArchiveEntryFalsePositiveException ex) {
            final URI enclMountPoint = ex.getMountPoint();
            final ArchiveController enclController = getController(enclMountPoint);
            final String enclPath = enclMountPoint.relativize(
                    enclMountPoint
                    .resolve(ex.getPath() + SEPARATOR_CHAR)
                    .resolve(dstPath)).toString();
            // Reroute call to the destination's enclosing ArchiveController.
            copy(   preserve, createParents,
                    src, in,
                    enclController, enclPath);
        }
    }
}
