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
package de.schlichtherle.truezip.file;

import de.schlichtherle.truezip.fs.FsClassPathDriverProvider;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsDefaultDriver;
import de.schlichtherle.truezip.fs.FsMountPoint;
import de.schlichtherle.truezip.util.SuffixSet;
import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsPath;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.util.ServiceLocator;
import de.schlichtherle.truezip.util.regex.ThreadLocalMatcher;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * An {@link ArchiveDetector} which matches file paths against a pattern of
 * file suffixes in order to detect prospective federated virtual file systems
 * (i.e. archive files) and look up their corresponding file system driver in
 * its map.
 * <p>
 * Constructors are provided which allow an instance to:
 * <ol>
 * <li>Filter the set of archive file suffixes in the global map.
 *     For example, {@code "tar|zip"} could be accepted by the filter in order
 *     to recognize only the TAR and ZIP file formats.</li>
 * <li>Add custom archive file suffixes to a local map in order to support
 *     <i>pseudo archive types</i>.
 *     For example, {@code "mysuffix"} could be added as an custom archive file
 *     suffix for the JAR file format.</li>
 * <li>Add custom archive file suffixes and archive drivers to a local map in
 *     order to support custom archive types.
 *     For example, the suffix {@code "7z"} could be associated to a custom
 *     archive driver which supports the 7z file format.</li>
 * <li>Put together multiple instances to build a chain of responsibility:
 *     The first instance which holds a mapping for any given archive file
 *     suffix in its map determines the archive driver to be used.</li>
 * </ol>
 * <p>
 * Where a constructor expects a suffix list as a parameter,
 * it must obeye the syntax definition in {@link SuffixSet}.
 * As an example, the parameter {@code "zip|jar"} would cause
 * the archive detector to recognize ZIP and JAR files in a path.
 * The same would be true for {@code "||.ZIP||.JAR||ZIP||JAR||"},
 * but this notation is discouraged because it's not in canonical form.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 * @see DefaultArchiveDetector#NULL
 * @see DefaultArchiveDetector#ALL
 */
public final class DefaultArchiveDetector implements ArchiveDetector {

    private static final ServiceLocator serviceLocator
            = new ServiceLocator(DefaultArchiveDetector.class.getClassLoader());

    /**
     * Never recognizes archive files in a path.
     * This can be used as the end of a chain of
     * {@code DefaultArchiveDetector} instances or if archive files
     * shall be treated like ordinary files rather than (virtual) directories.
     */
    public static final DefaultArchiveDetector
            NULL = new DefaultArchiveDetector("");

    /**
     * Recognizes all archive file suffixes registered in the global archive
     * driver registry by the configuration file(s).
     * <p>
     * This requires <a href="{@docRoot}/overview.html#defaults">additional JARs</a>
     * on the run time class path.
     */
    public static final DefaultArchiveDetector
            ALL = new DefaultArchiveDetector();

    private final @NonNull Map<FsScheme, ? extends FsDriver> drivers;

    /**
     * The canonical string respresentation of the set of suffixes recognized
     * by this archive detector.
     * This set is used to filter the registered archive file suffixes in
     * {@link #drivers}.
     */
    private final @NonNull String suffixes;

    /**
     * The thread local matcher used to match archive file suffixes.
     * This field should be considered final.
     */
    private final @NonNull ThreadLocalMatcher matcher;

    public DefaultArchiveDetector() {
        this.drivers = FsClassPathDriverProvider.INSTANCE.getDrivers();
        final SuffixSet set = getSuffixes(drivers);
        this.suffixes = set.toString();
        this.matcher = new ThreadLocalMatcher(set.toPattern());
    }

    private static @NonNull SuffixSet getSuffixes(final Map<FsScheme, ? extends FsDriver> map) {
        SuffixSet set = new SuffixSet();
        for (Map.Entry<FsScheme, ? extends FsDriver> entry : map.entrySet())
            if (null != entry.getValue())
                set.add(entry.getKey().toString());
        return set;
    }

