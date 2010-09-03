/*
 * Copyright 2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.socket;

import de.schlichtherle.truezip.io.util.InputException;
import de.schlichtherle.truezip.io.util.Streams;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Provides static utility methods for dealing with sockets.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class Sockets {

    /** You cannot instantiate this class. */
    Sockets() {
    }

    /**
     * Copies an input stream created by the input stream socket {@code iss}
     * to an output stream created by the output stream socket {@code oss}.
     * This method <em>always</em> closes the acquired input stream and
     * output stream.
     *
     * @param  iss the input stream socket.
     * @param  oss the output stream socket.
     * @throws InputException if copying the data fails because of an
     *         {@code IOException} in the <em>input</em> stream.
     * @throws IOException if copying the data fails because of an
     *         {@code IOException} in the <em>output</em> stream.
     * @throws NullPointerException if any parameter is {@code null}.
     */
    public static <T> void copy(
            final InputStreamSocket<? extends T, ? super T> iss,
            final OutputStreamSocket<? extends T, ? super T> oss)
            throws IOException {
        final InputStream is;
        final OutputStream os;
        try {
            is = iss.newInputStream(oss);
        } catch (IOException ex) {
            throw new InputException(ex);
        }
        try {
            os = oss.newOutputStream(iss);
        } catch (IOException ex) {
            try {
                is.close(); // ignore any exception!
            } finally {
                throw ex;
            }
        }
        Streams.copy(is, os);
    }

    /**
     * Returns a non-{@code null} reference to an I/O reference to the given
     * target of I/O operations.
     *
     * @param  <T> The type of the target of I/O operations.
     * @throws NullPointerException if {@code target} is {@code null}.
     */
    public static <T> IOReference<T> getReference(final T target) {
        if (target == null)
            throw new NullPointerException();
        class TargetIOReference implements IOReference<T> {
            @Override
            public T getTarget() {
                return target;
            }
        } // class Reference
        return new TargetIOReference();
    }

    /**
     * Returns a non-{@code null} reference to an I/O stream socket for
     * accesing the given file.
     * 
     * @param  <PT> The minimum required type of the <i>peer targets</i> for
     *         reading and writing from and to the given file target.
     * @throws NullPointerException if {@code file} is {@code null}.
     */
    public static <PT> IOStreamSocket<File, PT> getSocket(final File file) {
        if (file == null)
            throw new NullPointerException();
        class FileIOStreamSocket<O> implements IOStreamSocket<File, O> {
            @Override
            public File getTarget() {
                return file;
            }

            @Override
            public InputStream newInputStream(final IOReference<? extends O> dst)
            throws IOException {
                return new FileInputStream(file);
            }

            @Override
            public OutputStream newOutputStream(final IOReference<? extends O> src)
            throws IOException {
                class FileOutputStreamDecorator extends FileOutputStream {
                    public FileOutputStreamDecorator() throws FileNotFoundException {
                        super(file);
                    }

                    @Override
                    public void close() throws IOException {
                        super.close();
                        final Object srcTarget = src != null ? src.getTarget() : null;
                        if (srcTarget instanceof File)
                            file.setLastModified(((File) srcTarget).lastModified());
                    }
                }
                return new FileOutputStreamDecorator();
            }
        } // class FileStreamSocket
        return new FileIOStreamSocket<PT>();
    }
}
