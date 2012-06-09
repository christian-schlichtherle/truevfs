/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.access;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.*;
import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.io.DecoratingInputStream;

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
     * This constructor calls {@link TFile#TFile(String) new TFile(path)} for
     * the given path.
     *
     * @param  path the path of the plain old file or entry in an archive file
     *         to read.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    public TFileInputStream(String path) throws IOException {
        super(newInputStream(new TFile(path)));
    }

    /**
     * Constructs a new input stream for reading plain old files or entries
     * in an archive file.
     *
     * @param  file the plain old file or entry in an archive file to read.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    public TFileInputStream(File file) throws IOException {
        super(newInputStream(file));
    }

    @CreatesObligation
    private static InputStream newInputStream(final File src)
    throws IOException {
        return TBIO.input(TConfig.get().getAccessPreferences(), src).stream(null);
    }
}
