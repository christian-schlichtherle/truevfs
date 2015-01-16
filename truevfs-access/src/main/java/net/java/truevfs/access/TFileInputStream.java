/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.*;
import javax.annotation.concurrent.Immutable;
import net.java.truecommons.io.DecoratingInputStream;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.FsAccessOption;
import net.java.truevfs.kernel.spec.FsAccessOptions;

/**
 * A replacement for the class {@link FileInputStream} for reading plain old
 * files or entries in an archive file.
 * Mind that applications cannot read archive files directly - just their
 * entries!
 * <p>
 * To prevent exceptions to be thrown subsequently, applications should
 * <em>always</em> close their streams using the following idiom:
 * <pre>{@code
 * TFileInputStream in = new TFileInputStream(file);
 * try {
 *     // Do I/O here...
 * } finally {
 *     in.close(); // ALWAYS close the stream!
 * }
 * }</pre>
 * <p>
 * Note that the {@link #close()} method may throw an {@link IOException}, too.
 * Applications need to deal with this appropriately, for example by enclosing
 * the entire block with another {@code try-catch}-block:
 * <pre>{@code
 * try {
 *     TFileInputStream in = new TFileInputStream(file);
 *     try {
 *         // Do I/O here...
 *     } finally {
 *         in.close(); // ALWAYS close the stream!
 *     }
 * } catch (IOException ex) {
 *     ex.printStackTrace();
 * }
 * }</pre>
 * <p>
 * Applications cannot read from an entry in an archive file if an implicit
 * {@linkplain TVFS#umount() unmount} is required but cannot get performed
 * because another {@link TFileInputStream} or {@link TFileOutputStream} object
 * hasn't been closed or garbage collected yet.
 * A {@link FileNotFoundException} is thrown by the constructors of this class
 * in this case.
 * <p>
 * If you would like to use this class in order to cp files,
 * please consider using one of the
 * <a href="TFile.java#bulkIOMethods">cp methods</a> of the class {@link TFile}
 * instead.
 * These methods provide ease of use, enhanced features, superior performance
 * and require less space in the temp file folder.
 *
 * @see    TFile#cat(InputStream, OutputStream)
 * @see    TFileOutputStream
 * @author Christian Schlichtherle
 */
@Immutable
public final class TFileInputStream extends DecoratingInputStream {

    /**
     * Constructs a new input stream for reading plain old files or entries
     * in an archive file.
     *
     * @param  path the path of the file or archive entry.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    public TFileInputStream(String path) throws IOException {
        this(new TFile(path));
    }

    /**
     * Constructs a new input stream for reading plain old files or entries
     * in an archive file.
     *
     * @param  path the path of the file or archive entry.
     * @param  options additional options for accessing the file or archive
     *         entry.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    public TFileInputStream(String path, FsAccessOption... options)
    throws IOException {
        this(new TFile(path), options);
    }

    /**
     * Constructs a new input stream for reading plain old files or entries
     * in an archive file.
     *
     * @param  file the file or archive entry.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    public TFileInputStream(File file) throws IOException {
        this(file, FsAccessOptions.NONE);
    }

    /**
     * Constructs a new input stream for reading plain old files or entries
     * in an archive file.
     *
     * @param  file the file or archive entry.
     * @param  options additional options for accessing the file or archive
     *         entry.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    public TFileInputStream(File file, FsAccessOption... options)
    throws IOException {
        this(file, FsAccessOptions.of(options));
    }

    /**
     * Constructs a new input stream for reading plain old files or entries
     * in an archive file.
     *
     * @param  file the file or archive entry.
     * @param  options additional options for accessing the file or archive
     *         entry.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    public TFileInputStream(File file, BitField<FsAccessOption> options)
    throws IOException {
        super(TBIO.input(
                TConfig.current().getAccessPreferences().or(options),
                file).stream(null));
    }
}
