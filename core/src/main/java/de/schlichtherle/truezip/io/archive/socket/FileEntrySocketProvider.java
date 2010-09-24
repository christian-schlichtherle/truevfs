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
package de.schlichtherle.truezip.io.archive.socket;

import de.schlichtherle.truezip.io.archive.entry.CommonEntry;
import de.schlichtherle.truezip.io.archive.entry.FileEntry;
import de.schlichtherle.truezip.io.archive.input.ArchiveInputSocket;
import de.schlichtherle.truezip.io.archive.input.ArchiveInputSocketProvider;
import de.schlichtherle.truezip.io.archive.output.ArchiveOutputSocket;
import de.schlichtherle.truezip.io.archive.output.ArchiveOutputSocketProvider;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.InputSocketProvider;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.io.socket.OutputSocketProvider;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FileEntrySocketProvider
implements  InputSocketProvider<FileEntry, CommonEntry>,
            OutputSocketProvider<FileEntry, CommonEntry> {

    private static final FileEntrySocketProvider singleton
            = new FileEntrySocketProvider();

    public static FileEntrySocketProvider get() {
        return singleton;
    }

    private FileEntrySocketProvider() {
    }

    @Override
    public InputSocket<FileEntry, CommonEntry> getInputSocket(final FileEntry target)
    throws IOException {
        class Input extends InputSocket<FileEntry, CommonEntry> {
            @Override
            public FileEntry getTarget() {
                return target;
            }

            @Override
            public InputStream newInputStream()
            throws IOException {
                return new FileInputStream(target.getFile());
            }
        }
        return new Input();
    }

    @Override
    public OutputSocket<FileEntry, CommonEntry> getOutputSocket(final FileEntry target)
    throws IOException {
        class Output extends OutputSocket<FileEntry, CommonEntry> {
            @Override
            public FileEntry getTarget() {
                return target;
            }

            @Override
            public OutputStream newOutputStream()
            throws IOException {
                return new FileOutputStream(target.getFile());
            }
        }
        return new Output();
    }
}
