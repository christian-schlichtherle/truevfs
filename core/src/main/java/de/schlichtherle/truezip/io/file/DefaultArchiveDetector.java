/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.file;

import de.schlichtherle.truezip.io.archive.driver.registry.GlobalArchiveDriverRegistry;
import de.schlichtherle.truezip.io.archive.driver.registry.ArchiveDriverRegistry;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.SuffixSet;
import de.schlichtherle.truezip.io.filesystem.Scheme;
import de.schlichtherle.truezip.util.regex.ThreadLocalMatcher;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * An {@link ArchiveDetector} which matches file paths against a pattern of
 * archive file suffixes in order to detect prospective archive files and
 * look up their corresponding {@link ArchiveDriver} in its <i>registry</i>.
 * <p>
 * Constructors are provided which allow an instance to:
 * <ol>
 * <li>Filter the set of archive file suffixes in the
 *     {@link GlobalArchiveDriverRegistry}.
 *     For example, {@code "tar|zip"} could be accepted by the filter in order
 *     to recognize only the TAR and ZIP file formats.</li>
 * <li>Add custom archive file suffixes
 *     to a local {@link ArchiveDriverRegistry} in order to support
 *     <i>pseudo archive types</i>.
 *     For example, {@code "mysuffix"} could be added as an custom archive file
 *     suffix for the JAR file format.</li>
 * <li>Add custom archive file suffixes and archive drivers
 *     to a local {@link ArchiveDriverRegistry} in order to support
 *     custom archive types.
 *     For example, the suffix {@code "7z"} could be associated to a custom
 *     archive driver which supports the 7z file format.</li>
 * <li>Put together multiple instances to build a chain of responsibility:
 *     The first instance which holds a mapping for any given archive file
 *     suffix in its registry determines the archive driver to be used.</li>
 * </ol>
 * <p>
 * Where a constructor expects a suffix list as a parameter,
 * it must obeye the syntax definition in {@link SuffixSet}.
 * As an example, the parameter {@code "zip|jar"} would cause
 * the archive detector to recognize ZIP and JAR files in a path.
 * The same would be true for {@code "||.ZIP||.JAR||ZIP||JAR||"},
 * but this notation is discouraged because it's not in canonical form.
 * <p>
 * This implementation is (virtually) immutable and thread safe.
 * <p>
 * This class is serializable in order to meet the requirements of some client
 * classes.
 * However, it's not recommended to serialize instances of this class:
 * Together with the instance, all associated archive drivers are serialized
 * too, which is pretty inefficient.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 * @see ArchiveDetector#NULL
 * @see ArchiveDetector#DEFAULT
 * @see ArchiveDetector#ALL
 */
