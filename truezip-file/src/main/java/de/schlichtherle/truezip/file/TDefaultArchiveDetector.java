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

import de.schlichtherle.truezip.fs.sl.FsDriverLocator;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsMountPoint;
import de.schlichtherle.truezip.util.SuffixSet;
import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsDriverProvider;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.FsPath;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.fs.spi.FsDriverService;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.regex.Matcher;
import net.jcip.annotations.Immutable;

/**
 * An archive detector which matches file paths against a pattern of
 * file suffixes in order to detect prospective archive files (i.e. prospective
 * federated file systems) and look up their corresponding file system
 * driver using a file system driver provider.
 * <p>
 * There are basically two types of constructors available in this class:
 * <ol>
 * <li>Constructors which filter the drivers of a given file system driver
 *     provider by a given list of file suffixes.
 *     For example, the drivers known by the provider
 *     {@link FsDriverLocator#SINGLETON} could be filtered by the suffix
 *     list {@code "tar|zip"} in order to recognize only TAR and ZIP files.
 * <li>Constructors which decorate a given file system driver provider with a
 *     given map of file system schemes to file system drivers - whereby a
 *     number of options are available to conveniently specify the map.
 *     This could be used to specify custom archive file suffixes or file
 *     system schemes, i.e. file system drivers.
 *     For example, the suffix list {@code "foo|bar"} could be used to
 *     recognize a custom variant of the JAR file format (you would need to
 *     provide a custom file system driver then, too).
 * </ol>
 * <p>
 * Where a constructor expects a suffix list as a parameter,
 * it must obeye the syntax constraints for {@link SuffixSet}s.
 * As an example, the parameter {@code "zip|jar"} would cause
 * the archive detector to recognize ZIP and JAR files in a path.
 * The same would be true for {@code "||.ZIP||.JAR||ZIP||JAR||"},
 * but this notation is discouraged because it's obviously not in canonical
 * form.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public final class TDefaultArchiveDetector
implements TArchiveDetector, FsDriverProvider {

    /**
     * This instance never recognizes any archive files in a path.
     * This could be used as the end of a chain of
     * {@code TDefaultArchiveDetector} instances or if archive files
     * shall be treated like ordinary files rather than (virtual) directories.
     */
    public static final TDefaultArchiveDetector
            NULL = new TDefaultArchiveDetector("");

    /**
     * This instance recognizes all archive files which are known by the file
     * system driver provider {@link FsDriverLocator#SINGLETON}.
     * The file system schemes are used as the archive file suffixes to
     * recognize.
     */
    public static final TDefaultArchiveDetector
            ALL = new TDefaultArchiveDetector(null);

    private final Map<FsScheme, FsDriver> drivers;

    /**
     * The canonical string respresentation of the set of suffixes recognized
     * by this archive detector.
     * This set is used to filter the registered archive file suffixes in
     * {@link #drivers}.
     */
    private final String suffixes;

    /**
     * The thread local matcher used to match archive file suffixes.
     * This field should be considered final.
     */
    private final ThreadLocalMatcher matcher;

    /**
     * Equivalent to
     * {@link #TDefaultArchiveDetector(FsDriverProvider, String)
     * TDefaultArchiveDetector(FsDriverLocator.SINGLETON, suffixes)}.
     */
    public TDefaultArchiveDetector(@CheckForNull String suffixes) {
        this(FsDriverLocator.SINGLETON, suffixes);
    }

    /**
     * Constructs a new {@code TDefaultArchiveDetector} by filtering the given
     * driver provider for all canonicalized suffixes in the {@code suffixes}
     * list.
     *
     * @param  provider the file system driver provider to filter.
     * @param  suffixes A list of suffixes which shall identify prospective
     *         archive files.
     *         If this is {@code null}, no filtering is applied and all drivers
     *         known by the given provider are available for use with this
     *         archive detector.
     * @throws IllegalArgumentException If any of the suffixes in the list
     *         names a suffix for which no file system driver is known by the
     *         provider.
     * @see    SuffixSet Syntax constraints for suffix lists.
     */
    public TDefaultArchiveDetector( final FsDriverProvider provider,
                                    final @CheckForNull String suffixes) {
        final Map<FsScheme, FsDriver> drivers = provider.get();
        final SuffixSet known = getSuffixes(provider);
        final SuffixSet given;
        if (null != suffixes) {
            given = new SuffixSet(suffixes);
            if (given.retainAll(known)) {
                final SuffixSet unknown = new SuffixSet(suffixes);
                unknown.removeAll(known);
                throw new IllegalArgumentException(
                        "\"" + unknown + "\" (no archive driver installed for these suffixes)");
            }
        } else {
            given = known;
        }
        this.drivers = drivers;
        this.suffixes = given.toString();
        this.matcher = new ThreadLocalMatcher(given.toPattern());
    }

    private static SuffixSet getSuffixes(FsDriverProvider provider) {
        SuffixSet set = new SuffixSet();
        for (Map.Entry<FsScheme, FsDriver> entry
                : provider.get().entrySet()) {
            FsDriver driver = entry.getValue();
            if (null != driver && driver.isFederated())
                set.add(entry.getKey().toString());
        }
        return set;
    }

    /**
     * Equivalent to
     * {@link #TDefaultArchiveDetector(FsDriverProvider, String, FsDriver)
     * TDefaultArchiveDetector(TDefaultArchiveDetector.NULL, suffixes, driver)}.
     */
    public TDefaultArchiveDetector( String suffixes,
                                    @CheckForNull FsDriver driver) {
        this(NULL, suffixes, driver);
    }

    /**
     * Constructs a new {@code TDefaultArchiveDetector} by
     * decorating the configuration of {@code delegate} with
     * mappings for all canonicalized suffixes in {@code suffixes} to
     * {@code driver}.
     * 
     * @param  delegate the file system driver provider to decorate.
     * @param  suffixes a list of suffixes which shall identify prospective
     *         archive files.
     *         This must not be {@code null} and must not be empty.
     * @param  driver the file system driver to map for the suffix list.
     *         {@code null} may be used to <i>shadow</i> a mapping for an equal
     *         file system scheme in {@code delegate} by removing it from the
     *         resulting map for this detector.
     * @throws NullPointerException if a required configuration element is
     *         {@code null}.
     * @throws IllegalArgumentException if any other parameter precondition
     *         does not hold.
     * @see    SuffixSet Syntax contraints for suffix lists.
     */
    public TDefaultArchiveDetector( FsDriverProvider delegate,
                                    String suffixes,
                                    @CheckForNull FsDriver driver) {
        this(delegate, new Object[][] {{ suffixes, driver }});
    }

    /**
     * Creates a new {@code TDefaultArchiveDetector} by
     * decorating the configuration of {@code delegate} with
     * mappings for all entries in {@code config}.
     * 
     * @param  delegate the file system driver provider to decorate.
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
     *         {@code null} may be used to <i>shadow</i> a mapping for an equal
     *         file system scheme in {@code delegate} by removing it from the
     *         resulting map for this detector.
     * @throws NullPointerException if a required configuration element is
     *         {@code null}.
     * @throws IllegalArgumentException if any other parameter precondition
     *         does not hold.
     * @see    SuffixSet Syntax contraints for suffix lists.
     */
    public TDefaultArchiveDetector( FsDriverProvider delegate,
                                    Object[][] config) {
        this(delegate, FsDriverService.newMap(config));
    }

    /**
     * Constructs a new {@code TDefaultArchiveDetector} by
     * decorating the configuration of {@code delegate} with
     * mappings for all entries in {@code config}.
     * 
     * @param  delegate the file system driver provider to decorate.
     * @param  config a map of file system schemes to file system drivers.
     *         {@code null} may be used to <i>shadow</i> a mapping for an equal
     *         file system scheme in {@code delegate} by removing it from the
     *         resulting map for this detector.
     * @throws NullPointerException if a required configuration element is
     *         {@code null}.
     * @throws ClassCastException if a configuration element is of the wrong
     *         type.
     * @throws IllegalArgumentException if any other parameter precondition
     *         does not hold.
     * @see    SuffixSet Syntax contraints for suffix lists.
     */
    public TDefaultArchiveDetector( final FsDriverProvider delegate,
                                    final Map<FsScheme, FsDriver> config) {
        final Map<FsScheme, FsDriver>
                drivers = new HashMap<FsScheme, FsDriver>(delegate.get());
        final SuffixSet suffixes = getSuffixes(delegate);
        for (final Map.Entry<FsScheme, FsDriver> entry : config.entrySet()) {
            final FsScheme scheme = entry.getKey();
            final FsDriver driver = entry.getValue();
            if (null != driver) {
                suffixes.add(scheme.toString());
                drivers.put(scheme, driver);
            } else {
                suffixes.remove(scheme.toString());
                drivers.remove(scheme);
            }
        }
        this.drivers = Collections.unmodifiableMap(drivers);
        this.suffixes = suffixes.toString();
        this.matcher = new ThreadLocalMatcher(suffixes.toPattern());
    }

    @Override
    public Map<FsScheme, FsDriver> get() {
        return drivers;
    }

    @Override
    public @CheckForNull FsScheme getScheme(String path) {
        Matcher m = matcher.reset(path);
        return m.matches()
                ? FsScheme.create(m.group(1).toLowerCase(Locale.ENGLISH))
                : null;
    }

    /** Reserved for unit testing only. */
    @CheckForNull FsDriver getDriver(FsScheme scheme) {
        return drivers.get(scheme);
    }

    @Override
    public FsController<?>
    newController(  final FsModel model,
                    final @CheckForNull FsController<?> parent) {
        assert null == model.getParent()
                ? null == parent
                : model.getParent().equals(parent.getModel());
        final FsMountPoint mountPoint = model.getMountPoint();
        final FsScheme declaredScheme = mountPoint.getScheme();
        final FsPath path = mountPoint.getPath();
        if (null != path) {
            final FsScheme detectedScheme = getScheme(path.getEntryName().getPath()); // may be null!
            if (!declaredScheme.equals(detectedScheme))
                throw new IllegalArgumentException(mountPoint.toString() + " (declared/detected scheme mismatch)");
        }
        final FsDriver driver = drivers.get(declaredScheme);
        if (null == driver)
            throw new ServiceConfigurationError(declaredScheme
                    + "(unknown file system scheme - check run-time class path configuration)");
        return driver.newController(model, parent);
    }

    /**
     * Returns the <i>canonical suffix list</i> for all federated file system
     * types recognized by this {@code TDefaultArchiveDetector}.
     *
     * @return Either {@code ""} to indicate an empty set or
     *         a string of the form {@code "suffix[|suffix]*"},
     *         where {@code suffix} is a combination of lower case
     *         letters which does <em>not</em> start with a dot.
     *         The string never contains empty or duplicated suffixes and the
     *         suffixes are sorted in natural sort order.
     * @see    #TDefaultArchiveDetector(String)
     * @see    SuffixSet Syntax constraints for suffix lists.
     */
    @Override
    public String toString() {
        return suffixes;
    }
}
