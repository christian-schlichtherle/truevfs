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

package de.schlichtherle.truezip.io.file;

import de.schlichtherle.truezip.io.FileBusyException;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.io.filesystem.SyncException;
import de.schlichtherle.truezip.io.socket.OutputOption;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import static de.schlichtherle.truezip.io.socket.OutputOption.APPEND;
import static de.schlichtherle.truezip.io.socket.OutputOption.CREATE_PARENTS;

/**
 * A drop-in replacement for {@link java.io.FileOutputStream} which
 * provides transparent write access to archive entries as if they were
 * (virtual) files.
 * All file system operations in this class are
 * <a href="package-summary.html#atomicity">virtually atomic</a>.
 * <p>
 * To prevent exceptions to be thrown subsequently, client applications
 * should always close their streams using the following idiom:
 * <pre>{@code 
 * FileOutputStream fos = new FileOutputStream(file);
 * try {
 *     // access fos here
 * } finally {
 *     fos.close();
 * }
 * }</pre>
 * <p>
 * Note that for various (mostly archive driver specific) reasons, the
 * {@code close()} method may throw an {@code IOException}, too.
 * Client applications need to deal with this appropriately, for example
 * by enclosing the entire block with another {@code try-catch}-block.
 * <p>
 * Client applications cannot write to an entry in an archive file if an
 * automatic update is required but cannot get performed because other
 * {@code FileInputStream} or {@code FileOutputStream} instances
 * haven't been closed or garbage collected yet.
 * A {@link FileBusyException} is thrown by the constructors of this class
 * in this case.
 * <p>
 * Whether or not a client application can write to more than one entry
 * in the same archive archive file concurrently is an implementation
 * detail of the respective archive driver.
 * As of version 6.5, all archive drivers provided by TrueZIP don't restrict
 * this.
 * However, custom archive drivers provided by third parties may do so.
 * <p>
 * If a client application tries to exceed the number of entry streams
 * supported to operate on the same archive file concurrently, a
 * {@link FileBusyException} is thrown by the constructors of this class.
 * <p>
 * If you would like to use this class in order to copy files,
 * please consider using the {@code *copy*} methods in the {@link File}
 * class instead.
 * These methods provide ease of use, enhanced features, superior performance
 * and require less space in the temp file folder.
 *
 * @see     <a href="package-summary.html#streams">Using Archive Entry Streams</a>
 * @see     FileInputStream
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class FileOutputStream extends DecoratingOutputStream {

    /**
     * Creates a new {@code FileOutputStream} for accessing regular files or
     * archive entries.
     *
     * @throws FileBusyException If the path denotes an archive entry and the
     *         archive driver does not support to create an additional output
     *         stream for the archive file.
     * @throws FileNotFoundException On any other I/O related issue.
     */
    public FileOutputStream(String name)
    throws FileNotFoundException {
        super(newOutputStream(
                File.getDefaultArchiveDetector().newFile(name), false));
    }

    /**
     * Creates a new {@code FileOutputStream} for accessing regular files or
     * archive entries.
     *
     * @throws FileBusyException If the path denotes an archive entry and the
     *         archive driver does not support to create an additional output
     *         stream for the archive file.
     * @throws FileNotFoundException On any other I/O related issue.
     */
    public FileOutputStream(String name, boolean append)
    throws FileNotFoundException {
        super(newOutputStream(
                File.getDefaultArchiveDetector().newFile(name), append));
    }

    /**
     * Creates a new {@code FileOutputStream} for accessing regular files or
     * archive entries.
     *
     * @throws FileBusyException If the path denotes an archive entry and the
     *         archive driver does not support to create an additional output
     *         stream for the archive file.
     * @throws FileNotFoundException On any other I/O related issue.
     */
    public FileOutputStream(java.io.File file)
    throws FileNotFoundException {
        super(newOutputStream(file, false));
    }

    /**
     * Creates a new {@code FileOutputStream} for accessing regular files or
     * archive entries.
     *
     * @throws FileBusyException If the path denotes an archive entry and the
     *         archive driver does not support to create an additional output
     *         stream for the archive file.
     * @throws FileNotFoundException On any other I/O related issue.
     */
    public FileOutputStream(java.io.File file, boolean append)
    throws FileNotFoundException {
        super(newOutputStream(file, append));
    }

    public FileOutputStream(FileDescriptor fd) {
        super(new java.io.FileOutputStream(fd));
    }

    private static OutputStream newOutputStream(    final java.io.File dst,
                                                    final boolean append)
    throws FileNotFoundException {
        final OutputSocket<?> output = Files.getOutputSocket(dst,
                BitField.noneOf(OutputOption.class)
                    .set(APPEND, append)
                    .set(CREATE_PARENTS, File.isLenient()),
                null);
        try {
            return output.newOutputStream();
        } catch (FileNotFoundException ex) {
            throw ex;
        } catch (SyncException ex) {
            throw ex.getCause() instanceof FileBusyException
                    ? (FileBusyException) ex.getCause()
                    : (FileNotFoundException) new FileNotFoundException(
                        ex.toString()).initCause(ex);
        } catch (IOException ex) {
            throw (FileNotFoundException) new FileNotFoundException(
                    ex.toString()).initCause(ex);
        }
    }
}
