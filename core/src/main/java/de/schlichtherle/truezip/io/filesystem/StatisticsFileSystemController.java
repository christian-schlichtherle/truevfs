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
package de.schlichtherle.truezip.io.filesystem;

import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.socket.FilterInputSocket;
import de.schlichtherle.truezip.io.socket.FilterOutputSocket;
import de.schlichtherle.truezip.io.socket.InputOption;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputOption;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.jcip.annotations.ThreadSafe;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class StatisticsFileSystemController
extends FilterFileSystemController<FileSystemModel, FileSystemController<?>> {

    private final StatisticsFileSystemManager manager;

    StatisticsFileSystemController(
            FileSystemController<?> controller,
            StatisticsFileSystemManager manager) {
        super(controller);
        this.manager = manager;
    }

    @Override
    public InputSocket<?> getInputSocket(
            final FileSystemEntryName name,
            final BitField<InputOption> options) {
        return new Input(name, options);
    }

    private class Input extends FilterInputSocket<Entry> {
        Input(FileSystemEntryName name, BitField<InputOption> options) {
            super(controller.getInputSocket(name, options));
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            return manager.getStatistics().countBytes(
                    getBoundSocket().newReadOnlyFile());
        }

        @Override
        public InputStream newInputStream() throws IOException {
            return manager.getStatistics().countBytes(
                    getBoundSocket().newInputStream());
        }
    } // class Input

    @Override
    public OutputSocket<?> getOutputSocket(
            FileSystemEntryName name,
            BitField<OutputOption> options,
            Entry template) {
        return new Output(name, options, template);
    }

    private class Output extends FilterOutputSocket<Entry> {
        Output(FileSystemEntryName name, BitField<OutputOption> options, Entry template) {
            super(controller.getOutputSocket(name, options, template));
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            return manager.getStatistics().countBytes(
                    getBoundSocket().newOutputStream());
        }
    } // class Output
}
