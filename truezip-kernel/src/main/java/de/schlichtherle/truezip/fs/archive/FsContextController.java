/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.entry.Entry.Access;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsDecoratingController;
import de.schlichtherle.truezip.fs.FsEntry;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsInputOption;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.fs.FsOutputOptions;
import de.schlichtherle.truezip.fs.FsSyncException;
import de.schlichtherle.truezip.fs.FsSyncOption;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.DecoratingInputSocket;
import de.schlichtherle.truezip.socket.DecoratingOutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import net.jcip.annotations.NotThreadSafe;

/**
 * A file system controller which decorates another file system controller in
 * order to provide the original values of selected parameters for its
 * operation in progress.
 * 
 * @see     FsContextModel
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
final class FsContextController
extends FsDecoratingController< FsContextModel,
                                FsController<? extends FsContextModel>> {

    private static final FsOperationContext
            NULL = new FsOperationContext();
    private static final FsOperationContext
            NONE = new FsOperationContext();
    static {
        NONE.setOutputOptions(FsOutputOptions.NO_OUTPUT_OPTIONS);
    }

    private static final Map<BitField<FsOutputOption>, FsOperationContext>
            contexts = new HashMap<BitField<FsOutputOption>, FsOperationContext>();

    /**
     * Constructs a new operation file system controller.
     *
     * @param controller the decorated file system controller.
     */
    FsContextController(
            @NonNull FsController<? extends FsContextModel> controller) {
        super(controller);
    }

    @Override
    public Icon getOpenIcon() throws IOException {
        final FsContextModel model = getModel();
        final FsOperationContext context = model.getContext();
        model.setContext(NULL);
        try {
            return delegate.getOpenIcon();
        } finally {
            model.setContext(context);
        }
    }

    @Override
    public Icon getClosedIcon() throws IOException {
        final FsContextModel model = getModel();
        final FsOperationContext context = model.getContext();
        model.setContext(NULL);
        try {
            return delegate.getClosedIcon();
        } finally {
            model.setContext(context);
        }
    }

    @Override
    public boolean isReadOnly() throws IOException {
        final FsContextModel model = getModel();
        final FsOperationContext context = model.getContext();
        model.setContext(NULL);
        try {
            return delegate.isReadOnly();
        } finally {
            model.setContext(context);
        }
    }

    @Override
    public FsEntry getEntry(FsEntryName name)
    throws IOException {
        final FsContextModel model = getModel();
        final FsOperationContext context = model.getContext();
        model.setContext(NULL);
        try {
            return delegate.getEntry(name);
        } finally {
            model.setContext(context);
        }
    }

    @Override
    public boolean isReadable(FsEntryName name) throws IOException {
        final FsContextModel model = getModel();
        final FsOperationContext context = model.getContext();
        model.setContext(NULL);
        try {
            return delegate.isReadable(name);
        } finally {
            model.setContext(context);
        }
    }

    @Override
    public boolean isWritable(FsEntryName name) throws IOException {
        final FsContextModel model = getModel();
        final FsOperationContext context = model.getContext();
        model.setContext(NULL);
        try {
            return delegate.isWritable(name);
        } finally {
            model.setContext(context);
        }
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        final FsContextModel model = getModel();
        final FsOperationContext context = model.getContext();
        model.setContext(NONE);
        try {
            delegate.setReadOnly(name);
        } finally {
            model.setContext(context);
        }
    }

    @Override
    public boolean setTime(FsEntryName name, BitField<Access> types, long value)
    throws IOException {
        final FsContextModel model = getModel();
        final FsOperationContext context = model.getContext();
        model.setContext(NONE);
        try {
            return delegate.setTime(name, types, value);
        } finally {
            model.setContext(context);
        }
    }

    @Override
    public boolean setTime(FsEntryName name, Map<Access, Long> times)
    throws IOException {
        final FsContextModel model = getModel();
        final FsOperationContext context = model.getContext();
        model.setContext(NONE);
        try {
            return delegate.setTime(name, times);
        } finally {
            model.setContext(context);
        }
    }

    @Override
    public InputSocket<?> getInputSocket(   FsEntryName name,
                                            BitField<FsInputOption> options) {
        return new Input(name, options);
    }

    private final class Input extends DecoratingInputSocket<Entry> {
        Input(  FsEntryName name,
                BitField<FsInputOption> options) {
            super(delegate.getInputSocket(name, options));
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            final FsContextModel model = getModel();
            final FsOperationContext context = model.getContext();
            model.setContext(NULL);
            try {
                return getBoundSocket().getLocalTarget();
            } finally {
                model.setContext(context);
            }
        }

        @Override
        public Entry getPeerTarget() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().getPeerTarget();
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            final FsContextModel model = getModel();
            final FsOperationContext context = model.getContext();
            model.setContext(NULL);
            try {
                return getBoundSocket().newReadOnlyFile();
            } finally {
                model.setContext(context);
            }
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            final FsContextModel model = getModel();
            final FsOperationContext context = model.getContext();
            model.setContext(NULL);
            try {
                return getBoundSocket().newSeekableByteChannel();
            } finally {
                model.setContext(context);
            }
        }

        @Override
        public InputStream newInputStream() throws IOException {
            final FsContextModel model = getModel();
            final FsOperationContext context = model.getContext();
            model.setContext(NULL);
            try {
                return getBoundSocket().newInputStream();
            } finally {
                model.setContext(context);
            }
        }
    } // Input

    @Override
    public OutputSocket<?> getOutputSocket( FsEntryName name,
                                            BitField<FsOutputOption> options,
                                            Entry template) {
        return new Output(name, options, template);
    }

    private final class Output extends DecoratingOutputSocket<Entry> {
        final FsOperationContext operation;

        Output( FsEntryName name,
                BitField<FsOutputOption> options,
                Entry template) {
            super(delegate.getOutputSocket(name, options, template));
            this.operation = makeContext(options);
            
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            final FsContextModel model = getModel();
            final FsOperationContext context = model.getContext();
            model.setContext(operation);
            try {
                return getBoundSocket().getLocalTarget();
            } finally {
                model.setContext(context);
            }
        }

        @Override
        public Entry getPeerTarget() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().getPeerTarget();
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            final FsContextModel model = getModel();
            final FsOperationContext context = model.getContext();
            model.setContext(operation);
            try {
                return getBoundSocket().newSeekableByteChannel();
            } finally {
                model.setContext(context);
            }
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            final FsContextModel model = getModel();
            final FsOperationContext context = model.getContext();
            model.setContext(operation);
            try {
                return getBoundSocket().newOutputStream();
            } finally {
                model.setContext(context);
            }
        }
    } // Output

    @Override
    public void mknod(
            @NonNull FsEntryName name,
            @NonNull Type type,
            @NonNull BitField<FsOutputOption> options,
            @CheckForNull Entry template)
    throws IOException {
        final FsContextModel model = getModel();
        final FsOperationContext context = model.getContext();
        model.setContext(makeContext(options));
        try {
            delegate.mknod(name, type, options, template);
        } finally {
            model.setContext(context);
        }
    }

    @Override
    public void unlink(FsEntryName name)
    throws IOException {
        final FsContextModel model = getModel();
        final FsOperationContext context = model.getContext();
        model.setContext(NONE);
        try {
            delegate.unlink(name);
        } finally {
            model.setContext(context);
        }
    }

    @Override
    public <X extends IOException>
    void sync(
            @NonNull final BitField<FsSyncOption> options,
            @NonNull final ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        final FsContextModel model = getModel();
        final FsOperationContext context = model.getContext();
        model.setContext(NONE);
        try {
            delegate.sync(options, handler);
        } finally {
            model.setContext(context);
        }
    }

    /**
     * Returns an operation context holding the given output options.
     * <p>
     * TODO: Consider reusing the created object by mapping it.
     * 
     * @param  options the options for the output operation in progress.
     * @return An operation context holding the given output options.
     */
    private static FsOperationContext makeContext(BitField<FsOutputOption> options) {
        FsOperationContext context;
        synchronized (FsContextController.class) {
            context = contexts.get(options);
            if (null == context) {
                context = new FsOperationContext();
                context.setOutputOptions(options);
                contexts.put(options, context);
            }
        }
        return context;
    }
}
