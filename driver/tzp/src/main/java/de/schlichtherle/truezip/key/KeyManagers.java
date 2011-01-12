/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.key;

import de.schlichtherle.truezip.util.ServiceLocator;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.ServiceConfigurationError;
import net.jcip.annotations.ThreadSafe;

/**
 * A static service locator and container for a key manager instance.
 * If you want to use this package with dependency injection, then you should
 * avoid using this class if possible because it uses a static field for
 * storing the stateful key manager instance.
 * <p>
 * This class is thread-safe.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public abstract class KeyManagers {

    private static volatile KeyManager instance; // volatile required for DCL in JSE 5!

    /** You cannot instantiate this class. */
    private KeyManagers() {
    }

    /**
     * Returns the default key manager.
     * <p>
     * If the default key manager has been explicitly set to
     * non-{@code null} by calling {@link #setManager}, then this instance is
     * returned.
     * <p>
     * Otherwise, the service is located by loading the class name from any
     * resource file with the name
     * {@code "META-INF/services/de.schlichtherle.truezip.key.KeyManager"}.
     * on the class path and instantiating it using its no-arg constructor.
     * In order to support this plug-in architecture, you should <em>not</em>
     * cache the instance returned by this method!
     *
     * @return The default key manager.
     * @throws RuntimeException at the discretion of the {@link ServiceLocator}.
     * @throws ServiceConfigurationError at the discretion of the
     *         {@link ServiceLocator}.
     */
    public static @NonNull KeyManager getManager() {
        KeyManager manager = instance;
        if (null == manager) {
            synchronized (KeyManagers.class) { // DCL does work in combination with volatile in JSE 5!
                manager = instance;
                if (null == manager) {
                    instance = manager
                            = new ServiceLocator(KeyManagers.class.getClassLoader())
                            .getServices(KeyManager.class)
                            .next();
                }
            }
        }
        return manager;
    }

    /**
     * Sets the default key manager.
     * <p>
     * If the given default key manager is {@code null},
     * a new instance will be created on the next call to {@link #getManager}.
     *
     * @param manager the nullable default key manager.
     * @throws IllegalStateException if the current key manager has any
     *         key providers.
     */
    public static synchronized void setManager(final @Nullable KeyManager manager) {
        instance = manager;
    }
}
