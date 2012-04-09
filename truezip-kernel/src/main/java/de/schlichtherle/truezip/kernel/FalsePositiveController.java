/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.FsControlFlowIOException;
import de.truezip.kernel.*;
import de.truezip.kernel.FsEntryName;
import static de.truezip.kernel.FsEntryName.ROOT;
import de.truezip.kernel.FsPath;
import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.cio.Entry.Access;
import de.truezip.kernel.cio.Entry.Type;
import de.truezip.kernel.cio.InputSocket;
import de.truezip.kernel.cio.OutputSocket;
import de.truezip.kernel.FsAccessOption;
import de.truezip.kernel.FsSyncOption;
import de.truezip.kernel.util.BitField;
import de.truezip.kernel.util.ExceptionHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Implements a chain of responsibility for resolving
 * {@link FalsePositiveException}s which may get thrown by its decorated file
 * system controller.
 * <p>
 * Whenever the decorated controller for the prospective file system throws a
 * {@link FalsePositiveException}, the file system operation is routed to the
 * controller of the parent file system in order to continue the operation.
 * If this fails with another exception, then the {@link IOException} which is
 * associated as the cause of the initial exception gets rethrown unless the
 * other exception is an {@link FsControlFlowIOException}.
 * In this case the {@link FsControlFlowIOException} gets rethrown as is in order
 * to enable the caller to resolve it.
 * <p>
 * This algorithm effectively achieves the following objectives:
 * <ol>
 * <li>False positive federated file systems (i.e. false positive archive files)
 *     get resolved correctly by accessing them as entities of the parent file
 *     system.
 * <li>If the file system driver for the parent file system throws another
 *     exception, then it gets discarded and the exception initially thrown by
 *     the file system driver for the false positive archive file takes its
 *     place in order to provide the caller with a good indication of what went
 *     wrong in the first place.
 * <li>Exceptions which are thrown by the TrueZIP Kernel itself identify
 *     themselves by the type {@link FsControlFlowIOException}.
 *     They are excempt from this masquerade in order to support resolving them
 *     by a more competent caller.
 * </ol>
 * <p>
 * As an example consider accessing a RAES encrypted ZIP file:
 * With the default driver configuration of the module TrueZIP ZIP.RAES,
 * whenever a ZIP.RAES file gets mounted, the user is prompted for a password.
 * If the user cancels the password prompting dialog, then an appropriate
 * exception gets thrown.
 * The target archive controller would then catch this exception and flag the
 * archive file as a false positive by wrapping this exception in a
 * {@link FalsePositiveException}.
 * This class would then catch this false positive exception and try to resolve
 * the issue by using the parent file system controller.
 * Failing that, the initial exception would get rethrown in order to signal
 * to the caller that the user had cancelled password prompting.
 *
 * @see    FalsePositiveException
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class FalsePositiveController
extends FsDecoratingController<FsModel, FsController<?>> {

    private volatile State state = new TryChild();

    // These fields don't need to be volatile because reads and writes of
    // references are always atomic.
    // See The Java Language Specification, Third Edition, section 17.7
    // "Non-atomic Treatment of double and long".
    private /*volatile*/ @CheckForNull FsController<?> parent;
    private /*volatile*/ @CheckForNull FsPath path;

    /**
     * Constructs a new false positive file system controller.
     *
     * @param controller the decorated file system controller.
     */
    FalsePositiveController(final FsController<?> controller) {
        super(controller);
        assert null != super.getParent();
    }

    @Nullable <T> T call(   final IOOperation<T> operation,
                            final FsEntryName name)
    throws IOException {
        final State state = this.state;
        try {
            return state.call(operation, name);
        } catch (final PersistentFalsePositiveException ex) {
            assert state instanceof TryChild;
            return (this.state = new UseParent(ex)).call(operation, name);
        } catch (final FalsePositiveException ex) {
            assert state instanceof TryChild;
            return new UseParent(ex).call(operation, name);
        }
    }

    @Override
    public FsController<?> getParent() {
        final FsController<?> parent = this.parent;
        return null != parent ? parent : (this.parent = controller.getParent());
    }

    FsEntryName resolveParent(FsEntryName name) {
        return getPath().resolve(name).getEntryName();
    }

    private FsPath getPath() {
        final FsPath path = this.path;
        return null != path ? path : (this.path = getModel().getMountPoint().getPath());
    }

    @Override
    public boolean isReadOnly() throws IOException {
        return call(new IsReadOnly(), ROOT);
    }

    private static final class IsReadOnly implements IOOperation<Boolean> {
        @Override
        public Boolean call(final FsController<?> controller,
                            final FsEntryName name)
        throws IOException {
            return controller.isReadOnly();
        }
    } // IsReadOnly
    
    @Override
    public FsEntry getEntry(final FsEntryName name) throws IOException {
        return call(new GetEntry(), name);
    }

    private static final class GetEntry implements IOOperation<FsEntry> {
        @Override
        public FsEntry call(final FsController<?> controller,
                            final FsEntryName name)
        throws IOException {
            return controller.getEntry(name);
        }
    } // GetEntry

    @Override
    public boolean isReadable(final FsEntryName name) throws IOException {
        return call(new IsReadable(), name);
    }

    private static final class IsReadable implements IOOperation<Boolean> {
        @Override
        public Boolean call(final FsController<?> controller,
                            final FsEntryName name)
        throws IOException {
            return controller.isReadable(name);
        }
    } // IsReadable
    
    @Override
    public boolean isWritable(final FsEntryName name) throws IOException {
        return call(new IsWritable(), name);
    }

    private static final class IsWritable implements IOOperation<Boolean> {
        @Override
        public Boolean call(final FsController<?> controller,
                            final FsEntryName name)
        throws IOException {
            return controller.isWritable(name);
        }
    } // IsWritable

    @Override
    public boolean isExecutable(final FsEntryName name) throws IOException {
        return call(new IsExecutable(), name);
    }

    private static final class IsExecutable implements IOOperation<Boolean> {
        @Override
        public Boolean call(final FsController<?> controller,
                            final FsEntryName name)
        throws IOException {
            return controller.isExecutable(name);
        }
    } // IsWritable
    
    @Override
    public void setReadOnly(final FsEntryName name) throws IOException {
        call(new SetReadOnly(), name);
    }

    private static final class SetReadOnly implements IOOperation<Void> {
        @Override
        public Void call(   final FsController<?> controller,
                            final FsEntryName name)
        throws IOException {
            controller.setReadOnly(name);
            return null;
        }
    } // SetReadOnly
    
    @Override
    public boolean setTime(
            final FsEntryName name,
            final Map<Access, Long> times,
            final BitField<FsAccessOption> options)
    throws IOException {
        final class SetTime implements IOOperation<Boolean> {
            @Override
            public Boolean call(final FsController<?> controller,
                                final FsEntryName name)
            throws IOException {
                return controller.setTime(name, times, options);
            }
        } // SetTime

        return call(new SetTime(), name);
    }

    @Override
    public boolean setTime(
            final FsEntryName name,
            final BitField<Access> types,
            final long value,
            final BitField<FsAccessOption> options)
    throws IOException {
        final class SetTime implements IOOperation<Boolean> {
            @Override
            public Boolean call(final FsController<?> controller,
                                final FsEntryName name)
            throws IOException {
                return controller.setTime(name, types, value, options);
            }
        } // SetTime

        return call(new SetTime(), name);
    }

    @Override
    public InputSocket<?> getInputSocket(
            final FsEntryName name,
            final BitField<FsAccessOption> options) {
        @NotThreadSafe
        final class Input extends InputSocket<Entry> {
            @CheckForNull FsController<?> lastController;
            @Nullable InputSocket<?> socket;

            InputSocket<?> getBoundDelegate(final FsController<?> controller,
                                            final FsEntryName name) {
                return (lastController == controller
                        ? socket
                        : (socket = (lastController = controller)
                            .getInputSocket(name, options)))
                        .bind(this);
            }

            @Override
            public Entry getLocalTarget() throws IOException {                
                return call(new GetLocalTarget(), name);
            }

            final class GetLocalTarget implements IOOperation<Entry> {
                @Override
                public Entry call(
                        final FsController<?> controller,
                        final FsEntryName name)
                throws IOException {
                    return getBoundDelegate(controller, name)
                            .getLocalTarget();
                }
            } // GetLocalTarget

            @Override
            public InputStream newStream() throws IOException {
                return call(new NewStream(), name);
            }

            final class NewStream implements IOOperation<InputStream> {
                @Override
                public InputStream call(
                        final FsController<?> controller,
                        final FsEntryName name)
                throws IOException {
                    return getBoundDelegate(controller, name)
                            .newStream();
                }
            } // NewStream

            @Override
            public SeekableByteChannel newChannel()
            throws IOException {
                return call(new NewChannel(), name);
            }

            final class NewChannel implements IOOperation<SeekableByteChannel> {
                @Override
                public SeekableByteChannel call(
                        final FsController<?> controller,
                        final FsEntryName name)
                throws IOException {
                    return getBoundDelegate(controller, name)
                            .newChannel();
                }
            } // NewChannel
        } // Input

        return new Input();
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
    public OutputSocket<?> getOutputSocket(
            final FsEntryName name,
            final BitField<FsAccessOption> options,
            final @CheckForNull Entry template) {
        @NotThreadSafe
        final class Output extends OutputSocket<Entry> {
            @CheckForNull FsController<?> lastController;
            @Nullable OutputSocket<?> socket;

            OutputSocket<?> getBoundDelegate(final FsController<?> controller,
                                             final FsEntryName name) {
                return (lastController == controller
                        ? socket
                        : (socket = (lastController = controller)
                            .getOutputSocket(name, options, template)))
                        .bind(this);
            }

            @Override
            public Entry getLocalTarget() throws IOException {                
                return call(new GetLocalTarget(), name);
            }

            final class GetLocalTarget implements IOOperation<Entry> {
                @Override
                public Entry call(
                        final FsController<?> controller,
                        final FsEntryName name)
                throws IOException {
                    return getBoundDelegate(controller, name)
                            .getLocalTarget();
                }
            } // GetLocalTarget

            @Override
            public OutputStream newStream() throws IOException {
                return call(new NewStream(), name);
            }

            final class NewStream implements IOOperation<OutputStream> {
                @Override
                public OutputStream call(
                        final FsController<?> controller,
                        final FsEntryName name)
                throws IOException {
                    return getBoundDelegate(controller, name)
                            .newStream();
                }
            } // NewStream

            @Override
            public SeekableByteChannel newChannel()
            throws IOException {
                return call(new NewChannel(), name);
            }

            final class NewChannel implements IOOperation<SeekableByteChannel> {
                @Override
                public SeekableByteChannel call(
                        final FsController<?> controller,
                        final FsEntryName name)
                throws IOException {
                    return getBoundDelegate(controller, name)
                            .newChannel();
                }
            } // NewChannel
        } // Output

        return new Output();
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
    public void mknod(
            final FsEntryName name,
            final Type type,
            final BitField<FsAccessOption> options,
            final @CheckForNull Entry template)
    throws IOException {
        final class Mknod implements IOOperation<Void> {
            @Override
            public Void call(final FsController<?> controller,
                             final FsEntryName name)
            throws IOException {
                controller.mknod(name, type, options, template);
                return null;
            }
        } // Mknod

        call(new Mknod(), name);
    }

    @Override
    public void unlink(
            final FsEntryName name,
            final BitField<FsAccessOption> options)
    throws IOException {
        final class Unlink implements IOOperation<Void> {
            @Override
            public Void call(final FsController<?> controller,
                             final FsEntryName name)
            throws IOException {
                controller.unlink(name, options);
                if (name.isRoot()) {
                    assert controller == FalsePositiveController.this.controller;
                    // We have successfully removed the virtual root directory
                    // of a federated file system, i.e. an archive file.
                    // Now unlink the target archive file from the parent file
                    // system.
                    // Note that this makes an unlink operation NOT atomic
                    // because the lock for the parent file system is not
                    // acquired!
                    getParent().unlink(resolveParent(name), options);
                }
                return null;
            }
        } // Unlink

        if (name.isRoot())
            unlinkRoot(new Unlink());
        else
            call(new Unlink(), name);
    }

    private void unlinkRoot(final IOOperation<Void> operation)
    throws IOException {
        try {
            (state = new TryChild()).call(operation, ROOT);
        } catch (final FalsePositiveException ex) {
            new UseParent(ex).call(operation, ROOT);
        }
    }

    @Override
    public <X extends IOException> void
    sync(   final BitField<FsSyncOption> options,
            final ExceptionHandler<? super FsSyncException, X> handler)
    throws IOException {
        try {
            controller.sync(options, handler);
        } catch (FalsePositiveException ex) {
            throw new AssertionError(ex);
        }
        state = new TryChild();
    }

    private interface IOOperation<T> {
        @Nullable T call(FsController<?> controller, FsEntryName name)
        throws IOException;
    } // IOOperation

    private interface State {
        @Nullable <T> T call(IOOperation<T> operation, FsEntryName name)
        throws IOException;
    } // State

    @Immutable
    private final class TryChild implements State {
        @Override
        public <T> T call(  final IOOperation<T> operation,
                            final FsEntryName name)
        throws IOException {
            return operation.call(controller, name);
        }
    } // TryChild

    @Immutable
    private final class UseParent implements State {
        final IOException originalCause;

        UseParent(final FalsePositiveException ex) {
            this.originalCause = ex.getCause();
        }

        @Override
        public <T> T call(  final IOOperation<T> operation,
                            final FsEntryName name)
        throws IOException {
            try {
                return operation.call(getParent(), resolveParent(name));
            } catch (final FsControlFlowIOException ex) {
                assert !(ex instanceof FalsePositiveException);
                throw ex;
            } catch (final IOException ignored) {
                throw originalCause;
            }
        }
    } // UseParent
}
