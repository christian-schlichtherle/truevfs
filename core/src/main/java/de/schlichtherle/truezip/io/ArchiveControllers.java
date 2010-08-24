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

package de.schlichtherle.truezip.io;

import de.schlichtherle.truezip.io.archive.controller.ArchiveControllerException;
import de.schlichtherle.truezip.io.archive.controller.ArchiveControllerExceptionBuilder;
import de.schlichtherle.truezip.io.archive.controller.DefaultArchiveControllerExceptionBuilder;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.key.PromptingKeyManager;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
    private static final Logger logger = Logger.getLogger(CLASS_NAME, CLASS_NAME);

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

    //
    // Static initializers.
    //

    static {
        Runtime.getRuntime().addShutdownHook(ShutdownHook.SINGLETON);
    }

    /** This class cannot get instantiated. */
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
    static ArchiveController get(final File file) {
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
     * Updates all archive files in the real file system which's canonical
     * path name start with {@code prefix} with the contents of their
     * virtual file system, resets all cached state and deletes all temporary
     * files.
     * This method is thread safe.
     * 
     * @param prefix The prefix of the canonical path name of the archive files
     *        which shall get updated - {@code null} is not allowed!
     *        If the canonical pathname of an archive file does not start with
     *        this string, then it is not updated.
     * @throws ArchiveWarningException If only warning conditions occur
     *         throughout the course of this method which imply that the
     *         respective archive file has been updated with
     *         constraints, such as a failure to set the last modification
     *         time of the archive file to the last modification time of its
     *         virtual root directory.
     * @throws ArchiveControllerException If any error conditions occur throughout the
     *         course of this method which imply loss of data.
     *         This usually means that at least one of the archive files
     *         has been created externally and was corrupted or it cannot
     *         get updated because the file system of the temp file or target
     *         file folder is full.
     * @throws NullPointerException If {@code prefix} is {@code null}.
     * @throws IllegalArgumentException If {@code closeInputStreams} is
     *         {@code false} and {@code closeOutputStreams} is
     *         {@code true}.
     */
    public static void umount(
            final String prefix,
            final boolean waitForInputStreams,
            final boolean closeInputStreams,
            final boolean waitForOutputStreams,
            final boolean closeOutputStreams,
            final boolean umount)
    throws ArchiveControllerException {
        if (prefix == null)
            throw new NullPointerException();
        final DefaultArchiveControllerExceptionBuilder builder
                = new DefaultArchiveControllerExceptionBuilder();
        final UmountConfiguration config = new UmountConfiguration()
                .setArchiveControllerExceptionBuilder(builder)
                .setArchiveControllerExceptionBuilder(new DefaultArchiveControllerExceptionBuilder())
                .setWaitForInputStreams(waitForInputStreams)
                .setCloseInputStreams(closeInputStreams)
                .setWaitForOutputStreams(waitForOutputStreams)
                .setCloseOutputStreams(closeOutputStreams)
                .setRelease(umount)
                .setReassemble(true);
        umount0(prefix, config);
    }

    private static void umount0(
            final String prefix,
            final UmountConfiguration config)
    throws ArchiveControllerException {
        if (prefix == null)
            throw new NullPointerException();
        if (!config.getCloseInputStreams() && config.getCloseOutputStreams())
            throw new IllegalArgumentException();

        int controllersTotal = 0, controllersTouched = 0;
        logger.log(Level.FINE, "update.entering", // NOI18N
                new Object[] {
            prefix,
            Boolean.valueOf(config.getWaitForInputStreams()),
            Boolean.valueOf(config.getCloseInputStreams()),
            Boolean.valueOf(config.getWaitForOutputStreams()),
            Boolean.valueOf(config.getCloseOutputStreams()),
            Boolean.valueOf(config.getRelease()),
        });
        try {
            // Reset statistics if it hasn't happened yet.
            CountingReadOnlyFile.init();
            CountingOutputStream.init();
            try {
                final ArchiveControllerExceptionBuilder builder
                        = config.getArchiveControllerExceptionBuilder();

                // The general algorithm is to sort the targets in descending order
                // of their pathnames (considering the system's default name
                // separator character) and then walk the array in reverse order to
                // call the umount() method on each respective archive controller.
                // This ensures that an archive file will always be updated
                // before its enclosing archive file.
                final Enumeration<ArchiveController> e
                        = new ControllerEnumeration(
                            prefix, REVERSE_CONTROLLERS);
                while (e.hasMoreElements()) {
                    final ArchiveController controller = e.nextElement();
                    controller.writeLock().lock();
                    try {
                        if (controller.isTouched())
                            controllersTouched++;
                        try {
                            // Upon return, some new ArchiveWarningException's may
                            // have been generated. We need to remember them for
                            // later throwing.
                            controller.umount(config);
                        } catch (ArchiveControllerException exception) {
                            // Updating the archive file or wrapping it back into
                            // one of it's enclosing archive files resulted in an
                            // exception for some reason.
                            // We are bullheaded and store the exception chain for
                            // later throwing only and continue updating the rest.
                            builder.reset(exception);
                        }
                    } finally {
                        controller.writeLock().unlock();
                    }
                    controllersTotal++;
                }

                // Check to rethrow exception chain sorted by priority.
                builder.check();
            } finally {
                CountingReadOnlyFile.resetOnInit();
                CountingOutputStream.resetOnInit();
            }
        } catch (ArchiveControllerException chain) {
            logger.log(Level.FINE, "update.throwing", chain);// NOI18N
            throw chain;
        }
        logger.log(Level.FINE, "update.exiting", // NOI18N
                new Object[] {
            new Integer(controllersTotal),
            new Integer(controllersTouched)
        });
    }

    static ArchiveStatistics getLiveArchiveStatistics() {
        return LiveArchiveStatistics.SINGLETON;
    }

    //
    // Static member classes and interfaces.
    //

    /**
     * TrueZIP's singleton shutdown hook for the JVM.
     * This shutdown hook is always run, even if the JVM terminates due to an
     * uncatched Throwable.
     * Only a JVM crash could prevent this, but this is an extremely rare
     * situation.
     */
    static final class ShutdownHook extends Thread {
        /** The singleton instance. */
        private static final ShutdownHook SINGLETON = new ShutdownHook();

        /**
         * The set of files to delete when the shutdown hook is run.
         * When iterating over it, its elements are returned in insertion order.
         */
        static final Set deleteOnExit
                = Collections.synchronizedSet(new LinkedHashSet());

        /** You cannot instantiate this singleton class. */
        private ShutdownHook() {
            super("TrueZIP ArchiveController Shutdown Hook");
            setPriority(Thread.MAX_PRIORITY);
            // Force loading the key manager now in order to prevent class
            // loading in the shutdown hook. This may help with environments
            // (app servers) which disable class loading in shutdown hooks.
            PromptingKeyManager.getInstance();
        }

        /**
         * Deletes all files that have been marked by
         * {@link File#deleteOnExit} and finally unmounts all controllers.
         * <p>
         * Logging and password prompting will be disabled (they wouldn't work
         * in a JVM shutdown hook anyway) in order to provide a deterministic
         * behaviour and in order to avoid RuntimeExceptions or even Errors
         * in the API.
         * <p>
         * Any exceptions thrown throughout the umount will be printed on
         * standard error output.
         * <p>
         * Note that this method is <em>not</em> re-entrant and should not be
         * directly called except for unit testing (you couldn't do a unit test
         * on a shutdown hook otherwise, could you?).
         */
        @Override
        @SuppressWarnings("CallToThreadDumpStack")
        public void run() {
            synchronized (PromptingKeyManager.class) {
                try { // paranoid, but safe.
                    PromptingKeyManager.setPrompting(false);
                    logger.setLevel(Level.OFF);

                    for (Iterator i = deleteOnExit.iterator(); i.hasNext(); ) {
                        final File file = (File) i.next();
                        if (file.exists() && !file.delete()) {
                            System.err.println(
                                    file.getPath() + ": failed to deleteOnExit()!");
                        }
                    }
                } finally {
                    try {
                        umount("", false, true, false, true, true);
                    } catch (ArchiveControllerException ouch) {
                        ouch.printStackTrace();
                    }
                }
            }
        }
    } // class ShutdownHook

;

    private static final class LiveArchiveStatistics
            implements ArchiveStatistics {
        private static final LiveArchiveStatistics SINGLETON
                = new LiveArchiveStatistics();

        /** You cannot instantiate this singleton class. */
        private LiveArchiveStatistics() {
        }

        public long getUpdateTotalByteCountRead() {
            return CountingReadOnlyFile.getTotal();
        }

        public long getUpdateTotalByteCountWritten() {
            return CountingOutputStream.getTotal();
        }

        public int getArchivesTotal() {
            // This is not 100% correct:
            // Controllers which have been removed from the WeakReference
            // VALUE in the map meanwhile, but not yet removed from the map
            // are counted as well.
            // But hey, this is only statistics, right?
            return controllers.size();
        }

        public int getArchivesTouched() {
            int result = 0;

            final Enumeration e = new ControllerEnumeration();
            while (e.hasMoreElements()) {
                final ArchiveController c = (ArchiveController) e.nextElement();
                c.readLock().lock();
                try {
                    if (c.isTouched())
                        result++;
                } finally {
                    c.readLock().unlock();
                }
            }

            return result;
        }

        public int getTopLevelArchivesTotal() {
            int result = 0;

            final Enumeration e = new ControllerEnumeration();
            while (e.hasMoreElements()) {
                final ArchiveController c = (ArchiveController) e.nextElement();
                if (c.getEnclController() == null)
                    result++;
            }

            return result;
        }

        public int getTopLevelArchivesTouched() {
            int result = 0;

            final Enumeration e = new ControllerEnumeration();
            while (e.hasMoreElements()) {
                final ArchiveController c = (ArchiveController) e.nextElement();
                c.readLock().lock();
                try {
                    if (c.getEnclController() == null && c.isTouched())
                        result++;
                } finally {
                    c.readLock().unlock();
                }
            }

            return result;
        }
    } // class LiveStatistics

    private static final class ControllerEnumeration implements Enumeration<ArchiveController> {
        private final Iterator<ArchiveController> it;

        ControllerEnumeration() {
            this("", null);
        }

        ControllerEnumeration(final String prefix, final Comparator c) {
            assert prefix != null;

            final Set<ArchiveController> snapshot;
            synchronized (controllers) {
                if (c != null) {
                    snapshot = new TreeSet(c);
                } else {
                    snapshot = new HashSet((int) (controllers.size() / 0.75f));
                }

                final Iterator it = controllers.values().iterator();
                while (it.hasNext()) {
                    Object value = it.next();
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

            it = snapshot.iterator();
        }

        public boolean hasMoreElements() {
            return it.hasNext();
        }

        public ArchiveController nextElement() {
            return it.next();
        }
    } // class ControllerEnumeration
}