// TODO: Consider removing support for serialization in this class.
public final class DefaultArchiveDetector
implements ArchiveDetector, Serializable {

    private static final long serialVersionUID = 848158760183179884L;

    /**
     * The local registry for archive file suffixes and archive drivers.
     * This could actually be the global registry
     * ({@link GlobalArchiveDriverRegistry#INSTANCE}), filtered by a custom
     * {@link #list}.
     */
    private final ArchiveDriverRegistry registry;

    /**
     * The canonical suffix list recognized by this archive detector.
     * This list is used to filter the registered archive file suffixes in
     * {@link #registry}.
     */
    private final String list;

    /**
     * The thread local matcher used to match archive file suffixes.
     * This field should be considered final.
     */
    private transient ThreadLocalMatcher matcher; // never transmit this over the wire!

    /**
     * Creates a new {@code DefaultArchiveDetector} by filtering the
     * {@link GlobalArchiveDriverRegistry} for all canonicalized suffixes in
     * the {@code list}.
     * 
     * @param list A list of suffixes which shall identify prospective
     *        archive files.
     *        May be {@code null} or empty.
     * @see SuffixSet Syntax definition for suffix lists.
     * @throws IllegalArgumentException If any of the suffixes in the suffix
     *         list names a suffix for which no {@link ArchiveDriver} is
     *         configured in the {@link GlobalArchiveDriverRegistry}.
     */
    public DefaultArchiveDetector(final String list) {
        registry = GlobalArchiveDriverRegistry.INSTANCE;
        final SuffixSet set = new SuffixSet(list);
        final SuffixSet all = registry.getSuffixes();
        if (set.retainAll(all)) {
            final SuffixSet unknown = new SuffixSet(set);
            unknown.removeAll(all);
            throw new IllegalArgumentException("\"" + unknown + "\" (no archive driver installed for these suffixes)");
        }
        this.list = set.toString();
        matcher = new ThreadLocalMatcher(set.toRegex());
    }

    /**
     * Equivalent to
     * {@link #DefaultArchiveDetector(DefaultArchiveDetector, String, ArchiveDriver)
     * DefaultArchiveDetector(ArchiveDetector.NULL, list, driver)}.
     */
    public DefaultArchiveDetector(String set, ArchiveDriver<?> driver) {
        this(NULL, set, driver);
    }

    /**
     * Creates a new {@code DefaultArchiveDetector} by
     * decorating the configuration of {@code delegate} with
     * mappings for all canonicalized suffixes in {@code list} to
     * {@code driver}.
     * 
     * @param delegate The {@code DefaultArchiveDetector} which's
     *        configuration is to be virtually inherited.
     * @param list A list of suffixes which shall identify prospective
     *        archive files.
     *        Must not be {@code null} and must not be empty.
     * @see SuffixSet Syntax definition for suffix lists.
     * @param driver The archive driver to map for the suffix list.
     *        This must either be an archive driver instance or
     *        {@code null}.
     *        A {@code null} archive driver may be used to shadow a
     *        mapping for the same archive driver in {@code delegate},
     *        effectively removing it.
     * @throws NullPointerException If {@code delegate} or
     *         {@code list} is {@code null}.
     * @throws IllegalArgumentException If any other parameter precondition
     *         does not hold or an illegal keyword is found in the
     *         suffix list.
     */
    public DefaultArchiveDetector(
            DefaultArchiveDetector delegate,
            String list,
            ArchiveDriver<?> driver) {
        this(delegate, new Object[] { list, driver });
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
     *         {@link ArchiveDriver}s.
     * @see    SuffixSet Syntax definition for suffix lists.
     */
    public DefaultArchiveDetector(
            DefaultArchiveDetector delegate,
            Object[] config) {
        this(delegate, toMap(config));
    }

    private static Map<String, Object> toMap(final Object[] config) {
        if (config == null)
            return null;

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
     *         {@link Class}es or {@link ArchiveDriver}s.
     * @see SuffixSet Syntax definition for suffix lists.
     */
    public DefaultArchiveDetector(
            final DefaultArchiveDetector delegate,
            final Map<String, Object> config) {
        registry = new ArchiveDriverRegistry(delegate.registry, config);
        final SuffixSet set = registry.decorate(new SuffixSet(delegate.list)); // may be a subset of delegate.registry.decorate(new SuffixSet())!
        list = set.toString();
        matcher = new ThreadLocalMatcher(set.toRegex());
    }

    private void readObject(final ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        matcher = new ThreadLocalMatcher(new SuffixSet(list).toRegex());
    }

    @Override
    public Scheme getScheme(final String path) {
        final Matcher m = matcher.reset(path);
        return m.matches()
                ? Scheme.create(m.group(1).toLowerCase(Locale.ENGLISH))
                : null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * An archive driver is looked up in the {@link ArchiveDriverRegistry}
     * as follows:
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
     */
    @Override
    public ArchiveDriver<?> getDriver(Scheme type) {
        return registry.getArchiveDriver(type.toString());
    }

    public final @CheckForNull ArchiveDriver<?>
    getDriver(final @NonNull String path) {
        final Scheme scheme = getScheme(path);
        return null == scheme ? null : getDriver(scheme);
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
    public @NonNull String getSuffixes() {
        return list; // canonical form
    }

    /** Equivalent to {@link #getSuffixes()}. */
    @Override public String toString() {
        return list;
    }
}
