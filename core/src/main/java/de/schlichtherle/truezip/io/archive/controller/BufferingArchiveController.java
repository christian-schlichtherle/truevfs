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
import de.schlichtherle.truezip.io.socket.FileEntry;
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

    private final static class Pool implements CommonEntryPool<FileEntry> {
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

    private Map<String, Pool> pools;

    BufferingArchiveController( final ArchiveController controller) {
        super(controller);
    }

    private synchronized Pool getPool(final String path) {
        if (true) return null;
        Pool pool;
        if (null == pools)
            (pools = new HashMap<String, Pool>()).put(path, pool = new Pool());
        else if (null == (pool = pools.get(path)))
            pools.put(path, pool = new Pool());
        return pool;
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
            input = new BufferingInputSocket<CommonEntry>(input, getPool(path));
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
                    super(output, getPool(path));
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
        pools = null;
        super.sync(builder, options);
    }
}
