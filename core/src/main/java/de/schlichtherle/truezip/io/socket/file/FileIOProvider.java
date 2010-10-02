/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.socket.file;

import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.rof.SimpleReadOnlyFile;
import de.schlichtherle.truezip.io.socket.input.CommonInputSocket;
import de.schlichtherle.truezip.io.socket.input.CommonInputSocketFactory;
import de.schlichtherle.truezip.io.socket.output.CommonOutputSocket;
import de.schlichtherle.truezip.io.socket.output.CommonOutputSocketFactory;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FileIOProvider
implements  CommonInputSocketFactory<FileEntry>,
            CommonOutputSocketFactory<FileEntry> {

    private static final FileIOProvider singleton = new FileIOProvider();

    public static FileIOProvider get() {
        return singleton;
    }

    private FileIOProvider() {
    }

    @Override
    public CommonInputSocket<FileEntry> newInputSocket(final FileEntry target)
    throws IOException {
        class InputSocket extends CommonInputSocket<FileEntry> {
            @Override
            public FileEntry getTarget() {
                return target;
            }

            @Override
            public InputStream newInputStream()
            throws IOException {
                return new FileInputStream(target);
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                return new SimpleReadOnlyFile(target);
            }
        }
        return new InputSocket();
    }

    @Override
    public CommonOutputSocket<FileEntry> newOutputSocket(final FileEntry target)
    throws IOException {
        class OutputSocket extends CommonOutputSocket<FileEntry> {
            @Override
            public FileEntry getTarget() {
                return target;
            }

            @Override
            public OutputStream newOutputStream()
            throws IOException {
                return new FileOutputStream(target);
            }
        }
        return new OutputSocket();
    }
}
