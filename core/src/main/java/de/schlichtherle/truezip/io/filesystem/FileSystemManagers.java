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

import java.lang.reflect.UndeclaredThrowableException;

import static de.schlichtherle.truezip.util.ClassLoaders.loadClass;

/**
 * Provides static utility methods to access file system managers.
 * If you want to use this package with dependency injection, then you should
 * avoid using this class if possible because it uses static fields for
 * storing stateful objects.
 * <p>
 * This class is thread-safe.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FileSystemManagers {

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
        final int count = null == instance
                ? 0
                : instance.getControllers(null, null).size();
        if (0 < count)
            throw new IllegalStateException("There are " + count + " managed federated file systems!");
        if (null == manager)
            throw new NullPointerException();
        instance = manager;
    }
}
