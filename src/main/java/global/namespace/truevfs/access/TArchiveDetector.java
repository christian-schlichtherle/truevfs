/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.access;

import global.namespace.truevfs.comp.util.ExtensionSet;
import global.namespace.truevfs.kernel.api.FsAbstractCompositeDriver;
import global.namespace.truevfs.kernel.api.FsDriver;
import global.namespace.truevfs.kernel.api.FsScheme;
import global.namespace.truevfs.kernel.api.sl.FsDriverMapLocator;
import lombok.val;

import java.io.File;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Detects a <em>prospective</em> archive file and declares its file system
 * scheme by mapping its file name extension to an archive driver.
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
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class TArchiveDetector extends FsAbstractCompositeDriver {

    /**
     * This instance recognizes all archive file name extensions for which an
     * archive driver can get located on the class path by the file system
     * driver map locator singleton {@link FsDriverMapLocator#SINGLETON}.
     */
    public static final TArchiveDetector ALL = new TArchiveDetector();

    /**
     * This instance never recognizes any archive files in a path.
     * This can get used as the end of a chain of
     * {@code TArchiveDetector} instances or if archive files
     * shall be treated like regular files rather than (virtual) directories.
     */
    public static final TArchiveDetector NULL = new TArchiveDetector("");

    private static ExtensionSet extensions(final Supplier<Map<FsScheme, ? extends FsDriver>> provider) {
        if (provider instanceof TArchiveDetector) {
            return new ExtensionSet(((TArchiveDetector) provider).extensions);
        } else {
            val drivers = provider.get();
            return drivers
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().isArchiveDriver())
                    .map(entry -> entry.getKey().toString())
                    .collect(Collectors.toCollection(ExtensionSet::new));
        }
    }

    private static Map<FsScheme, Optional<? extends FsDriver>> map(final Object[][] config) {
        val drivers = new TreeMap<FsScheme, Optional<FsDriver>>();
        for (val param : config) {
            val schemes = schemes(param[0]);
            if (schemes.isEmpty()) {
                throw new IllegalArgumentException("No file system schemes given.");
            }
            Object param1 = param[1];
            if (param1 instanceof Class<?>) {
                try {
                    param1 = ((Class<?>) param1).newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new IllegalArgumentException("Cannot instantiate prospective file system driver class.", e);
                }
            }
            val driver = param1 instanceof FsDriver
                    ? Optional.of((FsDriver) param1)
                    : Optional.<FsDriver>empty();
            schemes.forEach(scheme -> drivers.put(scheme, driver));
        }
        return Collections.unmodifiableMap(drivers);
    }

    private static Collection<FsScheme> schemes(final Object o) {
        val set = new TreeSet<FsScheme>();
        if (o instanceof Collection<?>) {
            for (val p : (Collection<?>) o) {
                if (p instanceof FsScheme) {
                    set.add((FsScheme) p);
                } else {
                    new ExtensionSet(p.toString()).stream().map(FsScheme::create).forEach(set::add);
                }
            }
        } else if (o instanceof FsScheme) {
            set.add((FsScheme) o);
        } else {
            new ExtensionSet(o.toString()).stream().map(FsScheme::create).forEach(set::add);
        }
        return set;
    }

    /**
     * The set of extensions recognized by this archive detector.
     * This set is used to filter the registered archive file extensions in
     * {@link #drivers}.
     */
    private final ExtensionSet extensions;

    private final Map<FsScheme, ? extends FsDriver> drivers;

    /**
     * Equivalent to {@link #TArchiveDetector(String, TArchiveDetector)
     * TArchiveDetector(extensions, TArchiveDetector.ALL)}.
     */
    public TArchiveDetector(String extensions) {
        this(extensions, TArchiveDetector.ALL);
    }

    private TArchiveDetector() {
        this(Optional.empty(), FsDriverMapLocator.SINGLETON);
    }

    /**
     * Constructs a new {@code TArchiveDetector} by filtering the given driver provider for all canonical extensions in
     * the {@code extensions} list.
     *
     * @param extensions A list of file name extensions which shall identify prospective archive files.
     * @param detector   the archive detector to filter.
     * @throws IllegalArgumentException If any of the extensions in the list names a extension for which no file system
     *                                  driver is known by the provider.
     * @see ExtensionSet Syntax constraints for extension lists.
     */
    public TArchiveDetector(String extensions, TArchiveDetector detector) {
        this(Optional.of(extensions), detector);
    }

    private TArchiveDetector(final Optional<String> extensions,
                             final Supplier<Map<FsScheme, ? extends FsDriver>> provider) {
        val available = extensions(provider);
        ExtensionSet accepted;
        if (extensions.isPresent()) {
            val e = extensions.get();
            accepted = new ExtensionSet(e);
            if (accepted.retainAll(available)) {
                accepted = new ExtensionSet(e);
                accepted.removeAll(available);
                assert !accepted.isEmpty();
                throw new IllegalArgumentException(
                        "\"" + accepted + "\" (no archive driver installed for these extensions)");
            }
        } else {
            accepted = available;
        }
        this.extensions = accepted;
        this.drivers = (provider instanceof TArchiveDetector)
                ? provider.get()
                : Collections.unmodifiableMap(new TreeMap<>(provider.get()));
    }

    /**
     * Equivalent to {@link #TArchiveDetector(String, Optional, TArchiveDetector)
     * TArchiveDetector(extensions, driver, TArchiveDetector.NULL)}.
     */
    public TArchiveDetector(String extensions, Optional<? extends FsDriver> driver) {
        this(extensions, driver, NULL);
    }

    /**
     * Constructs a new {@code TArchiveDetector} by decorating the configuration of {@code provider} with mappings for
     * all canonical extensions in {@code extensions} to {@code driver}.
     *
     * @param extensions A non-empty list of file name extensions which shall identify
     *                   prospective archive files.
     * @param driver     the optional file system driver to map for the extension list.
     *                   If a file system driver is not present, the mapping for the corresponding file system schemes
     *                   is removed from the resulting detector.
     * @param detector   the archive detector to decorate.
     * @throws IllegalArgumentException if any parameter precondition does not hold.
     * @see ExtensionSet Syntax contraints for extension lists.
     */
    public TArchiveDetector(String extensions, Optional<? extends FsDriver> driver, TArchiveDetector detector) {
        this(new Object[][]{{extensions, driver.orElse(null)}}, detector);
    }

    /**
     * Creates a new {@code TArchiveDetector} by decorating the configuration of {@code provider} with mappings for all
     * entries in {@code config}.
     *
     * @param config   an array of key-value pair arrays.
     *                 The first element of each inner array must either be a
     *                 {@link FsScheme file system scheme}, an object {@code o} which
     *                 can get converted to a set of file name extensions by calling
     *                 {@link ExtensionSet#ExtensionSet(String) new ExtensionSet(o.toString())}
     *                 or a {@link Collection collection} of these.
     *                 The second element of each inner array must either be a
     *                 {@link FsDriver file system driver object}, a
     *                 {@link Class file system driver class} or {@code null}.
     *                 {@code null} may be used to <i>shadow</i> a mapping for an equal
     *                 file system scheme in {@code provider} by removing it from the
     *                 resulting map for this detector.
     * @param detector the archive detector to decorate.
     * @throws NullPointerException     if a required configuration element is
     *                                  {@code null}.
     * @throws IllegalArgumentException if any other parameter precondition
     *                                  does not hold.
     * @see ExtensionSet Syntax contraints for extension lists.
     */
    public TArchiveDetector(Object[][] config, TArchiveDetector detector) {
        this(map(config), detector);
    }

    /**
     * Constructs a new {@code TArchiveDetector} by decorating the given driver provider with mappings for all entries
     * in {@code config}.
     *
     * @param config   a map of file system schemes to optional file system drivers.
     *                 If a file system driver is not present, the mapping for the corresponding file system schemes
     *                 is removed from the resulting detector.
     * @param detector the archive detector to decorate.
     * @throws IllegalArgumentException if any parameter precondition does not hold.
     * @see ExtensionSet Syntax contraints for extension lists.
     */
    public TArchiveDetector(final Map<FsScheme, Optional<? extends FsDriver>> config,
                            final TArchiveDetector detector) {
        val extensions = extensions(detector);
        val available = detector.get();
        val drivers = new TreeMap<FsScheme, FsDriver>(available);
        for (val entry : config.entrySet()) {
            val scheme = entry.getKey();
            val driver = entry.getValue();
            if (driver.isPresent()) {
                extensions.add(scheme.toString());
                drivers.put(scheme, driver.get());
            } else {
                extensions.remove(scheme.toString());
                //drivers.remove(scheme); // keep the driver!
            }
        }
        this.extensions = extensions;
        this.drivers = Collections.unmodifiableMap(drivers);
    }

    /**
     * Returns the <i>canonical extension list</i> for all archive file system
     * schemes recognized by this {@code TArchiveDetector}.
     *
     * @return Either {@code ""} to indicate an empty set or
     * a string of the form {@code "extension[|extension]*"},
     * where {@code extension} is a combination of lower case
     * letters which does <em>not</em> start with a dot.
     * The string never contains empty or duplicated extensions and the
     * extensions are sorted in natural order.
     * @see #TArchiveDetector(String)
     * @see ExtensionSet Syntax constraints for extension lists.
     */
    public String getExtensions() {
        return extensions.toString();
    }

    /**
     * Returns the immutable map of file system drivers.
     * This is equivalent to {@link #get()}.
     *
     * @return the immutable map of file system drivers.
     */
    public Map<FsScheme, ? extends FsDriver> getDrivers() {
        return drivers;
    }

    @Override
    public Map<FsScheme, ? extends FsDriver> get() {
        return drivers;
    }

    /**
     * Detects whether the given {@code path} name identifies a prospective
     * archive file by matching its file name extension against the set of
     * file system schemes in the file system driver map.
     * If a match is found, the file name extension gets converted to a file
     * system scheme and returned.
     * Otherwise, {@code null} is returned.
     *
     * @param path the path name.
     * @return A file system scheme to declare the file system type of the
     * prospective archive file or {@code null} if no archive file name
     * extension has been detected.
     */
    public Optional<FsScheme> scheme(String path) {
        // An archive file name extension may contain a dot (e.g. "tar.gz"), so
        // we can't just look for the last dot in the file name and look up the
        // remainder in the key set of the archive driver map.
        // Likewise, a file name may contain additional dots, so we can't just
        // look for the first dot in it and look up the remainder ...
        path = path.replace('/', File.separatorChar);
        int i = path.lastIndexOf(File.separatorChar) + 1;
        path = path.substring(i);
        val l = path.length();
        for (i = 0; 0 < (i = path.indexOf('.', i) + 1) && i < l; ) {
            final String scheme = path.substring(i);
            if (extensions.contains(scheme)) {
                try {
                    return Optional.of(new FsScheme(scheme)); // TODO: Support 7z
                } catch (URISyntaxException ignored) {
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TArchiveDetector)) {
            return false;
        }
        val that = (TArchiveDetector) obj;
        return this.extensions.equals(that.extensions) && this.drivers.equals(that.drivers);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + extensions.hashCode();
        hash = 59 * hash + drivers.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return String.format("%s[extensions=%s, drivers=%s]", getClass().getName(), extensions, drivers);
    }
}