    /**
     * Creates a new {@code DefaultArchiveDetector} by filtering the global map
     * for all canonicalized suffixes in the {@code suffixes} list.
     * 
     * @param suffixes A list of suffixes which shall identify prospective
     *        archive files.
     * @see SuffixSet Syntax definition for suffix lists.
     * @throws IllegalArgumentException If any of the suffixes in the suffix
     *         list names a suffix for which no file system driver is
     *         configured in the global map.
     */
    public DefaultArchiveDetector(final @NonNull String suffixes) {
        this.drivers = FsClassPathDriverProvider.INSTANCE.getDrivers();
        final SuffixSet set = new SuffixSet(suffixes);
        final SuffixSet all = getSuffixes(drivers);
        if (set.retainAll(all)) {
            final SuffixSet unknown = new SuffixSet(suffixes);
            unknown.removeAll(all);
            throw new IllegalArgumentException("\"" + unknown + "\" (no archive driver installed for these suffixes)");
        }
        this.suffixes = set.toString();
        this.matcher = new ThreadLocalMatcher(set.toPattern());
    }

    /**
     * Equivalent to
     * {@link #DefaultArchiveDetector(DefaultArchiveDetector, String, FsDriver)
     * DefaultArchiveDetector(DefaultArchiveDetector.NULL, suffixes, driver)}.
     */
    public DefaultArchiveDetector(  @NonNull String suffixes,
                                    @CheckForNull FsDriver driver) {
        this(NULL, suffixes, driver);
    }

    /**
     * Creates a new {@code DefaultArchiveDetector} by
     * decorating the configuration of {@code delegate} with
     * mappings for all canonicalized suffixes in {@code suffixes} to
     * {@code driver}.
     * 
     * @param delegate The {@code DefaultArchiveDetector} which's
     *        configuration is to be virtually inherited.
     * @param suffixes A list of suffixes which shall identify prospective
     *        archive files.
     *        Must not be {@code null} and must not be empty.
     * @see SuffixSet Syntax definition for suffix lists.
     * @param driver The archive driver to map for the suffix list.
     *        This must either be an archive driver instance or
     *        {@code null}.
     *        A {@code null} archive driver may be used to shadow a
     *        mapping for the same archive driver in {@code delegate},
     *        effectively removing it.
     * @throws IllegalArgumentException If any other parameter precondition
     *         does not hold or an illegal keyword is found in the
     *         suffix list.
     */
    public DefaultArchiveDetector(  @NonNull DefaultArchiveDetector delegate,
                                    @NonNull String suffixes,
                                    @CheckForNull FsDriver driver) {
        this(delegate, new Object[] { suffixes, driver });
    }

    /**
     * Creates a new {@code DefaultArchiveDetector} by
     * decorating the configuration of {@code delegate} with
     * mappings for all entries in {@code config}.
     * 
     * @param  delegate the {@code DefaultArchiveDetector} which's
     *         configuration is to be virtually inherited.
     * @param  config an array of suffix lists and archive driver IDs.
     *         Each key in this map must be a non-null, non-empty archive file
     *         suffix list.
     *         Each value must either be an archive driver instance, an archive
     *         driver class, a string with the fully qualified name name of
     *         an archive driver class, or {@code null}.
     *         A {@code null} archive driver may be used to shadow a
     *         mapping for the same archive driver in {@code delegate},
     *         effectively removing it.
     * @throws NullPointerException if any parameter or configuration element
     *         other than an archive driver is {@code null}.
     * @throws IllegalArgumentException if any other parameter precondition
     *         does not hold or an illegal keyword is found in the
     *         configuration.
     * @throws ClassCastException if the keys are not {@link String}s or the
     * 		   values are not {@link String}s, {@link Class}es or
     *         {@link FsDriver}s.
     * @see    SuffixSet Syntax definition for suffix lists.
     */
    public DefaultArchiveDetector(
            @NonNull DefaultArchiveDetector delegate,
            @NonNull Object[] config) {
        this(delegate, toMap(config));
    }

    private static @NonNull Map<String, Object> toMap(final @NonNull Object[] config) {
        final Map<String, Object> map
                = new LinkedHashMap<String, Object>((int) (config.length / .75f) + 1); // order may be important!
        for (int i = 0, l = config.length; i < l; i++)
            map.put((String) config[i], config[++i]);
        return map;
    }

