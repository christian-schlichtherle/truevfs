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

import de.schlichtherle.truezip.io.archive.model.ArchiveModel;
import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.filesystem.DecoratingFileSystemController;
import de.schlichtherle.truezip.io.filesystem.FileSystemController;
import de.schlichtherle.truezip.io.filesystem.FileSystemEntryName;
import de.schlichtherle.truezip.io.filesystem.FileSystemException;
import de.schlichtherle.truezip.io.filesystem.InputOption;
import de.schlichtherle.truezip.io.filesystem.OutputOption;
import de.schlichtherle.truezip.io.filesystem.SyncException;
import de.schlichtherle.truezip.io.filesystem.SyncOption;
import de.schlichtherle.truezip.io.socket.DecoratingInputSocket;
import de.schlichtherle.truezip.io.socket.DecoratingOutputSocket;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;
import net.jcip.annotations.ThreadSafe;

/**
 * Caches I/O sockets created by its delegate.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public final class IOSocketCachingArchiveController
extends DecoratingFileSystemController<
        ArchiveModel,
        FileSystemController<? extends ArchiveModel>> {

    private Map<FileSystemEntryName, Input> inputs
            = new WeakHashMap<FileSystemEntryName, Input>();

    private Map<FileSystemEntryName, Output> outputs
            = new WeakHashMap<FileSystemEntryName, Output>();

    public IOSocketCachingArchiveController(
            FileSystemController<? extends ArchiveModel> controller) {
        super(controller);
    }

    @Override
    public InputSocket<?> getInputSocket(
            FileSystemEntryName name,
            BitField<InputOption> options) {
        Input input = inputs.get(name);
        if (null == input || !input.options.equals(options))
            inputs.put(name, input = new Input(name, options));
        return input;
    }

    private class Input extends DecoratingInputSocket<Entry> {
        final FileSystemEntryName name;
        final BitField<InputOption> options;

        Input(final FileSystemEntryName name, final BitField<InputOption> options) {
            super(delegate.getInputSocket(name, options));
            this.name = name;
            this.options = options;
        }
    } // class Input

    @Override
    public OutputSocket<?> getOutputSocket(
            FileSystemEntryName name,
            BitField<OutputOption> options,
            Entry template) {
        Output output = outputs.get(name);
        if (null == output || !output.options.equals(options))
            outputs.put(name, output = new Output(name, options, template));
        return output;
    }

    private class Output extends DecoratingOutputSocket<Entry> {
        final FileSystemEntryName name;
        final BitField<OutputOption> options;

        Output( final FileSystemEntryName name,
                final BitField<OutputOption> options,
                final Entry template) {
            super(delegate.getOutputSocket(name, options, template));
            this.name = name;
            this.options = options;
        }
    } // class Output

    @Override
    public <X extends IOException>
    void sync(  @NonNull BitField<SyncOption> options,
                @NonNull ExceptionBuilder<? super SyncException, X> builder)
    throws X, FileSystemException {
        try {
            delegate.sync(options, builder);
        } finally {
            inputs.clear();
            outputs.clear();
        }
    }
}
