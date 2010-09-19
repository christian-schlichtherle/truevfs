/*
 * Copyright (C) 2004-2010 Schlichtherle IT Services
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

import de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.io.archive.controller.ArchiveEntryNotFoundException;
import java.util.Collection;
import java.util.Arrays;
import de.schlichtherle.truezip.io.Paths.Splitter;
import de.schlichtherle.truezip.io.archive.controller.ArchiveEntryFalsePositiveException;
import de.schlichtherle.truezip.io.archive.controller.FalsePositiveException;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.archive.controller.ArchiveStatistics;
import de.schlichtherle.truezip.io.archive.controller.ArchiveController;
import de.schlichtherle.truezip.io.archive.controller.ArchiveControllers;
import de.schlichtherle.truezip.io.archive.controller.ArchiveSyncException;
import de.schlichtherle.truezip.io.archive.controller.DefaultArchiveSyncExceptionBuilder;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.Streams;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.Icon;

import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.CLOSE_INPUT_STREAMS;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.CLOSE_OUTPUT_STREAMS;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.UMOUNT;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.WAIT_FOR_INPUT_STREAMS;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.WAIT_FOR_OUTPUT_STREAMS;
import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.ROOT;
import static de.schlichtherle.truezip.io.Files.cutTrailingSeparators;
import static de.schlichtherle.truezip.io.Files.getRealFile;
import static de.schlichtherle.truezip.io.Files.normalize;

/**
 * A drop-in replacement for its subclass which provides transparent
 * read/write access to archive files and their entries as if they were
 * (virtual) directories and files.
 * <p>
 * <b>Warning:</b>The classes in this package access and manipulate archive
 * files as external resources and may cache some of their state in memory
 * and temporary files. Third parties must not concurrently access these
 * archive files unless some precautions have been taken!
 * For more information please refer to the section
 * &quot;<a href="package-summary.html#state">Managing Archive File State</a>&quot;
 * in the package summary.
 *
 * <h4><a name="copy_methods">Copy methods</a></h4>
 * <p>
 * This class provides a bunch of convenient copy methods which work much
 * faster and more reliable than the usual read-write-in-a-loop approach for
 * individual files and its recursive companion for directory trees.
 * These copy methods fall into the following categories:
 * <ol>
 * <li>The (archiveC|c)opy(All)?(To|From) methods (note the regular expression)
 *     simply return a boolean value indicating success or failure.
 *     Though this is suboptimal, this is consistent with most methods in
 *     the super class.
 * <li>The copy(_p)? methods return void and throw an {@code IOException}
 *     on failure.
 *     The exception hierarchy is fine grained enough to let a client
 *     application differentiate between access restrictions, input exceptions
 *     and output exceptions.
 *     The method names have been modelled after the Unix &quot;copy -p&quot;
 *     utility.
 *     None of these methods does recursive copying, however.
 * <li>The cat(To|From) methods return a boolean value. In contrast to the
 *     previous methods, they never close their argument streams, so you
 *     can call them multiple times on the same streams to concatenate data.
 *     Their name is modelled after the Unix command line utility &quot;cat&quot;.
 * <li>Finally, the cat method is the core engine for all these methods.
 *     It performs the asynchronous data transfer from an input stream to an
 *     output stream. When used with properly crafted input and output stream
 *     implementations, it delivers the same performance as the transfer
 *     method in the package {@code java.nio}.
 * </ol>
 * All copy methods use asynchronous I/O, pooled large buffers and pooled
 * threads (if run on JSE 1.5) to achieve best performance.
 *
 * <h4><a name="DDC">Direct Data Copying (DDC)</a></h4>
 * <p>
 * If data is copied from an archive file to another archive file of the
 * same type, some of the copy methods use a feature called <i>Direct Data
 * Copying</i> (DDC) to achieve even better performance:</a>
 * DDC copies the raw data from the source archive entry to the destination
 * archive entry without the need to temporarily reproduce, copy and process
 * the original data again.
 * <p>
 * The benefits of this feature are archive driver specific:
 * In case of ZIP compatible files with compressed entries, it avoids the
 * need to decompress the data from the source entry just to compress it
 * again for the destination entry.
 * In case of TAR compatible files, it avoids the need to create an
 * additional temporary file, but shows no impact otherwise - TAR doesn't
 * support compression.
 *
 * <h4><a name="false_positives">Identifying Archive Paths and False Positives</a></h4>
 * <p>
 * Whenever an archive file suffix is recognized in a path, TrueZIP treats
 * the corresponding file or directory as a <i>prospective archive file</i>.
 * The word &quot;prospective&quot; suggests that just because a file is named
 * <i>archive.zip</i> it isn't necessarily a valid ZIP file.
 * In fact, it could be anything, even a regular directory!
 * <p>
 * Such an invalid archive file is called a <i>false positive</i>, which
 * means a file, special file (a Unix concept) or directory which's path has
 * a configured archive file suffix, but is actually something else.
 * TrueZIP correctly identifies all kinds of false positives and treats them
 * according to what they really are: Regular files, special files or
 * directories.
 * <p>
 * The following table shows how certain methods in this class behave,
 * depending upon a file's path and its <i>true state</i> in the file system:
 * <p>
 * <table border="2" cellpadding="4">
 * <tr>
 *   <th>Path</th>
 *   <th>True State</th>
 *   <th>{@code isArchive()}<sup>1</sup></th>
 *   <th>{@code isDirectory()}</th>
 *   <th>{@code isFile()}</th>
 *   <th>{@code exists()}</th>
 *   <th>{@code length()}<sup>2</sup></th>
 * </tr>
 * <tr>
 *   <td><i>archive.zip</i><sup>3</sup></td>
 *   <td>Valid ZIP file</td>
 *   <td>{@code true}</td>
 *   <td>{@code true}</td>
 *   <td>{@code false}</td>
 *   <td>{@code true}</td>
 *   <td>{@code 0}</td>
 * </tr>
 * <tr>
 *   <td><i>archive.zip</i></td>
 *   <td>False positive: Regular directory</td>
 *   <td>{@code true}</td>
 *   <td>{@code true}</td>
 *   <td>{@code false}</td>
 *   <td>{@code true}</td>
 *   <td><code><i>?</i></code></td>
 * </tr>
 * <tr>
 *   <td><i>archive.zip</i></td>
 *   <td>False positive: Regular file</td>
 *   <td>{@code true}</td>
 *   <td>{@code false}</td>
 *   <td>{@code true}</td>
 *   <td>{@code true}</td>
 *   <td><code><i>?</i></code></td>
 * </tr>
 * <tr>
 *   <td><i>archive.zip</i></td>
 *   <td>False positive: Regular special file</td>
 *   <td>{@code true}</td>
 *   <td>{@code false}</td>
 *   <td>{@code false}</td>
 *   <td>{@code true}</td>
 *   <td><code><i>?</i></code></td>
 * </tr>
 * <tr>
 *   <td><i>archive.zip</i></td>
 *   <td>File or directory does not exist</td>
 *   <td>{@code true}</td>
 *   <td>{@code false}</td>
 *   <td>{@code false}</td>
 *   <td>{@code false}</td>
 *   <td>{@code 0}</td>
 * </tr>
 * <tr>
 *   <td colspan="7">&nbsp;</td>
 * </tr>
 * <tr>
 *   <td><i>archive.tzp</i><sup>4</sup></td>
 *   <td>Valid RAES encrypted ZIP file with valid key (e.g. password)</td>
 *   <td>{@code true}</td>
 *   <td>{@code true}</td>
 *   <td>{@code false}</td>
 *   <td>{@code true}</td>
 *   <td>{@code 0}</td>
 * </tr>
 * <tr>
 *   <td><i>archive.tzp</i></td>
 *   <td>Valid RAES encrypted ZIP file with unknown key<sup>5</sup></td>
 *   <td>{@code true}</td>
 *   <td>{@code false}</td>
 *   <td>{@code false}</td>
 *   <td>{@code true}</td>
 *  <td><code><i>?</i></code></td>
 * </tr>
 * <tr>
 *   <td><i>archive.tzp</i></td>
 *   <td>False positive: Regular directory</td>
 *   <td>{@code true}</td>
 *   <td>{@code true}</td>
 *   <td>{@code false}</td>
 *   <td>{@code true}</td>
 *   <td><code><i>?</i></code></td>
 * </tr>
 * <tr>
 *   <td><i>archive.tzp</i></td>
 *   <td>False positive: Regular file</td>
 *   <td>{@code true}</td>
 *   <td>{@code false}</td>
 *   <td>{@code true}</td>
 *   <td>{@code true}</td>
 *   <td><code><i>?</i></code></td>
 * </tr>
 * <tr>
 *   <td><i>archive.tzp</i></td>
 *   <td>False positive: Regular special file</td>
 *   <td>{@code true}</td>
 *   <td>{@code false}</td>
 *   <td>{@code false}</td>
 *   <td>{@code true}</td>
 *   <td><code><i>?</i></code></td>
 * </tr>
 * <tr>
 *   <td><i>archive.tzp</i></td>
 *   <td>File or directory does not exist</td>
 *   <td>{@code true}</td>
 *   <td>{@code false}</td>
 *   <td>{@code false}</td>
 *   <td>{@code false}</td>
 *   <td>{@code 0}</td>
 * </tr>
 * <tr>
 *   <td colspan="7">&nbsp;</td>
 * </tr>
 * <tr>
 *   <td><i>other</i></td>
 *   <td>Regular directory</td>
 *   <td>{@code false}</td>
 *   <td>{@code true}</td>
 *   <td>{@code false}</td>
 *   <td>{@code true}</td>
 *   <td><i>{@code ?}</i></td>
 * </tr>
 * <tr>
 *   <td><i>other</i></td>
 *   <td>Regular file</td>
 *   <td>{@code false}</td>
 *   <td>{@code false}</td>
 *   <td>{@code true}</td>
 *   <td>{@code true}</td>
 *   <td><i>{@code ?}</i></td>
 * </tr>
 * <tr>
 *   <td><i>other</i></td>
 *   <td>Regular special file</td>
 *   <td>{@code false}</td>
 *   <td>{@code false}</td>
 *   <td>{@code false}</td>
 *   <td>{@code true}</td>
 *   <td><i>{@code ?}</i></td>
 * </tr>
 * <tr>
 *   <td><i>other</i></td>
 *   <td>File or directory does not exist</td>
 *   <td>{@code false}</td>
 *   <td>{@code false}</td>
 *   <td>{@code false}</td>
 *   <td>{@code false}</td>
 *   <td>{@code 0}</td>
 * </tr>
 * </table>
 * <ol>
 * <li>{@link #isArchive} doesn't check the true state of the file - it just
 *     looks at its path: If the path ends with a configured archive file
 *     suffix, {@code isArchive()} always returns {@code true}.
 * <li>{@link #length} always returns {@code 0} if the path denotes a
 *     valid archive file.
 *     Otherwise, the return value of {@code length()} depends on the
 *     platform and file system, which is indicated by <i>{@code ?}</i>.
 *     For regular directories on Windows/NTFS for example, the return value
 *     would be {@code 0}.
 * <li><i>archive.zip</i> is just an example: If TrueZIP is configured to
 *     recognize TAR.GZ files, the same behavior applies to
 *     <i>archive.tar.gz</i>.</li>
 * <li>This assumes that <i>.tzp</i> is configured as an archive file suffix
 *     for RAES encrypted ZIP files.
 *     By default, this is <b>not</b> the case.</li>
 * <li>The methods behave exactly the same for both <i>archive.zip</i> and
 *    <i>archive.tzp</i> with one exception: If the key for a RAES encrypted
 *    ZIP file remains unknown (e.g. because the user cancelled password
 *    prompting), then these methods behave as if the true state of the path
 *    were a special file: Both {@link #isDirectory} and {@link #isFile}
 *    return {@code false}, while {@link #exists} returns
 *    {@code true}.</li>
 * </ol>
 *
 * <h4><a name="miscellaneous">Miscellaneous</a></h4>
 * <ol>
 * <li>Since TrueZIP 6.4, this class is serializable in order to meet the
 *     requirements of its super class.
 *     However, it's not recommended to serialize File instances:
 *     Together with the instance, its archive detector and all associated
 *     archive drivers are serialized, too, which is pretty inefficient for
 *     a single instance.
 *     Serialization might even fail since it's not a general requirement for
 *     the interface implementations to be serializable - although the default
 *     implementations in TrueZIP 6.4 are all serializable.
 *     Instead of serializing File instances, a client application should
 *     serialize paths (which are simply String instances) and leave it up
 *     to the receiver to create a new File instance from it with archive
 *     files recognized by a suitable local archive detector - usually the
 *     {@link #getDefaultArchiveDetector default archive detector}.
 * </ol>
 *
 * @see DefaultArchiveDetector API reference for configuring archive type
 *      recognition
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class File extends java.io.File {

    //
    // Static fields:
    //

    private static final long serialVersionUID = 3617072259051821745L;

    /** The filesystem roots. */
    private static final Set roots = new TreeSet(Arrays.asList(listRoots()));

    /** The prefix of a UNC (a Windows concept). */
    private static final String uncPrefix = separator + separator;

    private static boolean lenient
            = !Boolean.getBoolean(File.class.getPackage().getName() + ".strict");

    private static ArchiveDetector defaultDetector = ArchiveDetector.DEFAULT;

    //
    // Instance fields:
    //

    /**
     * The delegate is used to implement the behaviour of the file system
     * operations in case this instance represents neither an archive file
     * nor an entry in an archive file.
     * If this instance is constructed from another {@code java.io.File}
     * instance, then this field is initialized with that instance.
     * <p>
     * This enables "stacking" of virtual file system implementations and is
     * essential to enable the broken implementation in
     * {@code javax.swing.JFileChooser} to browse archive files.
     */
    private final java.io.File delegate;

    /**
     * @see #getArchiveDetector
     */
    private final ArchiveDetector detector;

    /**
     * This field should be considered final!
     *
     * @see #getInnerArchive
     * @see #readObject
     */
    private File innerArchive;

    /**
     * This field should be considered final!
     *
     * @see #getEnclArchive
     */
    private File enclArchive;

    /**
     * This field should be considered final!
     *
     * @see #getEnclEntryName
     */
    private String enclEntryName;

    /**
     * This refers to the archive controller if and only if this file refers
     * to an archive file, otherwise it's {@code null}.
     * This field should be considered to be {@code final}!
     *
     * @see #readObject
     */
    private transient ArchiveController controller;

    //
    // Constructor and helper methods:
    //

    /**
     * Copy constructor.
     * Equivalent to {@link #File(java.io.File, ArchiveDetector)
     * File(template, getDefaultArchiveDetector())}.
     */
    public File(java.io.File template) {
        this(template, defaultDetector);
    }

    /**
     * Constructs a new {@code File} instance which may use the given
     * {@link ArchiveDetector} to detect any archive files in its path.
     * 
     * @param template The file to use as a template. If this is an instance
     *        of this class, its fields are copied and the
     *        {@code detector} parameter is ignored.
     * @param detector The object used to detect any archive files in the path.
     *        This parameter is ignored if {@code template} is an
     *        instance of this class.
     *        Otherwise, it must not be {@code null}.
     * @see <a href="package-summary.html#third_parties">Third Party
     *      Access using different Archive Detectors</a>
     * @throws NullPointerException If a required parameter is {@code null}.
     */
    public File(
            final java.io.File template,
            final ArchiveDetector detector) {
        super(template.getPath());

        if (template instanceof File) {
            final File file = (File) template;
            this.delegate = file.delegate;
            this.detector = file.detector;
            this.enclArchive = file.enclArchive;
            this.enclEntryName = file.enclEntryName;
            this.innerArchive = file.isArchive() ? this : file.innerArchive;
            this.controller = file.controller;
        } else {
            this.delegate = template;
            this.detector = detector;
            init((File) null);
        }

        assert invariants();
    }

    /**
     * Equivalent to {@link #File(String, ArchiveDetector)
     * File(path, getDefaultArchiveDetector())}.
     */
    public File(String path) {
        this(path, defaultDetector);
    }

    /**
     * Constructs a new {@code File} instance which uses the given
     * {@link ArchiveDetector} to detect any archive files in its path.
     *
     * @param path The path of the file.
     * @param detector The object used to detect any archive files in the path.
     * @see <a href="package-summary.html#third_parties">Third Party
     *      Access using different Archive Detectors</a>
     */
    public File(
            final String path,
            final ArchiveDetector detector) {
        super(path);

        delegate = new java.io.File(path);
        this.detector = detector;
        init((File) null);

        assert invariants();
    }

    /**
     * Equivalent to {@link #File(String, String, ArchiveDetector)
     * File(parent, child, getDefaultArchiveDetector())}.
     */
    public File(String parent, String child) {
        this(parent, child, defaultDetector);
    }

    /**
     * Constructs a new {@code File} instance which uses the given
     * {@link ArchiveDetector} to detect any archive files in its path.
     *
     * @param parent The parent path as a {@link String}.
     * @param detector The object used to detect any archive files in the path.
     * @param child The child path as a {@link String}.
     * @see <a href="package-summary.html#third_parties">Third Party
     *      Access using different Archive Detectors</a>
     */
    public File(
            final String parent,
            final String child,
            final ArchiveDetector detector) {
        super(parent, child);

        delegate = new java.io.File(parent, child);
        this.detector = detector;
        init((File) null);

        assert invariants();
    }

    /**
     * Equivalent to {@link #File(java.io.File, String, ArchiveDetector)
     * File(parent, child, null)}.
     *
     * @param parent The parent directory as a {@code File} instance.
     *        If this parameter is an instance of this class, its
     *        {@code ArchiveDetector} is used to detect any archive files
     *        in the path of this {@code File} instance.
     *        Otherwise, the {@link #getDefaultArchiveDetector()} is used.
     *        This is used in order to make this {@code File} instance
     *        behave as if it had been created by one of the {@link #listFiles}
     *        methods called on {@code parent} instead.
     * @param child The child path as a {@link String}.
     */
    public File(java.io.File parent, String child) {
        this(parent, child, null);
    }

    /**
     * Constructs a new {@code File} instance which uses the given
     * {@link ArchiveDetector} to detect any archive files in its path
     * and configure their parameters.
     *
     * @param parent The parent directory as a {@code File} instance.
     * @param child The child path as a {@link String}.
     * @param detector The object used to detect any archive files in the path.
     *        If this is {@code null} and {@code parent} is an
     *        instance of this class, the archive detector is inherited from
     *        this instance.
     *        If this is {@code null} and {@code parent} is
     *        <em>not</em> an instance of this class, the archive detector
     *        returned by {@link #getDefaultArchiveDetector()} is used.
     * @throws NullPointerException If {@code child} is {@code null}.
     * @see <a href="package-summary.html#third_parties">Third Party
     *      Access using different Archive Detectors</a>
     */
    public File(
            final java.io.File parent,
            final String child,
            final ArchiveDetector detector) {
        super(parent, child);

        delegate = new java.io.File(parent, child);
        if (parent instanceof File) {
            final File smartParent = (File) parent;
            this.detector = detector != null ? detector : smartParent.detector;
            init(smartParent);
        } else {
            this.detector = detector != null ? detector : defaultDetector;
            init((File) null);
        }

        assert invariants();
    }

    /**
     * Constructs a new {@code File} instance from the given
     * {@code uri}. This method behaves similar to
     * {@link java.io.File#File(URI) new java.io.File(uri)} with the following
     * amendment:
     * If the URI matches the pattern
     * <code>(jar:)*file:(<i>path</i>!/)*<i>entry</i></code>, then the
     * constructed file object treats the URI like a (possibly ZIPped) file.
     * <p>
     * For example, in a Java application which is running from a JAR in the
     * local file system you could use this constructor to arbitrarily access
     * (and modify) all entries in the JAR file from which the application is
     * currently running by using the following simple method:
     * <pre>
     * public File getResourceAsFile(String resource) {
     *   URL url = getClass().getResource(resource);
     *   try {
     *     return new File(new URI(url.toExternalForm()));
     *   } catch (Exception notAJaredFileURI) {
     *     return null;
     *   }
     * }
     * </pre>
     * The newly created {@code File} instance uses
     * {@link ArchiveDetector#ALL} as its {@code ArchiveDetector}.
     *
     * @param uri an absolute, hierarchical URI with a scheme equal to
     *        {@code file} or {@code jar}, a non-empty path component,
     *        and undefined authority, query, and fragment components.
     * @throws NullPointerException if {@code uri} is {@code null}.
     * @throws IllegalArgumentException if the preconditions on the
     *         parameter {@code uri} do not hold.
     */
    public File(URI uri) {
        this(uri, ArchiveDetector.ALL);
    }

    // Unfortunately, this constructor has a significant overhead as the jar:
    // schemes need to be processed twice, first before initializing the super
    // class and second when initializing this sub class.
    File(   final URI uri,
            final ArchiveDetector detector) {
        super(unjarFileURI(uri));

        delegate = new java.io.File(super.getPath());
        this.detector = detector;
        init(uri);

        assert invariants();
    }

    /**
     * Converts a (jar:)*file: URI to a plain file: URI or returns the
     * provided URI again if it doesn't match this pattern.
     */
    private static URI unjarFileURI(final URI uri) {
        try {
            final String scheme = uri.getScheme();
            final String ssp = normalize(uri.getSchemeSpecificPart(), '/');
            return unjarFileURI0(new URI(scheme, ssp, null));
        } catch (URISyntaxException ignored) {
            // Ignore any exception with possibly only a subpart of the
            // original URI.
        }
        throw new IllegalArgumentException(uri + ": Not a valid (possibly jared) file URI!");
    }

    private static URI unjarFileURI0(final URI uri)
    throws URISyntaxException {
        final String scheme = uri.getScheme();
        if ("jar".equalsIgnoreCase(scheme)) {
            final String rssp = uri.getRawSchemeSpecificPart();
            final int i;
            if (rssp.endsWith("!"))
                i = rssp.length() - 1;
            else
                i = rssp.lastIndexOf("!/");

            if (i <= 0)
                return unjarFileURI(new URI(rssp)); // ignore redundant jar: scheme

            final URI subURI = new URI(
                    rssp.substring(0, i) + rssp.substring(i + 1)); // cut out '!'
            final String subScheme = subURI.getScheme();
            if ("jar".equalsIgnoreCase(subScheme)) {
                final URI processedSubURI = unjarFileURI0(subURI);
                if (processedSubURI != subURI)
                    return processedSubURI;
                // No match, e.g. "jar:jar:http://host/dir!/dir!/file".
            } else if ("file".equalsIgnoreCase(subScheme)) {
                return subURI; // e.g. "file:///usr/bin"
            }
        } else if ("file".equalsIgnoreCase(scheme)) {
            return uri;
        }
        throw new URISyntaxException(uri.toString(), "Not a valid (possibly jared) file URI!");
    }

    /**
     * @deprecated This constructor is not intended for public use - do <em>not</em> use it!
     * @see FileFactory
     */
    @SuppressWarnings("LeakingThisInConstructor")
    public File(
            final java.io.File delegate,
            final File innerArchive,
            final ArchiveDetector detector) {
        super(delegate.getPath());

        assert parameters(delegate, innerArchive, detector);

        this.delegate = delegate;

        final String path = delegate.getPath();
        if (innerArchive != null) {
            final int innerArchivePathLength
                    = innerArchive.getPath().length();
            if (path.length() == innerArchivePathLength) {
                this.detector = innerArchive.detector;
                this.innerArchive = this;
                this.enclArchive = innerArchive.enclArchive;
                this.enclEntryName = innerArchive.enclEntryName;
                initController();
            } else {
                this.detector = detector;
                this.innerArchive = this.enclArchive = innerArchive;
                this.enclEntryName
                        = path.substring(innerArchivePathLength + 1) // cut off leading separatorChar
                        .replace(separatorChar, ArchiveEntry.SEPARATOR_CHAR);
            }
        } else {
            this.detector = detector;
        }

        assert invariants();
    }

    private void initController() {
        final java.io.File target = getRealFile(delegate);
        this.controller = ArchiveControllers.get(
                target.toURI(),
                enclArchive == null ? null : enclArchive.getArchiveController().getMountPoint(),
                detector.getArchiveDriver(target.getPath()));
    }

    /**
     * This is called by some private constructors if and only if assertions
     * are enabled to assert that their parameters are valid.
     * If assertions are disabled, the call to this method is thrown away by
     * the HotSpot compiler, so there is no performance penalty.
     */
    private static boolean parameters(
            final java.io.File delegate,
            final File innerArchive,
            final ArchiveDetector detector)
    throws AssertionError {
        assert delegate != null;
        assert !(delegate instanceof File);
        if (innerArchive != null) {
            assert innerArchive.isArchive();
            assert contains(innerArchive.getPath(), delegate.getPath());
        }
        assert detector != null;

        return true;
    }

    /**
     * @deprecated This constructor is not intended for public use - do <em>not</em> use it!
     * @see FileFactory
     */
    public File(
            final File template,
            final java.io.File delegate,
            final File enclArchive) {
        super(delegate.getPath());

        assert parameters(template, delegate, enclArchive);

        this.delegate = delegate;
        this.detector = template.detector;
        this.enclArchive = enclArchive;
        this.enclEntryName = template.enclEntryName;
        this.innerArchive = template.isArchive() ? this : enclArchive;
        this.controller = template.controller;

        assert invariants();
    }

    /**
     * This is called by some private constructors if and only if assertions
     * are enabled to assert that their parameters are valid.
     * If assertions are disabled, the call to this method is thrown away by
     * the HotSpot compiler, so there is no performance penalty.
     */
    private static boolean parameters(
            final File template,
            final java.io.File delegate,
            final File enclArchive)
    throws AssertionError {
        assert delegate != null;
        assert !(delegate instanceof File);
        assert template != null;

        String delegatePath = delegate.getPath();
        final java.io.File normalizedTemplate = normalize(template);
        String normalizedTemplatePath = normalizedTemplate.getPath();
        String normalizedTemplateBase = normalizedTemplate.getName();
        // Windows and MacOS are case preserving, however UNIX is case
        // sensitive. If we meet an unknown platform, we assume that it is
        // case preserving, which means that two paths are considered
        // equal if they differ by case only.
        // In the context of this constructor, this implements a liberal
        // in-dubio-pro-reo parameter check.
        if (separatorChar != '/') {
            delegatePath = delegatePath.toLowerCase();
            normalizedTemplatePath = normalizedTemplatePath.toLowerCase();
            normalizedTemplateBase = normalizedTemplateBase.toLowerCase();
        }
        if (!".".equals(normalizedTemplateBase)
            && !"..".equals(normalizedTemplateBase)
            && !normalizedTemplatePath.startsWith("." + separator)
            && !normalizedTemplatePath.startsWith(".." + separator)) {
            assert delegatePath.endsWith(normalizedTemplatePath)
                    : "delegate and template must identify the same file or directory!";
            if (enclArchive != null) {
                assert enclArchive.isArchive();
                assert enclArchive.isParentOf(delegate);
            }
        }

        return true;
    }

    /**
     * Initialize this file object by scanning its path for archive
     * files, using the given {@code ancestor} file (i.e. a direct or
     * indirect parent file) if any.
     * {@code delegate} and {@code detector} must already be initialized!
     * Must not be called to re-initialize this object!
     */
    private void init(final File ancestor) {
        final String path = super.getPath();
        assert ancestor == null || path.startsWith(ancestor.getPath());
        assert delegate.getPath().equals(path);
        assert detector != null;

        final StringBuilder enclEntryNameBuf = new StringBuilder(path.length());
        init(ancestor, detector, 0, path, enclEntryNameBuf, new Splitter(separatorChar));
        enclEntryName = enclEntryNameBuf.length() > 0 ? enclEntryNameBuf.toString() : null;

        if (innerArchive == this) {
            // controller initialization has been deferred until now in
            // order to provide the ArchiveController with an otherwise fully
            // initialized object.
            initController();
        }
    }

    private void init(
            File ancestor,
            ArchiveDetector detector,
            int skip,
            final String path,
            final StringBuilder enclEntryNameBuf,
            final Splitter splitter) {
        if (path == null) {
            assert enclArchive == null;
            enclEntryNameBuf.setLength(0);
            return;
        }

        splitter.split(path);
        final String parent = splitter.getParentPath();
        final String base = splitter.getBaseName();

        if (base.length() == 0 || ".".equals(base)) {
            // Fall through.
        } else if ("..".equals(base)) {
            skip++;
        } else if (skip > 0) {
            skip--;
        } else {
            if (ancestor != null) {
                final int pathLen = path.length();
                final int ancestorPathLen = ancestor.getPath().length();
                if (pathLen == ancestorPathLen) {
                    // Found ancestor: Process it and stop.
                    // The following assertion is wrong: enclEntryNameBuf may
                    // indeed be null if the full path ends with just
                    // a single dot after the last separator, i.e. the base
                    // name is ".", indicating the current directory.
                    // assert enclEntryNameBuf.getLength() > 0;
                    enclArchive = ancestor.innerArchive;
                    if (!ancestor.isArchive()) {
                        if (ancestor.isEntry()) {
                            if (enclEntryNameBuf.length() > 0) {
                                enclEntryNameBuf.insert(0, '/');
                                enclEntryNameBuf.insert(0, ancestor.enclEntryName);
                            } else { // TODO: Simplify this!
                                // Example: new File(new File(new File("archive.zip"), "entry"), ".")
                                // with ArchiveDetector.DEFAULT.
                                assert enclArchive == ancestor.enclArchive;
                                enclEntryNameBuf.append(ancestor.enclEntryName);
                            }
                        } else {
                            assert enclArchive == null;
                            enclEntryNameBuf.setLength(0);
                        }
                    } else if (enclEntryNameBuf.length() <= 0) { // TODO: Simplify this!
                        // Example: new File(new File("archive.zip"), ".")
                        // with ArchiveDetector.DEFAULT.
                        assert enclArchive == ancestor;
                        innerArchive = this;
                        enclArchive = ancestor.enclArchive;
                        if (ancestor.enclEntryName != null)
                            enclEntryNameBuf.append(ancestor.enclEntryName);
                    }
                    if (innerArchive != this)
                        innerArchive = enclArchive;
                    return;
                } else if (pathLen < ancestorPathLen) {
                    detector = ancestor.detector;
                    ancestor = ancestor.enclArchive;
                }
            }

            final boolean isArchive = detector.getArchiveDriver(path) != null;
            if (enclEntryNameBuf.length() > 0) {
                if (isArchive) {
                    enclArchive = detector.createFile(path); // use the same detector for the parent directory
                    if (innerArchive != this)
                        innerArchive = enclArchive;
                    return;
                }
                enclEntryNameBuf.insert(0, '/');
                enclEntryNameBuf.insert(0, base);
            } else {
                if (isArchive)
                    innerArchive = this;
                enclEntryNameBuf.append(base);
            }
        }

        init(ancestor, detector, skip, parent, enclEntryNameBuf, splitter);
    }

    /**
     * Uses the given (jar:)*file: URI to initialize this file object.
     * Note that we already know that the provided URI matches this pattern!
     * {@code entry} and {@code detector} must already be
     * initialized!
     * Must not be called to re-initialize this object!
     */
    private void init(final URI uri) {
        assert uri != null;
        assert delegate.getPath().equals(super.getPath());
        assert detector != null;

        init(uri, 0,
                cutTrailingSeparators(uri.getSchemeSpecificPart(), '/'),
                new Splitter('/'));

        if (innerArchive == this) {
            // controller init has been deferred until now in
            // order to provide the ArchiveController with a fully
            // initialized object.
            initController();
        }
    }

    /**
     * TODO: Provide a means to detect other archive schemes, not only
     * {@code "jar:"}.
     */
    private void init(
            URI uri,
            int skip,
            final String path,
            final Splitter splitter) {
        String scheme = uri.getScheme();
        if (path == null || !"jar".equalsIgnoreCase(scheme)) {
            assert enclArchive == null;
            enclEntryName = null;
            return;
        }
        splitter.split(path);
        String parent = splitter.getParentPath();
        final String base = splitter.getBaseName();
        if (base.length() == 0 || ".".equals(base)) {
            // Fall through.
        } else if ("..".equals(base)) {
            skip++;
        } else if (skip > 0) {
            skip--;
        } else {
            final int baseEnd = base.length() - 1;
            final boolean isArchive = base.charAt(baseEnd) == '!';
            if (enclEntryName != null) {
                if (isArchive) {
                    enclArchive = detector.createFile(newURI(scheme, path)); // use the same detector for the parent directory
                    if (innerArchive != this) {
                        innerArchive = enclArchive;
                    }
                    return;
                }
                enclEntryName = base + "/" + enclEntryName;
            } else {
                if (isArchive) {
                    innerArchive = this;
                    int i = parent.indexOf(':');
                    assert i >= 0;
                    scheme = parent.substring(0, i);
                    assert scheme.matches("[a-zA-Z]+");
                    if (i == parent.length() - 1) // scheme only?
                        return;
                    uri = newURI(parent.substring(0, i), parent.substring(i + 1));
                    enclEntryName = base.substring(0, baseEnd); // cut off trailing '!'!
                    parent = uri.getSchemeSpecificPart();
                } else {
                    enclEntryName = base;
                }
            }
        }
        init(uri, skip, parent, splitter);
    }

    /**
     * Creates a URI from a scheme and a scheme specific part.
     * Note that the scheme specific part may contain whitespace.
     */
    private static URI newURI(String scheme, String ssp)
    throws IllegalArgumentException {
        try {
            return new URI(scheme, ssp, null);
        } catch (URISyntaxException syntaxError) {
            throw new IllegalArgumentException(syntaxError);
        }
    }

    /**
     * Postfixes the instance after its default deserialization.
     *
     * @throws InvalidObjectException If the instance invariants are not met.
     */
    private void readObject(final ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        if (innerArchive == this) {
            assert controller == null;      // transient!
            initController();               // postfix!
        }

        try {
            invariants();
        } catch (AssertionError ex) {
            throw (InvalidObjectException) new InvalidObjectException(ex.toString()).initCause(ex);
        }
    }

    /**
     * Checks the invariants of this class and throws an AssertionError if
     * any is violated even if assertion checking is disabled.
     * <p>
     * The constructors call this method like this:
     * <pre>{@code assert invariants(); }</pre>
     * This calls the method if and only if assertions are enabled in order
     * to assert that the instance invariants are properly obeyed.
     * If assertions are disabled, the call to this method is thrown away by
     * the HotSpot compiler, so there is no performance penalty.
     * <p>
     * When deserializing however, this method is called regardless of the
     * assertion status. On error, the {@link AssertionError} is wrapped
     * in an {@link InvalidObjectException} and thrown instead.
     *
     * @throws AssertionError If any invariant is violated even if assertions
     *         are disabled.
     * @return {@code true}
     */
    private boolean invariants() {
        if (delegate == null)
            throw new AssertionError();
        if (delegate instanceof File)
            throw new AssertionError();
        if (!delegate.getPath().equals(super.getPath()))
            throw new AssertionError();
        if (detector == null)
            throw new AssertionError();
        if ((innerArchive != null) != (getInnerEntryName() != null))
            throw new AssertionError();
        if ((enclArchive != null) != (enclEntryName != null))
            throw new AssertionError();
        if (enclArchive == this)
            throw new AssertionError();
        if (!((innerArchive == this && controller != null)
                ^ (innerArchive == enclArchive && controller == null)))
            throw new AssertionError();
        if (!(enclArchive == null
                || contains(enclArchive.getPath(), delegate.getParentFile().getPath())
                    && enclEntryName.length() > 0
                    && (separatorChar == '/'
                        || enclEntryName.indexOf(separatorChar) == -1)))
            throw new AssertionError();

        return true;
    }

    //
    // Methods:
    //

    /**
     * Writes all changes to the contents of all target archive files to the
     * underlying file system.
     * This will reset the state of the respective archive controllers.
     * This method is thread-safe.
     *
     * @throws ArchiveWarningException If the configuration uses the
     *         {@link DefaultArchiveSyncExceptionBuilder} and <em>only</em>
     *         warning conditions occured throughout the course of this method.
     *         This implies that the respective archive file has been updated
     *         with constraints, such as a failure to map the last modification
     *         time of the archive file to the last modification time of its
     *         implicit root directory.
     * @throws ArchiveWarningException If the configuration uses the
     *         {@link DefaultArchiveSyncExceptionBuilder} and any error
     *         condition occured throughout the course of this method.
     *         This implies loss of data!
     * @throws NullPointerException If {@code config} is {@code null}.
     * @throws IllegalArgumentException If the configuration property
     *         {@code closeInputStreams} is {@code false} and
     *         {@code closeOutputStreams} is {@code true}.
     * @see <a href="package-summary.html#state">Managing Archive File State</a>
     */
    public static void sync(BitField<SyncOption> options)
    throws ArchiveSyncException {
        ArchiveControllers.sync(
                null, options, new DefaultArchiveSyncExceptionBuilder());
    }

    /**
     * Equivalent to {@code
        sync(BitField.of(UMOUNT, CLOSE_INPUT_STREAMS, CLOSE_OUTPUT_STREAMS));
     * }.
     *
     * @see #sync(BitField)
     */
    public static void umount()
    throws ArchiveSyncException {
        sync(BitField.of(UMOUNT, CLOSE_INPUT_STREAMS, CLOSE_OUTPUT_STREAMS));
    }

    /**
     * Equivalent to {@code
        sync(   BitField.of(UMOUNT)
                .set(CLOSE_INPUT_STREAMS, closeStreams)
                .set(CLOSE_OUTPUT_STREAMS, closeStreams));
     * }.
     *
     * @see #sync(BitField)
     */
    public static void umount(boolean closeStreams)
    throws ArchiveSyncException {
        sync(   BitField.of(UMOUNT)
                .set(CLOSE_INPUT_STREAMS, closeStreams)
                .set(CLOSE_OUTPUT_STREAMS, closeStreams));
    }

    /**
     * Equivalent to {@code
        sync(   BitField.of(UMOUNT)
                .set(WAIT_FOR_INPUT_STREAMS, waitForInputStreams)
                .set(CLOSE_INPUT_STREAMS, closeInputStreams)
                .set(WAIT_FOR_OUTPUT_STREAMS, waitForOutputStreams)
                .set(CLOSE_OUTPUT_STREAMS, closeOutputStreams));
     * }.
     *
     * @see #sync(BitField)
     */
    public static void umount(
            boolean waitForInputStreams, boolean closeInputStreams,
            boolean waitForOutputStreams, boolean closeOutputStreams)
    throws ArchiveSyncException {
        sync(   BitField.of(UMOUNT)
                .set(WAIT_FOR_INPUT_STREAMS, waitForInputStreams)
                .set(CLOSE_INPUT_STREAMS, closeInputStreams)
                .set(WAIT_FOR_OUTPUT_STREAMS, waitForOutputStreams)
                .set(CLOSE_OUTPUT_STREAMS, closeOutputStreams));
    }

    /**
     * Similar to {@link #sync(BitField) sync(options)},
     * but will only update the given {@code archive} and all its enclosed
     * (nested) archives.
     * <p>
     * If a client application needs to unmount an individual archive file,
     * the following idiom can be used:
     * <pre>{@code
     * if (file.isArchive() && file.getEnclArchive() == null) // filter top level archive
     *   if (file.isDirectory()) // ignore false positives
     *     File.umount(File); // update archive and all enclosed archives
     * }</pre>
     * Again, this will also unmount all archive files which are located
     * within the archive file referred to by the {@code file} instance.
     *
     * @param archive A top level archive file.
     * @throws NullPointerException If {@code archive} is {@code null}.
     * @throws IllegalArgumentException If {@code archive} is not an
     *         archive or is enclosed in another archive (is not top level).
     * @see #sync(BitField)
     */
    public static void sync(
            final File archive,
            final BitField<SyncOption> options)
    throws ArchiveSyncException {
        if (!archive.isArchive())
            throw new IllegalArgumentException(archive.getPath() + " (not an archive)");
        if (archive.getEnclArchive() != null)
            throw new IllegalArgumentException(archive.getPath() + " (not a top level archive)");
        ArchiveControllers.sync(
                archive.getCanOrAbsFile().toURI(),
                options,
                new DefaultArchiveSyncExceptionBuilder());
    }

    /**
     * Equivalent to {@code
        sync(   archive,
                BitField.of(UMOUNT, CLOSE_INPUT_STREAMS, CLOSE_OUTPUT_STREAMS));
     * }.
     *
     * @see #sync(File, BitField)
     */
    public static void umount(File archive)
    throws ArchiveSyncException {
        sync(   archive,
                BitField.of(UMOUNT, CLOSE_INPUT_STREAMS, CLOSE_OUTPUT_STREAMS));
    }

    /**
     * Equivalent to {@code
        sync(   archive,
                BitField.of(UMOUNT)
                .set(CLOSE_INPUT_STREAMS, closeStreams)
                .set(CLOSE_OUTPUT_STREAMS, closeStreams));
     * }.
     *
     * @see #sync(File, BitField)
     */
    public static void umount(File archive, boolean closeStreams)
    throws ArchiveSyncException {
        sync(   archive,
                BitField.of(UMOUNT)
                .set(CLOSE_INPUT_STREAMS, closeStreams)
                .set(CLOSE_OUTPUT_STREAMS, closeStreams));
    }

    /**
     * Equivalent to {@code
        sync(   archive,
                BitField.of(UMOUNT)
                .set(WAIT_FOR_INPUT_STREAMS, waitForInputStreams)
                .set(CLOSE_INPUT_STREAMS, closeInputStreams)
                .set(WAIT_FOR_OUTPUT_STREAMS, waitForOutputStreams)
                .set(CLOSE_OUTPUT_STREAMS, closeOutputStreams));
     * }.
     *
     * @see #sync(File, BitField)
     */
    public static void umount(File archive,
            boolean waitForInputStreams, boolean closeInputStreams,
            boolean waitForOutputStreams, boolean closeOutputStreams)
    throws ArchiveSyncException {
        sync(   archive,
                BitField.of(UMOUNT)
                .set(WAIT_FOR_INPUT_STREAMS, waitForInputStreams)
                .set(CLOSE_INPUT_STREAMS, closeInputStreams)
                .set(WAIT_FOR_OUTPUT_STREAMS, waitForOutputStreams)
                .set(CLOSE_OUTPUT_STREAMS, closeOutputStreams));
    }

    /**
     * Equivalent to {@code
        sync(BitField.of(CLOSE_INPUT_STREAMS, CLOSE_OUTPUT_STREAMS));
     * }.
     *
     * @see #sync(BitField)
     */
    public static void update()
    throws ArchiveSyncException {
        sync(BitField.of(CLOSE_INPUT_STREAMS, CLOSE_OUTPUT_STREAMS));
    }

    /**
     * Equivalent to {@code
        sync(   BitField.noneOf(ArchiveSyncOption.class)
                .set(CLOSE_INPUT_STREAMS, closeStreams)
                .set(CLOSE_OUTPUT_STREAMS, closeStreams));
     * }.
     *
     * @see #sync(BitField)
     */
    public static void update(boolean closeStreams)
    throws ArchiveSyncException {
        sync(   BitField.noneOf(SyncOption.class)
                .set(CLOSE_INPUT_STREAMS, closeStreams)
                .set(CLOSE_OUTPUT_STREAMS, closeStreams));
    }

    /**
     * Equivalent to {@code
        sync(   BitField.noneOf(ArchiveSyncOption.class)
                .set(WAIT_FOR_INPUT_STREAMS, waitForInputStreams)
                .set(CLOSE_INPUT_STREAMS, closeInputStreams)
                .set(WAIT_FOR_OUTPUT_STREAMS, waitForOutputStreams)
                .set(CLOSE_OUTPUT_STREAMS, closeOutputStreams));
     * }.
     *
     * @see #sync(BitField)
     */
    public static void update(
            boolean waitForInputStreams, boolean closeInputStreams,
            boolean waitForOutputStreams, boolean closeOutputStreams)
    throws ArchiveSyncException {
        sync(   BitField.noneOf(SyncOption.class)
                .set(WAIT_FOR_INPUT_STREAMS, waitForInputStreams)
                .set(CLOSE_INPUT_STREAMS, closeInputStreams)
                .set(WAIT_FOR_OUTPUT_STREAMS, waitForOutputStreams)
                .set(CLOSE_OUTPUT_STREAMS, closeOutputStreams));
    }

    /**
     * Equivalent to {@code
        sync(   archive,
                BitField.of(CLOSE_INPUT_STREAMS, CLOSE_OUTPUT_STREAMS));
     * }.
     *
     * @see #sync(File, BitField)
     */
    public static void update(File archive)
    throws ArchiveSyncException {
        sync(   archive,
                BitField.of(CLOSE_INPUT_STREAMS, CLOSE_OUTPUT_STREAMS));
    }

    /**
     * Equivalent to {@code
        sync(   archive,
                BitField.noneOf(ArchiveSyncOption.class)
                .set(CLOSE_INPUT_STREAMS, closeStreams)
                .set(CLOSE_OUTPUT_STREAMS, closeStreams));
     * }.
     *
     * @see #sync(File, BitField)
     */
    public static void update(File archive, boolean closeStreams)
    throws ArchiveSyncException {
        sync(   archive,
                BitField.noneOf(SyncOption.class)
                .set(CLOSE_INPUT_STREAMS, closeStreams)
                .set(CLOSE_OUTPUT_STREAMS, closeStreams));
    }

    /**
     * Equivalent to {@code
        sync(   archive,
                BitField.noneOf(ArchiveSyncOption.class)
                .set(WAIT_FOR_INPUT_STREAMS, waitForInputStreams)
                .set(CLOSE_INPUT_STREAMS, closeInputStreams)
                .set(WAIT_FOR_OUTPUT_STREAMS, waitForOutputStreams)
                .set(CLOSE_OUTPUT_STREAMS, closeOutputStreams));
     * }.
     *
     * @see #sync(File, BitField)
     */
    public static void update(
            File archive,
            boolean waitForInputStreams, boolean closeInputStreams,
            boolean waitForOutputStreams, boolean closeOutputStreams)
    throws ArchiveSyncException {
        sync(   archive,
                BitField.noneOf(SyncOption.class)
                .set(WAIT_FOR_INPUT_STREAMS, waitForInputStreams)
                .set(CLOSE_INPUT_STREAMS, closeInputStreams)
                .set(WAIT_FOR_OUTPUT_STREAMS, waitForOutputStreams)
                .set(CLOSE_OUTPUT_STREAMS, closeOutputStreams));
    }

    /**
     * Returns a proxy instance which encapsulates <em>live</em> statistics
     * about the total map of archive files accessed by this package.
     * Any call to a method of the returned interface instance returns
     * up-to-date data, so there is no need to repeatedly call this method in
     * order to optain updated statistics.
     * <p>
     * Note that this method returns <em>live</em> statistics rather than
     * <em>real time</em> statistics.
     * So there may be a slight delay until the values returned reflect
     * the actual state of this package.
     * This delay increases if the system is under heavy load.
     */
    public static ArchiveStatistics getLiveArchiveStatistics() {
        return ArchiveControllers.getLiveArchiveStatistics();
    }

    /**
     * Returns the value of the class property {@code lenient}.
     * By default, this is the inverse of the boolean system property
     * {@code de.schlichtherle.truezip.io.archive.controllers.ArchiveControllers.strict}.
     * In other words, this returns {@code true} unless you map the
     * system property
     * {@code de.schlichtherle.truezip.io.archive.controllers.ArchiveControllers.strict}
     * to {@code true} or call {@link #setLenient(boolean) setLenient(false)}.
     *
     * @see #setLenient(boolean)
     */
    public static boolean isLenient() {
        return lenient;
    }

    /**
     * This class property controls whether (1) archive files and enclosed
     * directories shall be created on the fly if they don't exist and (2)
     * open archive entry streams should automatically be closed if they are
     * only weakly reachable.
     * By default, this class property is {@code true}.
     * <ol>
     * <li>
     * Consider the following path: &quot;a/outer.zip/b/inner.zip/c&quot;.
     * Now let's assume that &quot;a&quot; isExisting as a directory in the real file
     * system, while all other parts of this path don't, and that TrueZIP's
     * default configuration is used which would recognize &quot;outer.zip&quot; and
     * &quot;inner.zip&quot; as ZIP files.
     * <p>
     * If this class property is map to {@code false}, then
     * the client application would have to call
     * {@code new File(&quot;a/outer.zip/b/inner.zip&quot;).mkdirs()}
     * before it could actually create the innermost &quot;c&quot; entry as a file
     * or directory.
     * <p>
     * More formally, before you can access a node in the virtual file
     * system, all its parent directories must exist, including archive
     * files. This emulates the behaviour of real file systems.
     * <p>
     * If this class property is map to {@code true} however, then
     * any missing parent directories (including archive files) up to the
     * outermost archive file (&quot;outer.zip&quot;) are created on the fly when using
     * operations to create the innermost element of the path (&quot;c&quot;).
     * <p>
     * This allows applications to succeed when doing this:
     * {@code new File(&quot;a/outer.zip/b/inner.zip/c&quot;).createNewFile()},
     * or that:
     * {@code new FileOutputStream(&quot;a/outer.zip/b/inner.zip/c&quot;)}.
     * <p>
     * Note that in any case the parent directory of the outermost archive
     * file (&quot;a&quot;), must exist - TrueZIP does not create regular directories
     * in the real file system on the fly.
     * </li>
     * <li>
     * Many Java applications unfortunately fail to close their streams in all
     * cases, in particular if an {@code IOException} occured while
     * accessing it.
     * However, open streams are a limited resource in any operating system
     * and may interfere with other services of the OS (on Windows, you can't
     * delete an open file).
     * This is called the &quot;unclosed streams issue&quot;.
     * <p>
     * Likewise, in TrueZIP an unclosed archive entry stream may result in an
     * {@code ArchiveFileBusy(Warning)?Exception} to be thrown when
     * {@link #umount} or {@link #update} is called.
     * In order to prevent this, TrueZIP's archive entry streams have a
     * {@link Object#finalize()} method which closes an archive entry stream
     * if its garbage collected.
     * <p>
     * Now if this class property is map to {@code false}, then
     * TrueZIP maintains a hard reference to all archive entry streams
     * until {@link #umount} or {@link #update} is called, which will deal
     * with them: If they are not closed, an
     * {@code ArchiveFileBusy(Warning)?Exception} is thrown, depending on
     * the boolean parameters to these methods.
     * <p>
     * This setting is useful if you do not want to tolerate the
     * &quot;unclosed streams issue&quot; in a client application.
     * <p>
     * If this class property is map to {@code true} however, then
     * TrueZIP maintains only a weak reference to all archive entry streams.
     * This allows the garbage collector to finalize them before
     * {@link #umount} or {@link #update} is called.
     * The finalize() method will then close these archive entry streams,
     * which exempts them, from triggering an
     * {@code ArchiveBusy(Warning)?Exception} on the next call to
     * {@link #umount} or {@link #update}.
     * However, closing an archive entry output stream this way may result
     * in loss of buffered data, so it's only a workaround for this issue.
     * <p>
     * Note that for the setting of this class property to take effect, any
     * change must be made before an archive is first accessed.
     * The setting will then persist until the archive is reset by the next
     * call to {@link #umount} or {@link #update}.
     * </li>
     * </ol>
     *
     * @see #isLenient()
     */
    public static void setLenient(boolean lenient) {
        File.lenient = lenient;
    }

    /**
     * Returns the default {@link ArchiveDetector} to be used if no
     * archive detector is passed explicitly to the constructor of a
     * {@code File} instance.
     * <p>
     * This class property is initially map to
     * {@code ArchiveDetector.DEFAULT}
     *
     * @see ArchiveDetector
     * @see #setDefaultArchiveDetector
     */
    public static ArchiveDetector getDefaultArchiveDetector() {
        return defaultDetector;
    }

    /**
     * This class property controls how archive files are recognized.
     * When a new {@code File} instance is created and no
     * {@link ArchiveDetector} is provided to the constructor,
     * or when some method of this class are called which accept an
     * {@code ArchiveDetector} parameter,
     * then this class property is used.
     * Changing this value affects all newly created {@code File}
     * instances, but not any existing ones.
     *
     * @param detector The default {@link ArchiveDetector} to use
     *        for newly created {@code File} instances which have not
     *        been constructed with an explicit {@code ArchiveDetector}
     *        parameter
     * @throws NullPointerException If {@code detector} is
     *         {@code null}.
     * @see ArchiveDetector
     * @see #getDefaultArchiveDetector()
     * @see <a href="package-summary.html#third_parties">Third Party
     *      Access using different Archive Detectors</a>
     */
    public static void setDefaultArchiveDetector(
            final ArchiveDetector detector) {
        if (detector == null)
            throw new NullPointerException();
        File.defaultDetector = detector;
    }

    @Override
    public File getParentFile() {
        final java.io.File parent = delegate.getParentFile();
        if (parent == null)
            return null;

        assert super.getName().equals(delegate.getName());
        if (enclArchive != null
                && enclArchive.getPath().length() == parent.getPath().length()) {
            assert enclArchive.getPath().equals(parent.getPath());
            return enclArchive;
        }

        // This must not only be called for performance reasons, but also in
        // order to prevent the parent path from being rescanned for
        // archive files with a different detector, which could
        // trigger an update and reconfiguration of the respective
        // archive controller!
        return detector.createFile(parent, enclArchive);
    }

    /**
     * Returns the first parent directory (starting from this file) which is
     * <em>not</em> an archive file or a file located in an archive file.
     */
    public File getNonArchivedParentFile() {
        final File enclArchive = this.enclArchive;
        return enclArchive != null
                ? enclArchive.getNonArchivedParentFile()
                : (File) getParentFile();
    }

    @Override
    public File getAbsoluteFile() {
        File enclArchive = this.enclArchive;
        if (enclArchive != null)
            enclArchive = (File) enclArchive.getAbsoluteFile();
        return detector.createFile(this, delegate.getAbsoluteFile(), enclArchive);
    }

    /**
     * Similar to {@link #getAbsoluteFile()}, but removes any
     * {@code "."} and {@code ".."} directories from the path name wherever
     * possible.
     * The result is similar to {@link #getCanonicalFile()}, but symbolic
     * links are not resolved.
     * This may be useful if {@code getCanonicalFile()} throws an
     * IOException.
     *
     * @see #getCanonicalFile()
     * @see #getNormalizedFile()
     */
    public File getNormalizedAbsoluteFile() {
        File enclArchive = this.enclArchive;
        if (enclArchive != null)
            enclArchive = enclArchive.getNormalizedAbsoluteFile();
        return detector.createFile(
                this, normalize(delegate.getAbsoluteFile()), enclArchive);
    }

    /**
     * Similar to {@link #getAbsolutePath()}, but removes any redundant
     * {@code "."} and {@code ".."} directories from the path name.
     * The result is similar to {@link #getCanonicalPath()}, but symbolic
     * links are not resolved.
     * This may be useful if {@code getCanonicalPath()} throws an
     * IOException.
     *
     * @see #getCanonicalPath()
     * @see #getNormalizedPath()
     */
    public String getNormalizedAbsolutePath() {
        return normalize(getAbsolutePath(), separatorChar);
    }

    /**
     * Removes any {@code "."} and {@code ".."} directories from the path name
     * wherever possible.
     *
     * @return If this file is already normalized, it is returned.
     *         Otherwise a new instance of this class is returned.
     */
    public File getNormalizedFile() {
        final java.io.File normalizedFile = normalize(this);
        assert normalizedFile != null;
        if (normalizedFile == this)
            return this;
        assert !(normalizedFile instanceof File);
        assert enclArchive == null || normalize(enclArchive) == enclArchive;
        return detector.createFile(this, normalizedFile, enclArchive);
    }

    /**
     * Removes any redundant {@code "."}, {@code ".."} directories from the
     * path name.
     *
     * @return The normalized path of this file as a {@link String}.
     */
    public String getNormalizedPath() {
        return normalize(getPath(), separatorChar);
    }

    @Override
    public File getCanonicalFile() throws IOException {
        File enclArchive = this.enclArchive;
        if (enclArchive != null)
            enclArchive = (File) enclArchive.getCanonicalFile();
        // Note: entry.getCanonicalFile() may change case!
        return detector.createFile(this, delegate.getCanonicalFile(), enclArchive);
    }

    /**
     * This convenience method simply returns the canonical form of this
     * abstract path or the normalized absolute form if resolving the
     * prior fails.
     *
     * @return The canonical or absolute path of this file as an
     *         instance of this class.
     */
    public final File getCanOrAbsFile() {
        File enclArchive = this.enclArchive;
        if (enclArchive != null)
            enclArchive = enclArchive.getCanOrAbsFile();
        return detector.createFile(this, getRealFile(delegate), enclArchive);
    }

    /**
     * This convenience method simply returns the canonical form of this
     * abstract path or the normalized absolute form if resolving the
     * prior fails.
     *
     * @return The canonical or absolute path of this file as a
     *         {@code String} instance.
     */
    public String getCanOrAbsPath() {
        return getCanOrAbsFile().getPath();
    }

    /**
     * Returns {@code true} if and only if the path represented by this
     * instance denotes an archive file.
     * Whether or not this is true solely depends on the
     * {@link ArchiveDetector} which was used to construct this
     * {@code File} instance: If no {@code ArchiveDetector} is
     * explicitly passed to the constructor,
     * {@link #getDefaultArchiveDetector()} is used.
     * <p>
     * Please note that no file system tests are performed!
     * If a client application needs to know whether this file really isExisting
     * as an archive file in the file system (and the correct password has
     * been entered in case it's a RAES encrypted ZIP file), it should
     * subsequently call {@link #isDirectory}, too.
     * This will automount the virtual file system from the archive file and
     * return {@code true} if and only if it's a valid archive file.
     *
     * @see <a href="#false_positives">Identifying Archive Paths and False Positives</a>
     * @see #isDirectory
     * @see #isEntry
     */
    public final boolean isArchive() {
        return innerArchive == this;
    }

    /**
     * Returns {@code true} if and only if the path represented by this
     * instance names an archive file as an ancestor.
     * <p>
     * Whether or not this is true depends solely on the {@link ArchiveDetector}
     * used to construct this instance.
     * If no {@code ArchiveDetector} was explicitly passed to the
     * constructor, {@link #getDefaultArchiveDetector()} is used.
     * <p>
     * Please note that no tests on the file's true state are performed!
     * If you need to know whether this file is really an entry in an archive
     * file (and the correct password has been entered in case it's a RAES
     * encrypted ZIP file), you should call
     * {@link #getParentFile getParentFile()}.{@link #isDirectory isDirectory()}, too.
     * This will automount the virtual file system from the archive file and
     * return {@code true} if and only if it's a valid archive file.
     *
     * @see #isArchive
     */
    public final boolean isEntry() {
        return enclEntryName != null;
    }

    /**
     * Returns the innermost archive file in this path.
     * I.e. if this object is an archive file, then this method returns
     * this object.
     * If this object is a file or directory located within a
     * archive file, then this methods returns the file representing the
     * enclosing archive file, or {@code null} otherwise.
     * <p>
     * This method always returns a normalized path, i.e. all occurences of
     * {@code "."} and {@code ".."} in the path name are removed according to
     * their meaning wherever possible.
     * <p>
     * In order to support unlimited nesting levels, this method returns
     * a {@code File} instance which again could be an entry within
     * another archive file.
     */
    public final File getInnerArchive() {
        return innerArchive; // TODO: Rename to nextArchive
    }

    /**
     * Returns the entry name in the innermost archive file.
     * I.e. if this object is a archive file, then this method returns the
     * empty string {@code ""}.
     * If this object is a file or directory located within an
     * archive file, then this method returns the relative path of
     * the entry in the enclosing archive file separated by the entry
     * separator character {@code '/'}, or {@code null}
     * otherwise.
     * <p>
     * This method always returns an undotified path, i.e. all occurences of
     * {@code "."} and {@code ".."} in the path name are removed according to
     * their meaning wherever possible.
     */
    public final String getInnerEntryName() {
        return innerArchive == this ? ROOT : enclEntryName;
    }

    /**
     * Returns the enclosing archive file in this path.
     * I.e. if this object is an entry located within an archive file,
     * then this method returns the file representing the enclosing archive
     * file, or {@code null} otherwise.
     * <p>
     * This method always returns a normalized path, i.e. all occurences of
     * {@code "."} and {@code ".."} in the path name are removed according to
     * their meaning wherever possible.
     * <p>
     * In order to support unlimited nesting levels, this method returns
     * a {@code File} instance which again could be an entry within
     * another archive file.
     */
    public final File getEnclArchive() {
        return enclArchive;
    }

    /**
     * Returns the entry path in the enclosing archive file.
     * I.e. if this object is an entry located within a archive file,
     * then this method returns the relative path of the entry in the
     * enclosing archive file separated by the entry separator character
     * {@code '/'}, or {@code null} otherwise.
     * <p>
     * This method always returns an undotified path, i.e. all occurences of
     * {@code "."} and {@code ".."} in the path are removed according to their
     * meaning wherever possible.
     */
    public final String getEnclEntryName() {
        return enclEntryName;
    }

    /**
     * Returns the {@link ArchiveDetector} that was used to construct this
     * object - never {@code null}.
     */
    public final ArchiveDetector getArchiveDetector() {
        return detector;
    }

    /**
     * Returns the legacy {@link java.io.File java.io.File} object to which
     * some methods of this class delegate if this object does not represent an
     * archive file or an entry in an archive file.
     * <p>
     * <b>Warning:</b> This method is <em>not</em> intended for public use!
     * It's required to support the federation of file system implementations.
     * For example, {@link javax.swing.JFileChooser javax.swing.JFileChooser}
     * creates instances of
     * {@link sun.awt.shell.ShellFolder sun.awt.shell.ShellFolder},
     * which is a subclass of {@link java.io.File java.io.File}, too.
     * These instances are <i>wrapped</i> as the delegate in instances of this
     * class when using
     * {@link de.schlichtherle.truezip.io.swing.JFileChooser de.schlichtherle.truezip.io.swing.JFileChooser}
     * in order to federate the functionality of these different file system
     * implementations.
     * <p>
     * In case you want to convert an instance of this class which recognized
     * the base name of its path as an archive file to a file instance which
     * doesn't recognize this archive file, use the following code instead:
     * {@code ArchiveDetector.NULL.createFile((File) file.getParentFile(), file.getName())}
     *
     * @return An instance of the {@link java.io.File java.io.File} class or
     *         one of its subclasses, but never an instance of this class or
     *         its subclasses and never {@code null}.
     */
    public final java.io.File getDelegate() {
        return delegate;
    }

    /**
     * Returns an archive controller if and only if the path denotes an
     * archive file, or {@code null} otherwise.
     */
    final ArchiveController getArchiveController() {
        assert (controller != null) == isArchive();
        return controller;
    }

    /**
     * Returns {@code true} if and only if the path represented
     * by this instance is a direct or indirect parent of the path
     * represented by the specified {@code file}.
     * <p>
     * <b>Note:</b>
     * <ul>
     * <li>This method uses the canonical paths or, if failing to
     *     canonicalize the paths, at least the normalized absolute
     *     paths in order to compute reliable results.
     * <li>This method does <em>not</em> test the actual status
     *     of any file or directory in the file system.
     *     It just tests the paths.
     * </ul>
     *
     * @param file The path to test for being a child of this path.
     * @throws NullPointerException If the parameter is {@code null}.
     */
    public boolean isParentOf(final java.io.File file) {
        final String a = getRealFile(this).getPath();
        final String b = getRealFile(file).getParent();
        return b != null ? contains(a, b) : false;
    }

    /**
     * Returns {@code true} if and only if the path represented
     * by this instance contains the path represented by the specified
     * {@code file},
     * where a path is said to contain another path if and only
     * if it is equal or a parent of the other path.
     * <p>
     * <b>Note:</b>
     * <ul>
     * <li>This method uses the canonical paths or, if failing to
     *     canonicalize the paths, at the least normalized absolute
     *     paths in order to compute reliable results.
     * <li>This method does <em>not</em> test the actual status
     *     of any file or directory in the file system.
     *     It just tests the paths.
     * </ul>
     *
     * @param file The path to test for being contained by this path.
     * @throws NullPointerException If the parameter is {@code null}.
     */
    public boolean contains(java.io.File file) {
        return contains(this, file);
    }

    /**
     * Returns {@code true} if and only if the path represented
     * by {@code a} contains the path represented by {@code b},
     * where a path is said to contain another path if and only
     * if it is equal or a parent of the other path.
     * <p>
     * <b>Note:</b>
     * <ul>
     * <li>This method uses the canonical path name of the given files or,
     *     if failing to canonicalize the path names, the normalized absolute
     *     path names in order to compute reliable results.
     * <li>This method does <em>not</em> access the file system.
     *     It just tests the path names.
     * </ul>
     *
     * @param a The file to test for containing {@code b}.
     * @param b The file to test for being contained by {@code a}.
     * @throws NullPointerException If any parameter is {@code null}.
     */
    public static boolean contains(java.io.File a, java.io.File b) {
        return de.schlichtherle.truezip.io.Files.contains(a, b);
    }

    private static boolean contains(String a, String b) {
        return de.schlichtherle.truezip.io.Files.contains(a, b);
    }

    /**
     * Returns {@code true} if and only if this file denotes a file system
     * root or a UNC (if running on the Windows platform).
     */
    public boolean isFileSystemRoot() {
        File canOrAbsFile = getCanOrAbsFile();
        return roots.contains(canOrAbsFile) || isUNC(canOrAbsFile.getPath());
    }

    /**
     * Returns {@code true} if and only if this file denotes a UNC.
     * Note that this may be only relevant on the Windows platform.
     */
    public boolean isUNC() {
        return isUNC(getCanOrAbsFile().getPath());
    }

    /**
     * Returns {@code true} if and only if the given path is a UNC.
     * Note that this may be only relevant on the Windows platform.
     */
    private static boolean isUNC(final String path) {
        return path.startsWith(uncPrefix) && path.indexOf(separatorChar, 2) > 2;
    }

    @Override
    public int hashCode() {
        // Note that we cannot just return the paths' hash code:
        // Some platforms consider the case of files when comparing file
        // paths and some don't.
        // However, the entries INSIDE a archive file ALWAYS consider
        // case.
        // In addition, on Mac OS the Java implementation is not consistent
        // with the filesystem, i.e. the fs ignores case whereas
        // java.io.File.equals(...) and java.io.File.hashcode() consider case.
        // The following code distinguishes these cases.
        final File enclArchive = this.enclArchive;
        if (enclArchive != null) {
            // This file IS enclosed in an archive file.
            return 31 * enclArchive.hashCode() + enclEntryName.hashCode();
        } else {
            // This file is NOT enclosed in an archive file.
            return delegate.hashCode();
        }
    }

    /**
     * Tests this abstract path for equality with the given object.
     * Returns {@code true} if and only if the argument is not
     * {@code null} and is an abstract path that denotes the same
     * abstract path for a file or directory as this abstract path.
     * <p>
     * If the given file is not an instance of this class, the call is
     * forwarded to the superclass in order to ensure the required symmetry
     * of {@link Object#equals(Object)}.
     * <p>
     * Otherwise, whether or not two abstract paths are equal depends upon the
     * underlying operating and file system:
     * On UNIX systems, alphabetic case is significant in comparing paths.
     * On Microsoft Windows systems it is not unless the path denotes
     * an entry in an archive file. In the latter case, the left part of the
     * path up to the (leftmost) archive file is compared ignoring case
     * while the remainder (the entry name) is compared considering case.
     * This case distinction allows an application on Windows to deal with
     * archive files generated on other platforms which may contain different
     * entry with names that just differ in case (like e.g. hello.txt and
     * HELLO.txt).
     * <p>
     * Thus, on Windows the following assertions all succeed:
     * <pre>
     * File a, b;
     * a = new File(&quot;c:\\any.txt&quot;);
     * b = new File(&quot;C:\\ANY.TXT&quot;);
     * assert a.equals(b);
     * assert b.equals(a);
     * a = new File(&quot;c:\\any.zip\\test.txt&quot;),
     * b = new File(&quot;C:\\ANY.ZIP\\test.txt&quot;);
     * assert a.equals(b);
     * assert b.equals(a);
     * a = new File(&quot;c:/any.zip/test.txt&quot;);
     * b = new File(&quot;C:\\ANY.ZIP\\test.txt&quot;);
     * assert a.equals(b);
     * assert b.equals(a);
     * a = new File(&quot;c:\\any.zip\\test.txt&quot;);
     * b = new File(&quot;C:/ANY.ZIP/test.txt&quot;);
     * assert a.equals(b);
     * assert b.equals(a);
     * a = new File(&quot;c:/any.zip/test.txt&quot;);
     * b = new File(&quot;C:/ANY.ZIP/test.txt&quot;);
     * assert a.equals(b);
     * assert b.equals(a);
     * a = new File(&quot;\\\\localhost\\any.zip\\test.txt&quot;);
     * b = new File(&quot;\\\\LOCALHOST\\ANY.ZIP\\test.txt&quot;);
     * assert a.equals(b);
     * assert b.equals(a);
     * a = new File(&quot;//localhost/any.zip/test.txt&quot;);
     * b = new File(&quot;\\\\LOCALHOST\\ANY.ZIP\\test.txt&quot;);
     * assert a.equals(b);
     * assert b.equals(a);
     * a = new File(&quot;\\\\localhost\\any.zip\\test.txt&quot;);
     * b = new File(&quot;//LOCALHOST/ANY.ZIP/test.txt&quot;);
     * assert a.equals(b);
     * assert b.equals(a);
     * a = new File(&quot;//localhost/any.zip/test.txt&quot;);
     * b = new File(&quot;//LOCALHOST/ANY.ZIP/test.txt&quot;);
     * assert a.equals(b);
     * assert b.equals(a);
     * a = new File(&quot;c:\\any.zip\\test.txt&quot;);
     * b = new File(&quot;c:\\any.zip\\TEST.TXT&quot;);
     * assert !a.equals(b); // two different entries in same ZIP file!
     * assert !b.equals(a);
     * </pre>
     *
     * @param other The object to be compared with this abstract path.
     *
     * @return {@code true} if and only if the objects are equal,
     *         {@code false} otherwise
     *
     * @see #compareTo(Object)
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof File)
            return compareTo((File) other) == 0;
        return super.equals(other); // don't use entry - would break symmetry requirement!
    }

    /**
     * Compares this file's path to the given file's path.
     * <p>
     * If the given file is not an instance of this class, the call is
     * forwarded to the superclass in order to ensure the required symmetry
     * of {@link Comparable#compareTo(Object)}.
     * <p>
     * Otherwise, whether or not two abstract paths compare equal depends
     * upon the underlying operating and file system:
     * On UNIX platforms, alphabetic case is significant in comparing paths.
     * On the Windows platform it is not unless the path denotes
     * an entry in an archive file. In the latter case, the left part of the
     * path up to the (leftmost) archive file is compared in platform
     * dependent manner (hence ignoring case) while the remainder (the entry
     * name) is compared considering case.
     * This case distinction allows an application on the Windows platform to
     * deal with archive files generated on other platforms which may contain
     * different entries with names that just differ in case
     * (like e.g. {@code "hello.txt"} and {@code "HELLO.txt"}).
     *
     * @param other The file to be compared with this abstract path.
     *
     * @return A negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the given file.
     *
     * @see #equals(Object)
     * @see Comparable#compareTo(Object)
     */
    @Override
    public int compareTo(java.io.File other) {
        if (this == other)
            return 0;

        if (!(other instanceof File)) {
            // Degrade this file to a plain file in order to ensure
            // sgn(this.compareTo(other)) == -sgn(other.compareTo(this)).
            return super.compareTo(other); // don't use entry - would break antisymmetry requirement!
        }

        final File file = (File) other;

        // Note that we cannot just compare the paths:
        // Some platforms consider the case of files when comparing file
        // paths and some don't.
        // However, the entries INSIDE a archive file ALWAYS consider
        // case.
        // The following code distinguishes these cases.
        final File enclArchive = this.enclArchive;
        if (enclArchive != null) {
            // This file IS enclosed in a archive file.
            final File fileEnclArchive = file.enclArchive;
            if (fileEnclArchive != null) {
                // The given file IS enclosed in a archive file, too.
                int ret = enclArchive.compareTo(fileEnclArchive);
                if (ret == 0) {
                    // Now that the paths of the enclosing archive
                    // files compare equal, let's compare the entry names.
                    ret = enclEntryName.compareTo(file.enclEntryName);
                }

                return ret;
            }
        }

        // Degrade this file to a plain file in order to ensure
        // sgn(this.compareTo(other)) == -sgn(other.compareTo(this)).
        return super.compareTo(other); // don't use entry - would break antisymmetry requirement!
    }

    /**
     * Returns The top level archive file in the path or {@code null}
     * if this path does not denote an archive.
     * A top level archive is not enclosed in another archive.
     * If this does not return {@code null}, this denotes the longest
     * part of the path which actually may (but does not need to) exist
     * as a regular file in the real file system.
     */
    public File getTopLevelArchive() {
        final File enclArchive = this.enclArchive;
        return enclArchive != null
                ? enclArchive.getTopLevelArchive()
                : innerArchive;
    }

    @Override
    public String getAbsolutePath() {
        return delegate.getAbsolutePath();
    }

    @Override
    public String getCanonicalPath() throws IOException {
        return delegate.getCanonicalPath();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String getParent() {
        return delegate.getParent();
    }

    @Override
    public String getPath() {
        return delegate.getPath();
    }

    @Override
    public boolean isAbsolute() {
        return delegate.isAbsolute();
    }

    @Override
    public boolean isHidden() {
        return delegate.isHidden();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public java.net.URI toURI() {
        return delegate.toURI();
    }

    /**
     * @deprecated This method has been deprecated in JSE 6.
     * @see java.io.File#toURL
     */
    @Override
    public URL toURL() throws MalformedURLException {
        return delegate.toURL();
    }

    /**
     * Throws an {@code ArchiveEntryNotFoundException} if and only if this
     * file is a true archive file, not just a false positive, including
     * RAES encrypted ZIP files for which key prompting has been cancelled
     * or disabled.
     */
    final void ensureNotVirtualRoot(final String prefix)
    throws ArchiveEntryNotFoundException {
        if (isArchive() && (isDirectory() || (exists() && !isFile()))) {
            String msg = "virtual root directory";
            if (prefix != null)
                msg = prefix + " " + msg;
            throw new ArchiveEntryNotFoundException(getArchiveController(), ROOT, msg);
        }
    }

    //
    // File system operations:
    //

    /**
     * This file system operation is <a href="package-summary.html#atomicity">virtually atomic</a>.
     *
     * @see <a href="#false_positives">Identifying Archive Paths and False Positives</a>
     */
    @Override
    public boolean exists() {
        try {
            if (enclArchive != null)
                return enclArchive.getArchiveController()
                        .isExisting(enclEntryName);
        } catch (FalsePositiveException isNotArchive) {
            assert !(isNotArchive instanceof ArchiveEntryFalsePositiveException)
                    : "Must be handled by ArchiveController!";
            // Fall through!
        }
        return delegate.exists();
    }

    /**
     * Similar to its super class implementation, but returns
     * {@code false} for a valid archive file.
     * <p>
     * This file system operation is <a href="package-summary.html#atomicity">virtually atomic</a>.
     *
     * @see <a href="#false_positives">Identifying Archive Paths and False Positives</a>
     */
    @Override
    public boolean isFile() {
        try {
            if (innerArchive != null)
                return innerArchive.getArchiveController()
                        .isFile(getInnerEntryName());
        } catch (FalsePositiveException isNotArchive) {
            assert !(isNotArchive instanceof ArchiveEntryFalsePositiveException)
                    : "Must be handled by ArchiveController!";
            // Fall through!
            // See ArchiveDriver#newArchiveInput!
            if (isArchive()
                    && isNotArchive.getCause() instanceof FileNotFoundException)
                return false;
        }
        return delegate.isFile();
    }

    /**
     * Similar to its super class implementation, but returns
     * {@code true} for a valid archive file.
     * <p>
     * In case a RAES encrypted ZIP file is tested which is accessed for the
     * first time, the user is prompted for the password (if password based
     * encryption is used).
     * Note that this is not the only method which would prompt the user for
     * a password: For example, {@link #length} would prompt the user and
     * return {@code 0} unless the user cancels the prompting or the
     * file is a false positive archive file.
     * <p>
     * This file system operation is <a href="package-summary.html#atomicity">virtually atomic</a>.
     *
     * @see <a href="#false_positives">Identifying Archive Paths and False Positives</a>
     */
    @Override
    public boolean isDirectory() {
        try {
            if (innerArchive != null)
                return innerArchive.getArchiveController()
                        .isDirectory(getInnerEntryName());
        } catch (FalsePositiveException isNotArchive) {
            assert !(isNotArchive instanceof ArchiveEntryFalsePositiveException)
                    : "Must be handled by ArchiveController!";
            // Fall through!
        }
        return delegate.isDirectory();
    }

    /**
     * Returns an icon for this file or directory if it is in <i>open</i>
     * state for {@link de.schlichtherle.truezip.io.swing.JFileTree}
     * or {@code null} if the default should be used.
     */
    public Icon getOpenIcon() {
        try {
            if (innerArchive != null)
                return innerArchive.getArchiveController()
                        .getOpenIcon(getInnerEntryName());
        } catch (FalsePositiveException isNotArchive) {
            assert !(isNotArchive instanceof ArchiveEntryFalsePositiveException)
                    : "Must be handled by ArchiveController!";
            // Fall through!
        }
        return null;
    }

    /**
     * Returns an icon for this file or directory if it is in <i>closed</i>
     * state for {@link de.schlichtherle.truezip.io.swing.JFileTree}
     * or {@code null} if the default should be used.
     */
    public Icon getClosedIcon() {
        try {
            if (innerArchive != null)
                return innerArchive.getArchiveController()
                        .getClosedIcon(getInnerEntryName());
        } catch (FalsePositiveException isNotArchive) {
            assert !(isNotArchive instanceof ArchiveEntryFalsePositiveException)
                    : "Must be handled by ArchiveController!";
            // Fall through!
        }
        return null;
    }

    @Override
    public boolean canRead() {
        // More thorough test than isExisting
        try {
            if (innerArchive != null)
                return innerArchive.getArchiveController()
                        .isReadable(getInnerEntryName());
        } catch (FalsePositiveException isNotArchive) {
            assert !(isNotArchive instanceof ArchiveEntryFalsePositiveException)
                    : "Must be handled by ArchiveController!";
            // Fall through!
        }
        return delegate.canRead();
    }

    @Override
    public boolean canWrite() {
        try {
            if (innerArchive != null)
                return innerArchive.getArchiveController()
                        .isWritable(getInnerEntryName());
        } catch (FalsePositiveException isNotArchive) {
            assert !(isNotArchive instanceof ArchiveEntryFalsePositiveException)
                    : "Must be handled by ArchiveController!";
            // Fall through!
        }
        return delegate.canWrite();
    }

    /**
     * Like the super class implementation, but is aware of archive
     * files in its path.
     * For entries in a archive file, this is effectively a no-op:
     * The method will only return {@code true} if the entry isExisting and the
     * archive file was mounted read only.
     * <p>
     * This file system operation is <a href="package-summary.html#atomicity">virtually atomic</a>.
     */
    @Override
    public boolean setReadOnly() {
        try {
            if (innerArchive != null) {
                innerArchive.getArchiveController()
                        .setReadOnly(getInnerEntryName());
                return true;
            }
        } catch (FalsePositiveException isNotArchive) {
            assert !(isNotArchive instanceof ArchiveEntryFalsePositiveException)
                    : "Must be handled by ArchiveController!";
            // Fall through!
        } catch (IOException ex) {
            return false;
        }
        return delegate.setReadOnly();
    }

    /**
     * Returns the (uncompressed) getLength of the file.
     * The getLength returned of a valid archive file is {@code 0} in order
     * to correctly emulate virtual directories across all platforms.
     * <p>
     * In case a RAES encrypted ZIP file is tested which is accessed for the
     * first time, the user is prompted for the password (if password based
     * encryption is used).
     * Note that this is not the only method which would prompt the user for
     * a password: For example, {@link #isDirectory} would prompt the user and
     * return {@code true} unless the user cancels the prompting or the
     * file is a false positive archive file.
     * <p>
     * This file system operation is <a href="package-summary.html#atomicity">virtually atomic</a>.
     *
     * @see <a href="#false_positives">Identifying Archive Paths and False Positives</a>
     */
    @Override
    public long length() {
        try {
            if (innerArchive != null)
                return innerArchive.getArchiveController()
                        .getLength(getInnerEntryName());
        } catch (FalsePositiveException isNotArchive) {
            assert !(isNotArchive instanceof ArchiveEntryFalsePositiveException)
                    : "Must be handled by ArchiveController!";
            // Fall through!
        }
        return delegate.length();
    }

    /**
     * Returns a {@code long} value representing the time this file was
     * last modified, measured in milliseconds since the epoch (00:00:00 GMT,
     * January 1, 1970), or {@code 0L} if the file does not exist or if an
     * I/O error occurs or if this is a ghost directory in an archive file.
     * <p>
     * This file system operation is <a href="package-summary.html#atomicity">virtually atomic</a>.
     *
     * @see <a href="package.html">Package description for more information
     *      about ghost directories</a>
     */
    @Override
    public long lastModified() {
        try {
            if (innerArchive != null)
                return innerArchive.getArchiveController()
                        .getLastModified(getInnerEntryName());
        } catch (FalsePositiveException isNotArchive) {
            assert !(isNotArchive instanceof ArchiveEntryFalsePositiveException)
                    : "Must be handled by ArchiveController!";
            // Fall through!
        }
        return delegate.lastModified();
    }

    /**
     * Sets the last modification of this file or (virtual) directory.
     * If this is a ghost directory within an archive file, it's reincarnated
     * as a regular directory within the archive file.
     * <p>
     * Note that calling this method may incur a severe performance penalty
     * if the file is an entry in an archive file which has just been written
     * (such as after a normal copy operation).
     * If you want to copy a file's contents as well as its last modification
     * time, use {@link #archiveCopyFrom(java.io.File)} or
     * {@link #archiveCopyTo(java.io.File)} instead.
     * <p>
     * This file system operation is <a href="package-summary.html#atomicity">virtually atomic</a>.
     *
     * @see #archiveCopyFrom(java.io.File)
     * @see #archiveCopyTo(java.io.File)
     * @see <a href="package.html">Package description for more information
     *      about ghost directories</a>
     */
    @Override
    public boolean setLastModified(final long time) {
        try {
            if (innerArchive != null)
                return innerArchive.getArchiveController()
                        .setLastModified(getInnerEntryName(),time);
        } catch (FalsePositiveException isNotArchive) {
            assert !(isNotArchive instanceof ArchiveEntryFalsePositiveException)
                    : "Must be handled by ArchiveController!";
            // Fall through!
        }
        return delegate.setLastModified(time);
    }

    /**
     * Returns the names of the members in this directory in a newly
     * created array.
     * The returned array is <em>not</em> sorted.
     * This is the most efficient list method.
     * <p>
     * <b>Note:</b> Archive entries with absolute paths are ignored by
     * this method and are never returned.
     * <p>
     * This file system operation is <a href="package-summary.html#atomicity">virtually atomic</a>.
     */
    @Override
    public String[] list() {
        try {
            if (innerArchive != null) {
                final Set<String> members = innerArchive
                        .getArchiveController()
                        .list(getInnerEntryName());
                return members == null
                        ? null : members.toArray(new String[members.size()]);
            }
        } catch (FalsePositiveException isNotArchive) {
            assert !(isNotArchive instanceof ArchiveEntryFalsePositiveException)
                    : "Must be handled by ArchiveController!";
            // Fall through!
        }
        return delegate.list();
    }

    /**
     * Returns the names of the members in this directory which are
     * accepted by {@code filenameFilter} in a newly created array.
     * The returned array is <em>not</em> sorted.
     * <p>
     * <b>Note:</b> Archive entries with absolute paths are ignored by
     * this method and are never returned.
     * <p>
     * This file system operation is <a href="package-summary.html#atomicity">virtually atomic</a>.
     *
     * @return {@code null} if this is not a directory or an archive file,
     *         a valid (but maybe empty) array otherwise.
     */
    @Override
    public String[] list(final FilenameFilter filter) {
        try {
            if (innerArchive != null) {
                final Set<String> members = innerArchive.getArchiveController()
                        .list(getInnerEntryName());
                if (members == null)
                    return null;
                if (filter == null)
                    return members.toArray(new String[members.size()]);
                final Collection<String> filtered
                        = new ArrayList<String>(members.size());
                for (final String member : members)
                    if (filter.accept(this, member))
                        filtered.add(member);
                return filtered.toArray(new String[filtered.size()]);
            }
        } catch (FalsePositiveException isNotArchive) {
            assert !(isNotArchive instanceof ArchiveEntryFalsePositiveException)
                    : "Must be handled by ArchiveController!";
            // Fall through!
        }
        return delegate.list(filter);
    }

    /**
     * Equivalent to {@link #listFiles(FilenameFilter, FileFactory)
     * listFiles((FilenameFilter) null, getArchiveDetector())}.
     */
    @Override
    public File[] listFiles() {
        return listFiles((FilenameFilter) null, detector);
    }

    /**
     * Returns {@code File} objects for the members in this directory
     * in a newly created array.
     * The returned array is <em>not</em> sorted.
     * <p>
     * Note that archive entries with absolute paths are ignored by this
     * method and are never returned.
     * <p>
     * This file system operation is <a href="package-summary.html#atomicity">virtually atomic</a>.
     *
     * @param factory The factory used to create the member file of this
     *        directory.
     *        This could be an {@link ArchiveDetector} in order to detect any
     *        archives by the member file names.
     * @return {@code null} if this is not a directory or an archive file,
     *         a valid (but maybe empty) array otherwise.
     */
    public File[] listFiles(final FileFactory factory) {
        return listFiles((FilenameFilter) null, factory);
    }

    /**
     * Equivalent to {@link #listFiles(FilenameFilter, FileFactory)
     * listFiles(filenameFilter, getArchiveDetector())}.
     */
    @Override
    public File[] listFiles(final FilenameFilter filenameFilter) {
        return listFiles(filenameFilter, detector);
    }

    /**
     * Returns {@code File} objects for the members in this directory
     * which are accepted by {@code filenameFilter} in a newly created
     * array.
     * The returned array is <em>not</em> sorted.
     * <p>
     * Note that archive entries with absolute paths are ignored by this
     * method and are never returned.
     * <p>
     * This file system operation is <a href="package-summary.html#atomicity">virtually atomic</a>.
     *
     * @param factory The factory used to create the member file of this
     *        directory.
     *        This could be an {@link ArchiveDetector} in order to detect any
     *        archives by the member file names.
     * @return {@code null} if this is not a directory or an archive file,
     *         a valid (but maybe empty) array otherwise.
     * @see <a href="package-summary.html#third_parties">Third Party
     *      Access using different Archive Detectors</a>
     */
    public File[] listFiles(
            final FilenameFilter filter,
            final FileFactory factory) {
        try {
            if (innerArchive != null) {
                final Set<String> members = innerArchive.getArchiveController()
                        .list(getInnerEntryName());
                if (members == null)
                    return null;
                final Collection<File> filtered
                        = new ArrayList<File>(members.size());
                for (final String member : members)
                    if (filter == null || filter.accept(this, member))
                        filtered.add(factory.createFile(File.this, member));
                return filtered.toArray(new File[filtered.size()]);
            }
        } catch (FalsePositiveException isNotArchive) {
            assert !(isNotArchive instanceof ArchiveEntryFalsePositiveException)
                    : "Must be handled by ArchiveController!";
            // Fall through!
        }
        return convert(delegate.listFiles(filter), factory);
    }

    private static File[] convert(
            final java.io.File[] files,
            final FileFactory factory) {
        if (files == null)
            return null; // no directory

        File[] results = new File[files.length];
        for (int i = files.length; 0 <= --i; )
            results[i] = factory.createFile(files[i]);

        return results;
    }

    /**
     * Equivalent to {@link #listFiles(FileFilter, FileFactory)
     * listFiles(fileFilter, getArchiveDetector())}.
     */
    @Override
    public final File[] listFiles(final FileFilter fileFilter) {
        return listFiles(fileFilter, detector);
    }

    /**
     * Returns {@code File} objects for the members in this directory
     * which are accepted by {@code fileFilter} in a newly created array.
     * The returned array is <em>not</em> sorted.
     * <p>
     * Note that archive entries with absolute paths are ignored by this
     * method and are never returned.
     * <p>
     * This file system operation is <a href="package-summary.html#atomicity">virtually atomic</a>.
     *
     * @param factory The factory used to create the member file of this
     *        directory.
     *        This could be an {@link ArchiveDetector} in order to detect any
     *        archives by the member file names.
     * @return {@code null} if this is not a directory or an archive file,
     *         a valid (but maybe empty) array otherwise.
     * @see <a href="package-summary.html#third_parties">Third Party
     *      Access using different Archive Detectors</a>
     */
    public File[] listFiles(
            final FileFilter filter,
            final FileFactory factory) {
        try {
            if (innerArchive != null) {
                final Set<String> members = innerArchive.getArchiveController()
                        .list(getInnerEntryName());
                if (members == null)
                    return null;
                final Collection<File> filtered
                        = new ArrayList<File>(members.size());
                for (final String member : members) {
                    final File file = factory.createFile(File.this, member);
                    if (filter == null || filter.accept(file))
                        filtered.add(file);
                }
                return filtered.toArray(new File[filtered.size()]);
            }
        } catch (FalsePositiveException isNotArchive) {
            assert !(isNotArchive instanceof ArchiveEntryFalsePositiveException)
                    : "Must be handled by ArchiveController!";
            // Fall through!
        }
        return delegateListFiles(filter, factory);
    }

    private File[] delegateListFiles(
            final FileFilter filter,
            final FileFactory factory) {
        // When filtering, we want to pass in {@code de.schlichtherle.truezip.io.File}
        // objects rather than {@code java.io.File} objects, so we cannot
        // just call {@code entry.listFiles(FileFilter)}.
        // Instead, we will query the entry for the children names (i.e.
        // Strings) only, construct {@code de.schlichtherle.truezip.io.File}
        // instances from this and then apply the filter to construct the
        // result list.

        final List filteredList = new ArrayList();
        final String[] children = delegate.list();
        if (children == null)
            return null; // no directory

        for (int i = 0, l = children.length; i < l; i++) {
            final String child = children[i];
            final File file = factory.createFile(this, child);
            if (filter == null || filter.accept(file))
                filteredList.add(file);
        }
        final File[] list = new File[filteredList.size()];
        filteredList.toArray(list);

        return list;
    }

    /**
     * Creates a new, empty file similar to its superclass implementation.
     * Note that this method doesn't create archive files because archive
     * files are virtual directories, not files!
     * <p>
     * This file system operation is <a href="package-summary.html#atomicity">virtually atomic</a>.
     *
     * @see #mkdir
     */
    @Override
    public boolean createNewFile() throws IOException {
        try {
            if (enclArchive != null)
                return enclArchive.getArchiveController()
                        .createNewFile(enclEntryName, isLenient());
        } catch (FalsePositiveException isNotArchive) {
            assert !(isNotArchive instanceof ArchiveEntryFalsePositiveException)
                    : "Must be handled by ArchiveController!";
            // Fall through!
        } catch (IOException ex) {
            throw ex;
        }
        return delegate.createNewFile();
    }

    @Override
    public boolean mkdirs() {
        if (innerArchive == null)
            return delegate.mkdirs();

        final File parent = (File) getParentFile();
        if (parent != null && !parent.exists())
            parent.mkdirs();

        // TODO: Profile: return parent.isDirectory() && mkdir();
        // May perform better in certain situations where (probably false
        // positive) archive files are involved.
        return mkdir();
    }

    /**
     * Creates a new, empty (virtual) directory similar to its superclass
     * implementation.
     * This method creates an archive file if {@link #isArchive} returns
     * {@code true}.
     * Example:
     * {@code new File(&quot;archive.zip&quot;).mkdir();}
     * <p>
     * Alternatively, archive files can be created on the fly by simply
     * creating their entries.
     * Example:
     * {@code new FileOutputStream(&quot;archive.zip/README&quot;);}
     * <p>
     * These examples assume TrueZIP's default configuration where ZIP file
     * recognition is enabled and {@link #isLenient} returns {@code true}.
     * <p>
     * This file system operation is <a href="package-summary.html#atomicity">virtually atomic</a>.
     */
    @Override
    public boolean mkdir() {
        try {
            if (innerArchive != null)
                return innerArchive.getArchiveController()
                        .mkdir( getInnerEntryName(), isLenient());
        } catch (FalsePositiveException isNotArchive) {
            assert !(isNotArchive instanceof ArchiveEntryFalsePositiveException)
                    : "Must be handled by ArchiveController!";
            // Fall through!
        }
        return delegate.mkdir();
    }

    /**
     * Deletes an archive entry, archive file or regular node in the real file
     * system.
     * If the file is a directory, it must be empty.
     * <p>
     * This file system operation is <a href="package-summary.html#atomicity">virtually atomic</a>.
     *
     * @see #deleteAll
     */
    @Override
    public boolean delete() {
        try {
            if (innerArchive != null)
                return innerArchive.getArchiveController()
                        .delete(getInnerEntryName());
        } catch (FalsePositiveException isNotArchive) {
            assert !(isNotArchive instanceof ArchiveEntryFalsePositiveException)
                    : "Must be handled by ArchiveController!";
            // Fall through!
            // See ArchiveDriver#newArchiveInput!
            if (isArchive()
            && !delegate.isDirectory()
            && isNotArchive.getCause() instanceof FileNotFoundException)
                return false;
        }
        return delegate.delete();
    }

    /**
     * Deletes the entire directory tree represented by this object,
     * regardless whether this is a file or directory, whether the directory
     * is empty or not or whether the file or directory is actually an
     * archive file, an entry in an archive file or not enclosed in an
     * archive file at all.
     * <p>
     * This file system operation is <em>not</em> atomic.
     *
     * @return Whether or not the entire directory tree was successfully
     *         deleted.
     */
    public boolean deleteAll() {
        return Files.deleteAll(this);
    }

    @Override
    public void deleteOnExit() {
        if (innerArchive == null) {
            delegate.deleteOnExit();
            return;
        }

        if (isArchive()) {
            // We cannot prompt the user for a password in the shutdown hook
            // in case this is a RAES encrypted ZIP file.
            // So we do this now instead.
            isDirectory();
        }

        class DeleteOnExit implements Runnable {
            public void run() {
                if (exists() && !delete()) {
                    // Logging may not work in a shutdown hook!
                    System.err.println(getPath() + ": failed to deleteOnExit()!");
                }
            }
        }
        ArchiveControllers.addToShutdownHook(new DeleteOnExit());
    }

    /**
     * Equivalent to {@link #renameTo(java.io.File, ArchiveDetector)
     * renameTo(dst, getArchiveDetector())}.
     */
    @Override
    public final boolean renameTo(final java.io.File dst) {
        return renameTo(dst, detector);
    }

    /**
     * Behaves similar to the super class, but renames this file or directory
     * by recursively copying its data if this object or the {@code dst}
     * object is either an archive file or an entry located in an archive file.
     * Hence, in these cases only this file system operation is <em>not</em>
     * atomic.
     *
     * @param detector The object used to detect any archive
     *        files in the path and configure their parameters.
     * @see <a href="package-summary.html#third_parties">Third Party
     *      Access using different Archive Detectors</a>
     */
    public boolean renameTo(
            final java.io.File dst,
            final ArchiveDetector detector) {
        // Nice trick, but wouldn't be thread safe!
        /*if (enclArchive == null) {
            if (!(dst instanceof File) || ((File) dst).enclArchive == null) {
                try {
                    sync(this);
                    sync((File) dst);
                } catch (ArchiveSyncException ex) {
                    return false;
                }
                return delegate.renameTo(dst);
            }
        }*/

        if (innerArchive == null)
            if (!(dst instanceof File) || ((File) dst).innerArchive == null)
                return delegate.renameTo(dst);

        return !dst.exists() && Files.move(this, dst, detector);
    }

    /**
     * Copies the input stream {@code in} to this file and closes it.
     * <p>
     * <table border="2" cellpadding="4">
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * <tr>
     *   <td>Preserves file attributes</td>
     *   <td>None</td>
     * </tr>
     * <tr>
     *   <td>Copies directories recursively</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Reads and overwrites special files</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Closes parameter streams</td>
     *   <td>Always</td>
     * </tr>
     * <tr>
     *   <td><a href="#DDC">Direct Data Copying (DDC)</a></td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written files on failure</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written directories on failure</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Atomic</td>
     *   <td>No</td>
     * </tr>
     * </table>
     *
     * @return {@code true} if and only if the operation succeeded.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public boolean copyFrom(final InputStream in) {
        try {
            final OutputStream out = detector.createFileOutputStream(this, false);
            try {
                cp(in, out); // always closes in and out
                return true;
            } catch (IOException ex) {
                delete();
            }
        } catch (IOException ex) {
        }
        return false;
    }

    /**
     * Copies the file {@code src} to this file.
     * <p>
     * <table border="2" cellpadding="4">
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * <tr>
     *   <td>Preserves file attributes</td>
     *   <td>None</td>
     * </tr>
     * <tr>
     *   <td>Copies directories recursively</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Reads and overwrites special files</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Closes parameter streams</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td><a href="#DDC">Direct Data Copying (DDC)</a></td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written files on failure</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written directories on failure</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Atomic</td>
     *   <td>No</td>
     * </tr>
     * </table>
     *
     * @param src The source file. Note that although this just needs to
     *        be a plain {@code java.io.File}, archive entries are only
     *        supported for instances of this class.
     * @return {@code true} if and only if the operation succeeded.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public boolean copyFrom(final java.io.File src) {
        try {
            cp(src, this);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Recursively copies the file or directory {@code src}
     * to this file or directory.
     * This version uses the {@link ArchiveDetector} which was used to
     * construct this object to detect any archive files in the source
     * and destination directory trees.
     * <p>
     * <table border="2" cellpadding="4">
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * <tr>
     *   <td>Preserves file attributes</td>
     *   <td>None</td>
     * </tr>
     * <tr>
     *   <td>Copies directories recursively</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Reads and overwrites special files</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Closes parameter streams</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td><a href="#DDC">Direct Data Copying (DDC)</a></td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written files on failure</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written directories on failure</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Atomic</td>
     *   <td>No</td>
     * </tr>
     * </table>
     *
     * @param src The source file. Note that although this just needs to
     *        be a plain {@code java.io.File}, archive files and entries
     *        are only supported for instances of this class.
     * @return {@code true} if and only if the operation succeeded.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public boolean copyAllFrom(final java.io.File src) {
        try {
            Files.copyAll(false, src, this, detector, detector);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Recursively copies the file or directory {@code src}
     * to this file or directory.
     * This version uses the given archive detector to detect any archive
     * files in the source and destination directory trees.
     * <p>
     * <table border="2" cellpadding="4">
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * <tr>
     *   <td>Preserves file attributes</td>
     *   <td>None</td>
     * </tr>
     * <tr>
     *   <td>Copies directories recursively</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Reads and overwrites special files</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Closes parameter streams</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td><a href="#DDC">Direct Data Copying (DDC)</a></td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written files on failure</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written directories on failure</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Atomic</td>
     *   <td>No</td>
     * </tr>
     * </table>
     *
     * @param src The source file. Note that although this just needs to
     *        be a plain {@code java.io.File}, archive files and entries
     *        are only supported for instances of this class.
     * @param detector The object used to detect any archive files
     *        in the source and destination directory trees.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     * @see <a href="package-summary.html#third_parties">Third Party
     *      Access using different Archive Detectors</a>
     */
    public boolean copyAllFrom(
            final java.io.File src,
            final ArchiveDetector detector) {
        try {
            Files.copyAll(false, src, this, detector, detector);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Recursively copies the file or directory {@code src}
     * to this file or directory.
     * By using different {@link ArchiveDetector}s for the source and
     * destination, this method can be used to do advanced stuff like
     * unzipping any archive file in the source tree to a plain directory
     * in the destination tree (where {@code srcDetector} could be
     * {@link ArchiveDetector#DEFAULT} and {@code dstDetector} must be
     * {@link ArchiveDetector#NULL}) or changing the charset by configuring
     * a custom {@link DefaultArchiveDetector}.
     * <p>
     * <table border="2" cellpadding="4">
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * <tr>
     *   <td>Preserves file attributes</td>
     *   <td>None</td>
     * </tr>
     * <tr>
     *   <td>Copies directories recursively</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Reads and overwrites special files</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Closes parameter streams</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td><a href="#DDC">Direct Data Copying (DDC)</a></td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written files on failure</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written directories on failure</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Atomic</td>
     *   <td>No</td>
     * </tr>
     * </table>
     *
     * @param src The source file. Note that although this just needs to
     *        be a plain {@code java.io.File}, archive files and entries
     *        are only supported for instances of this class.
     * @param srcDetector The object used to detect any archive files
     *        in the source directory tree.
     * @param dstDetector The object used to detect any archive files
     *        in the destination directory tree.
     * @return {@code true} if and only if the operation succeeded.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     * @see <a href="package-summary.html#third_parties">Third Party
     *      Access using different Archive Detectors</a>
     */
    public boolean copyAllFrom(
            final java.io.File src,
            final ArchiveDetector srcDetector,
            final ArchiveDetector dstDetector) {
        try {
            Files.copyAll(false, src, this, srcDetector, dstDetector);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Copies this file to the output stream {@code out} and closes it.
     * <p>
     * <table border="2" cellpadding="4">
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * <tr>
     *   <td>Preserves file attributes</td>
     *   <td>None</td>
     * </tr>
     * <tr>
     *   <td>Copies directories recursively</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Reads and overwrites special files</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Closes parameter streams</td>
     *   <td>Always</td>
     * </tr>
     * <tr>
     *   <td><a href="#DDC">Direct Data Copying (DDC)</a></td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written files on failure</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written directories on failure</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Atomic</td>
     *   <td>No</td>
     * </tr>
     * </table>
     *
     * @return {@code true} if and only if the operation succeeded.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public boolean copyTo(final OutputStream out) {
        try {
            final InputStream in = detector.createFileInputStream(this);
            cp(in, out); // always closes in and out
            return true;
        } catch (IOException failed) {
            return false;
        }
    }

    /**
     * Copies this file to the file {@code dst}.
     * <p>
     * <table border="2" cellpadding="4">
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * <tr>
     *   <td>Preserves file attributes</td>
     *   <td>None</td>
     * </tr>
     * <tr>
     *   <td>Copies directories recursively</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Reads and overwrites special files</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Closes parameter streams</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td><a href="#DDC">Direct Data Copying (DDC)</a></td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written files on failure</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written directories on failure</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Atomic</td>
     *   <td>No</td>
     * </tr>
     * </table>
     *
     * @param dst The destination file. Note that although this just needs to
     *        be a plain {@code java.io.File}, archive entries are only
     *        supported for instances of this class.
     * @return {@code true} if the file has been successfully copied.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public boolean copyTo(final java.io.File dst) {
        try {
            cp(this, dst);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Recursively copies this file or directory to the file or directory
     * {@code dst}.
     * This version uses the {@link ArchiveDetector} which was used to
     * construct this object to detect any archive files in the source
     * and destination directory trees.
     * <p>
     * <table border="2" cellpadding="4">
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * <tr>
     *   <td>Preserves file attributes</td>
     *   <td>None</td>
     * </tr>
     * <tr>
     *   <td>Copies directories recursively</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Reads and overwrites special files</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Closes parameter streams</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td><a href="#DDC">Direct Data Copying (DDC)</a></td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written files on failure</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written directories on failure</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Atomic</td>
     *   <td>No</td>
     * </tr>
     * </table>
     *
     * @param dst The destination file. Note that although this just needs to
     *        be a plain {@code java.io.File}, archive files and entries
     *        are only supported for instances of this class.
     * @return {@code true} if and only if the operation succeeded.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public boolean copyAllTo(final java.io.File dst) {
        try {
            Files.copyAll(false, this, dst, detector, detector);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Recursively copies this file or directory to the file or directory
     * {@code dst}.
     * This version uses the given archive detector to detect any archive
     * files in the source and destination directory trees.
     * <p>
     * <table border="2" cellpadding="4">
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * <tr>
     *   <td>Preserves file attributes</td>
     *   <td>None</td>
     * </tr>
     * <tr>
     *   <td>Copies directories recursively</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Reads and overwrites special files</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Closes parameter streams</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td><a href="#DDC">Direct Data Copying (DDC)</a></td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written files on failure</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written directories on failure</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Atomic</td>
     *   <td>No</td>
     * </tr>
     * </table>
     *
     * @param dst The destination file. Note that although this just needs to
     *        be a plain {@code java.io.File}, archive files and entries
     *        are only supported for instances of this class.
     * @param detector The object used to detect any archive files
     *        in the source and destination directory trees.
     * @return {@code true} if and only if the operation succeeded.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     * @see <a href="package-summary.html#third_parties">Third Party
     *      Access using different Archive Detectors</a>
     */
    public boolean copyAllTo(
            final java.io.File dst,
            final ArchiveDetector detector) {
        try {
            Files.copyAll(false, this, dst, detector, detector);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Recursively copies this file or directory to the file or directory
     * {@code dst}.
     * By using different {@link ArchiveDetector}s for the source and
     * destination, this method can be used to do advanced stuff like
     * unzipping any archive file in the source tree to a plain directory
     * in the destination tree (where {@code srcDetector} could be
     * {@link ArchiveDetector#DEFAULT} and {@code dstDetector} must be
     * {@link ArchiveDetector#NULL}) or changing the charset by configuring
     * a custom {@link DefaultArchiveDetector}.
     * <p>
     * <table border="2" cellpadding="4">
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * <tr>
     *   <td>Preserves file attributes</td>
     *   <td>None</td>
     * </tr>
     * <tr>
     *   <td>Copies directories recursively</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Reads and overwrites special files</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Closes parameter streams</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td><a href="#DDC">Direct Data Copying (DDC)</a></td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written files on failure</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written directories on failure</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Atomic</td>
     *   <td>No</td>
     * </tr>
     * </table>
     *
     * @param dst The destination file. Note that although this just needs to
     *        be a plain {@code java.io.File}, archive files and entries
     *        are only supported for instances of this class.
     * @param srcDetector The object used to detect any archive files
     *        in the source directory tree.
     * @param dstDetector The object used to detect any archive files
     *        in the destination directory tree.
     * @return {@code true} if and only if the operation succeeded.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     * @see <a href="package-summary.html#third_parties">Third Party
     *      Access using different Archive Detectors</a>
     */
    public boolean copyAllTo(
            final java.io.File dst,
            final ArchiveDetector srcDetector,
            final ArchiveDetector dstDetector) {
        try {
            Files.copyAll(false, this, dst, srcDetector, dstDetector);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Copies the file {@code src} to this file and tries to preserve
     * all attributes of the source file to the destination file, too.
     * Note that the current implementation only preserves the last
     * modification time.
     * <p>
     * <table border="2" cellpadding="4">
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * <tr>
     *   <td>Preserves file attributes</td>
     *   <td>Best effort</td>
     * </tr>
     * <tr>
     *   <td>Copies directories recursively</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Reads and overwrites special files</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Closes parameter streams</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td><a href="#DDC">Direct Data Copying (DDC)</a></td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written files on failure</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written directories on failure</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Atomic</td>
     *   <td>No</td>
     * </tr>
     * </table>
     *
     * @param src The source file. Note that although this just needs to
     *        be a plain {@code java.io.File}, archive entries are only
     *        supported for instances of this class.
     * @return {@code true} if and only if the operation succeeded.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public boolean archiveCopyFrom(final java.io.File src) {
        try {
            cp_p(src, this);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Recursively copies the file or directory {@code src} to this file
     * or directory and tries to preserve all attributes of the source
     * file to the destination file, too.
     * Note that the current implementation only preserves the last
     * modification time.
     * <p>
     * This version uses the {@link ArchiveDetector} which was used to
     * construct this object to detect any archive files in the source
     * and destination directory trees.
     * <p>
     * <table border="2" cellpadding="4">
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * <tr>
     *   <td>Preserves file attributes</td>
     *   <td>Best effort</td>
     * </tr>
     * <tr>
     *   <td>Copies directories recursively</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Reads and overwrites special files</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Closes parameter streams</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td><a href="#DDC">Direct Data Copying (DDC)</a></td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written files on failure</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written directories on failure</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Atomic</td>
     *   <td>No</td>
     * </tr>
     * </table>
     *
     * @param src The source file. Note that although this just needs to
     *        be a plain {@code java.io.File}, archive files and entries
     *        are only supported for instances of this class.
     * @return {@code true} if and only if the operation succeeded.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public boolean archiveCopyAllFrom(final java.io.File src) {
        try {
            Files.copyAll(true, src, this, detector, detector);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Recursively copies the file or directory {@code src} to this file
     * or directory and tries to preserve all attributes of the source
     * file to the destination file, too.
     * Note that the current implementation only preserves the last
     * modification time.
     * <p>
     * This version uses the given archive detector to detect any archive
     * files in the source and destination directory trees.
     * <p>
     * <table border="2" cellpadding="4">
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * <tr>
     *   <td>Preserves file attributes</td>
     *   <td>Best effort</td>
     * </tr>
     * <tr>
     *   <td>Copies directories recursively</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Reads and overwrites special files</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Closes parameter streams</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td><a href="#DDC">Direct Data Copying (DDC)</a></td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written files on failure</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written directories on failure</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Atomic</td>
     *   <td>No</td>
     * </tr>
     * </table>
     *
     * @param src The source file. Note that although this just needs to
     *        be a plain {@code java.io.File}, archive files and entries
     *        are only supported for instances of this class.
     * @param detector The object used to detect any archive files
     *        in the source and destination directory trees.
     * @return {@code true} if and only if the operation succeeded.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     * @see <a href="package-summary.html#third_parties">Third Party
     *      Access using different Archive Detectors</a>
     */
    public boolean archiveCopyAllFrom(
            final java.io.File src,
            final ArchiveDetector detector) {
        try {
            Files.copyAll(true, src, this, detector, detector);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Recursively copies the file or directory {@code src} to this file
     * or directory and tries to preserve all attributes of the source
     * file to the destination file, too.
     * Note that the current implementation only preserves the last
     * modification time.
     * <p>
     * By using different {@link ArchiveDetector}s for the source and
     * destination, this method can be used to do advanced stuff like
     * unzipping any archive file in the source tree to a plain directory
     * in the destination tree (where {@code srcDetector} could be
     * {@link ArchiveDetector#DEFAULT} and {@code dstDetector} must be
     * {@link ArchiveDetector#NULL}) or changing the charset by configuring
     * a custom {@link DefaultArchiveDetector}.
     * <p>
     * <table border="2" cellpadding="4">
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * <tr>
     *   <td>Preserves file attributes</td>
     *   <td>Best effort</td>
     * </tr>
     * <tr>
     *   <td>Copies directories recursively</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Reads and overwrites special files</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Closes parameter streams</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td><a href="#DDC">Direct Data Copying (DDC)</a></td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written files on failure</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written directories on failure</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Atomic</td>
     *   <td>No</td>
     * </tr>
     * </table>
     *
     * @param src The source file. Note that although this just needs to
     *        be a plain {@code java.io.File}, archive files and entries
     *        are only supported for instances of this class.
     * @param srcDetector The object used to detect any archive files
     *        in the source directory tree.
     * @param dstDetector The object used to detect archive files
     *        in the destination directory tree.
     * @return {@code true} if and only if the operation succeeded.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     * @see <a href="package-summary.html#third_parties">Third Party
     *      Access using different Archive Detectors</a>
     */
    public boolean archiveCopyAllFrom(
            final java.io.File src,
            final ArchiveDetector srcDetector,
            final ArchiveDetector dstDetector) {
        try {
            Files.copyAll(true, src, this, srcDetector, dstDetector);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Copies this file to the file {@code dst} and tries to preserve
     * all attributes of the source
     * file to the destination file, too.
     * Note that the current implementation only preserves the last
     * modification time.
     * <p>
     * <table border="2" cellpadding="4">
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * <tr>
     *   <td>Preserves file attributes</td>
     *   <td>Best effort</td>
     * </tr>
     * <tr>
     *   <td>Copies directories recursively</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Reads and overwrites special files</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Closes parameter streams</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td><a href="#DDC">Direct Data Copying (DDC)</a></td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written files on failure</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written directories on failure</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Atomic</td>
     *   <td>No</td>
     * </tr>
     * </table>
     *
     * @param dst The destination file. Note that although this just needs to
     *        be a plain {@code java.io.File}, archive entries are only
     *        supported for instances of this class.
     * @return {@code true} if and only if the operation succeeded.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public boolean archiveCopyTo(java.io.File dst) {
        try {
            cp_p(this, dst);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Recursively copies this file or directory to the file or directory
     * {@code dst} and tries to preserve all attributes of the source
     * file to the destination file, too.
     * Note that the current implementation only preserves the last
     * modification time.
     * <p>
     * This version uses the {@link ArchiveDetector} which was used to
     * construct <em>this</em> object to detect any archive files in the
     * source <em>and</em> destination directory trees.
     * <p>
     * <table border="2" cellpadding="4">
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * <tr>
     *   <td>Preserves file attributes</td>
     *   <td>Best effort</td>
     * </tr>
     * <tr>
     *   <td>Copies directories recursively</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Reads and overwrites special files</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Closes parameter streams</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td><a href="#DDC">Direct Data Copying (DDC)</a></td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written files on failure</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written directories on failure</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Atomic</td>
     *   <td>No</td>
     * </tr>
     * </table>
     *
     * @param dst The destination file. Note that although this just needs to
     *        be a plain {@code java.io.File}, archive files and entries
     *        are only supported for instances of this class.
     * @return {@code true} if and only if the operation succeeded.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public boolean archiveCopyAllTo(final java.io.File dst) {
        try {
            Files.copyAll(true, this, dst, detector, detector);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Recursively copies this file or directory to the file or directory
     * {@code dst} and tries to preserve all attributes of the source
     * file to the destination file, too.
     * Note that the current implementation only preserves the last
     * modification time.
     * <p>
     * This version uses the given archive detector to detect any archive
     * files in the source and destination directory trees.
     * This version uses the given archive detector to detect any archive
     * files in the source and destination directory trees.
     * <p>
     * <table border="2" cellpadding="4">
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * <tr>
     *   <td>Preserves file attributes</td>
     *   <td>Best effort</td>
     * </tr>
     * <tr>
     *   <td>Copies directories recursively</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Reads and overwrites special files</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Closes parameter streams</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td><a href="#DDC">Direct Data Copying (DDC)</a></td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written files on failure</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written directories on failure</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Atomic</td>
     *   <td>No</td>
     * </tr>
     * </table>
     *
     * @param dst The destination file. Note that although this just needs to
     *        be a plain {@code java.io.File}, archive files and entries
     *        are only supported for instances of this class.
     * @param detector The object used to detect any archive files
     *        in the source and destination directory trees.
     * @return {@code true} if and only if the operation succeeded.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     * @see <a href="package-summary.html#third_parties">Third Party
     *      Access using different Archive Detectors</a>
     */
    public boolean archiveCopyAllTo(
            final java.io.File dst,
            final ArchiveDetector detector) {
        try {
            Files.copyAll(true, this, dst, detector, detector);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Recursively copies this file or directory to the file or directory
     * {@code dst} and tries to preserve all attributes of the source
     * file to the destination file, too.
     * Note that the current implementation only preserves the last
     * modification time.
     * <p>
     * By using different {@link ArchiveDetector}s for the source and
     * destination, this method can be used to do advanced stuff like
     * unzipping any archive file in the source tree to a plain directory
     * in the destination tree (where {@code srcDetector} could be
     * {@link ArchiveDetector#DEFAULT} and {@code dstDetector} must be
     * {@link ArchiveDetector#NULL}) or changing the charset by configuring
     * a custom {@link DefaultArchiveDetector}.
     * <p>
     * <table border="2" cellpadding="4">
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * <tr>
     *   <td>Preserves file attributes</td>
     *   <td>Best effort</td>
     * </tr>
     * <tr>
     *   <td>Copies directories recursively</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Reads and overwrites special files</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Closes parameter streams</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td><a href="#DDC">Direct Data Copying (DDC)</a></td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written files on failure</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written directories on failure</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Atomic</td>
     *   <td>No</td>
     * </tr>
     * </table>
     *
     * @param dst The destination file. Note that although this just needs to
     *        be a plain {@code java.io.File}, archive files and entries
     *        are only supported for instances of this class.
     * @param srcDetector The object used to detect any archive files
     *        in the source directory tree.
     * @param dstDetector The object used to detect any archive files
     *        in the destination directory tree.
     * @return {@code true} if and only if the operation succeeded.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     * @see <a href="package-summary.html#third_parties">Third Party
     *      Access using different Archive Detectors</a>
     */
    public boolean archiveCopyAllTo(
            final java.io.File dst,
            final ArchiveDetector srcDetector,
            final ArchiveDetector dstDetector) {
        try {
            Files.copyAll(true, this, dst, srcDetector, dstDetector);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Copies the input stream {@code in} to the output stream
     * {@code out}.
     * <p>
     * <table border="2" cellpadding="4">
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * <tr>
     *   <td>Preserves file attributes</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Copies directories recursively</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Reads and overwrites special files</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Closes parameter streams</td>
     *   <td>Always</td>
     * </tr>
     * <tr>
     *   <td><a href="#DDC">Direct Data Copying (DDC)</a></td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written files on failure</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written directories on failure</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Atomic</td>
     *   <td>No</td>
     * </tr>
     * </table>
     *
     * @param in The input stream.
     * @param out The output stream.
     * @throws InputException If copying the data fails because of an
     *         {@code IOException} in the <em>input</em> stream.
     * @throws IOException If copying the data fails because of an
     *         {@code IOException} in the <em>output</em> stream.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public static void cp(final InputStream in, final OutputStream out)
    throws IOException {
        Streams.copy(in, out);
    }

    /**
     * Copies {@code src} to {@code dst}.
     * <p>
     * <table border="2" cellpadding="4">
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * <tr>
     *   <td>Preserves file attributes</td>
     *   <td>None</td>
     * </tr>
     * <tr>
     *   <td>Copies directories recursively</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Reads and overwrites special files</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Closes parameter streams</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td><a href="#DDC">Direct Data Copying (DDC)</a></td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written files on failure</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written directories on failure</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Atomic</td>
     *   <td>No</td>
     * </tr>
     * </table>
     * 
     * @param src The source file. Note that although this just needs to
     *        be a plain {@code java.io.File}, archive entries are only
     *        supported for instances of this class.
     * @param dst The destination file. Note that although this just needs to
     *        be a plain {@code java.io.File}, archive entries are only
     *        supported for instances of this class.
     * @throws FileBusyException If an archive entry cannot get accessed
     *         because the client application is trying to input or output
     *         to the same archive file simultaneously and the respective
     *         archive driver does not support this or the archive file needs
     *         an automatic update which cannot get performed because the
     *         client is still using other open {@link FileInputStream}s or
     *         {@link FileOutputStream}s for other entries in the same archive
     *         file.
     * @throws FileNotFoundException If either the source or the destination
     *         cannot get accessed.
     * @throws InputException If copying the data fails because of an
     *         IOException in the source.
     * @throws IOException If copying the data fails because of an
     *         IOException in the destination.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public static void cp(java.io.File src, java.io.File dst)
    throws IOException {
        Files.copy(false, src, dst);
    }

    /**
     * Copies {@code src} to {@code dst} and tries to preserve
     * all attributes of the source file to the destination file, too.
     * Currently, only the last modification time is preserved.
     * <p>
     * <table border="2" cellpadding="4">
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * <tr>
     *   <td>Preserves file attributes</td>
     *   <td>Best effort</td>
     * </tr>
     * <tr>
     *   <td>Copies directories recursively</td>
     *   <td>No</td>
     * </tr>
     * <tr>
     *   <td>Reads and overwrites special files</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Closes parameter streams</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td><a href="#DDC">Direct Data Copying (DDC)</a></td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written files on failure</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written directories on failure</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Atomic</td>
     *   <td>No</td>
     * </tr>
     * </table>
     * 
     * @param src The source file. Note that although this just needs to
     *        be a plain {@code java.io.File}, archive entries are only
     *        supported for instances of this class.
     * @param dst The destination file. Note that although this just needs to
     *        be a plain {@code java.io.File}, archive entries are only
     *        supported for instances of this class.
     * @throws FileBusyException If an archive entry cannot get accessed
     *         because the client application is trying to input or output
     *         to the same archive file simultaneously and the respective
     *         archive driver does not support this or the archive file needs
     *         an automatic update which cannot get performed because the
     *         client is still using other open {@link FileInputStream}s or
     *         {@link FileOutputStream}s for other entries in the same archive
     *         file.
     * @throws FileNotFoundException If either the source or the destination
     *         cannot get accessed.
     * @throws InputException If copying the data fails because of an
     *         IOException in the source.
     * @throws IOException If copying the data fails because of an
     *         IOException in the destination.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public static void cp_p(java.io.File src, java.io.File dst)
    throws IOException {
        Files.copy(true, src, dst);
    }

    /**
     * Copies the input stream {@code in} to this file or
     * entry in an archive file without closing the input stream.
     * <p>
     * <table border="2" cellpadding="4">
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * <tr>
     *   <td>Preserves file attributes</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Copies directories recursively</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Reads and overwrites special files</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Closes parameter streams</td>
     *   <td>Never</td>
     * </tr>
     * <tr>
     *   <td><a href="#DDC">Direct Data Copying (DDC)</a></td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written files on failure</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written directories on failure</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Atomic</td>
     *   <td>No</td>
     * </tr>
     * </table>
     *
     * @param in The input stream.
     * @return {@code true} if and only if the operation succeeded.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public boolean catFrom(final InputStream in) {
        try {
            final OutputStream out = detector.createFileOutputStream(this, false);
            try {
                try {
                    Streams.cat(in, out);
                } finally {
                    out.close();
                }
                return true;
            } catch (IOException ex) {
                delete();
                throw ex;
            }
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Copies this file or entry in an archive file to the output stream
     * {@code out} without closing it.
     * <p>
     * <table border="2" cellpadding="4">
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * <tr>
     *   <td>Preserves file attributes</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Copies directories recursively</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Reads and overwrites special files</td>
     *   <td>Yes</td>
     * </tr>
     * <tr>
     *   <td>Closes parameter streams</td>
     *   <td>Never</td>
     * </tr>
     * <tr>
     *   <td><a href="#DDC">Direct Data Copying (DDC)</a></td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written files on failure</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written directories on failure</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Atomic</td>
     *   <td>No</td>
     * </tr>
     * </table>
     *
     * @param out The output stream.
     * @return {@code true} if and only if the operation succeeded.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public boolean catTo(final OutputStream out) {
        try {
            final InputStream in = detector.createFileInputStream(this);
            try {
                Streams.cat(in, out);
            } finally {
                in.close();
            }
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Copies all data from one stream to another without closing them.
     * <p>
     * <table border="2" cellpadding="4">
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * <tr>
     *   <td>Preserves file attributes</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Copies directories recursively</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Reads and overwrites special files</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Closes parameter streams</td>
     *   <td>Never</td>
     * </tr>
     * <tr>
     *   <td><a href="#DDC">Direct Data Copying (DDC)</a></td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written files on failure</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Deletes partial written directories on failure</td>
     *   <td>n/a</td>
     * </tr>
     * <tr>
     *   <td>Atomic</td>
     *   <td>No</td>
     * </tr>
     * </table>
     *
     * @param in The input stream.
     * @param out The output stream.
     * @throws InputException If copying the data fails because of an
     *         IOException in the input stream.
     * @throws IOException If copying the data fails because of an
     *         IOException in the output stream.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public static void cat(final InputStream in, final OutputStream out)
    throws IOException {
        Streams.cat(in, out);
    }
}
