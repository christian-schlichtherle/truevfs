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
package de.schlichtherle.truezip.io.fs;

import de.schlichtherle.truezip.util.ServiceLocator;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import net.jcip.annotations.ThreadSafe;

import static de.schlichtherle.truezip.io.fs.FsController.*;

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
public class FSManagers {

    /** You cannot instantiate this class. */
    private FSManagers() {
    }

    private static volatile FSManager instance; // volatile required for DCL in JSE 5!
    private static ShutdownThread shutdownThread; // lazily initialized

    /**
     * Returns the file system manager value of this class property.
     * <p>
     * If the class property has been explicitly set using
     * {@link #setInstance}, then this instance is returned.
     * Otherwise, the service is located by loading the class name from the
     * resource file {@code /META-INF/services/de.schlichtherle.truezip.io.filesystem.FSManager}.
     * In order to support this plug-in architecture, you should <em>not</em>
     * cache the instance returned by this method!
     * <p>
     * Note that the returned file system manager is instrumented to run its
     * {@link FSManager#sync} method when the JVM terminates by means
     * of a shutdown hook.
     *
     * @throws ClassCastException If the class name in the system property
     *         does not denote a subclass of this class.
     * @throws UndeclaredThrowableException If any other precondition on the
     *         value of the system property does not hold.
     * @return The non-{@code null} file system manager value of this class
     *         property.
     */
    @NonNull
    public static FSManager getInstance() {
        FSManager manager = instance;
        if (null == manager) {
            synchronized (FSManagers.class) { // DCL does work in combination with volatile in JSE 5!
                manager = instance;
                if (null == manager) {
                    manager = new ServiceLocator(FSManagers.class.getClassLoader())
                            .getService(FSManager.class, FSFederationManager.class);
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
     * To avoid this, call its {@link FSManager#sync} method and make
     * sure to purge all references to the file system controllers which are
     * returned by its {@link FSManager#getController} method prior to
     * calling this method.
     * <p>
     * If the given file system manager is {@code null}, a new instance will
     * be created on the next call to {@link #getInstance}.
     * <p>
     * If the given file system manager is not {@code null}, this instance is
     * instrumented to run its {@link FSManager#sync} method when the
     * JVM terminates by means of a shutdown hook.
     *
     * @param  manager the nullable file system manager value of this class
     *         property.
     * @throws IllegalStateException if the current file system manager has any
     *         managed file systems.
     */
    public static synchronized void setInstance(
            @CheckForNull final FSManager manager) {
        final int count = null == instance ? 0 : instance.getSize();
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
        ShutdownThread(final FSManager manager) {
            super(  new ShutdownRunnable(manager),
                    "TrueZIP FileSystemManager Shutdown Hook");
            super.setPriority(Thread.MAX_PRIORITY);
        }
    } // class ShutdownThread

    private static final class ShutdownRunnable implements Runnable {

        final FSManager manager;

        ShutdownRunnable(final FSManager manager) {
            assert null != manager;
            this.manager = manager;
        }

        @Override
        @SuppressWarnings("CallToThreadDumpStack")
        public void run() {
            try {
                FSSyncExceptionBuilder builder = new FSSyncExceptionBuilder();
                manager.sync(UMOUNT, builder);
                builder.check();
            } catch (IOException ouch) {
                // Logging doesn't work in a shutdown hook!
                ouch.printStackTrace();
            }
        }
    } // class ShutdownRunnable
}
