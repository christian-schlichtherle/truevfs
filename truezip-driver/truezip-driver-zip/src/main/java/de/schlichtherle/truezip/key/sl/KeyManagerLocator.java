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
package de.schlichtherle.truezip.key.sl;

import de.schlichtherle.truezip.fs.spi.FsDriverService;
import de.schlichtherle.truezip.key.AbstractKeyManagerProvider;
import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.key.spi.KeyManagerService;
import de.schlichtherle.truezip.util.ServiceLocator;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import static java.util.logging.Level.*;
import java.util.logging.Logger;
import net.jcip.annotations.Immutable;

/**
 * Locates all key managers found on the class path.
 * The map of key managers is populated by instantiating all classes
 * which are named in the resource files with the name
 * {@code "META-INF/services/de.schlichtherle.truezip.key.spi.KeyManagerService"}
 * on the class path by calling their public no-argument constructor.
 *
 * @see     KeyManagerService
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public final class KeyManagerLocator extends AbstractKeyManagerProvider {

    /** The singleton instance of this class. */
    public static final KeyManagerLocator SINGLETON = new KeyManagerLocator();

    /** You cannot instantiate this class. */
    private KeyManagerLocator() {
    }

    @Override
    public Map<Class<?>, KeyManager<?>> get() {
        return Boot.MANAGERS;
    }

    /** A static data utility class used for lazy initialization. */
    private static final class Boot {
        static final Map<Class<?>, KeyManager<?>> MANAGERS;
        static {
            final Logger logger = Logger.getLogger(
                    KeyManagerLocator.class.getName(),
                    KeyManagerLocator.class.getName());
            final Iterator<KeyManagerService>
                    i = new ServiceLocator(KeyManagerLocator.class.getClassLoader())
                        .getServices(KeyManagerService.class);
            final Map<Class<?>, KeyManager<?>>
                    sorted = new TreeMap<Class<?>, KeyManager<?>>(
                        ClassComparator.INSTANCE);
            if (!i.hasNext())
                logger.log(WARNING, "null", FsDriverService.class);
            while (i.hasNext()) {
                KeyManagerService service = i.next();
                logger.log(CONFIG, "located", service);
                for (final Map.Entry<Class<?>, KeyManager<?>> entry
                        : service.get().entrySet()) {
                    final Class<?> type = entry.getKey();
                    final KeyManager<?> newManager = entry.getValue();
                    if (null != type && null != newManager) {
                        final KeyManager<?> oldManager = sorted.put(type, newManager);
                        if (null != oldManager
                                && oldManager.getPriority() > newManager.getPriority())
                            sorted.put(type, oldManager);
                    }
                }
            }
            final Map<Class<?>, KeyManager<?>>
                    fast = new LinkedHashMap<Class<?>, KeyManager<?>>(
                        sorted.size() * 4 / 3 + 1);
            for (final Map.Entry<Class<?>, KeyManager<?>> entry : sorted.entrySet()) {
                final Class<?> type = entry.getKey();
                final KeyManager<?> manager = entry.getValue();
                logger.log(CONFIG, "mapping",
                        new Object[] { type, manager });
                fast.put(type, manager);
            }
            MANAGERS = Collections.unmodifiableMap(fast);
        }
    } // class Boot

    private static final class ClassComparator implements Comparator<Class<?>> {
        static final ClassComparator INSTANCE = new ClassComparator();

        @Override
        public int compare(Class<?> o1, Class<?> o2) {
            return o1.getName().compareTo(o2.getName());
        }
    } // ClassComparator
}
