/*
 * Copyright (C) 2004-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.file;

import static de.schlichtherle.truezip.entry.Entry.Size.*;
import static de.schlichtherle.truezip.entry.Entry.Type.*;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsEntry;
import static de.schlichtherle.truezip.fs.FsEntry.*;
import de.schlichtherle.truezip.fs.FsEntryName;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import de.schlichtherle.truezip.fs.FsFilteringManager;
import de.schlichtherle.truezip.fs.FsMountPoint;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import de.schlichtherle.truezip.fs.FsPath;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.fs.FsSyncException;
import de.schlichtherle.truezip.fs.FsSyncOption;
import static de.schlichtherle.truezip.fs.FsSyncOption.*;
import static de.schlichtherle.truezip.fs.FsSyncOptions.*;
import de.schlichtherle.truezip.fs.FsSyncWarningException;
import static de.schlichtherle.truezip.fs.FsUriModifier.*;
import de.schlichtherle.truezip.io.Paths;
import de.schlichtherle.truezip.io.Paths.Splitter;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.UriBuilder;
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
import java.util.ServiceConfigurationError;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;
import net.jcip.annotations.Immutable;

/**
 * A replacement for the class {@link File} which provides transparent
 * read/write access to archive files and their entries as if they were
 * (virtual) directories and files.
 * Because this class actually extends the class {@link File} it can get used
 * polymorphically with the class {@link FileSystemView} or any other class
 * which depends on the class {@link File}.
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
 * additional temporary file, but shows no impact otherwise because the TAR
 * file format doesn't support compression.
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
 * In fact, it could be anything, even a plain directory in the platform file system!
 * <p>
 * Such an invalid archive file is called a <i>false positive</i> archive file.
 * TrueZIP correctly identifies all types of false positive archive files by
 * performing a recursive look up operation for the first parent file system
 * where the respective prospective archive file actually exists and treats it
 * according to its <i>true state</i>.
 * <p>
 * The following table shows how certain methods in this class behave,
 * depending upon an archive file's path and its <i>true state</i> in the
 * first parent file system where it actually exists.
 * <p>
 * <table border=1 cellpadding=5 summary="">
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
 *     platform file system, which is indicated by <i>{@code ?}</i>.
 *     For regular directories on Windows/NTFS for example, the return value
 *     would be {@code 0}.
 * <li>This example presumes that the JAR of the module
 *     TrueZIP&nbsp;Driver&nbsp;ZIP is present on the run time class path.</li>
 * <li>This example presumes that the JAR of the module
 *     TrueZIP&nbsp;Driver&nbsp;ZIP.RAES&nbsp;(TZP) is present on the run time
 *     class path.</li>
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

    private static final File CURRENT_DIRECTORY = new File(".");

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
     * new TFile(template, (TArchiveDetector) null)}.
     * 
     * @param file the file object to decorate.
     */
    public TFile(File file) {
        this(file, (TArchiveDetector) null);
    }

    /**
     * Constructs a new {@code TFile} instance which may use the given archive
     * detector to scan the path name for prospective archive files.
     * 
     * @param file the file object to decorate.
     *        If this is an instance of this class, its fields are copied and
     *        the {@code detector} parameter is ignored.
     * @param detector the archive detector to use for scanning the path name
     *        for prospective archive files.
     *        This parameter is ignored if and only if {@code file} is an
     *        instance of this class.
     *        Otherwise, if it's {@code null}, then the
     *        {@link #getDefaultArchiveDetector() default archive detector} is
     *        used instead.
     */
    // TODO: For TrueZIP 8, consider removing the copy construction feature.
    // This is useless in Java.
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
            this.detector = null != detector ? detector : TConfig.get().getArchiveDetector();
            scan(null);
        }

        assert invariants();
    }

    /**
     * Equivalent to {@link #TFile(String, TArchiveDetector)
     * new TFile(path, (TArchiveDetector) null)}.
     * <p>
     * The {@link #getDefaultArchiveDetector() default archive detector}
     * is used to scan the path name for prospective archive files.
     * 
     * @param path the path name.
     */
    public TFile(String path) {
        this(path, (TArchiveDetector) null);
    }

    /**
     * Constructs a new {@code TFile} instance which may use the given
     * {@link TArchiveDetector} to scan its path name for prospective archive
     * files.
     *
     * @param path the path name.
     * @param detector the archive detector to use for scanning the path name
     *        for prospective archive files.
     *        If this is {@code null}, then the
     *        {@link #getDefaultArchiveDetector() default archive detector} is
     *        used instead.
     */
    public TFile(   final String path,
                    final @CheckForNull TArchiveDetector detector) {
        super(path);

        this.delegate = new File(path);
        this.detector = null != detector ? detector : TConfig.get().getArchiveDetector();
        scan(null);

        assert invariants();
    }

    /**
     * Equivalent to {@link #TFile(String, String, TArchiveDetector)
     * new TFile(parent, child, null)}.
     * <p>
     * The {@link #getDefaultArchiveDetector() default archive detector}
     * is used to scan the <em>entire path name</em> for prospective archive
     * files.
     *
     * @param parent the parent directory.
     * @param member the member path name.
     */
    public TFile(@CheckForNull String parent, String member) {
        this(parent, member, null);
    }

    /**
     * Constructs a new {@code TFile} instance which may use the given archive
     * detector to scan the <em>entire path name</em> for prospective archive
     * files.
     *
     * @param parent the parent directory.
     * @param member the member path name.
     * @param detector the archive detector to use for scanning the path name
     *        for prospective archive files.
     *        If this is {@code null}, then the
     *        {@link #getDefaultArchiveDetector() default archive detector} is
     *        used instead.
     */
    public TFile(
            final @CheckForNull String parent,
            final String member,
            final @CheckForNull TArchiveDetector detector) {
        super(parent, member);

        this.delegate = new File(parent, member);
        this.detector = null != detector ? detector : TConfig.get().getArchiveDetector();
        scan(null);

        assert invariants();
    }

    /**
     * Equivalent to {@link #TFile(File, String, TArchiveDetector)
     * new TFile(parent, child, null)}.
     *
     * @param parent the parent directory.
     *        If this parameter is an instance of this class, its archive
     *        detector is used to scan only the <em>member path name</em>
     *        for prospective archive files.
     *        Otherwise, the
     *        {@link #getDefaultArchiveDetector() default archive detector}
     *        is used to to scan the <em>entire path name</em>
     *        for prospective archive files.
     * @param member a member path name.
     */
    public TFile(@CheckForNull File parent, String member) {
        this(parent, member, null);
    }

    /**
     * Constructs a new {@code TFile} instance which may use the given archive
     * detector to scan the path name for prospective archive files.
     * <p>
     * If {@code parent} is an instance of this class,
     * then {@code detector} is used to scan only the <em>member path name</em>
     * for prospective archive files.
     * If {@code detector} is {@code null}, then the
     * parent's archive detector
     * is used instead.
     * <p>
     * Otherwise, if {@code parent} is not an instance of this class,
     * then {@code detector} is used to scan the <em>entire path name</em>
     * for prospective archive files.
     * If {@code detector} is {@code null}, then the
     * {@link #getDefaultArchiveDetector() default archive detector}
     * is used instead.
     * 
     * @param parent the parent directory.
     * @param member the member path name.
     * @param detector the archive detector to use for scanning the path name
     *        for prospective archive files.
     */
    public TFile(
            final @CheckForNull File parent,
            final String member,
            final @CheckForNull TArchiveDetector detector) {
        super(parent, member);

        this.delegate = new File(parent, member);
        if (parent instanceof TFile) {
            final TFile p = (TFile) parent;
            this.detector = null != detector ? detector : p.detector;
            scan(p);
        } else {
            this.detector = null != detector ? detector : TConfig.get().getArchiveDetector();
            scan(null);
        }

        assert invariants();
    }

    /**
     * Constructs a new {@code TFile} instance from the given {@code uri}.
     * This constructor is equivalent to
     * <code>new {@link #TFile(FsPath, TArchiveDetector) TFile(FsPath.create(uri, CANONICALIZE), TConfig.get().getArchiveDetector())}</code>,
     *
     * @param  uri an absolute URI which has a scheme component which is
     *         known by the
     *         {@link #getDefaultArchiveDetector() default archive detector}.
     * @throws IllegalArgumentException if the given URI does not conform to
     *         the syntax constraints for {@link FsPath}s or
     *         {@link File#File(URI)}.
     * @see    #toURI()
     * @see    #TFile(FsPath)
     */
    public TFile(URI uri) {
        this(FsPath.create(uri, CANONICALIZE), TConfig.get().getArchiveDetector());
    }

    /**
     * Constructs a new {@code TFile} instance from the given {@code path}.
     * This constructor is equivalent to
     * <code>new {@link #TFile(FsPath, TArchiveDetector) TFile(path, TConfig.get().getArchiveDetector())}</code>
     *
     * @param  path a path with an absolute
     *         {@link FsPath#toHierarchicalUri() hierarchical URI} which has a
     *         scheme component which is known by the
     *         {@link #getDefaultArchiveDetector() default archive detector}.
     * @throws IllegalArgumentException if the
     *         {@link FsPath#toHierarchicalUri() hierarchical URI} of the given
     *         path does not conform to the syntax constraints for
     *         {@link File#File(URI)}.
     * @see    #toFsPath()
     * @see    #TFile(URI)
     */
    public TFile(FsPath path) {
        this(path, TConfig.get().getArchiveDetector());
    }

    /**
     * Constructs a new {@code TFile} instance for the given {@code path} and
     * {@code detector}.
     * <p>
     * This constructor is a super set of the super class constructor
     * {@link File#File(URI)} with the following additional features:
     * If the given URI is opaque, it must match the pattern
     * {@code <scheme>:<uri>!/<entry>}.
     * The constructed file object then parses the URI to address an entry in
     * a federated file system (i.e. prospective archive file) with the name
     * {@code <entry>} in the prospective archive file addressed by
     * {@code <uri>} which is of the type identified by {@code <scheme>}  .
     * This is recursively applied to access entries within other prospective
     * archive files until {@code <uri>} is a hierarchical URI.
     * The scheme component of this hierarchical URI must be {@code file}.
     * <p>
     * The constructed {@code TFile} instance uses the
     * {@link #getDefaultArchiveDetector() default archive detector} to look
     * up archive file system drivers for the named URI scheme components.
     *
     * @param  path a path with an absolute
     *         {@link FsPath#toHierarchicalUri() hierarchical URI} which has a
     *         scheme component which is known by the given {@code detector}.
     * @param  detector the archive detector to look up archive file system
     *         drivers for the named URI scheme components.
     * @throws IllegalArgumentException if the
     *         {@link FsPath#toHierarchicalUri() hierarchical URI} of the given
     *         path does not conform to the syntax constraints for
     *         {@link File#File(URI)}.
     * @see    #toFsPath()
     * @since  TrueZIP 7.3.2
     */
    public TFile(FsPath path, TArchiveDetector detector) {
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
                this.enclArchive = new TFile(mpp.getMountPoint(), detector);
                this.enclEntryName = mpp.getEntryName();
            } else {
                this.enclArchive = null;
                this.enclEntryName = null;
            }
            this.innerArchive = this;
            this.controller = getController(mp);
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

        final FsPath mpp = mountPoint.getPath();

        if (null == mpp) {
            assert !mountPoint.toUri().isOpaque();
            this.enclArchive = null;
            this.enclEntryName = null;
            this.innerArchive = null;
        } else {
            assert mountPoint.toUri().isOpaque();
            if (mpp.toUri().isOpaque()) {
                this.enclArchive
                        = new TFile(mpp.getMountPoint(), detector);
                this.enclEntryName = mpp.getEntryName();
            } else {
                this.enclArchive = null;
                this.enclEntryName = null;
            }
            this.innerArchive = this;
            this.controller = getController(mountPoint);
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
            final int iapl = innerArchive.getPath().length();
            if (path.length() == iapl) {
                this.detector = innerArchive.detector;
                this.enclArchive = innerArchive.enclArchive;
                this.enclEntryName = innerArchive.enclEntryName;
                this.innerArchive = this;
                this.controller = innerArchive.controller;
            } else {
                this.detector = detector;
                this.innerArchive = this.enclArchive = innerArchive;
                try {
                    this.enclEntryName = new FsEntryName(
                            new UriBuilder()
                                .path(
                                    path.substring(iapl + 1) // cut off leading separatorChar
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
                TConfig.get().getArchiveDetector());
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
     * @throws AssertionError If assertions are enabled and any invariant is
     *         violated.
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
        TConfig.get().getManager().sync(options);
    }

    /**
     * Commits all unsynchronized changes to the contents of the federated file
     * system (i.e. prospective archive files) identified by {@code archive}
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
     * @param  archive a top level federated file system, i.e. a prospective
     *         archive file.
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
                TConfig.get().getManager(),
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
     * {@link #sync(BitField) sync(FsSyncOptions.UMOUNT)}.
     *
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
                .set(FsSyncOption.FORCE_CLOSE_INPUT, forceCloseInputAndOutput)
                .set(FsSyncOption.FORCE_CLOSE_OUTPUT, forceCloseInputAndOutput))
     * }.
     *
     * @param  forceCloseInputAndOutput see
     *         {@link FsSyncOption#FORCE_CLOSE_INPUT}
     *         and {@link FsSyncOption#FORCE_CLOSE_OUTPUT}.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         occur.
     *         This implies that the respective parent file system has been
     *         updated with constraints, such as a failure to set the last
     *         modification time of the entry for the federated file system
     *         (i.e. prospective archive file) in its parent file system.
     * @throws FsSyncException if any error conditions occur.
     *         This implies loss of data!
     * @see    #sync(BitField)
     */
    public static void umount(boolean forceCloseInputAndOutput)
    throws FsSyncException {
        sync(   BitField.of(CLEAR_CACHE)
                    .set(FORCE_CLOSE_INPUT, forceCloseInputAndOutput)
                    .set(FORCE_CLOSE_OUTPUT, forceCloseInputAndOutput));
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
                .set(FsSyncOption.WAIT_CLOSE_INPUT, waitCloseInput)
                .set(FsSyncOption.FORCE_CLOSE_INPUT, forceCloseInput)
                .set(FsSyncOption.WAIT_CLOSE_OUTPUT, waitCloseOutput)
                .set(FsSyncOption.FORCE_CLOSE_OUTPUT, forceCloseOutput))
     * }.
     *
     * @param  waitCloseInput see {@link FsSyncOption#WAIT_CLOSE_INPUT}.
     * @param  forceCloseInput see {@link FsSyncOption#FORCE_CLOSE_INPUT}.
     * @param  waitCloseOutput see {@link FsSyncOption#WAIT_CLOSE_OUTPUT}.
     * @param  forceCloseOutput see {@link FsSyncOption#FORCE_CLOSE_OUTPUT}.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         occur.
     *         This implies that the respective parent file system has been
     *         updated with constraints, such as a failure to set the last
     *         modification time of the entry for the federated file system
     *         (i.e. prospective archive file) in its parent file system.
     * @throws FsSyncException if any error conditions occur.
     *         This implies loss of data!
     * @see    #sync(BitField)
     */
    public static void umount(
            boolean waitCloseInput, boolean forceCloseInput,
            boolean waitCloseOutput, boolean forceCloseOutput)
    throws FsSyncException {
        sync(   BitField.of(CLEAR_CACHE)
                    .set(WAIT_CLOSE_INPUT, waitCloseInput)
                    .set(FORCE_CLOSE_INPUT, forceCloseInput)
                    .set(WAIT_CLOSE_OUTPUT, waitCloseOutput)
                    .set(FORCE_CLOSE_OUTPUT, forceCloseOutput));
    }

    /**
     * Commits all unsynchronized changes to the contents of the federated file
     * system (i.e. prospective archive files) identified by {@code archive}
     * and all its member federated file systems to their respective parent
     * system, releases the associated resources (i.e. target archive files)
     * for access by third parties (e.g. other processes), cleans up any
     * temporary allocated resources (e.g. temporary files) and purges any
     * cached data.
     * Note that temporary files may get used even if the archive files where
     * accessed read-only.
     * <p>
     * This method is equivalent to calling
     * {@link #sync(BitField)
        sync(archive, FsSyncOptions.UMOUNT)
     * }.
     *
     * @param  archive a top level federated file system, i.e. a prospective
     *         archive file.
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
    public static void umount(TFile archive)
    throws FsSyncException {
        sync(archive, UMOUNT);
    }

    /**
     * Commits all unsynchronized changes to the contents of the federated file
     * system (i.e. prospective archive files) identified by {@code archive}
     * and all its member federated file systems to their respective parent
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
                .set(FsSyncOption.FORCE_CLOSE_INPUT, forceCloseInputAndOutput)
                .set(FsSyncOption.FORCE_CLOSE_OUTPUT, forceCloseInputAndOutput))
     * }.
     *
     * @param  archive a top level federated file system, i.e. a prospective
     *         archive file.
     * @param  forceCloseInputAndOutput see
     *         {@link FsSyncOption#FORCE_CLOSE_INPUT} and
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
     * @see    #sync(TFile, BitField)
     */
    public static void umount(TFile archive, boolean forceCloseInputAndOutput)
    throws FsSyncException {
        sync(   archive,
                BitField.of(CLEAR_CACHE)
                    .set(FORCE_CLOSE_INPUT, forceCloseInputAndOutput)
                    .set(FORCE_CLOSE_OUTPUT, forceCloseInputAndOutput));
    }

    /**
     * Commits all unsynchronized changes to the contents of the federated file
     * system (i.e. prospective archive files) identified by {@code archive}
     * and all its member federated file systems to their respective parent
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
                .set(FsSyncOption.WAIT_CLOSE_INPUT, waitCloseInput)
                .set(FsSyncOption.FORCE_CLOSE_INPUT, forceCloseInput)
                .set(FsSyncOption.WAIT_CLOSE_OUTPUT, waitCloseOutput)
                .set(FsSyncOption.FORCE_CLOSE_OUTPUT, forceCloseOutput))
     * }.
     *
     * @param  archive a top level federated file system, i.e. a prospective
     *         archive file.
     * @param  waitCloseInput see {@link FsSyncOption#WAIT_CLOSE_INPUT}.
     * @param  forceCloseInput see {@link FsSyncOption#FORCE_CLOSE_INPUT}.
     * @param  waitCloseOutput see {@link FsSyncOption#WAIT_CLOSE_OUTPUT}.
     * @param  forceCloseOutput see {@link FsSyncOption#FORCE_CLOSE_OUTPUT}.
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
            boolean waitCloseInput, boolean forceCloseInput,
            boolean waitCloseOutput, boolean forceCloseOutput)
    throws FsSyncException {
        sync(   archive,
                BitField.of(CLEAR_CACHE)
                    .set(WAIT_CLOSE_INPUT, waitCloseInput)
                    .set(FORCE_CLOSE_INPUT, forceCloseInput)
                    .set(WAIT_CLOSE_OUTPUT, waitCloseOutput)
                    .set(FORCE_CLOSE_OUTPUT, forceCloseOutput));
    }

    /**
     * Equivalent to
     * {@link TConfig#isLenient TConfig.get().isLenient()}.
     * 
     * @see #setLenient(boolean)
     */
    public static boolean isLenient() {
        return TConfig.get().isLenient();
    }

    /**
     * Equivalent to
     * {@link TConfig#setLenient TConfig.get().setLenient(lenient)}.
     * 
     * @see #isLenient()
     */
    public static void setLenient(boolean lenient) {
        TConfig.get().setLenient(lenient);
    }

    /**
     * Equivalent to
     * {@link TConfig#getArchiveDetector TConfig.get().getArchiveDetector()}.
     * 
     * @see #setDefaultArchiveDetector(TArchiveDetector)
     */
    public static TArchiveDetector getDefaultArchiveDetector() {
        return TConfig.get().getArchiveDetector();
    }

    /**
     * Equivalent to
     * {@link TConfig#setArchiveDetector TConfig.get().setArchiveDetector(detector)}.
     * 
     * @see #getDefaultArchiveDetector()
     */
    public static void setDefaultArchiveDetector(TArchiveDetector detector) {
        TConfig.get().setArchiveDetector(detector);
    }

    TFile getNonArchiveFile() {
        return new TFile(getParentFile(), getName(), TArchiveDetector.NULL);
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
     * Removes any redundant {@code "."} and {@code ".."} directories from the
     * path name.
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
     * Returns {@code true} if and only if this {@code TFile} addresses an
     * archive file.
     * Whether or not this is true solely depends on the
     * {@link TArchiveDetector} which was used to construct this {@code TFile}
     * - no file system tests are performed by this method!
     *
     * @return {@code true} if and only if this {@code TFile} addresses an
     *         archive file.
     * @see    #isEntry
     * @see    #isDirectory
     * @see    <a href="#falsePositives">Detecting Archive Paths and False Positives</a>
     */
    public boolean isArchive() {
        return this == innerArchive;
    }

    /**
     * Returns {@code true} if and only if this {@code TPath} addresses an
     * entry located within an archive file.
     * Whether or not this is true solely depends on the
     * {@link TArchiveDetector} which was used to construct this {@code TPath}
     * - no file system tests are performed by this method!
     *
     * @return {@code true} if and only if this {@code TPath} addresses an
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

    @Nullable FsEntryName getInnerFsEntryName() {
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

    @Nullable FsEntryName getEnclFsEntryName() {
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
        // FIXME: http://java.net/jira/browse/TRUEZIP-154
        if (null == scheme)
            throw new ServiceConfigurationError(
                    "Unknown file system scheme for path \""
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
        return controller = getController(mountPoint);
    }

    private FsController<?> getController(FsMountPoint mountPoint) {
        return TConfig.get().getManager().getController(mountPoint, detector);
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
     * as a plain file in the platform file system.
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

    @Deprecated
    @Override
    @SuppressWarnings("deprecation")
    public URL toURL() throws MalformedURLException {
        return null != innerArchive ? toURI().toURL() : delegate.toURL();
    }

    /**
     * In case no prospective archive file has been detected in the path name
     * at construction time, this method behaves like its super class
     * implementation.
     * <p>
     * Otherwise, an opaque URI of the form {@code <scheme>:<uri>!/<entry>} is
     * returned, where {@code <scheme>} is the URI scheme component identifying
     * a file system driver, {@code <uri>} is the URI returned by this method
     * for the innermost archive file and {@code <entry>} is the relative path
     * name of the the entry addressed by this file object in the innermost
     * archive file.
     * If this file object addresses an archive file, then {@code <uri>} is the
     * URI which would have been returned by this method if the file name of
     * the archive file had not been detected as a prospective archive file
     * and {@code entry} is an empty string.
     * 
     * <a name="exampleUris"/><h3>Example URIs</h3>
     * <p>
     * <ul>
     * <li>{@code file:/foo} addresses a regular file</li>
     * <li>{@code war:file:/foo.war!/} addresses the root entry of the WAR file
     *     addressed by {@code file:/foo.war}.</li>
     * <li>{@code war:file:/foo.war!/META-INF/MANIFEST.MF} addresses the entry
     *     {@code META-INF/MANIFEST.MF} in the WAR file addressed by
     *     {@code file:/foo.war}.</li>
     * <li>{@code jar:war:file:/foo.war!/WEB-INF/lib/bar.jar!/META-INF/MANIFEST.MF}
     *     addresses the entry {@code META-INF/MANIFEST.MF} in the JAR file
     *     addressed by {@code war:file:/foo.war!/WEB-INF/lib/bar.jar}, which
     *     recursively addresses the entry {@code WEB-INF/lib/bar.jar} in the
     *     WAR file addressed by {@code file:/foo.war}.</li>
     * </ul>
     * 
     * @return A URI for this file object.
     * @see    #TFile(URI)
     * @see    #toFsPath()
     */
    @Override
    public URI toURI() {
        try {
            if (this == innerArchive) {
                final FsScheme scheme = getScheme();
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

    /**
     * Returns a file system path which is consistent with {@link #toURI()}.
     * 
     * @return A file system path which is consistent with {@link #toURI()}.
     * @see    #TFile(FsPath)
     * @see    #toURI()
     */
    public FsPath toFsPath() {
        try {
            if (this == innerArchive) {
                final FsScheme scheme = getScheme();
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

    private @Nullable FsScheme getScheme() {
        if (this != innerArchive)
            return null;
        if (null != controller)
            return controller.getModel().getMountPoint().getScheme();
        return detector.getScheme(delegate.getPath());
    }

    /**
     * The reasons for always throwing an {@link UnsupportedOperationException}
     * are:
     * <p>
     * <ol>
     * <li>Circular dependency issues:
     *     This method introduces a circular dependency between the
     *     packages {@code java.io} and {@code java.nio.file}.
     *     Circular dependencies are the root of all evil in API design:
     *     They limit reusability and extensibility because you cannot reuse
     *     or exchange a package individually - it's either both or none at
     *     all.</li>
     * <li>Extensibility issues:
     *     Whatever {@link java.nio.file.Path} object would be returned here,
     *     you could not exchange its implementation.
     *     This is because the NIO.2 API lacks a feature to create a
     *     {@code Path} object by looking up an appropriate file
     *     system provider from a plain path string:
     * <ul>
     * <li>The super class implementation of this method always uses the
     *     {@link java.nio.file.FileSystems#getDefault() default file system provider}.</li>
     * <li>{@link java.nio.file.Paths#get(String, String[])} always uses the
     *     default file system provider, too.</li>
     * </ul>
     *     Using {@link URI}s is no alternative, too, because the various URI
     *     schemes provided by the method {@link #toURI()} cannot be made
     *     compatible with the singular URI scheme provided by the method
     *     {@link java.nio.file.spi.FileSystemProvider#getScheme()} of any
     *     NIO.2 {@link java.nio.file.spi.FileSystemProvider} implementation.
     * </li>
     * <li>Behavior: A typical {@code Path} implementation is <em>greedy</em>,
     *     i.e. when creating another {@code Path} object from an instance of
     *     the implementation class (e.g. by calling
     *     {@link java.nio.file.Path#resolve(String)}), then the returned
     *     object is typically another instance of this implementation class
     *     rather than some other {@code Path} implementation class which may
     *     be required to do I/O on the resulting path.
     * </ol>
     * <p>
     * As an alternative, you can always create a {@code Path}
     * object from a {@link File} object {@code file} as follows:
     * <p>
     * <ul>
     * <li>Associated with the default file system provider:
     *     {@link java.nio.file.Paths#get(String, String[]) Paths.get(file.getPath())}.</li>
     * <li>Associated with a TrueZIP file system provider:
     *     {@code new de.schlichtherle.truezip.nio.file.TPath(file)}.
     *     This requires the TrueZIP Path module to be present on the compile
     *     time class path.</li>
     * </ul>
     * 
     * @deprecated Throws {@link UnsupportedOperationException}.
     * @throws UnsupportedOperationException always.
     */
    @Deprecated
    @Override
    public java.nio.file.Path toPath() {
        throw new UnsupportedOperationException("Use a Path constructor or method instead!");
    }

    /**
     * This file system operation is <a href="package-summary.html#atomicity">virtually atomic</a>.
     *
     * @see <a href="#falsePositives">Detecting Archive Paths and False Positives</a>
     */
    @Override
    public boolean exists() {
        // DONT test existance of getEnclFsEntryName() in enclArchive because
        // it doesn't need to exist - see
        // http://java.net/jira/browse/TRUEZIP-136 .
        if (null != innerArchive) {
            try {
                FsEntry entry = innerArchive.getController()
                        .getEntry(getInnerFsEntryName());
                return null != entry;
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
                        .getEntry(getInnerFsEntryName());
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
                        .getEntry(getInnerFsEntryName());
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

    @Override
    public boolean canRead() {
        if (null != innerArchive) {
            try {
                return innerArchive.getController()
                        .isReadable(getInnerFsEntryName());
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
                        .isWritable(getInnerFsEntryName());
            } catch (IOException ex) {
                return false;
            }
        }
        return delegate.canWrite();
    }

    @Override
    public boolean canExecute() {
        if (null != innerArchive) {
            try {
                return innerArchive.getController()
                        .isExecutable(getInnerFsEntryName());
            } catch (IOException ex) {
                return false;
            }
        }
        return delegate.canExecute();
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
                innerArchive.getController().setReadOnly(getInnerFsEntryName());
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
                entry = innerArchive.getController().getEntry(getInnerFsEntryName());
            } catch (IOException ex) {
                return 0;
            }
            if (null == entry)
                return 0;
            final long size = entry.getSize(DATA);
            return UNKNOWN == size ? 0 : size;
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
                entry = innerArchive.getController().getEntry(getInnerFsEntryName());
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
                innerArchive.getController().setTime(
                        getInnerFsEntryName(),
                        BitField.of(Access.WRITE),
                        time,
                        TConfig.get().getOutputPreferences());
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
                entry = innerArchive.getController().getEntry(getInnerFsEntryName());
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
                entry = innerArchive.getController().getEntry(getInnerFsEntryName());
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
            final Collection<String> accepted
                    = new ArrayList<String>(members.size());
            for (final String member : members)
                if (filter.accept(this, member))
                    accepted.add(member);
            return accepted.toArray(new String[accepted.size()]);
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
    public @Nullable TFile[] listFiles(@CheckForNull FilenameFilter filter) {
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
                entry = innerArchive.getController().getEntry(getInnerFsEntryName());
            } catch (IOException ignored) {
                return null;
            }
            if (null == entry)
                return null;
            return filter(entry.getMembers(), filter, detector);
        }
        return filter(delegate.list(filter), (FilenameFilter) null, detector);
    }

    private @Nullable TFile[] filter(
            @CheckForNull String[] members,
            @CheckForNull FilenameFilter filter,
            TArchiveDetector detector) {
        return null == members
                ? null
                : filter(Arrays.asList(members), filter, detector);
    }

    private @Nullable TFile[] filter(
            final @CheckForNull Collection<String> members,
            final @CheckForNull FilenameFilter filter,
            final TArchiveDetector detector) {
        if (null == members)
            return null;
        if (null != filter) {
            final Collection<TFile>
                    accepted = new ArrayList<TFile>(members.size());
            for (final String member : members)
                if (filter.accept(this, member))
                    accepted.add(new TFile(this, member, detector));
            return accepted.toArray(new TFile[accepted.size()]);
        } else {
            final TFile[] accepted = new TFile[members.size()];
            int i = 0;
            for (final String member : members)
                accepted[i++] = new TFile(this, member, detector);
            return accepted;
        }
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
                entry = innerArchive.getController().getEntry(getInnerFsEntryName());
            } catch (IOException ex) {
                return null;
            }
            if (null == entry)
                return null;
            return filter(entry.getMembers(), filter, detector);
        }
        return filter(Arrays.asList(delegate.list()), filter, detector);
    }

    private @Nullable TFile[] filter(
            final @CheckForNull Collection<String> members,
            final @CheckForNull FileFilter filter,
            final TArchiveDetector detector) {
        if (null == members)
            return null;
        final Collection<TFile>
                accepted = new ArrayList<TFile>(members.size());
        for (final String member : members) {
            final TFile file = new TFile(this, member, detector);
            if (null == filter || filter.accept(file))
                accepted.add(file);
        }
        return accepted.toArray(new TFile[accepted.size()]);
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
            final FsEntryName entryName = getInnerFsEntryName();
            // This is not really atomic, but should be OK in this case.
            if (null != controller.getEntry(entryName))
                return false;
            controller.mknod(
                    entryName,
                    FILE,
                    TConfig.get().getOutputPreferences().set(EXCLUSIVE),
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
                        getInnerFsEntryName(),
                        DIRECTORY,
                        TConfig.get().getOutputPreferences(),
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
            final FsEntryName innerEntryName = getInnerFsEntryName();
            try {
                controller.mknod(
                        innerEntryName,
                        DIRECTORY,
                        TConfig.get().getOutputPreferences(),
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
     *         Note that although this just needs to be a plain {@code File},
     *         archive files and entries are only supported for instances of
     *         this class.
     * @throws IOException if any I/O error occurs.
     * @see    <a href="#bulkIOMethods">Bulk I/O Methods</a>
     */
    public static void rm(File node) throws IOException {
        if (node instanceof TFile) {
            TFile file = (TFile) node;
            if (null != file.innerArchive) {
                file.innerArchive.getController().unlink(
                        file.getInnerFsEntryName(),
                        TConfig.get().getOutputPreferences());
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
     *         Note that although this just needs to be a plain {@code File},
     *         archive files and entries are only supported for instances of
     *         this class.
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
     *         Note that although this just needs to be a plain {@code File},
     *         archive files and entries are only supported for instances of
     *         this class.
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
     *         Note that although this just needs to be a plain {@code File},
     *         archive files and entries are only supported for instances of
     *         this class.
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
     * In these cases, an attempt is made to copy all attributes of each
     * source file to the destination file, too.
     * Which attributes are actually copied is specific to the destination
     * file system driver implementation, but the minimum guarantee is to
     * copy the last modification time.
     * 
     * @param  src the source file or directory tree.
     *         Note that although this just needs to be a plain {@code File},
     *         archive files and entries are only supported for instances of
     *         this class.
     * @param  dst the destination file or directory tree.
     *         Note that although this just needs to be a plain {@code File},
     *         archive files and entries are only supported for instances of
     *         this class.
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
                dstDelegate = dst;
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
     * <p>
     * <table border=1 cellpadding=5 summary="">
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
     *   <td>Closes parameter stream(s)</td>
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
     * <p>
     * <table border=1 cellpadding=5 summary="">
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
     *   <td>Closes parameter stream(s)</td>
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
     * @param  dst the destination file.
     *         Note that although this just needs to be a plain {@code File},
     *         archive files and entries are only supported for instances of
     *         this class.
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
     * <p>
     * <table border=1 cellpadding=5 summary="">
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
     *   <td>Closes parameter stream(s)</td>
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
     * @param  src the source file.
     *         Note that although this just needs to be a plain {@code File},
     *         archive files and entries are only supported for instances of
     *         this class.
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
     *         Note that although this just needs to be a plain {@code File},
     *         archive files and entries are only supported for instances of
     *         this class.
     * @return {@code this}
     * @throws IOException if any I/O error occurs.
     */
    public TFile cp(File dst) throws IOException {
        TBIO.cp(false, this, dst);
        return this;
    }

    /**
     * Copies the file {@code src} to the file {@code dst}.
     * <p>
     * <table border=1 cellpadding=5 summary="">
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
     *   <td>Closes parameter stream(s)</td>
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
     * @param  src the source file.
     *         Note that although this just needs to be a plain {@code File},
     *         archive files and entries are only supported for instances of
     *         this class.
     * @param  dst the destination file.
     *         Note that although this just needs to be a plain {@code File},
     *         archive files and entries are only supported for instances of
     *         this class.
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
     *         Note that although this just needs to be a plain {@code File},
     *         archive files and entries are only supported for instances of
     *         this class.
     * @return {@code this}
     * @throws IOException if any I/O error occurs.
     */
    public TFile cp_p(File dst) throws IOException {
        TBIO.cp(true, this, dst);
        return this;
    }

    /**
     * Copies the file {@code src} to the file {@code dst} and attempts to
     * copy all attributes of the source file to the destination file, too.
     * Which attributes are actually copied is specific to the source and
     * destination file system driver implementations, but the minimum
     * guarantee is to copy the last modification time.
     * For example, starting with TrueZIP 7.2, the last modification, last
     * access and creation times are copied if all of the following are true:
     * <p>
     * <ol>
     * <li>Both parameters refer to the platform file system
     *     (even if any one is only a {@code java.io.File}), and
     * <li>the JVM complies to JSE&nbsp;7, and
     * <li>the TrueZIP Driver FILE module is used.
     * </ol>
     * <p>
     * <table border=1 cellpadding=5 summary="">
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
     *   <td>Closes parameter stream(s)</td>
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
     * @param  src the source file.
     *         Note that although this just needs to be a plain {@code File},
     *         archive files and entries are only supported for instances of
     *         this class.
     * @param  dst the destination file.
     *         Note that although this just needs to be a plain {@code File},
     *         archive files and entries are only supported for instances of
     *         this class.
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
     *         Note that although this just needs to be a plain {@code File},
     *         archive files and entries are only supported for instances of
     *         this class.
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
     * Equivalent to {@link #cp_r(File, File, TArchiveDetector, TArchiveDetector) cp_r(this, dst, detector, detector)}.
     * 
     * @param  src the source file or directory tree.
     *         Note that although this just needs to be a plain {@code File},
     *         archive files and entries are only supported for instances of
     *         this class.
     * @param  dst the destination file or directory tree.
     *         Note that although this just needs to be a plain {@code File},
     *         archive files and entries are only supported for instances of
     *         this class.
     * @param  detector the archive detector to use for detecting any
     *         archive files in the source and destination directory tree.
     * @throws IOException if any I/O error occurs.
     * @since  TrueZIP 7.2
     * @see    <a href="#bulkIOMethods">Bulk I/O Methods</a>
     * @see    <a href="#traversal">Traversing Directory Trees</a>
     */
    public static void cp_r(   File src, File dst,
                                TArchiveDetector detector)
    throws IOException {
        TBIO.cp_r(false, src, dst, detector, detector);
    }

    /**
     * Recursively copies the file or directory {@code src}
     * to the file or directory {@code dst}.
     * <p>
     * <table border=1 cellpadding=5 summary="">
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
     *   <td>Closes parameter stream(s)</td>
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
     * @param  src the source file or directory tree.
     *         Note that although this just needs to be a plain {@code File},
     *         archive files and entries are only supported for instances of
     *         this class.
     * @param  dst the destination file or directory tree.
     *         Note that although this just needs to be a plain {@code File},
     *         archive files and entries are only supported for instances of
     *         this class.
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
     *         Note that although this just needs to be a plain {@code File},
     *         archive files and entries are only supported for instances of
     *         this class.
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
     * Equivalent to {@link #cp_rp(File, File, TArchiveDetector, TArchiveDetector) cp_rp(this, dst, detector, detector)}.
     * 
     * @param  src the source file or directory tree.
     *         Note that although this just needs to be a plain {@code File},
     *         archive files and entries are only supported for instances of
     *         this class.
     * @param  dst the destination file or directory tree.
     *         Note that although this just needs to be a plain {@code File},
     *         archive files and entries are only supported for instances of
     *         this class.
     * @param  detector the archive detector to use for detecting any
     *         archive files in the source and destination directory tree.
     * @throws IOException if any I/O error occurs.
     * @since  TrueZIP 7.2
     * @see    <a href="#bulkIOMethods">Bulk I/O Methods</a>
     * @see    <a href="#traversal">Traversing Directory Trees</a>
     */
    public static void cp_rp(   File src, File dst,
                                TArchiveDetector detector)
    throws IOException {
        TBIO.cp_r(true, src, dst, detector, detector);
    }

    /**
     * Recursively copies the file or directory {@code src} to the file or
     * directory {@code dst} and attempts to copy all attributes of each
     * source file to the destination file, too.
     * Which attributes are actually copied is specific to the source and
     * destination file system driver implementations, but the minimum
     * guarantee is to copy the last modification time.
     * For example, starting with TrueZIP 7.2, the last modification, last
     * access and creation times are copied if all of the following are true:
     * <p>
     * <ol>
     * <li>Both parameters refer to the platform file system
     *     (even if any one is only a {@code java.io.File}), and
     * <li>the JVM complies to JSE&nbsp;7, and
     * <li>the TrueZIP Driver FILE module is used.
     * </ol>
     * <p>
     * <table border=1 cellpadding=5 summary="">
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
     *   <td>Closes parameter stream(s)</td>
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
     * @param  src the source file or directory tree.
     *         Note that although this just needs to be a plain {@code File},
     *         archive files and entries are only supported for instances of
     *         this class.
     * @param  dst the destination file or directory tree.
     *         Note that although this just needs to be a plain {@code File},
     *         archive files and entries are only supported for instances of
     *         this class.
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
     * <p>
     * <table border=1 cellpadding=5 summary="">
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
     *   <td>Closes parameter stream(s)</td>
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
        try {
            final OutputStream out = new TFileOutputStream(this, false);
            try {
                Streams.cat(in, out);
            } finally {
                out.close();
            }
        } catch (IOException ex) {
            try {
                rm();
            } catch (IOException discard) {
                throw ex;
            }
            throw ex;
        }
    }

    /**
     * Copies this file or entry in an archive file to the output stream
     * {@code out}
     * <em>without</em> closing it.
     * <p>
     * <table border=1 cellpadding=5 summary="">
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
     *   <td>Closes parameter stream(s)</td>
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
     * <p>
     * <table border=1 cellpadding=5 summary="">
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
     *   <td>Closes parameter stream(s)</td>
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

    /**
     * Compacts this archive file by removing any redundant archive entry
     * contents and meta data, including central directories.
     * If this instance does not identify an {@link #isArchive() archive file},
     * then this operation does nothing and returns immediately.
     * <p>
     * This operation is intended to compact archive files which have been
     * frequently updated with {@link FsOutputOption#GROW} or similar means.
     * If this output option preference is set and an archive file is updated
     * frequently, then over time a lot of redundant artifacts such as archive
     * entry contents and meta data, including central directories may be
     * physically present in the archive file, even if all its entries have
     * been deleted.
     * This operation could then get used to remove any redundant artifacts
     * again.
     * <p>
     * Mind that this operation has no means to detect if there is actually any
     * redundant data present in this archive file.
     * Any invocation will perform exactly the same steps, so if this archive
     * file is already compact, then this will just waste time and temporary
     * space in the platform file system.
     * <p>
     * Note that this operation is not thread-safe and hence not atomic, so you
     * should not concurrently access this archive file or any of its entries!
     * <p>
     * This operation performs in the order of <i>O(s)</i>, where <i>s</i> is
     * the total size of the archive file either before (worst case) or after
     * (best case) compacting it.
     * If this archive file has already been mounted, then <i>s</i> is the
     * total size of the archive file after compacting it (best case).
     * Otherwise, the definition of <i>s</i> is specific to the archive file
     * system driver.
     * Usually, if the archive file contains a central directory, you could
     * expect the best case, otherwise the worst case, but this information
     * is given without warranty.
     * <p>
     * If this archive file has been successfully compacted, then it's left
     * unmounted, so any subsequent operation will mount it again, which
     * requires additional time.
     * 
     * @return this
     * @throws IOException On any I/O error.
     * @see    FsOutputOption#GROW
     * @since  TrueZIP 7.3
     */
    public TFile compact() throws IOException {
        if (isArchive())
            compact(this);
        return this;
    }

    private static void compact(TFile grown) throws IOException {
        assert grown.isArchive();
        grown = grown.getNormalizedFile();
        assert grown.isArchive();

        final File dir = getParent(grown);
        final String suffix = getSuffix(grown);
        final TConfig config = TConfig.push();
        try {
            // Switch off FsOutputOption.GROW.
            config.setOutputPreferences(
                    config.getOutputPreferences().clear(GROW));

            // Create temp file.
            final TFile compact = new TFile(createTempFile("tzp", suffix, dir));
            compact.rm();
            try {
                // Make a structural copy of the grown archive file, thereby
                // compacting it.
                grown.cp_rp(compact);

                // Unmount both archive files so we can delete and move them
                // safely and fast like regular files.
                umount(grown);
                umount(compact);

                // Move the compacted archive file over to the grown archive
                // file like a regular file.
                if (!move(compact.getNonArchiveFile(), grown.getNonArchiveFile()))
                    throw new IOException(compact + " (cannot move to " + grown + ")");
            } catch (IOException ex) {
                compact.rm();
                throw ex;
            }
        } finally {
            config.close();
        }
    }

    private static File getParent(final File file) {
        final File parent = file.getParentFile();
        return null != parent ? parent : CURRENT_DIRECTORY;
    }

    private static @Nullable String getSuffix(final TFile file) {
        final FsScheme scheme = file.getScheme();
        return null != scheme ? "." + scheme : null;
    }

    private static boolean move(File src, File dst) {
        return src.exists()
                && (!dst.exists() || dst.delete())
                && src.renameTo(dst);
    }
}
