/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access;

import net.java.truecommons.services.Container;
import net.java.truecommons.services.Loader;
import java.io.File;
import java.net.URISyntaxException;
import java.util.*;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import javax.inject.Provider;
import net.java.truevfs.kernel.spec.FsAbstractCompositeDriver;
import net.java.truevfs.kernel.spec.FsDriver;
import net.java.truevfs.kernel.spec.FsScheme;
import net.java.truevfs.kernel.spec.sl.FsDriverMapLocator;
import net.java.truecommons.shed.ExtensionSet;
import net.java.truecommons.shed.HashMaps;
import static net.java.truecommons.shed.HashMaps.initialCapacity;

/**
 * Detects a <em>prospective</em> archive file and declares its file system
 * type by mapping its file name extension to an archive driver.
 * Note that this class does <em>not</em> access any file system!
  * <p>
 * The map of detectable archive file name extensions and corresponding archive
 * drivers is configured by the constructors of this class.
 * There are two types of constructors available:
 * <ol>
 * <li>Constructors which filter the driver map of a given file system driver
 *     provider by a given list of file name extensions.
 *     For example, the driver map of the provider
 *     {@link FsDriverMapLocator#SINGLETON} could be filtered by the file name
 *     extension list {@code "tar|zip"} in order to recognize only TAR and ZIP
 *     files.
 * <li>Constructors which decorate a given file system driver provider with a
 *     given map of file system schemes to file system drivers.
 *     This can get used to specify custom archive file name extensions or
 *     archive drivers.
 *     For example, the file name extension list {@code "foo|bar"} could be used
 *     to detect a custom variant of the JAR file format (you need to provide
 *     a custom archive driver then, too).
 * </ol>
 * <p>
 * Where a constructor expects a list of file name extensions as a parameter,
 * it must obeye the syntax constraints for {@link ExtensionSet}s.
 * As an example, the parameter {@code "zip|jar"} would cause
 * the archive detector to recognize ZIP and JAR files in a path.
 * The same would be true for {@code "||.ZiP||.JaR||ZIP||JAR||"},
 * but this notation is discouraged because it's not in canonical form.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public final class TArchiveDetector
extends FsAbstractCompositeDriver implements Container<Map<FsScheme, FsDriver>> {

    /**
     * This instance never recognizes any archive files in a path.
     * This can get used as the end of a chain of
     * {@code TArchiveDetector} instances or if archive files
     * shall be treated like regular files rather than (virtual) directories.
     */
    public static final TArchiveDetector NULL = new TArchiveDetector("");

    /**
     * This instance recognizes all archive types for which an archive driver
     * can be found by the file system driver service locator singleton
     * {@link FsDriverMapLocator#SINGLETON}.
     */
    public static final TArchiveDetector ALL = new TArchiveDetector(null);

    private final Map<FsScheme, FsDriver> drivers;

    /**
     * The canonical string respresentation of the set of extensions recognized
     * by this archive detector.
     * This set is used to filter the registered archive file extensions in
     * {@link #drivers}.
     */
    private final String extensions;

    /**
     * Equivalent to
     * {@link #TArchiveDetector(FsDriverMapProvider, String)
     * TArchiveDetector(FsDriverMapLocator.SINGLETON, extensions)}.
     */
    public TArchiveDetector(@CheckForNull String extensions) {
        this(FsDriverMapLocator.SINGLETON, extensions);
    }

    /**
     * Constructs a new {@code TArchiveDetector} by filtering the given driver
     * provider for all canonicalized extensions in the {@code extensions} list.
     *
     * @param  provider the file system driver provider to filter.
     * @param  extensions A list of file name extensions which shall identify
     *         prospective archive files.
     *         If this is {@code null}, no filtering is applied and all drivers
     *         known by the given provider are available for use with this
     *         archive detector.
     * @throws IllegalArgumentException If any of the extensions in the list
     *         names a extension for which no file system driver is known by the
     *         provider.
     * @see    ExtensionSet Syntax constraints for extension lists.
     */
    public TArchiveDetector(final Provider<Map<FsScheme, FsDriver>> provider,
                            final @CheckForNull String extensions) {
        final Map<FsScheme, FsDriver> inDrivers = provider.get();
        final ExtensionSet inExtensions;
        final Map<FsScheme, FsDriver> outDrivers;
        if (null != extensions) {
            inExtensions = new ExtensionSet(extensions);
            outDrivers = new HashMap<>(initialCapacity(inDrivers.size()));
        } else {
            inExtensions = null;
            outDrivers = inDrivers;
        }
        final ExtensionSet outExtensions = new ExtensionSet();
        for (final Map.Entry<FsScheme, FsDriver> entry : inDrivers.entrySet()) {
            final FsDriver driver = entry.getValue();
            assert null != driver;
            final FsScheme scheme = entry.getKey();
            final boolean ad = driver.isArchiveDriver();
            if (null != inExtensions) {
                final boolean accepted = inExtensions.contains(scheme.toString());
                if (!ad || accepted) outDrivers.put(scheme, driver);
                if (ad && accepted) outExtensions.add(scheme.toString());
            } else {
                if (ad) outExtensions.add(scheme.toString());
            }
        }
        if (null != inExtensions) {
            inExtensions.removeAll(outExtensions);
            if (!inExtensions.isEmpty())
                throw new IllegalArgumentException(
                        "\"" + inExtensions + "\" (no archive driver installed for these extensions)");
        }
        this.drivers = Collections.unmodifiableMap(outDrivers);
        this.extensions = outExtensions.toString();
    }

    /**
     * Equivalent to
     * {@link #TArchiveDetector(FsDriverMapProvider, String, FsDriver)
     * TArchiveDetector(TArchiveDetector.NULL, extensions, driver)}.
     */
    public TArchiveDetector(String extensions, @CheckForNull FsDriver driver) {
        this(NULL, extensions, driver);
    }

    /**
     * Constructs a new {@code TArchiveDetector} by
     * decorating the configuration of {@code provider} with
     * mappings for all canonicalized extensions in {@code extensions} to
     * {@code driver}.
     * 
     * @param  provider the file system driver provider to decorate.
     * @param  extensions A list of file name extensions which shall identify
     *         prospective archive files.
     *         This must not be {@code null} and must not be empty.
     * @param  driver the file system driver to map for the extension list.
     *         {@code null} may be used to <i>shadow</i> a mapping for an equal
     *         file system scheme in {@code provider} by removing it from the
     *         resulting map for this detector.
     * @throws NullPointerException if a required configuration element is
     *         {@code null}.
     * @throws IllegalArgumentException if any other parameter precondition
     *         does not hold.
     * @see    ExtensionSet Syntax contraints for extension lists.
     */
    public TArchiveDetector(Provider<Map<FsScheme, FsDriver>> provider,
                            String extensions,
                            @CheckForNull FsDriver driver) {
        this(provider, new Object[][] {{ extensions, driver }});
    }

    /**
     * Creates a new {@code TArchiveDetector} by
     * decorating the configuration of {@code provider} with
     * mappings for all entries in {@code config}.
     * 
     * @param  provider the file system driver provider to decorate.
     * @param  config an array of key-value pair arrays.
     *         The first element of each inner array must either be a
     *         {@link FsScheme file system scheme}, an object {@code o} which
     *         can get converted to a set of file name extensions by calling
     *         {@link ExtensionSet#ExtensionSet(String) new ExtensionSet(o.toString())}
     *         or a {@link Collection collection} of these.
     *         The second element of each inner array must either be a
     *         {@link FsDriver file system driver object}, a
     *         {@link Class file system driver class}, a
     *         {@link String fully qualified name of a file system driver class},
     *         or {@code null}.
     *         {@code null} may be used to <i>shadow</i> a mapping for an equal
     *         file system scheme in {@code provider} by removing it from the
     *         resulting map for this detector.
     * @throws NullPointerException if a required configuration element is
     *         {@code null}.
     * @throws IllegalArgumentException if any other parameter precondition
     *         does not hold.
     * @see    ExtensionSet Syntax contraints for extension lists.
     */
    public TArchiveDetector(Provider<Map<FsScheme, FsDriver>> provider, Object[][] config) {
        this(provider, newMap(config));
    }

    /**
     * Constructs a new {@code TArchiveDetector} by decorating the given driver
     * provider with mappings for all entries in {@code config}.
     * 
     * @param  provider the file system driver provider to decorate.
     * @param  config a map of file system schemes to file system drivers.
     *         {@code null} may be used to <i>shadow</i> a mapping for an equal
     *         file system scheme in {@code provider} by removing it from the
     *         resulting map for this detector.
     * @throws NullPointerException if a required configuration element is
     *         {@code null}.
     * @throws ClassCastException if a configuration element is of the wrong
     *         type.
     * @throws IllegalArgumentException if any other parameter precondition
     *         does not hold.
     * @see    ExtensionSet Syntax contraints for extension lists.
     */
    public TArchiveDetector(final Provider<Map<FsScheme, FsDriver>> provider,
                            final Map<FsScheme, FsDriver> config) {
        final Map<FsScheme, FsDriver> inDrivers = provider.get();
        final Map<FsScheme, FsDriver> 
                outDrivers = new HashMap<>(initialCapacity(inDrivers.size()));
        final ExtensionSet outExtensions = new ExtensionSet();
        for (final Map.Entry<FsScheme, FsDriver> entry : inDrivers.entrySet()) {
            final FsDriver driver = entry.getValue();
            assert null != driver;
            /*if (null == driver)
                continue;*/
            final FsScheme scheme = entry.getKey();
            outDrivers.put(scheme, driver);
            if (driver.isArchiveDriver()) outExtensions.add(scheme.toString());
        }
        for (final Map.Entry<FsScheme, FsDriver> entry : config.entrySet()) {
            final FsScheme scheme = entry.getKey();
            final FsDriver driver = entry.getValue();
            if (null != driver) {
                outDrivers.put(scheme, driver);
                outExtensions.add(scheme.toString());
            } else {
                outDrivers.remove(scheme);
                outExtensions.remove(scheme.toString());
            }
        }
        this.drivers = Collections.unmodifiableMap(outDrivers);
        this.extensions = outExtensions.toString();
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<FsScheme, FsDriver> get() {
        return drivers;
    }

    /**
     * Detects whether the given {@code path} name identifies a prospective
     * archive file by matching its file name extensions against the set of file
     * system schemes in the archive driver map.
     * If a match is found, the file name extension gets converted to a file
     * system scheme and returned.
     * Otherwise, {@code null} is returned.
     *
     * @param  path the path name.
     * @return A file system scheme to declare the file system type of the
     *         prospective archive file or {@code null} if no archive file name
     *         extension has been detected.
     */
    public @CheckForNull FsScheme scheme(String path) {
        // An archive file name extension may contain a dot (e.g. "tar.gz"), so
        // we can't just look for the last dot in the file name and look up the
        // remainder in the key set of the archive driver map.
        // Likewise, a file name may contain additional dots, so we can't just
        // look for the first dot in it and look up the remainder ...
        path = path.replace('/', File.separatorChar);
        int i = path.lastIndexOf(File.separatorChar) + 1;
        path = path.substring(i);//.toLowerCase(Locale.ROOT);
        final int l = path.length();
        FsScheme scheme;
        for (i = 0; 0 < (i = path.indexOf('.', i) + 1) && i < l ;) {
            try {
                scheme = new FsScheme(path.substring(i));
            } catch (URISyntaxException noSchemeNoArchiveBadLuck) {
                continue; // TODO: http://java.net/jira/browse/TRUEZIP-132
            }
            final FsDriver driver = drivers.get(scheme);
            if (null != driver && driver.isArchiveDriver()) return scheme;
        }
        return null;
    }

    /**
     * Returns the <i>canonical extension list</i> for all federated file system
     * types recognized by this {@code TArchiveDetector}.
     *
     * @return Either {@code ""} to indicate an empty set or
     *         a string of the form {@code "extension[|extension]*"},
     *         where {@code extension} is a combination of lower case
     *         letters which does <em>not</em> start with a dot.
     *         The string never contains empty or duplicated extensions and the
     *         extensions are sorted in natural order.
     * @see    #TArchiveDetector(String)
     * @see    ExtensionSet Syntax constraints for extension lists.
     */
    @Override
    public String toString() {
        return extensions;
    }

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
    private static Map<FsScheme, FsDriver> newMap(final Object[][] config) {
        final Map<FsScheme, FsDriver> drivers = new HashMap<>(
                HashMaps.initialCapacity(config.length) * 2); // heuristics
        for (final Object[] param : config) {
            final Collection<FsScheme> schemes = toSchemes(param[0]);
            if (schemes.isEmpty())
                throw new IllegalArgumentException("No file system schemes!");
            final FsDriver driver = Loader.promote(param[1], FsDriver.class);
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
