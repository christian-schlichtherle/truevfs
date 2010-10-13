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

import de.schlichtherle.truezip.util.ExceptionBuilder;
import de.schlichtherle.truezip.io.file.ArchiveWarningException;
import java.io.IOException;
import de.schlichtherle.truezip.io.filesystem.FileSystemController;
import de.schlichtherle.truezip.util.Link;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.key.PromptingKeyManager;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.Links;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;

import static de.schlichtherle.truezip.io.archive.controller.SyncOption.ABORT_CHANGES;
import static de.schlichtherle.truezip.io.archive.controller.SyncOption.FORCE_CLOSE_INPUT;
import static de.schlichtherle.truezip.io.archive.controller.SyncOption.FORCE_CLOSE_OUTPUT;
import static de.schlichtherle.truezip.io.archive.controller.SyncOption.REASSEMBLE_BUFFERS;
import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.SEPARATOR;
import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.SEPARATOR_CHAR;
import static de.schlichtherle.truezip.util.Link.Type.STRONG;
import static de.schlichtherle.truezip.util.Link.Type.WEAK;

/**
 * Provides static utility methods for {@link FileSystemController}s.
 * This class cannot get instantiated outside its package.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class Controllers {

    private static final Comparator<ArchiveController> REVERSE_CONTROLLERS
            = new Comparator<ArchiveController>() {
        @Override
		public int compare(ArchiveController l, ArchiveController r) {
            return  r.getModel().getMountPoint().compareTo(l.getModel().getMountPoint());
        }
    };

    /**
     * The map of all archive controllers.
     * The keys are plain {@link URI} instances and the values are either
     * {@code FileSystemController}s or {@link WeakReference}s to
     * {@code FileSystemController}s.
     * All access to this map must be externally synchronized!
     */
    private static final Map<URI, Link<ArchiveController>> controllers
            = new WeakHashMap<URI, Link<ArchiveController>>();

    private Controllers() {
    }

    public static FileSystemController getController(URI mountPoint) {
        return getController(mountPoint, null, null);
    }

    /**
     * Factory method returning a {@link FileSystemController} object for the
     * given mount point.
     * <p>
     * <b>Note:</b>
     * <ul>
     * <li>Neither {@code file} nor the enclosing archive file(s)
     *     need to actually exist for this to return a valid {@code FileSystemController}.
     *     Just the parent directories of {@code file} need to look like either
     *     an ordinary directory or an archive file, e.g. their lowercase
     *     representation needs to have a .zip or .jar ending.</li>
     * <li>It is an error to call this method on a target file which is
     *     not a valid name for an archive file</li>
     * </ul>
     */
    public static <AE extends ArchiveEntry> FileSystemController getController(
            URI mountPoint,
            final ArchiveDriver<AE> driver,
            FileSystemController enclController) {
        if (!mountPoint.isAbsolute()) throw new IllegalArgumentException();
        if (mountPoint.isOpaque()) throw new IllegalArgumentException();
        //if (!mountPoint.equals(mountPoint.normalize())) throw new IllegalArgumentException();
        mountPoint = URI.create(mountPoint.toString() + SEPARATOR_CHAR).normalize();
        assert mountPoint.getPath().endsWith(SEPARATOR);
        synchronized (controllers) {
            final ArchiveController controller
                    = Links.getTarget(controllers.get(mountPoint));
            if (null != controller) {
                // If required, reconfiguration of the FileSystemController
                // must be deferred until we have released the lock on
                // controllers in order to prevent dead locks.
                //reconfigure = driver != null && driver != controller.getDriver();
                return controller;
            }
            if (null == driver) // pure lookup operation?
                return null;
            if (null == enclController) {
                enclController = new OSFileSystemController(
                        mountPoint.resolve(".."));
            }
            final SyncScheduler<AE> syncScheduler = new SyncScheduler<AE>();
            final ArchiveModel model = new ArchiveModel(
                    enclController.getModel(), mountPoint, syncScheduler);
            // TODO: Support append strategy.
            syncScheduler.controller
                    = new ProspectiveArchiveController(
                        enclController,
                        new BufferingArchiveController(
                            new LockingArchiveController(
                                new UpdatingArchiveController<AE>(
                                    enclController, model, driver))));
            syncScheduler.setTouched(false);
            return syncScheduler.controller;
        }
    }

    private static class SyncScheduler<AE extends ArchiveEntry>
    implements TouchListener {
        ArchiveController controller;

        @Override
        public void setTouched(boolean touched) {
            if (null != controller)
                scheduleSync(touched ? STRONG : WEAK, controller);
        }
    }

    /**
     * Schedules the given archive controller for synchronization according to
     * the given TypedLink Type.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	static void scheduleSync(   final Link.Type type,
                                final ArchiveController controller) {
        synchronized (controllers) {
            controllers.put(controller.getModel().getMountPoint(),
                            type.newLink(controller));
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
     *         {@link DefaultSyncExceptionBuilder} and <em>only</em>
     *         warning conditions occured throughout the course of this method.
     *         This implies that the respective archive file has been updated
     *         with constraints, such as a failure to set the last modification
     *         time of the archive file to the last modification time of its
     *         implicit root directory.
     * @throws ArchiveWarningException if the configuration uses the
     *         {@link DefaultSyncExceptionBuilder} and any error
     *         condition occured throughout the course of this method.
     *         This implies loss of data!
     * @throws NullPointerException if {@code config} is {@code null}.
     * @throws IllegalArgumentException if the configuration property
     *         {@code closeInputStreams} is {@code false} and
     *         {@code closeOutputStreams} is {@code true}.
     * @see    ArchiveController#sync(ExceptionBuilder, BitField)
     */
    public static <E extends IOException>
    void sync(  final URI prefix,
                final ExceptionBuilder<? super IOException, E> builder,
                BitField<SyncOption> options)
    throws E {
        if (options.get(FORCE_CLOSE_OUTPUT) && !options.get(FORCE_CLOSE_INPUT)
                || options.get(ABORT_CHANGES))
            throw new IllegalArgumentException();
        options = options.set(REASSEMBLE_BUFFERS);

        int total = 0, touched = 0;
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
                try {
                    if (controller.isTouched())
                        touched++;
                    // Upon return, some new ArchiveWarningException's may
                    // have been generated. We need to remember them for
                    // later throwing.
                    controller.sync(builder, options);
                } catch (IOException ex) {
                    // Updating the archive file or wrapping it back into
                    // one of it's enclosing archive files resulted in an
                    // I/O exception for some reason.
                    // We are bullheaded and store the exception for later
                    // throwing and continue updating the rest.
                    builder.warn(ex);
                }
                total++;
            }
            builder.check();
        } finally {
            CountingReadOnlyFile.resetOnInit();
            CountingOutputStream.resetOnInit();
        }
    }

    static Set<ArchiveController> getControllers() {
        return getControllers(null, null);
    }

    static Set<ArchiveController> getControllers(
            URI prefix,
            final Comparator<ArchiveController> comparator) {
        if (null == prefix)
            prefix = URI.create(""); // catch all
        final Set<ArchiveController> snapshot;
        synchronized (controllers) {
            snapshot = null != comparator
                    ? new TreeSet<ArchiveController>(comparator)
                    : new HashSet<ArchiveController>((int) (controllers.size() / .75f) + 1);
            for (final Link<ArchiveController> link : controllers.values()) {
                final ArchiveController controller = Links.getTarget(link);
                if (null != controller && controller
                        .getModel()
                        .getMountPoint()
                        .getPath()
                        .startsWith(prefix.toString()))
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
    public static ArchiveStatistics getStatistics() {
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
        @Override
        @SuppressWarnings({"NestedSynchronizedStatement", "CallToThreadDumpStack"})
        public void run() {
            synchronized (PromptingKeyManager.class) {
                try {
                    // paranoid, but safe.
                    PromptingKeyManager.setPrompting(false);
                    //Controllers.logger.setLevel(Level.OFF);
                    synchronized (this) {
                        for (Runnable runnable : runnables)
                            runnable.run();
                    }
                } finally {
                    try {
                        sync(   null, new DefaultSyncExceptionBuilder(),
                                BitField.of(FORCE_CLOSE_INPUT, FORCE_CLOSE_OUTPUT));
                    } catch (IOException ouch) {
                        ouch.printStackTrace();
                    }
                }
            }
        }
    } // class ShutdownHook
}
