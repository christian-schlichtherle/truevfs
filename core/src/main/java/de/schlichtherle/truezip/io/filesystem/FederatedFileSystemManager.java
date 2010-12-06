/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
import java.lang.reflect.UndeclaredThrowableException;
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
import static de.schlichtherle.truezip.util.ClassLoaders.loadClass;
import static de.schlichtherle.truezip.util.Link.Type.STRONG;
import static de.schlichtherle.truezip.util.Link.Type.WEAK;

/**
 * Manages federated file systems.
 * <p>
 * Note that this class is thread-safe.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FederatedFileSystemManager {

    private static final Comparator<FileSystemController<?>> REVERSE_CONTROLLERS
            = new Comparator<FileSystemController<?>>() {
        @Override
		public int compare( FileSystemController<?> l,
                            FileSystemController<?> r) {
            return r.getModel().getMountPoint().compareTo(l.getModel().getMountPoint());
        }
    };

    private static volatile FederatedFileSystemManager instance; // volatile required for DCL in JSE 5!

    /**
     * Returns the non-{@code null} federated file system manager class
     * property instance.
     * <p>
     * If the class property has been explicitly set using
     * {@link #setInstance}, then this instance is returned.
     * <p>
     * Otherwise, the value of the system property
     * {@code de.schlichtherle.truezip.io.filesystem.FederatedFileSystemManager}
     * is considered:
     * <p>
     * If this system property is set, it must denote the fully qualified
     * class name of a subclass of this class. The class is loaded and
     * instantiated using its public, no-arguments constructor.
     * <p>
     * Otherwise, this class is instantiated.
     * <p>
     * In order to support this plug-in architecture, you should <em>not</em>
     * cache the instance returned by this method!
     *
     * @throws ClassCastException If the class name in the system property
     *         does not denote a subclass of this class.
     * @throws UndeclaredThrowableException If any other precondition on the
     *         value of the system property does not hold.
     * @return The non-{@code null} federated file system manager class
     *         property instance.
     */
    public static FederatedFileSystemManager getInstance() {
        FederatedFileSystemManager manager = instance;
        if (null == manager) {
            synchronized (FederatedFileSystemManager.class) { // DCL does work in combination with volatile in JSE 5!
                manager = instance;
                if (null == manager) {
                    final String n = System.getProperty(
                            FederatedFileSystemManager.class.getName(),
                            FederatedFileSystemManager.class.getName());
                    try {
                        Class<?> c = loadClass(n, FederatedFileSystemManager.class);
                        instance = manager = (FederatedFileSystemManager) c.newInstance();
                    } catch (RuntimeException ex) {
                        throw ex;
                    } catch (Exception ex) {
                        throw new UndeclaredThrowableException(ex);
                    }
                }
            }
        }
        return manager;
    }

    /**
     * Sets the federated file system manager class property instance.
     * If the current federated file system manager has any managed federated
     * file systems, an {@link IllegalStateException} is thrown.
     * Call {@link #sync} and make sure to purge all references to the
     * federated file system controllers which have been returned by
     * {@link #getController} to prevent this.
     *
     * @param  manager The file system manager instance to use as the class
     *         property.
     *         If this is {@code null}, a new instance will be created on the
     *         next call to {@link #getInstance}.
     * @throws IllegalStateException if the current file system manager has any
     *         managed file systems.
     */
    public static synchronized void setInstance(final FederatedFileSystemManager manager) {
        final int count = instance.schedulers.size();
        if (0 < count)
            throw new IllegalStateException("There are " + count + " managed federated file systems!");
        if (null == manager)
            throw new NullPointerException();
        instance = manager;
    }

    /**
     * The map of all schedulers for composite file system controllers,
     * keyed by the mount point of their respective file system model.
     * All access to this map must be externally synchronized!
     */
    private final Map<URI, Link<Scheduler>> schedulers
            = new WeakHashMap<URI, Link<Scheduler>>();

    /**
     * Equivalent to
     * {@link #getController(FileSystemDriver, URI, FederatedFileSystemController) getController(driver, mountPoint, null)}.
     */
    public final <FSM extends FileSystemModel>
    FederatedFileSystemController<?> getController(
            FileSystemDriver<FSM> driver,
            URI mountPoint) {
        return getController(driver, mountPoint, null);
    }

    /**
     * Returns a federated file system controller for the given mount point.
     * The returned file system controller will use the given parent federated
     * file system controller to mount its file system when required.
     * Mind that the given mount point gets normalized and is not necessarily
     * {@link Object#equals equal} to the mount point of the file system model
     * of the returned federated file system controller.
     *
     * @param  driver the non-{@code null} file system driver which will be
     *         used to create a file system model and optionally a file system
     *         controller.
     * @param  mountPoint the non-{@code null}
     *         {@link FileSystemModel#getMountPoint() mount point}
     *         of the federated file system.
     * @param  parent the nullable controller for the parent federated file
     *         system.
     * @return A non-{@code null} federated file system controller.
     */
    public <M extends FileSystemModel>
    FederatedFileSystemController<?> getController(
            final FileSystemDriver<M> driver,
            URI mountPoint,
            FederatedFileSystemController<?> parent) {
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
                parent = getController(
                        driver, new URI(ssp.substring(0, split + 1)), null);
            } catch (URISyntaxException ex) {
                throw new IllegalArgumentException(ex);
            }
        }
        final M model = driver.newModel(mountPoint,
                null == parent ? null : parent.getModel());
        mountPoint = model.getMountPoint(); // mind URI normalization!
        FederatedFileSystemController<?> controller;
        synchronized (schedulers) {
            Scheduler scheduler = Links.getTarget(schedulers.get(mountPoint));
            if (null != scheduler) {
                controller = scheduler.controller;
            } else {
                final FileSystemController<?> c
                        = driver.newController(model, parent);
                if (null == c.getParent()) {
                    controller = (FederatedFileSystemController<?>) c;
                } else {
                    scheduler = new Scheduler(c);
                    controller = scheduler.controller;
                    model.addFileSystemListener(scheduler);
                }
                assert model == controller.getModel();
            }
        }
        assert (null != controller.getModel().getParent())
                == (null != controller.getParent());
        return controller;
    }

    private final class Scheduler implements FileSystemListener {

        final CompositeFileSystemController controller;

        Scheduler(final FileSystemController<?> prospect) {
            controller = new CompositeFileSystemController(prospect);
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
    } // class Scheduler

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
    public <E extends IOException>
    void sync(  final URI prefix,
                final ExceptionBuilder<? super IOException, E> builder,
                BitField<SyncOption> options)
    throws E {
        if (options.get(FORCE_CLOSE_OUTPUT) && !options.get(FORCE_CLOSE_INPUT)
                || options.get(ABORT_CHANGES))
            throw new IllegalArgumentException();
        // The general algorithm is to sort the mount points in descending
        // order of their pathnames and then traverse the array in reverse
        // order to call the sync() method on each respective archive
        // controller.
        // This ensures that an archive file system will always be synced
        // before its parent archive file system.
        for (final FederatedFileSystemController<?> controller
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
    }

    final Set<FederatedFileSystemController<?>> getControllers(
            URI prefix,
            final Comparator<? super FederatedFileSystemController<?>> comparator) {
        if (null == prefix)
            prefix = URI.create(""); // catch all
        else
            prefix = URI.create(prefix.toString() + SEPARATOR_CHAR).normalize();
        final Set<FederatedFileSystemController<?>> snapshot;
        synchronized (schedulers) {
            snapshot = null != comparator
                    ? new TreeSet<FederatedFileSystemController<?>>(comparator)
                    : new HashSet<FederatedFileSystemController<?>>((int) (schedulers.size() / .75f) + 1);
            for (final Link<Scheduler> link : schedulers.values()) {
                final Scheduler scheduler = Links.getTarget(link);
                final CompositeFileSystemController controller
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
     * Adds the given {@code runnable} to the set of runnables to run by a
     * shutdown hook.
     * This is typically used to delete archive files or entries.
     */
    public void addShutdownHook(final Runnable runnable) {
        getShutdownHook().add(runnable);
    }

    private ShutdownThread shutdownThread; // lazily initialized

    private synchronized ShutdownThread getShutdownHook() {
        if (null == shutdownThread) {
            shutdownThread = new ShutdownThread(new ShutdownRunnable());
            Runtime.getRuntime().addShutdownHook(shutdownThread);
        }
        return shutdownThread;
    }

    /**
     * This shutdown thread class runs the runnable provided to its constructor
     * when it starts execution.
     *
     * @see FederatedFileSystemManager#addShutdownHook(java.lang.Runnable)
     */
    private static final class ShutdownThread extends Thread {

        final ShutdownRunnable runnables;

        ShutdownThread(final ShutdownRunnable runnables) {
            super("TrueZIP FileSystemProvider Shutdown Hook");
            super.setPriority(Thread.MAX_PRIORITY);
            this.runnables = runnables;
        }

        /**
         * Adds the given {@code runnable} to the set of runnables to run
         * when this thread starts execution.
         */
        void add(final Runnable runnable) {
            runnables.add(runnable);
        }

        @Override
        public void run() {
            runnables.run();
        }
    } // class ShutdownThread

    /**
     * This singleton shutdown hook runnable class runs a set of user-provided
     * runnables which may perform cleanup tasks when it's {@link #run()}
     * method is invoked.
     * This is typically used to delete archive files or entries.
     */
    private final class ShutdownRunnable implements Runnable {

        final Collection<Runnable> runnables = new HashSet<Runnable>();

        ShutdownRunnable() {
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
                    //FederatedFileSystemManager.logger.setLevel(Level.OFF);
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
    } // class ShutdownRunnable
}
