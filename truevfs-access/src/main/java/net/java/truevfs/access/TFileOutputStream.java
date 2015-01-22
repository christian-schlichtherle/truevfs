/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.*;
import javax.annotation.concurrent.Immutable;
import net.java.truecommons.io.DecoratingOutputStream;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.FsAccessOption;
import net.java.truevfs.kernel.spec.FsAccessOptions;

/**
 * A replacement for the class {@link FileOutputStream} for writing plain old
 * files or entries in an archive file.
 * Mind that applications cannot write archive files directly - just their
 * entries!
 * <p>
 * To prevent exceptions to be thrown subsequently, applications should
 * <em>always</em> close their streams using the following idiom:
 * <pre>{@code 
 * TFileOutputStream out = new TFileOutputStream(file);
 * try {
 *     // Do I/O here...
 * } finally {
 *     out.close(); // ALWAYS close the stream!
 * }
 * }</pre>
 * <p>
 * Note that the {@link #close()} method may throw an {@link IOException}, too.
 * Applications need to deal with this appropriately, for example by enclosing
 * the entire block with another {@code try-catch}-block:
 * <pre>{@code
 * try {
 *     TFileOutputStream out = new TFileOutputStream(file);
 *     try {
 *         // Do I/O here...
 *     } finally {
 *         out.close(); // ALWAYS close the stream!
 *     }
 * } catch (IOException ex) {
 *     ex.printStackTrace();
 * }
 * }</pre>
 * <p>
 * Applications cannot write to an entry in an archive file if an implicit
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
 * @see    TConfig#setLenient
 * @see    TFileInputStream
 * @author Christian Schlichtherle
 */
@Immutable
public final class TFileOutputStream extends DecoratingOutputStream {

    /**
     * Constructs a new output stream for writing plain old files or entries
     * in an archive file.
     *
     * @param  path the path of the file or archive entry.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    public TFileOutputStream(String path) throws IOException {
        this(path, false);
    }

    /**
     * Constructs a new output stream for writing plain old files or entries
     * in an archive file.
     *
     * @param  path the path of the file or archive entry.
     * @param  append if the data shall get appended to the file or archive
     *         entry rather than replacing it.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    public TFileOutputStream(String path, boolean append) throws IOException {
        this(new TFile(path), append);
    }

    /**
     * Constructs a new output stream for writing plain old files or entries
     * in an archive file.
     *
     * @param  path the path of the file or archive entry.
     * @param  options additional options for accessing the file or archive
     *         entry.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    public TFileOutputStream(String path, FsAccessOption... options)
    throws IOException {
        this(new TFile(path), options);
    }

    /**
     * Constructs a new output stream for writing plain old files or entries
     * in an archive file.
     *
     * @param  file the file or archive entry.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    public TFileOutputStream(File file) throws IOException {
        this(file, false);
    }

    /**
     * Constructs a new output stream for writing plain old files or entries
     * in an archive file.
     *
     * @param  file the file or archive entry.
     * @param  append if the data shall get appended to the file or archive
     *         entry rather than replacing it.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    public TFileOutputStream(File file, boolean append) throws IOException {
        this(file, FsAccessOptions.NONE.set(FsAccessOption.APPEND, append));
    }

    /**
     * Constructs a new output stream for writing plain old files or entries
     * in an archive file.
     *
     * @param  file the file or archive entry.
     * @param  options additional options for accessing the file or archive
     *         entry.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    public TFileOutputStream(File file, FsAccessOption... options)
    throws IOException {
        this(file, FsAccessOptions.of(options));
    }

    /**
     * Constructs a new output stream for writing plain old files or entries
     * in an archive file.
     *
     * @param  file the file or archive entry.
     * @param  options additional options for accessing the file or archive
     *         entry.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    public TFileOutputStream(File file, BitField<FsAccessOption> options)
    throws IOException {
        super(TBIO.output(
                TConfig.current().getAccessPreferences().or(options),
                file,
                null).stream(null));
    }
}
