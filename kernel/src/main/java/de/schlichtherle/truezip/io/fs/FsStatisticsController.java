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
package de.schlichtherle.truezip.io.fs;

import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.socket.DecoratorInputSocket;
import de.schlichtherle.truezip.io.socket.DecoratorOutputSocket;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.jcip.annotations.ThreadSafe;

/**
 * Implements statistics for its decorated file system controller.
 * <p>
 * This class is thread-safe iff the decorated file system controller is
 * thread-safe.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
class FsStatisticsController
extends FsDecoratorController<FsModel, FsController<?>> {

    private final FsStatisticsManager manager;

    /**
     * Constructs a new statistics file system controller.
     *
     * @param controller the decorated file system controller.
     * @param manager the statistics file system manager.
     */
    FsStatisticsController(
            @NonNull FsController<?> controller,
            @NonNull FsStatisticsManager manager) {
        super(controller);
        assert null != manager;
        this.manager = manager;
    }

    @Override
    public InputSocket<?> getInputSocket(
            final FsEntryName name,
            final BitField<FsInputOption> options) {
        return new Input(name, options);
    }

    private class Input extends DecoratorInputSocket<Entry> {
        Input(FsEntryName name, BitField<FsInputOption> options) {
            super(delegate.getInputSocket(name, options));
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
            FsEntryName name,
            BitField<FsOutputOption> options,
            Entry template) {
        return new Output(name, options, template);
    }

    private class Output extends DecoratorOutputSocket<Entry> {
        Output(FsEntryName name, BitField<FsOutputOption> options, Entry template) {
            super(delegate.getOutputSocket(name, options, template));
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            return manager.getStatistics().countBytes(
                    getBoundSocket().newOutputStream());
        }
    } // class Output
}
