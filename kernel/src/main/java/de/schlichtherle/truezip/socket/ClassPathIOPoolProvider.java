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
import net.jcip.annotations.Immutable;

/**
 * Loads an I/O pool provider from the class path and delegates any call to
 * {@link #getPool()} to it.
 * <p>
 * The I/O pool provider is located by instantiating one of the classes
 * which are named in the resource files with the name
 * {@code "META-INF/services/de.schlichtherle.truezip.socket.IOPoolProvider"}
 * on the class path.
 * Note that all named classes must implement the interface
 * {@link IOPoolProvider} and provide a public available no-arg constructor.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public class ClassPathIOPoolProvider implements IOPoolProvider {

    public static final ClassPathIOPoolProvider
            INSTANCE = new ClassPathIOPoolProvider();

    private final IOPoolProvider provider;

    /** You cannot instantiate this class. */
    private ClassPathIOPoolProvider() {
        provider = new ServiceLocator(
                    ClassPathIOPoolProvider.class.getClassLoader())
                .getServices(IOPoolProvider.class)
                .next();
    }

    @Override
    public IOPool<?> getPool() {
        return provider.getPool();
    }
}
