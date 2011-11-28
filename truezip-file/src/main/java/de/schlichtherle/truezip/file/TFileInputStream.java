/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.file;

import de.schlichtherle.truezip.io.FileBusyException;
import de.schlichtherle.truezip.io.DecoratingInputStream;
import de.schlichtherle.truezip.socket.InputSocket;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.jcip.annotations.Immutable;

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
 * {@link TFile#umount() unmount} is required but cannot get performed because
 * another {@link TFileInputStream} or {@link TFileOutputStream} object hasn't
 * been closed or garbage collected yet.
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
 * @see     TFile#cat(InputStream, OutputStream)
 * @see     TFileOutputStream
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
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
     * @throws FileBusyException If the path denotes an archive entry and the
     *         archive driver does not support to create an additional input
     *         stream for the archive file.
     * @throws FileNotFoundException On any other I/O related issue.
     */
    public TFileInputStream(String path)
    throws FileNotFoundException {
        super(newInputStream(new TFile(path)));
    }

    /**
     * Constructs a new input stream for reading plain old files or entries
     * in an archive file.
     *
     * @param  file the plain old file or entry in an archive file to read.
     * @throws FileBusyException If the path denotes an archive entry and the
     *         archive driver does not support to create an additional input
     *         stream for the archive file.
     * @throws FileNotFoundException On any other I/O related issue.
     */
    public TFileInputStream(File file)
    throws FileNotFoundException {
        super(newInputStream(file));
    }

    private static InputStream newInputStream(final File src)
    throws FileNotFoundException {
        final InputSocket<?> input = TBIO.getInputSocket(src,
                TConfig.get().getInputPreferences());
        try {
            return input.newInputStream();
        } catch (FileNotFoundException ex) {
            throw ex;
        } catch (IOException ex) {
            throw (FileNotFoundException) new FileNotFoundException(
                    src.toString()).initCause(ex);
        }
    }
}
