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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Provides static utility methods for dealing with I/O sockets.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class IOStreamSockets {

    /** You cannot instantiate this class. */
    IOStreamSockets() {
    }

    /**
     * Returns a non-{@code null} reference to an I/O stream socket for
     * accessing the given file.
     * 
     * @param  <PT> The minimum required type of the <i>peer targets</i> for
     *         accessing the given file.
     * @return A non-{@code null} reference to an I/O stream socket for
     *         accessing the given file.
     * @throws NullPointerException if {@code file} is {@code null}.
     */
    public static <PT> IOStreamSocket<File, PT> get(final File file) {
        if (file == null)
            throw new NullPointerException();
        class FileIOStreamSocket<PT> implements IOStreamSocket<File, PT> {
            @Override
            public File get() {
                return file;
            }

            @Override
            public InputStream newInputStream(PT dst)
            throws FileNotFoundException {
                return new FileInputStream(file);
            }

            @Override
            public OutputStream newOutputStream(final PT src)
            throws FileNotFoundException {
                class FileOutputStreamDecorator extends FileOutputStream {
                    public FileOutputStreamDecorator() throws FileNotFoundException {
                        super(file);
                    }

                    @Override
                    public void close() throws IOException {
                        super.close();
                        if (src instanceof File)
                            file.setLastModified(((File) src).lastModified());
                    }
                }
                return new FileOutputStreamDecorator();
            }
        } // class FileIOStreamSocket
        return new FileIOStreamSocket<PT>();
    }
}
