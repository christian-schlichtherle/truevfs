/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io;

import de.schlichtherle.truezip.io.archive.driver.registry.GlobalArchiveDriverRegistry;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.util.SuffixSet;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.Map;

/**
 * An {@link ArchiveDetector} which matches file paths against a pattern of
 * archive file suffixes in order to detect prospective archive files and
 * look up their corresponding {@link ArchiveDriver} in its <i>registry</i>.
 * <p>
 * When this class is initialized, it enumerates all instances of the relative
 * path <i>META-INF/services/de.schlichtherle.truezip.io.registry.properties</i>
 * on the class path (this ensures that TrueZIP is compatible with JNLP as used
 * by Java Web Start and can be safely added to the Extension Class Path).
 * <p>
 * These <i>configuration files</i> are processed in arbitrary order
 * to configure the <i>global registry</i> of archive file suffixes and
 * archive drivers.
 * This allows archive drivers to be &quot;plugged in&quot; by simply providing
 * their own configuration file somewhere on the class path.
 * One such instance is located inside the JAR for TrueZIP itself and contains
 * TrueZIP's default configuration (please refer to this file for full details
 * on the syntax).
 * Likewise, client applications may provide their own configuration
 * file somewhere on the class path in order to extend or override the settings
 * configured by TrueZIP and any optional plug-in drivers.
 * <p>
 * Each instance has a <i>local registry</i>. Constructors are provided which
 * allow an instance to:
 * <ol>
 * <li>Filter the set of archive file suffixes in the global registry.
 *     For example, <code><font color="#800080">"tar|zip"</font></code> could
 *     be accepted by the filter in order to recognize only the TAR and ZIP
 *     file formats.</li>
 * <li>Add custom archive file suffixes for supported archive types to the
 *     local registry in order to create <i>pseudo archive types</i>.
 *     For example, <code><font color="#800080">&quot;myapp&quot;</font></code>
 *     could be added as an custom archive file suffix for the JAR file format.</li>
 * <li>Add custom archive file suffixes and archive drivers to the local
 *     registry in order to support new archive types.
 *     For example, the suffix <code><font color="#800080">"7z&quot;</font></code>
 *     could be associated to a custom archive driver which supports the 7z
 *     file format.</li>
 * <li>Put multiple instances in a chain of responsibility:
 *     The first instance which holds a mapping for any given archive file
 *     suffix in its registry determines the archive driver to be used.</li>
 * </ol>
 * <p>
 * Where a constructor expects a suffix list as a parameter, this string must
 * have the form {@code &quot;suffix[|suffix]*&quot;}, where
 * {@code suffix} is a combination of case insensitive letters.
 * Empty or duplicated suffixes and leading dots are silently ignored
 * and {@code null} is interpreted as an empty list.
 * As an example, the parameter {@code &quot;zip|jar&quot;} would cause
 * the archive detector to recognize ZIP and JAR files in a path.
 * The same would be true for {@code &quot;||.ZIP||.JAR||ZIP||JAR||&quot;},
 * but this notation is discouraged because it's not in canonical form
 * (see {@link #getSuffixes}.
 * <p>
 * This implementation is (virtually) immutable and thread safe.
 * <p>
 * Since TrueZIP 6.4, this class is serializable in order to meet the
 * requirements of the {@link de.schlichtherle.truezip.io.File} class.
 * However, it's not recommended to serialize DefaultArchiveDetector instances:
 * Together with the instance, all associated archive drivers are serialized,
 * too, which is pretty inefficient for a single instance.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 * @see ArchiveDetector#NULL
 * @see ArchiveDetector#DEFAULT
 * @see ArchiveDetector#ALL
 */
public class DefaultArchiveDetector
        extends de.schlichtherle.truezip.io.archive.detector.DefaultArchiveDetector
        implements ArchiveDetector {
    private static final long serialVersionUID = 848158760326123684L;

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
    public DefaultArchiveDetector(String list) {
        super(list);
    }

    /**
     * Equivalent to
     * {@link #DefaultArchiveDetector(DefaultArchiveDetector, String, ArchiveDriver)
     * DefaultArchiveDetector(ArchiveDetector.NULL, list, driver)}.
     */
    public DefaultArchiveDetector(String list, ArchiveDriver driver) {
        super(list, driver);
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
            ArchiveDriver driver) {
        super(delegate, list, driver);
    }

    /**
     * Creates a new {@code DefaultArchiveDetector} by
     * decorating the configuration of {@code delegate} with
     * mappings for all entries in {@code config}.
     *
     * @param delegate The {@code DefaultArchiveDetector} which's
     *        configuration is to be virtually inherited.
     * @param config An array of suffix lists and archive driver IDs.
     *        Each key in this map must be a non-null, non-empty archive file
     *        suffix list.
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
     * @see SuffixSet Syntax definition for suffix lists.
     */
    public DefaultArchiveDetector(
            DefaultArchiveDetector delegate,
            Object[] config) {
        super(delegate, config);
    }

    /**
     * Creates a new {@code DefaultArchiveDetector} by
     * decorating the configuration of {@code delegate} with
     * mappings for all entries in {@code config}.
     *
     * @param delegate The {@code DefaultArchiveDetector} which's
     *        configuration is to be virtually inherited.
     * @param config A map of suffix lists and archive drivers.
     *        Each key in this map must be a non-null, non-empty archive file
     *        suffix list.
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
     * @see SuffixSet Syntax definition for suffix lists.
     */
    public DefaultArchiveDetector(
            DefaultArchiveDetector delegate,
            Map config) {
        super(delegate, config);
    }

    public File createFile(java.io.File blueprint) {
        return new File(blueprint, this);
    }


    public File createFile(java.io.File delegate, File innerArchive) {
        return new File(delegate, innerArchive, this);
    }


    public File createFile(
            File blueprint,
            java.io.File delegate,
            File enclArchive) {
        return new File(blueprint, delegate, enclArchive);
    }


    public File createFile(java.io.File parent, String child) {
        return new File(parent, child, this);
    }


    public File createFile(String pathName) {
        return new File(pathName, this);
    }


    public File createFile(String parent, String child) {
        return new File(parent, child, this);
    }


    public File createFile(URI uri) {
        return new File(uri, this);
    }


    public FileInputStream createFileInputStream(java.io.File file)
    throws FileNotFoundException {
        return new FileInputStream(file);
    }


    public FileOutputStream createFileOutputStream(
            java.io.File file,
            boolean append)
    throws FileNotFoundException {
        return new FileOutputStream(file, append);
    }
}
