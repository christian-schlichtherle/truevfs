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
package de.schlichtherle.truezip.io.filesystem;

import de.schlichtherle.truezip.key.PromptingKeyManager;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import de.schlichtherle.truezip.util.Link;
import de.schlichtherle.truezip.util.Links;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;

import static de.schlichtherle.truezip.io.filesystem.FileSystemModel.BANG_SEPARATOR;
import static de.schlichtherle.truezip.io.filesystem.SyncOption.ABORT_CHANGES;
import static de.schlichtherle.truezip.io.filesystem.SyncOption.FORCE_CLOSE_INPUT;
import static de.schlichtherle.truezip.io.filesystem.SyncOption.FORCE_CLOSE_OUTPUT;
import static de.schlichtherle.truezip.io.entry.Entry.SEPARATOR_CHAR;
import static de.schlichtherle.truezip.util.Link.Type.STRONG;
import static de.schlichtherle.truezip.util.Link.Type.WEAK;

/**
 * Provides static utility methods for {@link ComponentFileSystemController}s.
 * This class cannot get instantiated.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FileSystems {

    private static final Comparator<FileSystemController<?>> REVERSE_CONTROLLERS
            = new Comparator<FileSystemController<?>>() {
        @Override
		public int compare(FileSystemController<?> l, FileSystemController<?> r) {
            return  r.getModel().getMountPoint().compareTo(l.getModel().getMountPoint());
        }
    };

    /**
     * The map of all schedulers for composite file system controllers,
     * keyed by the mount point of their respective file system model.
     * All access to this map must be externally synchronized!
     */
    private static final Map<URI, Link<Scheduler>> schedulers
            = new WeakHashMap<URI, Link<Scheduler>>();

    private FileSystems() {
    }

    /**
     * Equivalent to
     * {@link #getController(FileSystemDriver, URI, ComponentFileSystemController) getController(driver, mountPoint, null)}.
     */
    public static <FSM extends FileSystemModel>
    ComponentFileSystemController<?> getController(
            FileSystemDriver<FSM> driver,
            URI mountPoint) {
        return getController(driver, mountPoint, null);
    }

    /**
     * Returns a file system controller for the given mount point.
     * The returned file system controller will use the given parent file
     * system controller to mount its file system when required.
     * Mind that the mount point gets normalized and may or may not be
     * identical to the mount point of the file system model of the returned
     * file system controller.
     *
     * @param  driver the non-{@code null} file system driver which will be
     *         used to create the file system model and the file system
     *         controller.
     * @param  mountPoint the non-{@code null}
     *         {@link FileSystemModel#getMountPoint() mount point}
     *         of the (federated) file system.
     * @param  parent the nullable file system controller for the parent file
     *         system.
     * @return A non-{@code null} component file system controller.
     */
    public static <FSM extends FileSystemModel>
    ComponentFileSystemController<?> getController(
            final FileSystemDriver<FSM> driver,
            URI mountPoint,
            ComponentFileSystemController<?> parent) {
        if (null == parent && mountPoint.isOpaque()) {
            try {
                String ssp = mountPoint.getSchemeSpecificPart();
                if (!ssp.endsWith(BANG_SEPARATOR))
                    throw new URISyntaxException(   mountPoint.toString(),
                                                    "Doesn't end with the bang separator \""
                                                    + BANG_SEPARATOR + '"');
                final int split = ssp.lastIndexOf(SEPARATOR_CHAR, ssp.length() - 2);
                if (0 > split)
                    throw new URISyntaxException(   mountPoint.toString(),
                                                    "Missing separator '"
                                                    + SEPARATOR_CHAR + "'");
                parent = getController(driver,new URI(ssp.substring(0, split + 1)), null);
            } catch (URISyntaxException ex) {
                throw new IllegalArgumentException(ex);
            }
        }
        final FSM model = driver.newModel(mountPoint,
                null == parent ? null : parent.getModel());
        mountPoint = model.getMountPoint(); // mind URI normalization!
        Scheduler scheduler;
        synchronized (schedulers) {
            scheduler = Links.getTarget(schedulers.get(mountPoint));
            if (null == scheduler) {
                final FileSystemController<?> prospect
                        = driver.newController(model, parent);
                if (null == prospect.getParent())
                    return (ComponentFileSystemController<?>) prospect;
                scheduler = new Scheduler(prospect);
            }
        }
        return scheduler.controller;
    }

    private static final class Scheduler implements FileSystemListener {

        final CompositeFileSystemController controller;

        @SuppressWarnings("LeakingThisInConstructor")
        Scheduler(final FileSystemController<?> prospect) {
            if (prospect instanceof CompositeFileSystemController)
                controller = (CompositeFileSystemController) prospect;
            else
                controller = new CompositeFileSystemController(prospect);
            controller.getModel().addFileSystemListener(this);
            touchChanged(null); // setup schedule
        }

        /**
         * Schedules the file system controller for synchronization according
         * to the given touch status.
         */
        @Override
        public void touchChanged(final FileSystemEvent event) {
            synchronized (schedulers) {
                final FileSystemModel model = controller.getModel();
                assert null == event || event.getSource() == model;
                schedulers.put(model.getMountPoint(),
                        (model.isTouched() ? STRONG : WEAK).newLink(this));
            }
        }
    }

    /**
     * Writes all changes to the contents of the file systems who's canonical
     * path name (mount point) starts with the given {@code prefix} to their
     * parent file system.
     * This will reset the state of the respective file system controllers.
     * This method is thread-safe.
     *
     * @param  prefix the prefix of the canonical path name of the file systems
     *         which shall get synchronized with their parent file system.
     *         This may be {@code null} or empty in order to select all
     *         accessed files systems.
     * @throws SyncWarningException if the configuration uses the
     *         {@link SyncExceptionBuilder} and <em>only</em>
     *         warning conditions occured throughout the course of this method.
     *         This implies that the respective file system has been
     *         synchronized with constraints, e.g. a failure to set the last
     *         modification time of the parent file system entry to the last
     *         modification time of the (virtual) root directory of its file
     *         system.
     * @throws SyncException if the configuration uses the
     *         {@link SyncExceptionBuilder} and any error
     *         condition occured throughout the course of this method.
     *         This implies loss of data!
     * @throws NullPointerException if {@code builder} or {@code options} is
     *         {@code null}.
     * @throws IllegalArgumentException if the configuration property
     *         {@code FORCE_CLOSE_INPUT} is {@code false} and
     *         {@code FORCE_CLOSE_OUTPUT} is {@code true}.
     */
    public static <E extends IOException>
    void sync(  final URI prefix,
                final ExceptionBuilder<? super IOException, E> builder,
                BitField<SyncOption> options)
    throws E {
        if (options.get(FORCE_CLOSE_OUTPUT) && !options.get(FORCE_CLOSE_INPUT)
                || options.get(ABORT_CHANGES))
            throw new IllegalArgumentException();

        // Reset statistics if it hasn't happened yet.
        CountingReadOnlyFile.init();
        CountingOutputStream.init();
        try {
            // The general algorithm is to sort the mount points in descending
            // order of their pathnames and then traverse the array in reverse
            // order to call the sync() method on each respective archive
            // controller.
            // This ensures that an archive file system will always be synced
            // before its parent archive file system.
            for (final ComponentFileSystemController<?> controller
                    : getControllers(prefix, REVERSE_CONTROLLERS)) {
                try {
                    // Upon return, some new ArchiveWarningException's may
                    // have been generated. We need to remember them for
                    // later throwing.
                    controller.sync(builder, options);
                } catch (IOException ex) {
                    // Updating the archive file or wrapping it back into
                    // one of it's parent archive files resulted in an
                    // I/O exception for some reason.
                    // We are bullheaded and store the exception for later
                    // throwing and continue updating the rest.
                    builder.warn(ex);
                }
            }
            builder.check();
        } finally {
            CountingReadOnlyFile.resetOnInit();
            CountingOutputStream.resetOnInit();
        }
    }

    static Set<ComponentFileSystemController<?>> getControllers() {
        return getControllers(null, null);
    }

    static Set<ComponentFileSystemController<?>> getControllers(
            URI prefix,
            final Comparator<FileSystemController<?>> comparator) {
        if (null == prefix)
            prefix = URI.create(""); // catch all
        else
            prefix = URI.create(prefix.toString() + SEPARATOR_CHAR).normalize();
        final Set<ComponentFileSystemController<?>> snapshot;
        synchronized (schedulers) {
            snapshot = null != comparator
                    ? new TreeSet<ComponentFileSystemController<?>>(comparator)
                    : new HashSet<ComponentFileSystemController<?>>((int) (schedulers.size() / .75f) + 1);
            for (final Link<Scheduler> link : schedulers.values()) {
                final Scheduler scheduler = Links.getTarget(link);
                final ComponentFileSystemController<?> controller
                        = null == scheduler ? null : scheduler.controller;
                if (null != controller && controller
                        .getModel()
                        .getMountPoint()
                        .getPath()
                        .startsWith(prefix.getPath()))
                    snapshot.add(controller);
            }
        }
        return snapshot;
    }

    /**
     * Returns a proxy instance which encapsulates <em>live</em> statistics
     * about the total set of archive files accessed by this package.
     * Any call to a method of the returned interface instance returns
     * up-to-date data, so there is <em>no</em> need to repeatedly call this
     * method in order to update the statistics.
     * <p>
     * Note that this method returns <em>live</em> statistics rather than
     * <em>real time</em> statistics.
     * So there may be a slight delay until the values returned reflect
     * the actual state of this package.
     * This delay increases if the system is under heavy load.
     */
    public static FileSystemStatistics getStatistics() {
        return FileSystemStatistics.SINGLETON;
    }

    /**
     * Adds the given {@code runnable} to the set of runnables to run by a
     * shutdown hook.
     * This is typically used to delete archive files or entries.
     */
    public static void addToShutdownHook(final Runnable runnable) {
        //ShutdownRunnables.SINGLETON.add(runnable);
        ShutdownHook.SINGLETON.add(runnable);
    }

    static {
        Runtime.getRuntime().addShutdownHook(ShutdownHook.SINGLETON);
    }

    /**
     * This singleton shutdown hook thread class runs
     * {@link FileSystems.ShutdownRunnables#SINGLETON} when the JVM terminates.
     * You cannot instantiate this class.
     *
     * @see FileSystems#addToShutdownHook(java.lang.Runnable)
     */
    private static final class ShutdownHook extends Thread {

        /** The singleton instance of this class. */
        static final ShutdownHook SINGLETON = new ShutdownHook();

        ShutdownHook() {
            super(  FileSystems.ShutdownRunnables.SINGLETON,
                    "TrueZIP ArchiveController Shutdown Hook");
            setPriority(Thread.MAX_PRIORITY);
        }

        /**
         * Adds the given {@code runnable} to the set of runnables to run by
         * {@link FileSystems.ShutdownRunnables#SINGLETON} when the JVM
         * terminates.
         */
        void add(final Runnable runnable) {
            FileSystems.ShutdownRunnables.SINGLETON.add(runnable);
        }
    } // class JVMShutdownHook

    /**
     * This singleton shutdown hook runnable class runs a set of user-provided
     * runnables which may perform cleanup tasks when it's {@link #run()}
     * method is invoked.
     * This is typically used to delete archive files or entries.
     */
    private static final class ShutdownRunnables implements Runnable {

        /** The singleton instance of this class. */
        static final ShutdownRunnables SINGLETON = new ShutdownRunnables();

        final Collection<Runnable> runnables = new HashSet<Runnable>();

        ShutdownRunnables() {
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
        synchronized void add(final Runnable runnable) {
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
        @SuppressWarnings("NestedSynchronizedStatement")
        public synchronized void run() {
            synchronized (PromptingKeyManager.class) {
                try {
                    // paranoid, but safe.
                    PromptingKeyManager.setPrompting(false);
                    // Logging doesn't work in a shutdown hook!
                    //FileSystems.logger.setLevel(Level.OFF);
                    for (Runnable runnable : runnables)
                        runnable.run();
                } finally {
                    try {
                        sync(   null,
                                new SyncExceptionBuilder(),
                                BitField.of(    FORCE_CLOSE_INPUT,
                                                FORCE_CLOSE_OUTPUT));
                    } catch (IOException ouch) {
                        // Logging doesn't work in a shutdown hook!
                        ouch.printStackTrace();
                    }
                }
            }
        }
    } // class ShutdownRunnables
}
