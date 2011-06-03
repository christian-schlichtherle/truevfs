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

import de.schlichtherle.truezip.util.UriBuilder;
import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.sl.FsManagerLocator;
import de.schlichtherle.truezip.fs.FsSyncWarningException;
import de.schlichtherle.truezip.io.Paths.Splitter;
import de.schlichtherle.truezip.io.Paths;
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
import java.util.Collections;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;
import net.jcip.annotations.Immutable;

import static de.schlichtherle.truezip.fs.FsEntry.*;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import static de.schlichtherle.truezip.fs.FsSyncOption.*;
import static de.schlichtherle.truezip.fs.FsSyncOptions.*;
import static de.schlichtherle.truezip.fs.FsUriModifier.*;
import static de.schlichtherle.truezip.entry.Entry.Size.*;
import static de.schlichtherle.truezip.entry.Entry.Type.*;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;

/**
 * A replacement for the class {@link File} which provides transparent
 * read/write access to archive files and their entries as if they were
 * (virtual) directories and files.
 * Because this class actually extends the class {@link File} it can be used
 * with the class {@link FileSystemView} or any other classes which depend on
 * the class {@link File}.
 *
 * <a name="bulkIOMethods"/><h3>Bulk I/O Methods</h3>
 * <p>
 * This class provides some convenient methods which use pooled buffers and
 * pooled threads in order to achieve superior performance as compared to the
 * naive read-then-write-in-a-loop approach.
 * These bulk I/O methods fall into the following categories:
 * <ol>
 * <li>The {@code cp(_p|_r|_rp)?} methods copy files or directory trees.
 *     The method names have been modeled after the Unix command line utility
 *     {@code cp} with its options.
 * <li>The {@code mv} methods move files or directory trees.
 *     The method names have been modeled after the Unix command line utility
 *     {@code mv}.
 * <li>The {@code rm(_r)?} methods remove files or directory trees.
 *     The method names have been modeled after the Unix command line utility
 *     {@code rm} with its options.
 * <li>The {@code input|output} methods copy the given streams to this file or
 *     vice versa.
 *     In contrast to the previous methods they never close their argument
 *     streams, so applications can call them multiple times on the same
 *     streams to con<em>cat</em>enate data.
 * <li>Finally, the {@link #cat(InputStream, OutputStream)}
 *     method is the core copy engine for all these methods.
 *     It performs the data transfer from an input stream to an output stream.
 *     When used with <em>unbuffered</em> input and output stream
 *     implementations, it delivers the same performance as the transfer
 *     method in the package {@code java.nio}.
 *     Its name is modeled after the Unix command line utility {@code cat}.
 * </ol>
 * <b>Important:</b> You must provide the <em>full path name</em> for both
 * source and destination parameters to any of these methods!
 * In particular, both the source and destination parameters must either
 * represent a file or a directory - mixing file and directory parameters will
 * not work.
 * This limitation is designed to prevent ambiguous method semantics.
 *
 * <a name="directDataCopying"/><h4>Direct Data Copying (DDC)</h4>
 * <p>
 * If data is copied from an archive file to another archive file of the
 * same type, some of the copy methods support a feature called <i>Direct Data
 * Copying</i> (DDC) to achieve best performance:</a>
 * DDC copies the raw data from the source archive entry to the destination
 * archive entry without the need to temporarily reproduce, copy and process
 * the original data again.
 * <p>
 * The benefits of this feature are archive driver specific:
 * In case of ZIP compatible files with compressed entries, this avoids the
 * need to inflate the data from the source entry just to deflate it again for
 * the destination entry.
 * In case of TAR compatible files, it avoids the need to create an
 * additional temporary file, but shows no impact otherwise - the TAR file
 * format doesn't support compression.
 *
 * <a name="traversal"><h3>Traversing Directory Trees</h3></a>
 * <p>
 * When traversing directory trees using different instances of this class,
 * it's important that they use {@link TArchiveDetector}s which recognize the
 * same set of archive files, i.e. their {@link TArchiveDetector#toString()}
 * method compares {@link String#equals(Object) equal}.
 * This is required in order to make sure that the cached data structures
 * in the TrueZIP Kernel do not get bypassed.
 * Otherwise, archive files may loose data or even get corrupted!
 * <p>
 * When copying or moving a directory tree, if you need a verbatim copy of any
 * archive files within this directory tree, then make sure that any cached
 * data structures in the TrueZIP Kernel are purged by calling {@link #umount()}
 * before calling one of the methods which accept the additional parameter
 * {@link TArchiveDetector#NULL} for the traversal of this directory tree.
 * For example, to make a recursive archive copy, call
 * <pre><code>
 * TFile src = ...
 * TFile dst = ...
 * TFile.umount(); // commit changes and purge any cached data
 * TFile.cp_rp(src, dst, TArchiveDetector.NULL, TArchiveDetector.NULL);
 * </code></pre>
 * 
 * <a name="falsePositives"/><h3>Detecting Archive Files and False Positives</h3>
 * <p>
 * Whenever an archive file suffix is detected in a path, this class treats
 * the corresponding file or directory as a <i>prospective archive file</i>.
 * The word &quot;prospective&quot; suggests that just because a file is named
 * <i>archive.zip</i> it isn't necessarily a valid ZIP file.
 * In fact, it could be anything, even a plain directory in the OS file system!
 * <p>
 * Such an invalid archive file is called a <i>false positive</i> archive file.
 * TrueZIP correctly identifies all types of false positive archive files by
 * performing a recursive look up operation for the first parent file system
 * where the given archive file actually exists and treats it according to the
 * <i>true state</i> of this entity.
 * <p>
 * The following table shows how certain methods in this class behave,
 * depending upon an archive file's path and its <i>true state</i> in the
 * first parent file system where it actually exists.
 * <p>
 * <table border="2" cellpadding="4">
 * <thead>
 * <tr>
 *   <th>Path</th>
 *   <th>True State</th>
 *   <th>{@code isArchive()}<sup>1</sup></th>
 *   <th>{@code isDirectory()}</th>
 *   <th>{@code isFile()}</th>
 *   <th>{@code exists()}</th>
 *   <th>{@code length()}<sup>2</sup></th>
 * </tr>
 * </thead>
 * <tbody>
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
 *   <td>False positive: Plain directory</td>
 *   <td>{@code true}</td>
 *   <td>{@code true}</td>
 *   <td>{@code false}</td>
 *   <td>{@code true}</td>
 *   <td><i>{@code ?}</i></td>
 * </tr>
 * <tr>
 *   <td><i>archive.zip</i></td>
 *   <td>False positive: Plain file</td>
 *   <td>{@code true}</td>
 *   <td>{@code false}</td>
 *   <td>{@code true}</td>
 *   <td>{@code true}</td>
 *   <td><i>{@code ?}</i></td>
 * </tr>
 * <tr>
 *   <td><i>archive.zip</i></td>
 *   <td>False positive: Regular special file</td>
 *   <td>{@code true}</td>
 *   <td>{@code false}</td>
 *   <td>{@code false}</td>
 *   <td>{@code true}</td>
 *   <td><i>{@code ?}</i></td>
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
 *   <td><i>{@code ?}</i></td>
 * </tr>
 * <tr>
 *   <td><i>archive.tzp</i></td>
 *   <td>False positive: Plain directory</td>
 *   <td>{@code true}</td>
 *   <td>{@code true}</td>
 *   <td>{@code false}</td>
 *   <td>{@code true}</td>
 *   <td><i>{@code ?}</i></td>
 * </tr>
 * <tr>
 *   <td><i>archive.tzp</i></td>
 *   <td>False positive: Plain file</td>
 *   <td>{@code true}</td>
 *   <td>{@code false}</td>
 *   <td>{@code true}</td>
 *   <td>{@code true}</td>
 *   <td><i>{@code ?}</i></td>
 * </tr>
 * <tr>
 *   <td><i>archive.tzp</i></td>
 *   <td>False positive: Regular special file</td>
 *   <td>{@code true}</td>
 *   <td>{@code false}</td>
 *   <td>{@code false}</td>
 *   <td>{@code true}</td>
 *   <td><i>{@code ?}</i></td>
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
 *   <td>Plain directory</td>
 *   <td>{@code false}</td>
 *   <td>{@code true}</td>
 *   <td>{@code false}</td>
 *   <td>{@code true}</td>
 *   <td><i>{@code ?}</i></td>
 * </tr>
 * <tr>
 *   <td><i>other</i></td>
 *   <td>Plain file</td>
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
 * </tbody>
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
 * <li>This example assumes that the TrueZIP Driver ZIP module is
 *     present on the run-time class path.</li>
 * <li>This example assumes that the TrueZIP Driver TZP (ZIP.RAES) module is
 *     present on the run-time class path.</li>
 * <li>The methods behave exactly the same for both <i>archive.zip</i> and
 *    <i>archive.tzp</i> with one exception: If the key for a RAES encrypted
 *    ZIP file remains unknown (e.g. because the user cancelled password
 *    prompting), then these methods behave as if the true state of the path
 *    were a special file, i.e. both {@link #isDirectory} and {@link #isFile}
 *    return {@code false}, while {@link #exists} returns {@code true}.</li>
 * </ol>
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
@Immutable
@edu.umd.cs.findbugs.annotations.SuppressWarnings("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
public final class TFile extends File {

    private static final long serialVersionUID = 3617072259051821745L;

    /** The prefix of a UNC (a Windows concept). */
    private static final String UNC_PREFIX = separator + separator;

    /** The file system roots. */
    private static final Set<File>
            ROOTS = Collections.unmodifiableSet(
                new TreeSet<File>(Arrays.asList(listRoots())));

    private static boolean lenient = true;

    /** The file system manager to use within this package. */
    static final FsManager manager = FsManagerLocator.SINGLETON.get();

    private static TArchiveDetector
            defaultDetector = TArchiveDetector.ALL;

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
    private transient File delegate;

    private transient TArchiveDetector detector;
    private transient @Nullable TFile innerArchive;
    private transient @Nullable TFile enclArchive;
    private transient @Nullable FsEntryName enclEntryName;

    /**
     * This refers to the file system controller if and only if this file
     * refers to a prospective archive file, otherwise it's {@code null}.
     * This field should be considered to be {@code final}!
     *
     * @see #readObject
     */
    private transient volatile @Nullable FsController<?> controller;

    /**
     * Copy constructor.
     * Equivalent to {@link #TFile(File, TArchiveDetector)
     * new TFile(template, getDefaultArchiveDetector())}.
     * 
     * @param file a file object.
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
     * new TFile(path, getDefaultArchiveDetector())}.
     * 
     * @param path a file path.
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
     * new TFile(parent, child, getDefaultArchiveDetector())}.
     *
     * @param parent The path of the parent directory.
     *        The {@link #getDefaultArchiveDetector() default archive detector}
     *        is used to detect any archive files in the path of this
     *        {@code TFile} instance.
     * @param member The child path as a {@link String}.
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
     * new TFile(parent, child, null)}.
     *
     * @param parent The parent directory as a {@code TFile} instance.
     *        If this parameter is an instance of this class, its
     *        {@code TArchiveDetector} is used to detect any archive files
     *        in the path of this {@code TFile} instance.
     *        Otherwise, the
     *        {@link #getDefaultArchiveDetector() default archive detector}
     *        is used.
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
     * Constructs a new {@code TFile} instance from the given {@code uri}.
     * This method behaves similar to the super class constructor
     * {@link File#File(URI)} with the following amendment:
     * If the URI matches the pattern {@code scheme:file:path!/entry},
     * then the constructed file object treats the URI like an entry in the
     * federated file system (i.e. prospective archive file) of the type named
     * {@code scheme}.
     * This may be recursively applied to access archive entries within other
     * archive files.
     * <p>
     * Note that the constructed {@code TFile} instance uses the
     * {@link #getDefaultArchiveDetector() default archive detector} to look
     * up archive drivers for the named scheme(s).
     *
     * @param  uri an absolute URI with a scheme supported by the
     *         {@link #getDefaultArchiveDetector() default archive detector}.
     * @throws IllegalArgumentException if the given URI does not conform to
     *         the syntax constraints for {@link FsPath}s or
     *         {@link File#File(URI)}.
     */
    public TFile(URI uri) {
        this(FsPath.create(uri, CANONICALIZE), defaultDetector);
    }

    /**
     * Constructs a new {@code TFile} instance from the given {@code path}.
     * This method behaves similar to the super class constructor
     * {@link File#File(URI)} with the following amendment:
     * If the path matches the pattern {@code scheme:file:path!/entry},
     * then the constructed file object treats the URI like an entry in the
     * federated file system (i.e. prospective archive file) of the type named
     * {@code scheme}.
     * This may be recursively applied to access archive entries within other
     * archive files.
     * <p>
     * Note that the constructed {@code TFile} instance uses the
     * {@link #getDefaultArchiveDetector() default archive detector} to look
     * up archive drivers for the named scheme(s).
     *
     * @param  path an absolute path with a scheme supported by the
     *         {@link #getDefaultArchiveDetector() default archive detector}.
     * @throws IllegalArgumentException if the given URI does not conform to
     *         the syntax constraints for {@link FsPath}s or
     *         {@link File#File(URI)}.
     */
    public TFile(FsPath path) {
        this(path, defaultDetector);
    }

    private TFile(FsPath path, TArchiveDetector detector) {
        super(path.toHierarchicalUri());
        parse(path, detector);
    }

    private void parse( final FsPath path,
                        final TArchiveDetector detector) {
        this.delegate = new File(super.getPath());
        this.detector = detector;

        final FsMountPoint mp = path.getMountPoint();
        final FsPath mpp = mp.getPath();
        final FsEntryName en;

        if (null == mpp) {
            assert !path.toUri().isOpaque();
            this.enclArchive = null;
            this.enclEntryName = null;
            this.innerArchive = null;
        } else if ((en = path.getEntryName()).isRoot()) {
            assert path.toUri().isOpaque();
            if (mpp.toUri().isOpaque()) {
                this.enclArchive
                        = new TFile(mpp.getMountPoint(), detector);
                this.enclEntryName = mpp.getEntryName();
            } else {
                this.enclArchive = null;
                this.enclEntryName = null;
            }
            this.innerArchive = this;
        } else {
            assert path.toUri().isOpaque();
            this.enclArchive = new TFile(mp, detector);
            this.enclEntryName = en;
            this.innerArchive = this.enclArchive;
        }

        assert invariants();
    }

    @SuppressWarnings("LeakingThisInConstructor")
    private TFile(  final FsMountPoint mountPoint,
                    final TArchiveDetector detector) {
        super(mountPoint.toHierarchicalUri());

        this.delegate = new File(super.getPath());
        this.detector = detector;

        final FsPath mountPointPath = mountPoint.getPath();

        if (null == mountPointPath) {
            assert !mountPoint.toUri().isOpaque();
            this.enclArchive = null;
            this.enclEntryName = null;
            this.innerArchive = null;
        } else {
            assert mountPoint.toUri().isOpaque();
            if (mountPointPath.toUri().isOpaque()) {
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
                try {
                    this.enclEntryName = new FsEntryName(
                            new UriBuilder()
                                .path(
                                    path.substring(innerArchivePathLength + 1) // cut off leading separatorChar
                                        .replace(separatorChar, SEPARATOR_CHAR))
                                .getUri(),
                            CANONICALIZE);
                } catch (URISyntaxException ex) {
                    throw new AssertionError(ex);
                }
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
        scan(ancestor, detector, 0, path, enclEntryNameBuf, new Splitter(separatorChar, false));
        try {
            enclEntryName = 0 >= enclEntryNameBuf.length()
                    ? null
                    : new FsEntryName(
                        new UriBuilder().path(enclEntryNameBuf.toString()).getUri(),
                        CANONICALIZE);
        } catch (URISyntaxException ex) {
            throw new AssertionError(ex);
        }
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
     * <pre>{@code assert invariants();}</pre>
     * This calls the method if and only if assertions are enabled in order
     * to assert that the instance invariants are properly obeyed.
     * If assertions are disabled, the call to this method is thrown away by
     * the HotSpot compiler, so there is no performance penalty.
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
        assert (null != innerArchive) == (getInnerEntryName() != null);
        assert (null != enclArchive) == (enclEntryName != null);
        assert this != enclArchive;
        assert (this == innerArchive)
                ^ (innerArchive == enclArchive && null == controller);
        assert null == enclArchive
                || Paths.contains(  enclArchive.getPath(),
                                    delegate.getParentFile().getPath(),
                                    separatorChar)
                    && !enclEntryName.toString().isEmpty();
        return true;
    }

    /**
     * Commits all unsynchronized changes to the contents of all federated file
     * systems (i.e. prospective archive files) to their respective parent file
     * system, releases the associated resources (i.e. target archive files)
     * for access by third parties (e.g. other processes), cleans up any
     * temporary allocated resources (e.g. temporary files) and purges any
     * cached data.
     * Note that temporary files may get used even if the archive files where
     * accessed read-only.
     *
     * @param  options a bit field of synchronization options.
     * @throws IllegalArgumentException if the combination of synchronization
     *         options is illegal, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_INPUT} is cleared and
     *         {@code FsSyncOption.FORCE_CLOSE_OUTPUT} is set or if the
     *         synchronization option {@code FsSyncOption.ABORT_CHANGES} is set.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         occur.
     *         This implies that the respective parent file system has been
     *         updated with constraints, such as a failure to set the last
     *         modification time of the entry for the federated file system
     *         (i.e. archive file) in its parent file system.
     * @throws FsSyncException if any error conditions occur.
     *         This implies loss of data!
     */
    public static void sync(BitField<FsSyncOption> options)
    throws FsSyncException {
        manager.sync(options);
    }

    /**
     * Commits all unsynchronized changes to the contents of all federated file
     * systems (i.e. prospective archive files) identified by {@code archive}
     * and all its member federated file systems to their respective parent
     * file system, releases the associated resources (i.e. target archive
     * files) for access by third parties (e.g. other processes), cleans up any
     * temporary allocated resources (e.g. temporary files) and purges any
     * cached data.
     * Note that temporary files may get used even if the archive files where
     * accessed read-only.
     * <p>
     * If a client application needs to sync an individual archive file,
     * the following idiom could be used:
     * <pre>{@code
     * if (file.isArchive() && file.getEnclArchive() == null) // filter top level federated file system
     *   if (file.isDirectory()) // ignore false positives
     *     TFile.sync(file); // sync federated file system and all its members
     * }</pre>
     * Again, this will also sync all federated file systems which are
     * located within the file system referred to by {@code file}.
     *
     * @param  archive a top level federated file system, i.e. archive file.
     * @param  options a bit field of synchronization options.
     * @throws IllegalArgumentException if {@code archive} is not a top level
     *         federated file system or the combination of synchronization
     *         options is illegal, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_INPUT} is cleared and
     *         {@code FsSyncOption.FORCE_CLOSE_OUTPUT} is set or if the
     *         synchronization option {@code FsSyncOption.ABORT_CHANGES} is set.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         occur.
     *         This implies that the respective parent file system has been
     *         updated with constraints, such as a failure to set the last
     *         modification time of the entry for the federated file system
     *         (i.e. archive file) in its parent file system.
     * @throws FsSyncException if any error conditions occur.
     *         This implies loss of data!
     * @see    #sync(BitField)
     */
    public static void sync(
            final TFile archive,
            final BitField<FsSyncOption> options)
    throws FsSyncException {
        if (!archive.isArchive())
            throw new IllegalArgumentException(archive + " (not an archive file)");
        if (null != archive.getEnclArchive())
            throw new IllegalArgumentException(archive + " (not a top level archive file)");
        new FsFilteringManager(
                manager,
                archive .getController()
                        .getModel()
                        .getMountPoint())
                    .sync(options);
    }

    /**
     * Commits all unsynchronized changes to the contents of all federated file
     * systems (i.e. prospective archive files) to their respective parent file
     * system, releases the associated resources (i.e. target archive files)
     * for access by third parties (e.g. other processes), cleans up any
     * temporary allocated resources (e.g. temporary files) and purges any
     * cached data.
     * Note that temporary files may get used even if the archive files where
     * accessed read-only.
     * <p>
     * This method is equivalent to calling
     * {@link #sync(BitField) sync(FsManager.UMOUNT)}.
     *
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         occur.
     *         This implies that the respective parent file system has been
     *         updated with constraints, such as a failure to set the last
     *         modification time of the entry for the federated file system
     *         (i.e. archive file) in its parent file system.
     * @throws FsSyncException if any error conditions occur.
     *         This implies loss of data!
     * @see #sync(BitField)
     */
    public static void umount()
    throws FsSyncException {
        sync(UMOUNT);
    }

    /**
     * Commits all unsynchronized changes to the contents of all federated file
     * systems (i.e. prospective archive files) to their respective parent file
     * system, releases the associated resources (i.e. target archive files)
     * for access by third parties (e.g. other processes), cleans up any
     * temporary allocated resources (e.g. temporary files) and purges any
     * cached data.
     * Note that temporary files may get used even if the archive files where
     * accessed read-only.
     * <p>
     * This method is equivalent to calling
     * {@link #sync(BitField)
        sync(   BitField.of(FsSyncOption.CLEAR_CACHE)
                .set(FsSyncOption.FORCE_CLOSE_INPUT, closeStreams)
                .set(FsSyncOption.FORCE_CLOSE_OUTPUT, closeStreams))
     * }.
     *
     * @param  closeStreams see {@link FsSyncOption#FORCE_CLOSE_INPUT} and
     *         {@link FsSyncOption#FORCE_CLOSE_OUTPUT}.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         occur.
     *         This implies that the respective parent file system has been
     *         updated with constraints, such as a failure to set the last
     *         modification time of the entry for the federated file system
     *         (i.e. prospective archive file) in its parent file system.
     * @throws FsSyncException if any error conditions occur.
     *         This implies loss of data!
     * @see #sync(BitField)
     */
    public static void umount(boolean closeStreams)
    throws FsSyncException {
        sync(   BitField.of(CLEAR_CACHE)
                    .set(FORCE_CLOSE_INPUT, closeStreams)
                    .set(FORCE_CLOSE_OUTPUT, closeStreams));
    }

    /**
     * Commits all unsynchronized changes to the contents of all federated file
     * systems (i.e. prospective archive files) to their respective parent file
     * system, releases the associated resources (i.e. target archive files)
     * for access by third parties (e.g. other processes), cleans up any
     * temporary allocated resources (e.g. temporary files) and purges any
     * cached data.
     * Note that temporary files may get used even if the archive files where
     * accessed read-only.
     * <p>
     * This method is equivalent to calling
     * {@link #sync(BitField)
        sync(   BitField.of(FsSyncOption.CLEAR_CACHE)
                .set(FsSyncOption.WAIT_CLOSE_INPUT, waitForInputStreams)
                .set(FsSyncOption.FORCE_CLOSE_INPUT, closeInputStreams)
                .set(FsSyncOption.WAIT_CLOSE_OUTPUT, waitForOutputStreams)
                .set(FsSyncOption.FORCE_CLOSE_OUTPUT, closeOutputStreams))
     * }.
     *
     * @param  waitForInputStreams see {@link FsSyncOption#WAIT_CLOSE_INPUT}.
     * @param  closeInputStreams see {@link FsSyncOption#FORCE_CLOSE_INPUT}.
     * @param  waitForOutputStreams see {@link FsSyncOption#WAIT_CLOSE_OUTPUT}.
     * @param  closeOutputStreams see {@link FsSyncOption#FORCE_CLOSE_OUTPUT}.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         occur.
     *         This implies that the respective parent file system has been
     *         updated with constraints, such as a failure to set the last
     *         modification time of the entry for the federated file system
     *         (i.e. prospective archive file) in its parent file system.
     * @throws FsSyncException if any error conditions occur.
     *         This implies loss of data!
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
     * Commits all unsynchronized changes to the contents of all federated file
     * systems (i.e. prospective archive files) to their respective parent file
     * system, releases the associated resources (i.e. target archive files)
     * for access by third parties (e.g. other processes), cleans up any
     * temporary allocated resources (e.g. temporary files) and purges any
     * cached data.
     * Note that temporary files may get used even if the archive files where
     * accessed read-only.
     * <p>
     * This method is equivalent to calling
     * {@link #sync(BitField)
        sync(archive, FsManager.UMOUNT)
     * }.
     *
     * @param  archive a top level federated file system, i.e. prospective
     *         archive file.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         occur.
     *         This implies that the respective parent file system has been
     *         updated with constraints, such as a failure to set the last
     *         modification time of the entry for the federated file system
     *         (i.e. prospective archive file) in its parent file system.
     * @throws FsSyncException if any error conditions occur.
     *         This implies loss of data!
     * @see #sync(TFile, BitField)
     */
    public static void umount(TFile archive)
    throws FsSyncException {
        sync(archive, UMOUNT);
    }

    /**
     * Commits all unsynchronized changes to the contents of all federated file
     * systems (i.e. prospective archive files) to their respective parent file
     * system, releases the associated resources (i.e. target archive files)
     * for access by third parties (e.g. other processes), cleans up any
     * temporary allocated resources (e.g. temporary files) and purges any
     * cached data.
     * Note that temporary files may get used even if the archive files where
     * accessed read-only.
     * <p>
     * This method is equivalent to calling
     * {@link #sync(BitField)
        sync(   archive,
                BitField.of(FsSyncOption.CLEAR_CACHE)
                .set(FsSyncOption.FORCE_CLOSE_INPUT, closeStreams)
                .set(FsSyncOption.FORCE_CLOSE_OUTPUT, closeStreams))
     * }.
     *
     * @param  archive a top level federated file system, i.e. prospective
     *         archive file.
     * @param  closeStreams see {@link FsSyncOption#FORCE_CLOSE_INPUT} and
     *         {@link FsSyncOption#FORCE_CLOSE_OUTPUT}.
     * @throws IllegalArgumentException if {@code archive} is not a top level
     *         federated file system or the combination of synchronization
     *         options is illegal, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_INPUT} is cleared and
     *         {@code FsSyncOption.FORCE_CLOSE_OUTPUT} is set or if the
     *         synchronization option {@code FsSyncOption.ABORT_CHANGES} is set.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         occur.
     *         This implies that the respective parent file system has been
     *         updated with constraints, such as a failure to set the last
     *         modification time of the entry for the federated file system
     *         (i.e. prospective archive file) in its parent file system.
     * @throws FsSyncException if any error conditions occur.
     *         This implies loss of data!
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
     * Commits all unsynchronized changes to the contents of all federated file
     * systems (i.e. prospective archive files) to their respective parent file
     * system, releases the associated resources (i.e. target archive files)
     * for access by third parties (e.g. other processes), cleans up any
     * temporary allocated resources (e.g. temporary files) and purges any
     * cached data.
     * Note that temporary files may get used even if the archive files where
     * accessed read-only.
     * <p>
     * This method is equivalent to calling
     * {@link #sync(BitField)
        sync(   archive,
                BitField.of(FsSyncOption.CLEAR_CACHE)
                .set(FsSyncOption.WAIT_CLOSE_INPUT, waitForInputStreams)
                .set(FsSyncOption.FORCE_CLOSE_INPUT, closeInputStreams)
                .set(FsSyncOption.WAIT_CLOSE_OUTPUT, waitForOutputStreams)
                .set(FsSyncOption.FORCE_CLOSE_OUTPUT, closeOutputStreams))
     * }.
     *
     * @param  archive a top level federated file system, i.e. prospective
     *         archive file.
     * @param  waitForInputStreams see {@link FsSyncOption#WAIT_CLOSE_INPUT}.
     * @param  closeInputStreams see {@link FsSyncOption#FORCE_CLOSE_INPUT}.
     * @param  waitForOutputStreams see {@link FsSyncOption#WAIT_CLOSE_OUTPUT}.
     * @param  closeOutputStreams see {@link FsSyncOption#FORCE_CLOSE_OUTPUT}.
     * @throws IllegalArgumentException if {@code archive} is not a top level
     *         federated file system or the combination of synchronization
     *         options is illegal, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_INPUT} is cleared and
     *         {@code FsSyncOption.FORCE_CLOSE_OUTPUT} is set or if the
     *         synchronization option {@code FsSyncOption.ABORT_CHANGES} is set.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         occur.
     *         This implies that the respective parent file system has been
     *         updated with constraints, such as a failure to set the last
     *         modification time of the entry for the federated file system
     *         (i.e. prospective archive file) in its parent file system.
     * @throws FsSyncException if any error conditions occur.
     *         This implies loss of data!
     * @see    #sync(TFile, BitField)
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
     * By default, the value of this class property is {@code true}.
     *
     * @see #setLenient(boolean)
     * @return The value of the class property {@code lenient}.
     */
    public static boolean isLenient() {
        return lenient;
    }

    /**
     * Sets the value of the class property {@code lenient}.
     * This class property controls whether archive files and their member
     * directories get automatically created whenever required.
     * By default, the value of this class property is {@code true}!
     * <p>
     * Consider the following path: {@code a/outer.zip/b/inner.zip/c}.
     * Now let's assume that {@code a} exists as a plain directory in the OS
     * file system, while all other parts of this path don't, and that the
     * module TrueZIP Driver ZIP is present on the run-time class path in order
     * to recognize {@code outer.zip} and {@code inner.zip} as ZIP files by
     * default.
     * <p>
     * Now, if this class property is set to {@code false}, then an application
     * would have to call {@code new TFile("a/outer.zip/b/inner.zip").mkdirs()}
     * before it could actually create the innermost {@code c} entry as a file
     * or directory.
     * <p>
     * More formally, before an application could access an entry in a
     * federated file system, all its parent directories would need to exist,
     * including archive files.
     * This emulates the behaviour of any OS file system type.
     * <p>
     * If this class property is set to {@code true} however, then any missing
     * parent directories (including archive files) up to the outermost archive
     * file {@code outer.zip} would get automatically created when using
     * operations to create the innermost element of the path {@code c}.
     * <p>
     * This allows applications to succeed with doing this:
     * {@code new TFile("a/outer.zip/b/inner.zip/c").createNewFile()},
     * or that:
     * {@code new TFileOutputStream("a/outer.zip/b/inner.zip/c")}.
     * <p>
     * Note that in either case the parent directory of the outermost archive
     * file {@code a} must exist - TrueZIP does not automatically create
     * directories in the OS file system!
     *
     * @param lenient the value of the class property {@code lenient}.
     * @see   #isLenient()
     */
    public static void setLenient(boolean lenient) {
        TFile.lenient = lenient;
    }

    /**
     * Returns the {@link TArchiveDetector} to use if no archive detector is
     * explicitly passed to the constructor of a {@code TFile} instance.
     * <p>
     * This class property is initially set to
     * {@link TArchiveDetector#ALL}
     *
     * @return The {@link TArchiveDetector} to use if no archive detector is
     *         explicitly passed to the constructor of a {@code TFile} instance.
     * @see #setDefaultArchiveDetector
     */
    public static TArchiveDetector getDefaultArchiveDetector() {
        return defaultDetector;
    }

    /**
     * Sets the {@link TArchiveDetector} to use if no archive detector is
     * explicitly passed to the constructor of a {@code TFile} instance.
     * When a new {@code TFile} instance is constructed, but no archive
     * detector is provided, then the value of this class property is used.
     * So changing the value of this class property affects only subsequently
     * constructed {@code TFile} instances - not any existing ones.
     *
     * @param detector the {@link TArchiveDetector} to use for subsequently
     *        constructed {@code TFile} instances if no archive detector is
     *        explicitly provided to the constructor
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
     * 
     * @return The first parent directory (starting from this file) which is
     *         <em>not</em> an archive file or a file located in an archive
     *         file.
     */
    public @Nullable TFile getNonArchivedParentFile() {
        final TFile enclArchive = this.enclArchive;
        return null != enclArchive
                ? enclArchive.getNonArchivedParentFile()
                : getParentFile();
    }

    /** {@inheritDoc} */
    @Override
    public @Nullable String getParent() {
        return delegate.getParent();
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    public TFile getAbsoluteFile() {
        String p = getAbsolutePath();
        return p.equals(getPath()) ? this : new TFile(p, detector);
    }

    /** {@inheritDoc} */
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
     * @return The normalized absolute file object denoting the same file or
     *         directory as this instance.
     * @see    #getCanonicalFile()
     * @see    #getNormalizedFile()
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
     * @return The normalized absolute path string denoting the same file or
     *         directory as this instance.
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
     * @return The normalized file object denoting the same file or
     *         directory as this instance.
     */
    public TFile getNormalizedFile() {
        String p = getNormalizedPath();
        return p.equals(getPath()) ? this : new TFile(p, detector);
    }

    /**
     * Removes any redundant {@code "."}, {@code ".."} directories from the
     * path name.
     *
     * @return The normalized path string denoting the same file or
     *         directory as this instance.
     */
    public String getNormalizedPath() {
        return Paths.normalize(getPath(), separatorChar);
    }

    /** {@inheritDoc} */
    @Override
    public TFile getCanonicalFile() throws IOException {
        String p = getCanonicalPath();
        return p.equals(getPath()) ? this : new TFile(p, detector);
    }

    /** {@inheritDoc} */
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
    public TFile getCanOrAbsFile() {
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
     * {@link TArchiveDetector} which was used to construct this file object
     * - no file system tests are performed by this method!
     *
     * @return {@code true} if and only if the path represented by this
     *         instance denotes an archive file.
     * @see    #isEntry
     * @see    #isDirectory
     * @see    <a href="#falsePositives">Detecting Archive Paths and False Positives</a>
     */
    public boolean isArchive() {
        return this == innerArchive;
    }

    /**
     * Returns {@code true} if and only if this file object addresses an
     * entry located within an archive file.
     * Whether or not this is true solely depends on the
     * {@link TArchiveDetector} which was used to construct this file object
     * - no file system tests are performed by this method!
     *
     * @return {@code true} if and only if this file object addresses an
     *         entry located within an archive file.
     * @see #isArchive
     * @see #isDirectory
     * @see <a href="#falsePositives">Detecting Archive Paths and False Positives</a>
     */
    public boolean isEntry() {
        return enclEntryName != null;
    }

    /**
     * Returns the innermost archive file object for this file object.
     * That is, if this object addresses an archive file, then this method
     * returns {@code this}.
     * If this object addresses an entry located within an archive file, then
     * this methods returns the file object representing the enclosing archive
     * file, or {@code null} otherwise.
     * <p>
     * This method always returns a normalized path, i.e. all occurences of
     * {@code "."} and {@code ".."} in the path name are removed according to
     * their meaning wherever possible.
     * <p>
     * In order to support unlimited nesting levels, this method returns
     * a {@code TFile} instance which may recursively address an entry within
     * another archive file.
     * 
     * @return The innermost archive path object for this path object.
     */
    public @CheckForNull TFile getInnerArchive() {
        return innerArchive;
    }

    /**
     * Returns the entry name relative to the innermost archive file.
     * That is, if this object addresses an archive file, then this method
     * returns the empty string {@code ""}.
     * If this object addresses an entry located within an archive file,
     * then this method returns the relative path of the entry in the
     * enclosing archive file separated by the entry separator character
     * {@code '/'}, or {@code null} otherwise.
     * <p>
     * This method always returns an undotified path, i.e. all redundant
     * occurences of {@code "."} and {@code ".."} in the path are removed
     * wherever possible.
     * 
     * @return The entry name relative to the innermost archive file.
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
     * Returns the enclosing archive file object for this file object.
     * That is, if this object addresses an entry located within an archive
     * file, then this method returns the file object representing the
     * enclosing archive file, or {@code null} otherwise.
     * <p>
     * This method always returns a normalized path, i.e. all occurences of
     * {@code "."} and {@code ".."} in the path name are removed according to
     * their meaning wherever possible.
     * <p>
     * In order to support unlimited nesting levels, this method returns
     * a {@code TFile} instance which may recursively address an entry within
     * another archive file.
     * 
     * @return The enclosing archive file in this path.
     */
    public @CheckForNull TFile getEnclArchive() {
        return enclArchive;
    }

    /**
     * Returns the entry name relative to the enclosing archive file.
     * That is, if this object addresses an entry located within an archive
     * file, then this method returns the relative path of the entry in the
     * enclosing archive file separated by the entry separator character
     * {@code '/'}, or {@code null} otherwise.
     * <p>
     * This method always returns an undotified path, i.e. all redundant
     * occurences of {@code "."} and {@code ".."} in the path are removed
     * wherever possible.
     * 
     * @return The entry name relative to the enclosing archive file.
     */
    public @Nullable String getEnclEntryName() {
        return null == enclEntryName ? null : enclEntryName.getPath();
    }

    @Nullable FsEntryName getEnclEntryName0() {
        return enclEntryName;
    }

    /**
     * Returns the {@link TArchiveDetector} that was used to detect any archive
     * files in the path of this file object at construction time.
     * 
     * @return The {@link TArchiveDetector} that was used to detect any archive
     *         files in the path of this file object at construction time.
     */
    public TArchiveDetector getArchiveDetector() {
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
     * <p>
     * TODO: Consider making this public in order to enable applications to
     * get access to archive entry properties like this:
     * <pre><code>
     * </code></pre>
     * 
     * @return A file system controller if and only if the path denotes an
     *         archive file, or {@code null} otherwise.
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
        return controller = manager.getController(mountPoint, detector);
    }

    /**
     * Returns {@code true} if and only if the path represented
     * by this instance is a direct or indirect parent of the path
     * represented by the given {@code file}.
     * <p>
     * Note:
     * <ul>
     * <li>This method uses the absolute path name of the given files.
     * <li>This method does <em>not</em> access the file system.
     *     It just tests the paths.
     * </ul>
     *
     * @param file The file object for the path to test for being a direct or
     *        indirect child of the path of this instance.
     * @return {@code true} if and only if the path represented
     *         by this instance is a direct or indirect parent of the path
     *         represented by the given {@code file}.
     */
    public boolean isParentOf(File file) {
        String a = this.getAbsolutePath();
        String b = file.getAbsoluteFile().getParent();
        return b != null ? Paths.contains(a, b, separatorChar) : false;
    }

    /**
     * Returns {@code true} if and only if the path represented
     * by this instance contains the path represented by the given {@code file},
     * where a path is said to contain another path if and only
     * if it's equal or an ancestor of the other path.
     * <p>
     * Note:
     * <ul>
     * <li>This method uses the absolute path name of the given files.
     * <li>This method does <em>not</em> access the file system.
     *     It just tests the paths.
     * </ul>
     *
     * @param  file The file object for the path to test for being contained by
     *         the path of this instance.
     * @return {@code true} if and only if the path represented
     *         by this instance contains the path represented by the given
     *         {@code file}
     * @throws NullPointerException If the parameter is {@code null}.
     */
    public boolean contains(File file) {
        return contains(this, file);
    }

    /**
     * Returns {@code true} if and only if the path represented
     * by {@code a} contains the path represented by {@code b},
     * where a path is said to contain another path if and only
     * if it's equal or an ancestor of the other path.
     * <p>
     * Note:
     * <ul>
     * <li>This method uses the absolute path name of the given files.
     * <li>This method does <em>not</em> access the file system.
     *     It just tests the paths.
     * </ul>
     *
     * @param  a the file to test for containing {@code b}.
     * @param  b the file to test for being contained by {@code a}.
     * @return {@code true} if and only if the path represented
     *         by {@code a} contains the path represented by {@code b}.
     * @throws NullPointerException If any parameter is {@code null}.
     */
    public static boolean contains(File a, File b) {
        return Paths.contains(  a.getAbsolutePath(),
                                b.getAbsolutePath(),
                                separatorChar);
    }

    /**
     * Returns {@code true} if and only if this file denotes a file system
     * root or a UNC (if running on the Windows platform).
     * 
     * @return {@code true} if and only if this file denotes a file system
     *         root or a UNC (if running on the Windows platform).
     */
    public boolean isFileSystemRoot() {
        TFile canOrAbsFile = getCanOrAbsFile();
        return ROOTS.contains(canOrAbsFile) || isUNC(canOrAbsFile.getPath());
    }

    /**
     * Returns {@code true} if and only if this file denotes a UNC.
     * Note that this should be relevant on the Windows platform only.
     * 
     * @return {@code true} if and only if this file denotes a UNC.
     */
    public boolean isUNC() {
        return isUNC(getCanOrAbsPath());
    }

    /**
     * Returns {@code true} if and only if this file denotes a UNC.
     * Note that this may be only relevant on the Windows platform.
     * 
     * @param  path a file path.
     * @return {@code true} if and only if {@code path} denotes a UNC.
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
     * {@code a.toFsPath().toHierarchicalUri().equals(b.toFsPath().toHierarchicalUri())}
     * is true, the expression {@code a.equals(b)} is also true.
     * <p>
     * Note that this does <em>not</em> work vice versa:
     * E.g. on Windows, the expression
     * {@code new TFile("file").equals(new TFile("FILE"))} is true, but
     * {@code new TFile("file").toFsPath().toHierarchicalUri().equals(new TFile("FILE").toFsPath().toHierarchicalUri())}
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
     * {@inheritDoc }
     * <p>
     * The implementation in the class {@link TFile} delegates the call to its
     * {@link #getFile() decorated file} object.
     * This implies that only the hierarchicalized file system path
     * of this file instance is considered in the comparison.
     * E.g. {@code new TFile(FsPath.create("zip:file:/archive!/entry"))} and
     * {@code new TFile(FsPath.create("tar:file:/archive!/entry"))} would
     * compare equal because their hierarchicalized file system path is
     * {@code "file:/archive/entry"}.
     * <p>
     * More formally, let {@code a} and {@code b} be two TFile objects.
     * Then if the expression
     * {@code a.toFsPath().toHierarchicalUri().compareTo(b.toFsPath().toHierarchicalUri()) == 0}
     * is true, the expression {@code a.compareTo(b) == 0} is also true.
     * <p>
     * Note that this does <em>not</em> work vice versa:
     * E.g. on Windows, the expression
     * {@code new TFile("file").compareTo(new TFile("FILE")) == 0} is true, but
     * {@code new TFile("file").toFsPath().toHierarchicalUri().compareTo(new TFile("FILE").toFsPath().toHierarchicalUri()) == 0}
     * is false because {@link FsPath#equals(Object)} is case sensitive.
     *
     * @see   #equals(Object)
     */
    @Override
    public int compareTo(File other) {
        return delegate.compareTo(other);
    }

    /**
     * Returns The top level archive file in the path or {@code null}
     * if this file object does not name an archive file.
     * A top level archive is not enclosed in another archive.
     * If this does not return {@code null}, this denotes the longest
     * part of the path which actually may (but does not need to) exist
     * as a plain file in the OS file system.
     */
    public TFile getTopLevelArchive() {
        final TFile enclArchive = this.enclArchive;
        return null != enclArchive
                ? enclArchive.getTopLevelArchive()
                : innerArchive;
    }

    /** {@inheritDoc} */
    @Override
    public String getPath() {
        return delegate.getPath();
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return delegate.getName();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAbsolute() {
        return delegate.isAbsolute();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isHidden() {
        return delegate.isHidden();
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
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
                                enclEntryName)).toUri();
                } else {
                    return new FsMountPoint(scheme, new FsPath(delegate)).toUri();
                }
            } else if (null != enclArchive) {
                return new FsPath(
                        new FsMountPoint(enclArchive.toURI(), CANONICALIZE),
                        enclEntryName).toUri();
            } else {
                return delegate.toURI();
            }
        } catch (URISyntaxException ex) {
            throw new AssertionError(ex);
        }
    }

    /** {@inheritDoc} */
    @Deprecated
    @Override
    public URL toURL() throws MalformedURLException {
        return null != innerArchive ? toURI().toURL() : delegate.toURL();
    }

    /**
     * This file system operation is <a href="package-summary.html#atomicity">virtually atomic</a>.
     *
     * @see <a href="#falsePositives">Detecting Archive Paths and False Positives</a>
     */
    @Override
    public boolean exists() {
        // This methods deviates from the general pattern of testing if
        // innerArchive is not null.
        // This optimization avoids mounting an archive file system just in
        // order to test for its existence.
        // See http://java.net/jira/browse/TRUEZIP-77
        if (null != enclArchive) {
            try {
                return null != enclArchive.getController()
                        .getEntry(getEnclEntryName0());
            } catch (IOException ex) {
                return false;
            }
        }
        return delegate.exists();
    }

    /**
     * Similar to its super class implementation, but returns
     * {@code false} for a valid archive file, too.
     * <p>
     * For archive file validation its virtual file system gets mounted.
     * In case a RAES encrypted ZIP file gets mounted, the user gets prompted
     * for its password unless the default configuration for key management
     * hasn't been overridden.
     * <p>
     * This file system operation is <a href="package-summary.html#atomicity">virtually atomic</a>.
     *
     * @see <a href="#falsePositives">Detecting Archive Paths and False Positives</a>
     */
    @Override
    public boolean isFile() {
        if (null != innerArchive) {
            try {
                FsEntry entry = innerArchive.getController()
                        .getEntry(getInnerEntryName0());
                return null != entry && entry.isType(FILE);
            } catch (IOException ex) {
                return false;
            }
        }
        return delegate.isFile();
    }

    /**
     * Similar to its super class implementation, but returns
     * {@code true} for a valid archive file, too.
     * <p>
     * For archive file validation its virtual file system gets mounted.
     * In case a RAES encrypted ZIP file gets mounted, the user gets prompted
     * for its password unless the default configuration for key management
     * hasn't been overridden.
     * <p>
     * This file system operation is <a href="package-summary.html#atomicity">virtually atomic</a>.
     *
     * @see <a href="#falsePositives">Detecting Archive Paths and False Positives</a>
     * @see #isArchive
     * @see #isEntry
     */
    @Override
    public boolean isDirectory() {
        if (null != innerArchive) {
            try {
                FsEntry entry = innerArchive.getController()
                        .getEntry(getInnerEntryName0());
                return null != entry && entry.isType(DIRECTORY);
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
     * 
     * @return An icon or {@code null}.
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
     * 
     * @return An icon or {@code null}.
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

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
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
     * Returns the (uncompressed) length of the file.
     * The length returned of a valid archive file is {@code 0} in order
     * to properly emulate virtual directories across all platforms.
     * <p>
     * For archive file validation its virtual file system gets mounted.
     * In case a RAES encrypted ZIP file gets mounted, the user gets prompted
     * for its password unless the default configuration for key management
     * hasn't been overridden.
     * <p>
     * This file system operation is <a href="package-summary.html#atomicity">virtually atomic</a>.
     *
     * @see <a href="#falsePositives">Detecting Archive Paths and False Positives</a>
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
            if (null == entry || entry.isType(DIRECTORY))
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
     * as a plain directory within the archive file.
     * <p>
     * Note that calling this method may incur a severe performance penalty
     * if the file is an entry in an archive file which has just been written
     * (such as after a normal copy operation).
     * If you want to copy a file's contents as well as its last modification
     * time, use {@link #cp_p(File, File)} instead.
     * <p>
     * This file system operation is <a href="package-summary.html#atomicity">virtually atomic</a>.
     *
     * @see #cp_p(File, File)
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
            @CheckForNull FilenameFilter filter) {
        return listFiles(filter, detector);
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
     * @param  filter the file filter.
     * @param  detector the archive detector to detect any archives files in
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
    public @Nullable TFile[] listFiles(@CheckForNull FileFilter filter) {
        return listFiles(filter, detector);
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
     * @param  filter the file filter.
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

    /** {@inheritDoc} */
    @Override
    public boolean mkdirs() {
        if (null == innerArchive)
            return delegate.mkdirs();

        final TFile parent = getParentFile();
        if (null != parent && !parent.exists())
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
     * {@code new TFile("archive.zip").mkdir();}
     * <p>
     * Alternatively, archive files get created automatically by simply
     * creating their entries.
     * Example:
     * {@code new TFileOutputStream("archive.zip/README");}
     * This assumes the default configuration where {@link #isLenient} returns
     * {@code true}.
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
     * Ensures that a (virtual) directory with {@link #getPath() this path name}
     * exists in the (federated) file system.
     * 
     * @param  recursive whether or not any missing ancestor directories shall
     *         get created if required.
     * @return {@code this}
     * @throws IOException if any I/O error occurs.
     */
    public TFile mkdir(final boolean recursive) throws IOException {
        if (null != innerArchive) {
            if (recursive) {
                final TFile parent = getParentFile();
                if (null != parent && !parent.exists())
                    parent.mkdir(recursive);
            }
            final FsController<?> controller = innerArchive.getController();
            final FsEntryName innerEntryName = getInnerEntryName0();
            try {
                controller.mknod(
                        innerEntryName,
                        DIRECTORY,
                        BitField.of(EXCLUSIVE) // redundant for directory entries
                            .set(CREATE_PARENTS, isLenient()),
                        null);
            } catch (IOException ex) {
                final FsEntry entry = controller.getEntry(innerEntryName);
                if (null == entry || !entry.isType(DIRECTORY))
                    throw ex;
            }
        } else {
            final File dir = delegate;
            if (!(recursive ? dir.mkdirs() : dir.mkdir()) && !dir.isDirectory())
                throw new IOException(dir + " (cannot create directory)");
        }
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @deprecated This method just returns a boolean value to indicate failure,
     *             which is hard to analyze.
     * @see #rm()
     */
    @Deprecated
    @Override
    public boolean delete() {
        try {
            rm(this);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Equivalent to {@link #rm(File) rm(this)}.
     * 
     * @return {@code this}
     * @throws IOException if any I/O error occurs.
     * @see    <a href="#bulkIOMethods">Bulk I/O Methods</a>
     */
    public TFile rm() throws IOException {
        rm(this);
        return this;
    }

    /**
     * Deletes the given file or directory.
     * If the file is a directory, it must be empty.
     * <p>
     * This file system operation is <a href="package-summary.html#atomicity">virtually atomic</a>.
     *
     * @param  node the file or directory.
     * @throws IOException if any I/O error occurs.
     * @see    <a href="#bulkIOMethods">Bulk I/O Methods</a>
     */
    public static void rm(File node) throws IOException {
        if (node instanceof TFile) {
            TFile file = (TFile) node;
            if (null != file.innerArchive) {
                file.innerArchive.getController().unlink(file.getInnerEntryName0());
                return;
            }
            node = file.delegate;
        }
        if (!node.delete())
            throw new IOException(node + " (cannot delete)");
    }

    /**
     * Equivalent to {@link #rm_r(File) rm_r(this)}.
     * 
     * @return {@code this}
     * @throws IOException if any I/O error occurs.
     * @see    <a href="#bulkIOMethods">Bulk I/O Methods</a>
     * @see    <a href="#traversal">Traversing Directory Trees</a>
     */
    public TFile rm_r() throws IOException {
        TBIO.rm_r(this, detector);
        return this;
    }

    /**
     * Recursively deletes the given file or directory tree.
     * <p>
     * If {@code node} is an instance of this
     * class, its {@link #getArchiveDetector() archive detector}
     * is used to detect prospective archive files in the directory tree.
     * Otherwise,
     * {@link TArchiveDetector#NULL}
     * is used to detect prospective archive files in the directory tree.
     * <p>
     * This file system operation is <em>not</em> atomic.
     * 
     * @param  node the file or directory tree.
     * @throws IOException if any I/O error occurs.
     * @see    <a href="#bulkIOMethods">Bulk I/O Methods</a>
     * @see    <a href="#traversal">Traversing Directory Trees</a>
     */
    public static void rm_r(File node) throws IOException {
        TBIO.rm_r(node,
                node instanceof TFile
                    ? ((TFile) node).detector
                    : TArchiveDetector.NULL);
    }

    @Override
    public void deleteOnExit() {
        if (innerArchive != null) {
            // Support for this operation for archive files and entries has been
            // removed in TrueZIP 7 because using a shutdown hook uncautiously
            // introduces a potential memory leak when using multiple class
            // loaders to load TrueZIP.
            throw new UnsupportedOperationException();
        }
        delegate.deleteOnExit();
    }

    /**
     * {@inheritDoc}
     * 
     * @param  dst the destination file or directory tree.
     * @return {@code true} on success or {@code false} otherwise.
     * @deprecated This method just returns a boolean value to indicate failure,
     *             which is hard to analyze.
     * @see #mv(File)
     */
    @Deprecated
    @Override
    public boolean renameTo(final File dst) {
        try {
            mv(this, dst, detector);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Equivalent to {@link #mv(File, File, TArchiveDetector) mv(this, dst, getArchiveDetector())}.
     * 
     * @param  dst the destination file or directory tree.
     * @return {@code this}
     * @throws IOException if any I/O error occurs.
     * @see    <a href="#bulkIOMethods">Bulk I/O Methods</a>
     * @see    <a href="#traversal">Traversing Directory Trees</a>
     */
    public TFile mv(File dst) throws IOException {
        mv(this, dst, detector);
        return this;
    }

    /**
     * Moves the given source file or directory to the given destination file
     * or directory.
     * <p>
     * In certain cases, this method might perform a recursive copy-then-delete
     * operation rather than an atomic move operation.
     * This method uses the given archive detector to detect any
     * archive files when traversing a directory tree.
     * 
     * @param  src the source file or directory tree.
     * @param  dst the destination file or directory tree.
     * @param  detector the archive detector to use for detecting any archive
     *         files in the source directory tree.
     * @throws IOException if any I/O error occurs.
     * @see    <a href="#bulkIOMethods">Bulk I/O Methods</a>
     * @see    <a href="#traversal">Traversing Directory Trees</a>
     */
    public static void mv(  final File src,
                            final File dst,
                            final TArchiveDetector detector)
    throws IOException {
        if (detector.toString().isEmpty()) {
            final boolean srcArchived;
            final File srcDelegate;
            if (src instanceof TFile) {
                final TFile srcFile = (TFile) src;
                srcArchived = null != srcFile.innerArchive;
                srcDelegate = srcFile.delegate;
            } else {
                srcArchived = false;
                srcDelegate = src;
            }
            final boolean dstArchived;
            final File dstDelegate;
            if (dst instanceof TFile) {
                final TFile dstFile = (TFile) dst;
                dstArchived = null != dstFile.innerArchive;
                dstDelegate = dstFile.delegate;
            } else {
                dstArchived = false;
                dstDelegate = src;
            }
            if (!srcArchived && !dstArchived)
                if (srcDelegate.renameTo(dstDelegate))
                    return;
                else
                    throw new IOException(src + " (cannot move to " + dst + ")");
        }
        TBIO.mv(src, dst, detector);
    }

    /**
     * Copies the data from the input stream {@code in} to the output stream
     * {@code out} and closes both streams - even if an exception occurs.
     * <p>
     * This is a high performance implementation which uses a pooled background
     * thread to fill a FIFO of pooled buffers which is concurrently flushed by
     * the current thread.
     * It performs best when used with <em>unbuffered</em> streams.
     * <table border="2" cellpadding="4">
     * <thead>
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * </thead>
     * <tbody>
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
     *   <td><a href="#directDataCopying">Direct Data Copying (DDC)</a></td>
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
     * </tbody>
     * </table>
     *
     * @param  in the input stream.
     * @param  out the output stream.
     * @throws IOException if any I/O error occurs.
     * @see    #cat(InputStream, OutputStream)
     * @see    <a href="#bulkIOMethods">Bulk I/O Methods</a>
     */
    public static void cp(InputStream in, OutputStream out)
    throws IOException {
        Streams.copy(in, out);
    }

    /**
     * Copies the input stream {@code in} to the file {@code dst} and
     * closes the stream - even if an exception occurs.
     * <table border="2" cellpadding="4">
     * <thead>
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * </thead>
     * <tbody>
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
     *   <td><a href="#directDataCopying">Direct Data Copying (DDC)</a></td>
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
     * </tbody>
     * </table>
     *
     * @param  in the input stream.
     * @param  dst the destination file. Note that although this just needs to
     *         be a plain {@code File}, archive entries are only
     *         supported for instances of this class.
     * @throws IOException if any I/O error occurs.
     * @see    <a href="#bulkIOMethods">Bulk I/O Methods</a>
     */
    public static void cp(final InputStream in, final File dst)
    throws IOException {
        final OutputStream out = new TFileOutputStream(dst);
        try {
            cp(in, out); // always closes in and out
        } catch (IOException ex) {
            if (!dst.delete())
                throw new IOException(dst + " (cannot delete)", ex);
        }
    }

    /**
     * Copies the file {@code src} to the output stream {@code out} and
     * closes the stream - even if an exception occurs.
     * <table border="2" cellpadding="4">
     * <thead>
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * </thead>
     * <tbody>
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
     *   <td><a href="#directDataCopying">Direct Data Copying (DDC)</a></td>
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
     * </tbody>
     * </table>
     *
     * @param  src the source file. Note that although this just needs to
     *         be a plain {@code File}, archive entries are only
     *         supported for instances of this class.
     * @param  out the output stream.
     * @throws IOException if any I/O error occurs.
     * @see    <a href="#bulkIOMethods">Bulk I/O Methods</a>
     */
    public static void cp(File src, OutputStream out)
    throws IOException {
        cp(new TFileInputStream(src), out); // always closes in and out
    }

    /**
     * Equivalent to {@link #cp(File, File) cp(this, dst)}.
     * 
     * @param  dst the destination file.
     * @return {@code this}
     * @throws IOException if any I/O error occurs.
     */
    public TFile cp(File dst) throws IOException {
        TBIO.cp(false, this, dst);
        return this;
    }

    /**
     * Copies the file {@code src} to the file {@code dst}.
     * <table border="2" cellpadding="4">
     * <thead>
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * </thead>
     * <tbody>
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
     *   <td><a href="#directDataCopying">Direct Data Copying (DDC)</a></td>
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
     * </tbody>
     * </table>
     * 
     * @param  src the source file. Note that although this just needs to
     *         be a plain {@code File}, archive entries are only
     *         supported for instances of this class.
     * @param  dst the destination file. Note that although this just needs to
     *         be a plain {@code File}, archive entries are only
     *         supported for instances of this class.
     * @throws IOException if any I/O error occurs.
     * @see    <a href="#bulkIOMethods">Bulk I/O Methods</a>
     */
    public static void cp(File src, File dst)
    throws IOException {
        TBIO.cp(false, src, dst);
    }

    /**
     * Equivalent to {@link #cp_p(File, File) cp_p(this, dst)}.
     * 
     * @param  dst the destination file.
     * @return {@code this}
     * @throws IOException if any I/O error occurs.
     */
    public TFile cp_p(File dst) throws IOException {
        TBIO.cp(true, this, dst);
        return this;
    }

    /**
     * Copies the file {@code src} to the file {@code dst} and tries to
     * preserve all attributes of the source file to the destination file, too.
     * Currently, only the last modification time is preserved.
     * <table border="2" cellpadding="4">
     * <thead>
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * </thead>
     * <tbody>
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
     *   <td><a href="#directDataCopying">Direct Data Copying (DDC)</a></td>
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
     * </tbody>
     * </table>
     * 
     * @param  src the source file. Note that although this just needs to
     *         be a plain {@code File}, archive entries are only
     *         supported for instances of this class.
     * @param  dst the destination file. Note that although this just needs to
     *         be a plain {@code File}, archive entries are only
     *         supported for instances of this class.
     * @throws IOException if any I/O error occurs.
     * @see    <a href="#bulkIOMethods">Bulk I/O Methods</a>
     */
    public static void cp_p(File src, File dst)
    throws IOException {
        TBIO.cp(true, src, dst);
    }

    /**
     * Equivalent to {@link #cp_r(File, File, TArchiveDetector, TArchiveDetector) cp_r(this, dst, getArchiveDetector(), getArchiveDetector())}.
     * 
     * @param  dst the destination file or directory tree.
     * @return {@code this}
     * @throws IOException if any I/O error occurs.
     * @see    <a href="#bulkIOMethods">Bulk I/O Methods</a>
     * @see    <a href="#traversal">Traversing Directory Trees</a>
     */
    public TFile cp_r(File dst) throws IOException {
        TBIO.cp_r(false, this, dst, detector, detector);
        return this;
    }

    /**
     * Recursively copies the file or directory {@code src}
     * to the file or directory {@code dst}.
     * <p>
     * This method uses the given archive detectors to detect any
     * archive files in the source and destination directory trees.
     * <table border="2" cellpadding="4">
     * <thead>
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * </thead>
     * <tbody>
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
     *   <td><a href="#directDataCopying">Direct Data Copying (DDC)</a></td>
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
     * </tbody>
     * </table>
     *
     * @param  src the source file. Note that although this just needs to
     *         be a plain {@code File}, archive files and entries
     *         are only supported for instances of this class.
     * @param  dst the destination file. Note that although this just needs to
     *         be a plain {@code File}, archive files and entries
     *         are only supported for instances of this class.
     * @param  srcDetector the archive detector to use for detecting any
     *         archive files in the source directory tree.
     * @param  dstDetector the archive detector to use for detecting any
     *         archive files in the destination directory tree.
     * @throws IOException if any I/O error occurs.
     * @see    <a href="#bulkIOMethods">Bulk I/O Methods</a>
     * @see    <a href="#traversal">Traversing Directory Trees</a>
     */
    public static void cp_r(File src, File dst,
                            TArchiveDetector srcDetector,
                            TArchiveDetector dstDetector)
    throws IOException {
        TBIO.cp_r(false, src, dst, srcDetector, dstDetector);
    }

    /**
     * Equivalent to {@link #cp_rp(File, File, TArchiveDetector, TArchiveDetector) cp_rp(this, dst, getArchiveDetector(), getArchiveDetector())}.
     * 
     * @param  dst the destination file or directory tree.
     * @return {@code this}
     * @throws IOException if any I/O error occurs.
     * @see    <a href="#bulkIOMethods">Bulk I/O Methods</a>
     * @see    <a href="#traversal">Traversing Directory Trees</a>
     */
    public TFile cp_rp(File dst) throws IOException {
        TBIO.cp_r(true, this, dst, detector, detector);
        return this;
    }

    /**
     * Recursively copies the file or directory {@code src} to the file or
     * directory {@code dst} and tries to preserve all attributes of each
     * source file to the destination file, too.
     * Note that the current implementation only preserves the last
     * modification time.
     * <p>
     * This method uses the given archive detectors to detect any
     * archive files when traversing a directory tree.
     * <table border="2" cellpadding="4">
     * <thead>
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * </thead>
     * <tbody>
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
     *   <td><a href="#directDataCopying">Direct Data Copying (DDC)</a></td>
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
     * </tbody>
     * </table>
     *
     * @param  src the source file. Note that although this just needs to
     *         be a plain {@code File}, archive files and entries
     *         are only supported for instances of this class.
     * @param  dst the destination file. Note that although this just needs to
     *         be a plain {@code File}, archive files and entries
     *         are only supported for instances of this class.
     * @param  srcDetector the archive detector to use for detecting any
     *         archive files in the source directory tree.
     * @param  dstDetector the archive detector to use for detecting any
     *         archive files in the destination directory tree.
     * @throws IOException if any I/O error occurs.
     * @see    <a href="#bulkIOMethods">Bulk I/O Methods</a>
     * @see    <a href="#traversal">Traversing Directory Trees</a>
     */
    public static void cp_rp(   File src, File dst,
                                TArchiveDetector srcDetector,
                                TArchiveDetector dstDetector)
    throws IOException {
        TBIO.cp_r(true, src, dst, srcDetector, dstDetector);
    }

    /**
     * Copies the input stream {@code in} to this file or entry in an archive
     * file
     * <em>without</em> closing it.
     * <table border="2" cellpadding="4">
     * <thead>
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * </thead>
     * <tbody>
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
     *   <td><a href="#directDataCopying">Direct Data Copying (DDC)</a></td>
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
     * </tbody>
     * </table>
     *
     * @param  in the input stream.
     * @throws IOException if any I/O error occurs.
     * @see    <a href="#bulkIOMethods">Bulk I/O Methods</a>
     */
    public void input(final InputStream in) throws IOException {
        final OutputStream out = new TFileOutputStream(this, false);
        try {
            try {
                Streams.cat(in, out);
            } finally {
                out.close();
            }
        } catch (IOException ex) {
            delete();
            throw ex;
        }
    }

    /**
     * Copies this file or entry in an archive file to the output stream
     * {@code out}
     * <em>without</em> closing it.
     * <table border="2" cellpadding="4">
     * <thead>
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * </thead>
     * <tbody>
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
     *   <td><a href="#directDataCopying">Direct Data Copying (DDC)</a></td>
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
     * </tbody>
     * </table>
     *
     * @param  out the output stream.
     * @throws IOException if any I/O error occurs.
     * @see    <a href="#bulkIOMethods">Bulk I/O Methods</a>
     */
    public void output(final OutputStream out) throws IOException {
        final InputStream in = new TFileInputStream(this);
        try {
            Streams.cat(in, out);
        } finally {
            in.close();
        }
    }

    /**
     * Copies the data from the given input stream to the given output stream
     * <em>without</em> closing them.
     * The name of this method is inspired by the Unix command line utility
     * {@code cat} because you could use it to con<em>cat</em>enate the
     * contents of multiple streams.
     * <p>
     * This is a high performance implementation which uses a pooled background
     * thread to fill a FIFO of data buffers which is concurrently flushed by
     * the current thread.
     * It performs best when used with <em>unbuffered</em> streams.
     * <table border="2" cellpadding="4">
     * <thead>
     * <tr>
     *   <th>Feature</th>
     *   <th>Supported</th>
     * </tr>
     * </thead>
     * <tbody>
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
     *   <td><a href="#directDataCopying">Direct Data Copying (DDC)</a></td>
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
     * </tbody>
     * </table>
     *
     * @param  in the input stream.
     * @param  out the output stream.
     * @throws IOException if any I/O error occurs.
     * @see    #cp(InputStream, OutputStream)
     * @see    <a href="#bulkIOMethods">Bulk I/O Methods</a>
     */
    public static void cat(final InputStream in, final OutputStream out)
    throws IOException {
        Streams.cat(in, out);
    }
}
