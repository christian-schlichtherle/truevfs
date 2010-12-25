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
package de.schlichtherle.truezip.io.filesystem.concurrent;

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
import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;
import net.jcip.annotations.NotThreadSafe;

/**
 * Caches I/O sockets created by its delegate.
 * 
 * @author Christian Schlichtherle
 * @version $Id: IOSocketCachingFileSystemController.java,v f5c420e8744e 2010/12/22 03:03:09 christian $
 */
@NotThreadSafe
public final class IOSocketCachingFileSystemController<
        M extends ConcurrentFileSystemModel,
        C extends FileSystemController<? extends M>>
extends DecoratingFileSystemController<M, C> {

    private Map<FileSystemEntryName, Input> inputs
            = new WeakHashMap<FileSystemEntryName, Input>();

    private Map<FileSystemEntryName, Output> outputs
            = new WeakHashMap<FileSystemEntryName, Output>();

    /**
     * Constructs a new I/O socket caching file system controller.
     *
     * @param controller the decorated file system controller.
     */
    public IOSocketCachingFileSystemController(@NonNull C controller) {
        super(controller);
    }

    @Override
    public synchronized InputSocket<?> getInputSocket(
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
    public synchronized OutputSocket<?> getOutputSocket(
            FileSystemEntryName name,
            BitField<OutputOption> options,
            Entry template) {
        Output output = outputs.get(name);
        if (null == output
                || !output.options.equals(options)
                || output.template != template)
            outputs.put(name, output = new Output(name, options, template));
        return output;
    }

    private class Output extends DecoratingOutputSocket<Entry> {
        final FileSystemEntryName name;
        final BitField<OutputOption> options;
        final Entry template;

        Output( final FileSystemEntryName name,
                final BitField<OutputOption> options,
                final Entry template) {
            super(delegate.getOutputSocket(name, options, template));
            this.name = name;
            this.options = options;
            this.template = template;
        }
    } // class Output

    @Override
    public void unlink(final FileSystemEntryName name) throws IOException {
        assert getModel().writeLock().isHeldByCurrentThread();

        delegate.unlink(name);
        inputs.remove(name);
        outputs.remove(name);
    }

    @Override
    public <X extends IOException>
    void sync(
            @NonNull final BitField<SyncOption> options,
            @NonNull final ExceptionHandler<? super SyncException, X> handler)
    throws X, FileSystemException {
        assert getModel().writeLock().isHeldByCurrentThread();

        try {
            delegate.sync(options, handler);
        } finally {
            inputs.clear();
            outputs.clear();
        }
    }
}
