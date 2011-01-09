/*
 * Copyright (C) 2007-2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.fs.archive;

import de.schlichtherle.truezip.io.fs.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.util.ServiceLocator;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A global registry for mappings from archive file suffixes [{@link String}]
 * to archive drivers [{@link ArchiveDriver}] which is configured by the set
 * of all <i>configuration files</i> on the class path.
 * This registry does not have a delegate, so it can only be used as the tail
 * in a {@link ArchiveDriverRegistry registry chain}.
 * <p>
 * When this class is instantiated, it enumerate all instances of the relative
 * path {@code META-INF/services/de.schlichtherle.truezip.io.fs.archive.driver.properties}
 * on the class path (this ensures that TrueZIP is compatible with JNLP as used
 * by Java Web Start and can be safely added to the Extension Class Path).
 * <p>
 * These configuration files are processed in arbitrary order.
 * However, configuration files which contain the entry
 * {@code DRIVER=true} have lesser priority and will be overruled by
 * any other configuration files which do not contain this entry.
 * This is used by the default configuration file in TrueZIP's JAR:
 * It contains this entry in order to allow any client application provided
 * configuration file to overrule it.
 * <p>
 * This class may appear to be a singleton (there's not much point in
 * having multiple instances of this class, all with the same configuration).
 * However, it actually isn't a true singleton because it's
 * {@link Serializable} in order to meet the requirements of some client
 * classes.
 * Of course, this will only work if the mapped {@link ArchiveDriver}s are
 * serializable, too.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class GlobalArchiveDriverRegistry
extends ArchiveDriverRegistry
implements Serializable {

    private static final long serialVersionUID = 1579600190374703884L;
    private static final String CLASS_NAME
            = GlobalArchiveDriverRegistry.class.getName();
    private static final Logger logger
            = Logger.getLogger(CLASS_NAME, CLASS_NAME);

    static final String KWD_NULL = "NULL";  // NOI18N
    static final String KWD_ALL = "ALL";    // NOI18N


    /** The (pseudo) singleton instance. */
    public static final GlobalArchiveDriverRegistry INSTANCE
            = new GlobalArchiveDriverRegistry();

    static {
        logger.config("banner"); // NOI18N
        INSTANCE.logConfiguration();
    }

    /**
     * Creates a new {@code GlobalArchiveDriverRegistry}.
     * This constructor logs some configuration messages at
     * {@code Level.CONFIG}.
     * If an exception occurs during processing of the configuration resource
     * files or no archive drivers are registered, then one or more warnings
     * messages are logged at {@code Level.WARNING}, but otherwise the
     * constructor terminates normally.
     * This is to ensure that TrueZIP can be used without throwing exceptions
     * in static initializers just because of a bug in a configuration
     * resource file.
     */
    private GlobalArchiveDriverRegistry() {
        registerArchiveDrivers();
    }

    private void registerArchiveDrivers() {
        final ArchiveDriverRegistry clientRegistry = new ArchiveDriverRegistry();
        registerArchiveDrivers(
                "META-INF/services/" + ArchiveDriver.class.getPackage().getName() + ".properties",
                this,
                clientRegistry);
        drivers.putAll(clientRegistry.drivers);
    }

    /**
     * Enumerates all resource URLs for {@code service} on the class
     * path and calls
     * {@link #registerArchiveDrivers(URL, ArchiveDriverRegistry, ArchiveDriverRegistry)}
     * on each instance.
     * <p>
     * Ensures that configuration files specified by client
     * applications always override configuration files specified
     * by driver implementations.
     */
    private static void registerArchiveDrivers(
            final String service,
            final ArchiveDriverRegistry driverRegistry,
            final ArchiveDriverRegistry clientRegistry) {
        assert service != null;
        assert driverRegistry != null;
        assert clientRegistry != null;

        final Enumeration<URL> urls;
        try {
            urls = new ServiceLocator(GlobalArchiveDriverRegistry.class.getClassLoader())
                    .getResources(service);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "lookup.ex", ex); // NOI18N
            return;
        }

        while (urls.hasMoreElements()) {
            final URL url = urls.nextElement();
            registerArchiveDrivers(url, driverRegistry, clientRegistry);
        }
    }

    /**
     * Loads and processes the given {@code url} in order to register
     * the archive drivers in its config resource file.
     */
    private static void registerArchiveDrivers(
            final URL url,
            final ArchiveDriverRegistry driverRegistry,
            final ArchiveDriverRegistry clientRegistry) {
        assert url != null;
        assert driverRegistry != null;
        assert clientRegistry != null;

        // Load the configuration map from the properties file.
        logger.log(Level.CONFIG, "loading", url); // NOI18N
        final Properties config = new Properties();
        try {
            final InputStream in = url.openStream();
            try {
                config.load(in);
                registerArchiveDrivers(config, driverRegistry, clientRegistry);
            } finally {
                in.close();
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "loading.ex", ex); // NOI18N
            // Continue normally.
        }
    }

    /**
     * Processes the given {@code config} in order to register
     * its archive drivers.
     * 
     * @throws NullPointerException If any archive driver ID in the
     *         configuration is {@code null}.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void registerArchiveDrivers(
            final Properties config,
            final ArchiveDriverRegistry driverRegistry,
            final ArchiveDriverRegistry clientRegistry) {
        assert config != null;
        assert driverRegistry != null;
        assert clientRegistry != null;

        // Consume and process DRIVER entry.
        final String driver = (String) config.remove(KWD_DRIVER);
        final boolean isDriver = Boolean.TRUE.equals(Boolean.valueOf(driver));

        // Select registry and register drivers.
        (isDriver ? driverRegistry : clientRegistry).registerArchiveDrivers(
                (Map) config, false);
    }

    private void logConfiguration() {
        final Iterator<Map.Entry<String, Object>> i = drivers.entrySet().iterator();
        if (i.hasNext()) {
            do {
                final Map.Entry<String, Object> entry = i.next();
                logger.log(Level.CONFIG, "driverRegistered", // NOI18N
                        new Object[] { entry.getKey(), entry.getValue() });
            } while (i.hasNext());
            logger.log(Level.CONFIG, "suffixList", getSuffixes().toString()); // NOI18N
        } else {
            logger.warning("noDriversRegistered"); // NOI18N
        }
    }
}
