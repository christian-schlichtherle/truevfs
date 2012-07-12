/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec;

import java.net.URISyntaxException;
import java.util.*;
import net.truevfs.kernel.spec.util.ExtensionSet;
import net.truevfs.kernel.spec.util.HashMaps;
import net.truevfs.kernel.spec.util.ServiceLocator;

/**
 * Static utility methods for implementing {@link FsDriverProvider}s.
 * 
 * @author Christian Schlichtherle
 */
public final class FsDriverMapProviders {

    /** Can't touch this - hammer time! */
    private FsDriverMapProviders() { }

    /**
     * Creates an unmodifiable file system driver map which is constructed from
     * the given configuration.
     *
     * @param  config an array of key-value pair arrays.
     *         The first element of each inner array must either be a
     *         {@link FsScheme file system scheme}, an object {@code o} which
     *         is convertable to a set of file name extensions by calling
     *         <code>new {@link ExtensionSet#ExtensionSet(String) ExtensionSet}(o.toString())</code>
     *         or a {@link Collection collection} of these.
     *         The second element of each inner array must either be a
     *         {@link FsDriver file system driver instance}, a
     *         {@link Class file system driver class}, a
     *         {@link String fully qualified name of a file system driver class},
     *         or {@code null}.
     * @return The file system driver map created from the given configuration.
     * @throws NullPointerException if a required configuration element is
     *         {@code null}.
     * @throws IllegalArgumentException if any other parameter precondition
     *         does not hold.
     * @see    ExtensionSet Syntax contraints for extension lists.
     */
    public static Map<FsScheme, FsDriver> newMap(final Object[][] config) {
        final Map<FsScheme, FsDriver> drivers = new HashMap<>(
                HashMaps.initialCapacity(config.length) * 2); // heuristics
        for (final Object[] param : config) {
            final Collection<FsScheme> schemes = toSchemes(param[0]);
            if (schemes.isEmpty())
                throw new IllegalArgumentException("No file system schemes!");
            final FsDriver driver = ServiceLocator.promote(param[1], FsDriver.class);
            for (final FsScheme scheme : schemes)
                drivers.put(scheme, driver);
        }
        return Collections.unmodifiableMap(drivers);
    }

    private static Collection<FsScheme> toSchemes(final Object o) {
        final Collection<FsScheme> set = new TreeSet<>();
        try {
            if (o instanceof Collection<?>)
                for (final Object p : (Collection<?>) o)
                    if (p instanceof FsScheme)
                        set.add((FsScheme) p);
                    else
                        for (final String q : new ExtensionSet(p.toString()))
                            set.add(new FsScheme(q));
            else if (o instanceof FsScheme)
                set.add((FsScheme) o);
            else
                for (final String p : new ExtensionSet(o.toString()))
                    set.add(new FsScheme(p));
        } catch (final URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
        return set;
    }
}
