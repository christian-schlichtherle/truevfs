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
package de.schlichtherle.truezip.io.archive.controller;

import de.schlichtherle.truezip.io.socket.FileEntry;
import de.schlichtherle.truezip.io.Files;
import de.schlichtherle.truezip.io.socket.CommonEntryPool;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import de.schlichtherle.truezip.io.socket.OutputOption;
import de.schlichtherle.truezip.io.socket.InputOption;
import de.schlichtherle.truezip.io.socket.BufferingInputSocket;
import de.schlichtherle.truezip.io.socket.BufferingOutputSocket;
import de.schlichtherle.truezip.io.socket.CommonEntry;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.TempFilePool;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import java.io.OutputStream;

import static de.schlichtherle.truezip.io.socket.CommonEntry.Type.FILE;
import static de.schlichtherle.truezip.io.socket.OutputOption.COPY_PROPERTIES;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class BufferingArchiveController extends FilterArchiveController {

    private final static class Buffer implements CommonEntryPool<FileEntry> {
        final String path;
        final FileEntry temp;

        Buffer(final String path) throws IOException {
            this.path = path;
            this.temp = TempFilePool.get().allocate();
        }

        File getFile() {
            return temp.getTarget();
        }

        @Override
        public FileEntry allocate() {
            return temp;
        }

        @Override
        public void release(final FileEntry entry) {
            assert entry == temp;
        }

        void release() throws IOException {
            TempFilePool.get().release(temp);
        }
    }

    private final Map<String, Buffer> buffers = new HashMap<String, Buffer>();

    BufferingArchiveController( final ArchiveController controller) {
        super(controller);
    }

    @Override
    public InputSocket<?> getInputSocket(
            final String path,
            final BitField<InputOption> options)
    throws IOException {
        final BitField<InputOption> options2 = options
                .clear(InputOption.BUFFER);
        InputSocket<?> input = getController()
                .getInputSocket(path, options2);
        if (options.get(InputOption.BUFFER)) {
            input = new BufferingInputSocket<CommonEntry>(input);
        }
        return input;
    }

    @Override
    public OutputSocket<?> getOutputSocket(
            final String path,
            final BitField<OutputOption> options)
    throws IOException {
        final BitField<OutputOption> options2 = options
                .clear(OutputOption.BUFFER);
        OutputSocket<?> output = getController()
                .getOutputSocket(path, options2);
        if (options.get(OutputOption.BUFFER)) {

            class Output extends BufferingOutputSocket<CommonEntry> {
                Output(OutputSocket<?> output) {
                    super(output);
                }

                @Override
                public OutputStream newOutputStream() throws IOException {
                    final OutputStream out = super.newOutputStream();
                    boolean ok = false;
                    try {
                        final CommonEntry template = options.get(COPY_PROPERTIES)
                            ? getRemoteTarget()
                            : null;
                        getController().mknod(path, FILE, template, options2);
                        ok = true;
                    } finally {
                        if (!ok)
                            out.close();
                    }
                    return out;
                }
            } // class Output

            output = new Output(output);
        }
        return output;
    }
}
