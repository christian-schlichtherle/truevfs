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

import de.schlichtherle.truezip.util.ExceptionBuilder;
import de.schlichtherle.truezip.io.entry.FileEntry;
import de.schlichtherle.truezip.io.entry.CommonEntryPool;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import de.schlichtherle.truezip.io.socket.OutputOption;
import de.schlichtherle.truezip.io.socket.InputOption;
import de.schlichtherle.truezip.io.socket.CachingInputSocket;
import de.schlichtherle.truezip.io.socket.CachingOutputSocket;
import de.schlichtherle.truezip.io.entry.CommonEntry;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.entry.TempFilePool;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import java.io.OutputStream;

import static de.schlichtherle.truezip.io.entry.CommonEntry.Type.FILE;
import static de.schlichtherle.truezip.io.socket.OutputOption.COPY_PROPERTIES;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class CachingArchiveController extends FilterArchiveController {

    private final static class Buffer implements CommonEntryPool<FileEntry> {
        FileEntry temp;

        File getFile() {
            return temp.getTarget();
        }

        @Override
        public synchronized FileEntry allocate() throws IOException {
            return null != temp ? temp : (temp = TempFilePool.get().allocate());
        }

        @Override
        public synchronized void release(final FileEntry entry) throws IOException {
            if (entry != temp)
                TempFilePool.get().release(temp);
        }
    }

    private Map<String, Buffer> buffers;

    CachingArchiveController( final ArchiveController controller) {
        super(controller);
    }

    private synchronized Buffer getBuffer(final String path) {
        if (true) return null;
        Buffer pool;
        if (null == buffers)
            (buffers = new HashMap<String, Buffer>()).put(path, pool = new Buffer());
        else if (null == (pool = buffers.get(path)))
            buffers.put(path, pool = new Buffer());
        return pool;
    }

    @Override
    public InputSocket<?> getInputSocket(
            final String path,
            final BitField<InputOption> options)
    throws IOException {
        final BitField<InputOption> options2 = options
                .clear(InputOption.CACHE);
        InputSocket<?> input = getController()
                .getInputSocket(path, options2);
        if (options.get(InputOption.CACHE)) {
            input = new CachingInputSocket<CommonEntry>(input, getBuffer(path));
        }
        return input;
    }

    @Override
    public OutputSocket<?> getOutputSocket(
            final String path,
            final BitField<OutputOption> options)
    throws IOException {
        final BitField<OutputOption> options2 = options
                .clear(OutputOption.CACHE);
        OutputSocket<?> output = getController()
                .getOutputSocket(path, options2);
        if (options.get(OutputOption.CACHE)) {

            class Output extends CachingOutputSocket<CommonEntry> {
                Output(OutputSocket<?> output) {
                    super(output, getBuffer(path));
                }

                @Override
                public OutputStream newOutputStream() throws IOException {
                    final OutputStream out = super.newOutputStream();
                    boolean ok = false;
                    try {
                        getController().mknod(
                                path, FILE,
                                options2.get(COPY_PROPERTIES) ? getRemoteTarget() : null,
                                options2);
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

    @Override
    public <E extends IOException>
    void sync(ExceptionBuilder<? super SyncException, E> builder, BitField<SyncOption> options)
    throws E {
        buffers = null;
        super.sync(builder, options);
    }
}
