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

import de.schlichtherle.truezip.io.entry.CommonEntry;
import de.schlichtherle.truezip.key.PromptingKeyManager;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import de.schlichtherle.truezip.util.Link;
import de.schlichtherle.truezip.util.Links;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;

import static de.schlichtherle.truezip.io.filesystem.SyncOption.ABORT_CHANGES;
import static de.schlichtherle.truezip.io.filesystem.SyncOption.FORCE_CLOSE_INPUT;
import static de.schlichtherle.truezip.io.filesystem.SyncOption.FORCE_CLOSE_OUTPUT;
import static de.schlichtherle.truezip.io.filesystem.SyncOption.FLUSH_CACHE;
import static de.schlichtherle.truezip.io.entry.CommonEntry.SEPARATOR;
import static de.schlichtherle.truezip.io.entry.CommonEntry.SEPARATOR_CHAR;
import static de.schlichtherle.truezip.util.Link.Type.STRONG;
import static de.schlichtherle.truezip.util.Link.Type.WEAK;

/**
 * Provides static utility methods for {@link ComponentFileSystemController}s.
 * This class cannot get instantiated outside its package.
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
     * The map of all archive controllers.
     * The keys are plain {@link URI} instances and the values are either
     * {@code ComponentFileSystemController}s or {@link WeakReference}s to
     * {@code ComponentFileSystemController}s.
     * All access to this map must be externally synchronized!
     */
    private static final Map<URI, Link<CompositeFileSystemController<?>>> controllers
            = new WeakHashMap<URI, Link<CompositeFileSystemController<?>>>();

    private FileSystems() {
    }

    /**
     * Returns a file system controller for the given mount point.
     * The returned file system controller will use the given parent file
     * system controller to mount its file system.
     *
     * @param  mountPoint the non-{@code null}
     *         {@link FileSystemModel#getMountPoint() mount point}
     *         of the (virtual) file system.
     * @param  parent the nullable file system controller for the parent file
     *         system.
     * @return A non-{@code null} file system controller.
     */
    public static <FSM extends FileSystemModel, CE extends CommonEntry>
    ComponentFileSystemController<?> getController(
            URI mountPoint,
            final FileSystemFactory<FSM, CE> factory,
            ComponentFileSystemController<?> parent) {
        // TODO: Make this method support arbitrary host file systems, e.g. by
        // using a factory from a service registry or similar.
        if (!"file".equals(mountPoint.getScheme()) || !mountPoint.isAbsolute()
                || mountPoint.isOpaque())
            throw new IllegalArgumentException();
        mountPoint = URI.create(mountPoint.toString() + SEPARATOR_CHAR).normalize();
        assert mountPoint.getPath().endsWith(SEPARATOR);
        if (null == factory)
            return new HostFileSystemController(
                    new FileSystemModel(mountPoint, null));
        if (null == parent)
            parent = new HostFileSystemController(
                    new FileSystemModel(mountPoint.resolve(".."), null));
        synchronized (controllers) {
            final ComponentFileSystemController<?> controller
                    = Links.getTarget(controllers.get(mountPoint));
            if (null != controller)
                return controller;
            final FSM model = factory.newModel(mountPoint, parent.getModel());
            final ScheduledFileSystemController<CE> scheduledController
                    = new ScheduledFileSystemController<CE>(
                        factory.newController(model, parent), parent);
            model.addFileSystemListener(scheduledController);
            return scheduledController;
        }
    }

    private static final class ScheduledFileSystemController<CE extends CommonEntry>
    extends CompositeFileSystemController<CE>
    implements FileSystemListener {

        ScheduledFileSystemController(
                final FileSystemController<CE> prospect,
                final ComponentFileSystemController<?> parent) {
            super(prospect, parent);
            touchChanged(new FileSystemEvent(getModel()));
        }

        /**
         * Schedules the file system controller for synchronization according
         * to the given touch status.
         */
        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public void touchChanged(final FileSystemEvent event) {
            synchronized (controllers) {
                final FileSystemModel model = event.getSource();
                assert getModel() == model;
                controllers.put(model.getMountPoint(),
                        (Link) (model.isTouched() ? STRONG : WEAK)
                            .newLink(this));
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
        options = options.set(FLUSH_CACHE);

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
        synchronized (controllers) {
            snapshot = null != comparator
                    ? new TreeSet<ComponentFileSystemController<?>>(comparator)
                    : new HashSet<ComponentFileSystemController<?>>((int) (controllers.size() / .75f) + 1);
            for (final Link<CompositeFileSystemController<?>> link : controllers.values()) {
                final ComponentFileSystemController<?> controller = Links.getTarget(link);
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
                                BitField.of(FORCE_CLOSE_INPUT, FORCE_CLOSE_OUTPUT));
                    } catch (IOException ouch) {
                        // Logging doesn't work in a shutdown hook!
                        ouch.printStackTrace();
                    }
                }
            }
        }
    } // class ShutdownRunnables
}
