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

import de.schlichtherle.truezip.io.socket.OutputOption;
import de.schlichtherle.truezip.io.socket.InputOption;
import de.schlichtherle.truezip.io.Files;
import de.schlichtherle.truezip.io.socket.BufferingInputSocket;
import de.schlichtherle.truezip.io.socket.BufferingOutputSocket;
import de.schlichtherle.truezip.io.socket.FileSystemEntry;
import de.schlichtherle.truezip.io.socket.CommonEntry;
import de.schlichtherle.truezip.io.socket.CommonEntry.Type;
import de.schlichtherle.truezip.io.socket.CommonEntry.Access;
import de.schlichtherle.truezip.io.socket.FileCreator;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class BufferingArchiveController extends ArchiveController {

    private static class Buffer implements FileCreator {
        final String path;
        final File temp;

        Buffer(final String path) throws IOException {
            this.path = path;
            this.temp = Files.createTempFile("tzp-bac");
        }

        public File createFile() throws IOException {
            return temp;
        }
    }

    private final ArchiveController controller;
    private final Map<String, Buffer> buffers = new HashMap<String, Buffer>();

    BufferingArchiveController( final ArchiveModel model,
                                final ArchiveController controller) {
        super(model);
        assert null != controller;
        this.controller = controller;
    }

    private ArchiveController getController() {
        return controller;
    }

    @Override
    public Icon getOpenIcon() {
        return getController().getOpenIcon();
    }

    @Override
    public Icon getClosedIcon() {
        return getController().getClosedIcon();
    }

    @Override
    public boolean isReadOnly() {
        return getController().isReadOnly();
    }

    @Override
    public FileSystemEntry getEntry(String path) {
        return getController().getEntry(path);
    }

    @Override
    public boolean isReadable(String path) {
        return getController().isReadable(path);
    }

    @Override
    public boolean isWritable(String path) {
        return getController().isWritable(path);
    }

    @Override
    public void setReadOnly(String path)
    throws IOException {
        getController().setReadOnly(path);
    }

    @Override
    public boolean setTime(String path, BitField<Access> types, long value)
    throws IOException {
        return getController().setTime(path, types, value);
    }

    @Override
    public InputSocket<?> newInputSocket(
            final String path,
            final BitField<InputOption> options)
    throws IOException {
        final BitField<InputOption> options2
                = options.clear(InputOption.BUFFER);
        InputSocket<?> input = getController().newInputSocket(path, options2);
        if (options.get(InputOption.BUFFER)) {
            input = new BufferingInputSocket<CommonEntry>(input);
        }
        return input;
    }

    @Override
    public OutputSocket<?> newOutputSocket(
            final String path,
            final BitField<OutputOption> options)
    throws IOException {
        final BitField<OutputOption> options2
                = options.clear(OutputOption.BUFFER);
        OutputSocket<?> output = getController().newOutputSocket(path, options2);
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
                        getController().mknod(path, Type.FILE, null, options2);
                        ok = true;
                    } finally {
                        if (!ok)
                            out.close();
                    }
                    return out;
                }
            }

            output = new Output(output);
        }
        return output;
    }

    @Override
    public boolean mknod(   String path,
                            Type type,
                            CommonEntry template,
                            BitField<OutputOption> options)
    throws IOException {
        return getController().mknod(path, type, template, options);
    }

    @Override
    public void unlink(String path)
    throws IOException {
        getController().unlink(path);
    }

    @Override
    public void sync(   ArchiveSyncExceptionBuilder builder,
                        BitField<SyncOption> options)
    throws ArchiveSyncException {
        getController().sync(builder, options);
    }
}
