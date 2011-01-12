/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.util.ServiceLocator;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.ServiceConfigurationError;
import net.jcip.annotations.ThreadSafe;

/**
 * A static service locator and container for a pool of I/O entries.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class IOPools {

    private static volatile IOPool<?> instance; // volatile required for DCL in JSE 5!

    /** You cannot instantiate this class. */
    private IOPools() {
    }

    /**
     * Returns the default I/O pool.
     * <p>
     * If the default I/O pool has been explicitly set to non-{@code null}
     * by calling {@link #setPool}, then this instance is returned.
     * <p>
     * Otherwise, the service is located by loading the class name from any
     * resource file with the name
     * {@code "META-INF/services/de.schlichtherle.truezip.socket.IOPool"}
     * on the class path and instantiating it using its no-arg constructor.
     * In order to support this plug-in architecture, you should <em>not</em>
     * cache the instance returned by this method!
     *
     * @return The default I/O pool.
     * @throws RuntimeException at the discretion of the {@link ServiceLocator}.
     * @throws ServiceConfigurationError at the discretion of the
     *         {@link ServiceLocator}.
     */
    public static @NonNull IOPool<?> getPool() {
        IOPool<?> pool = instance;
        if (null == pool) {
            synchronized (IOPools.class) { // DCL does work in combination with volatile in JSE 5!
                pool = instance;
                if (null == pool) {
                    instance = pool
                            = new ServiceLocator(IOPools.class.getClassLoader())
                            .getServices(IOPool.class)
                            .next();
                }
            }
        }
        return pool;
    }

    /**
     * Sets the default I/O pool.
     * <p>
     * If the given default I/O pool is {@code null},
     * a new instance will be created on the next call to {@link #getPool}.
     *
     * @param pool the nullable default I/O pool.
     */
    public static synchronized void setPool(@Nullable IOPool<?> pool) {
        instance = pool;
    }
}
