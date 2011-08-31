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
import de.schlichtherle.truezip.entry.Entry.Access;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.fs.FsDecoratingConcurrentModelController;
import de.schlichtherle.truezip.fs.FsEntry;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsInputOption;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.fs.FsOutputOptions;
import de.schlichtherle.truezip.fs.FsSyncException;
import de.schlichtherle.truezip.fs.FsSyncOption;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.DecoratingInputSocket;
import de.schlichtherle.truezip.socket.DecoratingOutputSocket;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
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
 * order to inject the original values of selected parameters for its operation
 * in progress.
 * 
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
final class FsContextController
extends FsDecoratingConcurrentModelController<FsDefaultArchiveController<?>> {

    private static final FsOperationContext
            NULL = new FsOperationContext();
    static {
        NULL.setOutputOptions(FsOutputOptions.NO_OUTPUT_OPTIONS);
    }

    private static final Map<BitField<FsOutputOption>, FsOperationContext>
            contexts = new HashMap<BitField<FsOutputOption>, FsOperationContext>();

    /**
     * Constructs a new operation file system controller.
     *
     * @param controller the decorated concurrent file system controller.
     */
    FsContextController(
            FsDefaultArchiveController<?> controller) {
        super(controller);
    }

    @Override
    public Icon getOpenIcon() throws IOException {
        final FsDefaultArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(NULL);
        try {
            return delegate.getOpenIcon();
        } finally {
            delegate.setContext(context);
        }
    }

    @Override
    public Icon getClosedIcon() throws IOException {
        final FsDefaultArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(NULL);
        try {
            return delegate.getClosedIcon();
        } finally {
            delegate.setContext(context);
        }
    }

    @Override
    public boolean isReadOnly() throws IOException {
        final FsDefaultArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(NULL);
        try {
            return delegate.isReadOnly();
        } finally {
            delegate.setContext(context);
        }
    }

    @Override
    public FsEntry getEntry(FsEntryName name)
    throws IOException {
        final FsDefaultArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(NULL);
        try {
            return delegate.getEntry(name);
        } finally {
            delegate.setContext(context);
        }
    }

    @Override
    public boolean isReadable(FsEntryName name) throws IOException {
        final FsDefaultArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(NULL);
        try {
            return delegate.isReadable(name);
        } finally {
            delegate.setContext(context);
        }
    }

    @Override
    public boolean isWritable(FsEntryName name) throws IOException {
        final FsDefaultArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(NULL);
        try {
            return delegate.isWritable(name);
        } finally {
            delegate.setContext(context);
        }
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        final FsDefaultArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(NULL);
        try {
            delegate.setReadOnly(name);
        } finally {
            delegate.setContext(context);
        }
    }

    @Override
    public boolean setTime(
            FsEntryName name,
            Map<Access, Long> times,
            BitField<FsOutputOption> options)
    throws IOException {
        final FsDefaultArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(makeContext(options));
        try {
            return delegate.setTime(name, times, options);
        } finally {
            delegate.setContext(context);
        }
    }

    @Override
    public boolean setTime(
            FsEntryName name,
            BitField<Access> types,
            long value,
            BitField<FsOutputOption> options)
    throws IOException {
        final FsDefaultArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(makeContext(options));
        try {
            return delegate.setTime(name, types, value, options);
        } finally {
            delegate.setContext(context);
        }
    }

    @Override
    public InputSocket<?> getInputSocket(   FsEntryName name,
                                            BitField<FsInputOption> options) {
        return new Input(delegate.getInputSocket(name, options));
    }

    @Override
    public OutputSocket<?> getOutputSocket( FsEntryName name,
                                            BitField<FsOutputOption> options,
                                            Entry template) {
        return new Output(
                delegate.getOutputSocket(name, options, template),
                options);
    }

    @Override
    public void mknod(
            FsEntryName name,
            Type type,
            BitField<FsOutputOption> options,
            @CheckForNull Entry template)
    throws IOException {
        final FsDefaultArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(makeContext(options));
        try {
            delegate.mknod(name, type, options, template);
        } finally {
            delegate.setContext(context);
        }
    }

    @Override
    public void unlink(FsEntryName name, BitField<FsOutputOption> options)
    throws IOException {
        final FsDefaultArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(makeContext(options));
        try {
            delegate.unlink(name, options);
        } finally {
            delegate.setContext(context);
        }
    }

    @Override
    public <X extends IOException>
    void sync(
            final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        final FsDefaultArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(NULL);
        try {
            delegate.sync(options, handler);
        } finally {
            delegate.setContext(context);
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

    private final class Input extends DecoratingInputSocket<Entry> {
        Input(InputSocket<?> input) {
            super(input);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            final FsDefaultArchiveController<?>
                    delegate = FsContextController.this.delegate;
            final FsOperationContext context = delegate.getContext();
            delegate.setContext(NULL);
            try {
                return getBoundSocket().getLocalTarget();
            } finally {
                delegate.setContext(context);
            }
        }

        @Override
        public Entry getPeerTarget() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().getPeerTarget();
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            final FsDefaultArchiveController<?>
                    delegate = FsContextController.this.delegate;
            final FsOperationContext context = delegate.getContext();
            delegate.setContext(NULL);
            try {
                return getBoundSocket().newReadOnlyFile();
            } finally {
                delegate.setContext(context);
            }
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            final FsDefaultArchiveController<?>
                    delegate = FsContextController.this.delegate;
            final FsOperationContext context = delegate.getContext();
            delegate.setContext(NULL);
            try {
                return getBoundSocket().newSeekableByteChannel();
            } finally {
                delegate.setContext(context);
            }
        }

        @Override
        public InputStream newInputStream() throws IOException {
            final FsDefaultArchiveController<?>
                    delegate = FsContextController.this.delegate;
            final FsOperationContext context = delegate.getContext();
            delegate.setContext(NULL);
            try {
                return getBoundSocket().newInputStream();
            } finally {
                delegate.setContext(context);
            }
        }
    } // Input

    private final class Output extends DecoratingOutputSocket<Entry> {
        final FsOperationContext operation;

        Output(
                OutputSocket<?> output,
                BitField<FsOutputOption> options) {
            super(output);
            this.operation = makeContext(options);
            
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            final FsDefaultArchiveController<?>
                    delegate = FsContextController.this.delegate;
            final FsOperationContext context = delegate.getContext();
            delegate.setContext(operation);
            try {
                return getBoundSocket().getLocalTarget();
            } finally {
                delegate.setContext(context);
            }
        }

        @Override
        public Entry getPeerTarget() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().getPeerTarget();
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            final FsDefaultArchiveController<?>
                    delegate = FsContextController.this.delegate;
            final FsOperationContext context = delegate.getContext();
            delegate.setContext(operation);
            try {
                return getBoundSocket().newSeekableByteChannel();
            } finally {
                delegate.setContext(context);
            }
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            final FsDefaultArchiveController<?>
                    delegate = FsContextController.this.delegate;
            final FsOperationContext context = delegate.getContext();
            delegate.setContext(operation);
            try {
                return getBoundSocket().newOutputStream();
            } finally {
                delegate.setContext(context);
            }
        }
    } // Output
}
