/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
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

import de.schlichtherle.truezip.io.FileBusyException;
import de.schlichtherle.truezip.io.DecoratingInputStream;
import de.schlichtherle.truezip.fs.FsSyncException;
import de.schlichtherle.truezip.fs.FsInputOption;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import net.jcip.annotations.Immutable;

/**
 * A replacement for {@link FileInputStream} which provides transparent read
 * access to archive entries as if they were (virtual) files.
 * All file system operations in this class are
 * <a href="package-summary.html#atomicity">virtually atomic</a>.
 * <p>
 * To prevent exceptions to be thrown subsequently, client applications
 * should always close their streams using the following idiom:
 * <pre>{@code 
 * TFileInputStream fis = new TFileInputStream(file);
 * try {
 *     // access fis here
 * } finally {
 *     fis.close();
 * }
 * }</pre>
 * <p>
 * Note that for various (mostly archive driver specific) reasons, the
 * {@code close()} method may throw an {@code IOException}, too.
 * Client applications need to deal with this appropriately, for example
 * by enclosing the entire block with another {@code try-catch}-block
 * <p>
 * Client applications cannot read from an entry in an archive file if an
 * automatic update is required but cannot get performed because other
 * {@code TFileInputStream} or {@code FileOutputStream} instances
 * haven't been closed or garbage collected yet.
 * A {@link FileBusyException} is thrown by the constructors of this class
 * in this case.
 * <p>
 * Whether or not a client application can read from more than one entry
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
 * please consider using the {@code *copy*} methods in the {@link TFile}
 * class instead.
 * These methods provide ease of use, enhanced features, superior performance
 * and require less space in the temp file folder.
 *
 * @see     <a href="package-summary.html#streams">Using Archive Entry Streams</a>
 * @see     TFile#cat(InputStream, OutputStream)
 * @see     TFileOutputStream
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
@Immutable
public final class TFileInputStream extends DecoratingInputStream {

    /**
     * Creates a new {@code TFileInputStream} for accessing regular files or
     * archive entries.
     *
     * @throws FileBusyException If the path denotes an archive entry and the
     *         archive driver does not support to create an additional input
     *         stream for the archive file.
     * @throws FileNotFoundException On any other I/O related issue.
     */
    public TFileInputStream(String name)
    throws FileNotFoundException {
        super(newInputStream(new TFile(name)));
    }

    /**
     * Creates a new {@code TFileInputStream} for accessing regular files or
     * archive entries.
     *
     * @throws FileBusyException If the path denotes an archive entry and the
     *         archive driver does not support to create an additional input
     *         stream for the archive file.
     * @throws FileNotFoundException On any other I/O related issue.
     */
    public TFileInputStream(File file)
    throws FileNotFoundException {
        super(newInputStream(file));
    }

    public TFileInputStream(FileDescriptor fd) {
        super(new FileInputStream(fd));
    }

    private static InputStream newInputStream(final File src)
    throws FileNotFoundException {
        final InputSocket<?> input = TIO.getInputSocket(
                src,
                BitField.noneOf(FsInputOption.class));
        try {
            return input.newInputStream();
        } catch (FileNotFoundException ex) {
            throw ex;
        } catch (IOException ex) {
            throw (FileNotFoundException) new FileNotFoundException(
                    ex.toString()).initCause(ex);
        }
    }
}
