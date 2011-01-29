/*
 * Copyright (C) 2004-2011 Schlichtherle IT Services
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

import de.schlichtherle.truezip.fs.FsSyncWarningException;
import de.schlichtherle.truezip.io.Paths.Splitter;
import de.schlichtherle.truezip.io.Paths;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.fs.FsPath;
import de.schlichtherle.truezip.fs.FsMountPoint;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.fs.FsEntry;
import de.schlichtherle.truezip.fs.FsFilteringManager;
import de.schlichtherle.truezip.fs.FsSyncException;
import de.schlichtherle.truezip.fs.FsSyncOption;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;
import net.jcip.annotations.Immutable;

import static de.schlichtherle.truezip.fs.FsEntry.*;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import static de.schlichtherle.truezip.fs.FsManager.*;
import static de.schlichtherle.truezip.fs.FsSyncOption.*;
import static de.schlichtherle.truezip.fs.FsUriModifier.*;
import static de.schlichtherle.truezip.entry.Entry.Size.*;
import static de.schlichtherle.truezip.entry.Entry.Type.*;
import static de.schlichtherle.truezip.file.TIO.*;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;

/**
 * A replacement for its subclass which provides transparent read/write access
 * to archive files and their entries as if they were (virtual) directories and
 * files.
 * This class extends {@link File} so that it can be used with a
 * {@link FileSystemView}.
 * <p>
 * <b>Warning:</b>The classes in this package access and manipulate archive
 * files as external resources and may cache some of their state in memory
 * and temporary files. Third parties must not concurrently access these
 * archive files unless some precautions have been taken!
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
 *   <td>{@code <i>?</i>}</td>
 * </tr>
 * <tr>
 *   <td><i>archive.zip</i></td>
 *   <td>False positive: Regular file</td>
 *   <td>{@code true}</td>
 *   <td>{@code false}</td>
 *   <td>{@code true}</td>
 *   <td>{@code true}</td>
 *   <td>{@code <i>?</i>}</td>
 * </tr>
 * <tr>
 *   <td><i>archive.zip</i></td>
 *   <td>False positive: Regular special file</td>
 *   <td>{@code true}</td>
 *   <td>{@code false}</td>
 *   <td>{@code false}</td>
 *   <td>{@code true}</td>
 *   <td>{@code <i>?</i>}</td>
 * </tr>
 * <tr>
 *   <td><i>archive.zip</i></td>
 *   <td>TFile or directory does not exist</td>
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
 *  <td>{@code <i>?</i>}</td>
 * </tr>
 * <tr>
 *   <td><i>archive.tzp</i></td>
 *   <td>False positive: Regular directory</td>
 *   <td>{@code true}</td>
 *   <td>{@code true}</td>
 *   <td>{@code false}</td>
 *   <td>{@code true}</td>
 *   <td>{@code <i>?</i>}</td>
 * </tr>
 * <tr>
 *   <td><i>archive.tzp</i></td>
 *   <td>False positive: Regular file</td>
 *   <td>{@code true}</td>
 *   <td>{@code false}</td>
 *   <td>{@code true}</td>
 *   <td>{@code true}</td>
 *   <td>{@code <i>?</i>}</td>
 * </tr>
 * <tr>
 *   <td><i>archive.tzp</i></td>
 *   <td>False positive: Regular special file</td>
 *   <td>{@code true}</td>
 *   <td>{@code false}</td>
 *   <td>{@code false}</td>
 *   <td>{@code true}</td>
 *   <td>{@code <i>?</i>}</td>
 * </tr>
 * <tr>
 *   <td><i>archive.tzp</i></td>
 *   <td>TFile or directory does not exist</td>
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
 *   <td>TFile or directory does not exist</td>
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
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
@Immutable
@edu.umd.cs.findbugs.annotations.SuppressWarnings("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
public final class TFile extends File {

    //
    // Static fields:
    //

    private static final long serialVersionUID = 3617072259051821745L;

    /** The file system roots. */
    private static final Set<File> roots
    		= new TreeSet<File>(Arrays.asList(listRoots()));

    private static boolean lenient = true;

    private static TArchiveDetector
            defaultDetector = TDefaultArchiveDetector.ALL;

    //
    // Instance fields:
    //

    /**
     * The delegate is used to implement the behaviour of the file system
     * operations in case this instance represents neither an archive file
     * nor an entry in an archive file.
     * If this instance is constructed from another {@code File}
     * instance, then this field is initialized with that instance.
     * <p>
     * This enables federation of file system implementations and is essential
     * to enable the broken implementation in
     * {@link javax.swing.JFileChooser} to browse archive files.
     */
    private transient File delegate; // TODO: Revision this: Still required?

    private transient TArchiveDetector detector;
    private transient @Nullable TFile innerArchive;
    private transient @Nullable TFile enclArchive;
    private transient @Nullable FsEntryName enclEntryName;

    /**
     * This refers to the file system controller if and only if this file
     * is located within a federated file system, otherwise it's {@code null}.
     * This field should be considered to be {@code final}!
     *
     * @see #readObject
     */
    private transient volatile @Nullable FsController<?> controller;

    //
    // Constructors and helper methods:
    //

    /**
     * Copy constructor.
     * Equivalent to {@link #TFile(File, TArchiveDetector)
     * TFile(template, getDefaultArchiveDetector())}.
     */
    public TFile(File file) {
        this(file, defaultDetector);
    }

    /**
     * Constructs a new {@code TFile} instance which may use the given
     * {@link TArchiveDetector} to detect any archive files in its path.
     * 
     * @param file The file to decorate. If this is an instance
     *        of this class, its fields are copied and the
     *        {@code detector} parameter is not used to scan the path.
     * @param detector The object to use for the detection of any archive files
     *        in the path if necessary.
     *        This parameter is ignored if and only if {@code file} is an
     *        instance of this class.
     *        Otherwise, if it's {@code null}, the
     *        {@link #getDefaultArchiveDetector() default archive detector} is
     *        used.
     */
    public TFile(   final File file,
                    final @CheckForNull TArchiveDetector detector) {
        super(file.getPath());

        if (file instanceof TFile) {
            final TFile tfile = (TFile) file;
            this.delegate = tfile.delegate;
            this.detector = tfile.detector;
            this.enclArchive = tfile.enclArchive;
            this.enclEntryName = tfile.enclEntryName;
            this.innerArchive = tfile.isArchive() ? this : tfile.innerArchive;
            this.controller = tfile.controller;
        } else {
            this.delegate = file;
            this.detector = null != detector ? detector : defaultDetector;
            scan(null);
        }

        assert invariants();
    }

    /**
     * Equivalent to {@link #TFile(String, TArchiveDetector)
     * TFile(path, getDefaultArchiveDetector())}.
     */
    public TFile(String path) {
        this(path, defaultDetector);
    }

    /**
     * Constructs a new {@code TFile} instance which uses the given
     * {@link TArchiveDetector} to detect any archive files in its path.
     *
     * @param path The path of the file.
     * @param detector The object to use for the detection of any archive files
     *        in the path.
     *        If this is {@code null}, the
     *        {@link #getDefaultArchiveDetector() default archive detector} is
     *        used.
     */
    public TFile(   final String path,
                    final @CheckForNull TArchiveDetector detector) {
        super(path);

        this.delegate = new File(path);
        this.detector = null != detector ? detector : defaultDetector;
        scan(null);

        assert invariants();
    }

    /**
     * Equivalent to {@link #TFile(String, String, TArchiveDetector)
     * TFile(parent, child, getDefaultArchiveDetector())}.
     */
    public TFile(String parent, String member) {
        this(parent, member, defaultDetector);
    }

    /**
     * Constructs a new {@code TFile} instance which uses the given
     * {@link TArchiveDetector} to detect any archive files in its path.
     *
     * @param parent The parent path as a {@link String}.
     * @param member The child path as a {@link String}.
     * @param detector The object to use for the detection of any archive files
     *        in the path.
     *        If this is {@code null}, the
     *        {@link #getDefaultArchiveDetector() default archive detector} is
     *        used.
     */
    public TFile(
            final String parent,
            final String member,
            final @CheckForNull TArchiveDetector detector) {
        super(parent, member);

        this.delegate = new File(parent, member);
        this.detector = null != detector ? detector : defaultDetector;
        scan(null);

        assert invariants();
    }

    /**
     * Equivalent to {@link #TFile(File, String, TArchiveDetector)
     * TFile(parent, child, null)}.
     *
     * @param parent The parent directory as a {@code TFile} instance.
     *        If this parameter is an instance of this class, its
     *        {@code TArchiveDetector} is used to detect any archive files
     *        in the path of this {@code TFile} instance.
     *        Otherwise, the {@link #getDefaultArchiveDetector()} is used.
     *        This is used in order to make this {@code TFile} instance
     *        behave as if it had been created by one of the {@link #listFiles}
     *        methods called on {@code parent} instead.
     * @param member The child path as a {@link String}.
     */
    public TFile(File parent, String member) {
        this(parent, member, null);
    }

    /**
     * Constructs a new {@code TFile} instance which uses the given
     * {@link TArchiveDetector} to detect any archive files in its path
     * and configure their parameters.
     *
     * @param parent The parent directory as a {@code TFile} instance.
     * @param member The child path as a {@link String}.
     * @param detector The object to use for the detection of any archive files
     *        in the path.
     *        If this is {@code null} and {@code parent} is an
     *        instance of this class, the archive detector is copied from
     *        {@code parent}.
     *        If this is {@code null} and {@code parent} is
     *        <em>not</em> an instance of this class, the
     *        {@link #getDefaultArchiveDetector() default archive detector}
     *        is used.
     */
    public TFile(
            final File parent,
            final String member,
            final @CheckForNull TArchiveDetector detector) {
        super(parent, member);

        this.delegate = new File(parent, member);
        if (parent instanceof TFile) {
            final TFile tparent = (TFile) parent;
            this.detector = null != detector ? detector : tparent.detector;
            scan(tparent);
        } else {
            this.detector = null != detector ? detector : defaultDetector;
            scan(null);
        }

        assert invariants();
    }

    /**
     * Constructs a new {@code TFile} instance from the given
     * {@code uri}.
     * This method behaves similar to the super class constructor
     * {@link File#File(URI) new File(uri)} with the following amendment:
     * If the URI matches the pattern
     * {@code (scheme:)*file:(path!/)*entry}, then the
     * constructed file object treats the URI like an entry in the federated
     * file system of the type named "scheme".
     * <p>
     * For example, in a Java application which is running from a JAR in the
     * local file system you could use this constructor to arbitrarily access
     * (and modify) any resource in the JAR file from which the application is
     * currently running by using the following simple expression:
     * {@code new TFile(getClass().getResource(resource).toURI())}.
     * </pre>
     * Note that the newly created {@code TFile} instance uses the
     * {@link #getDefaultArchiveDetector() default archive detector}.
     *
     * @param  uri an absolute, hierarchical URI with a scheme supported by the
     *         {@link #getDefaultArchiveDetector() default archive detector}
     *         with the explained syntax constraints.
     *         Its authority, query, and fragment components must be undefined.
     * @throws IllegalArgumentException if the given URI does not conform to
     *         its syntax constraints.
     */
    public TFile(URI uri) {
        this(FsPath.create(uri, CANONICALIZE), defaultDetector);
    }

    public TFile(FsPath path) {
        this(path, defaultDetector);
    }

    @SuppressWarnings("LeakingThisInConstructor")
    private TFile(FsPath path, TArchiveDetector detector) {
        super(path.hierarchicalize().getUri());
        parse(path, detector);
    }

    private void parse( final FsPath path,
                        final TArchiveDetector detector) {
        this.delegate = new File(super.getPath());
        this.detector = detector;

        final FsMountPoint mountPoint = path.getMountPoint();
        final FsPath mountPointPath = mountPoint.getPath();
        final FsEntryName entryName;

        if (null == mountPointPath) {
            assert !path.getUri().isOpaque();
            this.enclArchive = null;
            this.enclEntryName = null;
            this.innerArchive = null;
        } else if ((entryName = path.getEntryName()).isRoot()) {
            assert path.getUri().isOpaque();
            if (mountPointPath.getUri().isOpaque()) {
                this.enclArchive
                        = new TFile(mountPointPath.getMountPoint(), detector);
                this.enclEntryName = mountPointPath.getEntryName();
            } else {
                this.enclArchive = null;
                this.enclEntryName = null;
            }
            this.innerArchive = this;
        } else {
            assert path.getUri().isOpaque();
            this.enclArchive = new TFile(mountPoint, detector);
            this.enclEntryName = entryName;
            this.innerArchive = this.enclArchive;
        }

        assert invariants();
    }

    @SuppressWarnings("LeakingThisInConstructor")
    private TFile(  final FsMountPoint mountPoint,
                    TArchiveDetector detector) {
        super(mountPoint.hierarchicalize().getUri());

        this.delegate = new File(super.getPath());
        this.detector = detector;

        final FsPath mountPointPath = mountPoint.getPath();

        if (null == mountPointPath) {
            assert !mountPoint.getUri().isOpaque();
            this.enclArchive = null;
            this.enclEntryName = null;
            this.innerArchive = null;
        } else {
            assert mountPoint.getUri().isOpaque();
            if (mountPointPath.getUri().isOpaque()) {
                this.enclArchive
                        = new TFile(mountPointPath.getMountPoint(), detector);
                this.enclEntryName = mountPointPath.getEntryName();
            } else {
                this.enclArchive = null;
                this.enclEntryName = null;
            }
            this.innerArchive = this;
        }

        assert invariants();
    }

    @SuppressWarnings("LeakingThisInConstructor")
    private TFile(  final File delegate,
                    final @CheckForNull TFile innerArchive,
                    final TArchiveDetector detector) {
        super(delegate.getPath());

        this.delegate = delegate;

        final String path = delegate.getPath();
        if (null != innerArchive) {
            final int innerArchivePathLength
                    = innerArchive.getPath().length();
            if (path.length() == innerArchivePathLength) {
                this.detector = innerArchive.detector;
                this.innerArchive = this;
                this.enclArchive = innerArchive.enclArchive;
                this.enclEntryName = innerArchive.enclEntryName;
            } else {
                this.detector = detector;
                this.innerArchive = this.enclArchive = innerArchive;
                this.enclEntryName = FsEntryName.create(
                        path.substring(innerArchivePathLength + 1) // cut off leading separatorChar
                            .replace(separatorChar, SEPARATOR_CHAR),
                        null,
                        CANONICALIZE);
            }
        } else {
            this.detector = detector;
        }

        assert invariants();
    }

    /**
     * Initialize this file object by scanning its path for archive
     * files, using the given {@code ancestor} file (i.e. a direct or
     * indirect parent file) if any.
     * {@code delegate} and {@code detector} must already be initialized!
     * Must not be called to re-initialize this object!
     */
    private void scan(final @CheckForNull TFile ancestor) {
        final String path = super.getPath();
        assert ancestor == null || path.startsWith(ancestor.getPath());
        assert delegate.getPath().equals(path);
        assert null != detector;

        final StringBuilder enclEntryNameBuf = new StringBuilder(path.length());
        scan(ancestor, detector, 0, path, enclEntryNameBuf, new Splitter(separatorChar));
        enclEntryName = 0 < enclEntryNameBuf.length()
                ? FsEntryName.create(enclEntryNameBuf.toString(), null, CANONICALIZE)
                : null;
    }

    private void scan(
            @CheckForNull TFile ancestor,
            TArchiveDetector detector,
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
        final String member = splitter.getMemberName();

        if (member.length() == 0 || ".".equals(member)) {
            // Fall through.
        } else if ("..".equals(member)) {
            skip++;
        } else if (skip > 0) {
            skip--;
        } else {
            if (ancestor != null) {
                final int pathLen = path.length();
                final int ancestorPathLen = ancestor.getPath().length();
                if (pathLen == ancestorPathLen) {
                    // Found ancestor: Process it and stop.
                    // Using the following assertion would be wrong:
                    // enclEntryNameBuf may indeed be null if the full path
                    // ends with just a single dot after the last separator,
                    // i.e. the base name is ".", indicating the current
                    // directory.
                    // assert enclEntryNameBuf.getLength() > 0;
                    enclArchive = ancestor.innerArchive;
                    if (!ancestor.isArchive()) {
                        if (ancestor.isEntry()) {
                            if (enclEntryNameBuf.length() > 0) {
                                enclEntryNameBuf.insert(0, '/');
                                enclEntryNameBuf.insert(0, ancestor.enclEntryName.getPath());
                            } else { // TODO: Simplify this!
                                // Example: new TFile(new TFile(new TFile("archive.zip"), "entry"), ".")
                                assert enclArchive == ancestor.enclArchive;
                                enclEntryNameBuf.append(ancestor.enclEntryName.getPath());
                            }
                        } else {
                            assert enclArchive == null;
                            enclEntryNameBuf.setLength(0);
                        }
                    } else if (enclEntryNameBuf.length() <= 0) { // TODO: Simplify this!
                        // Example: new TFile(new TFile("archive.zip"), ".")
                        assert enclArchive == ancestor;
                        innerArchive = this;
                        enclArchive = ancestor.enclArchive;
                        if (ancestor.enclEntryName != null)
                            enclEntryNameBuf.append(ancestor.enclEntryName.getPath());
                    }
                    if (innerArchive != this)
                        innerArchive = enclArchive;
                    return;
                } else if (pathLen < ancestorPathLen) {
                    detector = ancestor.detector;
                    ancestor = ancestor.enclArchive;
                }
            }

            final boolean isArchive = detector.getScheme(path) != null;
            if (enclEntryNameBuf.length() > 0) {
                if (isArchive) {
                    enclArchive = new TFile(path, detector); // use the same detector for the parent directory
                    if (innerArchive != this)
                        innerArchive = enclArchive;
                    return;
                }
                enclEntryNameBuf.insert(0, '/');
                enclEntryNameBuf.insert(0, member);
            } else {
                if (isArchive)
                    innerArchive = this;
                enclEntryNameBuf.append(member);
            }
        }

        scan(ancestor, detector, skip, parent, enclEntryNameBuf, splitter);
    }

    private Object writeReplace() throws ObjectStreamException {
        return getAbsoluteFile();
    }

    private void writeObject(ObjectOutputStream out)
    throws IOException {
        out.writeObject(toURI());
    }

    private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        parse(  FsPath.create((URI) in.readObject(), CANONICALIZE),
                defaultDetector);
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
        assert null != delegate;
        assert !(delegate instanceof TFile);
        assert delegate.getPath().equals(super.getPath());
        assert null != detector;
        assert (innerArchive != null) == (getInnerEntryName() != null);
        assert (enclArchive != null) == (enclEntryName != null);
        assert this != enclArchive;
        assert (this == innerArchive)
                ^ (innerArchive == enclArchive && null == controller);
        assert null == enclArchive
                || Paths.contains(  enclArchive.getPath(),
                                    delegate.getParentFile().getPath(),
                                    separatorChar)
                    && 0 < enclEntryName.toString().length();
        return true;
    }

    //
    // Methods:
    //

    /**
     * Synchronizes all uncommitted changes to the contents of all federated
     * file systems (i.e. archive files) to their respective parent file system.
     *
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         occured throughout the course of this method.
     *         This implies that the respective parent file system has been
     *         updated with constraints, such as a failure to set the last
     *         modification time of the entry for the federated file system
     *         (i.e. archive file) in its parent file system.
     * @throws FsSyncException if any error condition occured throughout the
     *         course of this method.
     *         This implies loss of data!
     * @throws IllegalArgumentException if the combination of options is
     *         illegal.
     */
    private static void sync(BitField<FsSyncOption> options)
    throws FsSyncException {
        TIO.MANAGER.sync(options);
    }

    /**
     * Similar to {@link #sync(BitField) sync(options)},
     * but updates only the given {@code archive} and all its member file
     * systems.
     * <p>
     * If a client application needs to sync an individual federated file
     * system, the following idiom can be used:
     * <pre>{@code
     * if (file.isArchive() && file.getEnclArchive() == null) // filter top level federated file system
     *   if (file.isDirectory()) // ignore false positives
     *     TFile.sync(file); // update federated file system and all its members
     * }</pre>
     * Again, this will also sync all federated file systems which are
     * located within the file system referred to by {@code file}.
     *
     * @param  archive a top level federated file system.
     * @throws IllegalArgumentException If {@code archive} is not a top level
     *         federated file system or the combination of options is illegal.
     * @see    #sync(BitField)
     */
    private static void sync(
            final TFile archive,
            final BitField<FsSyncOption> options)
    throws FsSyncException {
        if (!archive.isArchive())
            throw new IllegalArgumentException(archive.getPath() + " (not a federated file system)");
        if (null != archive.getEnclArchive())
            throw new IllegalArgumentException(archive.getPath() + " (not a top level federated file system)");
        new FsFilteringManager(
                TIO.MANAGER,
                archive .getController()
                        .getModel()
                        .getMountPoint())
                    .sync(options);
    }

    /**
     * Equivalent to {@code sync(FsController.UMOUNT)}.
     *
     * @see #sync(BitField)
     */
    public static void umount()
    throws FsSyncException {
        sync(UMOUNT);
    }

    /**
     * Equivalent to {@code
        sync(   BitField.of(FsSyncOption.CLEAR_CACHE)
                .set(FsSyncOption.FORCE_CLOSE_INPUT, closeStreams)
                .set(FsSyncOption.FORCE_CLOSE_OUTPUT, closeStreams))
     * }.
     *
     * @see #sync(BitField)
     */
    public static void umount(boolean closeStreams)
    throws FsSyncException {
        sync(   BitField.of(CLEAR_CACHE)
                .set(FORCE_CLOSE_INPUT, closeStreams)
                .set(FORCE_CLOSE_OUTPUT, closeStreams));
    }

    /**
     * Equivalent to {@code
        sync(   BitField.of(FsSyncOption.CLEAR_CACHE)
                .set(FsSyncOption.WAIT_CLOSE_INPUT, waitForInputStreams)
                .set(FsSyncOption.FORCE_CLOSE_INPUT, closeInputStreams)
                .set(FsSyncOption.WAIT_CLOSE_OUTPUT, waitForOutputStreams)
                .set(FsSyncOption.FORCE_CLOSE_OUTPUT, closeOutputStreams))
     * }.
     *
     * @see #sync(BitField)
     */
    public static void umount(
            boolean waitForInputStreams, boolean closeInputStreams,
            boolean waitForOutputStreams, boolean closeOutputStreams)
    throws FsSyncException {
        sync(   BitField.of(CLEAR_CACHE)
                .set(WAIT_CLOSE_INPUT, waitForInputStreams)
                .set(FORCE_CLOSE_INPUT, closeInputStreams)
                .set(WAIT_CLOSE_OUTPUT, waitForOutputStreams)
                .set(FORCE_CLOSE_OUTPUT, closeOutputStreams));
    }

    /**
     * Equivalent to {@code
        sync(archive, BitField.of(FsController.UMOUNT))
     * }.
     *
     * @see #sync(TFile, BitField)
     */
    public static void umount(TFile archive)
    throws FsSyncException {
        sync(archive, UMOUNT);
    }

    /**
     * Equivalent to {@code
        sync(   archive,
                BitField.of(FsSyncOption.CLEAR_CACHE)
                .set(FsSyncOption.FORCE_CLOSE_INPUT, closeStreams)
                .set(FsSyncOption.FORCE_CLOSE_OUTPUT, closeStreams))
     * }.
     *
     * @see #sync(TFile, BitField)
     */
    public static void umount(TFile archive, boolean closeStreams)
    throws FsSyncException {
        sync(   archive,
                BitField.of(CLEAR_CACHE)
                .set(FORCE_CLOSE_INPUT, closeStreams)
                .set(FORCE_CLOSE_OUTPUT, closeStreams));
    }

    /**
     * Equivalent to {@code
        sync(   archive,
                BitField.of(FsSyncOption.CLEAR_CACHE)
                .set(FsSyncOption.WAIT_CLOSE_INPUT, waitForInputStreams)
                .set(FsSyncOption.FORCE_CLOSE_INPUT, closeInputStreams)
                .set(FsSyncOption.WAIT_CLOSE_OUTPUT, waitForOutputStreams)
                .set(FsSyncOption.FORCE_CLOSE_OUTPUT, closeOutputStreams))
     * }.
     *
     * @see #sync(TFile, BitField)
     */
    public static void umount(TFile archive,
            boolean waitForInputStreams, boolean closeInputStreams,
            boolean waitForOutputStreams, boolean closeOutputStreams)
    throws FsSyncException {
        sync(   archive,
                BitField.of(CLEAR_CACHE)
                .set(WAIT_CLOSE_INPUT, waitForInputStreams)
                .set(FORCE_CLOSE_INPUT, closeInputStreams)
                .set(WAIT_CLOSE_OUTPUT, waitForOutputStreams)
                .set(FORCE_CLOSE_OUTPUT, closeOutputStreams));
    }

    /**
     * Returns the value of the class property {@code lenient}.
     * By default, this is the inverse of the boolean system property
     * {@code de.schlichtherle.truezip.file.strict}.
     * In other words, this returns {@code true} unless you map the
     * system property {@code de.schlichtherle.truezip.file.strict}
     * to {@code true} or call {@link #setLenient(boolean) setLenient(false)}.
     *
     * @see #setLenient
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
     * {@code new TFile(&quot;a/outer.zip/b/inner.zip&quot;).mkdirs()}
     * before it could actually create the innermost &quot;c&quot; entry as a file
     * or directory.
     * <p>
     * More formally, before you can access an entry in the federated file
     * system, all its parent directories must exist, including archive
     * files. This emulates the behaviour of native file systems.
     * <p>
     * If this class property is map to {@code true} however, then
     * any missing parent directories (including archive files) up to the
     * outermost archive file (&quot;outer.zip&quot;) are created on the fly when using
     * operations to create the innermost element of the path (&quot;c&quot;).
     * <p>
     * This allows applications to succeed when doing this:
     * {@code new TFile(&quot;a/outer.zip/b/inner.zip/c&quot;).createNewFile()},
     * or that:
     * {@code new TFileOutputStream(&quot;a/outer.zip/b/inner.zip/c&quot;)}.
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
     * {@link #umount} is called.
     * In order to prevent this, TrueZIP's archive entry streams have a
     * {@link Object#finalize()} method which closes an archive entry stream
     * if its garbage collected.
     * <p>
     * Now if this class property is map to {@code false}, then
     * TrueZIP maintains a hard reference to all archive entry streams
     * until {@link #umount} is called, which will deal
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
     * {@link #umount} is called.
     * The finalize() method will then close these archive entry streams,
     * which exempts them, from triggering an
     * {@code ArchiveBusy(Warning)?Exception} on the next call to
     * {@link #umount}.
     * However, closing an archive entry output stream this way may result
     * in loss of buffered data, so it's only a workaround for this issue.
     * <p>
     * Note that for the setting of this class property to take effect, any
     * change must be made before an archive is first accessed.
     * The setting will then persist until the archive is reset by the next
     * call to {@link #umount}.
     * </li>
     * </ol>
     *
     * @see #isLenient()
     */
    public static void setLenient(boolean lenient) {
        TFile.lenient = lenient;
    }

    /**
     * Returns the {@link TArchiveDetector} to use if no archive detector is
     * explicitly passed to the constructor of a {@code TFile} instance.
     * <p>
     * This class property is initially set to
     * {@link TDefaultArchiveDetector#ALL}
     *
     * @see #setDefaultArchiveDetector
     */
    public static TArchiveDetector getDefaultArchiveDetector() {
        return defaultDetector;
    }

    /**
     * Sets the {@link TArchiveDetector} to use if no archive detector is
     * explicitly passed to the constructor of a {@code TFile} instance.
     * When a new {@code TFile} instance is constructed, but no archive
     * detector parameter is provided, then the value of this class property
     * is used.
     * So changing the value of this class property affects only subsequently
     * constructed {@code TFile} instances - not any existing ones.
     *
     * @param detector the {@link TArchiveDetector} to use for subsequently
     *        constructed {@code TFile} instances if no archive detector is
     *        explicitly passed to the constructor
     * @see   #getDefaultArchiveDetector()
     */
    public static void setDefaultArchiveDetector(TArchiveDetector detector) {
        if (null == detector)
            throw new NullPointerException();
        TFile.defaultDetector = detector;
    }

    /**
     * Returns the first parent directory (starting from this file) which is
     * <em>not</em> an archive file or a file located in an archive file.
     */
    public @Nullable TFile getNonArchivedParentFile() {
        final TFile enclArchive = this.enclArchive;
        return null != enclArchive
                ? enclArchive.getNonArchivedParentFile()
                : getParentFile();
    }

    @Override
    public @Nullable String getParent() {
        return delegate.getParent();
    }

    @Override
    public @Nullable TFile getParentFile() {
        final File parent = delegate.getParentFile();
        if (parent == null)
            return null;

        if (null != enclArchive
                && enclArchive.getPath().length() == parent.getPath().length()) {
            assert enclArchive.getPath().equals(parent.getPath());
            return enclArchive;
        }

        // This is not only called for performance reasons, but also in order
        // to prevent the parent path from being rescanned for archive files
        // with a different detector, which could trigger an update and
        // reconfiguration of the respective file system controller!
        return new TFile(parent, enclArchive, detector);
    }

    @Override
    public TFile getAbsoluteFile() {
        String p = getAbsolutePath();
        return p.equals(getPath()) ? this : new TFile(p, detector);
    }

    @Override
    public String getAbsolutePath() {
        return delegate.getAbsolutePath();
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
    public TFile getNormalizedAbsoluteFile() {
        String p = getNormalizedAbsolutePath();
        return p.equals(getPath()) ? this : new TFile(p, detector);
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
        return Paths.normalize(getAbsolutePath(), separatorChar);
    }

    /**
     * Removes any {@code "."} and {@code ".."} directories from the path name
     * wherever possible.
     *
     * @return If this file is already normalized, it is returned.
     *         Otherwise a new instance of this class is returned.
     */
    public TFile getNormalizedFile() {
        String p = getNormalizedPath();
        return p.equals(getPath()) ? this : new TFile(p, detector);
    }

    /**
     * Removes any redundant {@code "."}, {@code ".."} directories from the
     * path name.
     *
     * @return The normalized path of this file as a {@link String}.
     */
    public String getNormalizedPath() {
        return Paths.normalize(getPath(), separatorChar);
    }

    @Override
    public TFile getCanonicalFile() throws IOException {
        String p = getCanonicalPath();
        return p.equals(getPath()) ? this : new TFile(p, detector);
    }

    @Override
    public String getCanonicalPath() throws IOException {
        return delegate.getCanonicalPath();
    }

    /**
     * This convenience method simply returns the canonical form of this
     * abstract path or the normalized absolute form if resolving the
     * prior fails.
     *
     * @return The canonical or absolute path of this file as an
     *         instance of this class.
     */
    public final TFile getCanOrAbsFile() {
        String p = getCanOrAbsPath();
        return p.equals(getPath()) ? this : new TFile(p, detector);
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
        try {
            return getCanonicalPath();
        } catch (IOException ex) {
            return Paths.normalize(getAbsolutePath(), separatorChar);
        }
    }

    /**
     * Returns {@code true} if and only if the path represented by this
     * instance denotes an archive file.
     * Whether or not this is true solely depends on the
     * {@link TArchiveDetector} which was used to construct this {@code TFile}
     * instance.
     * <p>
     * Please note that no file system tests are performed!
     * If a client application needs to know whether this file really isExisting
     * as an archive file in the file system (and the correct password has
     * been entered in case it's a RAES encrypted ZIP file), it should
     * subsequently call {@link #isDirectory}, too.
     * This will automount the (virtual) file system from the archive file and
     * return {@code true} if and only if it's a valid archive file.
     *
     * @see <a href="#false_positives">Identifying Archive Paths and False Positives</a>
     * @see #isDirectory
     * @see #isEntry
     */
    public boolean isArchive() {
        return this == innerArchive;
    }

    /**
     * Returns {@code true} if and only if the path represented by this
     * instance names an archive file as an ancestor.
     * Whether or not this is true solely depends on the
     * {@link TArchiveDetector} which was used to construct this {@code TFile}
     * instance.
     * <p>
     * Please note that no tests on the file's true state are performed!
     * If you need to know whether this file is really an entry in an archive
     * file (and the correct password has been entered in case it's a RAES
     * encrypted ZIP file), you should call
     * {@link #getParentFile getParentFile()}.{@link #isDirectory isDirectory()}, too.
     * This will automount the (virtual) file system from the archive file and
     * return {@code true} if and only if it's a valid archive file.
     *
     * @see #isArchive
     */
    public boolean isEntry() {
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
     * a {@code TFile} instance which again could be an entry within
     * another archive file.
     */
    public @CheckForNull TFile getInnerArchive() {
        return innerArchive;
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
    public @Nullable String getInnerEntryName() {
        return this == innerArchive
                ? ROOT.getPath()
                : null == enclEntryName
                    ? null
                    : enclEntryName.getPath();
    }

    @Nullable FsEntryName getInnerEntryName0() {
        return this == innerArchive ? ROOT : enclEntryName;
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
     * a {@code TFile} instance which again could be an entry within
     * another archive file.
     */
    public @CheckForNull TFile getEnclArchive() {
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
    public @Nullable String getEnclEntryName() {
        return null == enclEntryName ? null : enclEntryName.getPath();
    }

    final @Nullable FsEntryName getEnclEntryName0() {
        return enclEntryName;
    }

    /**
     * Returns the {@link TArchiveDetector} that was used to construct this
     * object - never {@code null}.
     */
    public final TArchiveDetector getArchiveDetector() {
        return detector;
    }

    /**
     * Returns the decorated file object.
     * <p>
     * If this file instance has been created from a {@link FileSystemView},
     * the decorated file object may be an instance of a sibling class, i.e.
     * another sub-class of {@link File}.
     *
     * @return An instance of the {@link File File} class or any of its
     *         sub-classes, but never an instance of this class and never
     *         {@code null}.
     */
    public File getFile() {
        return delegate;
    }

    /**
     * Returns a file system controller if and only if the path denotes an
     * archive file, or {@code null} otherwise.
     */
    @Nullable FsController<?> getController() {
        if (this != innerArchive || null != controller)
            return controller;
        assert this == innerArchive;
        final String path = Paths.normalize(delegate.getPath(), separatorChar);
        final FsScheme scheme = detector.getScheme(path);
        if (null == scheme)
            throw new ServiceConfigurationError(
                    "unknown file system scheme for path \""
                    + path
                    + "\"! Check run-time class path configuration.");
        final FsMountPoint mountPoint;
        try {
            mountPoint = new FsMountPoint(scheme, null == enclArchive
                    ? new FsPath(   delegate)
                    : new FsPath(   enclArchive .getController()
                                                .getModel()
                                                .getMountPoint(),
                                    enclEntryName));
        } catch (URISyntaxException ex) {
            throw new AssertionError(ex);
        }
        return controller = TIO.MANAGER.getController(mountPoint, detector);
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
    public boolean isParentOf(final File file) {
        return TIO.contains(this, file);
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
    public boolean contains(File file) {
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
    public static boolean contains(File a, File b) {
        return TIO.contains(a, b);
    }

    /**
     * Returns {@code true} if and only if this file denotes a file system
     * root or a UNC (if running on the Windows platform).
     */
    public boolean isFileSystemRoot() {
        TFile canOrAbsFile = getCanOrAbsFile();
        return roots.contains(canOrAbsFile) || isUNC(canOrAbsFile.getPath());
    }

    /**
     * Returns {@code true} if and only if this file denotes a UNC.
     * Note that this should be relevant on the Windows platform only.
     */
    public boolean isUNC() {
        return isUNC(getCanOrAbsPath());
    }

    /** The prefix of a UNC (a Windows concept). */
    private static final String UNC_PREFIX = separator + separator;

    /**
     * Returns {@code true} iff the given path is a UNC.
     * Note that this may be only relevant on the Windows platform.
     */
    private static boolean isUNC(String path) {
        return path.startsWith(UNC_PREFIX) && path.indexOf(separatorChar, 2) > 2;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link TFile} delegates the call to its
     * {@link #getFile() decorated file}.
     *
     * @see #equals(Object)
     */
    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link TFile} delegates the call to its
     * {@link #getFile() decorated file}.
     * This implies that only the hierarchicalized file system path
     * of this file instance is considered in the comparison.
     * E.g. {@code new TFile(FsPath.create("zip:file:/archive!/entry"))} and
     * {@code new TFile(FsPath.create("tar:file:/archive!/entry"))} would
     * compare equal because their hierarchicalized file system path is
     * {@code "file:/archive/entry"}.
     * <p>
     * More formally, let {@code a} and {@code b} be two TFile objects.
     * Then if the expression
     * {@code a.toFsPath().hierarchicalize().equals(b.toFsPath().hierarchicalize())}
     * is true, the expression {@code a.equals(b)} is also true.
     * <p>
     * Note that this does <em>not</em> work vice versa:
     * E.g. on Windows, the expression
     * {@code new TFile("file").equals(new TFile("FILE"))} is true, but
     * {@code new TFile("file").toFsPath().hierarchicalize().equals(new TFile("FILE").toFsPath().hierarchicalize())}
     * is false because {@link FsPath#equals(Object)} is case sensitive.
     *
     * @see #hashCode()
     * @see #compareTo(File)
     */
    @Override
    @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
    public boolean equals(Object that) {
        return delegate.equals(that);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link TFile} delegates the call to its
     * {@link #getFile() decorated file}.
     * This implies that only the hierarchicalized file system path
     * Note that this implies that only the hierarchicalized file system path
     * of this file instance is considered in the comparison.
     * E.g. {@code new TFile(FsPath.create("zip:file:/archive!/entry"))} and
     * {@code new TFile(FsPath.create("tar:file:/archive!/entry"))} would
     * compare equal because their hierarchicalized file system path is
     * {@code "file:/archive/entry"}.
     * <p>
     * More formally, let {@code a} and {@code b} be two TFile objects.
     * Then if the expression
     * {@code a.toFsPath().hierarchicalize().compareTo(b.toFsPath().hierarchicalize()) == 0}
     * is true, the expression {@code a.compareTo(b) == 0} is also true.
     * <p>
     * Note that this does <em>not</em> work vice versa:
     * E.g. on Windows, the expression
     * {@code new TFile("file").compareTo(new TFile("FILE")) == 0} is true, but
     * {@code new TFile("file").toFsPath().hierarchicalize().compareTo(new TFile("FILE").toFsPath().hierarchicalize()) == 0}
     * is false because {@link FsPath#equals(Object)} is case sensitive.
     *
     * @see #equals(Object)
     */
    @Override
    public int compareTo(File other) {
        return delegate.compareTo(other);
    }

    /**
     * Returns The top level archive file in the path or {@code null}
     * if this path does not denote an archive.
     * A top level archive is not enclosed in another archive.
     * If this does not return {@code null}, this denotes the longest
     * part of the path which actually may (but does not need to) exist
     * as a regular file in the real file system.
     */
    public TFile getTopLevelArchive() {
        final TFile enclArchive = this.enclArchive;
        return null != enclArchive
                ? enclArchive.getTopLevelArchive()
                : innerArchive;
    }

    @Override
    public String getPath() {
        return delegate.getPath();
    }

    @Override
    public String getName() {
        return delegate.getName();
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

    public FsPath toFsPath() {
        try {
            if (this == innerArchive) {
                final FsScheme scheme = detector.getScheme(delegate.getPath());
                assert null != scheme; // make FindBugs happy!
                if (null != enclArchive) {
                    return new FsPath(
                            new FsMountPoint(
                                scheme,
                                new FsPath(
                                    new FsMountPoint(enclArchive.toURI(), CANONICALIZE),
                                    enclEntryName)),
                            ROOT);
                } else {
                    return new FsPath(
                            new FsMountPoint(scheme, new FsPath(delegate)),
                            ROOT);
                }
            } else if (null != enclArchive) {
                return new FsPath(
                        new FsMountPoint(enclArchive.toURI(), CANONICALIZE),
                        enclEntryName);
            } else {
                return new FsPath(delegate);
            }
        } catch (URISyntaxException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public URI toURI() {
        try {
            if (this == innerArchive) {
                final FsScheme scheme = detector.getScheme(delegate.getPath());
                assert null != scheme; // make FindBugs happy!
                if (null != enclArchive) {
                    return new FsMountPoint(
                            scheme,
                            new FsPath(
                                new FsMountPoint(enclArchive.toURI(), CANONICALIZE),
                                enclEntryName)).getUri();
                } else {
                    return new FsMountPoint(scheme, new FsPath(delegate)).getUri();
                }
            } else if (null != enclArchive) {
                return new FsPath(
                        new FsMountPoint(enclArchive.toURI(), CANONICALIZE),
                        enclEntryName).getUri();
            } else {
                return delegate.toURI();
            }
        } catch (URISyntaxException ex) {
            throw new AssertionError(ex);
        }
    }

    @Deprecated
    @Override
    public URL toURL() throws MalformedURLException {
        return null != innerArchive ? toURI().toURL() : delegate.toURL();
    }

    /**
     * This file system operation is <a href="package-summary.html#atomicity">virtually atomic</a>.
     *
     * @see <a href="#false_positives">Identifying Archive Paths and False Positives</a>
     */
    @Override
    public boolean exists() {
        if (innerArchive != null) {
            try {
                return innerArchive.getController()
                        .getEntry(getInnerEntryName0()) != null;
            } catch (IOException ex) {
                return false;
            }
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
        if (null != innerArchive) {
            try {
                final FsEntry entry = innerArchive.getController()
                        .getEntry(getInnerEntryName0());
                return null != entry && FILE == entry.getType();
            } catch (IOException ex) {
                return false;
            }
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
        if (null != innerArchive) {
            try {
                final FsEntry entry = innerArchive.getController()
                        .getEntry(getInnerEntryName0());
                return null != entry && entry.getType() == DIRECTORY;
            } catch (IOException ex) {
                return false;
            }
        }
        return delegate.isDirectory();
    }

    /**
     * If this file is a true archive file, its archive driver is asked to
     * return an icon to be displayed for this file.
     * Otherwise, null is returned.
     */
    public @CheckForNull Icon getOpenIcon() {
        if (this == innerArchive) {
            try {
                return getController().getOpenIcon();
            } catch (IOException ex) {
            }
        }
        return null;
    }

    /**
     * If this file is a true archive file, its archive driver is asked to
     * return an icon to be displayed for this file.
     * Otherwise, null is returned.
     */
    public @CheckForNull Icon getClosedIcon() {
        if (this == innerArchive) {
            try {
                return getController().getClosedIcon();
            } catch (IOException ex) {
            }
        }
        return null;
    }

    @Override
    public boolean canRead() {
        if (null != innerArchive) {
            try {
                return innerArchive.getController()
                        .isReadable(getInnerEntryName0());
            } catch (IOException ex) {
                return false;
            }
        }
        return delegate.canRead();
    }

    @Override
    public boolean canWrite() {
        if (null != innerArchive) {
            try {
                return innerArchive.getController()
                        .isWritable(getInnerEntryName0());
            } catch (IOException ex) {
                return false;
            }
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
        if (null != innerArchive) {
            try {
                innerArchive.getController().setReadOnly(getInnerEntryName0());
                return true;
            } catch (IOException ex) {
                return false;
            }
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
        if (null != innerArchive) {
            final FsEntry entry;
            try {
                entry = innerArchive.getController().getEntry(getInnerEntryName0());
            } catch (IOException ex) {
                return 0;
            }
            if (null == entry || entry.getType() == DIRECTORY)
                return 0;
            final long length = entry.getSize(DATA);
            return length >= 0 ? length : 0;
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
        if (null != innerArchive) {
            final FsEntry entry;
            try {
                entry = innerArchive.getController().getEntry(getInnerEntryName0());
            } catch (IOException ex) {
                return 0;
            }
            if (null == entry)
                return 0;
            final long time = entry.getTime(Access.WRITE);
            return 0 <= time ? time : 0;
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
     * time, use {@link #archiveCopyFrom(File)} or
     * {@link #archiveCopyTo(File)} instead.
     * <p>
     * This file system operation is <a href="package-summary.html#atomicity">virtually atomic</a>.
     *
     * @see #archiveCopyFrom(File)
     * @see #archiveCopyTo(File)
     * @see <a href="package.html">Package description for more information
     *      about ghost directories</a>
     */
    @Override
    public boolean setLastModified(final long time) {
        if (null != innerArchive) {
            try {
                innerArchive.getController()
                        .setTime(getInnerEntryName0(), BitField.of(Access.WRITE), time);
                return true;
            } catch (IOException ex) {
                return false;
            }
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
    public @Nullable String[] list() {
        if (null != innerArchive) {
            final FsEntry entry;
            try {
                entry = innerArchive.getController().getEntry(getInnerEntryName0());
            } catch (IOException ex) {
                return null;
            }
            if (null == entry)
                return null;
            final Set<String> members = entry.getMembers();
            return null == members ? null : members.toArray(new String[members.size()]);
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
    public @Nullable String[] list(final @CheckForNull FilenameFilter filter) {
        if (null != innerArchive) {
            final FsEntry entry;
            try {
                entry = innerArchive.getController().getEntry(getInnerEntryName0());
            } catch (IOException ex) {
                return null;
            }
            if (null == entry)
                return null;
            final Set<String> members = entry.getMembers();
            if (null == members)
                return null;
            if (null == filter)
                return members.toArray(new String[members.size()]);
            final Collection<String> filtered
                    = new ArrayList<String>(members.size());
            for (final String member : members)
                if (filter.accept(this, member))
                    filtered.add(member);
            return filtered.toArray(new String[filtered.size()]);
        }
        return delegate.list(filter);
    }

    /**
     * Equivalent to {@link #listFiles(FilenameFilter, TArchiveDetector)
     * listFiles((FilenameFilter) null, getArchiveDetector())}.
     */
    @Override
    public @Nullable TFile[] listFiles() {
        return listFiles((FilenameFilter) null, detector);
    }

    /**
     * Returns {@code TFile} objects for the members in this directory
     * in a newly created array.
     * The returned array is <em>not</em> sorted.
     * <p>
     * Note that archive entries with absolute paths are ignored by this
     * method and are never returned.
     * <p>
     * This file system operation is <a href="package-summary.html#atomicity">virtually atomic</a>.
     *
     * @param  detector The archive detector to detect any archives files in
     *         the member file names.
     * @return {@code null} if this is not a directory or an archive file,
     *         a valid (but maybe empty) array otherwise.
     */
    public @Nullable TFile[] listFiles(TArchiveDetector detector) {
        return listFiles((FilenameFilter) null, detector);
    }

    /**
     * Equivalent to {@link #listFiles(FilenameFilter, TArchiveDetector)
     * listFiles(filenameFilter, getArchiveDetector())}.
     */
    @Override
    public @Nullable TFile[] listFiles(
            @CheckForNull FilenameFilter filenameFilter) {
        return listFiles(filenameFilter, detector);
    }

    /**
     * Returns {@code TFile} objects for the members in this directory
     * which are accepted by {@code filenameFilter} in a newly created
     * array.
     * The returned array is <em>not</em> sorted.
     * <p>
     * Note that archive entries with absolute paths are ignored by this
     * method and are never returned.
     * <p>
     * This file system operation is <a href="package-summary.html#atomicity">virtually atomic</a>.
     *
     * @param  detector The archive detector to detect any archives files in
     *         the member file names.
     * @return {@code null} if this is not a directory or an archive file,
     *         a valid (but maybe empty) array otherwise.
     */
    public @Nullable TFile[] listFiles(
            final @CheckForNull FilenameFilter filter,
            final TArchiveDetector detector) {
        if (null != innerArchive) {
            final FsEntry entry;
            try {
                entry = innerArchive.getController().getEntry(getInnerEntryName0());
            } catch (IOException ex) {
                return null;
            }
            if (null == entry)
                return null;
            final Set<String> members = entry.getMembers();
            if (null == members)
                return null;
            final Collection<TFile> filtered
                    = new ArrayList<TFile>(members.size());
            for (final String member : members)
                if (filter == null || filter.accept(this, member))
                    filtered.add(new TFile(TFile.this, member, detector));
            return filtered.toArray(new TFile[filtered.size()]);
        }
        return convert(delegate.listFiles(filter), detector);
    }

    private static @Nullable TFile[] convert(
            final File[] files,
            final TArchiveDetector detector) {
        if (null == files)
            return null; // no directory
        TFile[] results = new TFile[files.length];
        for (int i = files.length; 0 <= --i; )
            results[i] = new TFile(files[i], detector);
        return results;
    }

    /**
     * Equivalent to {@link #listFiles(FileFilter, TArchiveDetector)
     * listFiles(fileFilter, getArchiveDetector())}.
     */
    @Override
    public @Nullable TFile[] listFiles(@CheckForNull FileFilter fileFilter) {
        return listFiles(fileFilter, detector);
    }

    /**
     * Returns {@code TFile} objects for the members in this directory
     * which are accepted by {@code fileFilter} in a newly created array.
     * The returned array is <em>not</em> sorted.
     * <p>
     * Note that archive entries with absolute paths are ignored by this
     * method and are never returned.
     * <p>
     * This file system operation is <a href="package-summary.html#atomicity">virtually atomic</a>.
     *
     * @param  detector The archive detector to detect any archives files in
     *         the member file names.
     * @return {@code null} if this is not a directory or an archive file,
     *         a valid (but maybe empty) array otherwise.
     */
    public @Nullable TFile[] listFiles(
            final @CheckForNull FileFilter filter,
            final TArchiveDetector detector) {
        if (null != innerArchive) {
            final FsEntry entry;
            try {
                entry = innerArchive.getController().getEntry(getInnerEntryName0());
            } catch (IOException ex) {
                return null;
            }
            if (null == entry)
                return null;
            final Set<String> members = entry.getMembers();
            if (null == members)
                return null;
            final Collection<TFile> filtered
                    = new ArrayList<TFile>(members.size());
            for (final String member : members) {
                final TFile file = new TFile(this, member, detector);
                if (filter == null || filter.accept(file))
                    filtered.add(file);
            }
            return filtered.toArray(new TFile[filtered.size()]);
        }
        return delegateListFiles(filter, detector);
    }

    private @Nullable TFile[] delegateListFiles(
            final @CheckForNull FileFilter filter,
            final TArchiveDetector detector) {
        // When filtering, we want to pass in {@code de.schlichtherle.truezip.io.TFile}
        // objects rather than {@code File} objects, so we cannot
        // just call {@code entry.listFiles(FileFilter)}.
        // Instead, we will query the entry for the children names (i.e.
        // Strings) only, construct {@code de.schlichtherle.truezip.io.TFile}
        // instances from this and then apply the filter to construct the
        // result list.

        final List<TFile> filteredList = new ArrayList<TFile>();
        final String[] members = delegate.list();
        if (members == null)
            return null; // no directory

        for (int i = 0, l = members.length; i < l; i++) {
            final String member = members[i];
            final TFile file = new TFile(this, member, detector);
            if (filter == null || filter.accept(file))
                filteredList.add(file);
        }
        final TFile[] list = new TFile[filteredList.size()];
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
        if (null != innerArchive) {
            final FsController<?> controller = innerArchive.getController();
            final FsEntryName entryName = getInnerEntryName0();
            // This is not really atomic, but should be OK in this case.
            if (null != controller.getEntry(entryName))
                return false;
            controller.mknod(
                    entryName,
                    FILE,
                    BitField.of(EXCLUSIVE).set(CREATE_PARENTS, isLenient()),
                    null);
            return true;
        }
        return delegate.createNewFile();
    }

    @Override
    public boolean mkdirs() {
        if (null == innerArchive)
            return delegate.mkdirs();

        final TFile parent = getParentFile();
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
     * {@code new TFile(&quot;archive.zip&quot;).mkdir();}
     * <p>
     * Alternatively, archive files can be created on the fly by simply
     * creating their entries.
     * Example:
     * {@code new TFileOutputStream(&quot;archive.zip/README&quot;);}
     * <p>
     * These examples assume TrueZIP's default configuration where ZIP file
     * recognition is enabled and {@link #isLenient} returns {@code true}.
     * <p>
     * This file system operation is <a href="package-summary.html#atomicity">virtually atomic</a>.
     */
    @Override
    public boolean mkdir() {
        if (null != innerArchive) {
            try {
                innerArchive.getController().mknod(
                        getInnerEntryName0(),
                        DIRECTORY,
                        BitField.of(EXCLUSIVE) // redundant for directory entries
                            .set(CREATE_PARENTS, isLenient()),
                        null);
                return true;
            } catch (IOException ex) {
                return false;
            }
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
            if (null != innerArchive) {
                innerArchive.getController().unlink(getInnerEntryName0());
                return true;
            }
        } catch (IOException ex) {
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
        try {
            TIO.deleteAll(this);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public void deleteOnExit() {
        if (innerArchive != null) {
            // Support for this operation for archive files and entries has been
            // removed in TrueZIP 7 because using a shutdown hook uncautiously
            // can easily introduce a memory leak when using multiple class loaders
            // to load TrueZIP.
            throw new UnsupportedOperationException();
        }
        delegate.deleteOnExit();
    }

    /**
     * Equivalent to {@link #renameTo(File, TArchiveDetector)
     * renameTo(dst, getArchiveDetector())}.
     */
    @Override
    public final boolean renameTo(final File dst) {
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
     */
    public boolean renameTo(final File dst,
                            final TArchiveDetector detector) {
        if (null == innerArchive)
            if (!(dst instanceof TFile) || null == ((TFile) dst).innerArchive)
                return delegate.renameTo(dst);
        if (dst.exists())
            return false;
        try {
            TIO.moveAll(this, dst, detector);
            return true;
        } catch (IOException ex) {
            return false;
        }
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
            final OutputStream out = new TFileOutputStream(this, false);
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
     *        be a plain {@code File}, archive entries are only
     *        supported for instances of this class.
     * @return {@code true} if and only if the operation succeeded.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public boolean copyFrom(final File src) {
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
     * This version uses the {@link TArchiveDetector} which was used to
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
     *        be a plain {@code File}, archive files and entries
     *        are only supported for instances of this class.
     * @return {@code true} if and only if the operation succeeded.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public boolean copyAllFrom(final File src) {
        try {
            TIO.copyAll(false, src, this, detector, detector);
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
     *        be a plain {@code File}, archive files and entries
     *        are only supported for instances of this class.
     * @param detector The object used to detect any archive files
     *        in the source and destination directory trees.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public boolean copyAllFrom(
            final File src,
            final TArchiveDetector detector) {
        try {
            TIO.copyAll(false, src, this, detector, detector);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Recursively copies the file or directory {@code src}
     * to this file or directory.
     * By using different {@link TArchiveDetector}s for the source and
     * destination, this method can be used to do advanced stuff like
     * unzipping any archive file in the source tree to a plain directory
     * in the destination tree (where {@code srcDetector} could be
     * {@link TDefaultArchiveDetector#ALL} and {@code dstDetector} must be
     * {@link TDefaultArchiveDetector#NULL}) or changing the charset by configuring
     * a custom {@link TDefaultArchiveDetector}.
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
     *        be a plain {@code File}, archive files and entries
     *        are only supported for instances of this class.
     * @param srcDetector The object used to detect any archive files
     *        in the source directory tree.
     * @param dstDetector The object used to detect any archive files
     *        in the destination directory tree.
     * @return {@code true} if and only if the operation succeeded.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public boolean copyAllFrom(
            final File src,
            final TArchiveDetector srcDetector,
            final TArchiveDetector dstDetector) {
        try {
            TIO.copyAll(false, src, this, srcDetector, dstDetector);
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
            final InputStream in = new TFileInputStream(this);
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
     *        be a plain {@code File}, archive entries are only
     *        supported for instances of this class.
     * @return {@code true} if the file has been successfully copied.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public boolean copyTo(final File dst) {
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
     * This version uses the {@link TArchiveDetector} which was used to
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
     *        be a plain {@code File}, archive files and entries
     *        are only supported for instances of this class.
     * @return {@code true} if and only if the operation succeeded.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public boolean copyAllTo(final File dst) {
        try {
            TIO.copyAll(false, this, dst, detector, detector);
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
     *        be a plain {@code File}, archive files and entries
     *        are only supported for instances of this class.
     * @param detector The object used to detect any archive files
     *        in the source and destination directory trees.
     * @return {@code true} if and only if the operation succeeded.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public boolean copyAllTo(
            final File dst,
            final TArchiveDetector detector) {
        try {
            TIO.copyAll(false, this, dst, detector, detector);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Recursively copies this file or directory to the file or directory
     * {@code dst}.
     * By using different {@link TArchiveDetector}s for the source and
     * destination, this method can be used to do advanced stuff like
     * unzipping any archive file in the source tree to a plain directory
     * in the destination tree (where {@code srcDetector} could be
     * {@link TDefaultArchiveDetector#ALL} and {@code dstDetector} must be
     * {@link TDefaultArchiveDetector#NULL}) or changing the charset by configuring
     * a custom {@link TDefaultArchiveDetector}.
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
     *        be a plain {@code File}, archive files and entries
     *        are only supported for instances of this class.
     * @param srcDetector The object used to detect any archive files
     *        in the source directory tree.
     * @param dstDetector The object used to detect any archive files
     *        in the destination directory tree.
     * @return {@code true} if and only if the operation succeeded.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public boolean copyAllTo(
            final File dst,
            final TArchiveDetector srcDetector,
            final TArchiveDetector dstDetector) {
        try {
            TIO.copyAll(false, this, dst, srcDetector, dstDetector);
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
     *        be a plain {@code File}, archive entries are only
     *        supported for instances of this class.
     * @return {@code true} if and only if the operation succeeded.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public boolean archiveCopyFrom(final File src) {
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
     * This version uses the {@link TArchiveDetector} which was used to
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
     *        be a plain {@code File}, archive files and entries
     *        are only supported for instances of this class.
     * @return {@code true} if and only if the operation succeeded.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public boolean archiveCopyAllFrom(final File src) {
        try {
            TIO.copyAll(true, src, this, detector, detector);
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
     *        be a plain {@code File}, archive files and entries
     *        are only supported for instances of this class.
     * @param detector The object used to detect any archive files
     *        in the source and destination directory trees.
     * @return {@code true} if and only if the operation succeeded.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public boolean archiveCopyAllFrom(
            final File src,
            final TArchiveDetector detector) {
        try {
            TIO.copyAll(true, src, this, detector, detector);
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
     * By using different {@link TArchiveDetector}s for the source and
     * destination, this method can be used to do advanced stuff like
     * unzipping any archive file in the source tree to a plain directory
     * in the destination tree (where {@code srcDetector} could be
     * {@link TDefaultArchiveDetector#ALL} and {@code dstDetector} must be
     * {@link TDefaultArchiveDetector#NULL}) or changing the charset by configuring
     * a custom {@link TDefaultArchiveDetector}.
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
     *        be a plain {@code File}, archive files and entries
     *        are only supported for instances of this class.
     * @param srcDetector The object used to detect any archive files
     *        in the source directory tree.
     * @param dstDetector The object used to detect archive files
     *        in the destination directory tree.
     * @return {@code true} if and only if the operation succeeded.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public boolean archiveCopyAllFrom(
            final File src,
            final TArchiveDetector srcDetector,
            final TArchiveDetector dstDetector) {
        try {
            TIO.copyAll(true, src, this, srcDetector, dstDetector);
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
     *        be a plain {@code File}, archive entries are only
     *        supported for instances of this class.
     * @return {@code true} if and only if the operation succeeded.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public boolean archiveCopyTo(File dst) {
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
     * This version uses the {@link TArchiveDetector} which was used to
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
     *        be a plain {@code File}, archive files and entries
     *        are only supported for instances of this class.
     * @return {@code true} if and only if the operation succeeded.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public boolean archiveCopyAllTo(final File dst) {
        try {
            TIO.copyAll(true, this, dst, detector, detector);
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
     *        be a plain {@code File}, archive files and entries
     *        are only supported for instances of this class.
     * @param detector The object used to detect any archive files
     *        in the source and destination directory trees.
     * @return {@code true} if and only if the operation succeeded.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public boolean archiveCopyAllTo(
            final File dst,
            final TArchiveDetector detector) {
        try {
            TIO.copyAll(true, this, dst, detector, detector);
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
     * By using different {@link TArchiveDetector}s for the source and
     * destination, this method can be used to do advanced stuff like
     * unzipping any archive file in the source tree to a plain directory
     * in the destination tree (where {@code srcDetector} could be
     * {@link TDefaultArchiveDetector#ALL} and {@code dstDetector} must be
     * {@link TDefaultArchiveDetector#NULL}) or changing the charset by configuring
     * a custom {@link TDefaultArchiveDetector}.
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
     *        be a plain {@code File}, archive files and entries
     *        are only supported for instances of this class.
     * @param srcDetector The object used to detect any archive files
     *        in the source directory tree.
     * @param dstDetector The object used to detect any archive files
     *        in the destination directory tree.
     * @return {@code true} if and only if the operation succeeded.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public boolean archiveCopyAllTo(
            final File dst,
            final TArchiveDetector srcDetector,
            final TArchiveDetector dstDetector) {
        try {
            TIO.copyAll(true, this, dst, srcDetector, dstDetector);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Copies the data from the given input stream to the given output stream
     * and <em>always</em> closes <em>both</em> streams - even if an exception
     * occurs.
     * <p>
     * This is a high performance implementation which uses a pooled background
     * thread to fill a FIFO of data buffers which is concurrently flushed by
     * the current thread.
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
     * @param  in the input stream.
     * @param  out the output stream.
     * @throws InputException if copying the data fails because of an
     *         {@code IOException} in the <em>input</em> stream.
     * @throws IOException if copying the data fails because of an
     *         {@code IOException} in the <em>output</em> stream.
     * @see    #cat(InputStream, OutputStream)
     * @see    <a href="#copy_methods">Copy Methods</a>
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
     *        be a plain {@code File}, archive entries are only
     *        supported for instances of this class.
     * @param dst The destination file. Note that although this just needs to
     *        be a plain {@code File}, archive entries are only
     *        supported for instances of this class.
     * @throws IOException If copying the data fails for some reason.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public static void cp(File src, File dst)
    throws IOException {
        TIO.copy(false, src, dst);
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
     *        be a plain {@code File}, archive entries are only
     *        supported for instances of this class.
     * @param dst The destination file. Note that although this just needs to
     *        be a plain {@code File}, archive entries are only
     *        supported for instances of this class.
     * @throws IOException If copying the data fails for some reason.
     * @throws NullPointerException If any parameter is {@code null}.
     * @see <a href="#copy_methods">Copy Methods</a>
     */
    public static void cp_p(File src, File dst)
    throws IOException {
        TIO.copy(true, src, dst);
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
            final OutputStream out = new TFileOutputStream(this, false);
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
            final InputStream in = new TFileInputStream(this);
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
     * Copies the data from the given input stream to the given output stream
     * <em>without</em> closing them.
     * The name of this method is inspired by the Unix command line utility
     * {@code cat} because you could use it to con<i>cat</i>enate the contents
     * of multiple streams.
     * <p>
     * This is a high performance implementation which uses a pooled background
     * thread to fill a FIFO of data buffers which is concurrently flushed by
     * the current thread.
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
     * @param  in the input stream.
     * @param  out the output stream.
     * @throws InputException if copying the data fails because of an
     *         {@code IOException} in the <em>input</em> stream.
     * @throws IOException if copying the data fails because of an
     *         {@code IOException} in the <em>output</em> stream.
     * @see    #cp(InputStream, OutputStream)
     * @see    <a href="#copy_methods">Copy Methods</a>
     */
    public static void cat(final InputStream in, final OutputStream out)
    throws IOException {
        Streams.cat(in, out);
    }
}
