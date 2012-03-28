/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel.fs;

import de.truezip.kernel.cio.Entry.Access;
import de.truezip.kernel.cio.Entry.Type;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.fs.FsDecoratingController;
import de.truezip.kernel.fs.FsEntry;
import de.truezip.kernel.fs.FsSyncException;
import de.truezip.kernel.fs.addr.FsEntryName;
import de.truezip.kernel.fs.option.FsInputOption;
import de.truezip.kernel.fs.option.FsOutputOption;
import de.truezip.kernel.fs.option.FsOutputOptions;
import de.truezip.kernel.fs.option.FsSyncOption;
import de.truezip.kernel.rof.ReadOnlyFile;
import de.truezip.kernel.util.BitField;
import de.truezip.kernel.util.ExceptionHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Injects the original values of selected parameters of the operation in
 * progress into its decorated controller.
 * 
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class FsContextController
extends FsDecoratingController<FsLockModel, FsTargetArchiveController<?>> {

    private static final FsOperationContext
            NULL = new FsOperationContext(FsOutputOptions.NONE);

    /**
     * Constructs a new file system context controller.
     *
     * @param controller the decorated file system controller.
     */
    FsContextController(FsTargetArchiveController<?> controller) {
        super(controller);
    }

    @Override
    public boolean isReadOnly() throws IOException {
        final FsTargetArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(NULL);
        try {
            return delegate.isReadOnly();
        } finally {
            delegate.setContext(context);
        }
    }

    @Override
    public FsEntry getEntry(final FsEntryName name)
    throws IOException {
        final FsTargetArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(NULL);
        try {
            return delegate.getEntry(name);
        } finally {
            delegate.setContext(context);
        }
    }

    @Override
    public boolean isReadable(final FsEntryName name) throws IOException {
        final FsTargetArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(NULL);
        try {
            return delegate.isReadable(name);
        } finally {
            delegate.setContext(context);
        }
    }

    @Override
    public boolean isWritable(final FsEntryName name) throws IOException {
        final FsTargetArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(NULL);
        try {
            return delegate.isWritable(name);
        } finally {
            delegate.setContext(context);
        }
    }

    @Override
    public boolean isExecutable(final FsEntryName name) throws IOException {
        final FsTargetArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(NULL);
        try {
            return delegate.isExecutable(name);
        } finally {
            delegate.setContext(context);
        }
    }

    @Override
    public void setReadOnly(final FsEntryName name) throws IOException {
        final FsTargetArchiveController<?> delegate = this.delegate;
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
            final FsEntryName name,
            final Map<Access, Long> times,
            final BitField<FsOutputOption> options)
    throws IOException {
        final FsTargetArchiveController<?> delegate = this.delegate;
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
            final FsEntryName name,
            final BitField<Access> types,
            final long value,
            final BitField<FsOutputOption> options)
    throws IOException {
        final FsTargetArchiveController<?> delegate = this.delegate;
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
                                            @CheckForNull Entry template) {
        return new Output(delegate.getOutputSocket(name, options, template),
                options);
    }

    @Override
    public void mknod(
            final FsEntryName name,
            final Type type,
            final BitField<FsOutputOption> options,
            final @CheckForNull Entry template)
    throws IOException {
        final FsTargetArchiveController<?> delegate = this.delegate;
        final FsOperationContext context = delegate.getContext();
        delegate.setContext(makeContext(options));
        try {
            delegate.mknod(name, type, options, template);
        } finally {
            delegate.setContext(context);
        }
    }

    @Override
    public void unlink( final FsEntryName name,
                        final BitField<FsOutputOption> options)
    throws IOException {
        final FsTargetArchiveController<?> delegate = this.delegate;
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
    void sync(  final BitField<FsSyncOption> options,
                final ExceptionHandler<? super FsSyncException, X> handler)
    throws IOException {
        final FsTargetArchiveController<?> delegate = this.delegate;
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
     * 
     * @param  options the options for the output operation in progress.
     * @return An operation context holding the given output options.
     */
    private static FsOperationContext makeContext(
            final BitField<FsOutputOption> options) {
        return new FsOperationContext(options);
    }

    private final class Input extends DecoratingInputSocket<Entry> {
        Input(InputSocket<?> input) {
            super(input);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            final FsTargetArchiveController<?>
                    delegate = FsContextController.this.delegate;
            final FsOperationContext context = delegate.getContext();
            delegate.setContext(NULL);
            try {
                return getBoundDelegate().getLocalTarget();
            } finally {
                delegate.setContext(context);
            }
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            final FsTargetArchiveController<?>
                    delegate = FsContextController.this.delegate;
            final FsOperationContext context = delegate.getContext();
            delegate.setContext(NULL);
            try {
                return getBoundDelegate().newReadOnlyFile();
            } finally {
                delegate.setContext(context);
            }
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            final FsTargetArchiveController<?>
                    delegate = FsContextController.this.delegate;
            final FsOperationContext context = delegate.getContext();
            delegate.setContext(NULL);
            try {
                return getBoundDelegate().newSeekableByteChannel();
            } finally {
                delegate.setContext(context);
            }
        }

        @Override
        public InputStream newInputStream() throws IOException {
            final FsTargetArchiveController<?>
                    delegate = FsContextController.this.delegate;
            final FsOperationContext context = delegate.getContext();
            delegate.setContext(NULL);
            try {
                return getBoundDelegate().newInputStream();
            } finally {
                delegate.setContext(context);
            }
        }
    } // Input

    private final class Output extends DecoratingOutputSocket<Entry> {
        final FsOperationContext operation;

        Output( OutputSocket<?> output,
                BitField<FsOutputOption> options) {
            super(output);
            this.operation = makeContext(options);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            final FsTargetArchiveController<?>
                    delegate = FsContextController.this.delegate;
            final FsOperationContext context = delegate.getContext();
            delegate.setContext(operation);
            try {
                return getBoundDelegate().getLocalTarget();
            } finally {
                delegate.setContext(context);
            }
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            final FsTargetArchiveController<?>
                    delegate = FsContextController.this.delegate;
            final FsOperationContext context = delegate.getContext();
            delegate.setContext(operation);
            try {
                return getBoundDelegate().newSeekableByteChannel();
            } finally {
                delegate.setContext(context);
            }
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            final FsTargetArchiveController<?>
                    delegate = FsContextController.this.delegate;
            final FsOperationContext context = delegate.getContext();
            delegate.setContext(operation);
            try {
                return getBoundDelegate().newOutputStream();
            } finally {
                delegate.setContext(context);
            }
        }
    } // Output
}