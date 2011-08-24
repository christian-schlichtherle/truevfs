/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a cp of the License at
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

import java.io.InputStream;
import net.jcip.annotations.Immutable;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import static de.schlichtherle.truezip.fs.FsOutputOption.APPEND;
import de.schlichtherle.truezip.io.FileBusyException;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import static de.schlichtherle.truezip.fs.FsOutputOptions.*;
import de.schlichtherle.truezip.socket.OutputSocket;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A replacement for the class {@link FileOutputStream} for writing plain old
 * files or entries in an archive file.
 * Note that applications cannot write archive <em>files</em> directly using
 * this class - just their entries.
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
 * @see     TConfig#setLenient
 * @see     TFileInputStream
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
@Immutable
public final class TFileOutputStream extends DecoratingOutputStream {

    /**
     * Constructs a new output stream for writing plain old files or entries
     * in an archive file.
     * This constructor calls {@link TFile#TFile(String) new TFile(path)} for
     * the given path.
     *
     * @param  path the path of the plain old file or entry in an archive file
     *         to write.
     * @throws FileBusyException If the path denotes an archive entry and the
     *         archive driver does not support to create an additional output
     *         stream for the archive file.
     * @throws FileNotFoundException On any other I/O related issue.
     */
    public TFileOutputStream(String path)
    throws FileNotFoundException {
        super(newOutputStream(new TFile(path), false));
    }

    /**
     * Constructs a new output stream for writing plain old files or entries
     * in an archive file.
     * This constructor calls {@link TFile#TFile(String) new TFile(path)} for
     * the given path.
     *
     * @param  path the path of the plain old file or entry in an archive file
     *         to write.
     * @param  append if the data shall get appended to the file rather than
     *         replacing it.
     * @throws FileBusyException If the path denotes an archive entry and the
     *         archive driver does not support to create an additional output
     *         stream for the archive file.
     * @throws FileNotFoundException On any other I/O related issue.
     */
    public TFileOutputStream(String path, boolean append)
    throws FileNotFoundException {
        super(newOutputStream(new TFile(path), append));
    }

    /**
     * Constructs a new output stream for writing plain old files or entries
     * in an archive file.
     *
     * @param  file the plain old file or entry in an archive file to write.
     * @throws FileBusyException If the path denotes an archive entry and the
     *         archive driver does not support to create an additional output
     *         stream for the archive file.
     * @throws FileNotFoundException On any other I/O related issue.
     */
    public TFileOutputStream(File file)
    throws FileNotFoundException {
        super(newOutputStream(file, false));
    }

    /**
     * Constructs a new output stream for writing plain old files or entries
     * in an archive file.
     *
     * @param  file the plain old file or entry in an archive file to write.
     * @param  append if the data shall get appended to the file rather than
     *         replacing it.
     * @throws FileBusyException If the path denotes an archive entry and the
     *         archive driver does not support to create an additional output
     *         stream for the archive file.
     * @throws FileNotFoundException On any other I/O related issue.
     */
    public TFileOutputStream(File file, boolean append)
    throws FileNotFoundException {
        super(newOutputStream(file, append));
    }

    private static OutputStream newOutputStream(final File dst,
                                                final boolean append)
    throws FileNotFoundException {
        final OutputSocket<?> output = TBIO.getOutputSocket(dst,
                TConfig.get().getOutputPreferences().set(APPEND, append),
                null);
        try {
            return output.newOutputStream();
        } catch (FileNotFoundException ex) {
            throw ex;
        } catch (IOException ex) {
            throw (FileNotFoundException) new FileNotFoundException(
                    dst.toString()).initCause(ex);
        }
    }
}
