/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.FsDecoratingController;
import de.truezip.kernel.FsEntry;
import de.truezip.kernel.FsSyncException;
import de.truezip.kernel.addr.FsEntryName;
import de.truezip.kernel.cio.Entry.Access;
import de.truezip.kernel.cio.Entry.Type;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.option.AccessOption;
import de.truezip.kernel.option.AccessOptions;
import de.truezip.kernel.option.SyncOption;
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
            NONE = new FsOperationContext(AccessOptions.NONE);

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
        final FsTargetArchiveController<?> controller = this.controller;
        final FsOperationContext context = controller.getContext();
        controller.setContext(NONE);
        try {
            return controller.isReadOnly();
        } finally {
            controller.setContext(context);
        }
    }

    @Override
    public FsEntry getEntry(final FsEntryName name)
    throws IOException {
        final FsTargetArchiveController<?> controller = this.controller;
        final FsOperationContext context = controller.getContext();
        controller.setContext(NONE);
        try {
            return controller.getEntry(name);
        } finally {
            controller.setContext(context);
        }
    }

    @Override
    public boolean isReadable(final FsEntryName name) throws IOException {
        final FsTargetArchiveController<?> controller = this.controller;
        final FsOperationContext context = controller.getContext();
        controller.setContext(NONE);
        try {
            return controller.isReadable(name);
        } finally {
            controller.setContext(context);
        }
    }

    @Override
    public boolean isWritable(final FsEntryName name) throws IOException {
        final FsTargetArchiveController<?> controller = this.controller;
        final FsOperationContext context = controller.getContext();
        controller.setContext(NONE);
        try {
            return controller.isWritable(name);
        } finally {
            controller.setContext(context);
        }
    }

    @Override
    public boolean isExecutable(final FsEntryName name) throws IOException {
        final FsTargetArchiveController<?> controller = this.controller;
        final FsOperationContext context = controller.getContext();
        controller.setContext(NONE);
        try {
            return controller.isExecutable(name);
        } finally {
            controller.setContext(context);
        }
    }

    @Override
    public void setReadOnly(final FsEntryName name) throws IOException {
        final FsTargetArchiveController<?> controller = this.controller;
        final FsOperationContext context = controller.getContext();
        controller.setContext(NONE);
        try {
            controller.setReadOnly(name);
        } finally {
            controller.setContext(context);
        }
    }

    @Override
    public boolean setTime(
            final FsEntryName name,
            final Map<Access, Long> times,
            final BitField<AccessOption> options)
    throws IOException {
        final FsTargetArchiveController<?> controller = this.controller;
        final FsOperationContext context = controller.getContext();
        controller.setContext(makeContext(options));
        try {
            return controller.setTime(name, times, options);
        } finally {
            controller.setContext(context);
        }
    }

    @Override
    public boolean setTime(
            final FsEntryName name,
            final BitField<Access> types,
            final long value,
            final BitField<AccessOption> options)
    throws IOException {
        final FsTargetArchiveController<?> controller = this.controller;
        final FsOperationContext context = controller.getContext();
        controller.setContext(makeContext(options));
        try {
            return controller.setTime(name, types, value, options);
        } finally {
            controller.setContext(context);
        }
    }

    @Override
    public InputSocket<?> getInputSocket(
            final FsEntryName name,
            final BitField<AccessOption> options) {
        @NotThreadSafe
        final class Input extends DecoratingInputSocket<Entry> {
            Input() {
                super(controller.getInputSocket(name, options));
            }

            @Override
            public Entry getLocalTarget() throws IOException {
                final FsTargetArchiveController<?>
                        controller = FsContextController.this.controller;
                final FsOperationContext context = controller.getContext();
                controller.setContext(NONE);
                try {
                    return getBoundSocket().getLocalTarget();
                } finally {
                    controller.setContext(context);
                }
            }

            @Override
            public InputStream newStream() throws IOException {
                final FsTargetArchiveController<?>
                        controller = FsContextController.this.controller;
                final FsOperationContext context = controller.getContext();
                controller.setContext(NONE);
                try {
                    return getBoundSocket().newStream();
                } finally {
                    controller.setContext(context);
                }
            }

            @Override
            public SeekableByteChannel newChannel() throws IOException {
                final FsTargetArchiveController<?>
                        controller = FsContextController.this.controller;
                final FsOperationContext context = controller.getContext();
                controller.setContext(NONE);
                try {
                    return getBoundSocket().newChannel();
                } finally {
                    controller.setContext(context);
                }
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                final FsTargetArchiveController<?>
                        controller = FsContextController.this.controller;
                final FsOperationContext context = controller.getContext();
                controller.setContext(NONE);
                try {
                    return getBoundSocket().newReadOnlyFile();
                } finally {
                    controller.setContext(context);
                }
            }
        } // Input

        return new Input();
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE") // false positive
    @Override
    public OutputSocket<?> getOutputSocket(
            final FsEntryName name,
            final BitField<AccessOption> options,
            final @CheckForNull Entry template) {
        @NotThreadSafe
        final class Output extends DecoratingOutputSocket<Entry> {
            final FsOperationContext operation;

            Output() {
                super(controller.getOutputSocket(name, options, template));
                this.operation = makeContext(options);
            }

            @Override
            public Entry getLocalTarget() throws IOException {
                final FsTargetArchiveController<?>
                        controller = FsContextController.this.controller;
                final FsOperationContext context = controller.getContext();
                controller.setContext(operation);
                try {
                    return getBoundSocket().getLocalTarget();
                } finally {
                    controller.setContext(context);
                }
            }

            @Override
            public OutputStream newStream() throws IOException {
                final FsTargetArchiveController<?>
                        controller = FsContextController.this.controller;
                final FsOperationContext context = controller.getContext();
                controller.setContext(operation);
                try {
                    return getBoundSocket().newStream();
                } finally {
                    controller.setContext(context);
                }
            }

            @Override
            public SeekableByteChannel newChannel() throws IOException {
                final FsTargetArchiveController<?>
                        controller = FsContextController.this.controller;
                final FsOperationContext context = controller.getContext();
                controller.setContext(operation);
                try {
                    return getBoundSocket().newChannel();
                } finally {
                    controller.setContext(context);
                }
            }
        } // Output

        return new Output();
    }

    @Override
    public void mknod(
            final FsEntryName name,
            final Type type,
            final BitField<AccessOption> options,
            final @CheckForNull Entry template)
    throws IOException {
        final FsTargetArchiveController<?> controller = this.controller;
        final FsOperationContext context = controller.getContext();
        controller.setContext(makeContext(options));
        try {
            controller.mknod(name, type, options, template);
        } finally {
            controller.setContext(context);
        }
    }

    @Override
    public void unlink( final FsEntryName name,
                        final BitField<AccessOption> options)
    throws IOException {
        final FsTargetArchiveController<?> controller = this.controller;
        final FsOperationContext context = controller.getContext();
        controller.setContext(makeContext(options));
        try {
            controller.unlink(name, options);
        } finally {
            controller.setContext(context);
        }
    }

    @Override
    public <X extends IOException>
    void sync(  final BitField<SyncOption> options,
                final ExceptionHandler<? super FsSyncException, X> handler)
    throws IOException {
        final FsTargetArchiveController<?> controller = this.controller;
        final FsOperationContext context = controller.getContext();
        controller.setContext(NONE);
        try {
            controller.sync(options, handler);
        } finally {
            controller.setContext(context);
        }
    }

    /**
     * Returns an operation context holding the given output options.
     * 
     * @param  options the options for the output operation in progress.
     * @return An operation context holding the given output options.
     */
    private static FsOperationContext makeContext(
            final BitField<AccessOption> options) {
        return new FsOperationContext(options);
    }
}
