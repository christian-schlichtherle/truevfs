/*
 * Copyright 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.sl;

import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.fs.FsDriverProvider;
import de.schlichtherle.truezip.fs.spi.FsDriverService;
import de.schlichtherle.truezip.util.ServiceLocator;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jcip.annotations.Immutable;

/**
 * Locates all file system drivers found on the class path.
 * The map of file system drivers is populated by instantiating all classes
 * which are named in the resource files with the name
 * {@code "META-INF/services/de.schlichtherle.truezip.fs.spi.FsDriverService"}
 * on the class path by calling their no-arg constructor.
 * <p>
 * If no file system drivers are found, a {@link ServiceConfigurationError} is
 * thrown.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
@edu.umd.cs.findbugs.annotations.SuppressWarnings("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
public final class FsDriverLocator implements FsDriverProvider {

    /** The singleton instance of this class. */
    public static final FsDriverLocator SINGLETON = new FsDriverLocator();

    private volatile @CheckForNull Map<FsScheme, FsDriver> drivers;

    /** You cannot instantiate this class. */
    private FsDriverLocator() {
    }

    @Override
    public Map<FsScheme, FsDriver> get() {
        Map<FsScheme, FsDriver> drivers = this.drivers;
        if (null != drivers) // DCL does work with volatile fields since JSE 5!
            return drivers;
        synchronized (this) {
            drivers = this.drivers;
            if (null != drivers)
                return drivers;
            final Logger
                    logger = Logger.getLogger(  FsDriverLocator.class.getName(),
                                                FsDriverLocator.class.getName());
            final Iterator<FsDriverService>
                    i = new ServiceLocator(FsDriverLocator.class.getClassLoader())
                        .getServices(FsDriverService.class);
            drivers = new HashMap<FsScheme, FsDriver>();
            if (!i.hasNext())
                throw new ServiceConfigurationError(
                        "No provider available for " + FsDriverService.class);
            while (i.hasNext()) {
                FsDriverService service = i.next();
                logger.log(Level.CONFIG, "located", service);
                for (final Map.Entry<FsScheme, FsDriver> entry
                        : service.get().entrySet()) {
                    final FsScheme scheme = entry.getKey();
                    final FsDriver driver = entry.getValue();
                    if (null != scheme && null != driver) {
                        drivers.put(scheme, driver);
                        logger.log(Level.CONFIG, "mapped",
                                new Object[] { scheme, driver });
                    }
                }
            }
            return this.drivers = Collections.unmodifiableMap(drivers);
        }
    }
}
