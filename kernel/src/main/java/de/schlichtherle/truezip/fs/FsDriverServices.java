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
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.util.ServiceLocator;
import de.schlichtherle.truezip.util.SuffixSet;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.TreeSet;
import net.jcip.annotations.Immutable;

/**
 * Contains all file system drivers found on the class path.
 * Its map of file system drivers is populated by instantiating all classes
 * which are named in the resource files with the name
 * {@code "META-INF/services/de.schlichtherle.truezip.fs.FsDriverService"}
 * on the class path by calling their no-arg constructor.
 * <p>
 * If no file system drivers are found, a {@link ServiceConfigurationError} is
 * thrown.
 * <p>
 * Note that the kernel classes have no dependency on this class; so using
 * this service locator is completely optional for a pure kernel application.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public final class FsDriverServices {

    /** You cannot instantiate this class. */
    private FsDriverServices() {
    }

    /**
     * A static factory method for an unmodifiable driver map which is
     * constructed from the given configuration.
     * This method is intended to be used by provider implementations
     * of the {@link FsDriverService} interface for convenient creation of the
     * map to return by their {@link FsDriverService#getDrivers()} method.
     *
     * @param  config an array of key-value pair arrays.
     *         The first element of each inner array must either be a
     *         {@link FsScheme file system scheme}, an object {@code o} which
     *         can get converted to a set of file system suffixes by calling
     *         {@link SuffixSet#SuffixSet(String) new SuffixSet(o.toString())}
     *         or a {@link Collection collection} of these.
     *         The second element of each inner array must either be a
     *         {@link FsDriver file system driver object}, a
     *         {@link Class file system driver class}, a
     *         {@link String fully qualified name of a file system driver class},
     *         or {@code null}.
     * @return The new map to use as the return value of
     *         {@link FsDriverService#getDrivers()}.
     * @throws NullPointerException if a required configuration element is
     *         {@code null}.
     * @throws IllegalArgumentException if any other parameter precondition
     *         does not hold.
     * @see    SuffixSet Syntax contraints for suffix lists.
     */
    public static Map<FsScheme, FsDriver> newMap(final Object[][] config) {
        final Map<FsScheme, FsDriver> drivers = new HashMap<FsScheme, FsDriver>();
        for (final Object[] param : config) {
            final Collection<FsScheme> schemes = toSchemes(param[0]);
            final FsDriver driver = toDriver(param[1]);
            if (schemes.isEmpty())
                throw new IllegalArgumentException("No schemes for " + driver);
            for (FsScheme scheme : schemes)
                drivers.put(scheme, driver);
        }
        return Collections.unmodifiableMap(drivers);
    }

    private static @NonNull Collection<FsScheme> toSchemes(@NonNull Object o) {
        Collection<FsScheme> set = new TreeSet<FsScheme>();
        try {
            if (o instanceof Collection<?>)
                for (Object p : (Collection<?>) o)
                    if (p instanceof FsScheme)
                        set.add((FsScheme) p);
                    else
                        for (String q : new SuffixSet(p.toString()))
                            set.add(new FsScheme(q));
            else if (o instanceof FsScheme)
                set.add((FsScheme) o);
            else
                for (String p : new SuffixSet(o.toString()))
                    set.add(new FsScheme(p));
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
        return set;
    }

    @SuppressWarnings("unchecked")
    private static @CheckForNull FsDriver toDriver(@CheckForNull Object driver) {
        try {
        if (driver instanceof String)
            driver = new ServiceLocator(FsDriverServices.class.getClassLoader())
                    .getClass((String) driver);
        } catch (ServiceConfigurationError ex) {
            throw new IllegalArgumentException(ex);
        }
        try {
            if (driver instanceof Class<?>)
                driver = ((Class<? extends FsDriver>) driver).newInstance();
            return (FsDriver) driver; // may throw ClassCastException
        } catch (ClassCastException ex) {
            throw new IllegalArgumentException(ex);
        } catch (InstantiationException ex) {
            throw new IllegalArgumentException(ex);
        } catch (IllegalAccessException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}