    /**
     * Creates a new {@code DefaultArchiveDetector} by
     * decorating the configuration of {@code delegate} with
     * mappings for all entries in {@code config}.
     * 
     * @param  delegate the {@code DefaultArchiveDetector} which's
     *         configuration is to be virtually inherited.
     * @param  config a map of suffix lists and archive drivers.
     *         Each key in this map must be a non-null, non-empty archive file
     *         suffix list.
     *         Each value must either be an archive driver instance, an archive
     *         driver class, a string with the fully qualified name name of
     *         an archive driver class, or {@code null}.
     *         A {@code null} archive driver may be used to shadow a
     *         mapping for the same archive driver in {@code delegate},
     *         effectively removing it.
     * @throws NullPointerException if any parameter or configuration element
     *         other than an archive driver is {@code null}.
     * @throws IllegalArgumentException if any other parameter precondition
     *         does not hold or an illegal keyword is found in the
     *         configuration.
     * @throws ClassCastException if the values are not {@link String}s,
     *         {@link Class}es or {@link FsDriver}s.
     * @see SuffixSet Syntax definition for suffix lists.
     */
    public DefaultArchiveDetector(
            final @NonNull DefaultArchiveDetector delegate,
            final @NonNull Map<String, Object> config) {
        final Map<FsScheme, FsDriver> drivers
                = new HashMap<FsScheme, FsDriver>(delegate.drivers);
        final SuffixSet set = new SuffixSet(delegate.suffixes);
        for (final Map.Entry<String, Object> entry : config.entrySet()) {
            final SuffixSet keySet = new SuffixSet(entry.getKey());
            if (keySet.isEmpty())
                throw new IllegalArgumentException("No archive file suffixes!");
            for (final String suffix : keySet) {
                final FsScheme scheme = FsScheme.create(suffix);
                final FsDriver driver = newDriver(entry.getValue());
                if (null != driver) {
                    set.add(suffix);
                    drivers.put(scheme, driver);
                } else {
                    set.remove(suffix);
                    drivers.remove(scheme);
                }
            }
        }
        this.drivers = Collections.unmodifiableMap(drivers);
        this.suffixes = set.toString();
        this.matcher = new ThreadLocalMatcher(set.toPattern());
    }

    @SuppressWarnings("unchecked")
    private static @CheckForNull FsDriver newDriver(@CheckForNull Object driver) {
        try {
            if (driver instanceof String)
                driver = serviceLocator.getClass((String) driver);
            if (driver instanceof Class<?>)
                driver = ((Class<? extends FsDriver>) driver).newInstance();
            return (FsDriver) driver; // may throw ClassCastException
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex); // NOI18N
        }
    }

    @Override
    public FsScheme getScheme(final String path) {
        final Matcher m = matcher.reset(path);
        return m.matches()
                ? FsScheme.create(m.group(1).toLowerCase(Locale.ENGLISH))
                : null;
    }

    /** For unit testing only. */
    FsDriver getDriver(FsScheme scheme) {
        return drivers.get(scheme);
    }

    @Override
    public FsController<?>
    newController(final FsMountPoint mountPoint, final FsController<?> parent) {
        assert null == mountPoint.getParent()
                ? null == parent
                : mountPoint.getParent().equals(parent.getModel().getMountPoint());
        final FsScheme declaredScheme = mountPoint.getScheme();
        final FsPath path = mountPoint.getPath();
        final FsDriver driver;
        if (null != path) {
            final FsScheme detectedScheme = getScheme(path.getEntryName().getPath()); // may be null!
            if (!declaredScheme.equals(detectedScheme))
                throw new IllegalArgumentException(mountPoint.toString() + " (declared/detected scheme mismatch)");
            driver = drivers.get(declaredScheme);
            assert null != driver;
        } else {
            assert "file".equalsIgnoreCase(declaredScheme.toString());
            driver = FsDefaultDriver.ALL;
        }
        return driver.newController(mountPoint, parent); // throwing NPE is OK!
    }

    /**
     * Returns the <i>canonical suffix list</i> detected by this
     * {@code DefaultArchiveDetector}.
     *
     * @return Either {@code ""} to indicate an empty set or
     *         a string of the form {@code "suffix[|suffix]*"},
     *         where {@code suffix} is a combination of lower case
     *         letters which does <em>not</em> start with a dot.
     *         The string never contains empty or duplicated suffixes and the
     *         suffixes are sorted in natural sort order.
     * @see #DefaultArchiveDetector(String)
     * @see SuffixSet Syntax definition for canonical suffix lists.
     */
    @Override
    public String toString() {
        return suffixes;
    }
}
