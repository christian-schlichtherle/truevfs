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

package de.schlichtherle.truezip.io.archive;

import de.schlichtherle.truezip.io.archive.spi.ArchiveDriver;
import de.schlichtherle.truezip.io.util.SuffixSet;
import de.schlichtherle.truezip.util.ClassLoaderUtil;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The head of a chain of registries for archive file suffixes and archive
 * drivers.
 * Each element in the chain is an instance of this class which
 * maps single archive file suffixes (not lists) [{@code String}] to the
 * fully qualified class name, class object or instance of an archive driver
 * [{@code String}, {@code Class} or {@link ArchiveDriver}].
 * The {@link #getArchiveDriver} method can then be used to recursively
 * lookup an archive driver for a given (file name) suffix in the registry
 * chain.
 * <p>
 * This class is serializable in order to meet the requirements of the
 * {@link de.schlichtherle.truezip.io.File} class.
 * However, it's not really recommended to serialize this class:
 * Together with the instance, all associated archive drivers are serialized
 * too, which is pretty inefficient for a single instance.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ArchiveDriverRegistry extends HashMap {

    private static final long serialVersionUID = 3445783613096128268L;

    private static final String CLASS_NAME
            = "de.schlichtherle.truezip.io.archive.ArchiveDriverRegistry";
    private static final ResourceBundle resources
            = ResourceBundle.getBundle(CLASS_NAME);
    private static final Logger logger
            = Logger.getLogger(CLASS_NAME, CLASS_NAME);

    static final String KWD_DRIVER = "DRIVER";      // NOI18N
    static final String KWD_DEFAULT = "DEFAULT";    // NOI18N

    /**
     * The delegate used to lookup archive drivers when no driver is
     * configured locally.
     * May be {@code null}.
     */
    private final ArchiveDriverRegistry delegate;

    /**
     * Creates an empty {@code ArchiveDriverRegistry}.
     */
    public ArchiveDriverRegistry() {
        this.delegate = null;
    }

    /**
     * Creates a new {@code DefaultArchiveDriverRegistry} by
     * decorating the configuration of {@code delegate} with
     * mappings for all entries in {@code config}.
     * 
     * @param delegate The {@code ArchiveDriverRegistry} which's
     *        configuration is to be virtually inherited.
     * @param config A map of suffix lists and archive drivers.
     *        Each key in this map must be a non-null, non-empty archive file
     *        suffix list, obeying the usual syntax.
     *        Each value must either be an archive driver instance, an archive
     *        driver class, a string with the fully qualified name name of
     *        an archive driver class, or {@code null}.
     *        A {@code null} archive driver may be used to shadow a
     *        mapping for the same archive driver in {@code delegate},
     *        effectively removing it.
     * @throws NullPointerException If any parameter or configuration element
     *         other than an archive driver is {@code null}.
     * @throws IllegalArgumentException If any other parameter precondition
     *         does not hold or an illegal keyword is found in the
     *         configuration.
     * @see SuffixSet Syntax Definition for Suffix Lists
     */
    public ArchiveDriverRegistry(
            final ArchiveDriverRegistry delegate,
            final Map config) {
        if (delegate == null)
            throw new NullPointerException(getString("null", "delegate")); // NOI18N
        this.delegate = delegate;
        registerArchiveDrivers(config, true);
    }

    /**
     * Processes the given {@code config} and adds the configured archive
     * driver mappings.
     * 
     * @param config A map of suffix lists and archive driver IDs.
     *        Each key in this map must be a non-null, non-empty suffix list,
     *        obeying the usual syntax.
     *        Each value must either be an archive driver instance, an archive
     *        driver class, or a string with the fully qualified name name of
     *        an archive driver class.
     * @param eager Iff {@code true}, archive drivers are immediately
     *        instantiated and the keyword {@code DEFAULT} is not allowed.
     * @throws NullPointerException If any parameter or config element
     *         is {@code null}.
     * @throws IllegalArgumentException If any other parameter precondition
     *         does not hold or the keyword {@code DRIVER} is found.
     */
    final void registerArchiveDrivers(final Map config, final boolean eager) {
        try {
            for (final Iterator i = config.entrySet().iterator(); i.hasNext(); ) {
                final Map.Entry entry = (Map.Entry) i.next();
                final String key = (String) entry.getKey(); // may throw ClassCastException!
                if (KWD_DRIVER.equals(key))
                    throw new IllegalArgumentException(
                            getString("keyword", KWD_DRIVER)); // NOI18N
                final Object value = entry.getValue();
                if (KWD_DEFAULT.equals(key)) {
                    if (eager)
                        throw new IllegalArgumentException(
                                getString("keyword", KWD_DEFAULT)); // NOI18N
                    final SuffixSet set = (SuffixSet) super.get(key);
                    if (set != null)
                        set.addAll((String) value);
                    else
                        super.put(key, new SuffixSet((String) value));
                } else {
                    registerArchiveDriver(key, value, eager);
                }
            }
        } catch (NullPointerException npe) {
            throw npe;
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (RuntimeException rte) {
            throw new IllegalArgumentException(rte);
        }
    }

    /**
     * Registers the given archive {@code id} for the given
     * list of {@code suffixes}.
     * 
     * @param eager Whether the archive driver shall be instantiated or not.
     * @throws NullPointerException If {@code id} is {@code null}.
     * @throws ClassCastException If {@code eager} is {@code false}
     *         and {@code driver} isn't a string.
     * @throws IllegalArchiveDriverException If {@code eager} is
     *         {@code true} and {@code driver} can't get instantiated
     *         for some reason.
     *         The cause is wrapped in the exception.
     */
    private void registerArchiveDriver(
            final String list,
            Object driver,
            final boolean eager) {
        if (list == null)
            throw new NullPointerException(getString("noSuffixes")); // NOI18N
        final SuffixSet set = new SuffixSet(list);
        if (set.isEmpty()) {
            if (eager)
                throw new IllegalArgumentException(getString("noSuffixes")); // NOI18N
            else
                logger.log(Level.WARNING, "noSuffixes"); // NOI18N
        } else {
            driver = eager ? (Object) createArchiveDriver(driver) : (String) driver; // force cast
            for (Iterator i = set.iterator(); i.hasNext(); )
                super.put((String) i.next(), driver);
        }
    }

    /**
     * Returns the archive driver for the given canonical {@code suffix}
     * or {@code null} if no archive driver is found in the registry.
     * <p>
     * This instance is the head element of the registry chain.
     * If this head element does not hold an archive driver for the given
     * suffix, then the next element in the registry chain is searched.
     * This repeats recursively until either an archive driver is found or
     * the end of the chain is reached.
     * <p>
     * An archive driver is looked up in the registry as follows:
     * <ol>
     * <li>If the registry holds a string, it's supposed to be the fully
     *     qualified class name of an {@code ArchiveDriver}
     *     implementation. The class will be loaded and stored in the registry.
     * <li>If the registry then holds a class instance, it's instantiated
     *     with its no-arguments constructor, cast to the
     *     {@code ArchiveDriver} type and stored in the registry.
     * <li>If the registry then holds an instance of an
     *     {@code ArchiveDriver} implementation, it's returned.
     * <li>Otherwise, {@code null} is returned.
     * </ol>
     * This method is thread safe.
     *
     * @throws RuntimeException A subclass is thrown if loading or
     *         instantiating an archive driver class fails.
     */
    public final synchronized ArchiveDriver getArchiveDriver(
            final String suffix) {
        // Lookup driver locally.
        Object driver = super.get(suffix);
        if (!(driver instanceof ArchiveDriver)) {
            if (driver == null) {
                if (super.containsKey(suffix) || delegate == null)
                    return null;

                // Lookup the driver in the delegate and cache it in the
                // local registry.
                driver = delegate.getArchiveDriver(suffix); // may be null!
            } else {
                // We have found an entry in the drivers map, but it isn't
                // an ArchiveDriver, so we probably need to load its class
                // first and instantiate it.
                driver = createArchiveDriver(driver);
                logger.log(Level.FINE, "installed", // NOI18N
                        new Object[] { suffix, driver });
            }
            super.put(suffix, driver); // may map null!
        }
        return (ArchiveDriver) driver;
    }

    /**
     * Creates an archive driver from the given blueprint.
     *
     * @param driver A string with the fully qualified class name of an archive
     *        driver implementation, a class instance for an archive driver
     *        implementation, an archive driver instance or {@code null}.
     * @return An archive driver instance or {@code null} iff
     *         {@code driver} is {@code null}.
     * @throws IllegalArchiveDriverException If an archive driver cannot get
     *         returned.
     *         The cause is wrapped in the exception.
     */
    private static ArchiveDriver createArchiveDriver(Object driver) {
        try {
            if (driver instanceof String)
                driver = ClassLoaderUtil.loadClass((String) driver, ArchiveDriverRegistry.class);
            if (driver instanceof Class)
                driver = ((Class) driver).newInstance();
            return (ArchiveDriver) driver; // may throw ClassCastException
        } catch (Exception ex) {
            throw new IllegalArchiveDriverException(
                    getString("cannotCreate"), ex); // NOI18N
        }
    }

    /**
     * Returns the set of all suffixes which map to a valid archive driver in
     * the registry.
     * This includes the registry built by the entire chain, not just the
     * local registry.
     */
    public final SuffixSet suffixes() {
        return decorate(delegate != null ? delegate.suffixes() : new SuffixSet());
    }

    /**
     * Decorates the given suffix set by adding all suffixes which map to a
     * valid archive driver and removing all suffixes which map to
     * {@code null} in the local registry.
     *
     * @param set A non-null set of canonical archive file suffixes.
     * @return {@code set}, decorated according to the mappings in the
     *         local registry.
     */
    public final SuffixSet decorate(final SuffixSet set) {
        final SuffixSet local = new SuffixSet(super.keySet());
        for (final Iterator i = local.iterator(); i.hasNext(); ) {
            final String suffix = (String) i.next();
            assert super.containsKey(suffix);
            if (super.get(suffix) != null)
                set.addAll(suffix);
            else
                set.removeAll(suffix);
        }
        return set;
    }

    private static String getString(String key) {
        return resources.getString(key);
    }

    private static String getString(String key, String arg) {
        return MessageFormat.format(resources.getString(key),
                                    (Object[]) new String[] { arg });
    }

    static class IllegalArchiveDriverException
            extends IllegalArgumentException {
        private static final long serialVersionUID = 923470364629386423L;

        private IllegalArchiveDriverException(String msg, Exception ex) {
            super(msg);
            initCause(ex);
        }
    }
}
