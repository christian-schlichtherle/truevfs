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
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import net.jcip.annotations.ThreadSafe;

import static de.schlichtherle.truezip.util.ClassLoaders.loadClass;
import static de.schlichtherle.truezip.io.filesystem.SyncOption.*;

/**
 * A static service locator and container for a default file system manager
 * instance.
 * utility methods to access file system managers.
 * If you want to use this package with dependency injection, then you should
 * avoid using this class if possible because it uses static fields for
 * storing stateful objects.
 * <p>
 * This class is thread-safe.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class FileSystemManagers {

    /** You cannot instantiate this class. */
    private FileSystemManagers() {
    }

    private static volatile FileSystemManager instance; // volatile required for DCL in JSE 5!
    private static ShutdownThread shutdownThread; // lazily initialized

    /**
     * Returns the file system manager value of this class property.
     * <p>
     * If the class property has been explicitly set using
     * {@link #setInstance}, then this instance is returned.
     * Otherwise, the value of the system property
     * {@code de.schlichtherle.truezip.io.filesystem.FileSystemManager}
     * is considered:
     * <p>
     * If this system property is set, it must denote the fully qualified
     * class name of a subclass of the class {@link FileSystemManager}.
     * The class is loaded and instantiated using its public, no-arguments
     * constructor.
     * Otherwise, the class {@link FileSystemManager} is instantiated.
     * <p>
     * In any case, the returned file system manager is instrumented to run its
     * {@link FileSystemManager#sync} method when the JVM terminates by means
     * of a shutdown hook.
     * <p>
     * In order to support this plug-in architecture, you should <em>not</em>
     * cache the instance returned by this method!
     *
     * @throws ClassCastException If the class name in the system property
     *         does not denote a subclass of this class.
     * @throws UndeclaredThrowableException If any other precondition on the
     *         value of the system property does not hold.
     * @return The non-{@code null} file system manager value of this class
     *         property.
     */
    @NonNull
    public static FileSystemManager getInstance() {
        FileSystemManager manager = instance;
        if (null == manager) {
            synchronized (FileSystemManager.class) { // DCL does work in combination with volatile in JSE 5!
                manager = instance;
                if (null == manager) {
                    // FIXME: Use ServiceLoader instead!
                    final String name = System.getProperty(
                            FileSystemManager.class.getName(),
                            FileSystemManager.class.getName());
                    try {
                        manager = (FileSystemManager) loadClass(name, FileSystemManager.class)
                                .newInstance();
                    } catch (RuntimeException ex) {
                        throw ex;
                    } catch (Exception ex) {
                        throw new UndeclaredThrowableException(ex);
                    }
                    setInstance(manager);
                }
            }
        }

        assert invariants();

        return manager;
    }

    /**
     * Sets the file system manager value of this class property.
     * <p>
     * If the current file system manager manages any federated file systems,
     * an {@link IllegalStateException} is thrown.
     * To avoid this, call its {@link FileSystemManager#sync} method and make
     * sure to purge all references to the file system controllers which are
     * returned by its {@link FileSystemManager#getController} method prior to
     * calling this method.
     * <p>
     * If not {@code null}, the file system manager instance is instrumented
     * to run its {@link FileSystemManager#sync} method when the JVM terminates
     * by means of a shutdown hook.
     *
     * @param  manager the nullable file system manager value of this class
     *         property.
     *         If this is {@code null}, a new instance will be created on the
     *         next call to {@link #getInstance}.
     * @throws IllegalStateException if the current file system manager has any
     *         managed file systems.
     */
    public static synchronized void setInstance(
            @Nullable final FileSystemManager manager) {
        final int count = null == instance
                ? 0
                : instance.getControllers().size();
        if (0 < count)
            throw new IllegalStateException("There are still " + count + " managed federated file systems!");
        if (manager != instance) {
            if (null != instance) {
                assert null != shutdownThread;
                Runtime.getRuntime().removeShutdownHook(shutdownThread);
                shutdownThread = null;
            }
            if (null != manager) {
                shutdownThread = new ShutdownThread(manager);
                Runtime.getRuntime().addShutdownHook(shutdownThread);
            }
        }
        instance = manager;

        assert invariants();
    }

    private static boolean invariants() {
        return (null != instance) == (null != shutdownThread);
    }

    private static final class ShutdownThread extends Thread {
        ShutdownThread(final FileSystemManager manager) {
            super(  new ShutdownRunnable(manager),
                    "TrueZIP FileSystemManager Shutdown Hook");
            super.setPriority(Thread.MAX_PRIORITY);
        }
    } // class ShutdownThread

    private static final class ShutdownRunnable implements Runnable {

        final FileSystemManager manager;

        ShutdownRunnable(final FileSystemManager manager) {
            assert null != manager;
            this.manager = manager;
            // Force loading the key manager now in order to prevent class
            // loading when running the shutdown hook.
            // This may help if this shutdown hook is run as a JVM shutdown
            // hook in an app server environment where class loading is
            // disabled.
            // FIXME: This adds a dependency on the key manager,
            // which is not acceptible!
            PromptingKeyManager.getInstance();
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
        @SuppressWarnings("CallToThreadDumpStack")
        public void run() {
            try {
                // Paranoid, but safe.
                // FIXME: This adds a dependency on the key manager,
                // which is not acceptible!
                PromptingKeyManager.setPrompting(false);
                // Logging doesn't work in a shutdown hook!
                //FileSystemManager.logger.setLevel(Level.OFF);
            } finally {
                try {
                    manager.sync(   new SyncExceptionBuilder(),
                                    BitField.of(FORCE_CLOSE_INPUT,
                                                FORCE_CLOSE_OUTPUT), null);
                } catch (IOException ouch) {
                    // Logging doesn't work in a shutdown hook!
                    ouch.printStackTrace();
                }
            }
        }
    } // class ShutdownRunnable
}
